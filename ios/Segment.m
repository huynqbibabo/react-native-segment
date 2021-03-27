#import "Segment.h"

@implementation Segment

RCT_EXPORT_MODULE()

// Example method
// See // https://reactnative.dev/docs/native-modules-ios
RCT_EXPORT_METHOD(getFacebookAdCampaignId:(RCTPromiseResolveBlock)resolve withRejecter:(RCTPromiseRejectBlock)reject)
{
  resolve(_FacebookAdCampainId);
}

-(void) setFacebookAdCampaignId:(NSString* )id {
    _FacebookAdCampainId = id;
}

@end
