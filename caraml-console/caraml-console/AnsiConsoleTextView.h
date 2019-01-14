/*
 * Copyright (c) 2018 Eric Lange
 *
 * Distributed under the MIT License.  See LICENSE.md at
 * https://github.com/LiquidPlayer/caraml-console for terms and conditions.
 */
#import <UIKit/UIKit.h>

@protocol LCAnsiConsoleTextViewDelegate <NSObject>
- (void) onTextRefreshed;
@end

@interface LCAnsiConsoleTextView : UITextView
@property (nonatomic, readwrite, weak) NSObject<LCAnsiConsoleTextViewDelegate>* refreshDelegate;

- (void) print:(NSString *)ansiString;
- (void) println:(NSString *)ansiString;

@end
