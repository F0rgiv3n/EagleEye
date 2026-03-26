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

### Part 4 — Network Tools ✅
| Feature | Status |
|---|---|
| Ping — min/avg/max/jitter stats + visual bar chart per sample | ✅ Done |
| Traceroute — hop list with IP, hostname, latency, timeout detection | ✅ Done |
| Port scanner — Quick (10) / Full (35 ports) with service names | ✅ Done |
| Banner grabbing per open port | ✅ Done |
| Dangerous port warnings (Telnet, FTP, Metasploit, PPTP) | ✅ Done |
| DNS lookup — forward A/AAAA + reverse PTR | ✅ Done |
| Public IPv4 + IPv6 detection (dual provider fallback) | ✅ Done |
| Local IP + gateway display | ✅ Done |
| Wake on LAN — magic packet broadcast | ✅ Done |
| 6-tab tool selector UI | ✅ Done |

---

### Part 7 — Background Monitor + Notifications + Timeline ✅
| Feature | Status |
|---|---|
| Foreground Service με persistent notification | ✅ Done |
| Configurable scan interval (5/10/15/30/60 min) | ✅ Done |
| ARP spoof detection in background | ✅ Done |
| Evil twin detection in background | ✅ Done |
| New LAN device detection + notification | ✅ Done |
| DNS change detection + notification | ✅ Done |
| MAC conflict detection (multi-IP same MAC) | ✅ Done |
| Push notifications με severity priority | ✅ Done |
| Network Timeline — πλήρες ιστορικό events | ✅ Done |
| Timeline / Threats Only tabs | ✅ Done |
| Relative timestamps (just now / Xm ago / Xh ago) | ✅ Done |
| Unread badge στο bottom nav tab | ✅ Done |
| Per-notification toggle (new device, ARP, evil twin, DNS, weak sec) | ✅ Done |
| Event stats: CRITICAL / HIGH / TODAY / UNREAD | ✅ Done |
| Auto-cleanup events > 7 days | ✅ Done |

---

### Part 5 — MAC Privacy ✅
| Feature | Status |
|---|---|
| Display current MAC with type badge (REAL / RANDOMIZED / CUSTOM) | ✅ Done |
| Vendor lookup from current MAC | ✅ Done |
| Locally-administered bit detection (real vs randomized) | ✅ Done |
| Warning when real hardware MAC is exposed | ✅ Done |
| Custom MAC change via root (ip link set) | ✅ Done |
| Generate random locally-administered MAC | ✅ Done |
| One-tap MAC randomize (root) | ✅ Done |
| Per-network MAC profiles (SSID → MAC mapping, persisted in Room) | ✅ Done |
| Auto-rotate flag + interval (hours) per profile | ✅ Done |
| Apply profile for current network | ✅ Done |
| Hardware MAC display (root read) | ✅ Done |

---

### Part 6 — CVE Intelligence, SSL, VPN Leak & Export ✅
| Feature | Status |
|---|---|
| CVE lookup via NIST NVD API v2 (keyword → CVE list with CVSS score) | ✅ Done |
| CVE severity color coding (CRITICAL/HIGH/MEDIUM/LOW) | ✅ Done |
| Expandable CVE cards with description, date, references | ✅ Done |
| In-memory CVE cache (avoid duplicate API calls) | ✅ Done |
| SSL/TLS inspector — grade A+/A/B/C/F | ✅ Done |
| Certificate details: subject, issuer, expiry, days remaining | ✅ Done |
| Weak cipher/protocol detection (RC4, DES, TLSv1.0, TLSv1.1) | ✅ Done |
| Self-signed certificate detection | ✅ Done |
| VPN leak detector — DNS leak, IPv6 leak | ✅ Done |
| VPN active status detection | ✅ Done |
| Export report as JSON (full structured data) | ✅ Done |
| Export report as plain text (human-readable) | ✅ Done |
| Share via Android share sheet (FileProvider) | ✅ Done |
| Export triggered from Security screen via share icon | ✅ Done |

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
