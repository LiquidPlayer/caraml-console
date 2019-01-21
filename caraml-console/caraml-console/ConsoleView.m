/*
 * Copyright (c) 2018 Eric Lange
 *
 * Distributed under the MIT License.  See LICENSE.md at
 * https://github.com/LiquidPlayer/caraml-console for terms and conditions.
 */
#import "ConsoleView.h"

@implementation LCConsoleView

- (id) initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        [self xibSetup];
        [self initView];
    }
    return self;
}

- (id) initWithCoder:(NSCoder *)aDecoder
{
    self = [super initWithCoder:aDecoder];
    if (self) {
        [self xibSetup];
        [self initView];
    }
    return self;
}

- (void) xibSetup
{
    UIView* view = [self loadViewFromNib];
    view.frame = self.bounds;
    view.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    [self addSubview:view];
}

- (UIView *)loadViewFromNib
{
    NSBundle *bundle = [NSBundle bundleForClass:[self class]];
    UINib *nib = [UINib nibWithNibName:@"ConsoleView" bundle:bundle];
    UIView *view = [nib instantiateWithOwner:self options:nil][0];
    
    return view;
}

- (void) initView
{
    self.downHistory.enabled = NO;
    self.upHistory.enabled = NO;
    self.command.selected = YES;
}

- (void) layoutSubviews
{
    [super layoutSubviews];
    [self.command setDelegate:self];
    [self.command setCommandDelegate:self];
    [self.command becomeFirstResponder];
    [self.console setDelegate:self];
    if (@available(iOS 11.0, *)) {
        self.command.smartQuotesType = UITextSmartQuotesTypeNo;
    }
    self.command.autocorrectionType = UITextAutocorrectionTypeNo;
    
    [self.console setBackgroundColor:self.session.backgroundColor];
    [self.console setTextColorString:self.session.textColorString];
    [self.console setFontSize:self.session.fontSize];
    [self.command setTextColor:self.session.textColor];
    self.backgroundColor = self.session.backgroundColor;
    
    UITextInputAssistantItem* item = [self.command inputAssistantItem];
    item.leadingBarButtonGroups = @[];
    item.trailingBarButtonGroups = @[];
    
    NSString *someString = @"1234567890\n1234567890";
    UIFont *font = [UIFont fontWithName:@"Menlo" size:self.session.fontSize.floatValue];
    CGSize stringBoundingBox = [someString sizeWithAttributes:@{ NSFontAttributeName : font }];
    [self.console setFont:font];
    [self.command setFont:font];

    // FIXME: This doesn't quite work right.  It always seems off by a character or two.
    double heightChar = stringBoundingBox.height / 2;
    double widthChar = stringBoundingBox.width / 10;
    
    double heightBox = self.console.bounds.size.height - self.console.layoutMargins.bottom - self.console.layoutMargins.top;
    double widthBox = self.console.bounds.size.width - self.console.layoutMargins.left - self.console.layoutMargins.right;

    int numCols = floor(widthBox / widthChar);
    int numLines = floor(heightBox / heightChar);
    if (self.session != nil) {
        [self.session resize:numCols rows:numLines];
    }
}

- (void) detach
{
    dispatch_async(dispatch_get_main_queue(), ^{
        self.command.enabled = NO;
        self.upHistory.enabled = NO;
        self.downHistory.enabled = NO;
    });
}

- (void) textViewDidChangeSelection:(UITextView *)textView
{
    NSRange bottom = NSMakeRange(self.console.text.length -1, 1);
    [self.console scrollRangeToVisible:bottom];
}

- (void) onUpArrow
{
    if (self.upHistory.enabled) {
        self.item --;
        self.command.text = [self.history objectAtIndex:self.item];
        if (self.item == 0) {
            self.upHistory.enabled = NO;
        }
        self.downHistory.enabled = YES;
        self.command.selected = YES;
    }
}

- (void) onDownArrow
{
    if (self.downHistory.enabled) {
        self.item ++;
        if (self.item >= self.history.count) {
            self.downHistory.enabled = NO;
            self.command.text = @"";
        } else {
            self.command.text = [self.history objectAtIndex:self.item];
        }
        self.upHistory.enabled = YES;
        self.command.selected = YES;
    }
}

- (void) onReturnKey
{
    NSString *cmd = [self.command.text stringByTrimmingCharactersInSet: [NSCharacterSet whitespaceCharacterSet]];
    if (cmd.length > 0) {
        NSString *display = [NSString stringWithFormat:@"%C[1m> %@", 0x001B, cmd];
        
        [self.console println:display];
        [self.command setText:@""];
        if (self.history == nil) {
            self.history = [[NSMutableArray alloc] init];
        }
        [self.history addObject:cmd];
        self.item = self.history.count;
        self.upHistory.enabled = YES;
        self.downHistory.enabled = NO;
        
        if (self.session != nil) {
            [self.session processCommand:cmd];
        }
        self.command.selected = YES;
    }
}

#pragma UITextFieldDelegate

- (BOOL)textFieldShouldReturn:(UITextField *)textField {
    if (textField == self.command) {
        [textField resignFirstResponder];
        [self onReturnKey];
        return NO;
    }
    return YES;
}

#pragma IBActions

- (IBAction)onUpHistoryTouch:(id)sender
{
    [self onUpArrow];
}

- (IBAction)onDownHistoryTouch:(id)sender
{
    [self onDownArrow];
}

@end
