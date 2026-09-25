# Releasing Kmwazi

1. Bump `versionCode` / `versionName` in `app/build.gradle.kts` and add a `CHANGELOG.md` entry.
2. `bundle exec fastlane test` — must pass (ktlint, license headers, Android lint, all tests, ArchUnit).
3. Manual checks on a real phone (`./gradlew :app:installRelease`):
   - 5+ fingers: every finger gets a circle; the countdown restarts when a finger is added or lifted.
   - Result appears after the configured timeout; the overlay grows and fades out in about a second.
   - Lift everyone, touch again: a new round starts.
   - Tapping the mode button, Reset or ✕ never adds a circle.
   - Groups mode: circles gray before the result, group numbers readable after; Order: numbers readable.
   - Change mode while fingers are down: the round resets.
   - Rotate on the touch screen: no crash.
   - Rotate with fingers down: the round resets, no result is dealt for fingers nobody holds.
4. Upgrade check (only when settings code changed): install the previous Play version, choose Pastel,
   Groups of 4 and 7 s, then install the new build over it — all three must survive.
5. Fresh-install check: uninstall, install, change a setting, kill and reopen — the setting is kept.
6. `bundle exec fastlane beta` (internal track), smoke-test, then `bundle exec fastlane deploy`.
