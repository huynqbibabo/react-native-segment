#import <React/RCTBridgeModule.h>

@interface Segment : NSObject <RCTBridgeModule>

+(void)addIntegration:(id)factory;

@end
