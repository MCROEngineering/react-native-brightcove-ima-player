# Native Brightcove SDK upgrade

This upgrade retains the existing React Native player API and the fork's custom SSAI, lifecycle, fullscreen, audio-only and Now Playing integration. It is separate from PGA's Phase 3 framework upgrade. The versions below are the tested targets; newer Brightcove patch releases are not included in this change.

## Dependency changes

| Dependency | Before | After |
| --- | --- | --- |
| Android Brightcove SDK and plugins | 10.0.1 | 10.4.32 |
| Android Google IMA | 3.21.1 | 3.37.0 |
| iOS Brightcove Core and SSAI | 6.13.1 | 7.2.25 |
| Library Android minimum SDK | 19 | 21 |
| Library iOS minimum version | 11 | 12 |

PGA already requires higher minimum OS versions, so these library minimums do not change PGA's supported devices.

## Wrapper changes

- Align Android Brightcove modules and Google IMA. Add an Android namespace, core library desugaring 2.1.3, SDK 36 defaults and a standalone Android Gradle Plugin 8.10.1 default. Host SDK overrides remain supported.
- Import the iOS Brightcove modules instead of removed individual headers. Use SDK 7 playback-service and video constants.
- Adapt the Swift Now Playing handler to optional session, player and video declarations, and import MediaPlayer explicitly.
- Preserve `play={false}` on the first mount after a cold launch. The old generic prop export read the imperative `-play` method as a BOOL getter while React Native cached the initial value. An explicit custom setter avoids that call while keeping the JS prop and imperative commands.
- Initialize the default volume once when the native view is created. Applying settings later must not overwrite an explicit `volume={0}` already supplied by React Native.

The existing Android content-playback listener and landscape fix remain in place. No full Fabric component rewrite, Java-to-Kotlin conversion, new player controls or Swift Package Manager migration is included.

## Host integration

- **Android:** enable core library desugaring in the app module with `desugar_jdk_libs` 2.1.3 or later. PGA's existing 2.1.5 satisfies this requirement. Check the resolved Media3 version when sharing it with other players; the tested PGA graph resolves 1.8.0 alongside Brightcove and Blaze.
- **iOS:** static-library CocoaPods hosts need modular headers for Brightcove Core and SSAI so the Swift wrapper can import them. Refresh the Brightcove specs, update the player pods and retain CocoaPods' updated XCFramework resource paths. See the README for commands.
- **Hidden audio views:** keep the native view mounted instead of using `display: none` under Fabric. PGA already includes this change; it was not established as a regression caused by this SDK upgrade.
- **Dependency installation:** test an npm-packed tarball or a published Git revision. Yarn Classic's `file:../folder` installation can copy the fork's development React/React Native into the host and cause duplicate-runtime `PlatformConstants` failures, followed by undefined player/poster exports. The packed artifact excludes development dependencies.

After publishing the fork, replace PGA's temporary tarball reference with the reviewed Git revision, regenerate its lockfile and verify a fresh installation from that revision. CocoaPods remains usable for these SDK versions; Brightcove's migration away from that distribution route is separate work.

## Validation

Latest integration checks were completed on **11 November 2026**, using PGA with Expo 58, React Native 0.88 and the New Architecture enabled. Playback was muted throughout testing.

| Check | Result |
| --- | --- |
| Android arm64 Debug and Release builds | Passed; Release rebuilt after switching to the packaged dependency |
| Android content-playback listener tests | 9 passed |
| iOS Debug simulator build | Passed on iPhone 18 Pro / iOS 27 with Xcode 27.2 |
| iOS Release build for generic iOS devices | Passed with signing disabled, including the final playback-prop fixes |
| Embedded iOS frameworks | Core and SSAI both verified as 7.2.25 |
| Android native playback | 12 checks passed: ready, poster mounting, no autoplay, progress, pause, stable paused playhead, seek/resume, content-start event, close/reopen and audio-only playback |
| iOS native playback | 13 checks passed on each of 3 cold launches, including fullscreen entry/exit callbacks |
| iOS zero-volume handling | Native debugger confirmed both the stored target volume and AVPlayer volume were zero |
| PGA app tests and startup | 64 tests passed across 15 suites; normal startup completed with playback stopped |
| PGA TypeScript baseline | 125 existing diagnostics, with no additions |

The final package differs from the Android-validated package only in three iOS source files. Temporary test components and entry points were removed after the checks.

PR preparation on **12 November 2026** confirmed that all nine changed native files match the tested artifact. The fork's package/Bob build and podspec syntax check pass. Its repository-wide lint command also scans generated Android test-report JavaScript and reports an existing formatting error in `scripts/bootstrap.js`; its full TypeScript command reports two existing `publisherProvidedID` mismatches in `example/src/App.tsx`. Those files and the JavaScript/TypeScript player API are unchanged by this upgrade.

Earlier integration checks on **29 October 2026** covered both Android landscape directions, continued playback, returning to portrait, exiting fullscreen while sideways, miniplayer restore and reopening another asset. Those historical checks do not replace manual regression on the final host build.

## Remaining regression

- Verify PGA from the published Git revision after removing the temporary tarball dependency.
- Exercise signed physical-device builds, background/foreground transitions, interruptions, lock-screen controls, PiP and AirPlay.
- Validate actual preroll/midroll delivery, multi-ad breaks, skip/error paths and return to content. A configured SSAI stream without delivered ads does not validate those paths.
- Test representative live/audio streams. The repeatable playback checks used a known playable on-demand asset, including for audio-only mode.
- Complete manual player UI regression: Android landscape in both directions, fullscreen exit, miniplayer restore, close/reopen, unavailable-content errors and Blaze coexistence. iPad/scene-based rotation remains separate QA.

These checks establish focused build and playback compatibility, not a complete feature audit or release sign-off. In particular, the fork's custom Android control layout does not automatically expose the SDK's newer quality-selector button.

## References

- [Brightcove Android release notes](https://sdks.support.brightcove.com/android/resources/brightcove-native-sdk-android-release-notes.html)
- [Brightcove iOS changelog](https://github.com/brightcove/brightcove-player-sdk-ios/blob/master/CHANGELOG.md)
- [Android IMA plugin 10.4.32 dependency metadata](https://repo.brightcove.com/releases/com/brightcove/player/android-ima-plugin/10.4.32/android-ima-plugin-10.4.32.pom)
- [Brightcove Core 7.2.25 podspec](https://github.com/brightcove/BrightcoveSpecs/blob/master/Brightcove-Player-Core/7.2.25/Brightcove-Player-Core.podspec)
