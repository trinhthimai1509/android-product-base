# Quick start

The shortest path from an empty directory to a running Android app built on this Base.

Everything here was executed against this repository at tag `0.2.0-alpha01`. If a step does not
work, that is a documentation defect — report it (see [the friction rules](PRODUCT_INTEGRATION.md#consumer-rules)),
do not fix it by editing the Base.

---

## The one rule that is not negotiable

**A consumer product must not modify Product Base source code.**

Not a "temporary" edit, not a one-line fix, not a copied file. The Base is read-only
infrastructure. Everything a product needs to differ is a parameter, a Hilt binding, a lambda or
a module it chooses not to include. If you cannot find the seam, you have found either a
documentation gap or a Base defect — both are reported, neither is patched locally. The full
classification is in [PRODUCT_INTEGRATION.md](PRODUCT_INTEGRATION.md#consumer-rules).

---

## 0. Prerequisites

| Requirement | Value | Why |
|---|---|---|
| JDK for the Gradle daemon | **21** | `build-logic` targets JDK 21. A Java 17 daemon fails with *"Dependency requires at least JVM runtime version 21"*. |
| Android SDK | compileSdk **37** installed | Compose 1.12 / BOM 2026.08.00 requires it. |
| Android Studio | any version that runs Gradle 9.7 | Not required for the command line. |

The Base pins everything else — AGP 9.1.1, Kotlin 2.3.21, KSP 2.3.11, Hilt 2.60.1, minSdk 26,
Java 17 source level with core-library desugaring. A product sets none of these.

## 1. Lay out the workspace

The Base and the product are siblings. Nothing is copied between them.

```
workspace/
├── android-product-base/      ← this repository, read-only
└── consumer-app/              ← your product
```

```bash
cd workspace
mkdir -p consumer-app/app/src/main/kotlin/com/example/tracker
cd consumer-app
cp -R ../android-product-base/gradle ./gradle          # wrapper + daemon JVM criteria
cp ../android-product-base/gradlew ./gradlew           # do NOT copy libs.versions.toml
rm gradle/libs.versions.toml                           # the Base owns versions; see step 2
```

> The `gradle/gradle-daemon-jvm.properties` you just copied is what makes the daemon run on Java
> 21. Without it the build fails before it compiles anything.

Create `local.properties` with your SDK location (or let Android Studio create it):

```properties
sdk.dir=/Users/you/Library/Android/sdk
```

## 2. `settings.gradle.kts`

This is the whole consumption mechanism. It does three things: borrows the Base's convention
plugins, borrows its version catalog, and maps the Base's Gradle projects into this build.

```kotlin
// Where the Base checkout lives, relative to this file. Read-only: never edit anything under it.
val productBaseDir = file("../android-product-base")

pluginManagement {
    // The Base's convention plugins (productbase.android.*), as a separate included build.
    includeBuild("../android-product-base/build-logic")
    repositories {
        google()
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
    // The Base owns every dependency version, including the ones this product uses directly.
    versionCatalogs {
        create("libs") {
            from(files("../android-product-base/gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "consumer-app"

include(":app")

fun includeBase(path: String, dir: String) {
    include(path)
    project(path).projectDir = File(productBaseDir, dir)
}

// The `:core` and `:feature` containers are implied by the paths below, and Gradle would look
// for them inside this repository. Point them at the Base too.
includeBase(":core", "core")
includeBase(":feature", "feature")

// Include only what this product needs. Every module left out is code it does not ship.
includeBase(":core:common", "core/common")
includeBase(":core:designsystem", "core/designsystem")
includeBase(":core:datastore", "core/datastore")
includeBase(":core:testing", "core/testing")
includeBase(":feature:onboarding", "feature/onboarding")
includeBase(":feature:settings", "feature/settings")
```

See [MODULE_CATALOG.md](MODULE_CATALOG.md) for what else you can add, and
[PRODUCT_INTEGRATION.md](PRODUCT_INTEGRATION.md) for four worked product shapes.

## 3. Root `build.gradle.kts`

```kotlin
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Required by the Base's toolchain: AGP 9 pins KGP 2.2.10, and KSP 2.3.11 needs Kotlin
        // 2.3.x. Without this, KSP fails with "kotlin.sourceSets DSL is not allowed".
        classpath(libs.kotlin.gradlePlugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    // Add `alias(libs.plugins.kotlin.serialization) apply false` if you use core:network.
}
```

## 4. `gradle.properties`

```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true

kotlin.code.style=official
android.useAndroidX=true
```

## 5. `app/build.gradle.kts`

```kotlin
plugins {
    id("productbase.android.application")
    id("productbase.android.compose")
    id("productbase.android.hilt")
}

android {
    namespace = "com.example.tracker"

    defaultConfig {
        applicationId = "com.example.tracker"
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        buildConfig = true          // off by default in AGP 9; needed for BuildConfig.DEBUG
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:datastore"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
```

Create `app/proguard-rules.pro` — **the file must exist** even if it is only comments, because
release builds are minified by default:

```proguard
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
```

## 6. The three files that make it an app

`app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:name=".TrackerApplication"
        android:allowBackup="false"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.Tracker">
        <activity android:name=".MainActivity" android:exported="true"
            android:theme="@style/Theme.Tracker">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

`TrackerApplication.kt` and the one binding every product must supply:

```kotlin
@HiltAndroidApp
class TrackerApplication : Application()

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    /** Required: core:datastore, core:network, core:ads, core:billing and core:review all inject it. */
    @Provides
    @Singleton
    fun provideLogger(): Logger = if (BuildConfig.DEBUG) AndroidLogger() else NoOpLogger()
}
```

`MainActivity.kt`:

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: AppViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            ProductBaseTheme(
                themeMode = uiState.themeMode,
                dynamicColor = uiState.dynamicColorEnabled,
            ) {
                if (uiState.isLoading) return@ProductBaseTheme     // no theme flash, no onboarding flash
                // your NavHost here
            }
        }
    }
}
```

`AppViewModel` is product code — around twenty lines reading `AppPreferences`. The complete
version is in [PRODUCT_INTEGRATION.md](PRODUCT_INTEGRATION.md#app-level-state).

## 7. Run it

```bash
./gradlew :app:assembleDebug        # first run downloads the toolchain, ~1–2 min
./gradlew :app:installDebug
./gradlew :app:test :app:lint :app:assembleRelease
```

`assembleRelease` runs R8 with minification and resource shrinking — those are on by default from
the Base's application convention plugin, so a product cannot forget to enable them.

---

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `Dependency requires at least JVM runtime version 21` | Daemon on Java 17 | Copy `gradle/gradle-daemon-jvm.properties` from the Base (step 1), or run on a JDK 21 |
| `Configuring project ':core' without an existing directory` | The `:core` / `:feature` containers were not mapped | Add the two `includeBase(":core", "core")` lines from step 2 |
| `Supplied proguard configuration does not exist` | Missing `app/proguard-rules.pro` | Create it; comments are enough (step 5) |
| `Manifest placeholder admobApplicationId not found` | `core:ads` included without an AdMob id per build type | See [MONETIZATION.md](MONETIZATION.md#adsconfig) |
| Hilt reports a missing binding for `Logger` | No product `@Provides` for it | Step 6 |
| Hilt reports a missing binding for `PremiumState` | `core:ads` without `core:billing` | Bind `PremiumState.AlwaysFree`, see [MONETIZATION.md](MONETIZATION.md#free-app) |

## Where to go next

| Question | Document |
|---|---|
| What modules exist and what do they cost me? | [MODULE_CATALOG.md](MODULE_CATALOG.md) |
| How do I wire four common product shapes? | [PRODUCT_INTEGRATION.md](PRODUCT_INTEGRATION.md) |
| Ads, billing, paywall, consent | [MONETIZATION.md](MONETIZATION.md) |
| Branding, colours, typography, spacing | [THEMING.md](THEMING.md) |
| Fakes and what not to mock | [TESTING.md](TESTING.md) |
| A checklist to follow start to finish | [NEW_APP_CHECKLIST.md](NEW_APP_CHECKLIST.md) |
