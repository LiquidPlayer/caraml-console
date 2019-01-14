/*
 * Copyright (c) 2018 Eric Lange
 *
 * Distributed under the MIT License.  See LICENSE.md at
 * https://github.com/LiquidPlayer/caraml-console for terms and conditions.
 */
#import <UIKit/UIKit.h>

@protocol LCCommandTextFieldDelegate <NSObject>
- (void) onUpArrow;
- (void) onDownArrow;
@end

@interface LCCommandTextField : UITextField
@property (nonatomic, weak, readwrite) id<LCCommandTextFieldDelegate> commandDelegate;
@end
