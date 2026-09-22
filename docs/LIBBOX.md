# libbox / sing-box integration

## Upstream

| Item | Value |
|------|--------|
| Core project | [SagerNet/sing-box](https://github.com/SagerNet/sing-box) |
| License | **GPL-3.0-or-later** (upstream LICENSE; **no** mobile exception clause) |
| Pinned libbox version | **1.13.14** (`app/build.gradle.kts` → `libbox.version`) |
| Default AAR URL | `https://jitpack.io/com/github/singbox-android/libbox/1.13.14/libbox-1.13.14.aar` |
| Gradle task | `:app:fetchLibbox` (also hooked from `preBuild`) |
| Helper script | `scripts/fetch-libbox.sh` |

`app/libs/libbox.aar` is **gitignored** and must not be committed. CI / local builders download it (or you place a self-built AAR).

## Repro build (debug APK)

```bash
cd usgate-client
# local.properties → sdk.dir=…
./gradlew :app:fetchLibbox :app:assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

Override:

```bash
./gradlew :app:assembleDebug -Plibox.version=1.13.14
./gradlew :app:assembleDebug -Plibox.aar.url='https://…/libbox.aar'
```

Offline AAR from matching sing-box tag:

```bash
git clone https://github.com/SagerNet/sing-box.git
cd sing-box && git checkout v1.13.14
make lib_install && make lib_android
cp libbox.aar /path/to/usgate-client/app/libs/
```

More toolchain pins: [`BUILD_REPRO.md`](BUILD_REPRO.md).

## Combined APK / GPL obligations

Shipping an APK that embeds `libbox` distributes GPL-covered code. This repository’s Kotlin/resources are **Apache-2.0**; that does **not** relicense sing-box/libbox. Redistributors must meet GPL-3.0 (corresponding source / offers, notices, etc.). See [`../THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md). Seek your own legal advice before Play Store or binary releases.

## Related demo docs

Portal / acceptance / device checklist (physical E2E is **user-only**): [rong001/usgate-demo](https://github.com/rong001/usgate-demo).
