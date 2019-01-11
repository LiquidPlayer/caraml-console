//
//  ConsoleJS.m
//  caraml-console
//
//  Created by Eric Lange on 1/10/19.
//  Copyright © 2019 LiquidPlayer. All rights reserved.
//

#import "ConsoleJS.h"
#import "UIColor+String.h"
#import "ConsoleView.h"
#import <caraml_core/caraml_core.h>

typedef enum _State {
    Init,
    Attaching,
    Attached,
    Detaching,
    Detached
} State;

@interface ConsoleJS()
@property (nonatomic, strong) JSValue* thiz;
@property (nonatomic, strong, readonly) JSValue* emitFunc;
@end

@implementation ConsoleJS {
    State currentState_;
    JSValue* attachPromise_;
    JSValue* detachPromise_;
    ConsoleView* currentView_;
    BOOL processedException_;
    LCProcess *process_;
    JSContext *context_;
    JSValue *emitFunc_;
    LCCaramlJS *caramlJS_;
}

static NSString* createJSS =
    @"((thiz) => {"
    @"  let e=new events();"
    @"  e.attach = thiz.attach;"
    @"  e.detach = thiz.detach;"
    @"  e.state  = thiz.state;"
    @"  e.write  = thiz.write;"
    @"  return e;"
    @"})";

static NSString* createPromiseObjectS =
    @"(()=>{"
    @"  var po = {}; var clock = true;"
    @"  var timer = setInterval(()=>{if(!clock) clearTimeout(timer);}, 100); "
    @"  po.promise = new Promise((resolve,reject)=>{po.resolve=resolve;po.reject=reject});"
    @"  po.promise.then(()=>{clock=false}).catch(()=>{clock=false});"
    @"  return po;"
    @"})()";

static NSString* emitFuncS = @"() => arguments[0].emit.call(...arguments)";

- (id) init:(JSContext*)context service:(LCMicroService*)service opts:(JSValue*)opts
{
    self = [super init];
    if (self != nil) {
        currentState_ = Init;
        process_ = service.process;
        context_ = context;
        
        // defaults
        _backgroundColor = UIColor.blackColor;
        _textColor = UIColor.greenColor;
        _fontSize = @(12);
        
        if (opts != nil && [opts isObject]) {
            JSValue* bg = opts[@"backgroundColor"];
            JSValue* tc = opts[@"textColor"];
            JSValue* fs = opts[@"fontSize"];
            if ([bg isString]) {
                UIColor *bgc = [UIColor colorWithString:[bg toString]];
                if (bgc != nil) _backgroundColor = bgc;
            }
            if ([tc isString]) {
                UIColor *tcc = [UIColor colorWithString:[bg toString]];
                if (tcc != nil) _textColor = tcc;
            }
            if ([fs isNumber]) {
                _fontSize = [fs toNumber];
            }
        }
        
        dispatch_async(dispatch_get_main_queue(), ^{
            self->currentView_ = [[ConsoleView alloc] initWithFrame:CGRectMake(0, 0, 0, 0)];
            [self->currentView_ setSession:self];
            self->currentState_ = Detached;
            [self->process_ async:^(JSContext *context) {
                [self.emitFunc callWithArguments:@[self.thiz, @"ready"]];
            }];
        });
    }
    return self;
}

- (JSValue *)emitFunc
{
    if (emitFunc_ == nil) {
        emitFunc_ = [context_ evaluateScript:emitFuncS];
    }
    assert(emitFunc_ != nil);
    return emitFunc_;
}

+ (JSValue *) createConsole:(JSContext*)context
                 service:(LCMicroService*)service
                    opts:(JSValue*)opts
{
    ConsoleJS *consoleJS = [[ConsoleJS alloc] init:context service:service opts:opts];
    JSValue *createJS = [context evaluateScript:createJSS];
    JSValue *nativeObject = [createJS callWithArguments:@[consoleJS]];
    consoleJS.thiz = nativeObject;
    return nativeObject;
}

- (void) resize:(int)columns rows:(int)rows
{
    [process_ async:^(JSContext *context) {
        [self.emitFunc callWithArguments:@[self.thiz, @"resize", @(columns), @(rows)]];
    }];
}

- (void) processCommand:(NSString*)cmd
{
    processedException_ = NO;
    if (process_ != nil) {
        [process_ async:^(JSContext *context) {
            JSValue *output = [context evaluateScript:cmd];
            if (!self->processedException_) {
                [context[@"console"] invokeMethod:@"log" withArguments:@[output]];
            }
        }];
    }
}

#pragma JavaScript API

- (JSValue*) attach:(JSValue*)value
{
    attachPromise_ = [context_ evaluateScript:createPromiseObjectS];
    @try {
        if (value == nil || !value.isObject)
            @throw [NSException exceptionWithName:@"attach: first argument must be a caraml object"
                                           reason:nil userInfo:nil];
        if (currentState_ != Detached)
            @throw [NSException exceptionWithName:@"attach: must be in detached state"
                                           reason:nil userInfo:nil];
        caramlJS_ = [LCCaramlJS from:value];
        currentState_ = Attaching;
        [caramlJS_ attach:self];
    } @catch (NSException* e) {
        [self onError:e];
    }
    return attachPromise_[@"promise"];
}

- (NSString*) state
{
    switch (currentState_) {
        case Init:      return @"init";
        case Attaching: return @"attaching";
        case Attached:  return @"attached";
        case Detaching: return @"detaching";
        case Detached:  return @"detached";
    }
    assert(0);
}

- (JSValue *) detach
{
    detachPromise_ = [context_ evaluateScript:createPromiseObjectS];
    JSValue *promise = detachPromise_[@"promise"];
    if (currentState_ == Detached) {
        [self.emitFunc callWithArguments:@[self.thiz, @"detached"]];
        [detachPromise_[@"resolve"] callWithArguments:@[]];
        detachPromise_ = nil;
    } else if (currentState_ == Attached) {
        assert(caramlJS_ != nil);
        currentState_ = Detaching;
        [caramlJS_ detach];
    } else {
        [detachPromise_[@"reject"] callWithArguments:@[@"Attach/detach pending"]];
        detachPromise_ = nil;
    }
    return promise;
}

- (void) write:(NSString*) out
{
    if (currentView_ != nil && out != nil) {
        [currentView_.console print:out];
    }
}

#pragma LCCaramlSurface

- (UIView*) getView
{
    return currentView_;
}

- (void) onAttached:(BOOL)fromRestore
{
    currentState_ = Attached;
    
    [process_ async:^(JSContext *context) {
        context.exceptionHandler = ^(JSContext* context, JSValue *exception) {
            self->processedException_ = YES;
            [context[@"console"] invokeMethod:@"error" withArguments:@[exception]];
        };
        if (!fromRestore) {
            if (self->attachPromise_ != nil) {
                [self->attachPromise_[@"resolve"] callWithArguments:@[]];
            }
            [self.emitFunc callWithArguments:@[self.thiz, @"attached"]];
            self->attachPromise_ = nil;
        }
    }];
}

- (void) onDetached
{
    currentState_ = Detached;
    if (currentView_ != nil) [currentView_ detach];
    caramlJS_ = nil;
    
    [process_ async:^(JSContext *context) {
        [self.emitFunc callWithArguments:@[self.thiz, @"detached"]];
        if (self->detachPromise_ != nil) {
            [self->detachPromise_[@"resolve"] callWithArguments:@[]];
        }
        self->detachPromise_ = nil;
    }];
}

- (void) onError:(NSException*) e
{
    if (currentState_ != Init) {
        currentState_ = Detached;
    }
    JSValue *promise = attachPromise_ ? attachPromise_ : detachPromise_;
    [process_ async:^(JSContext *context) {
        if (promise != nil) {
            [promise[@"reject"] callWithArguments:@[e.name]];
        }
        [self.emitFunc callWithArguments:@[self.thiz, @"error", e.name]];
    }];
    attachPromise_ = detachPromise_ = nil;
}

@end
