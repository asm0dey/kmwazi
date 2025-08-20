# Requirements for Chwazi-like Android Desktop Application

## 1. Overview
The application is a touch-based Android desktop app inspired by Chwazi, designed to randomly select players, form groups, or define a play order based on finger touches. The app detects multiple finger inputs, waits for stabilization, and animates the final result.

## 2. Functional Requirements

### 2.1 Touch Detection
- The app must detect multiple simultaneous finger touches on the screen (up to 10 fingers).
- The app must wait for the number of finger touches to stabilize (no new touches or removals) for 4 seconds before proceeding to the result.
- If a finger is added or removed during the stabilization period, the 4-second timer resets.

### 2.2 Modes of Operation
The app must support three distinct modes, selectable via a user interface:

#### 2.2.1 Choose One Player
- Randomly select one finger (representing a player) from the detected touches.
- Display the selected player with a clear visual indication (e.g., highlighting the finger's position).

#### 2.2.2 Split into Groups
- Allow the user to specify the number of people per group (n) via an input field (1 to total number of detected fingers).
- Randomly assign detected fingers into groups of n people.
- If the number of fingers is not perfectly divisible by n, the remaining fingers form a smaller group or are left ungrouped, as appropriate.
- Display each group with distinct visual indicators (e.g., different colors or shapes).

#### 2.2.3 Define Play Order
- Assign a random order to all detected fingers (representing players).
- Display the order numerically or sequentially (e.g., "Player 1," "Player 2," etc.) next to each finger's position.

### 2.3 Color Palettes
- Provide at least 3 distinct color palettes for visual customization (e.g., vibrant, pastel, monochrome).
- Each palette must include enough colors to differentiate up to 10 fingers or groups.
- Allow users to select a palette via a settings menu before starting a session.
- Ensure color contrast complies with WCAG 2.1 accessibility guidelines for visibility.

### 2.4 Animation
- After the 4-second stabilization period, animate the transition to the final result based on the selected mode:
    - **Choose One Player**: Highlight the selected finger with a fade-in effect, dimming others.
    - **Split into Groups**: Grouped fingers are visually clustered or outlined with group-specific colors, animated with a smooth transition (e.g., sliding or glowing effect).
    - **Define Play Order**: Display numbers sequentially with a pop-up or scaling animation next to each finger.
- Animations must last between 1-2 seconds and be smooth (targeting 60 FPS).

## 3. Non-Functional Requirements

### 3.1 Platform
- Target Android desktop (Android 5.0 Lollipop or later).
- Optimize for tablet-sized screens (7-inch or larger) to accommodate multiple simultaneous touches.

### 3.2 Performance
- Handle up to 10 simultaneous touches with no noticeable lag (<100ms response time).
- Ensure animations run smoothly on mid-range Android devices (e.g., 2GB RAM, quad-core processor).

### 3.3 Usability
- Provide a simple, intuitive interface with minimal setup steps.
- Include a brief tutorial or help screen explaining how to use each mode.
- Support touch-based navigation (no reliance on hardware buttons).

### 3.4 Accessibility
- Ensure high contrast for all visual elements (WCAG 2.1 Level AA compliance).
- Provide haptic feedback (vibration) when fingers are detected and when results are displayed.
- Support screen reader compatibility for mode selection and result announcement.

## 4. Technical Requirements

### 4.1 Input Handling
- Use Android’s MultiTouch API to detect and track finger positions.
- Implement a timer to monitor touch stabilization (4 seconds).

### 4.2 Randomization
- Use a cryptographically secure random number generator (e.g., Java’s SecureRandom) for fair selection, grouping, and ordering.

### 4.3 UI Framework
- Use Android’s Jetpack Compose for a modern, reactive UI.
- Alternatively, use XML layouts with View system if targeting older devices.

### 4.4 Animation
- Leverage Android’s Animation API or Jetpack Compose animations for smooth transitions.
- Ensure animations are lightweight to maintain performance on low-end devices.

### 4.5 Storage
- Save user preferences (e.g., selected color palette) using SharedPreferences.
- No persistent storage of touch data or results is required.

## 5. Constraints
- The app must function without internet connectivity (offline mode).
- No external hardware (e.g., cameras) or additional sensors beyond the touchscreen are required.
- The app must not store sensitive user data or require permissions beyond touch input and vibration.

## 6. Assumptions
- Users are familiar with touch-based apps and can place fingers on the screen simultaneously.
- The Android device supports multi-touch input with at least 10 points.
- Users will select a mode before placing fingers on the screen.

## 7. Deliverables
- Android APK compatible with Android 5.0+.
- Source code with documentation for touch detection, mode logic, and animation implementation.
- User guide explaining mode selection, finger placement, and result interpretation.