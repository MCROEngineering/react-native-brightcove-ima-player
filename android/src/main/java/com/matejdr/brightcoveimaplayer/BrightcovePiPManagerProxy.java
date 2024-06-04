package com.matejdr.brightcoveimaplayer;

import android.app.Activity;
import android.content.res.Configuration;

import androidx.annotation.NonNull;

import com.brightcove.player.pictureinpicture.PictureInPictureManager;
import com.brightcove.player.view.BrightcoveExoPlayerVideoView;

public class BrightcovePiPManagerProxy {
  private static final BrightcovePiPManagerProxy mInstance = new BrightcovePiPManagerProxy();

  public BrightcoveExoPlayerVideoView brightcoveIMAPlayerView;

  private boolean isRegisteredActivity = false;


  public static BrightcovePiPManagerProxy getInstance() {
    return mInstance;
  }

  public void setBrightcoveIMAPlayerView(BrightcoveExoPlayerVideoView view) {
    this.brightcoveIMAPlayerView = view;
    this.isRegisteredActivity = true;
  }

  public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, @NonNull Configuration newConfig) {
    PictureInPictureManager.getInstance().onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
  }

  public void onUserLeaveHint() {
    if (this.isRegisteredActivity) {
      PictureInPictureManager.getInstance().onUserLeaveHint();
    }
  }

  public void unregisterActivity(Activity activity) {
    if (this.isRegisteredActivity) {
      PictureInPictureManager.getInstance().unregisterActivity(activity);
      this.isRegisteredActivity = false;
    }
  }
}
