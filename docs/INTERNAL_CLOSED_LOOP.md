# INTERNAL_CLOSED_LOOP (Android client pointer)

Full evidence: **[usgate-demo/docs/INTERNAL_ANDROID_CLOSED_LOOP.md](https://github.com/rong001/usgate-demo/blob/main/docs/INTERNAL_ANDROID_CLOSED_LOOP.md)**

## This repo

| Item | Status |
|------|--------|
| `InternalClosedLoopTest` (JVM) | **PASS** — synthetic sub → parser → SingBoxConfigBuilder; revoke-empty rejected |
| `./gradlew testDebugUnitTest` | **PASS** |
| Debug APK `0.2.1-branded` (versionCode 3) | **PASS** local rebuild — see `dist/SHA256SUMS.txt` on build box |
| 真实专属客户端联网 / 管理端见真机设备 | **BLOCKED（无物理设备）** |

Portal create→enable→revoke loop lives in `usgate-demo/portal/tests/e2e_internal_closed_loop.py`.
