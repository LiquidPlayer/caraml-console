//
//  UIColor+String.h
//  caraml-console
//
//  Created by Eric Lange on 1/11/19.
//  Copyright © 2019 LiquidPlayer. All rights reserved.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface UIColor(String)

+ (UIColor *) colorWithHexString: (NSString *) hexString;
+ (UIColor * _Nullable) colorWithString: (NSString *)string;
+ (CGFloat) colorComponentFrom: (NSString *) string start: (NSUInteger) start length: (NSUInteger) length;

@end

NS_ASSUME_NONNULL_END
