# react-native-brightcove-ima-player

<img src="./assets/preview.jpg" width="500">

React Native implementation of Brightcove Player native SDK video player.

## Native SDK versions

- iOS: Brightcove Core and SSAI **7.2.25**, requiring iOS **12.0** or later.
- Android: Brightcove SDK and plugins **10.4.32**, requiring Android API **21** or later. Google IMA is aligned with Brightcove's **3.37.0** dependency.

The host's React Native version and other dependencies may require higher minimum OS versions. The React Native player API is unchanged; the existing lifecycle, fullscreen, SSAI and Now Playing integration remains in this wrapper.

See the [SDK upgrade checkpoint](docs/native-sdk-upgrade.md) for migration details, completed checks and remaining validation.

## Installation

```sh
npm install react-native-brightcove-ima-player
```

### iOS

- Add Brightcove's source to `Podfile` and use iOS `12.0` or later.
- For CocoaPods hosts using static libraries, enable module maps for the Brightcove pods inside the app target so the Swift wrapper can import them.

```rb
source 'https://github.com/brightcove/BrightcoveSpecs.git'

platform :ios, '12.0'

target 'YourApp' do
  # Alongside the host's React Native/autolinking setup:
  pod 'Brightcove-Player-Core', :modular_headers => true
  pod 'Brightcove-Player-SSAI', :modular_headers => true
end
```

When upgrading an existing installation, refresh Brightcove's spec repository and update only the player dependencies:

```sh
pod repo update brightcove-brightcovespecs # Use the name shown by `pod repo list`.
pod update Brightcove-Player-Core Brightcove-Player-SSAI react-native-brightcove-ima-player --no-repo-update
```

Brightcove has deprecated CocoaPods distribution but still publishes these versions. This update retains CocoaPods; migrating the host to Swift Package Manager is separate work.

### Android

- Add maven source to repositories in `android/build.gradle`
- [Enables multiDex](https://developer.android.com/studio/build/multidex).
- Use a current Android toolchain (the library's standalone AGP default is `8.10.1`), compile SDK 36 or later, and min SDK 21 or later. Existing `BrightcoveImaPlayer_*SdkVersion` host overrides remain supported.
- Enable core library desugaring in the **app module** and use `desugar_jdk_libs` **2.1.3 or later**. The library also declares this requirement; a host pin to `2.0.4` fails the Android dependency metadata check.

```gradle
allprojects {
  repositories {
      maven {
          url 'https://repo.brightcove.com/releases'
      }
  }
}
```

In `android/app/build.gradle`:

```gradle
android {
  compileOptions {
    coreLibraryDesugaringEnabled true
  }
}

dependencies {
  coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:2.1.3'
}
```

Rebuild both native apps after updating the package; a JavaScript reload does not replace native SDKs. When another player shares Media3, verify the version selected by Gradle and test both integrations rather than forcing a downgrade.

## Usage

```jsx
import { BrightcoveIMAPlayer } from "react-native-brightcove-ima-player";

// ...

<BrightcoveIMAPlayer
  accountId="3636334163001"
  videoId="3666678807001"
  policyKey="BCpkADawqM1W-vUOMe6RSA3pA6Vw-VWUNn5rL0lzQabvrI63-VjS93gVUugDlmBpHIxP16X8TSe5LSKM415UHeMBmxl7pqcwVY_AZ4yKFwIpZPvXE34TpXEYYcmulxJQAOvHbv2dpfq-S_cm"
/>
```

- Video player component of Brightcove.
- It may not work on Android simulator.
- For a more detailed example, please see example app.

| Prop                   | Type     | Description                                                                         | Event Object                 |
| ---------------------- | -------- |-------------------------------------------------------------------------------------| ---------------------------- |
| accountId              | string   | Brightcove AccountId. Required                                                      |                              |
| policyKey              | string   | Brightcove PolicyKey. Required                                                      |                              |
| videoId                | string   | Brightcove VideoId to playback.                                                     |                              |
| autoPlay               | boolean  | Set `true` to play automatically                                                    |                              |
| play                   | boolean  | Control playback and pause                                                          |                              |
| fullscreen             | boolean  | Control full screen state                                                           |                              |
| volume                 | number   | Set audio volume (`0.0 ~ 1.0`)                                                      |                              |
| bitRate                | number   | Set maximum buffering bitrate. Set `0` to automatic quality                         |                              |
| adVideoLoadTimeout     | number   | Set the amount of milliseconds for video to load. Default is `3000`                 |                              |
| playbackRate           | number   | Set playback speed scale. Default is `1`                                            |                              |
| disableDefaultControl  | boolean  | Disable default player control. Set `true` when you implement own video controller. |                              |
| onAdsLoaded           | Function | Indicates the IMA ads manager has loaded                                             |                              |
| onReady                | Function | Indicates the video can be played back                                              |                              |
| onPlay                 | Function | Indicates the video playback starts                                                 |                              |
| onPause                | Function | Indicates the video is paused                                                       |                              |
| onEnd                  | Function | Indicates the video is played to the end                                            |                              |
| onProgress             | Function | Indicates the playback head of the video advances.                                  | `{ currentTime: number }`    |
| onChangeDuration       | Function | Indicates the video length is changed                                               | `{ duration: number }`       |
| onUpdateBufferProgress | Function | Indicates the video loading buffer is updated                                       | `{ bufferProgress: number }` |
| onEnterFullscreen      | Function | Indicates the player enters full screen                                             |                              |
| onExitFullscreen       | Function | Indicates the player exit full screen                                               |                              |

| Method                                | Description                       |
| ------------------------------------- | --------------------------------- |
| seekTo(timeInSeconds: number) => void | Change playhead to arbitrary time |

### BrightcovePlayerPoster

```jsx
import { BrightcoveIMAPlayerPoster } from "react-native-brightcove-ima-player";

// ...

<BrightcoveIMAPlayerPoster
  style={styles.player}
  accountId="3636334163001"
  videoId="3666678807001"
  policyKey="BCpkADawqM1W-vUOMe6RSA3pA6Vw-VWUNn5rL0lzQabvrI63-VjS93gVUugDlmBpHIxP16X8TSe5LSKM415UHeMBmxl7pqcwVY_AZ4yKFwIpZPvXE34TpXEYYcmulxJQAOvHbv2dpfq-S_cm"
  resizeMode="contain"
/>
```

- Displays a poster specified by `videoId`, `referenceId` or `videoToken`.
- Prop is about the same as `BrightcovePlayer`.

| Prop        | Type   | Description                                                                                                                                                                                                                          |
| ----------- | ------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| accountId   | string | Required                                                                                                                                                                                                                             |
| policyKey   | string | Required                                                                                                                                                                                                                             |
| videoId     | string |                                                                                                                                                                                                                                      |
| referenceId | string |                                                                                                                                                                                                                                      |
| videoToken  | string |                                                                                                                                                                                                                                      |
| resizeMode  | string | Set the image resize method. Specifying `cover` or `contain` works the same as CSS keywords of `background-size`. Specifying `fit`, scales to fit the component size without considering aspect ratio of the image. Default value is `cover`. |


## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT
