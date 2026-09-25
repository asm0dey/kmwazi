# Ponytail Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove four over-engineering findings from the kmwazi Android app: a single-impl settings interface, a redundant in-memory palette store, a hand-rolled shuffle, and dead DI/comment cruft.

**Architecture:** Pure cleanup. No new features. Collapse the `SettingsRepositoryInterface`/`DataStoreSettingsRepository` pair into one concrete `SettingsRepository`; delete `PaletteRepository` and read palette straight from DataStore's existing `paletteFlow()` (also fixes a latent bug — palette selection was only in-memory, never persisted across restart because nothing re-read it); replace Fisher–Yates with `java.util.Collections.shuffle`; drop a dead ServiceLocator field and a dangling comment.

**Tech Stack:** Kotlin, Jetpack Compose, AndroidX DataStore (Preferences), JUnit4 + kotlinx-coroutines-test, Gradle.

## Global Constraints

- Every `.kt` file starts with the existing 21-line GPL-3.0 header block — never remove or alter it when editing.
- Unit tests are JVM-only (no instrumentation). Run with `./gradlew :app:testDebugUnitTest`.
- Package root: `com.github.asm0dey.kmwazi`. Module: `app`.
- Compose UI wiring has no unit-test harness in this project; verify UI-only changes by a clean build (`./gradlew :app:assembleDebug`), not by unit test.

---

### Task 1: Collapse `SettingsRepositoryInterface` into concrete `SettingsRepository`

Single implementation, no fake — the tests use the real class against an in-memory DataStore. Delete the interface, rename the impl.

**Files:**
- Delete: `app/src/main/java/com/github/asm0dey/kmwazi/data/SettingsRepositoryInterface.kt`
- Rename + Modify: `app/src/main/java/com/github/asm0dey/kmwazi/data/DataStoreSettingsRepository.kt` → `.../data/SettingsRepository.kt`
- Modify: `app/src/main/java/com/github/asm0dey/kmwazi/di/ServiceLocator.kt`
- Rename + Modify (test): `app/src/test/java/com/github/asm0dey/kmwazi/data/DataStoreSettingsRepositoryTest.kt` → `.../data/SettingsRepositoryTest.kt`

**Interfaces:**
- Consumes: nothing new.
- Produces: `class SettingsRepository(dataStore: DataStore<Preferences>)` with secondary `constructor(context: Context)`. Public members unchanged: `paletteFlow(): Flow<Palette>`, `modeFlow(): Flow<Mode>`, `decisionTimeoutSecondsFlow(): Flow<Int>`, `suspend savePalette(Palette)`, `suspend saveMode(Mode)`, `suspend saveDecisionTimeoutSeconds(Int)`. `ServiceLocator.settingsRepository` now returns `SettingsRepository`.

- [ ] **Step 1: Baseline — run the settings tests, confirm they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.github.asm0dey.kmwazi.data.DataStoreSettingsRepositoryTest"`
Expected: PASS (this is a refactor; start green).

- [ ] **Step 2: Rename the implementation file**

```bash
git mv app/src/main/java/com/github/asm0dey/kmwazi/data/DataStoreSettingsRepository.kt \
       app/src/main/java/com/github/asm0dey/kmwazi/data/SettingsRepository.kt
```

- [ ] **Step 3: Delete the interface file**

```bash
git rm app/src/main/java/com/github/asm0dey/kmwazi/data/SettingsRepositoryInterface.kt
```

- [ ] **Step 4: Rewrite the class header in `SettingsRepository.kt`**

Change the doc comment + class declaration (lines 40–44) to drop the interface and the `override` on the class. Replace:

```kotlin
/**
 * DataStore-based implementation of SettingsRepositoryInterface.
 * Stores user preferences for palette, mode, and timeout settings.
 */
class DataStoreSettingsRepository(private val dataStore: DataStore<Preferences>) : SettingsRepositoryInterface {
    constructor(context: Context) : this(context.dataStore)
```

with:

```kotlin
/**
 * DataStore-based settings store for palette, mode, and timeout preferences.
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    constructor(context: Context) : this(context.dataStore)
```

- [ ] **Step 5: Remove the six `override` modifiers in `SettingsRepository.kt`**

There is no interface anymore. Delete the `override ` keyword (keep the rest of each signature) on all six members:

```kotlin
    fun paletteFlow(): Flow<Palette> =
    fun modeFlow(): Flow<Mode> =
    fun decisionTimeoutSecondsFlow(): Flow<Int> =
    suspend fun savePalette(palette: Palette) {
    suspend fun saveMode(mode: Mode) {
    suspend fun saveDecisionTimeoutSeconds(seconds: Int) {
```

- [ ] **Step 6: Update `ServiceLocator.kt` imports and types**

Replace the import line:

```kotlin
import com.github.asm0dey.kmwazi.data.DataStoreSettingsRepository
```

with:

```kotlin
import com.github.asm0dey.kmwazi.data.SettingsRepository
```

Delete this import line entirely:

```kotlin
import com.github.asm0dey.kmwazi.data.SettingsRepositoryInterface
```

Change the backing field:

```kotlin
    private var _settingsRepository: SettingsRepositoryInterface? = null
```

to:

```kotlin
    private var _settingsRepository: SettingsRepository? = null
```

Change the constructor call inside `initialize()`:

```kotlin
        _settingsRepository = DataStoreSettingsRepository(context.applicationContext)
```

to:

```kotlin
        _settingsRepository = SettingsRepository(context.applicationContext)
```

Change the getter type:

```kotlin
    val settingsRepository: SettingsRepositoryInterface
```

to:

```kotlin
    val settingsRepository: SettingsRepository
```

- [ ] **Step 7: Rename the test file and its class**

```bash
git mv app/src/test/java/com/github/asm0dey/kmwazi/data/DataStoreSettingsRepositoryTest.kt \
       app/src/test/java/com/github/asm0dey/kmwazi/data/SettingsRepositoryTest.kt
```

In `SettingsRepositoryTest.kt`, replace the three `DataStoreSettingsRepository` references:

```kotlin
class DataStoreSettingsRepositoryTest {
```
→
```kotlin
class SettingsRepositoryTest {
```

```kotlin
    private lateinit var repository: DataStoreSettingsRepository
```
→
```kotlin
    private lateinit var repository: SettingsRepository
```

```kotlin
        repository = DataStoreSettingsRepository(testDataStore)
```
→
```kotlin
        repository = SettingsRepository(testDataStore)
```

- [ ] **Step 8: Run the renamed test, confirm still green**

Run: `./gradlew :app:testDebugUnitTest --tests "com.github.asm0dey.kmwazi.data.SettingsRepositoryTest"`
Expected: PASS (same assertions, new names).

- [ ] **Step 9: Full unit-test run to catch stragglers**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL. (Catches any missed `SettingsRepositoryInterface` reference — should be none; grep `grep -rn SettingsRepositoryInterface app/src` must return nothing.)

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "refactor: collapse SettingsRepositoryInterface into SettingsRepository"
```

---

### Task 2: Delete `PaletteRepository`, read palette from DataStore

`PaletteRepository` is an in-memory `StateFlow` that mirrors DataStore's `paletteFlow()`, synced by `NavGraph`. It is a second source of truth and the reason palette selection is not truly reactive. Remove it; consumers read `settingsRepository.paletteFlow()` directly.

**Files:**
- Modify: `app/src/main/java/com/github/asm0dey/kmwazi/ui/Palette.kt` (remove `PaletteRepository` object)
- Modify: `app/src/main/java/com/github/asm0dey/kmwazi/ui/navigation/NavGraph.kt`
- Modify: `app/src/main/java/com/github/asm0dey/kmwazi/ui/screens/TouchScreen.kt`
- Modify: `app/src/main/java/com/github/asm0dey/kmwazi/ui/screens/SettingsScreen.kt`

**Interfaces:**
- Consumes: `SettingsRepository.paletteFlow(): Flow<Palette>` and `suspend savePalette(Palette)` from Task 1 (already existed; Task 1 only renamed the class).
- Produces: nothing new. Removes the symbol `PaletteRepository`.

- [ ] **Step 1: Remove the `PaletteRepository` object from `Palette.kt`**

Delete lines 30–38 (the object and its comment):

```kotlin
// Simple in-memory palette store for the current session
object PaletteRepository {
    private val _current = MutableStateFlow(Palettes.Vibrant)
    val current: StateFlow<Palette> get() = _current

    fun setPalette(palette: Palette) {
        _current.value = palette
    }
}
```

Then delete the now-unused imports at the top of `Palette.kt`:

```kotlin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
```

- [ ] **Step 2: Simplify `NavGraph.kt` — it only existed to feed `PaletteRepository`**

Delete the import:

```kotlin
import com.github.asm0dey.kmwazi.ui.PaletteRepository
```

Delete these lines from the body of `KmwaziNavHost` (lines 53–59):

```kotlin
    val settingsRepository = ServiceLocator.settingsRepository
    val palette by settingsRepository.paletteFlow().collectAsState(initial = com.github.asm0dey.kmwazi.ui.Palettes.Vibrant)

    // Apply saved palette as early as possible
    LaunchedEffect(palette) {
        PaletteRepository.setPalette(palette)
    }
```

Then delete the now-unused imports in `NavGraph.kt`:

```kotlin
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.github.asm0dey.kmwazi.di.ServiceLocator
```

- [ ] **Step 3: Point `TouchScreen.kt` at `paletteFlow()`**

Replace line 94:

```kotlin
    val palette = PaletteRepository.current.collectAsState().value
```

with:

```kotlin
    val palette by settingsRepository.paletteFlow().collectAsState(initial = Palettes.Vibrant)
```

Delete the import:

```kotlin
import com.github.asm0dey.kmwazi.ui.PaletteRepository
```

Add the import (grouped with the other `com.github.asm0dey.kmwazi.ui` imports):

```kotlin
import com.github.asm0dey.kmwazi.ui.Palettes
```

(`getValue`, `collectAsState`, and `settingsRepository` are already imported in this file.)

- [ ] **Step 4: Point `SettingsScreen.kt` at `paletteFlow()` and drop the mirror write**

Replace line 66:

```kotlin
    val current = PaletteRepository.current.collectAsState().value
```

with:

```kotlin
    val current by settingsRepository.paletteFlow().collectAsState(initial = Palettes.Vibrant)
```

In the dropdown `onClick` (lines 122–125), delete the mirror write so only DataStore is updated:

```kotlin
                            onClick = {
                                PaletteRepository.setPalette(palette)
                                scope.launch { settingsRepository.savePalette(palette) }
                                expandedState.value = false
                            },
```
→
```kotlin
                            onClick = {
                                scope.launch { settingsRepository.savePalette(palette) }
                                expandedState.value = false
                            },
```

Delete the import:

```kotlin
import com.github.asm0dey.kmwazi.ui.PaletteRepository
```

Add the `getValue` import (needed for the `by` delegate; `collectAsState` and `Palettes` are already imported):

```kotlin
import androidx.compose.runtime.getValue
```

- [ ] **Step 5: Verify `PaletteRepository` is fully gone**

Run: `grep -rn "PaletteRepository" app/src`
Expected: no output.

- [ ] **Step 6: Build to verify the Compose wiring compiles**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. (No unit test — this is Compose plumbing; the DataStore round-trip it depends on is already covered by `SettingsRepositoryTest.savePalette should update paletteFlow`.)

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor: read palette from DataStore, drop redundant PaletteRepository"
```

---

### Task 3: Replace hand-rolled Fisher–Yates with `Collections.shuffle`

`SecureRandomProvider.shuffle` reimplements a shuffle the JDK already ships. `SecureRandom` is a `java.util.Random`, so `Collections.shuffle(list, rng)` works directly.

**Files:**
- Modify: `app/src/main/java/com/github/asm0dey/kmwazi/domain/SecureRandomProvider.kt`
- Create (test): `app/src/test/java/com/github/asm0dey/kmwazi/domain/SecureRandomProviderTest.kt`

**Interfaces:**
- Consumes: `RandomProvider` (unchanged).
- Produces: `SecureRandomProvider().shuffle(list)` still returns a permutation of `list` (same contract, stdlib impl).

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/github/asm0dey/kmwazi/domain/SecureRandomProviderTest.kt` (copy the 21-line GPL header from any existing `.kt` file into the top, then):

```kotlin
package com.github.asm0dey.kmwazi.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SecureRandomProviderTest {
    @Test
    fun `shuffle returns a permutation of the input`() {
        val provider = SecureRandomProvider()
        val input = (1L..100L).toList()

        val result = provider.shuffle(input)

        assertEquals("shuffle must preserve every element", input.toSet(), result.toSet())
        assertEquals("shuffle must not change size", input.size, result.size)
    }

    @Test
    fun `nextInt stays within bound`() {
        val provider = SecureRandomProvider()
        repeat(1000) {
            val n = provider.nextInt(10)
            assert(n in 0 until 10) { "nextInt(10) returned $n" }
        }
    }
}
```

- [ ] **Step 2: Run it against the current (hand-rolled) impl — should PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "com.github.asm0dey.kmwazi.domain.SecureRandomProviderTest"`
Expected: PASS. (The test pins the *contract*; it must hold both before and after the swap. This is the guard against the refactor breaking behavior.)

- [ ] **Step 3: Swap the implementation**

In `SecureRandomProvider.kt`, add the import next to the existing `import java.security.SecureRandom`:

```kotlin
import java.util.Collections
```

Replace the `shuffle` method (lines 35–45):

```kotlin
    override fun <T> shuffle(list: List<T>): List<T> {
        val mutable = list.toMutableList()
        // Fisher-Yates shuffle
        for (i in mutable.indices.reversed()) {
            val j = nextInt(i + 1)
            val temp = mutable[i]
            mutable[i] = mutable[j]
            mutable[j] = temp
        }
        return mutable
    }
```

with:

```kotlin
    override fun <T> shuffle(list: List<T>): List<T> =
        list.toMutableList().also { Collections.shuffle(it, rng) }
```

- [ ] **Step 4: Run the test again — still PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "com.github.asm0dey.kmwazi.domain.SecureRandomProviderTest"`
Expected: PASS (contract preserved with the stdlib impl).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/github/asm0dey/kmwazi/domain/SecureRandomProvider.kt \
        app/src/test/java/com/github/asm0dey/kmwazi/domain/SecureRandomProviderTest.kt
git commit -m "refactor: use Collections.shuffle instead of hand-rolled Fisher-Yates"
```

---

### Task 4: Drop dead ServiceLocator field and dangling comment

`_randomProvider` is stored as a field but never exposed — it only feeds `ResultEngine`. Make it a local. Also delete a dangling comment in `TouchViewModel`.

**Files:**
- Modify: `app/src/main/java/com/github/asm0dey/kmwazi/di/ServiceLocator.kt`
- Modify: `app/src/main/java/com/github/asm0dey/kmwazi/viewmodel/TouchViewModel.kt`

**Interfaces:**
- Consumes: nothing new. (Depends on Task 1 having renamed the settings type in this file already.)
- Produces: no public API change.

- [ ] **Step 1: Make `_randomProvider` a local val in `ServiceLocator.kt`**

Delete the field declaration:

```kotlin
    private var _randomProvider: RandomProvider? = null
```

Change `initialize()` from:

```kotlin
    fun initialize(context: Context) {
        _randomProvider = SecureRandomProvider()
        _resultEngine = ResultEngine(_randomProvider!!)
        _settingsRepository = SettingsRepository(context.applicationContext)
    }
```

to:

```kotlin
    fun initialize(context: Context) {
        val randomProvider = SecureRandomProvider()
        _resultEngine = ResultEngine(randomProvider)
        _settingsRepository = SettingsRepository(context.applicationContext)
    }
```

Delete the now-unused import:

```kotlin
import com.github.asm0dey.kmwazi.domain.RandomProvider
```

- [ ] **Step 2: Delete the dangling comment in `TouchViewModel.kt`**

In the `companion object` (lines 134–138), remove the trailing orphan comment:

```kotlin
    companion object {
        private const val TICK_MS = 100L

        // Long-press duration to reset when result is shown
    }
```
→
```kotlin
    companion object {
        private const val TICK_MS = 100L
    }
```

- [ ] **Step 3: Build + full unit test run**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL. (`TouchViewModelTest` exercises `ServiceLocator`-independent VM paths; the ResultEngine wiring is verified by the app building.)

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/github/asm0dey/kmwazi/di/ServiceLocator.kt \
        app/src/main/java/com/github/asm0dey/kmwazi/viewmodel/TouchViewModel.kt
git commit -m "chore: inline ServiceLocator random provider, drop dead comment"
```

---

## Not in scope

- `TouchEventListener` (5-method interface, one anonymous impl at `TouchScreen.kt:202`) — flagged low-priority in review, kept: the callback grouping is defensible and converting to five lambdas is a wash. Leave it.

## Final verification

- [ ] Run: `./gradlew :app:testDebugUnitTest && ./gradlew :app:assembleDebug`
      Expected: BUILD SUCCESSFUL for both.
- [ ] Run: `grep -rn "SettingsRepositoryInterface\|PaletteRepository" app/src`
      Expected: no output.
