# desktopApp is a dev harness, not a product

`desktopApp` runs the shared UI in a window where held keys act as Fingers, so the game can be play-tested without a phone. It is never packaged or released, and tests do not depend on it (they run in `shared/desktopTest`). Do not add release tasks, installers or desktop-only features to it.
