/*
 * Copyright (c) 2018 Eric Lange
 *
 * Distributed under the MIT License.  See LICENSE.md at
 * https://github.com/LiquidPlayer/caraml-console for terms and conditions.
 */
#import <UIKit/UIKit.h>
#import "AnsiConsoleTextView.h"
#import "CommandTextField.h"
#import "ConsoleJS.h"

@interface ConsoleView : UIView <UITextFieldDelegate, CommandTextFieldDelegate, UITextViewDelegate>

@property (weak, nonatomic) IBOutlet AnsiConsoleTextView *console;
@property (weak, nonatomic) IBOutlet CommandTextField *command;
@property (weak, nonatomic) IBOutlet UIButton *upHistory;
@property (weak, nonatomic) IBOutlet UIButton *downHistory;

@property (nonatomic) NSMutableArray* history;
@property (nonatomic) NSInteger item;

@property (strong, nonatomic) ConsoleJS *session;

- (void) detach;

@end
