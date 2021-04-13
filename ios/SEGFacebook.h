#import <foundation/Foundation.h>

@interface SEGFacebook : NSObject {
    NSString *facebookCampaignId;
}

@property (nonatomic, retain) NSString *facebookCampaignId;

+ (id)sharedManager;

@end
