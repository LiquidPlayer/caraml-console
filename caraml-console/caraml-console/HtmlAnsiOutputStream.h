/*
 * Copyright (c) 2018 Eric Lange
 *
 * Distributed under the MIT License.  See LICENSE.md at
 * https://github.com/LiquidPlayer/caraml-console for terms and conditions.
 */
#ifndef HtmlAnsiOutputStream_h
#define HtmlAnsiOutputStream_h

#import "AnsiOutputStream.h"

@interface LCHtmlAnsiOutputStream : LCAnsiOutputStream
- (void) flush;
@end

#endif /* HtmlAnsiOutputStream_h */
