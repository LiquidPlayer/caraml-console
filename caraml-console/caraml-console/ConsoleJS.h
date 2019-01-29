/*
 * Copyright (c) 2019 Eric Lange
 *
 * Distributed under the MIT License.  See LICENSE.md at
 * https://github.com/LiquidPlayer/caraml-console for terms and conditions.
 */
#import <UIKit/UIKit.h>
#import <LiquidCore/LiquidCore.h>
#import <JavaScriptCore/JavaScriptCore.h>
#import "caraml_core.h"

NS_ASSUME_NONNULL_BEGIN

@protocol ConsoleJSExports<JSExport>

- (JSValue*) attach:(JSValue*)value;
- (NSString*) state;
- (JSValue*) detach;
- (void) write:(NSString*) out;

@end

@interface ConsoleJS : NSObject<ConsoleJSExports, LCCaramlSurface>

@property (nonatomic, strong) UIColor* backgroundColor;
@property (nonatomic, strong) UIColor* textColor;
@property (nonatomic, strong) NSString* textColorString;
@property (nonatomic, copy) NSNumber* fontSize;

- (void) resize:(int)columns rows:(int)rows;
- (void) processCommand:(NSString*)cmd;

+ (JSValue *) createConsole:(JSContext*)context service:(LCMicroService*)service opts:(JSValue*)opts;

@end

NS_ASSUME_NONNULL_END
