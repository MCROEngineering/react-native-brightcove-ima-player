#import "BrightcoveIMAPlayerPosterViewManager.h"
#import "BrightcoveIMAPlayerPosterView.h"
#import <React/RCTUIManager.h>

@implementation BrightcoveIMAPlayerPosterViewManager

RCT_EXPORT_MODULE();

@synthesize bridge = _bridge;

- (UIView *)view {
    return [[BrightcoveIMAPlayerPosterView alloc] init];
}

RCT_EXPORT_VIEW_PROPERTY(policyKey, NSString);
RCT_EXPORT_VIEW_PROPERTY(accountId, NSString);
RCT_EXPORT_VIEW_PROPERTY(videoId, NSString);
RCT_EXPORT_VIEW_PROPERTY(resizeMode, NSString);

@end
