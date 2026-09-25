# Kmwazi

Kmwazi is a multi-touch randomizer app for Android, inspired by Chwazi. It helps you quickly and fairly choose individuals, form groups, or determine play order using finger touches on your screen.

![Kmwazi Icon](app/src/main/ic_launcher-playstore.png)

## Features

- Choose One: Randomly selects a single person from the group.
- Split into Groups: Divides participants into groups of a specified size (2–10); the last group gets whoever is left over.
- Define Order: Assigns a random sequence to all participants.
- Customizable stabilization timeout (1 to 10 seconds).
- Multiple color palettes for visual variety.
- High-contrast, accessible design.

## How to Use

### Getting Started
1. Launch the app and tap "Start" on the home screen.
2. Tap the mode button in the top-left corner and pick "Choose One", "Order", or "Groups".
   - For "Groups" mode, use the plus and minus buttons in the same sheet to set the number of people per group (for example, 4 people in groups of 3 make one group of 3 and one of 1).

### Making a Selection
1. Have everyone place one finger on the screen.
2. Keep your fingers on the screen. The countdown starts as soon as the set of fingers stops changing.
3. If anyone adds or removes a finger, the countdown will reset.
4. Once the countdown finishes, the result is shown: the chosen finger keeps its colour, groups get a shared colour and a group number, and the order is shown as numbers.
5. To start a new round, everyone lifts their fingers and touches again, or tap "Reset".

### Settings
- Access the Settings from the home screen to:
  - Change the color palette.
  - Adjust the decision timeout (how long the same set of fingers must stay down before a selection is made).

## Project layout

- `shared/` — all app code (Compose Multiplatform): `round/` (pure game rules), `Settings`, `RoundViewModel`, `ui/`.
- `app/` — Android entry point (the published app).
- `desktopApp/` — desktop window for development; not released.

## Development

- Run on desktop: `./gradlew :desktopApp:run` — hold keyboard keys to add fingers at random spots (the mouse is one more finger); Esc goes back.
- Build Android: `./gradlew :app:assembleDebug`
- All checks: `./gradlew check` (or `bundle exec fastlane test`)
- Releasing: see `RELEASING.md`.
- App icon: edit the SVG masters in `art/icon/`, then run `art/icon/render.sh` to regenerate every raster.

### Fastlane

This project uses [fastlane](https://fastlane.tools/) to automate testing and deployment.

To get started:
1. Install Ruby (if not already installed).
2. Install dependencies:
   ```bash
   bundle install
   ```

Available lanes:
- `bundle exec fastlane test`: Runs all checks (ktlint, license headers, Android lint, tests, architecture rules) and fails if the tree is dirty.
- `bundle exec fastlane beta`: Builds the release APK and uploads it to the Play Store Internal track.
- `bundle exec fastlane deploy`: Builds the release App Bundle and uploads it to the Play Store Production track.

Note: Deployment lanes require a valid Google Play Service Account JSON key, which should be configured in `fastlane/Appfile` or via environment variables.

## License

Kmwazi is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

See the [LICENSE](LICENSE) file for more details.
