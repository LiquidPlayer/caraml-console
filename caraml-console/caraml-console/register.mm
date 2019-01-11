//
//  register.mm
//  caraml-console
//
//  Created by Eric Lange on 1/10/19.
//  Copyright © 2019 LiquidPlayer. All rights reserved.
//

#import <LiquidCore/addon/LCAddOn.h>
#import "ConsoleJS.h"
#include "caraml-console.h"

@interface CaramlConsole : NSObject<LCAddOn>

@end

@implementation CaramlConsole

- (id) init
{
    self = [super init];
    if (self != nil) {
        
    }
    return self;
}

- (void) register:(NSString*) module
{
    assert([@"caramlconsole" isEqualToString:module]);
    register_caramlconsole();
}

- (void) require:(JSValue*) binding service:(LCMicroService *)service
{
    assert(binding != nil);
    assert([binding isObject]);
    
    JSContext *context = [binding context];
    binding[@"newConsole"] = ^(JSValue* opts){
        return [ConsoleJS createConsole:context service:service opts:opts];
    };
}

@end

@interface CaramlConsoleFactory : LCAddOnFactory

@end

@implementation CaramlConsoleFactory

- (id<LCAddOn>)createInstance
{
    return [[CaramlConsole alloc] init];
}

@end
