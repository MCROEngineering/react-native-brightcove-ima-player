package com.matejdr.brightcoveimaplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.view.Choreographer;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.core.view.ViewCompat;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.MappingTrackSelector;

import com.brightcove.player.display.ExoPlayerVideoDisplayComponent;
import com.brightcove.player.edge.Catalog;
import com.brightcove.player.edge.CatalogError;
import com.brightcove.player.edge.VideoListener;
import com.brightcove.player.event.Event;
import com.brightcove.player.event.EventEmitter;
import com.brightcove.player.event.EventListener;
import com.brightcove.player.event.EventType;
import com.brightcove.player.mediacontroller.BrightcoveMediaController;
import com.brightcove.player.model.Video;
import com.brightcove.player.network.HttpRequestConfig;
import com.brightcove.player.pictureinpicture.PictureInPictureManager;
import com.brightcove.player.playback.PlaybackNotification;
import com.brightcove.player.playback.PlaybackNotificationConfig;
import com.brightcove.player.view.BrightcoveExoPlayerVideoView;
import com.brightcove.ssai.SSAIComponent;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.matejdr.brightcoveimaplayer.util.FullScreenHandler;
import com.brightcove.playback.notification.BackgroundPlaybackNotification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class BrightcoveIMAPlayerView extends RelativeLayout implements LifecycleEventListener {
  private final String TAG = this.getClass().getSimpleName();
  private final ThemedReactContext context;
  private final ReactApplicationContext applicationContext;
  private BrightcoveExoPlayerVideoView brightcoveVideoView;
  private BrightcoveMediaController mediaController;
  private ReadableMap settings;
  private String policyKey;
  private String accountId;
  private String videoId;

  private boolean isAudioOnly = true;

  private String adConfigId;
  private boolean autoPlay = false;
  private boolean playing = false;
  private boolean adsPlaying = false;
  private boolean disableDefaultControl = false;
  private int bitRate = 0;
  private int adVideoLoadTimeout = 3000;
  private float playbackRate = 1;
  private EventEmitter eventEmitter;
  private SSAIComponent plugin;

  private FullScreenHandler fullScreenHandler;
  private int controlbarTimeout = 4000;
  private boolean captionExists = false;

  public BrightcoveIMAPlayerView(ThemedReactContext context, ReactApplicationContext applicationContext) {
    super(context);
    this.context = context;
    this.applicationContext = applicationContext;
    this.applicationContext.addLifecycleEventListener(this);
    this.setBackgroundColor(Color.BLACK);
    setup();
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();

    PictureInPictureManager.getInstance().setOnUserLeaveEnabled(false);
    if (this.fullScreenHandler != null) {
      this.fullScreenHandler.cleanup();
    }
  }

  private void setup() {
    this.brightcoveVideoView = new BrightcoveExoPlayerVideoView(this.context);

    this.addView(this.brightcoveVideoView);
    this.brightcoveVideoView.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    this.brightcoveVideoView.finishInitialization();

    this.requestLayout();

    this.configurePlaybackControls(false);

    setupLayoutHack();

    ViewCompat.setTranslationZ(this, 9999);

    eventEmitter = this.brightcoveVideoView.getEventEmitter();

    setupSSAI();

    ExoPlayerVideoDisplayComponent videoDisplayComponent = (ExoPlayerVideoDisplayComponent) this.brightcoveVideoView.getVideoDisplay();
    videoDisplayComponent.setAllowHlsChunklessPreparation(false);

    if (videoDisplayComponent.getPlaybackNotification() == null) {
      videoDisplayComponent.setPlaybackNotification(createPlaybackNotification());
    }

    eventEmitter.on(EventType.VIDEO_SIZE_KNOWN, new EventListener() {
      @Override
      public void processEvent(Event e) {
        fixVideoLayout();
        updateBitRate();
        updatePlaybackRate();
      }
    });
    eventEmitter.on(EventType.READY_TO_PLAY, new EventListener() {
      @Override
      public void processEvent(Event e) {
        WritableMap event = Arguments.createMap();
        ReactContext reactContext = (ReactContext) BrightcoveIMAPlayerView.this.getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcoveIMAPlayerView.this.getId(), BrightcoveIMAPlayerViewManager.EVENT_READY, event);
      }
    });
    eventEmitter.on(EventType.DID_PLAY, new EventListener() {
      @Override
      public void processEvent(Event e) {
        BrightcoveIMAPlayerView.this.playing = true;
        WritableMap event = Arguments.createMap();
        ReactContext reactContext = (ReactContext) BrightcoveIMAPlayerView.this.getContext();
        // add hasCaptions prop on event if the video hasCaptions
        captionExists = BrightcoveIMAPlayerView.this.brightcoveVideoView.getClosedCaptioningController().checkIfCaptionsExist(
          BrightcoveIMAPlayerView.this.brightcoveVideoView.getCurrentVideo()
        );
        event.putBoolean("hasCaptions", captionExists);
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcoveIMAPlayerView.this.getId(), BrightcoveIMAPlayerViewManager.EVENT_PLAY, event);
      }
    });
    eventEmitter.on(EventType.DID_PAUSE, new EventListener() {
      @Override
      public void processEvent(Event e) {
        BrightcoveIMAPlayerView.this.playing = false;
        WritableMap event = Arguments.createMap();
        ReactContext reactContext = (ReactContext) BrightcoveIMAPlayerView.this.getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcoveIMAPlayerView.this.getId(), BrightcoveIMAPlayerViewManager.EVENT_PAUSE, event);
      }
    });
    eventEmitter.on(EventType.COMPLETED, new EventListener() {
      @Override
      public void processEvent(Event e) {
        WritableMap event = Arguments.createMap();
        ReactContext reactContext = (ReactContext) BrightcoveIMAPlayerView.this.getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcoveIMAPlayerView.this.getId(), BrightcoveIMAPlayerViewManager.EVENT_END, event);
      }
    });
    eventEmitter.on(EventType.PROGRESS, new EventListener() {
      @Override
      public void processEvent(Event e) {
        WritableMap event = Arguments.createMap();
        Integer playhead = (Integer) e.properties.get(Event.PLAYHEAD_POSITION);
        event.putDouble("currentTime", playhead / 1000d);
        ReactContext reactContext = (ReactContext) BrightcoveIMAPlayerView.this.getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcoveIMAPlayerView.this.getId(), BrightcoveIMAPlayerViewManager.EVENT_PROGRESS, event);
      }
    });
    eventEmitter.on(EventType.ENTER_FULL_SCREEN, new EventListener() {
      @Override
      public void processEvent(Event e) {
        mediaController.show();
        mediaController.setShowHideTimeout(controlbarTimeout);
        WritableMap event = Arguments.createMap();
        ReactContext reactContext = (ReactContext) BrightcoveIMAPlayerView.this.getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcoveIMAPlayerView.this.getId(), BrightcoveIMAPlayerViewManager.EVENT_ENTER_FULLSCREEN, event);
      }
    });
    eventEmitter.on(EventType.EXIT_FULL_SCREEN, new EventListener() {
      @Override
      public void processEvent(Event e) {
        if (disableDefaultControl) {
          mediaController.hide();
          mediaController.setShowHideTimeout(1);
        } else {
          mediaController.show();
          mediaController.setShowHideTimeout(controlbarTimeout);
        }
        WritableMap event = Arguments.createMap();
        ReactContext reactContext = (ReactContext) BrightcoveIMAPlayerView.this.getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcoveIMAPlayerView.this.getId(), BrightcoveIMAPlayerViewManager.EVENT_EXIT_FULLSCREEN, event);
      }
    });
    eventEmitter.on(EventType.VIDEO_DURATION_CHANGED, new EventListener() {
      @Override
      public void processEvent(Event e) {
        Long duration = (Long) e.properties.get(Event.VIDEO_DURATION);
        WritableMap event = Arguments.createMap();
        event.putDouble("duration", duration / 1000d);
        ReactContext reactContext = (ReactContext) BrightcoveIMAPlayerView.this.getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcoveIMAPlayerView.this.getId(), BrightcoveIMAPlayerViewManager.EVENT_CHANGE_DURATION, event);
      }
    });
    eventEmitter.on(EventType.BUFFERED_UPDATE, new EventListener() {
      @Override
      public void processEvent(Event e) {
        Integer percentComplete = (Integer) e.properties.get(Event.PERCENT_COMPLETE);
        WritableMap event = Arguments.createMap();
        event.putDouble("bufferProgress", percentComplete / 100d);
        ReactContext reactContext = (ReactContext) BrightcoveIMAPlayerView.this.getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcoveIMAPlayerView.this.getId(), BrightcoveIMAPlayerViewManager.EVENT_UPDATE_BUFFER_PROGRESS, event);
      }
    });

    PictureInPictureManager.getInstance().setOnUserLeaveEnabled(!this.isAudioOnly);
    PictureInPictureManager.getInstance().registerActivity(this.context.getCurrentActivity(), this.brightcoveVideoView);
    BrightcovePiPManagerProxy.getInstance().setBrightcoveIMAPlayerView(this.brightcoveVideoView);
  }

  private PlaybackNotification createPlaybackNotification() {
    ExoPlayerVideoDisplayComponent displayComponent = ((ExoPlayerVideoDisplayComponent) brightcoveVideoView.getVideoDisplay());
    PlaybackNotification notification = BackgroundPlaybackNotification.getInstance(this.context);
    PlaybackNotificationConfig config = new PlaybackNotificationConfig(this.context);
    notification.setConfig(new PlaybackNotificationConfig(this.context));
    notification.setPlayback(displayComponent.getPlayback());
    return notification;
  }

  public void setSettings(ReadableMap settings) {
    this.settings = settings;
    // disabling autoPlay coming from settings object
    if (settings != null && settings.hasKey("autoPlay")) {
      this.autoPlay = settings.getBoolean("autoPlay");
    }
    if (settings != null && settings.hasKey("adConfigId")) {
      this.setAdConfigId(settings.getString("adConfigId"));
    }
  }

  public void setPolicyKey(String policyKey) {
    this.policyKey = policyKey;
    this.loadVideo();
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
    this.loadVideo();
  }

  public void setVideoId(String videoId) {
    this.videoId = videoId;
    this.loadVideo();
  }

  public void setIsAudioOnly(boolean isAudioOnly) {
    this.isAudioOnly = isAudioOnly;
  }

  public void setAdConfigId(String adConfigId) {
    this.adConfigId = adConfigId;
  }

  public void setAutoPlay(boolean autoPlay) {
    this.autoPlay = autoPlay;
  }

  public void setPlay(boolean play) {
    if (this.playing == play) return;
    if (play) {
      this.brightcoveVideoView.start();
    } else {
      this.brightcoveVideoView.pause();
    }
  }

  public void setDisableDefaultControl(boolean disabled) {
    this.disableDefaultControl = disabled;
    if (disabled) {
      this.mediaController.hide();
      this.mediaController.setShowHideTimeout(1);
    } else {
      this.mediaController.show();
      this.mediaController.setShowHideTimeout(controlbarTimeout);
    }
  }

  public void setFullscreen(boolean fullscreen) {
    if (fullscreen) {
      this.fullScreenHandler.openFullscreenDialog();
      this.brightcoveVideoView.getEventEmitter().emit(EventType.ENTER_FULL_SCREEN);
    } else {
      this.fullScreenHandler.closeFullscreenDialog();
      this.brightcoveVideoView.getEventEmitter().emit(EventType.EXIT_FULL_SCREEN);
    }
  }

  public void showCaptionsDialog() {
    captionExists = this.brightcoveVideoView.getClosedCaptioningController().checkIfCaptionsExist(
      this.brightcoveVideoView.getCurrentVideo()
    );
    // Emit event
    if (captionExists) {
      this.brightcoveVideoView.getClosedCaptioningController().showCaptionsDialog();
    }

  }

  public void toggleFullscreen(boolean isFullscreen) {
    if (isFullscreen) {
      this.fullScreenHandler.openFullscreenDialog();
    } else {
      this.fullScreenHandler.closeFullscreenDialog();
    }
  }

  public void setVolume(float volume) {
    Map<String, Object> details = new HashMap<>();
    details.put(Event.VOLUME, volume);
    this.brightcoveVideoView.getEventEmitter().emit(EventType.SET_VOLUME, details);
  }

  public void setBitRate(int bitRate) {
    this.bitRate = bitRate;
    this.updateBitRate();
  }

  public void setAdVideoLoadTimeout(int adVideoLoadTimeout) {
    this.adVideoLoadTimeout = adVideoLoadTimeout;
    this.loadVideo();
  }

  public void setPlaybackRate(float playbackRate) {
    if (playbackRate == 0) return;
    this.playbackRate = playbackRate;
    this.updatePlaybackRate();
  }

  public void seekTo(int time) {
    this.brightcoveVideoView.seekTo(time);
  }

  public void stopPlayback() {
    if (this.brightcoveVideoView != null) {
      this.brightcoveVideoView.stopPlayback();
      this.brightcoveVideoView.destroyDrawingCache();
      this.brightcoveVideoView.clear();
    }
  }

  public void destroy() {
    if (this.brightcoveVideoView != null) {
      this.stopPlayback();

      this.brightcoveVideoView.destroyDrawingCache();
      this.brightcoveVideoView.clear();
      this.removeAllViews();
      this.applicationContext.removeLifecycleEventListener(this);
    }
  }

  public void pause() {
    if (this.playing && this.brightcoveVideoView != null) {
      this.brightcoveVideoView.pause();
    }
  }

  public void play() {
    if (this.brightcoveVideoView != null) {
      this.brightcoveVideoView.start();
    }
  }

  @OptIn(markerClass = UnstableApi.class) private void updateBitRate() {
    if (this.bitRate == 0) return;
    ExoPlayerVideoDisplayComponent videoDisplay = ((ExoPlayerVideoDisplayComponent) this.brightcoveVideoView.getVideoDisplay());
    ExoPlayer player = videoDisplay.getExoPlayer();
    DefaultTrackSelector trackSelector = videoDisplay.getTrackSelector();
    if (player == null) return;
    MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
    if (mappedTrackInfo == null) return;

    DefaultTrackSelector.Parameters params = trackSelector.buildUponParameters().setMaxVideoBitrate(bitRate).build();
    trackSelector.setParameters(params);
  }

  private void updatePlaybackRate() {
    ExoPlayer expPlayer = ((ExoPlayerVideoDisplayComponent) this.brightcoveVideoView.getVideoDisplay()).getExoPlayer();
    if (expPlayer != null) {
      expPlayer.setPlaybackParameters(new PlaybackParameters(playbackRate, 1f));
    }
  }

  private void loadVideo() {
    if (this.accountId == null || this.policyKey == null) {
      return;
    }
    Catalog catalog = new Catalog.Builder(eventEmitter, this.accountId)
      .setBaseURL(Catalog.DEFAULT_EDGE_BASE_URL)
      .setPolicy(this.policyKey)
      .build();

    HttpRequestConfig httpRequestConfig = new HttpRequestConfig.Builder()
      .addQueryParameter(HttpRequestConfig.KEY_AD_CONFIG_ID, this.adConfigId)
      .build();

    plugin.addListener("didSetSource", new EventListener() {
      @Override
      public void processEvent(Event event) {
        BrightcoveIMAPlayerView.this.brightcoveVideoView.start();
      }
    });

    if (this.videoId != null) {
      catalog.findVideoByID(this.videoId, httpRequestConfig, new VideoListener() {
        @Override
        public void onVideo(Video video) {
          if (adConfigId != null) {
            plugin.processVideo(video);
          } else {
            BrightcoveIMAPlayerView.this.brightcoveVideoView.clear();
            BrightcoveIMAPlayerView.this.brightcoveVideoView.add(video);
            BrightcoveIMAPlayerView.this.brightcoveVideoView.start();
            ReactContext reactContext = (ReactContext) BrightcoveIMAPlayerView.this.getContext();
            reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcoveIMAPlayerView.this.getId(), BrightcoveIMAPlayerViewManager.EVENT_VIDEO_PLAY, Arguments.createMap());
          }
        }

        @Override
        public void onError(@NonNull List<CatalogError> errors) {
          Log.e(TAG, errors.toString());
        }
      });
    }
  }

  private void fixVideoLayout() {
    int viewWidth = this.getMeasuredWidth();
    int viewHeight = this.getMeasuredHeight();
    SurfaceView surfaceView = (SurfaceView) this.brightcoveVideoView.getRenderView();
    surfaceView.measure(viewWidth, viewHeight);
    int surfaceWidth = surfaceView.getMeasuredWidth();
    int surfaceHeight = surfaceView.getMeasuredHeight();
    int leftOffset = (viewWidth - surfaceWidth) / 2;
    int topOffset = (viewHeight - surfaceHeight) / 2;
    surfaceView.layout(leftOffset, topOffset, leftOffset + surfaceWidth, topOffset + surfaceHeight);
  }

  /**
   * Setup the Brightcove IMA Plugin.
   */
  private void setupSSAI() {
    // Enable logging up ad start.
    eventEmitter.on(EventType.AD_STARTED, event -> {
      adsPlaying = true;
      PictureInPictureManager.getInstance().setOnUserLeaveEnabled(false);
    });
    eventEmitter.on("startAd", event -> {
      this.mediaController = this.fullScreenHandler.initMediaController(this.brightcoveVideoView, true);
      PictureInPictureManager.getInstance().setOnUserLeaveEnabled(false);
    });
    // Enable Logging upon ad completion.
    eventEmitter.on(EventType.AD_COMPLETED, event -> {
      adsPlaying = false;
      this.mediaController = this.fullScreenHandler.initMediaController(this.brightcoveVideoView, false);

      ReactContext reactContext = (ReactContext) BrightcoveIMAPlayerView.this.getContext();
      reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(BrightcoveIMAPlayerView.this.getId(), BrightcoveIMAPlayerViewManager.EVENT_VIDEO_PLAY, Arguments.createMap());

      PictureInPictureManager.getInstance().setOnUserLeaveEnabled(!this.isAudioOnly);
    });
    // Enable Logging upon ad break completion.
    eventEmitter.on(EventType.AD_BREAK_COMPLETED, event -> {
      adsPlaying = false;
    });

    plugin = new SSAIComponent(this.context, this.brightcoveVideoView);
  }

  @Override
  public void onHostResume() {
    // handleAppStateDidChange active
    this.configurePlaybackControls(this.adsPlaying);
  }

  @Override
  public void onHostPause() {
    // handleAppStateDidChange background
  }

  @Override
  public void onHostDestroy() {
    this.brightcoveVideoView.destroyDrawingCache();
    this.brightcoveVideoView.clear();
    this.removeAllViews();
    this.applicationContext.removeLifecycleEventListener(this);
  }

  public void setupLayoutHack() {
    Choreographer.getInstance().postFrameCallback(new Choreographer.FrameCallback() {
      @Override
      public void doFrame(long frameTimeNanos) {
        manuallyLayoutChildren();
        getViewTreeObserver().dispatchOnGlobalLayout();
        Choreographer.getInstance().postFrameCallback(this);
      }
    });
  }

  private void manuallyLayoutChildren() {
    for (int i = 0; i < getChildCount(); i++) {
      View child = getChildAt(i);
      child.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
      child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());
    }
  }

  private void configurePlaybackControls(boolean isAds) {
    if (this.fullScreenHandler != null) {
      this.fullScreenHandler.cleanup();
    }
    this.fullScreenHandler = new FullScreenHandler(context, this.brightcoveVideoView, this);
    this.mediaController = this.fullScreenHandler.initMediaController(this.brightcoveVideoView, isAds);
  }

}
