#import "Segment.h"
#import "RNAnalytics.h"

@implementation Segment

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(getFacebookAdCampaignId:(RCTPromiseResolveBlock)resolve withRejecter:(RCTPromiseRejectBlock)reject)
{
    
    RNAnalytics* rnAnalytics = [RNAnalytics sharedManager];

    resolve(rnAnalytics.facebookAdCampainId);
}

@end
