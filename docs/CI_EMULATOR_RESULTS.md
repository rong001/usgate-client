# CI / emulator results (honest matrix)

**Date (UTC):** 2026-09-22  
**Host for local run:** Linux box (OpenJDK 21; Android SDK 34)  
**Repo:** `rong001/usgate-client`  
**CI workflow:** `.github/workflows/android-ci.yml` (**local commit ready; not on GitHub `main` yet**)

Legend: **PASS** / **FAIL** / **BLOCKED**

## Automated / local runnable

| Check | Result | Notes |
|-------|--------|-------|
| `:app:fetchLibbox` | PASS* | *Skipped if `app/libs/libbox.aar` already present (gitignored) |
| `:app:assembleDebug` | PASS | Debug APK builds |
| Unit: subscription parse | PASS | `SubscriptionParserTest` (placeholders only) |
| Unit: UI disconnected→connecting mock | PASS | `ConnectionUiStateTest` |
| Unit: status / error strings | PASS | Robolectric resource lookup |
| Unit: SingBoxConfigBuilder Reality placeholder | PASS | `SingBoxConfigBuilderTest` |
| Instrumentation smoke (package + parse) | BLOCKED | **No emulator/system-image/AVD on this box**; CI `emulator-smoke` best-effort once workflow ships |
| Emulator VPN tunnel connect | BLOCKED | Headless CI cannot assert real VpnService success |
| Exit IP == VPS | BLOCKED | Physical / lab device only — **NOT PASS** |
| Disconnect / reconnect | BLOCKED | Physical / lab device only — **NOT PASS** |
| Traffic through tunnel | BLOCKED | Physical / lab device only — **NOT PASS** |
| GitHub Actions on `main` | BLOCKED | OAuth token lacks `workflow` scope; `git push` refused for `.github/workflows/android-ci.yml` |

## Explicit non-goals for CI

> **Physical Android exit IP / reconnect / traffic = NOT PASS** (never marked PASS by this automation).  
> **MOCK demo portal results ≠ real node PASS.**

## Commands used on this box

```bash
./gradlew :app:fetchLibbox :app:assembleDebug :app:testDebugUnitTest
# connectedDebugAndroidTest — not run (no AVD / system image)
```

### Local unit-test run (2026-09-22 refresh)

```text
./gradlew :app:assembleDebug :app:testDebugUnitTest → BUILD SUCCESSFUL
Suites: SubscriptionParserTest (5), ConnectionUiStateTest (3), SingBoxConfigBuilderTest (1)
Result: 9 tests, 0 failures, 0 errors (PASS)
Instrumentation / emulator VPN: BLOCKED (no system-image/AVD)
Physical exit IP / reconnect / traffic: NOT PASS
GitHub workflow push: BLOCKED (need `gh auth refresh -h github.com -s workflow` + browser device flow)
```

Update this file when CI or local results change; keep BLOCKED rows honest.
