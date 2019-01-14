/*
 * Copyright (c) 2018 Eric Lange
 *
 * Distributed under the MIT License.  See LICENSE.md at
 * https://github.com/LiquidPlayer/caraml-console for terms and conditions.
 */
#import <UIKit/UIKit.h>
#import "HtmlAnsiOutputStream.h"

#ifndef AnsiConsoleOutputStream_h
#define AnsiConsoleOutputStream_h

@protocol LCAnsiConsoleOutputStreamDelegate
- (void) onRefresh;
@end

@interface LCAnsiConsoleOutputStream : HtmlAnsiOutputStream
@property (strong, atomic) NSAttributedString *displayText;

- (id) initWithInitialDisplayText:(NSAttributedString *)text delegate:(NSObject<LCAnsiConsoleOutputStreamDelegate>*)delegate;
- (void) print:(NSString*)ansiString;
@end

#endif
