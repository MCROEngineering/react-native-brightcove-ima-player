package com.matejdr.brightcoveimaplayer;

import com.brightcove.player.event.Event;
import com.brightcove.player.event.EventEmitter;
import com.brightcove.player.event.EventListener;
import com.brightcove.player.event.EventType;
import com.brightcove.ssai.event.SSAIEvent;
import com.brightcove.ssai.event.SSAIEventType;
import com.brightcove.ssai.timeline.Timeline;

import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ContentPlaybackListenerTest {
  private final Map<String, EventListener> handlers = new HashMap<>();
  private long position;
  private boolean playing;
  private int contentEvents;
  private final EventEmitter emitter = (EventEmitter) Proxy.newProxyInstance(
    EventEmitter.class.getClassLoader(), new Class<?>[]{EventEmitter.class},
    (proxy, method, args) -> {
      if (!method.getName().equals("on")) throw new AssertionError(method.getName());
      handlers.put((String) args[0], (EventListener) args[1]);
      return handlers.size();
    });
  private final ContentPlaybackListener listener = new ContentPlaybackListener(
    emitter, new ContentPlaybackListener.PlaybackState() {
      public long getAbsolutePosition() { return position; }
      public boolean isPlaying() { return playing; }
    }, () -> contentEvents++);

  private void emit(String type) {
    dispatch(new Event(type));
  }

  private void dispatch(Event event) {
    EventListener handler = handlers.get(event.getType());
    if (handler != null) handler.processEvent(event);
  }

  private void adTimeline(long prerollEnd) {
    // Stand in for the SDK's timeline; the production event wiring is exercised.
    Timeline timeline = (Timeline) Proxy.newProxyInstance(
      Timeline.class.getClassLoader(), new Class<?>[]{Timeline.class},
      (proxy, method, args) -> {
        if (!method.getName().equals("isPlayingAd")) throw new AssertionError(method.getName());
        return (long) args[0] < prerollEnd;
      });
    Event metadata = new Event(SSAIEventType.AD_DATA_READY);
    metadata.properties.put(SSAIEvent.VMAP_TIMELINE, timeline);
    dispatch(metadata);
  }

  private void progress(long absolutePosition) {
    position = absolutePosition;
    Event progress = new Event(EventType.PROGRESS);
    // SSAI's content-relative playhead is not the absolute position in the stream.
    progress.properties.put(Event.PLAYHEAD_POSITION, 0);
    dispatch(progress);
  }

  @Test public void ssaiWithoutAdsSignalsContentWithoutAnAdCompletedEvent() {
    listener.startSource(true);
    adTimeline(0);
    emit(EventType.READY_TO_PLAY);
    assertEquals(0, contentEvents);
    playing = true;
    emit(EventType.DID_PLAY);
    progress(500);
    assertEquals(1, contentEvents);
  }

  @Test public void waitsForSsaiMetadataEvenWhenPlayAndProgressArriveFirst() {
    listener.startSource(true);
    playing = true;
    emit(EventType.DID_PLAY);
    progress(500);
    assertEquals(0, contentEvents);
    adTimeline(0);
    progress(1000);
    assertEquals(1, contentEvents);
  }

  @Test public void doesNotUnlockDuringAPrerollOrBetweenAdsInTheSamePod() {
    listener.startSource(true);
    adTimeline(10000);
    playing = true;
    emit(EventType.DID_PLAY);
    progress(500);
    assertEquals(0, contentEvents);
    emit(EventType.AD_COMPLETED);
    progress(6000);
    assertEquals(0, contentEvents);
    emit(EventType.AD_COMPLETED);
    emit(EventType.AD_BREAK_COMPLETED);
    assertEquals(0, contentEvents);
    progress(10500);
    assertEquals(1, contentEvents);
  }

  @Test public void skippedPrerollCanStartContentWithoutAdCompleted() {
    listener.startSource(true);
    adTimeline(10000);
    playing = true;
    progress(1000);
    emit(SSAIEventType.SKIP_AD);
    progress(10500);
    assertEquals(1, contentEvents);
  }

  @Test public void plainVideoWaitsForPlayingProgress() {
    listener.startSource(false);
    emit(EventType.READY_TO_PLAY);
    progress(500);
    assertEquals(0, contentEvents);
    playing = true;
    progress(500);
    assertEquals(1, contentEvents);
  }

  @Test public void pausedOrInvalidPlayheadDoesNotSignalContent() {
    listener.startSource(true);
    adTimeline(0);
    progress(500);
    assertEquals(0, contentEvents);
    playing = true;
    progress(-1);
    assertEquals(0, contentEvents);
    progress(500);
    assertEquals(1, contentEvents);
  }

  @Test public void pauseResumeAndLaterAdBreaksDoNotDuplicateTheContentStart() {
    listener.startSource(true);
    adTimeline(0);
    playing = true;
    progress(500);
    playing = false;
    emit(EventType.DID_PAUSE);
    progress(500);
    playing = true;
    emit(EventType.DID_PLAY);
    emit(EventType.AD_COMPLETED);
    emit(EventType.AD_BREAK_COMPLETED);
    progress(1000);
    assertEquals(1, contentEvents);
  }

  @Test public void nextSourceClearsOldMetadataAndCanSignalContentAgain() {
    listener.startSource(true);
    adTimeline(0);
    playing = true;
    progress(500);
    assertEquals(1, contentEvents);
    listener.startSource(true);
    progress(500);
    assertEquals(1, contentEvents);
    adTimeline(10000);
    progress(500);
    assertEquals(1, contentEvents);
    progress(10500);
    assertEquals(2, contentEvents);
    listener.startSource(false);
    progress(500);
    assertEquals(3, contentEvents);
  }

  @Test public void stoppedPlayerIgnoresLateMetadataAndProgress() {
    listener.startSource(true);
    playing = true;
    listener.stop();
    adTimeline(0);
    progress(500);
    assertEquals(0, contentEvents);
  }
}
