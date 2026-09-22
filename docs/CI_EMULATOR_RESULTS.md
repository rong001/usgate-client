# CI / emulator results (honest matrix)

**Date (UTC):** 2026-09-22  
**Host for local run:** Linux box (OpenJDK 21; Android SDK 34)  
**Repo:** `rong001/usgate-client`  
**CI workflow:** `.github/workflows/android-ci.yml`

Legend: **PASS** / **FAIL** / **BLOCKED**

## Automated / local runnable

| Check | Result | Notes |
|-------|--------|-------|
| `:app:fetchLibbox` | PASS* | *Skipped if `app/libs/libbox.aar` already present (gitignored) |
| `:app:assembleDebug` | PASS | Debug APK builds with JDK 17 target |
| Unit: subscription parse | PASS | `SubscriptionParserTest` (placeholders only) |
| Unit: UI disconnected→connecting mock | PASS | `ConnectionUiStateTest` |
| Unit: status / error strings | PASS | Robolectric resource lookup |
| Unit: SingBoxConfigBuilder Reality placeholder | PASS | `SingBoxConfigBuilderTest` |
| Instrumentation smoke (package + parse) | BLOCKED** | **No emulator/system-image on this box; CI `emulator-smoke` job best-effort |
| Emulator VPN tunnel connect | BLOCKED | Headless CI cannot assert real VpnService success |
| Exit IP == VPS | BLOCKED | Physical / lab device only — **NOT PASS** |
| Disconnect / reconnect | BLOCKED | Physical / lab device only — **NOT PASS** |
| Traffic through tunnel | BLOCKED | Physical / lab device only — **NOT PASS** |

## Explicit non-goals for CI

> **Physical Android exit IP / reconnect / traffic = NOT PASS** (never marked PASS by this automation).

Those rows stay **BLOCKED** until a human runs the usgate-demo device checklist on their own hardware with their own subscription (no secrets in git).

## Commands used on this box

```bash
./gradlew :app:fetchLibbox :app:assembleDebug :app:testDebugUnitTest
# connectedDebugAndroidTest — not run here (no AVD / system image)
```

### Local unit-test run (2026-09-22)

```text
./gradlew :app:testDebugUnitTest → BUILD SUCCESSFUL
Suites: SubscriptionParserTest, ConnectionUiStateTest, SingBoxConfigBuilderTest
Result: 9 tests, 0 failures, 0 errors (PASS)
Instrumentation / emulator VPN: BLOCKED (no system-image/AVD on this box)
Physical exit IP / reconnect / traffic: NOT PASS
```

Update this file when CI or local results change; keep BLOCKED rows honest.
