#import "RNAnalytics.h"

@implementation RNAnalytics

@synthesize facebookCampaignId;

#pragma mark Singleton Methods

+ (id)sharedManager {
    static RNAnalytics *sharedMyManager = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedMyManager = [[self alloc] init];
    });
    return sharedMyManager;
}

- (id)init {
  if (self = [super init]) {
      facebookCampaignId = nil;
  }
  return self;
}

- (void)dealloc {
  // Should never be called, but just here for clarity really.
}

@end
