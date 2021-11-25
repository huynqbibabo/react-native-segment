#import "RNSegment.h"
#import "SEGFacebook.h"
#import <React/RCTBridge.h>

@implementation RNSegment

+(void)initialize {
    [super initialize];
}

RCT_EXPORT_MODULE()

@synthesize bridge = _bridge;

static NSString* singletonJsonConfig = nil;

RCT_EXPORT_METHOD(getFacebookCampaignId:(RCTPromiseResolveBlock)resolve withRejecter:(RCTPromiseRejectBlock)reject)
{
    @try {
        SEGFacebook* rnAnalytics = [SEGFacebook sharedManager];
        resolve(rnAnalytics.facebookCampaignId);
    } @catch (NSException *exception) {
        resolve(NULL);
    }
}


@end
