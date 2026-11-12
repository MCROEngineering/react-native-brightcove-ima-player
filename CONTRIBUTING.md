# Contributing

We want this community to be friendly and respectful to each other. Please follow it in all your interactions with the project.

## Development workflow

To get started with the project, run `yarn` in the root directory to install the required dependencies for each package:

```sh
yarn
```

> While it's possible to use [`npm`](https://github.com/npm/cli), the tooling is built around [`yarn`](https://classic.yarnpkg.com/), so you'll have an easier time if you use `yarn` for development.

While developing, you can run the [example app](/example/) to test your changes. Any changes you make in your library's JavaScript code will be reflected in the example app without a rebuild. If you change any native code, then you'll need to rebuild the example app.

To start the packager:

```sh
yarn example start
```

To run the example app on Android:

```sh
yarn example android
```

To run the example app on iOS:

```sh
yarn example ios
```

Make sure your code passes TypeScript and ESLint. Run the following to verify:

```sh
yarn typescript
yarn lint
```

To fix formatting errors, run the following:

```sh
yarn lint --fix
```

Remember to add tests for your change if possible. Run the unit tests by:

```sh
yarn test
```

To edit the Objective-C files, open `example/ios/BrightcoveImaPlayerExample.xcworkspace` in XCode and find the source files at `Pods > Development Pods > react-native-brightcove-ima-player`.

To edit the Kotlin files, open `example/android` in Android studio and find the source files at `brightcoveimaplayer` under `Android`.

### Android source-loading regression checks

Source requests are committed after the view manager applies a complete prop transaction. This keeps the video request and its ad configuration together when Fabric applies props in a different order. Volume is retained until the native player exists, and playback controls are initialized even when the view is created after host resume.

Use this checklist for native regression QA in a host app with the new architecture enabled:

1. Open a real Brightcove video with SSAI settings. Confirm visible video and advancing native progress after the ready/play events.
2. Pause, resume, seek and enter/exit fullscreen. Open the captions dialog on a video with a caption track.
3. Seek near the end and verify the end event and playback of the next video.
4. Close during playback and reopen. Also destroy/close while a source request is pending; an old catalog response must not restart the disposed player.
5. Change the video or ad configuration, including removing `adConfigId`, and confirm that the updated request is used. Reapplying unchanged source props must not restart playback.
6. Check native logs for source-selection, early-volume and fullscreen initialization exceptions. Repeat on the legacy architecture before relying on an architecture rollback.

These checks exercise native SDK behavior that the JavaScript component snapshot test cannot cover. Full ad delivery, PiP, lock-screen controls and physical-device regression still require suitable streams and devices.

### Local host installation

Use an npm tarball when testing the fork in a separate host app:

```sh
npm pack
# In the host app, install the resulting .tgz using a local file dependency.
```

Check that the tarball excludes `node_modules`. Yarn Classic's `file:../folder` installation can copy the fork's development dependencies, including its older React and React Native. Metro can then bundle both runtimes and fail with `PlatformConstants could not be found`; player and poster exports can subsequently be undefined. A packaged tarball uses the host's peer dependencies. After publishing the reviewed Git revision, replace the local tarball dependency and verify an installation from that revision.

### iOS playback prop regression checks

Cold-launch the host before the first mount, with both `autoPlay={false}` and `play={false}`, `settings.autoPlay=false`, and `volume={0}`. Verify ready and poster rendering without playback, then exercise imperative play, pause, seek, fullscreen, close and audio-only reopening. Repeat after another cold launch: a warm reopening alone does not detect the initial prop-default read.

The `play` prop must have an explicit custom setter. React Native reads a property's getter while caching its initial value; using the imperative `-play` command as that getter starts playback even when the incoming JSON value is false. Keep volume initialization in the view initializer so later settings application preserves a volume prop already applied by React Native.

### Android content start and landscape regression checks

`onVideoPlay` means that main content is playing, once per loaded source. Do not infer it from `AD_COMPLETED`: a configured SSAI session may have no ads, and an individual ad can finish before the rest of its pod. `ContentPlaybackListener` waits for playing progress and, for SSAI, checks the SDK timeline against the video display's absolute stream position. The view's content-relative playhead cannot identify an ad's position in that timeline.

Run the native event tests from an Android host that links this library:

```sh
./gradlew :react-native-brightcove-ima-player:testDebugUnitTest
```

The tests cover no-ad SSAI, delayed metadata, a multi-ad preroll, skipping into content, non-SSAI playback, pause/resume, source changes and late events after disposal. They simulate SDK events and timeline positions; they do not replace testing actual ad delivery.

In the rebuilt host app, play an SSAI-configured video that returns no ads, rotate to both landscape orientations and verify fullscreen video keeps advancing. Return to portrait, exit fullscreen while sideways, rotate again, minimise/restore, then close and play a different video. Repeat with a stream that serves a preroll: the content-start callback must not arrive while ads are playing. Keep device audio muted during automated checks.

### Gradle 9 host builds

Use `mavenCentral()` for the standard Maven repository. Gradle 9 removes `jcenter()`, including calls in a library's own repositories block, so the old declaration prevents Expo 55 hosts from configuring the player project. Keep the Brightcove-specific repositories in place. Verify configuration and a native Debug build before running the Android player checklist above.

### iOS bridgeless queue regression checks

Both iOS view managers inherit `RCTViewManager`'s `methodQueue`. Do not override it with `bridge.uiManager.methodQueue`: the bridgeless UI manager proxy does not implement that getter. Returning nil makes React Native try to assign a fallback queue through a missing setter/ivar and shows a native Debug error on first mount. Inheriting the superclass also retains the UI manager queue on the legacy architecture.

Rebuild the native host app after changing these managers. In a Debug build with the new architecture enabled:

1. Cold-launch, then mount a player and a poster with valid catalog settings. Neither manager should log a missing `methodQueue` setter/ivar or an unsupported proxy selector.
2. Verify ready, play and advancing progress events. Pause, resume and seek to confirm native commands still reach the view on the correct queue.
3. Enter and exit fullscreen, then close during playback. Reopen and confirm a fresh playable instance; also exercise explicit stop/dispose followed by unmount.
4. Confirm the poster renders and can unmount/reopen without a queue error. Repeat the host smoke check on Android when changing the local package link.

The JavaScript test placeholder does not execute Objective-C module initialization. These checks require a rebuilt native host; there is no native XCTest target in this repository.

### Commit message convention

We follow the [conventional commits specification](https://www.conventionalcommits.org/en) for our commit messages:

- `fix`: bug fixes, e.g. fix crash due to deprecated method.
- `feat`: new features, e.g. add new method to the module.
- `refactor`: code refactor, e.g. migrate from class components to hooks.
- `docs`: changes into documentation, e.g. add usage example for the module..
- `test`: adding or updating tests, e.g. add integration tests using detox.
- `chore`: tooling changes, e.g. change CI config.

Our pre-commit hooks verify that your commit message matches this format when committing.

### Linting and tests

[ESLint](https://eslint.org/), [Prettier](https://prettier.io/), [TypeScript](https://www.typescriptlang.org/)

We use [TypeScript](https://www.typescriptlang.org/) for type checking, [ESLint](https://eslint.org/) with [Prettier](https://prettier.io/) for linting and formatting the code, and [Jest](https://jestjs.io/) for testing.

Our pre-commit hooks verify that the linter and tests pass when committing.

### Publishing to npm

We use [release-it](https://github.com/release-it/release-it) to make it easier to publish new versions. It handles common tasks like bumping version based on semver, creating tags and releases etc.

To publish new versions, run the following:

```sh
yarn release
```

### Scripts

The `package.json` file contains various scripts for common tasks:

- `yarn bootstrap`: setup project by installing all dependencies and pods.
- `yarn typescript`: type-check files with TypeScript.
- `yarn lint`: lint files with ESLint.
- `yarn test`: run unit tests with Jest.
- `yarn example start`: start the Metro server for the example app.
- `yarn example android`: run the example app on Android.
- `yarn example ios`: run the example app on iOS.

### Sending a pull request

> **Working on your first pull request?** You can learn how from this _free_ series: [How to Contribute to an Open Source Project on GitHub](https://app.egghead.io/playlists/how-to-contribute-to-an-open-source-project-on-github).

When you're sending a pull request:

- Prefer small pull requests focused on one change.
- Verify that linters and tests are passing.
- Review the documentation to make sure it looks good.
- Follow the pull request template when opening a pull request.
- For pull requests that change the API or implementation, discuss with maintainers first by opening an issue.

## Code of Conduct

### Our Pledge

We as members, contributors, and leaders pledge to make participation in our community a harassment-free experience for everyone, regardless of age, body size, visible or invisible disability, ethnicity, sex characteristics, gender identity and expression, level of experience, education, socio-economic status, nationality, personal appearance, race, religion, or sexual identity and orientation.

We pledge to act and interact in ways that contribute to an open, welcoming, diverse, inclusive, and healthy community.

### Our Standards

Examples of behavior that contributes to a positive environment for our community include:

- Demonstrating empathy and kindness toward other people
- Being respectful of differing opinions, viewpoints, and experiences
- Giving and gracefully accepting constructive feedback
- Accepting responsibility and apologizing to those affected by our mistakes, and learning from the experience
- Focusing on what is best not just for us as individuals, but for the overall community

Examples of unacceptable behavior include:

- The use of sexualized language or imagery, and sexual attention or
  advances of any kind
- Trolling, insulting or derogatory comments, and personal or political attacks
- Public or private harassment
- Publishing others' private information, such as a physical or email
  address, without their explicit permission
- Other conduct which could reasonably be considered inappropriate in a
  professional setting

### Enforcement Responsibilities

Community leaders are responsible for clarifying and enforcing our standards of acceptable behavior and will take appropriate and fair corrective action in response to any behavior that they deem inappropriate, threatening, offensive, or harmful.

Community leaders have the right and responsibility to remove, edit, or reject comments, commits, code, wiki edits, issues, and other contributions that are not aligned to this Code of Conduct, and will communicate reasons for moderation decisions when appropriate.

### Scope

This Code of Conduct applies within all community spaces, and also applies when an individual is officially representing the community in public spaces. Examples of representing our community include using an official e-mail address, posting via an official social media account, or acting as an appointed representative at an online or offline event.

### Enforcement

Instances of abusive, harassing, or otherwise unacceptable behavior may be reported to the community leaders responsible for enforcement at [INSERT CONTACT METHOD]. All complaints will be reviewed and investigated promptly and fairly.

All community leaders are obligated to respect the privacy and security of the reporter of any incident.

### Enforcement Guidelines

Community leaders will follow these Community Impact Guidelines in determining the consequences for any action they deem in violation of this Code of Conduct:

#### 1. Correction

**Community Impact**: Use of inappropriate language or other behavior deemed unprofessional or unwelcome in the community.

**Consequence**: A private, written warning from community leaders, providing clarity around the nature of the violation and an explanation of why the behavior was inappropriate. A public apology may be requested.

#### 2. Warning

**Community Impact**: A violation through a single incident or series of actions.

**Consequence**: A warning with consequences for continued behavior. No interaction with the people involved, including unsolicited interaction with those enforcing the Code of Conduct, for a specified period of time. This includes avoiding interactions in community spaces as well as external channels like social media. Violating these terms may lead to a temporary or permanent ban.

#### 3. Temporary Ban

**Community Impact**: A serious violation of community standards, including sustained inappropriate behavior.

**Consequence**: A temporary ban from any sort of interaction or public communication with the community for a specified period of time. No public or private interaction with the people involved, including unsolicited interaction with those enforcing the Code of Conduct, is allowed during this period. Violating these terms may lead to a permanent ban.

#### 4. Permanent Ban

**Community Impact**: Demonstrating a pattern of violation of community standards, including sustained inappropriate behavior, harassment of an individual, or aggression toward or disparagement of classes of individuals.

**Consequence**: A permanent ban from any sort of public interaction within the community.

### Attribution

This Code of Conduct is adapted from the [Contributor Covenant][homepage], version 2.0,
available at https://www.contributor-covenant.org/version/2/0/code_of_conduct.html.

Community Impact Guidelines were inspired by [Mozilla's code of conduct enforcement ladder](https://github.com/mozilla/diversity).

[homepage]: https://www.contributor-covenant.org

For answers to common questions about this code of conduct, see the FAQ at
https://www.contributor-covenant.org/faq. Translations are available at https://www.contributor-covenant.org/translations.
