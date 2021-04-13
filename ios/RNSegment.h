#import <React/RCTBridgeModule.h>

@interface RNSegment : NSObject <RCTBridgeModule>

+(void)addIntegration:(id)factory;

@end
