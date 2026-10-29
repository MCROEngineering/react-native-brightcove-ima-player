package com.matejdr.brightcoveimaplayer;

import com.brightcove.player.event.EventEmitter;
import com.brightcove.player.event.EventType;
import com.brightcove.ssai.event.SSAIEvent;
import com.brightcove.ssai.event.SSAIEventType;
import com.brightcove.ssai.timeline.Timeline;

/** Reports content playback once per source, including SSAI sessions with no ads. */
final class ContentPlaybackListener {
  interface PlaybackState {
    long getAbsolutePosition();
    boolean isPlaying();
  }

  private boolean active;
  private boolean requiresTimeline;
  private boolean notified;
  private Timeline timeline;

  ContentPlaybackListener(EventEmitter emitter, PlaybackState playback, Runnable onContentPlayback) {
    emitter.on(SSAIEventType.AD_DATA_READY, event -> {
      if (!active || !requiresTimeline) return;
      Object value = event.properties.get(SSAIEvent.VMAP_TIMELINE);
      if (value instanceof Timeline) timeline = (Timeline) value;
    });
    emitter.on(EventType.PROGRESS, event -> {
      if (!active || notified || !playback.isPlaying()) return;
      // A generic play/progress event can be a preroll. Wait for SSAI metadata
      // and inspect the absolute stream position, not the content-relative time.
      if (requiresTimeline && timeline == null) return;
      long position = playback.getAbsolutePosition();
      if (position < 0 || (timeline != null && timeline.isPlayingAd(position))) return;
      notified = true;
      onContentPlayback.run();
    });
  }

  void startSource(boolean requiresTimeline) {
    this.requiresTimeline = requiresTimeline;
    timeline = null;
    notified = false;
    active = true;
  }

  void stop() {
    active = false;
    timeline = null;
  }
}
