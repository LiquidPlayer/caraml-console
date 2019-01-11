//
//  ConsoleJS.h
//  caraml-console
//
//  Created by Eric Lange on 1/10/19.
//  Copyright © 2019 LiquidPlayer. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <LiquidCore/LiquidCore.h>
#import <JavaScriptCore/JavaScriptCore.h>
#import <caraml_core/caraml_core.h>

NS_ASSUME_NONNULL_BEGIN

@protocol ConsoleJSExports<JSExport>

- (JSValue*) attach:(JSValue*)value;
- (NSString*) state;
- (JSValue*) detach;
- (void) write:(NSString*) out;

@end

@interface ConsoleJS : JSValue<JSExport, LCCaramlSurface>

@property (nonatomic, strong) UIColor* backgroundColor;
@property (nonatomic, strong) UIColor* textColor;
@property (nonatomic, copy) NSNumber* fontSize;

- (void) resize:(int)columns rows:(int)rows;
- (void) processCommand:(NSString*)cmd;

+ (JSValue *) createConsole:(JSContext*)context service:(LCMicroService*)service opts:(JSValue*)opts;

@end

NS_ASSUME_NONNULL_END
