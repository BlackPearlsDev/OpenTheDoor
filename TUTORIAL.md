# OpenTheDoor Tutorial

This tutorial explains how to use **OpenTheDoor** to analyze a game client and start rebuilding a replacement server.

The recommended workflow is:

```text
1. Scan the client.
2. Find network endpoints.
3. Redirect the client to your machine.
4. Capture the first packets.
5. Mock the first server responses.
6. Repeat until the client progresses further.
```

---

## 1. Prepare the project

Extract the project:

```bash
unzip OpenTheDoor_v2_code.zip
cd openthedoor_v2
```

Expected structure:

```text
openthedoor_v2/
├─ src/
├─ libs/
├─ mocks/
├─ logs/
├─ reports/
├─ config.example.properties
├─ build.sh
├─ build.bat
└─ README.md
```

Create required folders if needed:

```bash
mkdir -p libs mocks logs reports
```

Place Netty here:

```text
libs/netty-all-4.1.68.Final.jar
```

---

## 2. Build OpenTheDoor

### Linux / macOS

```bash
chmod +x build.sh
./build.sh
```

### Windows

```bat
build.bat
```

After building, you should have:

```text
OpenTheDoor.jar
```

---

## 3. Run OpenTheDoor

### Linux / macOS

```bash
java -cp "OpenTheDoor.jar:libs/netty-all-4.1.68.Final.jar" openthedoor.Main --cli config.properties
```

### Windows

```bat
java -cp "OpenTheDoor.jar;libs\netty-all-4.1.68.Final.jar" openthedoor.Main --cli config.properties
```

---

## 4. Understand the configuration file

Example `config.example.properties`:

```properties
mode=auto
host=0.0.0.0
port=8080

targetHost=
targetPort=80

mockDir=mocks
defaultHttpStatus=200
defaultHttpContentType=application/json; charset=UTF-8

logDir=logs
savePackets=true
maxPrintableBytes=4096

apkPath=game.apk
scanOutput=reports/scan-report.md
```

### Important fields

```properties
mode=auto
```

Selects the tool mode.

Available modes:

```text
auto
tcp-listen
http-listen
tcp-proxy
apk-scan
```

---

```properties
host=0.0.0.0
```

Listen on all local network interfaces.

This does **not** mean “intercept everything”.

It only means OpenTheDoor accepts connections that arrive on this machine.

---

```properties
port=8080
```

The local port OpenTheDoor listens on.

---

```properties
mockDir=mocks
```

Folder used by `http-listen` to serve mock responses.

---

```properties
logDir=logs
```

Folder where captured packets are saved.

---

## 5. Mode: `apk-scan`

Start with this mode when you have an Android APK.

It helps you find domains, URLs, and protocol hints.

### Config

```properties
mode=apk-scan
apkPath=game.apk
scanOutput=reports/game-scan.md
```

### Run

```bash
java -cp "OpenTheDoor.jar:libs/netty-all-4.1.68.Final.jar" openthedoor.Main --cli config.properties
```

### Output

Open:

```text
reports/game-scan.md
```

Look for:

```text
URLs found
Domains found
Interesting paths
Protocol hints
Suggested redirects
```

### What to search for

Useful keywords:

```text
config
server
gateway
cdn
api
login
socket
mqtt
xml
json
php
```

The first useful endpoint is often a config file such as:

```text
/config.json
/server.xml
/cdn_live_scm_ref.json
```

---

## 6. Mode: `http-listen`

Use this when the game requests HTTP files.

### Config

```properties
mode=http-listen
host=0.0.0.0
port=80

mockDir=mocks
defaultHttpStatus=200
defaultHttpContentType=application/json; charset=UTF-8

logDir=logs
savePackets=true
```

### Example

If the game requests:

```http
GET /cdn_live_scm_ref.json
```

create:

```text
mocks/cdn_live_scm_ref.json
```

Example content:

```json
{
  "status": "ok",
  "server": "192.168.1.25",
  "port": 8080
}
```

If the requested file does not exist, OpenTheDoor returns:

```text
mocks/default.json
```

Create a default response:

```json
{
  "status": "ok",
  "message": "OpenTheDoor mock response"
}
```

---

## 7. Mode: `tcp-listen`

Use this mode when the game opens a raw socket connection.

### Config

```properties
mode=tcp-listen
host=0.0.0.0
port=8080

logDir=logs
savePackets=true
maxPrintableBytes=4096
```

### What it does

OpenTheDoor waits for a client connection and logs every packet it receives.

It tries to detect whether the payload is:

```text
HTTP
JSON
XML
XMLSocket
TLS
WebSocket
MQTT
AMF
gzip
zlib
text
binary
```

### Example output

```text
Client connected
Packet received
Direction: CLIENT_TO_SERVER
Detected type: XML_SOCKET
```

Logs are stored in:

```text
logs/session-YYYY-MM-DD_HH-mm-ss/
```

---

## 8. Mode: `tcp-proxy`

Use this when the real server still exists and you want to observe the traffic.

Traffic flow:

```text
game client -> OpenTheDoor -> real server
```

### Config

```properties
mode=tcp-proxy
host=0.0.0.0
port=8080

targetHost=real.game.server.com
targetPort=80

logDir=logs
savePackets=true
maxPrintableBytes=4096
```

### Why this is useful

The client continues talking to the real server, but OpenTheDoor logs both directions:

```text
CLIENT_TO_SERVER
SERVER_TO_CLIENT
```

This lets you study how the real server responds.

---

## 9. Mode: `auto`

Use this for quick tests.

### Config

```properties
mode=auto
host=0.0.0.0
port=8080
```

The `auto` mode starts a TCP listener and guesses the packet type.

---

## 10. Redirect the game client

This is the most important step.

OpenTheDoor will not see anything unless the game connects to it.

If the game connects to:

```text
game.example.com:80
```

you must redirect that traffic to your machine.

---

### Option A: temporarily patch the client

Replace the original server URL with your local IP.

For an Android emulator:

```text
10.0.2.2:8080
```

For a real phone on the same Wi-Fi:

```text
192.168.1.25:8080
```

Replace `192.168.1.25` with your computer IP.

---

### Option B: use a hosts file

On a computer, you can map a domain to your machine:

```text
127.0.0.1 game.example.com
```

For Android, modifying `/etc/hosts` usually requires root.

---

### Option C: use local DNS

Configure a local DNS server so the game domain resolves to your computer.

Example:

```text
game.example.com -> 192.168.1.25
```

Then configure your phone or emulator to use that DNS.

---

### Option D: Android emulator

In the default Android emulator:

```text
127.0.0.1
```

means the emulator itself.

To reach your computer, use:

```text
10.0.2.2
```

So if OpenTheDoor runs on your PC on port `8080`, the client should connect to:

```text
10.0.2.2:8080
```

---

### Option E: Android proxy

You can configure a proxy in Android Wi-Fi settings.

This works best for HTTP traffic.

It is less useful for custom raw TCP sockets unless the client supports proxies or you add a compatible proxy mode.

---

## 11. Read the logs

Each session creates a folder:

```text
logs/session-2026-05-08_16-10-44/
```

Example content:

```text
session.log
summary.md
0001-client-to-server.bin
0001-client-to-server.txt
0002-server-to-client.bin
0002-server-to-client.txt
```

### `.txt` files

Readable packet output:

```text
Timestamp: ...
Direction: CLIENT_TO_SERVER
Size: 128 bytes
Detected type: JSON

ASCII:
{"cmd":"login","user":"test"}

HEX:
7B 22 63 6D 64 22 ...
```

### `.bin` files

Raw packet bytes.

Keep them. They are important for binary, compressed, encrypted, AMF, MQTT, or custom protocols.

---

## 12. Typical workflow for an unknown Android game

### Step 1: scan the APK

```properties
mode=apk-scan
apkPath=game.apk
scanOutput=reports/game.md
```

Run OpenTheDoor and read the report.

---

### Step 2: identify the first endpoint

Look for:

```text
config.json
server.xml
gateway.php
cdn_live_scm_ref.json
```

This is often the first file the game downloads.

---

### Step 3: redirect the domain

If the report finds:

```text
old-game-cdn.example.com
```

redirect it to your computer.

Example DNS result:

```text
old-game-cdn.example.com -> 192.168.1.25
```

---

### Step 4: mock the config

Run:

```properties
mode=http-listen
host=0.0.0.0
port=80
mockDir=mocks
```

Create the file requested by the game:

```text
mocks/config.json
```

Example:

```json
{
  "server": "192.168.1.25",
  "port": 8080
}
```

---

### Step 5: capture the socket

Run:

```properties
mode=tcp-listen
host=0.0.0.0
port=8080
logDir=logs
savePackets=true
```

Start the game and inspect the first packet.

---

### Step 6: reproduce the first response

If the client sends:

```xml
<policy-file-request/>
```

it may expect:

```xml
<cross-domain-policy>
  <allow-access-from domain="*" to-ports="*" />
</cross-domain-policy>
```

Old Flash and Adobe AIR games often need this before login.

---

### Step 7: repeat

The process is iterative:

```text
1. capture a packet;
2. understand what it asks;
3. send a minimal valid response;
4. restart the client;
5. observe the next packet;
6. repeat.
```

Do not try to rebuild the full server immediately.

Unlock the client screen by screen:

```text
splash screen
version check
server list
login
character list
world enter
inventory
combat
```

---

## 13. Example: Flash / Adobe AIR game

Many Flash and Adobe AIR games use this pattern:

```text
1. HTTP request to a config file.
2. Config file contains socket host and port.
3. Client opens XMLSocket or raw TCP.
4. Client sends policy-file-request.
5. Client sends login packet.
6. Server returns account/session data.
```

Recommended setup:

### HTTP phase

```properties
mode=http-listen
host=0.0.0.0
port=80
mockDir=mocks
```

Mock file:

```text
mocks/cdn_live_scm_ref.json
```

Example:

```json
{
  "socketHost": "192.168.1.25",
  "socketPort": 8080
}
```

### Socket phase

```properties
mode=tcp-listen
host=0.0.0.0
port=8080
logDir=logs
savePackets=true
```

---

## 14. Common problems

### No packets arrive

Check:

```text
Is the game really connecting to your IP?
Is the port correct?
Is the domain redirected?
Is Java allowed through the firewall?
Is the phone on the same Wi-Fi network?
Are you using 10.0.2.2 for Android emulator?
Is the client using HTTPS or another port?
```

Most of the time, if no packet arrives, the issue is redirection, not packet parsing.

---

### Port 80 does not start

On Linux/macOS, ports below 1024 may require admin privileges.

Use:

```bash
sudo java -cp "OpenTheDoor.jar:libs/netty-all-4.1.68.Final.jar" openthedoor.Main --cli config.properties
```

Or use another port:

```properties
port=8080
```

---

### Client uses HTTPS

A simple HTTP listener cannot read HTTPS.

Possible solutions:

```text
force the client to use HTTP if possible;
use a TLS proxy;
install a trusted certificate on the device;
disable certificate pinning in a controlled test environment;
hook the client;
patch the endpoint.
```

---

### Detected type is `TLS_HANDSHAKE`

The client is starting an encrypted TLS connection.

The payload cannot be read as plain text by a normal TCP listener.

---

### Detected type is `BINARY`

The protocol may be:

```text
custom binary
compressed
encrypted
AMF
MQTT
length-prefixed
```

Inspect the `.bin` files and compare several packets.

Look for:

```text
packet length
first byte
repeated headers
readable strings
null terminators
endianness
compression signatures
```

---

### Client blocks after the first response

This is normal.

Your mock response may be incomplete or invalid.

Read the next packet, compare with the expected flow, and improve your response progressively.

---

## 15. Practical tips

Start with the smallest possible response.

Keep raw `.bin` packets.

Log both directions when using proxy mode.

Name mock files exactly like the requested URI.

Use one session per experiment.

Keep notes of which packet unlocks which screen.

Do not assume the first packet is login. It is often a version check, CDN config, policy request, or server list.

---

## 16. Minimal checklist

Before testing:

```text
OpenTheDoor compiled
Netty jar present
Correct mode selected
Correct host and port
Firewall allows Java
Client redirected to your machine
Mocks created if using HTTP
Logs enabled
```

If using Android emulator:

```text
Use 10.0.2.2 to reach your PC
```

If using a real phone:

```text
Use your PC local IP, for example 192.168.1.25
```

---

## 17. Recommended first experiment

Use this sequence:

### Scan

```properties
mode=apk-scan
apkPath=game.apk
scanOutput=reports/game.md
```

### HTTP mock

```properties
mode=http-listen
host=0.0.0.0
port=80
mockDir=mocks
```

### TCP capture

```properties
mode=tcp-listen
host=0.0.0.0
port=8080
logDir=logs
savePackets=true
```

This covers most old mobile or Flash/AIR games.

---

## 18. Summary

OpenTheDoor helps you make an old client talk again.

The core rule is:

```text
OpenTheDoor only sees traffic that reaches OpenTheDoor.
```

Once the client is redirected correctly, the process becomes:

```text
capture -> understand -> mock -> repeat
```

That is how you progressively rebuild a replacement server.
