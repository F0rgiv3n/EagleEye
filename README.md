<div align="center">

# EagleEye

**Native Android cybersecurity & network intelligence tool**

A single-Activity Jetpack Compose app that brings 28 network tools, threat detection, LAN scanning, and a background monitoring service into one polished surface — written in ~16 000 lines of idiomatic Kotlin.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Min SDK](https://img.shields.io/badge/min%20SDK-26%20(Android%208.0)-3DDC84?logo=android&logoColor=white)](#)
[![Target SDK](https://img.shields.io/badge/target%20SDK-35%20(Android%2015)-3DDC84?logo=android&logoColor=white)](#)
[![License](https://img.shields.io/badge/license-MIT-blue)](#license)

**[English](README.md)** · [Ελληνικά](README.el.md) · [User Guide](docs/USER_GUIDE.md)

</div>

---

## Table of Contents

- [Highlights](#highlights)
- [Screenshots](#screenshots)
- [Feature catalogue](#feature-catalogue)
- [Architecture](#architecture)
- [Project structure](#project-structure)
- [Tech stack](#tech-stack)
- [Build & install](#build--install)
- [Testing](#testing)
- [Permissions](#permissions)
- [Engineering notes](#engineering-notes)
- [Roadmap & known limits](#roadmap--known-limits)
- [License](#license)

---

## Highlights

EagleEye is a deliberately opinionated security tool. Every screen is built around the question *"what should an analyst see in the first 1.5 seconds?"* The result is dense but legible: a refined dark palette inspired by Linear/Vercel/GitHub dashboards, opacity-based text hierarchy, and Monospace reserved strictly for data (IPs, MACs, hex output).

- **28 network tools** in a single tab — ping, traceroute, port scanner, DNS lookup, WHOIS, SSL inspector, VPN leak test, CVE search, captive portal detector, HTTP security-headers analyzer, threat-intel lookup, Shodan, mDNS discovery, IPv6 inspector, DNS benchmark, firewall tester, speed test, bandwidth monitor, packet analyzer, and more.
- **Threat detection engine** — ARP-spoof / MITM detection, evil-twin detection (same SSID, foreign BSSID), DNS hijack detection, rogue-DHCP scanner, weak-Wi-Fi audit (WEP/WPS/open), captive-portal analyzer with phishing heuristics. Each finding is graded `CRITICAL · HIGH · MEDIUM · LOW · INFO` and persisted to a 7-day event log.
- **LAN scanner** with parallel ICMP sweep across the `/24`, OUI vendor lookup (bundled Wireshark OUI database, 41k entries), service fingerprinting on 16 common ports, per-device alias and "known" flag, plus an Android-11+ ARP fallback so detection still works when `/proc/net/arp` is restricted.
- **Background monitor** runs as a `FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE` service with proper Android 14+ semantics. Scheduled re-scans every N minutes; threat-grade home-screen widget; persistent threat notifications with severity-mapped priority.
- **Packet analyzer** that captures live device traffic via `VpnService` (no root needed) — parses IPv4/TCP/UDP/ICMP headers, extracts DNS queries, aggregates top destinations, with hard caps to prevent unbounded memory growth during long sessions.
- **MAC privacy tools** — randomized-MAC detection (locally-administered bit), root-based MAC change via `ip link`, per-SSID profiles with auto-rotation intervals, hardware MAC recovery.
- **Modern architecture** — single-Activity Compose UI, sealed `Screen` routing with `AnimatedContent` crossfade, MVVM with `StateFlow`, Room (3 entities), DataStore-backed settings, `viewModelScope` + structured concurrency throughout.

> **Why "EagleEye"?** The eye sees what the network hides — passive observation, no hostile traffic generated.

---

## Screenshots

> Capture from a real device with `adb exec-out screencap -p > docs/screenshots/<name>.png` and drop into `docs/screenshots/`. Five suggested shots:

| Screen | Filename |
|--------|----------|
| Dashboard with signal history | `docs/screenshots/dashboard.png` |
| LAN scanner with devices listed | `docs/screenshots/lan-scanner.png` |
| Security audit result | `docs/screenshots/security-audit.png` |
| Network Tools tab grid | `docs/screenshots/tools.png` |
| Threat radar / topology | `docs/screenshots/topology.png` |

---

## Feature catalogue

### Wi-Fi & connection

- Live connection info (SSID, BSSID, IP, gateway, subnet, DNS×2, link speed, RSSI, band, security type)
- Network scanner with security grade per AP, hidden-SSID flag, RSSI sort
- Signal history strip chart (60-sample sliding window)
- Wi-Fi QR generator (WIFI:T:WPA2;S:…;P:…;;)
- Spectrum visualizer (channel utilization)

### LAN intelligence

- Parallel ping sweep of the entire `/24` (254 hosts in ~2 s)
- ARP-based MAC resolution with Android-11+ ping-sweep fallback
- OUI vendor lookup (Wireshark database bundled in assets)
- Service fingerprinting on 16 ports (HTTP/HTTPS/SSH/FTP/SMB/RTSP/RDP/VNC/…)
- Per-device known/unknown flag, alias, latency, first/last seen
- Topology view (force-directed graph) and device action menu (ping, copy, mark known)
- Scan-history snapshots (last 5) with new-device deltas

### Security audit & threat detection

- Evil-twin AP detection — same SSID, foreign BSSID
- ARP MITM detection — IP→MAC change vs. baseline + multi-IP-same-MAC conflict
- DNS hijack detection — primary DNS change vs. captured baseline
- Rogue DHCP scanner — find non-router DHCP offers on the LAN
- Weak Wi-Fi audit — WEP, open, WPS-enabled neighbors
- Captive portal analyzer — HTTP/HTTPS probe + phishing-form heuristics
- Per-network security score (0–100, A→F grade) feeding the home-screen widget

### Network tools (the Tools tab, 28 items)

`Ping` · `Traceroute` · `Port scanner` · `DNS lookup` · `Public IP` · `Wake-on-LAN` · `SSL inspector` · `VPN leak test` · `CVE search` · `Captive portal` · `HTTP-headers analyzer` · `Threat intelligence` (ip-api + AbuseIPDB) · `Shodan` (free InternetDB tier) · `Bluetooth scan` (Classic + BLE) · `WHOIS` · `Rogue DHCP` · `Export` (JSON + text) · `Speed test` · `Bandwidth monitor` · `mDNS discovery` · `ARP cache` · `IPv6 inspector` · `DNS benchmark` (compares public resolvers) · `Firewall tester` · `Network interfaces` · `HTTP client` · `Certificate transparency log search` · `Packet analyzer` (VpnService)

### Privacy & MAC

- Current MAC + randomization-bit detection
- Root MAC change via `ip link set <iface> address …`
- Per-SSID MAC profiles + auto-rotation interval (hours)
- Vendor lookup for the active MAC

### Background & widgets

- Foreground service with configurable interval and per-threat-type toggles
- Threat notifications with severity-mapped priority
- Home-screen widget with current security grade, threat count, last-scan timestamp
- 7-day rolling event log persisted in Room

### Export & sharing

- JSON or text reports of the current audit + event history
- File-provider-backed share intent
- All exports include device metadata and timestamp

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                       MainActivity (single)                     │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  Compose UI — sealed Screen routing + AnimatedContent   │    │
│  │  Dashboard · Networks · LAN · Security · Tools          │    │
│  │  MAC · Monitor · Settings                               │    │
│  └─────────────────────────────────────────────────────────┘    │
│              │                          │                       │
│   ┌──────────▼──────────┐    ┌──────────▼──────────┐            │
│   │  ViewModels (10)    │    │  Services (2)       │            │
│   │  StateFlow exposed  │    │  MonitorService     │            │
│   │  viewModelScope     │    │  PacketCaptureSvc   │            │
│   └──────────┬──────────┘    └──────────┬──────────┘            │
│              │                          │                       │
│   ┌──────────▼──────────────────────────▼──────────┐            │
│   │              Repositories / Engines            │            │
│   │  WifiRepo · LanRepo · MacRepo · ThreatDetector │            │
│   │  MonitorEngine · NetworkTools · IoTProfiler    │            │
│   └──────────┬───────────────┬────────────────┬────┘            │
│              │               │                │                 │
│      ┌───────▼─────┐  ┌──────▼─────┐  ┌──────▼─────┐            │
│      │  Room DB    │  │  DataStore │  │   System   │            │
│      │  (3 tables) │  │   Settings │  │    APIs    │            │
│      └─────────────┘  └────────────┘  └────────────┘            │
└─────────────────────────────────────────────────────────────────┘
```

**Threading model.** Every long-running call lives in `Dispatchers.IO` via `viewModelScope.launch(Dispatchers.IO)` or `withContext(Dispatchers.IO)`. Hot data is exposed as `StateFlow` so Compose can `collectAsState()` it without leaking. Services use a private `CoroutineScope(SupervisorJob() + Dispatchers.IO)` cancelled in `onDestroy()`.

**Persistence.** Three Room entities — `LanDevice`, `MacProfile`, `NetworkEvent` — with observable DAOs (`Flow<List<…>>`). Settings live in `DataStore Preferences` for atomic updates without race conditions. Schema is `fallbackToDestructiveMigration` because the app is pre-1.0; production would add a `Migration` list.

**Single Activity.** No `Fragment`s, no navigation library — Compose is the navigation surface. The current screen is a `sealed Screen` saved via `rememberSaveable(stateSaver = …)` so it survives rotation and process death. `AnimatedContent` provides a 220 ms crossfade between tabs.

---

## Project structure

```
app/src/main/java/com/eagleeye/
├── MainActivity.kt                  Entry point — sealed Screen routing, NavigationBar, AnimatedContent
│
├── data/                            Immutable domain models + Room schema
│   ├── WifiData.kt                  ScannedNetwork, WifiConnectionInfo, SignalSample, …
│   ├── LanDevice.kt                 LAN scan result entity + DAO model
│   ├── SecurityData.kt              Threat, ThreatLevel, SecurityScore, ArpEntry
│   ├── MonitorData.kt               EventType, EventSeverity, NetworkEvent, MonitorConfig
│   ├── ToolsData.kt                 28 result types — PingResult, WhoisResult, ShodanResult, …
│   ├── PacketData.kt                CapturedPacket, IpProtocol, PacketStats
│   └── db/                          Room: AppDatabase + 3 DAOs
│
├── modules/                         Feature modules, each self-contained
│   ├── wifi/                        WifiRepository + WifiViewModel + WifiQrGenerator
│   ├── lan/                         LanRepository + OuiLookup + LanViewModel
│   ├── security/                    ThreatDetector + SecurityViewModel + RogueDhcpDetector
│   ├── monitor/                     MonitorService + MonitorEngine + NotificationHelper
│   ├── packet/                      PacketCaptureService + PacketParser + PacketViewModel
│   ├── tools/                       28 individual tool clients + ToolsViewModel
│   ├── mac/                         MacRepository + MacViewModel (root via `su`)
│   ├── iot/                         IoTProfiler + SsdpScanner
│   ├── bluetooth/                   BluetoothViewModel (Classic + BLE scanner)
│   ├── cve/                         NVD CVE lookup client
│   ├── export/                      ReportExporter (JSON + text)
│   ├── portal/                      CaptivePortalDetector
│   └── settings/                    SettingsRepository (DataStore)
│
├── ui/
│   ├── screens/                     One Composable per top-level screen
│   │   ├── DashboardScreen.kt       Hero card, signal history, network info
│   │   ├── NetworkScanScreen.kt     Nearby networks with security grading
│   │   ├── LanScannerScreen.kt      Device list, search, filters, history
│   │   ├── SecurityScreen.kt        Audit result, threat list
│   │   ├── ToolsScreen.kt           Tool grid + 28 individual tool composables
│   │   ├── MacScreen.kt             MAC info + profiles
│   │   ├── MonitorScreen.kt         Background monitor + event log
│   │   ├── SettingsScreen.kt        DataStore-backed settings
│   │   ├── OnboardingScreen.kt      First-run permission rationale
│   │   ├── TopologyScreen.kt        Force-directed LAN graph
│   │   ├── SpectrumScreen.kt        Wi-Fi channel utilization
│   │   ├── GeoMapScreen.kt          GeoIP map of recent destinations
│   │   ├── ThreatRadarScreen.kt     Radial threat overview
│   │   ├── PacketAnalyzerScreen.kt  VpnService UI
│   │   └── PlaceholderScreens.kt    Empty / loading states
│   └── theme/
│       ├── Color.kt                 Refined dark palette (emerald/blue/red/amber on near-black)
│       ├── Type.kt                  System sans for UI, Monospace reserved for data
│       └── Theme.kt                 Material 3 dark color scheme
│
└── widget/                          Home-screen security-grade widget
    ├── SecurityWidget.kt
    └── SecurityWidgetReceiver.kt

app/src/test/java/com/eagleeye/       JVM unit tests (run via `./gradlew test`)
└── modules/packet/PacketParserTest.kt   8 tests for the IPv4/TCP/UDP/ICMP/DNS parser
```

---

## Tech stack

| Layer | Choice | Notes |
|-------|--------|-------|
| Language | Kotlin 2.0 | Coroutines + Flow, no Java in production code |
| UI | Jetpack Compose (Material 3) | Compose BOM 2024.11, material-icons-extended, navigation-compose unused (sealed-class routing instead) |
| Architecture | Single-Activity MVVM | Sealed `Screen`, `StateFlow`, `viewModelScope` |
| Persistence | Room 2.6 + DataStore Preferences 1.1 | 3 tables, observable DAOs, atomic settings updates |
| Background | Foreground `Service` (FGS) | `connectedDevice` type for Android 14+ semantics |
| Packet capture | `VpnService` | No root required — system intercepts via VPN routing |
| Concurrency | Coroutines 1.9 | `Dispatchers.IO` for everything network-bound, `SupervisorJob()` in services |
| QR / Bitmap | ZXing core 3.5 | Wi-Fi QR codes |
| Build | Gradle 8.7 + AGP 8.4 + Kotlin 2.0 | Version catalog, kapt for Room compiler |
| Testing | JUnit 4 + kotlinx-coroutines-test | JVM unit tests, no instrumented tests yet |

---

## Build & install

```bash
# Debug APK
ANDROID_HOME=$ANDROID_HOME ./gradlew assembleDebug

# Install on a connected device or emulator
ANDROID_HOME=$ANDROID_HOME ./gradlew installDebug

# Signed release APK (requires keystore wired into app/build.gradle.kts)
ANDROID_HOME=$ANDROID_HOME ./gradlew assembleRelease

# Clean
./gradlew clean
```

**Requirements**
- JDK 17
- Android SDK with API 35
- Min device: Android 8.0 (API 26)
- Some advanced features (MAC change, deauth detection) require **root**

**Output:** `app/build/outputs/apk/debug/app-debug.apk` (~20.7 MB debug, includes the bundled OUI database)

---

## Testing

```bash
# JVM unit tests (fast — no emulator)
./gradlew testDebugUnitTest

# Lint
./gradlew lintDebug
```

**Current test coverage**

- `PacketParserTest` — 8 tests covering IPv4 rejection, TCP/UDP/ICMP parsing, DNS query-name extraction, truncated buffers, port→service mapping, size clamping. Hand-crafted byte arrays exercise the parser without needing real packets.

**Lint health:** `0 errors, 52 warnings` (warnings are deprecated `WifiInfo.SSID`, AGP/library version bumps available, and a couple of always-true lint heuristics — none functional).

---

## Permissions

| Permission | Purpose | Runtime? |
|------------|---------|----------|
| `ACCESS_WIFI_STATE` | Read Wi-Fi connection metadata | install-time |
| `CHANGE_WIFI_STATE` | Trigger scans via `WifiManager.startScan()` | install-time |
| `ACCESS_FINE_LOCATION` | Required by Android to receive scan results since API 28 | **runtime** |
| `ACCESS_COARSE_LOCATION` | Companion to fine location | runtime |
| `ACCESS_NETWORK_STATE` | Detect connectivity changes via `ConnectivityManager` | install-time |
| `INTERNET` | CVE lookup, ip-api GeoIP, Shodan InternetDB, public IP, captive-portal probes | install-time |
| `CHANGE_NETWORK_STATE` | Used by Wake-on-LAN and VPN service | install-time |
| `CHANGE_WIFI_MULTICAST_STATE` | mDNS / Bonjour discovery | install-time |
| `FOREGROUND_SERVICE` | MonitorService lifecycle | install-time |
| `FOREGROUND_SERVICE_CONNECTED_DEVICE` | FGS type required by Android 14+ | install-time |
| `FOREGROUND_SERVICE_DATA_SYNC` | Legacy FGS support pre-Android 14 | install-time |
| `POST_NOTIFICATIONS` | Threat alerts on Android 13+ | runtime |
| `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` | BT scanner on Android 12+ | runtime |
| `BLUETOOTH` / `BLUETOOTH_ADMIN` | Pre-Android-12 fallback | install-time |
| `RECEIVE_BOOT_COMPLETED` | Auto-restart monitor across reboots | install-time |

**The app launches `RequestMultiplePermissions` only for the still-missing subset** — already-granted permissions are not re-prompted.

---

## Engineering notes

A handful of decisions worth calling out:

- **Android 11+ ARP restriction.** `/proc/net/arp` started returning zeroed MACs on API 30. EagleEye detects the empty cache and falls back to a parallel ICMP ping sweep across the `/24`. New-device detection still works (IP becomes the key); ARP-spoof and MAC-conflict checks gracefully degrade with a one-time `INFO` event so the user knows monitoring is limited.

- **VpnService for packet capture.** Avoids root entirely — the system routes all device traffic through our VPN interface, we read packets from a `ParcelFileDescriptor`, parse, and drop them. Internet is paused for the device while capture is active (documented in-screen).

- **Foreground-service hygiene.** `MonitorService` uses `FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE` to avoid the Android 14+ 6-hour daily cap on `dataSync` services. `PacketCaptureService` calls `startForeground()` *before* `Builder.establish()` because Android 12+ throws `ForegroundServiceStartNotAllowedException` otherwise.

- **`scanResults` SecurityException trap.** Every call to `WifiManager.scanResults` is wrapped in `try { … } catch (SecurityException) { null }` because permission can be revoked while the app is running — six call sites were crashing before this fix.

- **Memory caps in the packet analyzer.** `dnsQuerySet` and `destCounts` are bounded (200 / 1000 entries with FIFO eviction or top-N retention) so long capture sessions can't OOM.

- **State survival.** The top-level `Screen` is `rememberSaveable` with a `Saver<Screen, String>` so the active tab survives rotation and process death.

- **Refined palette over neon.** The "Cyber*" colour names from the original cyberpunk palette are kept for codebase stability, but the *values* moved to Tailwind 500-tier hues (emerald, blue, red, amber) on a near-black layered surface hierarchy — reads as a polished dashboard instead of a 1990s terminal.

---

## Roadmap & known limits

**Limitations of the current platform**

- ARP-based detection is degraded on Android 11+ (kernel restriction, not fixable in user space without root).
- MAC randomization change requires root (`ip link set wlan0 address …`).
- Deauth-attack detection requires monitor-mode Wi-Fi, unavailable on stock Android.
- The packet analyzer routes traffic through a `VpnService` — only **this device's** traffic is visible, not the LAN's.

**Could do next**

- Migrate the 800-line user guide to a docs site (MkDocs / Docusaurus).
- Instrumented Compose UI tests.
- AGP 9.x + Compose BOM bump (currently AGP 8.4.2 with `compileSdk = 35` emits a warning).
- Move all hardcoded service-name port maps into a single shared constant.
- Optional Wireshark-format `.pcap` export from the packet analyzer.

---

## License

MIT — see [LICENSE](LICENSE).

This is a security tool intended for use on networks you own or have explicit authorization to audit. Do not run scans against networks without permission.

---

<div align="center">

**[← Back to top](#eagleeye)** · [User Guide](docs/USER_GUIDE.md) · [Ελληνικά](README.el.md)

</div>
