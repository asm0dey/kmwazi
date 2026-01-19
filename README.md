# Kmwazi

Kmwazi is a multi-touch randomizer app for Android, inspired by Chwazi. It helps you quickly and fairly choose individuals, form groups, or determine play order using finger touches on your screen.

![Kmwazi Icon](app/src/main/ic_launcher-playstore.png)

## Features

- Choose One: Randomly selects a single person from the group.
- Split into Groups: Divides participants into groups of a specified size.
- Define Order: Assigns a random sequence to all participants.
- Customizable stabilization timeout (1 to 10 seconds).
- Multiple color palettes for visual variety.
- High-contrast, accessible design.

## How to Use

### Getting Started
1. Launch the app and tap "Start" on the home screen.
2. Select your desired mode at the top of the screen: "Choose One", "Groups", or "Order".
   - For "Groups" mode, use the plus and minus buttons to set the number of people per group.

### Making a Selection
1. Have everyone place one finger on the screen.
2. Hold your fingers still. The app will start a countdown as soon as the touches stabilize.
3. If anyone adds or removes a finger, the countdown will reset.
4. Once the countdown finishes, the result will be displayed with animations and colors.
5. To start a new round, everyone should lift their fingers.

### Settings
- Access the Settings from the home screen to:
  - Change the color palette.
  - Adjust the decision timeout (the time fingers must remain still before a selection is made).

## Installation

This is an Android application. You can build it from source using Android Studio and Gradle.

```bash
./gradlew assembleDebug
```

## License

Kmwazi is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

See the [LICENSE](LICENSE) file for more details.
