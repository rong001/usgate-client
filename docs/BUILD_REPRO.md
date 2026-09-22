# Reproducible debug APK build

This document records exact toolchain pins and the steps used to produce
`app/build/outputs/apk/debug/app-debug.apk` from this repository.

## Toolchain (from project files)

| Piece | Version / value | Source |
|-------|-----------------|--------|
| JDK | **17+** (box used OpenJDK **21**) | `app/build.gradle.kts` `JavaVersion.VERSION_17` |
| Android Gradle Plugin (AGP) | **8.5.2** | root `build.gradle.kts` |
| Kotlin plugin | **1.9.24** | root `build.gradle.kts` |
| Gradle Wrapper | **8.7** | `gradle/wrapper/gradle-wrapper.properties` |
| compileSdk / targetSdk | **34** | `app/build.gradle.kts` |
| minSdk | **24** | `app/build.gradle.kts` |
| ABI filters | `arm64-v8a`, `x86_64` | `app/build.gradle.kts` |
| libbox AAR | **1.13.14** (JitPack) | `app/build.gradle.kts` `libbox.version` |
| Android SDK on build host | Platform **34**, Build-Tools **34.0.0** | `$ANDROID_HOME` |

Create a local (untracked) `local.properties`:

```properties
sdk.dir=/path/to/Android/Sdk
```

## Steps (network once for dependencies + libbox)

```bash
cd usgate-client
# optional explicit fetch:
./scripts/fetch-libbox.sh
# or let Gradle do it:
./gradlew :app:fetchLibbox :app:assembleDebug
```

Expected artifact:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Override libbox without editing sources:

```bash
./gradlew :app:assembleDebug -Plibox.version=1.13.14
# or
./gradlew :app:assembleDebug -Plibox.aar.url='https://…/libbox.aar'
```

## Full offline rebuild of libbox (optional)

Match the pin in `app/build.gradle.kts` (currently **1.13.14**):

```bash
git clone https://github.com/SagerNet/sing-box.git
cd sing-box
git checkout v1.13.14
make lib_install
make lib_android
cp libbox.aar /path/to/usgate-client/app/libs/
```

Then `./gradlew :app:assembleDebug` (fetch task skips if AAR already present).

## Build result on THIS box

### Checklist

- [x] `./gradlew :app:fetchLibbox :app:assembleDebug` completed
- [x] Artifact path exists: `app/build/outputs/apk/debug/app-debug.apk`
- [x] Result: **PASS**

### Recorded run (this box)

```text
Host: Linux box; OpenJDK 21.0.12.1; ANDROID_HOME=/home/box/Android/Sdk
SDK: platforms/android-34, build-tools/34.0.0
Command: ./gradlew :app:fetchLibbox :app:assembleDebug --no-daemon
:app:fetchLibbox SKIPPED (local app/libs/libbox.aar present; gitignored — not published)
BUILD SUCCESSFUL in 8s (40 actionable tasks: 1 executed, 39 up-to-date)
Artifact: app/build/outputs/apk/debug/app-debug.apk
Size: 55958259 bytes
sha256: f524001e04a498941ee3e5b6c1f2bbd32f249aa4e93187423c72cfba8b535dfc
Date: 2026-09-22
Note: A clean clone without a pre-fetched AAR downloads libbox 1.13.14 from JitPack on first build.
```
