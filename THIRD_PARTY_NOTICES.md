# Third-party notices

This repository’s **own** source is licensed under the **Apache License 2.0** (see [`LICENSE`](LICENSE)).

The Android app links against third-party components at **build / runtime**. Those components keep their own licenses. This file documents the important ones for reviewers and redistributors.

---

## sing-box (SagerNet/sing-box)

- Upstream: https://github.com/SagerNet/sing-box  
- License file fetched from upstream (`master` / tag `v1.13.14`): https://raw.githubusercontent.com/SagerNet/sing-box/master/LICENSE  
  (same text verified on `v1.13.14`)

**Quoted upstream LICENSE (verbatim):**

```text
Copyright (C) 2022 by nekohasekai <contact-sagernet@sekai.icu>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.

In addition, no derivative work may use the name or imply association
with this application without prior consent.
```

**Summary:** GPL-3.0-or-later, plus an additional naming / association restriction.  
**Note:** As of the fetched LICENSE text above, there is **no** separate “mobile exception” clause. Do not assume App Store / proprietary-linking exceptions exist; re-check upstream if you redistribute a combined APK.

Full GPL-3.0 text: https://www.gnu.org/licenses/gpl-3.0.html

---

## libbox AAR (`singbox-android/libbox` via JitPack)

Detailed pin / repro / GPL notes: [`docs/LIBBOX.md`](docs/LIBBOX.md).

- Pinned version in `app/build.gradle.kts`: **1.13.14**
- Default download URL (build-time only):

  `https://jitpack.io/com/github/singbox-android/libbox/1.13.14/libbox-1.13.14.aar`

- Gradle task: `:app:fetchLibbox` (runs before `preBuild` if `app/libs/libbox.aar` is missing)  
- Helper script: `scripts/fetch-libbox.sh`

**Not redistributed in this git repository.** `app/libs/*.aar` is gitignored. Each builder downloads (or self-builds) the AAR locally.

**GPL implications for distribution:**  
`libbox` is the gomobile-wrapped sing-box core and is under the **same GPL-3.0-or-later** terms as sing-box. Shipping a **combined APK** that includes `libbox` is distributing a work that incorporates GPL-covered code. Redistributors must comply with GPL-3.0 (source offer / corresponding source for the combined work as required by the license, notices, etc.). This Apache-2.0 project license covers **this repo’s Kotlin / resource sources**; it does **not** relicense `libbox` / sing-box. Seek your own legal advice before publishing Play Store / binary releases.

Offline rebuild of the AAR from matching sing-box sources:

```bash
git clone https://github.com/SagerNet/sing-box.git
cd sing-box
git checkout v1.13.14   # match app/build.gradle.kts pin
make lib_install
make lib_android        # produces libbox.aar
cp libbox.aar /path/to/usgate-client/app/libs/
```

---

## AndroidX, Material, OkHttp, Kotlin coroutines

| Component | Typical SPDX | Notes |
|-----------|--------------|--------|
| AndroidX (`androidx.*`) | Apache-2.0 | Google AndroidX |
| Material Components (`com.google.android.material`) | Apache-2.0 | |
| OkHttp 4.x (`com.squareup.okhttp3`) | Apache-2.0 | Square |
| Kotlin stdlib / coroutines | Apache-2.0 | JetBrains |

These are resolved from Maven Central / Google Maven at build time and are not vendored in this repository.

---

## Disclaimer

Operate and redistribute only where lawful. Do not commit real VPS IPs, UUIDs, `vless://` production share links, panel paths, or secrets. Placeholder samples live under `samples/`.
