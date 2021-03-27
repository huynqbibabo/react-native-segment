#import <React/RCTBridgeModule.h>

@interface Segment : NSObject <RCTBridgeModule>
@property (nonatomic, weak) NSString *FacebookAdCampainId;
- (void)setFacebookAdCampaignId:(NSString *)id;
@end
