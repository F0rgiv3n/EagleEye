# EagleEye

Android cybersecurity & network intelligence tool. Built for Wi-Fi analysis, LAN scanning, threat detection, and network auditing.

---

## Build

```bash
# Debug APK
ANDROID_HOME=/home/currahee/Android/Sdk ./gradlew assembleDebug

# Install on device/emulator
ANDROID_HOME=/home/currahee/Android/Sdk ./gradlew installDebug

# Release APK
ANDROID_HOME=/home/currahee/Android/Sdk ./gradlew assembleRelease

# Clean
ANDROID_HOME=/home/currahee/Android/Sdk ./gradlew clean
```

**Requirements:** Java 17, Android SDK (API 35), min device API 26 (Android 8.0)
**APK output:** `app/build/outputs/apk/debug/app-debug.apk`

---

## Project Structure

```
app/src/main/java/com/eagleeye/
├── MainActivity.kt              # Entry point, bottom nav, screen routing
├── ui/
│   ├── theme/
│   │   ├── Color.kt             # Cyber dark palette (green/blue/red)
│   │   ├── Type.kt              # Monospace typography
│   │   └── Theme.kt             # MaterialTheme dark scheme
│   └── screens/
│       ├── DashboardScreen.kt   # Wi-Fi connection info + signal card
│       ├── NetworkScanScreen.kt # Nearby networks list with security grades
│       ├── LanScannerScreen.kt  # [Part 2] LAN host discovery
│       ├── SecurityScreen.kt    # [Part 3] Threat detection engine
│       ├── ToolsScreen.kt       # [Part 4] Ping, traceroute, port scan
│       └── PlaceholderScreens.kt
├── data/
│   ├── WifiData.kt              # WifiConnectionInfo, ScannedNetwork, enums
│   ├── LanDevice.kt             # [Part 2] LAN host model + Room entity
│   └── AppDatabase.kt           # [Part 2] Room database
└── modules/
    ├── wifi/
    │   ├── WifiRepository.kt    # WifiManager wrapper, scan results
    │   └── WifiViewModel.kt     # StateFlow: connectionInfo, scanResults
    ├── lan/                     # [Part 2]
    │   ├── LanRepository.kt     # ARP sweep, ping, port scan, OUI lookup
    │   └── LanViewModel.kt
    ├── security/                # [Part 3]
    │   ├── ThreatDetector.kt    # ARP spoof, evil twin, DNS spoof detection
    │   └── SecurityViewModel.kt
    └── tools/                   # [Part 4]
        ├── PingUtil.kt
        ├── TracerouteUtil.kt
        └── PortScanner.kt
```

---

## Features

### Part 1 — Foundation & Wi-Fi Dashboard ✅
| Feature | Status |
|---|---|
| Gradle project (Kotlin DSL + AGP 8.4 + Compose) | ✅ Done |
| Dark cybersecurity theme (cyber green/blue palette) | ✅ Done |
| Bottom navigation (5 tabs) | ✅ Done |
| Wi-Fi Dashboard — SSID, BSSID, IP, Gateway, Subnet, DNS | ✅ Done |
| Signal strength (RSSI → bar + percentage + label) | ✅ Done |
| Frequency band detection (2.4 / 5 / 6 GHz) | ✅ Done |
| Link speed display | ✅ Done |
| Real-time auto-refresh every 3 seconds | ✅ Done |
| Network Scan — nearby APs with channel, band, RSSI | ✅ Done |
| Security grade per AP (WPA3/WPA2/WPA/WEP/OPEN) | ✅ Done |
| Current network highlight in scan list | ✅ Done |
| Hidden SSID detection | ✅ Done |

---

### Part 2 — LAN Scanner ✅
| Feature | Status |
|---|---|
| ARP + ICMP sweep (full subnet /24 discovery, 20 concurrent probes) | ✅ Done |
| Device list: IP, MAC, hostname, online status | ✅ Done |
| OUI/vendor lookup (bundled Wireshark database, 56k entries) | ✅ Done |
| Ping latency per host with color coding | ✅ Done |
| Port scanner — 16 common ports with service names | ✅ Done |
| Device persistence (Room DB — first seen / last seen) | ✅ Done |
| "NEW" badge for unknown devices | ✅ Done |
| Mark device as known | ✅ Done |
| Smart device icon (camera, server, SSH, web, etc.) | ✅ Done |
| Expandable device card with full details | ✅ Done |
| Scan progress bar (host counter) | ✅ Done |
| Stats: Online / Total / Unknown counts | ✅ Done |

---

### Part 3 — Security Engine ✅
| Feature | Status |
|---|---|
| ARP spoofing / MITM detection (MAC baseline + conflict detection) | ✅ Done |
| Evil twin AP detection (same SSID, different BSSID) | ✅ Done |
| DNS spoofing detection (baseline DNS + change alert) | ✅ Done |
| Unknown DNS server warning | ✅ Done |
| WPS vulnerability detection | ✅ Done |
| WEP / Open network / WPA weak encryption alerts | ✅ Done |
| Hidden SSID warning | ✅ Done |
| Multiple open networks alert | ✅ Done |
| Network security score 0–100 with grade (A+→F) | ✅ Done |
| Score breakdown: Encryption / No WPS / No Evil Twin / DNS / ARP / Devices | ✅ Done |
| Threat cards with severity (CRITICAL / HIGH / MEDIUM / LOW / INFO) | ✅ Done |
| Recommendations per threat | ✅ Done |
| Auto-audit on app launch | ✅ Done |

---

### Part 4 — Network Tools 📋 Planned
| Feature | Status |
|---|---|
| Ping with min/avg/max/jitter statistics | 📋 |
| Visual traceroute with hop list | 📋 |
| Port scanner (TCP connect, top 1000 ports) | 📋 |
| Service/banner detection (HTTP, SSH, FTP, Telnet) | 📋 |
| DNS lookup / reverse DNS | 📋 |
| Public IP + IPv6 detection | 📋 |
| Wake on LAN | 📋 |

---

### Part 5 — MAC Privacy 📋 Planned
| Feature | Status |
|---|---|
| Display device MAC address | 📋 |
| Detect if MAC is randomized | 📋 |
| Custom MAC address change (root) | 📋 |
| Auto MAC rotation scheduler | 📋 |
| Per-network MAC profiles | 📋 |

---

### Part 6 — Advanced / CVE Intelligence 📋 Planned
| Feature | Status |
|---|---|
| CVE lookup via NVD API (router model → known vulns) | 📋 |
| SSL/TLS inspector (expired certs, weak ciphers) | 📋 |
| Captive portal analyzer | 📋 |
| IoT device profiling (default credentials check) | 📋 |
| VPN leak detector (DNS, WebRTC, IPv6) | 📋 |
| Packet capture broadcast traffic (root) | 📋 |
| Export reports (PDF / JSON / CSV) | 📋 |
| Dark mode widgets + auto-scan notifications | 📋 |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material3 |
| Architecture | MVVM (ViewModel + StateFlow) |
| Database | Room |
| Build | Gradle 8.7 + AGP 8.4.2 |
| Min SDK | API 26 (Android 8.0) |
| Target SDK | API 35 (Android 15) |

---

## Permissions

| Permission | Purpose |
|---|---|
| `ACCESS_WIFI_STATE` | Read Wi-Fi connection info |
| `CHANGE_WIFI_STATE` | Trigger network scans |
| `ACCESS_FINE_LOCATION` | Required by Android for Wi-Fi scan results |
| `ACCESS_NETWORK_STATE` | Monitor connectivity changes |
| `INTERNET` | CVE API, public IP lookup, network tools |
| `FOREGROUND_SERVICE` | Background scanning service |
| `POST_NOTIFICATIONS` | Threat alerts |
