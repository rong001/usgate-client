# USGate Android 客户端

> **Public Android source** for USGate.  
> Stack demo / portal / deploy docs: **[rong001/usgate-demo](https://github.com/rong001/usgate-demo)**  
> License: **Apache-2.0** (project sources) — see [`LICENSE`](LICENSE) and [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) (sing-box / libbox are **GPL-3.0-or-later**).  
> Reproducible debug build: [`docs/BUILD_REPRO.md`](docs/BUILD_REPRO.md) · libbox: [`docs/LIBBOX.md`](docs/LIBBOX.md)  
> CI: [`.github/workflows/android-ci.yml`](.github/workflows/android-ci.yml) · emulator notes: [`docs/EMULATOR_TEST.md`](docs/EMULATOR_TEST.md)  
> **Physical device** exit-IP / reconnect / traffic acceptance is **user-only** (never CI PASS) — see usgate-demo [`docs/ANDROID_CI_STATUS.md`](https://github.com/rong001/usgate-demo/blob/main/docs/ANDROID_CI_STATUS.md).

品牌：**USGate**（`0.2.1-branded`）。Kotlin + AndroidX + Material 3 原生应用，对接 **3X-UI** 订阅链接，通过 **libbox（sing-box）** 在 `VpnService` 上代理真实流量。

> Flutter SDK 在本构建环境中不可用，故选用 **Kotlin Android (Gradle)** 技术栈。

**重要：** 仓库内不含真实服务器 IP、UUID 或密钥，一律使用占位符。也不捆绑 `libbox.aar`（构建时下载或由你放入 `app/libs/`）。

---

## 更新日志（节选）

### 0.2.1-branded

- **品牌视觉**：新 launcher 自适应图标（盾牌 + 门闸 / 钥匙孔，indigo–蓝配色），mipmap 密度资源齐全。
- **主题 / 启动**：主色改为 indigo；冷启动 splash；首页展示 USGate 品牌与「安全网关」标语。
- **中文文案**：连接 / 断开 / 订阅 / 节点 / 设置 等标签统一打磨。
- **通知图标**：VPN 前台服务使用自定义白色剪影 `ic_stat_vpn`（不再用系统锁图标）。
- **落地页**：`landing/index.html` 与 App 同色同标。
- **版本**：`versionName=0.2.1-branded`，`versionCode=3`。
- 未改动 libbox VPN 核心逻辑；仓库仍不含真实服务器密钥。

---

## 功能一览

| 模块 | 状态 |
|------|------|
| 首页：连接 / 断开 + 状态 | ✅ |
| 设置：粘贴订阅 URL 并本地保存 | ✅ |
| 解析订阅（base64 / `vless://` / `vmess://` / `ss://`） | ✅ |
| 节点列表与选择 | ✅ |
| **真实代理隧道（libbox / sing-box）** | ✅ 已接入（需 AAR） |
| `vless` → sing-box JSON（含 Reality / Vision / WS） | ✅ `SingBoxConfigBuilder` |
| TUN fd + `VpnService.protect()` | ✅ `LibboxPlatformInterface` |
| 静态落地页 `landing/index.html` | ✅ |

---

## 环境要求

- JDK **17+**
- Android SDK（`compileSdk 34`）：Platform 34、Build-Tools 34.x、Platform-Tools
- 构建时联网一次以下载 `libbox.aar`（约 90 MB），或手动放入 AAR

### 安装示例（Linux）

```bash
sudo apt-get update && sudo apt-get install -y openjdk-21-jdk-headless
# Android SDK / cmdline-tools …（略，见历史文档）
export ANDROID_HOME="$HOME/Android/Sdk"
```

在项目根目录创建 `local.properties`（勿提交）：

```properties
sdk.dir=/home/你的用户名/Android/Sdk
```

---

## libbox 如何接入（合法、不二次分发）

官方 [SagerNet/sing-box](https://github.com/SagerNet/sing-box) **Release 不直接提供** `libbox.aar`（需 `gomobile` 本地编译）。本项目采用两种等价方式，**均不把二进制提交进 Git**：

### 方式 A（默认）：构建时下载

Gradle 任务 `:app:fetchLibbox` 在 `preBuild` 前执行，若 `app/libs/libbox.aar` 不存在则下载：

```text
https://jitpack.io/com/github/singbox-android/libbox/1.13.14/libbox-1.13.14.aar
```

也可手动：

```bash
./scripts/fetch-libbox.sh
# 或
FORCE_LIBBOX_FETCH=1 ./scripts/fetch-libbox.sh
```

覆盖版本 / URL：

```bash
./gradlew :app:assembleDebug -Plibox.version=1.13.14
# 或
./gradlew :app:assembleDebug -Plibox.aar.url='https://…/libbox.aar'
```

### 方式 B：用户自备 AAR

1. 从源码编译（推荐与上游对齐）：

   ```bash
   git clone https://github.com/SagerNet/sing-box.git
   cd sing-box
   make lib_install
   make lib_android   # 产出 libbox.aar
   cp libbox.aar /path/to/usgate-client/app/libs/
   ```

2. 或从你信任的构建产物复制到 `app/libs/libbox.aar`。

`app/libs/*.aar` 已在 `.gitignore` 中，避免误提交与授权问题。

`app/build.gradle.kts` 中：

```kotlin
implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))
```

---

## 构建与运行

```bash
cd usgate-client   # or your clone path
./scripts/fetch-libbox.sh          # 可选；Gradle 也会自动拉
./gradlew :app:assembleDebug
./gradlew :app:installDebug        # 已连接设备 / 模拟器
```

APK：`app/build/outputs/apk/debug/app-debug.apk`  
（当前 `ndk.abiFilters` = `arm64-v8a` + `x86_64`。）

---

## 与 3X-UI 订阅如何配合

1. 在 3X-UI 复制 **订阅 URL** 或单条 `vless://…` 分享链接。
2. USGate → **设置** → 粘贴 → **保存** → **导入订阅**。
3. **选择节点** → 首页 **连接** → 授予 VPN 权限。

占位示例（非真实节点）：

```text
vless://00000000-0000-0000-0000-000000000000@example.com:443?encryption=none&security=tls&type=ws&path=%2Fpath#US-Placeholder
```

粘贴真实 `vless://`（含 Reality：`security=reality&pbk=…&sid=…&sni=…&flow=xtls-rprx-vision&fp=chrome`）后，应能代理设备流量。连接成功后可把最近一次配置写在应用私有目录 `files/sing-box-last.json` 便于排错（**勿把含密钥的文件提交到 Git**）。

---

## 架构

```
com.usgate.client
├── ui/
├── subscription/       # ProxyNode、Parser、Importer
├── data/               # PrefsStore
└── vpn/
    ├── UsGateVpnService      # VpnService；libbox 回调 openTun / protect
    ├── VpnController
    ├── VpnCoreBridge         # 接口 + VpnCoreFactory
    ├── LibboxVpnCore         # CommandServer + startOrReloadService
    ├── LibboxPlatformInterface / DefaultNetworkMonitor
    ├── SingBoxConfigBuilder  # ProxyNode → sing-box JSON
    └── ScaffoldVpnCore       # 无 AAR 时的占位
```

数据流：

1. 导入订阅 → `PrefsStore`
2. 选节点 → `selectedNodeId`
3. 连接 → `UsGateVpnService` → `LibboxVpnCore.startCore`
4. libbox 调 `openTun(TunOptions)` → `Builder.establish()` → 返回 fd  
5. 出站 socket → `autoDetectInterfaceControl` → `VpnService.protect(fd)`

---

## 仍待完善（TODO）

1. **DNS 精细化**：按地区分流、FakeIP、DoH 可选；当前固定 `1.1.1.1` via proxy。
2. **IPv6**：TUN / route 默认偏 IPv4（`strategy: ipv4_only`）。
3. **分应用代理 UI**：`OverrideOptions.includePackage / excludePackage` 已预留。
4. **连接稳定性**：重连、网络切换、Always-on VPN 深度测试。
5. **配置校验 UX**：把 `Libbox.checkConfig` 失败原因展示到首页。
6. **体积**：可再裁 ABI 或改用自编译瘦身 `libbox.aar`。
7. **升级 libbox**：换 JitPack / 自编译版本后对照 `PlatformInterface` 方法是否变更。
8. **单元测试**：已有 `SubscriptionParser` / `ConnectionUiState` / `SingBoxConfigBuilder` JVM 测试；可再扩 WS/SS 分支覆盖。

---

## 落地页

[`landing/index.html`](landing/index.html)：

```bash
cd landing && python3 -m http.server 8080
```

---

## 许可与免责

- 本仓库源码：**Apache License 2.0**（[`LICENSE`](LICENSE)）。
- 第三方：**sing-box / libbox** 为 **GPL-3.0-or-later**（见 [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)）。分发含 libbox 的 APK 时请自行满足 GPL 义务；本仓库 **不** 二次分发 `libbox.aar`。
- 仅供学习与二次开发。请遵守当地法律与目标服务条款。


## CI & tests

- Workflow: [`.github/workflows/android-ci.yml`](.github/workflows/android-ci.yml) — JDK 17, Android SDK, Gradle cache, `:app:fetchLibbox :app:assembleDebug`, unit tests, optional emulator instrumentation smoke, debug APK artifact.
- Local unit tests: `./gradlew :app:testDebugUnitTest`
- Emulator / BLOCKED VPN rows: [`docs/EMULATOR_TEST.md`](docs/EMULATOR_TEST.md), results [`docs/CI_EMULATOR_RESULTS.md`](docs/CI_EMULATOR_RESULTS.md).
- **Physical Android exit IP / reconnect / traffic = NOT PASS in CI** — acceptance stays on the device owner (usgate-demo checklist).

## Related repos

| Repo | Role |
|------|------|
| **[rong001/usgate-client](https://github.com/rong001/usgate-client)** | This repo — public Android client source |
| **[rong001/usgate-demo](https://github.com/rong001/usgate-demo)** | Portal + docs + deploy demo (desensitized) |
