package com.matejdr.brightcoveimaplayer.util;

import android.content.res.Configuration;

public interface PiPModeChangedListener {
  void onPiPModeChanged(boolean isInPictureInPictureMode, Configuration newConfig);
}

