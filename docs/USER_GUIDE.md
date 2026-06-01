# User Guide — Beginners in Networks & Protocols

> This guide explains every feature assuming you have no prior knowledge of networking or cybersecurity. Each section explains the underlying concept first, then how to use EagleEye to interact with it.
>
> [← Back to README](../README.md)

---

### Part 0 — Networking Concepts You Need to Know

Before diving into the app, here are the key terms you will see everywhere:

**IP Address**
Every device on a network gets a number called an IP address (e.g. `192.168.1.5`). Think of it like a house address — it tells other devices where to send data. Your router assigns these automatically via a protocol called DHCP. "Local" or "private" IPs start with `192.168.x.x`, `10.x.x.x`, or `172.16–31.x.x` and are only visible inside your home network. "Public" IPs are visible from the internet.

**MAC Address**
A MAC address (e.g. `A4:C3:F0:12:34:56`) is a hardware identifier burned into your network card by the manufacturer. Unlike an IP address (which can change), the MAC address is unique to your device. The first 3 bytes (the "OUI") identify the manufacturer — e.g. `A4:C3:F0` belongs to Apple. EagleEye uses this to identify what kind of device is on your network without needing a hostname.

**SSID / BSSID**
SSID is the name of a Wi-Fi network (e.g. "HomeNetwork"). BSSID is the MAC address of the access point broadcasting that name. Two networks can have the same SSID but different BSSIDs — this is how "evil twin" attacks work (an attacker creates a fake AP with the same name to intercept your traffic).

**Router / Gateway**
Your router is the device that connects your home network to the internet. Its local IP address is your "gateway" (usually `192.168.1.1` or `192.168.0.1`). All traffic goes through it. If an attacker controls your gateway (via ARP spoofing), they can see all your network traffic.

**DNS — Domain Name System**
DNS translates human-readable names (like `google.com`) into IP addresses. Your router usually acts as a DNS server, forwarding requests to your ISP or a public resolver (like Google's `8.8.8.8` or Cloudflare's `1.1.1.1`). If an attacker changes your DNS settings, they can redirect you to fake websites even if you type the correct address.

**ARP — Address Resolution Protocol**
ARP maps IP addresses to MAC addresses on your local network. When your phone wants to talk to `192.168.1.1`, it broadcasts "Who has 192.168.1.1?" and the router replies with its MAC address. ARP spoofing is a MITM (man-in-the-middle) attack where an attacker sends fake ARP replies to trick your device into sending traffic to them instead of the router.

**Port**
A port is a number (0–65535) that identifies a specific service on a device. Think of a building with many doors — the IP is the building address, and the port is the door number. Port 80 = HTTP web traffic, 443 = HTTPS, 22 = SSH remote login, 21 = FTP file transfer. If a port is "open", the service is running and accepting connections. Many ports open on a device can indicate a server, NAS, or misconfigured IoT device.

**Signal Strength (RSSI)**
RSSI (Received Signal Strength Indicator) is measured in dBm (decibels relative to 1 milliwatt). Values range from about -30 dBm (excellent, very close) to -90 dBm (barely usable). More negative = weaker. EagleEye converts this to a 0–100% scale with labels: Excellent / Good / Fair / Weak.

**Frequency Bands**
Wi-Fi operates on different radio frequency bands:
- **2.4 GHz** — longer range, penetrates walls better, but more crowded (used by microwaves, baby monitors, neighbors). Channels 1, 6, 11 are non-overlapping.
- **5 GHz** — faster, less interference, but shorter range.
- **6 GHz** — newest (Wi-Fi 6E), fastest, least interference, but shortest range.

**Subnet / Subnet Mask**
A subnet groups devices that can communicate directly without going through a router. A `/24` subnet (mask `255.255.255.0`) means all devices with addresses `192.168.1.1` through `192.168.1.254` are on the same local network. EagleEye scans all 254 possible hosts in your subnet when doing a LAN scan.

**Ping / Latency**
Ping sends a small packet to a device and measures how long it takes to get a reply (Round-Trip Time, in milliseconds). Low latency (< 10ms on LAN) = good connection. EagleEye uses ping to detect whether hosts are online and how fast they respond.

**Traceroute**
Traceroute shows every "hop" a packet takes from your device to a destination, and how long each hop takes. Each hop is a router along the path. It helps you diagnose where network slowdowns or outages occur.

**SSL/TLS**
SSL (Secure Sockets Layer) and its successor TLS (Transport Layer Security) encrypt network communications. When you see `https://` in a URL, TLS is protecting your data. Older versions (TLS 1.0, TLS 1.1, SSL 2.0, SSL 3.0) have known vulnerabilities and should not be used.

---

### Part 1 — Dashboard

**What it shows:**
The Dashboard is the first screen you see. It displays everything about your current Wi-Fi connection at a glance.

| Field | What it means |
|---|---|
| SSID | The name of the network you're connected to |
| BSSID | The MAC address of the specific access point (router/repeater) you're connected to |
| IP Address | Your device's local IP on this network |
| Gateway | Your router's IP — where all your traffic goes |
| Subnet Mask | Defines the size of your local network |
| DNS 1 / DNS 2 | The servers that resolve domain names for you |
| Link Speed | How fast your Wi-Fi connection currently is (Mbps) |
| Signal | Your signal strength as RSSI (dBm) and percentage |
| Band | Whether you're on 2.4 GHz, 5 GHz, or 6 GHz |
| Security | Encryption protocol in use (WPA3, WPA2, WPA, WEP, or OPEN) |

**The signal card** at the bottom shows a visual bar for signal quality and a color-coded label. Green = good, orange = fair, red = weak.

**What to do if you see problems:**
- DNS showing `0.0.0.0` or an unexpected IP → could indicate DNS hijacking (check Security screen)
- Signal below 30% → move closer to your router
- Showing OPEN security → your network has no password; anyone nearby can connect

---

### Part 2 — Network Scan

**What it does:**
Scans for all Wi-Fi access points (APs) visible from your location — not just yours, but all neighbors' networks too.

**Each result shows:**
- **SSID** — network name (or `<Hidden>` if the AP is hiding its name)
- **BSSID** — the AP's MAC address (useful for identifying specific hardware)
- **Signal** — RSSI in dBm + percentage bar
- **Channel** — which radio channel the AP uses (e.g. channel 6 on 2.4 GHz)
- **Frequency** — 2.4 / 5 / 6 GHz
- **Security Grade** — color-coded badge:
  - 🟢 WPA3 — best, modern encryption
  - 🟡 WPA2 — good, current standard
  - 🟠 WPA — old, has vulnerabilities
  - 🔴 WEP — broken, easily cracked in minutes
  - 🔴 OPEN — no encryption, all traffic readable

**Your current network** is highlighted at the top of the list.

**What to look out for:**
- Networks with the same SSID as yours but different BSSID → potential evil twin attack
- Many open networks in a public place → risky environment
- Your own network showing WEP or WPA → time to upgrade your router settings

---

### Part 3 — LAN Scanner

**What it does:**
Discovers all devices connected to the same local network as you. It sweeps all 254 possible IP addresses in your subnet, pinging each one and reading the ARP table to get MAC addresses.

**How to use:**
1. Tap the **Scan** button (radar icon, top right)
2. A progress bar shows how far the sweep has reached (1–254 hosts)
3. Devices appear in real-time as they are found
4. Tap any device card to expand it for full details

**What each device card shows:**
- **IP address** — the device's address on your network
- **Hostname** — the device's self-reported name (if it has one)
- **MAC address** — hardware identifier
- **Vendor** — manufacturer identified from the MAC's OUI prefix (e.g. "Apple Inc.", "Samsung Electronics")
- **Latency** — ping response time in ms
- **Open Ports** — which service ports are accepting connections
- **First Seen / Last Seen** — from the app's persistent database
- **NEW badge** — this device was not seen in any previous scan

**Filtering and Search:**
- Use the **search bar** to filter by IP, hostname, vendor, or MAC
- Use **filter chips** to show only: All / Online / Unknown (new) / Has Open Ports

**Device Actions (long-press or expand card):**
- **Mark as Known** — removes the NEW badge; you recognize this device
- **Set Alias** — give the device a friendly name (e.g. "Dad's laptop")
- **IoT Profile** — fingerprint the device for vulnerabilities (see Part 8)
- **Wake on LAN** — send a magic packet to wake the device remotely

**Topology Map** (hub icon, top right):
Shows all discovered devices as nodes connected to the gateway. Color coding:
- Green = online and known
- Orange = online but unknown/new
- Gray = offline
- Red dot = 3+ open ports (potential risk)

Pinch to zoom, drag to pan, tap any node for its details.

**Scan History** (clock icon, appears after first scan):
Shows the last 5 scan summaries — total devices found, how many were online, and how many were new/unknown per scan.

---

### Part 4 — Security

**What it does:**
Runs automated threat detection against your current network and shows a security score with detailed findings.

**Security Score (0–100 / Grade A+ to F):**
The score is calculated from multiple factors:
- Encryption type (WPA3 = full points, WEP/OPEN = 0)
- WPS enabled (vulnerable to brute-force attacks → penalty)
- Evil twin AP detected (-30 points)
- DNS suspicious or changed (-20 points)
- ARP spoofing detected (-40 points)
- Unknown devices on LAN (-5 points each)

**Threat Severity Levels:**
| Level | Color | Meaning |
|---|---|---|
| CRITICAL | Red | Active attack likely in progress |
| HIGH | Orange | Serious vulnerability or suspicious activity |
| MEDIUM | Yellow | Moderate risk, warrants attention |
| LOW | Cyan | Minor issue |
| INFO | Gray | Informational finding |

**Threat Types Explained:**

**ARP Spoofing / MITM**
Someone on your network is sending fake ARP replies to redirect traffic through their device. This means they can read your unencrypted traffic, inject data into pages, or steal credentials.
*What to do:* Disconnect immediately. Check who is connected (LAN Scanner). If on public Wi-Fi, use a VPN.

**Evil Twin AP**
An access point with the same SSID as your network but a different BSSID has been detected. An attacker may be trying to get you or others to connect to their fake AP instead.
*What to do:* Do not connect to unknown networks with familiar names. Verify the BSSID in your Dashboard matches your legitimate router.

**DNS Changed / DNS Hijacking**
Your router's DNS server address has changed since the monitoring session started. Attackers sometimes gain access to routers and change DNS settings to redirect websites.
*What to do:* Check your router admin panel. Ensure DNS is set to a trusted server (`8.8.8.8`, `1.1.1.1`, or your ISP's servers). Change your router admin password.

**Weak Encryption (WEP/WPA/OPEN)**
Your network uses an insecure or no encryption protocol.
- OPEN: anyone nearby can read all traffic
- WEP: can be cracked in under 60 seconds with freely available tools
- WPA (without 2/3): has TKIP vulnerabilities
*What to do:* Log into your router and change to WPA2 or WPA3.

**WPS Enabled**
Wi-Fi Protected Setup allows devices to connect by pressing a button or entering an 8-digit PIN. The PIN-based WPS is vulnerable to brute-force in hours.
*What to do:* Disable WPS in your router settings.

**Unknown Device on LAN**
A device appeared on your network that was not seen in previous scans and has not been marked as known.
*What to do:* Identify the device using its vendor/hostname. If unrecognized, check if a guest connected — or change your Wi-Fi password.

**Background Monitoring (Monitor tab in Security):**
Tap **Start Monitor** to run continuous threat scans in the background. A persistent notification confirms it is running. Configure the scan interval in Settings. You will receive push notifications when threats are detected.

---

### Part 5 — Network Tools

**What it does:**
A suite of 14 diagnostic and investigation tools, organized in tabs.

---

#### Ping
Tests whether a host is reachable and measures round-trip time.

**How to use:** Enter an IP address or hostname (e.g. `192.168.1.1` or `google.com`), choose packet count (5–50), tap **Ping**.

**Reading results:**
- **Min / Avg / Max** — fastest, average, and slowest response times
- **Jitter** — variation in latency (high jitter = unstable connection)
- **Bar chart** — each bar = one ping response. Taller = higher latency. Missing bar = packet lost.
- **Packet loss** — percentage of pings that got no response. >5% = problem.

**Good values:** LAN devices < 5ms, local ISP router < 20ms, international servers < 200ms.

---

#### Traceroute
Shows the path your traffic takes across the internet, hop by hop.

**How to use:** Enter a hostname (e.g. `google.com`), tap **Traceroute**.

**Reading results:**
Each row = one router (hop) your packet passed through:
- **Hop number** — 1 is usually your router, last hop is the destination
- **IP / Hostname** — the router's address
- **Latency** — how long to reach that hop
- **\* \* \*** — the router did not respond (common, not always an error)

**What to look for:** A sudden jump in latency between hops shows where a slowdown is. If traceroute stops before reaching the destination, there is a routing problem at that point.

---

#### Port Scan
Checks which network services are running on a device.

**How to use:** Enter an IP address, choose **Quick** (10 common ports) or **Full** (35 ports), tap **Scan**.

**Reading results:**
- **Open** ports are listed with their service name and any banner grabbed from the service
- **Dangerous port warnings** appear for insecure services:
  - Port 21 (FTP) — sends passwords in plaintext
  - Port 23 (Telnet) — sends all data in plaintext
  - Port 4444 (Metasploit) — Metasploit C2 framework default port
  - Port 1723 (PPTP VPN) — broken VPN protocol

**Why this matters:** If a device in your home has port 22 (SSH) or 3389 (RDP) open and is reachable from the internet, anyone can attempt to brute-force login to it.

---

#### DNS Lookup
Resolves hostnames to IP addresses and IP addresses to hostnames.

**How to use:** Enter a domain (e.g. `github.com`) or IP address, tap **Lookup**.

**Reading results:**
- **A record** — the IPv4 address(es) the domain resolves to
- **AAAA record** — IPv6 address(es)
- **PTR record** — reverse lookup: what hostname an IP claims to be

**Useful for:** Checking whether a suspicious IP has a known hostname, or verifying that a domain resolves to the expected address (not a hijacked one).

---

#### Public IP
Finds your public internet-facing IP address (the one websites see when you connect).

**Reading results:**
- **IPv4** — your public IPv4 address
- **IPv6** — your public IPv6 address (if your ISP supports it)
- **Gateway / Local IP** — your local network addresses for reference

**Why it matters:** If you're using a VPN, your public IP should show the VPN server's location, not your real one. EagleEye's VPN Leak Detector (Security screen) verifies this.

---

#### Wake on LAN
Sends a "magic packet" to wake a sleeping device remotely — as long as the device supports WoL and is configured for it.

**How to use:** Enter the target MAC address (or use a device from LAN Scanner), optionally the IP, tap **Send Wake Packet**.

**Requirements:** The target device must have "Wake on LAN" enabled in its BIOS/settings, and be connected via Ethernet or Wi-Fi with WoL support.

---

#### HTTP Client
Makes HTTP/HTTPS requests and shows the raw response — useful for testing APIs, checking server responses, and seeing what headers a server sends.

**How to use:** Enter a URL, select method (GET / POST / HEAD), optionally add a request body, tap **Send**.

**Reading results:**
- **Status code** — `200 OK` = success, `404` = not found, `301`/`302` = redirect, `500` = server error
- **Response headers** — metadata from the server (content type, cache settings, security headers)
- **Response body** — the actual content returned

---

#### SSL Inspector
Checks the security of a website's TLS/SSL certificate and connection.

**How to use:** Enter a hostname (e.g. `example.com`), tap **Inspect**.

**Reading results:**
- **Grade** — A+ (excellent) through F (broken)
- **Certificate subject / issuer** — who the cert is for and who signed it
- **Expiry** — days until the certificate expires (expired = connection unsafe)
- **Self-signed warning** — the cert was not issued by a trusted authority
- **Weak cipher/protocol** — the server accepts TLS 1.0, TLS 1.1, RC4, or DES (all broken)

---

#### HTTP Headers
Analyzes a website's security response headers.

**How to use:** Enter a URL, tap **Check Headers**.

**Reading results:**
Each of the 8 checked headers gets a PASS/MISSING badge:
- **HSTS** — forces HTTPS, prevents downgrade attacks
- **CSP** — Content Security Policy, prevents XSS injection
- **X-Frame-Options** — prevents clickjacking (loading the site in a hidden iframe)
- **X-Content-Type-Options** — prevents MIME type confusion attacks
- **Referrer-Policy** — controls what URL is sent to other sites when you click a link
- **Permissions-Policy** — controls browser feature access (camera, microphone, etc.)
- **COOP / COEP** — cross-origin isolation for Spectre mitigation

A score of 0–100 and a grade are computed. Well-configured sites should score 70+.

---

#### Threat Intelligence
Looks up an IP address to gather intelligence about its reputation and origin.

**How to use:** Enter an IP address, tap **Look Up**.

**Reading results:**
- **Country / City / ISP** — where the IP is located and who owns it
- **Proxy / VPN / Datacenter** — whether the IP belongs to a known anonymization service
- **AbuseIPDB** — if you have an API key set in Settings, shows the IP's abuse score and report history (0–100, higher = more reports of malicious activity)
- **Shodan InternetDB** — open ports, known CVEs, and tags associated with the IP

**When to use:** If an unfamiliar device appeared on your LAN, or you see a suspicious IP in the Packet Analyzer, look it up here to find out what it is.

---

#### Shodan Lookup
Queries Shodan's InternetDB for information about an IP — open ports, services, vulnerabilities (CVEs), and tags — without requiring a Shodan API key.

**How to use:** Enter an IP address, tap **Search**.

---

#### CVE Lookup
Searches the NIST National Vulnerability Database (NVD) for known security vulnerabilities by keyword.

**How to use:** Enter a product name (e.g. "OpenSSL 3.0" or "Hikvision camera"), tap **Search CVEs**.

**Reading results:**
- **CVE ID** — the vulnerability's official identifier (e.g. CVE-2021-44228)
- **CVSS Score** — severity from 0 to 10 (10 = most critical):
  - 🔴 CRITICAL: 9.0–10.0 — requires immediate action
  - 🟠 HIGH: 7.0–8.9 — serious, patch soon
  - 🟡 MEDIUM: 4.0–6.9 — moderate risk
  - 🔵 LOW: 0.1–3.9 — minimal risk
- **Description** — what the vulnerability is and how it can be exploited
- **Published date** — when the vulnerability was disclosed
- **References** — links to patches, advisories, and proof-of-concept code

**Practical use:** After identifying a device in LAN Scanner (e.g. a Hikvision IP camera), search CVEs for the vendor/model to see if it has known unpatched vulnerabilities.

---

#### Captive Portal Analyzer
Detects and analyzes captive portals — the login pages shown at hotels, airports, cafés when connecting to their Wi-Fi.

**How to use:** Tap **Analyze Portal**. EagleEye probes 4 known connectivity-check endpoints and follows redirects.

**Reading results:**
- **Portal detected** — a redirect was found, indicating a captive portal
- **Redirect chain** — every hop the redirect took (useful to see where you end up)
- **Suspicion analysis:**
  - Certificate issues on the portal = potential MITM
  - Unknown/unfamiliar domain = the portal may not be operated by the venue
  - Suspicious content patterns = potential phishing page

**Why it matters:** Malicious hotspots can use captive portals to collect credentials or inject malware. This tool helps you verify whether a portal is legitimate.

---

#### Packet Analyzer
Captures and inspects raw network packets from your device using Android's VPNService (no root required).

**Important:** While capturing, your internet connection is routed through a local VPN tunnel — traffic is analyzed on-device and not sent anywhere, but internet may be slower during capture.

**How to use:** Tap **Start Capture** (grants VPN permission via system dialog), then use your phone normally. Tap **Stop** when done.

**Reading results:**
- **Live packet stream** — each row = one packet (IP, protocol, ports, size)
- **Protocol badges** — TCP, UDP, ICMP
- **Stats panel** — total packets, bytes transferred, TCP/UDP/ICMP breakdown
- **Top Destinations** — which external IPs your device connected to most
- **DNS Queries** — every domain your device looked up

**Practical use:** Identify apps making unexpected network connections, check if your device is "phoning home" to unknown servers, or investigate unusual traffic patterns.

---

#### Bluetooth Scanner
Scans for nearby Bluetooth Classic and BLE (Bluetooth Low Energy) devices.

**How to use:** Tap **Scan** (scan runs for 15 seconds automatically). Both Classic and BLE devices appear.

**Reading results:**
- **Device name** — what the device calls itself (if it broadcasts a name)
- **MAC address** — hardware identifier
- **Type** — Phone, Headphones, Speaker, Keyboard, Smart Watch, IoT Sensor, etc.
- **Manufacturer** — identified from BLE manufacturer ID (Apple, Samsung, etc.)
- **RSSI** — signal strength (closer = stronger)
- **Distance** — estimated distance based on TX power (BLE only)
- **BONDED badge** — this device is paired with your phone

**Stats bar** shows counts: BLE / Classic / Bonded devices found.

---

### Part 6 — MAC Privacy

**What it does:**
Shows your device's MAC address and lets you manage it. On Android 10+, your phone uses a randomized MAC address for each Wi-Fi network — this protects your privacy by preventing tracking across networks.

**Reading the screen:**
- **Current MAC** — your active MAC address
- **Type badge:** REAL (your hardware MAC is exposed), RANDOMIZED (Android's privacy protection is working), or CUSTOM (manually changed)
- **Vendor** — manufacturer of the hardware MAC

**MAC Change (requires root):**
Enter any MAC address and tap **Apply**, or tap **Randomize** to generate a valid random locally-administered MAC.

**Per-Network Profiles:**
Assign a specific MAC to each Wi-Fi network (identified by SSID). When you connect to that network, EagleEye reminds you to apply the profile MAC. Enable **Auto-Rotate** to automatically change the MAC every N hours.

**Why MAC randomization matters:** Your real hardware MAC is a unique identifier. If you use the same MAC on every public network, your movements can be tracked (the mall's Wi-Fi, the coffee shop's router, etc. all log MAC addresses). With randomization, each network sees a different address.

---

### Part 7 — Settings

**Scan Interval** — How often the background monitor checks for threats. Shorter = more battery usage. Recommended: 15 minutes.

**Port Scan Mode** — Quick scans 10 ports, Full scans 35. Full scan takes longer (up to 30 seconds per device in LAN Scanner).

**Auto-Scan on Connect** — Automatically starts a LAN scan when your phone connects to a Wi-Fi network.

**Trusted Networks** — SSIDs you trust. When the monitor detects you are on a trusted network, some alerts may be suppressed (currently informational).

**Monitor Notifications** — Toggle which threat types generate push notifications:
- New Device on LAN
- ARP Spoofing
- Evil Twin AP
- DNS Change
- Weak Security

**Auto-Start Monitor** — Starts the background monitoring service automatically when the app opens.

**API Keys:**
- **Shodan API Key** — Unlocks full Shodan search beyond the free InternetDB (optional)
- **AbuseIPDB API Key** — Enables IP reputation scoring in Threat Intelligence (optional, free registration at abuseipdb.com)

---

### Part 8 — Common Scenarios and What To Do

**"There are devices on my network I don't recognize"**
1. Open LAN Scanner and tap Scan
2. Look for devices marked NEW or with unfamiliar vendor names
3. Tap the device and check the open ports — a device with many ports open is likely a server or IoT device
4. Use IoT Profile to fingerprint it
5. If truly unknown: change your Wi-Fi password

**"I'm connected to public Wi-Fi — is it safe?"**
1. Check Dashboard → is the security WPA2 or better? If OPEN, assume anyone can read your traffic
2. Run Security scan — check for ARP spoofing (most dangerous on public Wi-Fi)
3. Run Captive Portal analysis — verify the login page is legitimate
4. Use the Packet Analyzer to see what your apps are sending

**"I think someone is intercepting my traffic (MITM)"**
1. Security screen — look for CRITICAL ARP Spoofing alert
2. LAN Scanner — scan the network and look for devices with suspicious ports (4444, 8080, unusual)
3. Traceroute to a known good server — if there are unexpected hops inside your LAN (192.168.x.x hops that shouldn't exist), traffic is being routed through another device
4. Disconnect and notify your ISP or network admin

**"My internet is slow — is it the network or my ISP?"**
1. Ping your gateway (Dashboard → copy gateway IP → Ping tool) — if latency is high (> 50ms), the problem is between you and your router (signal, interference)
2. Ping a public server (e.g. `8.8.8.8`) — if gateway ping is fine but this is slow, the problem is your ISP
3. Traceroute to a distant server — find which hop is introducing latency

**"I want to know if my router has security vulnerabilities"**
1. LAN Scanner → find your router (it's the gateway IP)
2. Port Scan the router
3. IoT Profile → check for default credentials
4. CVE Lookup → search your router's brand and model

---
