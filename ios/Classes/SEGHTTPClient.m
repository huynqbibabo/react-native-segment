#import "SEGHTTPClient.h"
#import "NSData+SEGGZIP.h"
#import "SEGAnalyticsUtils.h"
#import "SEGUtils.h"

#define SEGMENT_CDN_BASE [NSURL URLWithString:@"https://segment.bibabo.vn/api/v1"]

static const NSUInteger kMaxBatchSize = 475000; // 475KB

NSString * const kSegmentAPIBaseHost = @"https://segment.bibabo.vn/api/v1";

@implementation SEGHTTPClient

+ (NSMutableURLRequest * (^)(NSURL *))defaultRequestFactory
{
    return ^(NSURL *url) {
        return [NSMutableURLRequest requestWithURL:url
                                       cachePolicy:NSURLRequestUseProtocolCachePolicy
                                   timeoutInterval:10.0];
    };
}

+ (NSString *)authorizationHeader:(NSString *)writeKey
{
    NSString *rawHeader = [writeKey stringByAppendingString:@":"];
    NSData *userPasswordData = [rawHeader dataUsingEncoding:NSUTF8StringEncoding];
    return [userPasswordData base64EncodedStringWithOptions:0];
}


- (instancetype)initWithRequestFactory:(SEGRequestFactory)requestFactory
{
    if (self = [self init]) {
        self.requestFactory = [SEGHTTPClient defaultRequestFactory];
        
        _sessionsByWriteKey = [NSMutableDictionary dictionary];
        NSURLSessionConfiguration *config = [NSURLSessionConfiguration defaultSessionConfiguration];
        config.HTTPAdditionalHeaders = @{
            @"Accept-Encoding" : @"gzip",
            @"User-Agent" : [NSString stringWithFormat:@"analytics-ios/%@", [SEGAnalytics version]],
        };
        _genericSession = [NSURLSession sessionWithConfiguration:config];
    }
    return self;
}

- (NSURLSession *)sessionForWriteKey:(NSString *)writeKey
{
    NSURLSession *session = self.sessionsByWriteKey[writeKey];
    if (!session) {
        NSURLSessionConfiguration *config = [NSURLSessionConfiguration defaultSessionConfiguration];
        config.HTTPAdditionalHeaders = @{
            @"Authorization": [@"Bearer " stringByAppendingString:[[self class] authorizationHeader:writeKey]],
            @"Content-Type": @"application/json"
        };
        session = [NSURLSession sessionWithConfiguration:config delegate:self.httpSessionDelegate delegateQueue:NULL];
        self.sessionsByWriteKey[writeKey] = session;
    }
    return session;
}

- (void)dealloc
{
    for (NSURLSession *session in self.sessionsByWriteKey.allValues) {
        [session finishTasksAndInvalidate];
    }
    [self.genericSession finishTasksAndInvalidate];
}


- (nullable NSURLSessionDataTask *)upload:(NSDictionary *)batch forWriteKey:(NSString *)writeKey completionHandler:(void (^)(BOOL retry))completionHandler
{
    //    batch = SEGCoerceDictionary(batch);
    NSURLSession *session = [self sessionForWriteKey:writeKey];

    NSURL *url = [[SEGUtils getAPIHostURL] URLByAppendingPathComponent:@"log-event"];
    NSMutableURLRequest *request = self.requestFactory(url);

    NSError *error = nil;
    NSException *exception = nil;
    NSData *payload = nil;
    @try {
        payload = [NSJSONSerialization dataWithJSONObject:batch options:0 error:&error];
    }
    @catch (NSException *exc) {
        exception = exc;
    }
    if (error || exception) {
        SEGLog(@"Error serializing JSON for batch upload %@", error);
        completionHandler(NO); // Don't retry this batch.
        return nil;
    }
    if (payload.length >= kMaxBatchSize) {
        SEGLog(@"Payload exceeded the limit of %luKB per batch", kMaxBatchSize / 1000);
        completionHandler(NO);
        return nil;
    }
    //    NSData *gzippedPayload = [payload seg_gzippedData];
    
    
    //    dispatch_semaphore_t sema = dispatch_semaphore_create(0);
    
    //    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:[NSURL URLWithString:url]
    //      cachePolicy:NSURLRequestUseProtocolCachePolicy
    //      timeoutInterval:10.0];
    NSDictionary *headers = @{
        @"Authorization": [@"Bearer " stringByAppendingString:[[self class] authorizationHeader:writeKey]],
        @"Content-Type": @"application/json"
    };
    
    [request setAllHTTPHeaderFields:headers];
    NSData *postData = [[NSData alloc] initWithData:payload];
    [request setHTTPBody:postData];
    
    [request setHTTPMethod:@"POST"];
    
    NSURLSessionDataTask *task = [session dataTaskWithRequest:request completionHandler:^(NSData *_Nullable data, NSURLResponse *_Nullable response, NSError *_Nullable error) {
        if (error) {
            // Network error. Retry.
            SEGLog(@"Error uploading request %@.", error);
            completionHandler(YES);
            return;
        }
        
        NSInteger code = ((NSHTTPURLResponse *)response).statusCode;
        if (code < 300) {
            // 2xx response codes. Don't retry.
            completionHandler(NO);
            return;
        }
        if (code < 400) {
            // 3xx response codes. Retry.
            SEGLog(@"Server responded with unexpected HTTP code %d.", code);
            completionHandler(YES);
            return;
        }
        if (code == 429) {
            // 429 response codes. Retry.
            SEGLog(@"Server limited client with response code %d.", code);
            completionHandler(YES);
            return;
        }
        if (code < 500) {
            // non-429 4xx response codes. Don't retry.
            SEGLog(@"Server rejected payload with HTTP code %d.", code);
            completionHandler(NO);
            return;
        }
        
        // 5xx response codes. Retry.
        SEGLog(@"Server error with HTTP code %d.", code);
        completionHandler(YES);
    }];
    [task resume];
    return task;
}

@end
