# R8 Configuration Analysis

## Configuration

- App module: `android/app` using Kotlin DSL and Android Gradle Plugin `8.13.2`.
- The `release` build type enables minification and resource shrinking.
- Release uses the optimized default `proguard-android-optimize.txt`.
- `android.r8.optimizedResourceShrinking=true` is enabled because the project uses AGP 8.13.x.
- R8 full mode is not disabled in `gradle.properties`.

AGP 9 can be evaluated as a separate upgrade. The current project remains on AGP 8.13.2 so the release baseline can be verified without mixing a toolchain migration into this product milestone.

## Keep-rule review

The project has no custom `proguard-rules.pro` rules. The app has no class-name loading, annotation-based reflection, Gson, Retrofit, Room, Parcelable, or custom Android component pattern that requires an app-owned keep rule. Android manifest components and AndroidX / Compose consumer rules are handled by the platform and libraries.

The recommended action is to keep the custom rule set empty and rely on the optimized default file plus library consumer rules. This avoids preserving the entire launcher package or framework libraries unnecessarily.

## Verification

Run the debug and release builds with lint, then use UI Automator on the installable debug APK for the provider binding, calendar permission, weather cache, rotation, restart, and removal flows. After the production keystore is supplied, repeat the same smoke flow against the signed release APK.
