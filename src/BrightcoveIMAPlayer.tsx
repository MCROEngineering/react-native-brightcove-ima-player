import React, { Component } from 'react';
import {
  requireNativeComponent,
  UIManager,
  Platform,
  ViewProps,
  findNodeHandle,
  NativeSyntheticEvent,
} from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-brightcove-ima-player' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo managed workflow\n';

type TBrightcoveIMAPlayerSettings = {
  IMAUrl: string;
  autoAdvance?: boolean; // iOS only
  autoPlay?: boolean;
  allowsExternalPlayback?: boolean; // iOS only
  adConfigId?: string;
};

type TBrightcoveIMAPlayerEventBase = {
  target: number;
  hasCaptions?: boolean;
};

type TBrightcoveIMAPlayerEventProgress = TBrightcoveIMAPlayerEventBase & {
  currentTime: number;
};

type TBrightcoveIMAPlayerEventDuration = TBrightcoveIMAPlayerEventBase & {
  duration: number;
};

type TBrightcoveIMAPlayerEventBuffer = TBrightcoveIMAPlayerEventBase & {
  bufferProgress: number;
};

type BrightcoveIMAPlayerProps = ViewProps & {
  accountId: string;
  policyKey: string;
  videoId: string;
  settings: TBrightcoveIMAPlayerSettings;
  autoPlay?: boolean;
  play?: boolean;
  fullscreen?: boolean;
  disableDefaultControl?: boolean;
  disablePictureInPicture?: boolean;
  volume?: number;
  bitRate?: number;
  /**
   * Ad Video Load Timeout in milliseconds, default is 3000.
   */
  adVideoLoadTimeout?: number;
  playbackRate?: number;
  onAdsLoaded?: (
    event: NativeSyntheticEvent<TBrightcoveIMAPlayerEventBase>
  ) => void;
  onReady?: (
    event: NativeSyntheticEvent<TBrightcoveIMAPlayerEventBase>
  ) => void;
  onPlay?: (event: NativeSyntheticEvent<TBrightcoveIMAPlayerEventBase>) => void;
  /**
   * Android only
   */
  onVideoPlay?: (event: NativeSyntheticEvent<TBrightcoveIMAPlayerEventBase>) => void;
  onPause?: (
    event: NativeSyntheticEvent<TBrightcoveIMAPlayerEventBase>
  ) => void;
  onEnd?: (event: NativeSyntheticEvent<TBrightcoveIMAPlayerEventBase>) => void;
  onProgress?: (
    event: NativeSyntheticEvent<TBrightcoveIMAPlayerEventProgress>
  ) => void;
  onChangeDuration?: (
    event: NativeSyntheticEvent<TBrightcoveIMAPlayerEventDuration>
  ) => void;
  onUpdateBufferProgress?: (
    event: NativeSyntheticEvent<TBrightcoveIMAPlayerEventBuffer>
  ) => void;
  onEnterFullscreen?: (
    event: NativeSyntheticEvent<TBrightcoveIMAPlayerEventBase>
  ) => void;
  onExitFullscreen?: (
    event: NativeSyntheticEvent<TBrightcoveIMAPlayerEventBase>
  ) => void;
  onStartPictureInPicture?: (
    event: NativeSyntheticEvent<TBrightcoveIMAPlayerEventBase>
  ) => void;
  onStopPictureInPicture?: (
    event: NativeSyntheticEvent<TBrightcoveIMAPlayerEventBase>
  ) => void;
  onMaximizePictureInPicture?: (
    event: NativeSyntheticEvent<TBrightcoveIMAPlayerEventBase>
  ) => void;
};

const ComponentName = 'BrightcoveIMAPlayerView';

const BrightcoveIMAPlayerView =
  UIManager.getViewManagerConfig(ComponentName) != null
    ? requireNativeComponent<BrightcoveIMAPlayerProps>(ComponentName)
    : () => {
        throw new Error(LINKING_ERROR);
      };

export class BrightcoveIMAPlayer extends Component<BrightcoveIMAPlayerProps> {
  // private _root: React.RefObject<BrightcoveIMAPlayer> = React.createRef();

  componentWillUnmount = () => {
    if (Platform.OS === 'ios') {
      UIManager.dispatchViewManagerCommand(
        findNodeHandle(this),
        UIManager.getViewManagerConfig(ComponentName).Commands.dispose,
        []
      );
    }
  };

  seekTo = (seconds: number) => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(this),
      UIManager.getViewManagerConfig(ComponentName).Commands.seekTo,
      [seconds]
    );
  };

  toggleFullscreen = (isFullscreen: boolean) => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(this),
      UIManager.getViewManagerConfig(ComponentName).Commands.toggleFullscreen,
      [isFullscreen]
    );
  };

  toggleInViewPort = (inViewPort: boolean) => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(this),
      UIManager.getViewManagerConfig(ComponentName).Commands.toggleInViewPort,
      [inViewPort]
    );
  };

  showCaptionsDialog = () => {
    if (Platform.OS !== 'android') {
      console.warn('toggleCaptions is only available on Android');
      return;
    }

    UIManager.dispatchViewManagerCommand(
      findNodeHandle(this),
      UIManager.getViewManagerConfig(ComponentName).Commands.showCaptionsDialog,
      []
    );
  };

  stopPlayback = () => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(this),
      UIManager.getViewManagerConfig(ComponentName).Commands.stopPlayback,
      []
    );
  };

  destroy = () => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(this),
      Platform.OS === 'ios' ? UIManager.getViewManagerConfig(ComponentName).Commands.stopPlayback : UIManager.getViewManagerConfig(ComponentName).Commands.destroy,
      []
    );
  };

  play = () => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(this),
      UIManager.getViewManagerConfig(ComponentName).Commands.play,
      []
    );
  };

  pause = () => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(this),
      UIManager.getViewManagerConfig(ComponentName).Commands.pause,
      []
    );
  };

  enterPictureInPicture = () => {
    if (Platform.OS !== 'ios') {
      console.warn('enterPictureInPicture is only available on Android');
      return;
    }
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(this),
      UIManager.getViewManagerConfig(ComponentName).Commands.enterPictureInPicture,
      []
    );
  }

  exitPictureInPicture = () => {
    if (Platform.OS !== 'ios') {
      console.warn('exitPictureInPicture is only available on Android');
      return;
    }
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(this),
      UIManager.getViewManagerConfig(ComponentName).Commands.exitPictureInPicture,
      []
    );
  }

  render() {
    return (
      // @ts-ignore
      <BrightcoveIMAPlayerView
        // ref={(e: React.RefObject<BrightcoveIMAPlayer>) => (this._root = e)}
        {...this.props}
      />
    );
  }
}
