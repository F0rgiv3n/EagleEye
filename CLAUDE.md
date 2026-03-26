# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**EagleEye** is a planned Android application for Wi-Fi security analysis, network management, and cybersecurity auditing. The project is currently in the specification phase — no source code exists yet.

The spec is in `EagleEye info` (Greek language). Key goal: build a comprehensive Android cybersecurity tool with capabilities comparable to Nmap and other professional network auditing tools.

## Planned Architecture

The app is organized around four feature pillars:

### 1. Wi-Fi / Network Analysis
- Current connection details: SSID, BSSID, IP, gateway, subnet, DNS, link speed, RSSI, band (2.4/5/6 GHz)
- Nearby network scanner: SSID, BSSID, channel, frequency, signal strength, security type (WEP/WPA/WPA2/WPA3), hidden networks
- LAN device scanner: IP, MAC, hostname, vendor (OUI lookup), online status
- Network tools: ping, latency, traceroute, basic port scanner, public/local IP, IPv6

### 2. Privacy / MAC Address
- Display and detect whether device MAC is randomized
- Custom MAC address changes and auto MAC rotation
- Per-network randomized MAC support

### 3. Security / Cybersecurity Detection
- Unknown device detection on LAN
- ARP spoofing / MITM detection
- Weak/open Wi-Fi detection: open networks, WEP, WPS-enabled APs
- Evil twin detection (same SSID, different BSSID)
- DNS spoofing detection (monitor router DNS for changes)
- Deauth attack detection (where permitted by Android)
- Per-network security scoring with vulnerability warnings

### 4. Advanced / Optional Features
- Packet sniffing of broadcast traffic (root required)
- IoT/router vulnerability scanning
- CVE lookup for known vulnerabilities
- Network behavior monitoring
- Wake on LAN
- Export/share results
- Dark mode, widgets, auto-scan notifications, device info persistence

## Build Commands

```bash
# Build debug APK
ANDROID_HOME=/home/currahee/Android/Sdk ./gradlew assembleDebug

# Install on connected device/emulator
ANDROID_HOME=/home/currahee/Android/Sdk ./gradlew installDebug

# Build release APK
ANDROID_HOME=/home/currahee/Android/Sdk ./gradlew assembleRelease

# Clean build
ANDROID_HOME=/home/currahee/Android/Sdk ./gradlew clean
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Technology Stack (to be decided)

For Android development, likely candidates:
- **Language:** Kotlin
- **Build system:** Gradle (Android)
- **Min SDK:** TBD (deauth/packet sniffing features may require higher API levels or root)
- **Key Android APIs:** `WifiManager`, `ConnectivityManager`, `NetworkInfo`, `WifiInfo`
- **Root features:** require `su` shell access or a root library (e.g., libsu)

## Development Notes

- Several features (packet sniffing, deauth detection, MAC spoofing) require root access or specific Android permissions — design these as optional/graceful-degradation features.
- LAN device scanning uses ARP or ICMP ping sweeps across the subnet — consider performance and battery impact.
- OUI/vendor lookup requires a local MAC vendor database (e.g., Wireshark's OUI list) bundled in assets.
- CVE lookup will require an external API (e.g., NVD/NIST).
