#import <foundation/Foundation.h>

@interface RNAnalytics : NSObject {
    NSString *facebookCampaignId;
}

@property (nonatomic, retain) NSString *facebookCampaignId;

+ (id)sharedManager;

@end
