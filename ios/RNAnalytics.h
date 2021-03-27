#import <foundation/Foundation.h>

@interface RNAnalytics : NSObject {
    NSString *facebookAdCampainId;
}

@property (nonatomic, retain) NSString *facebookAdCampainId;

+ (id)sharedManager;

@end
