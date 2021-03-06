/*
 * Copyright (c) 2018 Eric Lange
 *
 * Distributed under the MIT License.  See LICENSE.md at
 * https://github.com/LiquidPlayer/caraml-console for terms and conditions.
 */
#import "AnsiConsoleTextView.h"
#import "AnsiConsoleOutputStream.h"

@interface LCAnsiConsoleTextView() <LCAnsiConsoleOutputStreamDelegate>
@property (readonly, strong, nonatomic) LCAnsiConsoleOutputStream* stream;
@property (readwrite, assign, atomic) BOOL awaitingRefresh;
@end

@implementation LCAnsiConsoleTextView

- (id) initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        _awaitingRefresh = NO;
        _stream = [[LCAnsiConsoleOutputStream alloc] initWithInitialDisplayText:self.attributedText delegate:self];
    }
    return self;
}

- (id) initWithCoder:(NSCoder *)aDecoder
{
    self = [super initWithCoder:aDecoder];
    if (self) {
        _awaitingRefresh = NO;
        _stream = [[LCAnsiConsoleOutputStream alloc] initWithInitialDisplayText:self.attributedText delegate:self];
    }
    return self;
}

- (void) setTextColorString:(NSString *)textColorString
{
    [self.stream setTextColor:textColorString];
    _textColorString = textColorString;
}

- (void) setFontSize:(NSNumber *)fontSize
{
    [self.stream setFontSize:fontSize];
    _fontSize = fontSize;
}

- (void) onRefresh
{
    if (!self.awaitingRefresh) {
        self.awaitingRefresh = YES;
        dispatch_async(dispatch_get_main_queue(), ^{
            self.awaitingRefresh = NO;
            [self setAttributedText:self.stream.displayText];
            if (self.refreshDelegate != nil) {
                [self.refreshDelegate onTextRefreshed];
            }
        });
    }
}

- (void) print:(NSString *)ansiString
{
    [self.stream print:ansiString];
}

- (void) println:(NSString *)ansiString
{
    [self print:[NSString stringWithFormat:@"%@\n", ansiString]];
}

/*
// Only override drawRect: if you perform custom drawing.
// An empty implementation adversely affects performance during animation.
- (void)drawRect:(CGRect)rect {
    // Drawing code
}
*/

@end
