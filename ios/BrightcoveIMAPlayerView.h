#import <UIKit/UIKit.h>
#import <BrightcovePlayerSDK/BCOVPlayerSDKManager.h>
#import <BrightcovePlayerSDK/BCOVPlaybackController.h>
#import <BrightcovePlayerSDK/BCOVPlaybackService.h>
#import <BrightcovePlayerSDK/BCOVPUIPlayerView.h>
#import <BrightcovePlayerSDK/BCOVBasicSessionProvider.h>
#import <BrightcovePlayerSDK/BCOVPlayerSDKManager.h>
#import <BrightcovePlayerSDK/BCOVPUIBasicControlView.h>
#import <BrightcovePlayerSDK/BCOVPlaybackSession.h>
#import <BrightcovePlayerSDK/BCOVPUISlider.h>
#import <BrightcovePlayerSDK/BCOVOfflineVideoManager.h>
#import <BrightcoveSSAI/BrightcoveSSAI.h>
#import <React/RCTBridge.h>
#import <React/UIView+React.h>

@interface BrightcoveIMAPlayerView : UIView

@property (nonatomic) BCOVPlaybackService *playbackService;
@property (nonatomic) id<BCOVPlaybackController> playbackController;
@property (nonatomic) id<BCOVPlaybackSession> playbackSession;
@property (nonatomic) BCOVPUIPlayerView *playerView;
@property (nonatomic) BOOL playing;
@property (nonatomic) BOOL adsPlaying;
@property (nonatomic) BOOL autoPlay;
@property (nonatomic) BOOL isAudioOnly;
@property (nonatomic) BOOL disableDefaultControl;
@property (nonatomic) BOOL disablePictureInPicture;
@property (nonatomic) float lastBufferProgress;
@property (nonatomic) float targetVolume;
@property (nonatomic) float targetBitRate;
@property (nonatomic) int targetAdVideoLoadTimeout;
@property (nonatomic) float targetPlaybackRate;
@property (nonatomic) BOOL playbackServiceDirty;
@property (nonatomic) NSTimeInterval currentVideoDuration;
@property (nonatomic) BOOL inViewPort;

@property (nonatomic, copy) NSDictionary *settings;
@property (nonatomic, copy) NSString *videoId;
@property (nonatomic, copy) NSString *accountId;
@property (nonatomic, copy) NSString *policyKey;
@property (nonatomic, copy) NSString *adConfigId;
@property (nonatomic, copy) RCTDirectEventBlock onAdsLoaded;
@property (nonatomic, copy) RCTDirectEventBlock onReady;
@property (nonatomic, copy) RCTDirectEventBlock onPlay;
@property (nonatomic, copy) RCTDirectEventBlock onPause;
@property (nonatomic, copy) RCTDirectEventBlock onEnd;
@property (nonatomic, copy) RCTDirectEventBlock onProgress;
@property (nonatomic, copy) RCTDirectEventBlock onChangeDuration;
@property (nonatomic, copy) RCTDirectEventBlock onUpdateBufferProgress;
@property (nonatomic, copy) RCTDirectEventBlock onEnterFullscreen;
@property (nonatomic, copy) RCTDirectEventBlock onExitFullscreen;
@property (nonatomic, copy) RCTDirectEventBlock onStartPictureInPicture;
@property (nonatomic, copy) RCTDirectEventBlock onStopPictureInPicture;
@property (nonatomic, copy) RCTDirectEventBlock onMaximizePictureInPicture;

-(void) setupWithSettings:(NSString*)settings;
-(void) seekTo:(NSNumber *)time;
-(void) toggleFullscreen:(BOOL)isFullscreen;
-(void) toggleInViewPort:(BOOL)inViewPort;
-(void) pause;
-(void) play;
-(void) stopPlayback;
-(void) dispose;
-(void) enterPictureInPicture;
-(void) exitPictureInPicture;

@end
