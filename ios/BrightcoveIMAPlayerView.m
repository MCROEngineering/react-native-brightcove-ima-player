#import "BrightcoveIMAPlayerView.h"
#import <React/RCTUtils.h>
#import <AVKit/AVKit.h>

@interface BrightcoveIMAPlayerView () <BCOVPlaybackControllerDelegate, BCOVPUIPlayerViewDelegate, BCOVPlaybackControllerAdsDelegate>

@end

@implementation BrightcoveIMAPlayerView

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        //        [self setup];
    }
    return self;
}

- (id) init
{
    self = [super init];
    if (!self) return nil;
    
    for (NSString *name in @[
        UIApplicationDidBecomeActiveNotification,
        UIApplicationDidEnterBackgroundNotification
    ]) {
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(handleAppStateDidChange:)
                                                     name:name
                                                   object:nil];
    }
    
    return self;
}

- (void)setupWithSettings:(NSDictionary*)settings {
    _adConfigId = [settings objectForKey:@"adConfigId"];
    
    // By pass mute button
    NSError *error = nil;
    [[AVAudioSession sharedInstance] setCategory:AVAudioSessionCategoryPlayback error:&error];
    
    _targetVolume = 1.0;
    _inViewPort = YES;
    
    [self setupPlaybackController:settings];
    [self setupPlayerView];
}

- (void)setupPlaybackController:(NSDictionary*)settings
{
    // Do any additional setup after loading the view, typically from a nib.
    BCOVPlayerSDKManager *manager = [BCOVPlayerSDKManager sharedManager];
    
    if (!_adConfigId) {
        _playbackController = [manager createPlaybackController];
    } else {
        BCOVSSAIAdComponentDisplayContainer *adComponentDisplayContainer = [[BCOVSSAIAdComponentDisplayContainer alloc] initWithCompanionSlots:@[]];
        
        _playbackController = [manager createSSAIPlaybackController];
        
        [_playbackController addSessionConsumer:adComponentDisplayContainer];
        
    }
    
    self.playbackController.delegate = self;
    
    BOOL autoPlay = [settings objectForKey:@"autoPlay"] != nil ? [[settings objectForKey:@"autoPlay"] boolValue] : YES;
    BOOL allowsExternalPlayback = [settings objectForKey:@"allowsExternalPlayback"] != nil ? [[settings objectForKey:@"allowsExternalPlayback"] boolValue] : YES;
    self.playbackController.autoPlay = autoPlay;
    _playbackController.allowsExternalPlayback = allowsExternalPlayback;
    
    _autoPlay = autoPlay;
}

- (void)setupPlayerView
{
    BCOVPUIBasicControlView *controlView = [BCOVPUIBasicControlView basicControlViewWithVODLayout];
    BCOVPUIPlayerViewOptions *options;
    if (!_disableDefaultControl) {
        options = [[BCOVPUIPlayerViewOptions alloc] init];
        options.presentingViewController = RCTPresentedViewController();
        if (_disablePictureInPicture == false) {
            options.showPictureInPictureButton = YES;
        }
    }
    
    self.playerView = [[BCOVPUIPlayerView alloc] initWithPlaybackController:self.playbackController options:options controlsView:controlView];
    
    if (_disableDefaultControl == true) {
        _playerView.controlsView.hidden = true;
    }
    
    self.playerView.delegate = self;
    
    [self addSubview:self.playerView];
    self.playerView.autoresizingMask = UIViewAutoresizingFlexibleHeight | UIViewAutoresizingFlexibleWidth;
    self.playerView.backgroundColor = UIColor.blackColor;
}

- (void)setupService {
    if ((!_playbackService || _playbackServiceDirty) && _accountId && _policyKey) {
        _playbackServiceDirty = NO;
        _playbackService = [[BCOVPlaybackService alloc] initWithAccountId:_accountId policyKey:_policyKey];
    }
}

- (void)loadMovie {
    if (!_playbackService) return;
    if (_videoId) {
        NSMutableDictionary *parameters = [[NSMutableDictionary alloc] init];
        if (_adConfigId) {
            parameters[kBCOVPlaybackServiceParamaterKeyAdConfigId] = _adConfigId;
        }
        NSDictionary *configuration = @{kBCOVPlaybackServiceConfigurationKeyAssetID:_videoId};
        [_playbackService findVideoWithConfiguration:configuration queryParameters:parameters completion:^(BCOVVideo *video, NSDictionary *jsonResponse, NSError *error) {
            if (video != nil) {
                [self.playbackController setVideos: @[ video ]];
            }
        }];
    }
}

- (void)setVideoId:(NSString *)videoId {
    _videoId = videoId;
    [self setupService];
    [self loadMovie];
}

- (void)setAccountId:(NSString *)accountId {
    _accountId = accountId;
    _playbackServiceDirty = YES;
    [self setupService];
    [self loadMovie];
}

- (void)setPolicyKey:(NSString *)policyKey {
    _policyKey = policyKey;
    _playbackServiceDirty = YES;
    [self setupService];
    [self loadMovie];
}

- (void)setAutoPlay:(BOOL)autoPlay {
    _autoPlay = autoPlay;
}

- (void)setPlay:(BOOL)play {
    if (_playing == play) return;
    if (play) {
        [_playbackController play];
    } else {
        [_playbackController pause];
    }
}

- (void)setFullscreen:(BOOL)fullscreen {
    if (fullscreen) {
        [_playerView performScreenTransitionWithScreenMode:BCOVPUIScreenModeFull];
    } else {
        [_playerView performScreenTransitionWithScreenMode:BCOVPUIScreenModeNormal];
    }
}

- (void)setVolume:(NSNumber*)volume {
    _targetVolume = volume.doubleValue;
    [self refreshVolume];
}

- (void)setBitRate:(NSNumber*)bitRate {
    _targetBitRate = bitRate.doubleValue;
    [self refreshBitRate];
}

- (void)setAdVideoLoadTimeout:(NSNumber*)adVideoLoadTimeout {
    _targetAdVideoLoadTimeout = adVideoLoadTimeout.intValue / 1000;
    _playbackServiceDirty = YES;
    [self setupService];
    [self loadMovie];
}

- (void)setPlaybackRate:(NSNumber*)playbackRate {
    _targetPlaybackRate = playbackRate.doubleValue;
    if (_playing) {
        [self refreshPlaybackRate];
    }
}

- (void)refreshVolume {
    if (!_playbackSession) return;
    _playbackSession.player.volume = _targetVolume;
}

- (void)refreshBitRate {
    if (!_playbackSession) return;
    AVPlayerItem *item = _playbackSession.player.currentItem;
    if (!item) return;
    item.preferredPeakBitRate = _targetBitRate;
}

- (void)refreshPlaybackRate {
    if (!_playbackSession || !_targetPlaybackRate) return;
    _playbackSession.player.rate = _targetPlaybackRate;
}

- (void)setDisableDefaultControl:(BOOL)disable {
    _disableDefaultControl = disable;
    _playerView.controlsView.hidden = disable;
}

- (void)setDisablePictureInPicture:(BOOL)disable {
    _disablePictureInPicture = disable;
}

- (void)seekTo:(NSNumber *)time {
    [_playbackController seekToTime:CMTimeMakeWithSeconds([time floatValue], NSEC_PER_SEC) completionHandler:^(BOOL finished) {
    }];
}

-(void) toggleFullscreen:(BOOL)isFullscreen {
    if (isFullscreen) {
        [_playerView performScreenTransitionWithScreenMode:BCOVPUIScreenModeFull];
    } else {
        [_playerView performScreenTransitionWithScreenMode:BCOVPUIScreenModeNormal];
    }
}

-(void) toggleInViewPort:(BOOL)inViewPort {
    if (inViewPort) {
        _inViewPort = YES;
    } else {
        _inViewPort = NO;
        [self.playbackController pauseAd];
        [self.playbackController pause];
    }
}

-(void) pause {
    if (self.playbackController) {
        if (_adsPlaying) {
            [self.playbackController pauseAd];
        }
        [self.playbackController pause];
    }
}

-(void) play {
    if (self.playbackController) {
        if (_adsPlaying) {
            [self.playbackController resumeAd];
            //[self.playbackController pause];
        } else {
            // if ad hasnt started, this will kick it off
            [self.playbackController play];
        }
    }
}

-(void) stopPlayback {
    if (self.playbackController) {
        if (_adsPlaying) {
            [self.playbackController pauseAd];
        }
        [self.playbackController pause];
    }
}

-(void)dispose {
    [self.playbackController setVideos:@[]];
    self.playbackController = nil;
}

-(void)enterPictureInPicture {
    [self.playbackController.pictureInPictureController startPictureInPicture];
}

-(void)exitPictureInPicture {
    [self.playbackController.pictureInPictureController stopPictureInPicture];
}

- (void)handleAppStateDidChange:(NSNotification *)notification
{
    if ([notification.name isEqualToString:UIApplicationDidEnterBackgroundNotification]) {
    }
    
    if ([notification.name isEqualToString:UIApplicationDidBecomeActiveNotification]) {
    }
}

#pragma mark - BCOVPlaybackControllerBasicDelegate methods

- (void)playbackController:(id<BCOVPlaybackController>)controller playbackSession:(id<BCOVPlaybackSession>)session didReceiveLifecycleEvent:(BCOVPlaybackSessionLifecycleEvent *)lifecycleEvent {
    
    // NSLog(@"BC - DEBUG eventType: %@", lifecycleEvent.eventType);
    
    if (lifecycleEvent.eventType == kBCOVPlaybackSessionLifecycleEventPlaybackBufferEmpty ||
        lifecycleEvent.eventType == kBCOVPlaybackSessionLifecycleEventFail ||
        lifecycleEvent.eventType == kBCOVPlaybackSessionLifecycleEventError ||
        lifecycleEvent.eventType == kBCOVPlaybackSessionLifecycleEventTerminate) {
        _playbackSession = nil;
        return;
    }
    
    _playbackSession = session;
    if (lifecycleEvent.eventType == kBCOVPlaybackSessionLifecycleEventReady) {
        [self refreshVolume];
        [self refreshBitRate];
        if (self.onReady) {
            self.onReady(@{});
        }
        // disabling this due to video blip before pre-roll
        if (_autoPlay) {
            [_playbackController play];
        }
    } else if (lifecycleEvent.eventType == kBCOVPlaybackSessionLifecycleEventPlay) {
        _playing = true;
        [self refreshPlaybackRate];
        if (self.onPlay) {
            self.onPlay(@{});
        }
    } else if (lifecycleEvent.eventType == kBCOVPlaybackSessionLifecycleEventPause) {
        _playing = false;
        if (self.currentVideoDuration) {
            int curDur = (int)ceil(self.currentVideoDuration);
            int curTime = (int)ceil(CMTimeGetSeconds([session.player currentTime]));
            if (curDur <= curTime) {
                if (self.onEnd) {
                    self.onEnd(@{});
                }
            }
        }
        
        if (self.onPause) {
            self.onPause(@{});
        }
    } else if (lifecycleEvent.eventType == kBCOVPlaybackSessionLifecycleEventAdProgress) {
        // catches scroll away before ads start bug
        if (!_inViewPort) {
            [self.playbackController pauseAd];
            [self.playbackController pause];
        }
    }
}

- (void)playbackController:(id<BCOVPlaybackController>)controller playbackSession:(id<BCOVPlaybackSession>)session didChangeDuration:(NSTimeInterval)duration {
    self.currentVideoDuration = duration;
    if (self.onChangeDuration) {
        self.onChangeDuration(@{
            @"duration": @(duration)
        });
    }
}

-(void)playbackController:(id<BCOVPlaybackController>)controller playbackSession:(id<BCOVPlaybackSession>)session didProgressTo:(NSTimeInterval)progress {
    if (self.onProgress && progress > 0 && progress != INFINITY) {
        self.onProgress(@{
            @"currentTime": @(progress)
        });
    }
    float bufferProgress = _playerView.controlsView.progressSlider.bufferProgress;
    if (_lastBufferProgress != bufferProgress) {
        _lastBufferProgress = bufferProgress;
        if (self.onUpdateBufferProgress) {
            self.onUpdateBufferProgress(@{
                @"bufferProgress": @(bufferProgress),
            });
        }
    }
}

-(void)playerView:(BCOVPUIPlayerView *)playerView didTransitionToScreenMode:(BCOVPUIScreenMode)screenMode {
    if (screenMode == BCOVPUIScreenModeNormal) {
        // if controls are disabled, disable player controls on normal mode
        if (_disableDefaultControl == true) {
            _playerView.controlsView.hidden = true;
        }
        if (self.onExitFullscreen) {
            self.onExitFullscreen(@{});
        }
    } else if (screenMode == BCOVPUIScreenModeFull) {
        // enable player controls on fullscreen mode
        if (_disableDefaultControl == true) {
            _playerView.controlsView.hidden = false;
        }
        if (self.onEnterFullscreen) {
            self.onEnterFullscreen(@{});
        }
    }
}

#pragma mark - BCOVPlaybackControllerAdsDelegate methods

- (void)playbackController:(id<BCOVPlaybackController>)controller playbackSession:(id<BCOVPlaybackSession>)session didEnterAdSequence:(BCOVAdSequence *)adSequence {
    if (!_inViewPort) {
        [self.playbackController pauseAd];
    }
}

- (void)playbackController:(id<BCOVPlaybackController>)controller playbackSession:(id<BCOVPlaybackSession>)session didExitAdSequence:(BCOVAdSequence *)adSequence {
    //    if (_inViewPort) {
    //        [self.playbackController play];
    //    }
}

- (void)playbackController:(id<BCOVPlaybackController>)controller playbackSession:(id<BCOVPlaybackSession>)session didEnterAd:(BCOVAd *)ad {
    //    if (!_inViewPort) {
    //        [self.playbackController pauseAd];
    //    }
    //    [self.playbackController pause];
}

- (void)playbackController:(id<BCOVPlaybackController>)controller playbackSession:(id<BCOVPlaybackSession>)session didExitAd:(BCOVAd *)ad {
    //    if (_inViewPort) {
    //        [self.playbackController play];
    //    }
}

- (void)playbackController:(id<BCOVPlaybackController>)controller playbackSession:(id<BCOVPlaybackSession>)session ad:(BCOVAd *)ad didProgressTo:(NSTimeInterval)progress {
    //    if (_playing) {
    //        [self.playbackController pause];
    //    }
}

#pragma mark - BCOVPUIPlayerViewDelegate Methods

- (void)pictureInPictureControllerDidStartPictureInPicture:(AVPictureInPictureController *)pictureInPictureController
{
    NSLog(@"pictureInPictureControllerDidStartPictureInPicture");
}

- (void)pictureInPictureControllerDidStopPictureInPicture:(AVPictureInPictureController *)pictureInPictureController
{
    NSLog(@"pictureInPictureControllerDidStopPictureInPicture");
}

- (void)pictureInPictureControllerWillStartPictureInPicture:(AVPictureInPictureController *)pictureInPictureController
{
    if (self.onStartPictureInPicture) {
        self.onStartPictureInPicture(@{});
    }
}

- (void)pictureInPictureControllerWillStopPictureInPicture:(AVPictureInPictureController *)pictureInPictureController
{
    if (self.onStopPictureInPicture) {
        self.onStopPictureInPicture(@{});
    }
}

- (void)pictureInPictureController:(AVPictureInPictureController *)pictureInPictureController failedToStartPictureInPictureWithError:(NSError *)error
{
    NSLog(@"failedToStartPictureInPictureWithError: %@", error.localizedDescription);
}

- (void)pictureInPictureController:(AVPictureInPictureController *)pictureInPictureController restoreUserInterfaceForPictureInPictureStopWithCompletionHandler:(void (^)(BOOL))completionHandler {
  if (self.onMaximizePictureInPicture) {
    self.onMaximizePictureInPicture(@{});
  }
}

@end
