# Emulator / instrumentation testing

## What CI runs

| Layer | Command / job | VPN tunnel / exit IP |
|-------|---------------|----------------------|
| Unit (Robolectric + JVM) | `./gradlew :app:testDebugUnitTest` in `build-and-unit` | **N/A** — no VpnService |
| Instrumentation smoke | `connectedDebugAndroidTest` via `reactivecircus/android-emulator-runner` (`emulator-smoke` job) | **BLOCKED** — no connect/exit-IP/traffic asserts |
| Physical device E2E | Manual only (`usgate-demo` checklist) | **BLOCKED on CI** — **user-only acceptance** |

Workflow: [`.github/workflows/android-ci.yml`](../.github/workflows/android-ci.yml).

## Runnable without real VPN permission

Covered by unit / smoke tests:

1. **Subscription parse** — plain / base64 `vless://`, `vmess://`, `ss://` placeholders (`SubscriptionParserTest`)
2. **UI state** — `disconnected → connecting` mock via `ConnectionUiState` (`ConnectionUiStateTest`)
3. **Error / status strings** — `vpn_permission_required`, `need_node`, `subscription_required`, `import_fail`, status labels
4. **Sing-box JSON builder** — placeholder Reality VLESS (`SingBoxConfigBuilderTest`)
5. **Instrumentation** — package name + placeholder parse (`SmokeInstrumentedTest`) — still **no** TUN connect

## BLOCKED on CI / headless emulator

These require a real `VpnService` consent UI, a reachable panel/node, and network assertions. They are **not** marked PASS in automation:

- Full VPN tunnel establish with libbox
- Exit IP equals VPS (`api.ipify.org` / equivalent)
- Disconnect → reconnect stability
- Traffic counters / download through the tunnel
- Always-on VPN / network-switch depth tests

Record honest results in [`CI_EMULATOR_RESULTS.md`](CI_EMULATOR_RESULTS.md).

## Local emulator steps (optional)

```bash
# Requires Android SDK emulator + system image (x86_64 / google_apis)
export ANDROID_HOME=…
sdkmanager "system-images;android-30;google_apis;x86_64"
avdmanager create avd -n usgate_api30 -k "system-images;android-30;google_apis;x86_64" -d pixel_4
emulator -avd usgate_api30 -no-window -gpu swiftshader_indirect &
adb wait-for-device
./gradlew :app:fetchLibbox :app:installDebug :app:connectedDebugAndroidTest
```

Granting VPN on an emulator still does **not** prove production exit-IP; treat tunnel assertions as BLOCKED unless you manually verify against a known lab node (placeholders only in git).

## Physical device

Acceptance remains **user-only**. See [usgate-demo `docs/DEVICE_TEST_CHECKLIST.md`](https://github.com/rong001/usgate-demo/blob/main/docs/DEVICE_TEST_CHECKLIST.md) and [`ANDROID_CI_STATUS.md`](https://github.com/rong001/usgate-demo/blob/main/docs/ANDROID_CI_STATUS.md).  
**Do not** treat physical exit-IP / reconnect / traffic as CI PASS.
