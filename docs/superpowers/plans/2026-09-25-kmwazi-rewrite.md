# Kmwazi Rewrite Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rewrite Kmwazi from scratch as a Compose Multiplatform app (Android shipped + desktop JVM for dev/tests) with the same UX, fixed bugs, a pure testable round reducer, and architecture enforced by tests.

**Architecture:** `:shared` (KMP: Android library + `jvm("desktop")`) holds everything: a pure `round` package (reducer + dealing), `Settings` (multiplatform DataStore), `RoundViewModel` (timer), `App` and stateless `ui` screens. `:app` is a thin Android shell (Application + MainActivity). `:desktopApp` is a dev-only window. All tests run on the desktop JVM target with Kotest.

**Tech Stack:** Kotlin 2.4.20, AGP 9.4.1, Gradle 9.8.0, Compose Multiplatform 1.12.1 (material3 1.9.0), JetBrains lifecycle 2.11.0, DataStore 1.2.1, okio 3.18.2, kotlinx-coroutines 1.11.0, Kotest 6.2.5, ArchUnit 1.5.1, ktlint Gradle plugin 14.2.0, license-header plugin 1.0.1.

**Spec:** `docs/superpowers/specs/2026-09-25-kmwazi-rewrite-design.md`

## Global Constraints

- Versions: latest **stable** only (no alpha/beta/RC) — exact values in Task 2's catalog.
- `applicationId = "com.github.asm0dey.kmwazi"`; `versionCode = 4`; `versionName = "2.0.0"`; `minSdk = 23`; `compileSdk = 37`; `targetSdk = 37`.
- Settings file on Android: `filesDir/datastore/settings.preferences_pb`. Keys unchanged: `mode` (string), `group_size` (int), `palette_name` (string), `decision_timeout_sec` (int). Mode values unchanged: `"ChooseOne"`, `"DefineOrder"`, `"groups"`.
- Group size range `2..10`; timeout range `1..10` seconds; defaults: Vibrant, ChooseOne, 3 s, group size 2.
- Every new `.kt` file starts with the license block from `HEADER` wrapped as a `/* ... */` comment, formatted exactly like the existing files (e.g. `app/src/main/java/com/github/asm0dey/kmwazi/MainActivity.kt` lines 1–21). Code blocks below omit it; `./gradlew applyLicenseHeader` adds it if you forget, and `git diff --exit-code` after `check` must be clean.
- Package root: `com.github.asm0dey.kmwazi`. Compose resources class package: `com.github.asm0dey.kmwazi.resources`.
- Strings: en/de/ru copied verbatim from `app/src/main/res/values{,-de,-ru}/strings.xml`.
- ktlint must fail the build (`ignoreFailures = false`).
- No DI container, no navigation library, no `expect`/`actual`.
- Commits end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. A touch that starts on a button (mode, Reset, ✕) must not become a finger or re-arm the countdown — pinned in Task 8 (`TouchScreenTest`).
2. More fingers than palette colours (11+) — every finger drawn, colours wrap — pinned in Task 4 (`RoundTest`), Task 5 (`PaletteTest`), Task 8 (`TouchScreenTest`).
3. Changing mode while a countdown runs must never deal a result for the old round — pinned in Task 4 (`RoundTest`).
4. A corrupted settings file must read as defaults, not crash — pinned in Task 6 (`SettingsTest`).
5. Fewer fingers than the group size (e.g. 2 fingers, groups of 4) — one group with everyone — pinned in Task 3 (`DealTest`).

---

### Task 1: Remove intent-integrity-kit and tessl artifacts

**Files:**
- Delete: `CONSTITUTION.md`, `TECHNICAL_DEBT.md`, `docs/plan.md`, `docs/requirements.md`, `tessl.json`, `.tessl/`, `.mcp.json`, `AGENTS.md`, `CLAUDE.md`
- Delete: iikit skill entries under `.claude/skills/`, `.codex/skills/`, `.gemini/skills/`, and untracked `.agents/`, `.cursor/skills/`
- Keep: `.claude/settings.local.json`, `.codex/config.toml`, `.gemini/settings.json`, `docs/superpowers/`, `.superpowers/`

**Interfaces:** none.

- [ ] **Step 1: Inspect what will be removed**

```bash
git status --short | grep -iE 'tessl|iikit|\.agents|\.cursor' | head -50
ls .claude/skills .codex/skills .gemini/skills .agents .cursor 2>/dev/null
```

Expected: only `tessl:iikit-*` / `tessl__iikit-*` entries in the skills dirs. If anything else is listed there, stop and ask the author before deleting it.

- [ ] **Step 2: Delete**

```bash
git rm -r -q -f --ignore-unmatch CONSTITUTION.md TECHNICAL_DEBT.md docs/plan.md docs/requirements.md \
  tessl.json .tessl .mcp.json AGENTS.md CLAUDE.md \
  .claude/skills .codex/skills .gemini/skills
rm -rf .tessl .claude/skills .codex/skills .gemini/skills .agents .cursor/skills
rmdir .cursor 2>/dev/null || true
```

- [ ] **Step 3: Verify nothing references iikit or tessl**

Run: `grep -rIl -iE 'iikit|intent-integrity|tessl' --exclude-dir=.git --exclude-dir=build --exclude-dir=.gradle --exclude-dir=superpowers . || echo clean`
Expected: `clean`

- [ ] **Step 4: Commit**

```bash
git add -A CONSTITUTION.md TECHNICAL_DEBT.md docs tessl.json .tessl .mcp.json AGENTS.md CLAUDE.md .claude .codex .gemini
git commit -m "chore: remove intent-integrity-kit and tessl artifacts

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Toolchain, version catalog and `:shared` skeleton

Old `:app` keeps building from its old code until Task 10; this task only adds `:shared` and modernises the build.

**Files:**
- Modify: `gradle/wrapper/gradle-wrapper.properties` (via wrapper task), `gradle/libs.versions.toml`, `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `lint.xml`, `renovate.json`
- Delete: `versions.properties`
- Create: `shared/build.gradle.kts`, `shared/src/desktopTest/kotlin/com/github/asm0dey/kmwazi/SmokeTest.kt`

**Interfaces:**
- Produces: catalog aliases used by every later task (listed in Step 2); module `:shared` with source sets `commonMain`, `desktopMain`, `desktopTest`; test command `./gradlew :shared:desktopTest`.

- [ ] **Step 1: Upgrade Gradle wrapper**

```bash
./gradlew wrapper --gradle-version 9.8.0 && ./gradlew wrapper --gradle-version 9.8.0
```

Expected: `gradle-wrapper.properties` has `gradle-9.8.0-bin.zip`.

- [ ] **Step 2: Rewrite `gradle/libs.versions.toml`**

Old `:app` entries stay until Task 10 (marked `# old app`). `lifecycle-viewmodel-compose` switches to the JetBrains artifact now (its Android variant is the androidx one, so the old app still compiles).

```toml
[versions]
agp = "9.4.1"
kotlin = "2.4.20"
compose-multiplatform = "1.12.1"
compose-material3 = "1.9.0"
androidx-activity = "1.13.0"
coreKtx = "1.19.1"
jetbrains-lifecycle = "2.11.0"
androidx-datastore = "1.2.1"
okio = "3.18.2"
kotlinx-coroutines = "1.11.0"
kotest = "6.2.5"
archunit = "1.5.1"
ktlint = "14.2.0"
# old app
junit = "4.13.2"
junitVersion = "1.3.0"
espressoCore = "3.7.0"
appcompat = "1.7.1"
androidx-compose = "2026.06.01"
google-android-material = "1.13.0"
androidx-navigation = "2.9.8"
app-cash-turbine = "1.2.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "androidx-activity" }
compose-runtime = { module = "org.jetbrains.compose.runtime:runtime", version.ref = "compose-multiplatform" }
compose-foundation = { module = "org.jetbrains.compose.foundation:foundation", version.ref = "compose-multiplatform" }
compose-ui = { module = "org.jetbrains.compose.ui:ui", version.ref = "compose-multiplatform" }
compose-ui-backhandler = { module = "org.jetbrains.compose.ui:ui-backhandler", version.ref = "compose-multiplatform" }
compose-ui-test = { module = "org.jetbrains.compose.ui:ui-test", version.ref = "compose-multiplatform" }
compose-components-resources = { module = "org.jetbrains.compose.components:components-resources", version.ref = "compose-multiplatform" }
compose-material3 = { module = "org.jetbrains.compose.material3:material3", version.ref = "compose-material3" }
lifecycle-viewmodel-compose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "jetbrains-lifecycle" }
datastore-preferences-core = { module = "androidx.datastore:datastore-preferences-core", version.ref = "androidx-datastore" }
okio = { module = "com.squareup.okio:okio", version.ref = "okio" }
kotlinx-coroutines-swing = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-swing", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }
kotest-runner-junit5 = { module = "io.kotest:kotest-runner-junit5", version.ref = "kotest" }
kotest-assertions-core = { module = "io.kotest:kotest-assertions-core", version.ref = "kotest" }
kotest-property = { module = "io.kotest:kotest-property", version.ref = "kotest" }
archunit = { module = "com.tngtech.archunit:archunit", version.ref = "archunit" }
# old app
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
material = { group = "com.google.android.material", name = "material", version.ref = "google-android-material" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "app-cash-turbine" }
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "androidx-compose" }
ui-test-junit4 = { module = "androidx.compose.ui:ui-test-junit4" }
ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
ui-test-manifest = { module = "androidx.compose.ui:ui-test-manifest" }
material3 = { module = "androidx.compose.material3:material3" }
ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
material-icons-core = { module = "androidx.compose.material:material-icons-core" }
material-icons-extended = { module = "androidx.compose.material:material-icons-extended" }
adaptive = { module = "androidx.compose.material3.adaptive:adaptive" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "androidx-navigation" }
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "androidx-datastore" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-kmp-library = { id = "com.android.kotlin.multiplatform.library", version.ref = "agp" }
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
compose-multiplatform = { id = "org.jetbrains.compose", version.ref = "compose-multiplatform" }
ktlint = { id = "org.jlleitschuh.gradle.ktlint", version.ref = "ktlint" }
license = { id = "de.gematik.openhealth.licenseheader", version = "1.0.1" }
```

- [ ] **Step 3: Remove refreshVersions and register `:shared`**

Delete `versions.properties`. Replace `settings.gradle.kts` with:

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "kmwazi"
include(":app", ":shared")
```

In `lint.xml` delete the comment line and the two `<issue id="GradlePluginVersion" …/>` / `<issue id="GradleDependency" …/>` lines; keep the two accessibility issues.

- [ ] **Step 4: Root `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.license) apply false
}
```

In `gradle.properties` change `org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8` to `org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8` (KMP + Compose UI tests need more heap).

- [ ] **Step 5: `shared/build.gradle.kts`**

```kotlin
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.license)
}

kotlin {
    android {
        namespace = "com.github.asm0dey.kmwazi.shared"
        compileSdk = 37
        minSdk = 23
        androidResources.enable = true
    }
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.lifecycle.viewmodel.compose)
            api(libs.datastore.preferences.core)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.backhandler)
            implementation(libs.compose.material3)
            implementation(libs.compose.components.resources)
            implementation(libs.okio)
        }
        val desktopTest by getting {
            dependencies {
                implementation(libs.kotest.runner.junit5)
                implementation(libs.kotest.assertions.core)
                implementation(libs.kotest.property)
                implementation(libs.archunit)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.compose.ui.test)
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "com.github.asm0dey.kmwazi.resources"
}

tasks.named<Test>("desktopTest") {
    useJUnitPlatform()
}

ktlint {
    ignoreFailures.set(false)
    reporters {
        reporter(PLAIN)
        reporter(CHECKSTYLE)
    }
    filter {
        exclude { it.file.path.contains("generated") }
    }
}

licenseHeader {
    filesToScan.setFrom(fileTree("src") { include("**/*.kt") })
    header(rootProject.file("HEADER").readText())
}

tasks.named("check") { dependsOn("applyLicenseHeader") }
```

If Gradle reports `Unresolved reference: android` inside `kotlin { }`, this AGP build still uses the older DSL name: rename that block to `androidLibrary { … }` with the same body.

- [ ] **Step 6: Smoke test proving Kotest runs**

`shared/src/desktopTest/kotlin/com/github/asm0dey/kmwazi/SmokeTest.kt`:

```kotlin
package com.github.asm0dey.kmwazi

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SmokeTest : FunSpec({
    test("kotest runs on the desktop target") {
        (1 + 1) shouldBe 2
    }
})
```

- [ ] **Step 7: Build everything**

Run: `./gradlew :shared:desktopTest :shared:ktlintCheck :app:assembleDebug`
Expected: BUILD SUCCESSFUL; `shared/build/test-results/desktopTest/` contains a result for `SmokeTest` with 1 test passed. Then run `./gradlew :shared:desktopTest` once more after temporarily changing `2` to `3` in the smoke test: expected FAIL (proves tests are really executed). Revert.

- [ ] **Step 8: Renovate config (author's cross-project precedent)**

`renovate.json`:

```json
{
  "$schema": "https://docs.renovatebot.com/renovate-schema.json",
  "extends": ["config:recommended"],
  "vulnerabilityAlerts": { "enabled": true },
  "packageRules": [
    {
      "description": "One PR for everything that is not a major bump; majors stay on their own so a breaking change is read against one dependency.",
      "matchUpdateTypes": ["minor", "patch", "digest", "pin", "bump"],
      "groupName": "all non-major dependencies",
      "groupSlug": "all-minor-patch"
    }
  ]
}
```

- [ ] **Step 9: Commit**

```bash
git add -A gradle settings.gradle.kts build.gradle.kts gradle.properties lint.xml renovate.json versions.properties shared gradlew gradlew.bat
git commit -m "build: Gradle 9.8, KMP shared module, Kotest; drop refreshVersions; group Renovate updates

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Dealing (`round/Deal.kt`) with property tests

**Files:**
- Create: `shared/src/commonMain/kotlin/com/github/asm0dey/kmwazi/round/Mode.kt`
- Create: `shared/src/commonMain/kotlin/com/github/asm0dey/kmwazi/round/Deal.kt`
- Test: `shared/src/desktopTest/kotlin/com/github/asm0dey/kmwazi/round/DealTest.kt`
- Delete: `shared/src/desktopTest/kotlin/com/github/asm0dey/kmwazi/SmokeTest.kt`

**Interfaces:**
- Produces:
  - `sealed interface Mode { data object ChooseOne; data class Groups(val size: Int) /* require(size in SIZES) */; data object Order }`, `Mode.Groups.SIZES: IntRange = 2..10`
  - `sealed interface Result { data class One(val winner: Long); data class Groups(val groups: List<List<Long>>); data class Order(val order: List<Long>) }`
  - `class Deal(random: kotlin.random.Random)` with `chooseOne(ids: List<Long>): Long`, `groups(ids: List<Long>, size: Int): List<List<Long>>`, `order(ids: List<Long>): List<Long>`, `deal(mode: Mode, ids: List<Long>): Result`

Note: `Result` shadows `kotlin.Result` inside this package; other packages import it explicitly (`import com.github.asm0dey.kmwazi.round.Result`).

- [ ] **Step 1: Write the failing test**

```kotlin
package com.github.asm0dey.kmwazi.round

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.set
import io.kotest.property.checkAll
import kotlin.random.Random

class DealTest : FunSpec({
    val fingerIds = Arb.set(Arb.long(), 1..30).map { it.toList() }

    test("groups contain every finger exactly once and all but the last group are full") {
        checkAll(fingerIds, Arb.int(Mode.Groups.SIZES), Arb.long()) { ids, size, seed ->
            val groups = Deal(Random(seed)).groups(ids, size)
            groups.flatten() shouldContainExactlyInAnyOrder ids
            groups.dropLast(1).forEach { it.size shouldBe size }
            groups.last().size shouldBeInRange 1..size
        }
    }

    test("fewer fingers than the group size make one group with everyone") {
        Deal(Random(1)).groups(listOf(1L, 2L), 4).single() shouldContainExactlyInAnyOrder listOf(1L, 2L)
    }

    test("order is a permutation of the fingers") {
        checkAll(fingerIds, Arb.long()) { ids, seed ->
            Deal(Random(seed)).order(ids) shouldContainExactlyInAnyOrder ids
        }
    }

    test("choose one returns one of the fingers") {
        checkAll(fingerIds, Arb.long()) { ids, seed ->
            Deal(Random(seed)).chooseOne(ids) shouldBeIn ids
        }
    }

    test("deal follows the mode") {
        val deal = Deal(Random(1))
        deal.deal(Mode.ChooseOne, listOf(5L)) shouldBe Result.One(5L)
        deal.deal(Mode.Groups(2), listOf(5L)) shouldBe Result.Groups(listOf(listOf(5L)))
        deal.deal(Mode.Order, listOf(5L)) shouldBe Result.Order(listOf(5L))
    }
})
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :shared:desktopTest`
Expected: compilation FAIL — `Unresolved reference: Deal` / `Mode`.

- [ ] **Step 3: Implement**

`round/Mode.kt`:

```kotlin
package com.github.asm0dey.kmwazi.round

sealed interface Mode {
    data object ChooseOne : Mode

    data class Groups(val size: Int) : Mode {
        init {
            require(size in SIZES) { "group size $size outside $SIZES" }
        }

        companion object {
            val SIZES = 2..10
        }
    }

    data object Order : Mode
}

sealed interface Result {
    data class One(val winner: Long) : Result

    data class Groups(val groups: List<List<Long>>) : Result

    data class Order(val order: List<Long>) : Result
}
```

`round/Deal.kt`:

```kotlin
package com.github.asm0dey.kmwazi.round

import kotlin.random.Random

class Deal(private val random: Random) {
    fun chooseOne(ids: List<Long>): Long = ids.random(random)

    fun groups(ids: List<Long>, size: Int): List<List<Long>> = ids.shuffled(random).chunked(size)

    fun order(ids: List<Long>): List<Long> = ids.shuffled(random)

    fun deal(mode: Mode, ids: List<Long>): Result =
        when (mode) {
            Mode.ChooseOne -> Result.One(chooseOne(ids))
            is Mode.Groups -> Result.Groups(groups(ids, mode.size))
            Mode.Order -> Result.Order(order(ids))
        }
}
```

Delete `SmokeTest.kt`.

- [ ] **Step 4: Run tests**

Run: `./gradlew :shared:desktopTest :shared:ktlintCheck`
Expected: PASS, 5 tests.

- [ ] **Step 5: Commit**

```bash
git add -A shared/src
git commit -m "feat: dealing for choose-one, groups and order with property tests

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Round reducer (`round/Round.kt`)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/github/asm0dey/kmwazi/round/Round.kt`
- Test: `shared/src/desktopTest/kotlin/com/github/asm0dey/kmwazi/round/RoundTest.kt`

**Interfaces:**
- Consumes: `Mode`, `Result`, `Deal` (Task 3).
- Produces:
  - `data class Point(val x: Float, val y: Float)`
  - `data class Finger(val pos: Point, val colorIndex: Int)`
  - `data class Outcome(val result: Result, val snapshot: Map<Long, Finger>)`
  - `data class RoundState(val mode: Mode, val fingers: Map<Long, Finger> = emptyMap(), val nextColor: Int = 0, val armed: Int = 0, val outcome: Outcome? = null)`
  - `sealed interface Event { data class FingersChanged(val points: Map<Long, Point>); data class Expired(val armed: Int); data object Reset; data class ModeChanged(val mode: Mode) }`
  - `fun reduce(state: RoundState, event: Event, deal: Deal): RoundState`

While locked (`outcome != null`), `fingers` tracks the live finger set only to detect "everyone lifted"; rendering uses `outcome.snapshot`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.github.asm0dey.kmwazi.round

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.random.Random

private val deal = Deal(Random(0))

private fun RoundState.on(vararg events: Event): RoundState = events.fold(this) { s, e -> reduce(s, e, deal) }

private fun touch(vararg ids: Long) = Event.FingersChanged(ids.associateWith { Point(it * 10f, 0f) })

private fun RoundState.expire() = on(Event.Expired(armed))

class RoundTest : FunSpec({
    val start = RoundState(Mode.ChooseOne)

    test("a new finger set arms the countdown") {
        start.on(touch(1)).armed shouldBe 1
        start.on(touch(1), touch(1, 2)).armed shouldBe 2
    }

    test("moving fingers updates positions without re-arming") {
        val moved = start.on(touch(1), Event.FingersChanged(mapOf(1L to Point(5f, 5f))))
        moved.armed shouldBe 1
        moved.fingers.getValue(1L).pos shouldBe Point(5f, 5f)
    }

    test("lifting every finger before a result cancels the countdown and restarts colours") {
        val s = start.on(touch(1, 2), touch())
        s.fingers shouldBe emptyMap()
        s.nextColor shouldBe 0
        s.expire().outcome.shouldBeNull()
    }

    test("a matching expiry deals and locks with a snapshot") {
        val s = start.on(touch(1, 2)).expire()
        val outcome = s.outcome.shouldNotBeNull()
        outcome.snapshot.keys shouldBe setOf(1L, 2L)
        (outcome.result as Result.One).winner shouldBeIn listOf(1L, 2L)
    }

    test("a stale expiry is ignored") {
        start.on(touch(1), touch(1, 2), Event.Expired(1)).outcome.shouldBeNull()
    }

    test("changing mode during a countdown makes the pending expiry stale") {
        val counting = start.on(touch(1, 2))
        val changed = counting.on(Event.ModeChanged(Mode.Order))
        changed.mode shouldBe Mode.Order
        changed.fingers shouldBe emptyMap()
        changed.on(Event.Expired(counting.armed)).outcome.shouldBeNull()
    }

    test("while locked, finger changes are ignored until everyone lifts") {
        val locked = start.on(touch(1, 2)).expire()
        val s = locked.on(touch(1), touch(1, 3))
        s.outcome shouldBe locked.outcome
        s.armed shouldBe locked.armed
    }

    test("the first touch after everyone lifted starts a new round") {
        val locked = start.on(touch(1, 2)).expire()
        val s = locked.on(touch(), touch(3))
        s.outcome.shouldBeNull()
        s.fingers.keys shouldBe setOf(3L)
        s.fingers.getValue(3L).colorIndex shouldBe 0
        s.armed shouldBe locked.armed + 2
    }

    test("an expiry while locked does not deal again") {
        val locked = start.on(touch(1, 2)).expire()
        locked.expire() shouldBe locked
    }

    test("reset clears the round and keeps the mode") {
        RoundState(Mode.Order).on(touch(1), Event.Reset) shouldBe RoundState(Mode.Order, armed = 2)
    }

    test("new fingers take colours in arrival order and keep them") {
        val s = start.on(touch(1), touch(1, 2), touch(2), touch(2, 3))
        s.fingers.mapValues { it.value.colorIndex } shouldBe mapOf(2L to 1, 3L to 2)
    }

    test("colour indices keep counting past the palette size") {
        val s = start.on(touch(*LongArray(12) { it.toLong() }))
        s.fingers.values.map { it.colorIndex }.sorted() shouldBe (0..11).toList()
    }
})
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :shared:desktopTest`
Expected: compilation FAIL — `Unresolved reference: RoundState`.

- [ ] **Step 3: Implement `round/Round.kt`**

```kotlin
package com.github.asm0dey.kmwazi.round

data class Point(val x: Float, val y: Float)

data class Finger(val pos: Point, val colorIndex: Int)

data class Outcome(val result: Result, val snapshot: Map<Long, Finger>)

data class RoundState(
    val mode: Mode,
    val fingers: Map<Long, Finger> = emptyMap(),
    val nextColor: Int = 0,
    val armed: Int = 0,
    val outcome: Outcome? = null,
)

sealed interface Event {
    data class FingersChanged(val points: Map<Long, Point>) : Event

    data class Expired(val armed: Int) : Event

    data object Reset : Event

    data class ModeChanged(val mode: Mode) : Event
}

fun reduce(state: RoundState, event: Event, deal: Deal): RoundState =
    when (event) {
        is Event.FingersChanged -> onFingers(state, event.points)
        is Event.Expired ->
            if (event.armed != state.armed || state.outcome != null || state.fingers.isEmpty()) {
                state
            } else {
                state.copy(outcome = Outcome(deal.deal(state.mode, state.fingers.keys.toList()), state.fingers))
            }
        Event.Reset -> fresh(state.mode, state)
        is Event.ModeChanged -> fresh(event.mode, state)
    }

// armed + 1 so any countdown already running for the old round is stale.
private fun fresh(mode: Mode, state: RoundState) = RoundState(mode = mode, armed = state.armed + 1)

private fun onFingers(state: RoundState, points: Map<Long, Point>): RoundState {
    if (state.outcome != null) {
        return when {
            points.isEmpty() -> state.copy(fingers = emptyMap())
            state.fingers.isEmpty() -> onFingers(fresh(state.mode, state), points)
            else -> state.copy(fingers = points.mapValues { (_, p) -> Finger(p, 0) })
        }
    }
    var next = state.nextColor
    val fingers = points.mapValues { (id, p) -> state.fingers[id]?.copy(pos = p) ?: Finger(p, next++) }
    return when {
        fingers.keys == state.fingers.keys -> state.copy(fingers = fingers)
        fingers.isEmpty() -> state.copy(fingers = fingers, nextColor = 0, armed = state.armed + 1)
        else -> state.copy(fingers = fingers, nextColor = next, armed = state.armed + 1)
    }
}
```

- [ ] **Step 4: Run tests**

Run: `./gradlew :shared:desktopTest :shared:ktlintCheck`
Expected: PASS (DealTest 5 + RoundTest 12).

- [ ] **Step 5: Commit**

```bash
git add -A shared/src
git commit -m "feat: pure round reducer with countdown arming, locking and re-arm rules

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Strings, palettes and label contrast

**Files:**
- Create: `shared/src/commonMain/composeResources/values/strings.xml`, `values-de/strings.xml`, `values-ru/strings.xml`
- Create: `shared/src/commonMain/kotlin/com/github/asm0dey/kmwazi/Palettes.kt`
- Test: `shared/src/desktopTest/kotlin/com/github/asm0dey/kmwazi/PaletteTest.kt`

**Interfaces:**
- Produces:
  - Generated `com.github.asm0dey.kmwazi.resources.Res` with `Res.string.<name>` for every key in the old `strings.xml` except `app_name`.
  - `data class Palette(val id: String, val name: StringResource, val colors: List<Color>)` with `fun color(index: Int): Color` (wraps)
  - `object Palettes { val Vibrant; val Pastel; val Lucid; val Colorblind; val All: List<Palette> }`
  - `fun labelColor(background: Color): Color` (black or white)
  - `fun contrast(a: Color, b: Color): Float` (WCAG ratio)

- [ ] **Step 1: Copy strings**

```bash
for d in values values-de values-ru; do
  mkdir -p shared/src/commonMain/composeResources/$d
  grep -v 'name="app_name"' app/src/main/res/$d/strings.xml > shared/src/commonMain/composeResources/$d/strings.xml
done
```

Expected: each file keeps its license comment and all strings except `app_name`.

- [ ] **Step 2: Write the failing test**

```kotlin
package com.github.asm0dey.kmwazi

import androidx.compose.ui.graphics.Color
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe

class PaletteTest : FunSpec({
    test("every palette colour gets a label with at least 4.5:1 contrast") {
        Palettes.All.flatMap { it.colors }.forEach { c ->
            contrast(c, labelColor(c)) shouldBeGreaterThanOrEqual 4.5f
        }
    }

    test("light colours get black labels, dark colours white") {
        labelColor(Color.White) shouldBe Color.Black
        labelColor(Color(0xFFFFDE7D)) shouldBe Color.Black
        labelColor(Color(0xFF191919)) shouldBe Color.White
    }

    test("palette colours wrap past the palette size") {
        Palettes.Vibrant.color(10) shouldBe Palettes.Vibrant.color(0)
        Palettes.Vibrant.color(11) shouldBe Palettes.Vibrant.color(1)
    }

    test("palette ids match what 1.3.0 stored") {
        Palettes.All.map { it.id } shouldBe listOf("vibrant", "pastel", "lucid", "colorblind")
    }
})
```

- [ ] **Step 3: Run to verify it fails**

Run: `./gradlew :shared:desktopTest`
Expected: compilation FAIL — `Unresolved reference: Palettes`.

- [ ] **Step 4: Implement `Palettes.kt`**

Colour values copied from `app/src/main/java/com/github/asm0dey/kmwazi/ui/Palette.kt`.

```kotlin
package com.github.asm0dey.kmwazi

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.github.asm0dey.kmwazi.resources.Res
import com.github.asm0dey.kmwazi.resources.palette_colorblind
import com.github.asm0dey.kmwazi.resources.palette_lucid
import com.github.asm0dey.kmwazi.resources.palette_pastel
import com.github.asm0dey.kmwazi.resources.palette_vibrant
import org.jetbrains.compose.resources.StringResource

data class Palette(val id: String, val name: StringResource, val colors: List<Color>) {
    fun color(index: Int): Color = colors[index.mod(colors.size)]
}

object Palettes {
    val Vibrant =
        Palette(
            "vibrant",
            Res.string.palette_vibrant,
            listOf(
                Color(0xFFFF0000), Color(0xFFFF8000), Color(0xFFFFFF00), Color(0xFF00FF00), Color(0xFF00FFFF),
                Color(0xFF0000FF), Color(0xFFFF00FF), Color(0xFF8000FF), Color(0xFF00FF80), Color(0xFFFF007F),
            ),
        )
    val Pastel =
        Palette(
            "pastel",
            Res.string.palette_pastel,
            listOf(
                Color(0xFFFFB3BA), Color(0xFFFFDFBA), Color(0xFFFFFFBA), Color(0xFFBAFFC9), Color(0xFFBAE1FF),
                Color(0xFFE0BBE4), Color(0xFFFFC4E1), Color(0xFFBFFCC6), Color(0xFFD4F0F0), Color(0xFFFFEECC),
            ),
        )
    val Lucid =
        Palette(
            "lucid",
            Res.string.palette_lucid,
            listOf(
                Color(0xFF2E5BFF), Color(0xFF8C52FF), Color(0xFFFF2E63), Color(0xFF08D9D6), Color(0xFFFFDE7D),
                Color(0xFFF9A828), Color(0xFF4E9F3D), Color(0xFF950101), Color(0xFF191919), Color(0xFF005A8D),
            ),
        )
    val Colorblind =
        Palette(
            "colorblind",
            Res.string.palette_colorblind,
            listOf(
                Color(0xFF000000), Color(0xFFE69F00), Color(0xFF56B4E9), Color(0xFF009E73), Color(0xFFF0E442),
                Color(0xFF0072B2), Color(0xFFD55E00), Color(0xFFCC79A7), Color(0xFF999999), Color(0xFFFFFFFF),
            ),
        )
    val All = listOf(Vibrant, Pastel, Lucid, Colorblind)
}

// WCAG 2 contrast ratio, 1..21.
fun contrast(a: Color, b: Color): Float {
    val hi = maxOf(a.luminance(), b.luminance())
    val lo = minOf(a.luminance(), b.luminance())
    return (hi + 0.05f) / (lo + 0.05f)
}

fun labelColor(background: Color): Color =
    if (contrast(background, Color.Black) >= contrast(background, Color.White)) Color.Black else Color.White
```

- [ ] **Step 5: Run tests**

Run: `./gradlew :shared:desktopTest :shared:ktlintCheck`
Expected: PASS. If ktlint rejects several `Color(...)` per line, put one per line (`./gradlew :shared:ktlintFormat`).

- [ ] **Step 6: Commit**

```bash
git add -A shared/src
git commit -m "feat: palettes, shared string resources and WCAG label contrast

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Settings on multiplatform DataStore

**Files:**
- Create: `shared/src/commonMain/kotlin/com/github/asm0dey/kmwazi/Settings.kt`
- Create: `shared/src/desktopTest/kotlin/com/github/asm0dey/kmwazi/MemoryStore.kt`
- Test: `shared/src/desktopTest/kotlin/com/github/asm0dey/kmwazi/SettingsTest.kt`

**Interfaces:**
- Consumes: `Mode` (Task 3), `Palette`, `Palettes` (Task 5).
- Produces:
  - `val TIMEOUTS: IntRange = 1..10`
  - `data class Prefs(val palette: Palette = Palettes.Vibrant, val mode: Mode = Mode.ChooseOne, val timeoutSec: Int = 3, val groupSize: Int = 2)` (`groupSize` is the stored size, kept when the mode is not Groups, so the mode sheet can switch back to it)
  - `class Settings(store: DataStore<Preferences>)`, secondary `constructor(path: String)`; `val prefs: Flow<Prefs>`; `suspend fun setPalette(palette: Palette)`, `suspend fun setMode(mode: Mode)`, `suspend fun setTimeout(seconds: Int)`
  - Test helper `class MemoryStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences>` (desktopTest)

- [ ] **Step 1: Test helper `MemoryStore.kt`**

```kotlin
package com.github.asm0dey.kmwazi

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.MutableStateFlow

class MemoryStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
    private val flow = MutableStateFlow(initial)
    override val data = flow

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        transform(flow.value).also { flow.value = it }
}
```

- [ ] **Step 2: Write the failing test**

```kotlin
package com.github.asm0dey.kmwazi

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.asm0dey.kmwazi.round.Mode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import java.io.File

private val MODE = stringPreferencesKey("mode")
private val GROUP_SIZE = intPreferencesKey("group_size")
private val PALETTE = stringPreferencesKey("palette_name")
private val TIMEOUT = intPreferencesKey("decision_timeout_sec")

private suspend fun read(vararg stored: Preferences.Pair<*>) = Settings(MemoryStore(preferencesOf(*stored))).prefs.first()

class SettingsTest : FunSpec({
    test("defaults when nothing is stored") {
        read() shouldBe Prefs(Palettes.Vibrant, Mode.ChooseOne, timeoutSec = 3, groupSize = 2)
    }

    test("reads exactly what 1.3.0 wrote") {
        read(MODE to "DefineOrder", PALETTE to "pastel", TIMEOUT to 7) shouldBe Prefs(Palettes.Pastel, Mode.Order, 7, 2)
        read(MODE to "groups", GROUP_SIZE to 4).mode shouldBe Mode.Groups(4)
        read(MODE to "ChooseOne", GROUP_SIZE to 6) shouldBe Prefs(groupSize = 6)
        read(PALETTE to "lucid").palette shouldBe Palettes.Lucid
        read(PALETTE to "colorblind").palette shouldBe Palettes.Colorblind
    }

    test("unknown and out-of-range values fall back or clamp") {
        read(MODE to "groups", GROUP_SIZE to 1, PALETTE to "neon", TIMEOUT to 42) shouldBe
            Prefs(Palettes.Vibrant, Mode.Groups(2), timeoutSec = 10, groupSize = 2)
        read(GROUP_SIZE to 99, TIMEOUT to 0) shouldBe Prefs(timeoutSec = 1, groupSize = 10)
        read(MODE to "weird").mode shouldBe Mode.ChooseOne
    }

    test("round-trips through a real file and keeps group size after leaving groups") {
        val file = File.createTempFile("settings", ".preferences_pb").apply { delete(); deleteOnExit() }
        val settings = Settings(file.absolutePath)
        settings.setPalette(Palettes.Lucid)
        settings.setMode(Mode.Groups(5))
        settings.setTimeout(9)
        settings.prefs.first() shouldBe Prefs(Palettes.Lucid, Mode.Groups(5), 9, 5)
        settings.setMode(Mode.Order)
        settings.prefs.first() shouldBe Prefs(Palettes.Lucid, Mode.Order, 9, 5)
    }

    test("a corrupted file reads as defaults") {
        val file = File.createTempFile("corrupt", ".preferences_pb").apply { writeBytes(byteArrayOf(1, 2, 3, 4, 5)); deleteOnExit() }
        Settings(file.absolutePath).prefs.first() shouldBe Prefs()
    }
})
```

- [ ] **Step 3: Run to verify it fails**

Run: `./gradlew :shared:desktopTest`
Expected: compilation FAIL — `Unresolved reference: Settings`.

- [ ] **Step 4: Implement `Settings.kt`**

```kotlin
package com.github.asm0dey.kmwazi

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.asm0dey.kmwazi.round.Mode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

val TIMEOUTS = 1..10

data class Prefs(
    val palette: Palette = Palettes.Vibrant,
    val mode: Mode = Mode.ChooseOne,
    val timeoutSec: Int = 3,
    val groupSize: Int = 2,
)

// Keys and values must stay byte-compatible with 1.3.0 so upgrades keep settings.
private val MODE = stringPreferencesKey("mode")
private val GROUP_SIZE = intPreferencesKey("group_size")
private val PALETTE = stringPreferencesKey("palette_name")
private val TIMEOUT = intPreferencesKey("decision_timeout_sec")

class Settings(private val store: DataStore<Preferences>) {
    constructor(path: String) : this(
        PreferenceDataStoreFactory.createWithPath(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { path.toPath() },
        ),
    )

    val prefs: Flow<Prefs> = store.data.map { it.toPrefs() }

    suspend fun setPalette(palette: Palette) {
        store.edit { it[PALETTE] = palette.id }
    }

    suspend fun setMode(mode: Mode) {
        store.edit {
            when (mode) {
                Mode.ChooseOne -> it[MODE] = "ChooseOne"
                Mode.Order -> it[MODE] = "DefineOrder"
                is Mode.Groups -> {
                    it[MODE] = "groups"
                    it[GROUP_SIZE] = mode.size
                }
            }
        }
    }

    suspend fun setTimeout(seconds: Int) {
        store.edit { it[TIMEOUT] = seconds.coerceIn(TIMEOUTS) }
    }
}

private fun Preferences.toPrefs(): Prefs {
    val groupSize = (this[GROUP_SIZE] ?: 2).coerceIn(Mode.Groups.SIZES)
    return Prefs(
        palette = Palettes.All.find { it.id == this[PALETTE] } ?: Palettes.Vibrant,
        mode =
            when (this[MODE]) {
                "DefineOrder" -> Mode.Order
                "groups" -> Mode.Groups(groupSize)
                else -> Mode.ChooseOne
            },
        timeoutSec = (this[TIMEOUT] ?: 3).coerceIn(TIMEOUTS),
        groupSize = groupSize,
    )
}
```

- [ ] **Step 5: Run tests**

Run: `./gradlew :shared:desktopTest :shared:ktlintCheck`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add -A shared/src
git commit -m "feat: settings on multiplatform DataStore, compatible with 1.3.0 files

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 7: `RoundViewModel` and the countdown timer

**Files:**
- Create: `shared/src/commonMain/kotlin/com/github/asm0dey/kmwazi/RoundViewModel.kt`
- Test: `shared/src/desktopTest/kotlin/com/github/asm0dey/kmwazi/RoundViewModelTest.kt`

**Interfaces:**
- Consumes: `reduce`, `RoundState`, `Event`, `Deal`, `Mode` (Tasks 3–4); `Settings` (Task 6); `MemoryStore` (Task 6 test helper).
- Produces: `class RoundViewModel(settings: Settings, random: Random) : ViewModel()` with `val state: StateFlow<RoundState>`, `fun send(event: Event)`, `fun setMode(mode: Mode)` (resets the round and persists the mode).

- [ ] **Step 1: Write the failing test**

```kotlin
package com.github.asm0dey.kmwazi

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.asm0dey.kmwazi.round.Event
import com.github.asm0dey.kmwazi.round.Mode
import com.github.asm0dey.kmwazi.round.Point
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.random.Random

private fun fingers(vararg ids: Long) = Event.FingersChanged(ids.associateWith { Point(it.toFloat(), 0f) })

@OptIn(ExperimentalCoroutinesApi::class)
class RoundViewModelTest : FunSpec({
    val main = StandardTestDispatcher()
    beforeTest { Dispatchers.setMain(main) }
    afterTest { Dispatchers.resetMain() }

    test("deals once the stored timeout has passed") {
        runTest(main) {
            val vm = RoundViewModel(Settings(MemoryStore(preferencesOf(intPreferencesKey("decision_timeout_sec") to 5))), Random(0))
            runCurrent()
            vm.send(fingers(1, 2))
            advanceTimeBy(4_999)
            vm.state.value.outcome.shouldBeNull()
            advanceTimeBy(2)
            vm.state.value.outcome.shouldNotBeNull()
        }
    }

    test("a new finger restarts the countdown") {
        runTest(main) {
            val vm = RoundViewModel(Settings(MemoryStore()), Random(0))
            runCurrent()
            vm.send(fingers(1))
            advanceTimeBy(2_000)
            vm.send(fingers(1, 2))
            advanceTimeBy(2_999)
            vm.state.value.outcome.shouldBeNull()
            advanceTimeBy(2)
            vm.state.value.outcome.shouldNotBeNull()
        }
    }

    test("restores the saved mode and persists mode changes") {
        runTest(main) {
            val settings = Settings(MemoryStore(preferencesOf(stringPreferencesKey("mode") to "DefineOrder")))
            val vm = RoundViewModel(settings, Random(0))
            runCurrent()
            vm.state.value.mode shouldBe Mode.Order
            vm.setMode(Mode.Groups(4))
            runCurrent()
            vm.state.value.mode shouldBe Mode.Groups(4)
            settings.prefs.first().mode shouldBe Mode.Groups(4)
        }
    }
})
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :shared:desktopTest`
Expected: compilation FAIL — `Unresolved reference: RoundViewModel`.

- [ ] **Step 3: Implement `RoundViewModel.kt`**

```kotlin
package com.github.asm0dey.kmwazi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.asm0dey.kmwazi.round.Deal
import com.github.asm0dey.kmwazi.round.Event
import com.github.asm0dey.kmwazi.round.Mode
import com.github.asm0dey.kmwazi.round.RoundState
import com.github.asm0dey.kmwazi.round.reduce
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class RoundViewModel(private val settings: Settings, random: Random) : ViewModel() {
    private val deal = Deal(random)
    private val _state = MutableStateFlow(RoundState(Mode.ChooseOne))
    val state: StateFlow<RoundState> = _state.asStateFlow()

    init {
        viewModelScope.launch { send(Event.ModeChanged(settings.prefs.first().mode)) }
        // Every armed value gets its own countdown; collectLatest cancels the previous one.
        viewModelScope.launch {
            _state.map { it.armed }.distinctUntilChanged().collectLatest { armed ->
                delay(settings.prefs.first().timeoutSec * 1000L)
                send(Event.Expired(armed))
            }
        }
    }

    fun send(event: Event) {
        _state.update { reduce(it, event, deal) }
    }

    fun setMode(mode: Mode) {
        send(Event.ModeChanged(mode))
        viewModelScope.launch { settings.setMode(mode) }
    }
}
```

- [ ] **Step 4: Run tests**

Run: `./gradlew :shared:desktopTest :shared:ktlintCheck`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A shared/src
git commit -m "feat: RoundViewModel with a single collectLatest countdown

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 8: Touch screen — pointers, canvas, mode sheet

**Files:**
- Create: `shared/src/commonMain/kotlin/com/github/asm0dey/kmwazi/ui/Pointers.kt`
- Create: `shared/src/commonMain/kotlin/com/github/asm0dey/kmwazi/ui/FingerCanvas.kt`
- Create: `shared/src/commonMain/kotlin/com/github/asm0dey/kmwazi/ui/Common.kt`
- Create: `shared/src/commonMain/kotlin/com/github/asm0dey/kmwazi/ui/ModeSheet.kt`
- Create: `shared/src/commonMain/kotlin/com/github/asm0dey/kmwazi/ui/TouchScreen.kt`
- Create: `shared/src/commonMain/composeResources/drawable/close.xml`
- Test: `shared/src/desktopTest/kotlin/com/github/asm0dey/kmwazi/ui/TouchScreenTest.kt`

**Interfaces:**
- Consumes: `RoundState`, `Finger`, `Point`, `Mode`, `Result`, `Event`, `reduce`, `Deal` (Tasks 3–4); `Palette`, `Palettes`, `labelColor` (Task 5); `Res.string.*`.
- Produces:
  - `fun Modifier.multiTouch(onChange: (Map<Long, Point>) -> Unit): Modifier`
  - `@Composable fun FingerCanvas(state: RoundState, palette: Palette, pulse: Float, grow: Float, fade: Float, modifier: Modifier = Modifier)`
  - `internal fun circleColor(id: Long, finger: Finger, mode: Mode, result: Result?, labels: Map<Long, Int>, palette: Palette): Color`
  - `@Composable fun CloseButton(onClick: () -> Unit, modifier: Modifier = Modifier)`
  - `@Composable fun Stepper(value: Int, range: IntRange, label: String, onChange: (Int) -> Unit, modifier: Modifier = Modifier, decreaseDescription: String? = null, increaseDescription: String? = null)`
  - `@Composable fun ModeSheet(mode: Mode, groupSize: Int, onMode: (Mode) -> Unit, onDismiss: () -> Unit)`
  - `@Composable fun TouchScreen(state: RoundState, palette: Palette, groupSize: Int, onFingers: (Map<Long, Point>) -> Unit, onMode: (Mode) -> Unit, onReset: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier)`

- [ ] **Step 1: Write the failing UI test**

Circles are checked by sampling pixels at each finger's centre (always inside the circle: radius ≥ 40 dp, no labels before a result). `autoAdvance = false` because the pulse animation is infinite.

```kotlin
package com.github.asm0dey.kmwazi.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.github.asm0dey.kmwazi.Palettes
import com.github.asm0dey.kmwazi.round.Deal
import com.github.asm0dey.kmwazi.round.Event
import com.github.asm0dey.kmwazi.round.Mode
import com.github.asm0dey.kmwazi.round.Result
import com.github.asm0dey.kmwazi.round.RoundState
import com.github.asm0dey.kmwazi.round.reduce
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.random.Random

private val deal = Deal(Random(0))

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.showTouch(initial: RoundState = RoundState(Mode.ChooseOne)): MutableState<RoundState> {
    val state = mutableStateOf(initial)
    fun on(e: Event) {
        state.value = reduce(state.value, e, deal)
    }
    mainClock.autoAdvance = false
    setContent {
        TouchScreen(
            state = state.value,
            palette = Palettes.Vibrant,
            groupSize = 2,
            onFingers = { on(Event.FingersChanged(it)) },
            onMode = { on(Event.ModeChanged(it)) },
            onReset = { on(Event.Reset) },
            onClose = {},
        )
    }
    mainClock.advanceTimeByFrame()
    return state
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.pixel(at: Offset): Color {
    mainClock.advanceTimeByFrame()
    return onRoot().captureToImage().toPixelMap()[at.x.toInt(), at.y.toInt()]
}

@OptIn(ExperimentalTestApi::class)
class TouchScreenTest : FunSpec({
    val spots = List(12) { Offset(60f + it * 80f, 300f) }

    test("each finger gets a circle in the next palette colour") {
        runComposeUiTest {
            showTouch()
            onRoot().performTouchInput { spots.take(3).forEachIndexed { i, p -> down(i, p) } }
            spots.take(3).forEachIndexed { i, p -> pixel(p) shouldBe Palettes.Vibrant.color(i) }
        }
    }

    test("more fingers than colours are all drawn and colours wrap") {
        runComposeUiTest {
            val state = showTouch()
            onRoot().performTouchInput { spots.forEachIndexed { i, p -> down(i, p) } }
            state.value.fingers.size shouldBe 12
            pixel(spots[10]) shouldBe Palettes.Vibrant.color(0)
            pixel(spots[11]) shouldBe Palettes.Vibrant.color(1)
        }
    }

    test("a tap on the mode button is not a finger") {
        runComposeUiTest {
            val state = showTouch()
            onNodeWithText("Mode: Choose One").performClick()
            mainClock.advanceTimeByFrame()
            state.value.fingers shouldBe emptyMap()
            state.value.armed shouldBe 0
        }
    }

    test("fingers are gray in groups mode until the result") {
        runComposeUiTest {
            showTouch(RoundState(Mode.Groups(2)))
            onRoot().performTouchInput { down(0, spots[0]) }
            pixel(spots[0]) shouldBe Color.Gray
        }
    }

    test("choose one keeps the winner's colour and grays out the rest") {
        runComposeUiTest {
            val state = showTouch()
            onRoot().performTouchInput { spots.take(3).forEachIndexed { i, p -> down(i, p) } }
            mainClock.advanceTimeByFrame()
            state.value = reduce(state.value, Event.Expired(state.value.armed), deal)
            mainClock.advanceTimeBy(1_200) // overlay: 800 ms grow + 300 ms fade
            val winner = (state.value.outcome!!.result as Result.One).winner
            state.value.outcome!!.snapshot.forEach { (id, finger) ->
                val expected = if (id == winner) Palettes.Vibrant.color(finger.colorIndex) else Color.DarkGray
                pixel(Offset(finger.pos.x, finger.pos.y)) shouldBe expected
            }
        }
    }
})
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :shared:desktopTest`
Expected: compilation FAIL — `Unresolved reference: TouchScreen`.

- [ ] **Step 3: Close icon resource**

`shared/src/commonMain/composeResources/drawable/close.xml` (Material "close", Apache-2.0):

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF"
        android:pathData="M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z" />
</vector>
```

- [ ] **Step 4: `ui/Pointers.kt`**

```kotlin
package com.github.asm0dey.kmwazi.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.github.asm0dey.kmwazi.round.Point

// Reports the full set of pressed pointers whenever it changes. Pointers already consumed by a
// child (a button) are dropped, so tapping controls never counts as a finger.
fun Modifier.multiTouch(onChange: (Map<Long, Point>) -> Unit): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            val down = linkedMapOf<Long, Point>()
            do {
                val event = awaitPointerEvent()
                val before = down.toMap()
                event.changes.forEach { c ->
                    if (c.isConsumed || !c.pressed) {
                        down.remove(c.id.value)
                    } else {
                        down[c.id.value] = Point(c.position.x, c.position.y)
                    }
                }
                if (down != before) onChange(down.toMap())
            } while (event.changes.any { it.pressed })
            if (down.isNotEmpty()) onChange(emptyMap())
        }
    }
```

`pointerInput(Unit)` never restarts, so callers must pass a lambda that reads the latest callback (TouchScreen uses `rememberUpdatedState`).

- [ ] **Step 5: `ui/FingerCanvas.kt`**

```kotlin
package com.github.asm0dey.kmwazi.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.github.asm0dey.kmwazi.Palette
import com.github.asm0dey.kmwazi.labelColor
import com.github.asm0dey.kmwazi.round.Finger
import com.github.asm0dey.kmwazi.round.Mode
import com.github.asm0dey.kmwazi.round.Result
import com.github.asm0dey.kmwazi.round.RoundState
import kotlin.math.hypot

private val RADIUS = 40.dp

@Composable
fun FingerCanvas(
    state: RoundState,
    palette: Palette,
    pulse: Float,
    grow: Float,
    fade: Float,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val result = state.outcome?.result
    val fingers = state.outcome?.snapshot ?: state.fingers
    // Order: position in order. Groups: group number. Both 1-based.
    val labels =
        remember(result) {
            when (result) {
                is Result.Order -> result.order.withIndex().associate { (i, id) -> id to i + 1 }
                is Result.Groups -> result.groups.withIndex().flatMap { (g, ids) -> ids.map { it to g + 1 } }.toMap()
                else -> emptyMap()
            }
        }
    Canvas(modifier.fillMaxSize()) {
        val radius = RADIUS.toPx() * pulse
        fingers.forEach { (id, finger) ->
            val color = circleColor(id, finger, state.mode, result, labels, palette)
            val center = Offset(finger.pos.x, finger.pos.y)
            drawCircle(color, radius, center)
            labels[id]?.let { n ->
                val text = measurer.measure(n.toString(), TextStyle(color = labelColor(color), fontSize = (radius * 0.6f).toSp()))
                drawText(text, topLeft = center - Offset(text.size.width / 2f, text.size.height / 2f))
            }
        }
        if (result != null && fade > 0f) drawOverlay(result, fingers, palette, grow, 0.5f * fade)
    }
}

internal fun circleColor(
    id: Long,
    finger: Finger,
    mode: Mode,
    result: Result?,
    labels: Map<Long, Int>,
    palette: Palette,
): Color =
    when (result) {
        is Result.Groups -> palette.color(labels.getValue(id) - 1)
        is Result.One -> if (id == result.winner) palette.color(finger.colorIndex) else Color.DarkGray
        is Result.Order -> palette.color(finger.colorIndex)
        null -> if (mode is Mode.Groups) Color.Gray else palette.color(finger.colorIndex)
    }

private fun DrawScope.drawOverlay(
    result: Result,
    fingers: Map<Long, Finger>,
    palette: Palette,
    grow: Float,
    alpha: Float,
) {
    val lead =
        when (result) {
            is Result.One -> result.winner
            is Result.Order -> result.order.first()
            is Result.Groups -> null
        }
    if (lead == null) {
        drawRect(palette.color(0).copy(alpha = alpha), size = Size(size.width, size.height * grow))
    } else {
        val finger = fingers.getValue(lead)
        drawCircle(
            color = palette.color(finger.colorIndex).copy(alpha = alpha),
            radius = hypot(size.width, size.height) * grow,
            center = Offset(finger.pos.x, finger.pos.y),
        )
    }
}
```

- [ ] **Step 6: `ui/Common.kt`**

```kotlin
package com.github.asm0dey.kmwazi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.github.asm0dey.kmwazi.resources.Res
import com.github.asm0dey.kmwazi.resources.close
import com.github.asm0dey.kmwazi.resources.settings_close
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f), CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp),
    ) {
        Icon(
            painterResource(Res.drawable.close),
            contentDescription = stringResource(Res.string.settings_close),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun Stepper(
    value: Int,
    range: IntRange,
    label: String,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    decreaseDescription: String? = null,
    increaseDescription: String? = null,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        StepButton("-", decreaseDescription, enabled = value > range.first) { onChange(value - 1) }
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        StepButton("+", increaseDescription, enabled = value < range.last) { onChange(value + 1) }
    }
}

@Composable
private fun StepButton(
    symbol: String,
    description: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(48.dp).semantics { if (description != null) contentDescription = description },
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(symbol, style = MaterialTheme.typography.headlineSmall)
    }
}
```

- [ ] **Step 7: `ui/ModeSheet.kt`**

```kotlin
package com.github.asm0dey.kmwazi.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.asm0dey.kmwazi.resources.Res
import com.github.asm0dey.kmwazi.resources.touch_choose_one
import com.github.asm0dey.kmwazi.resources.touch_group_size
import com.github.asm0dey.kmwazi.resources.touch_groups
import com.github.asm0dey.kmwazi.resources.touch_play_order
import com.github.asm0dey.kmwazi.resources.touch_selection_mode_title
import com.github.asm0dey.kmwazi.round.Mode
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSheet(
    mode: Mode,
    groupSize: Int,
    onMode: (Mode) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp).padding(bottom = 32.dp)) {
            Text(
                stringResource(Res.string.touch_selection_mode_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            DropdownMenuItem(text = { Text(stringResource(Res.string.touch_choose_one)) }, onClick = { onMode(Mode.ChooseOne) })
            DropdownMenuItem(text = { Text(stringResource(Res.string.touch_play_order)) }, onClick = { onMode(Mode.Order) })
            DropdownMenuItem(text = { Text(stringResource(Res.string.touch_groups)) }, onClick = { onMode(Mode.Groups(groupSize)) })
            if (mode is Mode.Groups) {
                Stepper(
                    value = mode.size,
                    range = Mode.Groups.SIZES,
                    label = stringResource(Res.string.touch_group_size, mode.size),
                    onChange = { onMode(Mode.Groups(it)) },
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
```

- [ ] **Step 8: `ui/TouchScreen.kt`**

```kotlin
package com.github.asm0dey.kmwazi.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.github.asm0dey.kmwazi.Palette
import com.github.asm0dey.kmwazi.resources.Res
import com.github.asm0dey.kmwazi.resources.touch_groups_formed
import com.github.asm0dey.kmwazi.resources.touch_mode_choose_one
import com.github.asm0dey.kmwazi.resources.touch_mode_groups
import com.github.asm0dey.kmwazi.resources.touch_mode_play_order
import com.github.asm0dey.kmwazi.resources.touch_order_defined
import com.github.asm0dey.kmwazi.resources.touch_reset
import com.github.asm0dey.kmwazi.resources.touch_winner_selected
import com.github.asm0dey.kmwazi.round.Mode
import com.github.asm0dey.kmwazi.round.Point
import com.github.asm0dey.kmwazi.round.Result
import com.github.asm0dey.kmwazi.round.RoundState
import org.jetbrains.compose.resources.stringResource

@Composable
fun TouchScreen(
    state: RoundState,
    palette: Palette,
    groupSize: Int,
    onFingers: (Map<Long, Point>) -> Unit,
    onMode: (Mode) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val latestOnFingers by rememberUpdatedState(onFingers)
    val pressed = remember { mutableSetOf<Long>() }
    var sheetOpen by remember { mutableStateOf(false) }

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "pulse",
    )
    val grow = remember { Animatable(0f) }
    val fade = remember { Animatable(0f) }
    LaunchedEffect(state.outcome) {
        if (state.outcome == null) {
            fade.snapTo(0f)
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            fade.snapTo(1f)
            grow.snapTo(0f)
            grow.animateTo(1f, tween(800))
            fade.animateTo(0f, tween(300))
        }
    }

    val announcement =
        when (state.outcome?.result) {
            is Result.One -> stringResource(Res.string.touch_winner_selected)
            is Result.Groups -> stringResource(Res.string.touch_groups_formed)
            is Result.Order -> stringResource(Res.string.touch_order_defined)
            null -> ""
        }

    Box(
        modifier
            .fillMaxSize()
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = announcement
            }.multiTouch { points ->
                if ((points.keys - pressed).isNotEmpty()) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                pressed.clear()
                pressed.addAll(points.keys)
                latestOnFingers(points)
            },
    ) {
        FingerCanvas(state, palette, pulse, grow.value, fade.value)
        Button(onClick = { sheetOpen = true }, modifier = Modifier.padding(16.dp)) {
            Text(
                when (val m = state.mode) {
                    Mode.ChooseOne -> stringResource(Res.string.touch_mode_choose_one)
                    is Mode.Groups -> stringResource(Res.string.touch_mode_groups, m.size)
                    Mode.Order -> stringResource(Res.string.touch_mode_play_order)
                },
            )
        }
        Row(
            Modifier.align(Alignment.BottomEnd).padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.outcome != null) Button(onClick = onReset) { Text(stringResource(Res.string.touch_reset)) }
            CloseButton(onClose)
        }
        if (sheetOpen) {
            ModeSheet(
                mode = state.mode,
                groupSize = groupSize,
                onMode = { m ->
                    onMode(m)
                    if (m !is Mode.Groups) sheetOpen = false
                },
                onDismiss = { sheetOpen = false },
            )
        }
    }
}
```

- [ ] **Step 9: Run tests**

Run: `./gradlew :shared:desktopTest --tests '*TouchScreenTest' :shared:ktlintCheck`
Expected: PASS, 5 tests.
If "a tap on the mode button is not a finger" fails with `armed` = 2, the button did not consume the down event: change `awaitPointerEvent()` in `Pointers.kt` to `awaitPointerEvent(PointerEventPass.Final)` and re-run (Final runs after children have consumed).

- [ ] **Step 10: Commit**

```bash
git add -A shared/src
git commit -m "feat: touch screen with multi-touch tracking, canvas, labels and mode sheet

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 9: Home, Settings, Help screens and `App`

**Files:**
- Create: `shared/src/commonMain/kotlin/com/github/asm0dey/kmwazi/ui/HomeScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/github/asm0dey/kmwazi/ui/SettingsScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/github/asm0dey/kmwazi/ui/HelpScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/github/asm0dey/kmwazi/App.kt`
- Create: `shared/src/commonMain/composeResources/drawable/play.xml`, `settings.xml`, `help.xml`
- Test: `shared/src/desktopTest/kotlin/com/github/asm0dey/kmwazi/AppTest.kt`

**Interfaces:**
- Consumes: everything from Tasks 5–8.
- Produces:
  - `@Composable fun HomeScreen(onStart: () -> Unit, onSettings: () -> Unit, onHelp: () -> Unit, modifier: Modifier = Modifier)`
  - `@Composable fun SettingsScreen(prefs: Prefs, onPalette: (Palette) -> Unit, onTimeout: (Int) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier)`
  - `@Composable fun HelpScreen(onClose: () -> Unit, modifier: Modifier = Modifier)`
  - `@Composable fun App(settings: Settings, vm: RoundViewModel)` — entry points (Task 10) construct both; the ViewModel is owned by the platform (Activity `viewModels` on Android) so it survives rotation and is cleared properly.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.github.asm0dey.kmwazi

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.random.Random

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class AppTest : FunSpec({
    beforeTest { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    afterTest { Dispatchers.resetMain() }

    test("home leads to every screen and close returns home") {
        runComposeUiTest {
            val settings = Settings(MemoryStore())
            mainClock.autoAdvance = false
            setContent { App(settings, RoundViewModel(settings, Random(0))) }
            mainClock.advanceTimeBy(100)

            onNodeWithText("Start").performClick()
            mainClock.advanceTimeBy(100)
            onNodeWithText("Mode: Choose One").assertExists()
            onNodeWithContentDescription("Close").performClick()
            mainClock.advanceTimeBy(100)

            onNodeWithText("Settings").performClick()
            mainClock.advanceTimeBy(100)
            onNodeWithText("Select color palette").assertExists()
            onNodeWithText("3s").assertExists()
            onNodeWithContentDescription("Increase timeout").performClick()
            mainClock.advanceTimeBy(100)
            onNodeWithText("4s").assertExists()
            onNodeWithContentDescription("Close").performClick()
            mainClock.advanceTimeBy(100)

            onNodeWithText("Help").performClick()
            mainClock.advanceTimeBy(100)
            onNodeWithText("Place fingers on the screen. App will choose, group, or order after stabilization.").assertExists()
            onNodeWithContentDescription("Close").performClick()
            mainClock.advanceTimeBy(100)
            onNodeWithText("Start").assertExists()
        }
    }
})
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :shared:desktopTest --tests '*AppTest'`
Expected: compilation FAIL — `Unresolved reference: App`.

- [ ] **Step 3: Icon resources** (Material icons, Apache-2.0)

`drawable/play.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF" android:pathData="M8,5v14l11,-7z" />
</vector>
```

`drawable/help.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF"
        android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM13,19h-2v-2h2v2zM15.07,11.25l-0.9,0.92C13.45,12.9 13,13.5 13,15h-2v-0.5c0,-1.1 0.45,-2.1 1.17,-2.83l1.24,-1.26c0.37,-0.36 0.59,-0.86 0.59,-1.41 0,-1.1 -0.9,-2 -2,-2s-2,0.9 -2,2L8,9c0,-2.21 1.79,-4 4,-4s4,1.79 4,4c0,0.88 -0.36,1.68 -0.93,2.25z" />
</vector>
```

`drawable/settings.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF"
        android:pathData="M19.14,12.94c0.04,-0.3 0.06,-0.61 0.06,-0.94c0,-0.32 -0.02,-0.64 -0.07,-0.94l2.03,-1.58c0.18,-0.14 0.23,-0.41 0.12,-0.61l-1.92,-3.32c-0.12,-0.22 -0.37,-0.29 -0.59,-0.22l-2.39,0.96c-0.5,-0.38 -1.03,-0.7 -1.62,-0.94L14.4,2.81c-0.04,-0.24 -0.24,-0.41 -0.48,-0.41h-3.84c-0.24,0 -0.43,0.17 -0.47,0.41L9.25,5.35C8.66,5.59 8.12,5.92 7.63,6.29L5.24,5.33c-0.22,-0.08 -0.47,0 -0.59,0.22L2.74,8.87C2.62,9.08 2.66,9.34 2.86,9.48l2.03,1.58C4.84,11.36 4.8,11.69 4.8,12s0.02,0.64 0.07,0.94l-2.03,1.58c-0.18,0.14 -0.23,0.41 -0.12,0.61l1.92,3.32c0.12,0.22 0.37,0.29 0.59,0.22l2.39,-0.96c0.5,0.38 1.03,0.7 1.62,0.94l0.36,2.54c0.05,0.24 0.24,0.41 0.48,0.41h3.84c0.24,0 0.44,-0.17 0.47,-0.41l0.36,-2.54c0.59,-0.24 1.13,-0.56 1.62,-0.94l2.39,0.96c0.22,0.08 0.47,0 0.59,-0.22l1.92,-3.32c0.12,-0.22 0.07,-0.47 -0.12,-0.61L19.14,12.94zM12,15.6c-1.98,0 -3.6,-1.62 -3.6,-3.6s1.62,-3.6 3.6,-3.6s3.6,1.62 3.6,3.6S13.98,15.6 12,15.6z" />
</vector>
```

- [ ] **Step 4: `ui/HomeScreen.kt`**

```kotlin
package com.github.asm0dey.kmwazi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.asm0dey.kmwazi.resources.Res
import com.github.asm0dey.kmwazi.resources.help
import com.github.asm0dey.kmwazi.resources.home_help
import com.github.asm0dey.kmwazi.resources.home_settings
import com.github.asm0dey.kmwazi.resources.home_start
import com.github.asm0dey.kmwazi.resources.home_title
import com.github.asm0dey.kmwazi.resources.play
import com.github.asm0dey.kmwazi.resources.settings
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeScreen(
    onStart: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(Res.string.home_title), style = MaterialTheme.typography.displayLarge, modifier = Modifier.padding(bottom = 48.dp))
        HomeButton(Res.drawable.play, Res.string.home_start, onStart)
        HomeButton(Res.drawable.settings, Res.string.home_settings, onSettings)
        HomeButton(Res.drawable.help, Res.string.home_help, onHelp)
    }
}

@Composable
private fun HomeButton(
    icon: DrawableResource,
    label: StringResource,
    onClick: () -> Unit,
) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth(0.7f)) {
        Icon(painterResource(icon), contentDescription = null)
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp))
    }
}
```

(The icon is decorative — the button text already names it — so `contentDescription = null` avoids the "Start Start" double announcement 1.3.0 had.)

- [ ] **Step 5: `ui/SettingsScreen.kt`**

```kotlin
package com.github.asm0dey.kmwazi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.asm0dey.kmwazi.Palette
import com.github.asm0dey.kmwazi.Palettes
import com.github.asm0dey.kmwazi.Prefs
import com.github.asm0dey.kmwazi.TIMEOUTS
import com.github.asm0dey.kmwazi.resources.Res
import com.github.asm0dey.kmwazi.resources.settings_palette_label
import com.github.asm0dey.kmwazi.resources.settings_palette_title
import com.github.asm0dey.kmwazi.resources.settings_timeout_decrease
import com.github.asm0dey.kmwazi.resources.settings_timeout_increase
import com.github.asm0dey.kmwazi.resources.settings_timeout_label
import com.github.asm0dey.kmwazi.resources.settings_timeout_value
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(
    prefs: Prefs,
    onPalette: (Palette) -> Unit,
    onTimeout: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(Res.string.settings_palette_title), fontWeight = FontWeight.Bold)
            ColorStripes(prefs.palette.colors, Modifier.size(width = 160.dp, height = 16.dp))
            Box {
                PaletteRow(
                    prefs.palette,
                    Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(onClickLabel = stringResource(Res.string.settings_palette_label)) { expanded = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    Palettes.All.forEach { palette ->
                        DropdownMenuItem(
                            text = { PaletteRow(palette) },
                            onClick = {
                                onPalette(palette)
                                expanded = false
                            },
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.settings_timeout_label))
                Stepper(
                    value = prefs.timeoutSec,
                    range = TIMEOUTS,
                    label = stringResource(Res.string.settings_timeout_value, prefs.timeoutSec),
                    onChange = onTimeout,
                    decreaseDescription = stringResource(Res.string.settings_timeout_decrease),
                    increaseDescription = stringResource(Res.string.settings_timeout_increase),
                )
            }
        }
        CloseButton(onClose, Modifier.align(Alignment.BottomEnd).padding(24.dp))
    }
}

@Composable
private fun PaletteRow(
    palette: Palette,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ColorStripes(palette.colors, Modifier.size(width = 60.dp, height = 24.dp))
        Text(stringResource(palette.name), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ColorStripes(
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    Row(modifier) {
        colors.forEach { Box(Modifier.weight(1f).fillMaxSize().background(it)) }
    }
}
```

- [ ] **Step 6: `ui/HelpScreen.kt`**

```kotlin
package com.github.asm0dey.kmwazi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.asm0dey.kmwazi.resources.Res
import com.github.asm0dey.kmwazi.resources.help_text
import org.jetbrains.compose.resources.stringResource

@Composable
fun HelpScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(Res.string.help_text))
        }
        CloseButton(onClose, Modifier.align(Alignment.BottomEnd).padding(24.dp))
    }
}
```

- [ ] **Step 7: `App.kt`**

```kotlin
package com.github.asm0dey.kmwazi

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import com.github.asm0dey.kmwazi.round.Event
import com.github.asm0dey.kmwazi.ui.HelpScreen
import com.github.asm0dey.kmwazi.ui.HomeScreen
import com.github.asm0dey.kmwazi.ui.SettingsScreen
import com.github.asm0dey.kmwazi.ui.TouchScreen
import kotlinx.coroutines.launch

enum class Screen { Home, Touch, Settings, Help }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App(
    settings: Settings,
    vm: RoundViewModel,
) {
    val prefs by settings.prefs.collectAsState(Prefs())
    val scope = rememberCoroutineScope()
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }

    // Leaving the touch screen ends the round, like 1.3.0's per-visit ViewModel did.
    fun go(to: Screen) {
        if (screen == Screen.Touch) vm.send(Event.Reset)
        screen = to
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            // Android back / desktop Esc: everything returns to Home; Home lets the system handle it.
            BackHandler(enabled = screen != Screen.Home) { go(Screen.Home) }
            when (screen) {
                Screen.Home -> HomeScreen(onStart = { go(Screen.Touch) }, onSettings = { go(Screen.Settings) }, onHelp = { go(Screen.Help) })
                Screen.Touch -> {
                    val state by vm.state.collectAsState()
                    TouchScreen(
                        state = state,
                        palette = prefs.palette,
                        groupSize = prefs.groupSize,
                        onFingers = { vm.send(Event.FingersChanged(it)) },
                        onMode = vm::setMode,
                        onReset = { vm.send(Event.Reset) },
                        onClose = { go(Screen.Home) },
                    )
                }
                Screen.Settings ->
                    SettingsScreen(
                        prefs = prefs,
                        onPalette = { scope.launch { settings.setPalette(it) } },
                        onTimeout = { scope.launch { settings.setTimeout(it) } },
                        onClose = { go(Screen.Home) },
                    )
                Screen.Help -> HelpScreen(onClose = { go(Screen.Home) })
            }
        }
    }
}
```

- [ ] **Step 8: Run all shared tests**

Run: `./gradlew :shared:desktopTest :shared:ktlintCheck`
Expected: PASS (all specs).

- [ ] **Step 9: Commit**

```bash
git add -A shared/src
git commit -m "feat: home, settings and help screens wired together in App

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 10: Entry points — desktop window and Android shell

**Files:**
- Create: `desktopApp/build.gradle.kts`, `desktopApp/src/main/kotlin/com/github/asm0dey/kmwazi/desktop/Main.kt`
- Create: `app/src/main/java/com/github/asm0dey/kmwazi/KmwaziApplication.kt`
- Replace: `app/src/main/java/com/github/asm0dey/kmwazi/MainActivity.kt`, `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`, `settings.gradle.kts`, `gradle/libs.versions.toml`, `app/src/main/res/values{,-de,-ru}/strings.xml`
- Delete: every other file under `app/src/main/java/`, all of `app/src/test/`, `app/src/androidTest/` (if present)

**Interfaces:**
- Consumes: `App(settings, vm)`, `Settings(path)`, `RoundViewModel(settings, random)`.
- Produces: runnable `./gradlew :desktopApp:run`; Android APK/AAB from `:app`.

- [ ] **Step 1: Desktop module**

Add `":desktopApp"` to `include(...)` in `settings.gradle.kts`.

`desktopApp/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.license)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
}

compose.desktop {
    application {
        mainClass = "com.github.asm0dey.kmwazi.desktop.MainKt"
    }
}

ktlint { ignoreFailures.set(false) }

licenseHeader {
    filesToScan.setFrom(fileTree("src") { include("**/*.kt") })
    header(rootProject.file("HEADER").readText())
}

tasks.named("check") { dependsOn("applyLicenseHeader") }
```

`desktopApp/src/main/kotlin/com/github/asm0dey/kmwazi/desktop/Main.kt`:

```kotlin
package com.github.asm0dey.kmwazi.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.github.asm0dey.kmwazi.App
import com.github.asm0dey.kmwazi.RoundViewModel
import com.github.asm0dey.kmwazi.Settings
import java.io.File
import java.security.SecureRandom
import kotlin.random.asKotlinRandom

// Dev/test window only; never packaged or released.
fun main() {
    val dir = File(System.getProperty("user.home"), ".kmwazi").apply { mkdirs() }
    val settings = Settings(File(dir, "settings.preferences_pb").absolutePath)
    val vm = RoundViewModel(settings, SecureRandom().asKotlinRandom())
    application {
        Window(onCloseRequest = ::exitApplication, title = "Kmwazi") { App(settings, vm) }
    }
}
```

Run: `./gradlew :desktopApp:run` — expected: window with Kmwazi home. Click Start, click in the window (one circle appears where you hold the mouse; after 3 s it is chosen), press Esc → Home. Check the settings/play/help icons look right. Close the window.

- [ ] **Step 2: Android shell**

Delete old sources and tests:

```bash
git rm -r -q app/src/test app/src/androidTest 2>/dev/null || true
find app/src/main/java -name '*.kt' ! -name MainActivity.kt -print0 | xargs -0 git rm -q
```

`app/src/main/java/com/github/asm0dey/kmwazi/KmwaziApplication.kt`:

```kotlin
package com.github.asm0dey.kmwazi

import android.app.Application

class KmwaziApplication : Application() {
    // One DataStore per file per process: created here, not in the Activity, so rotation can't open a second one.
    // Same file 1.3.0's preferencesDataStore(name = "settings") wrote, so upgrades keep settings.
    val settings by lazy { Settings(filesDir.resolve("datastore/settings.preferences_pb").absolutePath) }
}
```

`app/src/main/java/com/github/asm0dey/kmwazi/MainActivity.kt`:

```kotlin
package com.github.asm0dey.kmwazi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.security.SecureRandom
import kotlin.random.asKotlinRandom

class MainActivity : ComponentActivity() {
    private val settings get() = (application as KmwaziApplication).settings
    private val vm: RoundViewModel by viewModels {
        viewModelFactory { initializer { RoundViewModel(settings, SecureRandom().asKotlinRandom()) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.statusBars())
        }
        setContent { App(settings, vm) }
    }
}
```

In `AndroidManifest.xml` add `android:name=".KmwaziApplication"` to `<application>`.

In each `app/src/main/res/values{,-de,-ru}/strings.xml` keep the license comment and only the `app_name` string (if a locale file has no `app_name`, delete that file).

- [ ] **Step 3: `app/build.gradle.kts`**

```kotlin
import org.gradle.api.JavaVersion.VERSION_11
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.license)
}

android {
    namespace = "com.github.asm0dey.kmwazi"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.github.asm0dey.kmwazi"
        minSdk = 23
        targetSdk = 37
        versionCode = 4
        versionName = "2.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = VERSION_11
        targetCompatibility = VERSION_11
    }
    buildFeatures { compose = true }
    lint { checkDependencies = true }
}

kotlin {
    compilerOptions { jvmTarget = JVM_11 }
}

ktlint {
    android.set(true)
    ignoreFailures.set(false)
    reporters {
        reporter(PLAIN)
        reporter(CHECKSTYLE)
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.activity.compose)
}

licenseHeader {
    filesToScan.setFrom(fileTree("src") { include("**/*.kt") })
    header(rootProject.file("HEADER").readText())
}

tasks.named("check") { dependsOn("applyLicenseHeader") }
```

- [ ] **Step 4: Drop old catalog entries**

In `gradle/libs.versions.toml` delete every line under both `# old app` markers (versions and libraries) and the markers themselves.

- [ ] **Step 5: Build Android**

Run: `./gradlew :app:assembleDebug :app:assembleRelease :app:lint :app:ktlintCheck`
Expected: BUILD SUCCESSFUL. If R8 strips something at runtime later (Step 6 crash with `ClassNotFoundException` for DataStore/protobuf), add to `app/proguard-rules.pro`:
`-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite { <fields>; }`

- [ ] **Step 6: Smoke on a device or emulator**

Run: `./gradlew :app:installRelease` (or `installDebug`), open the app: Home → Start → 3 fingers → result after 3 s → Reset → back gesture returns Home → back exits. Settings: change palette and timeout, kill the app, reopen: both kept. Rotate the device on the Touch screen: no crash.

- [ ] **Step 7: Commit**

```bash
git add -A settings.gradle.kts gradle desktopApp app
git commit -m "feat: Android shell and desktop dev window on top of shared; remove old app code

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 11: Architecture tests (ArchUnit in Kotest)

**Files:**
- Test: `shared/src/desktopTest/kotlin/com/github/asm0dey/kmwazi/ArchitectureTest.kt`

**Interfaces:**
- Consumes: package layout from Tasks 3–9; `Settings`, `RoundViewModel` classes.

- [ ] **Step 1: Write the test**

```kotlin
package com.github.asm0dey.kmwazi

import com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.kotest.core.spec.style.FunSpec

class ArchitectureTest : FunSpec({
    // Production classes of :shared only (desktop main output); test classes live under .../desktop/test/.
    val production =
        ClassFileImporter()
            .withImportOption { !it.contains("/test/") }
            .importPackages("com.github.asm0dey.kmwazi")

    test("round is pure Kotlin: no Android, Compose, coroutines or other app packages") {
        classes().that().resideInAPackage("..kmwazi.round..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("..kmwazi.round..", "kotlin..", "java.lang..", "java.util..", "org.jetbrains.annotations..")
            .check(production)
    }

    test("ui is stateless: no settings storage or view model") {
        noClasses().that().resideInAPackage("..kmwazi.ui..")
            .should().dependOnClassesThat(belongToAnyOf(Settings::class.java, RoundViewModel::class.java).or(resideInAPackage("androidx.datastore..")))
            .check(production)
    }

    test("randomness comes only from entry points") {
        noClasses().should().dependOnClassesThat().resideInAPackage("java.security..").check(production)
    }
})
```

- [ ] **Step 2: Run — expect PASS**

Run: `./gradlew :shared:desktopTest --tests '*ArchitectureTest'`
Expected: PASS, 3 tests.

- [ ] **Step 3: Prove each rule can fail**

Temporarily add `private val probe = com.github.asm0dey.kmwazi.Settings::class` to `ui/HelpScreen.kt`; run the same command. Expected: FAIL in "ui is stateless" naming `HelpScreenKt`. Revert. Then temporarily add `private val probe = java.security.SecureRandom()` to `round/Deal.kt`; expected FAIL in both "round is pure" and "randomness". Revert. Re-run: PASS.

- [ ] **Step 4: Commit**

```bash
git add shared/src/desktopTest/kotlin/com/github/asm0dey/kmwazi/ArchitectureTest.kt
git commit -m "test: enforce round purity, stateless UI and single randomness source with ArchUnit

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 12: Release lane, docs and full check

**Files:**
- Modify: `fastlane/Fastfile`, `README.md`, `CHANGELOG.md`
- Create: `RELEASING.md`

- [ ] **Step 1: fastlane test lane**

In `fastlane/Fastfile` replace the `test` lane body with:

```ruby
  desc "Runs all checks: ktlint, license headers, lint, tests, architecture rules"
  lane :test do
    gradle(task: "check")
    # applyLicenseHeader runs as part of check; any file it had to touch fails CI here.
    sh("git diff --exit-code")
  end
```

- [ ] **Step 2: `RELEASING.md`**

```markdown
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
4. Upgrade check (only when settings code changed): install the previous Play version, choose Pastel,
   Groups of 4 and 7 s, then install the new build over it — all three must survive.
5. Fresh-install check: uninstall, install, change a setting, kill and reopen — the setting is kept.
6. `bundle exec fastlane beta` (internal track), smoke-test, then `bundle exec fastlane deploy`.
```

- [ ] **Step 3: README and CHANGELOG**

In `README.md`, replace the `## Installation` and `## Development` sections' build instructions with:

```markdown
## Project layout

- `shared/` — all app code (Compose Multiplatform): `round/` (pure game rules), `Settings`, `RoundViewModel`, `ui/`.
- `app/` — Android entry point (the published app).
- `desktopApp/` — desktop window for development; not released.

## Development

- Run on desktop: `./gradlew :desktopApp:run`
- Build Android: `./gradlew :app:assembleDebug`
- All checks: `./gradlew check` (or `bundle exec fastlane test`)
- Releasing: see `RELEASING.md`.
```

Keep the existing Fastlane lanes list below it. Add at the top of `CHANGELOG.md` (below its title):

```markdown
## 2.0.0

- Rewritten from scratch on Compose Multiplatform; same features and settings.
- Circles are sized in dp, so they look the same on every screen density.
- Every finger is drawn (previously only the first 10).
- Groups show group numbers; Order and Groups labels pick black or white for readable contrast.
- The result overlay fades out faster.
- Group size is 2–10 everywhere.
```

- [ ] **Step 4: Full verification**

Run: `./gradlew clean check :app:assembleRelease :app:bundleRelease && git status --short`
Expected: BUILD SUCCESSFUL and no modified files. Then `bundle exec fastlane test` — expected: success.

- [ ] **Step 5: Commit**

```bash
git add fastlane/Fastfile README.md CHANGELOG.md RELEASING.md
git commit -m "docs: releasing checklist, 2.0.0 changelog; fastlane test runs full check

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
