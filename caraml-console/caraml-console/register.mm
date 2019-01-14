/*
 * Copyright (c) 2019 Eric Lange
 *
 * Distributed under the MIT License.  See LICENSE.md at
 * https://github.com/LiquidPlayer/caraml-console for terms and conditions.
 */
#import <LiquidCore/addon/LCAddOn.h>
#import "ConsoleJS.h"
#include "caraml-console.h"

@interface CaramlConsole : NSObject<LCAddOn>

@end

@implementation CaramlConsole

- (id) init
{
    self = [super init];
    if (self != nil) {
        
    }
    return self;
}

- (void) register:(NSString*) module
{
    assert([@"caramlconsole" isEqualToString:module]);
    register_caramlconsole();
}

- (void) require:(JSValue*) binding service:(LCMicroService *)service
{
    assert(binding != nil);
    assert([binding isObject]);
    
    JSContext *context = [binding context];
    binding[@"newConsole"] = ^(JSValue* opts){
        return [ConsoleJS createConsole:context service:service opts:opts];
    };
}

@end

@interface CaramlConsoleFactory : LCAddOnFactory

@end

@implementation CaramlConsoleFactory

- (id<LCAddOn>)createInstance
{
    return [[CaramlConsole alloc] init];
}

@end

__attribute__((constructor))
static void consoleJSRegistration()
{
    [LCAddOnFactory reigsterAddOnFactory:[[CaramlConsole alloc] init]];
}
