# OpenTheDoor

**OpenTheDoor** is an open-source Java tool designed to help revive old or offline games by quickly prototyping replacement servers.

Its main goal is simple:

> Let a game client connect to you, capture what it sends, understand its protocol step by step, and progressively rebuild enough server behavior to make the client work again.

OpenTheDoor is especially useful for old Flash, Adobe AIR, Unity, Java, Android, browser, mobile, and custom TCP-based games whose official servers are offline or partially unavailable.

---

## Why OpenTheDoor exists

Many old online games are no longer playable because their official servers disappeared.

In many cases, the client still exists, but it expects to contact a remote server for:

- version checks;
- CDN configuration;
- server lists;
- login;
- character data;
- inventory data;
- world data;
- gameplay packets.

Rebuilding a full server from scratch is difficult when you do not know the protocol.

OpenTheDoor helps with the first and most important step:

> Make the client talk to you and record everything it sends.

---

## Important concept

OpenTheDoor is **not** a global network sniffer.

It does not automatically capture all network traffic from your computer, emulator, or phone.

OpenTheDoor only sees traffic that is sent to the host and port where it is listening.

For example, if the game still connects to:

```text
real-game-server.example.com:80
```

OpenTheDoor will see nothing unless you redirect that domain or modify the client so it connects to your machine instead.

---

## Features

OpenTheDoor supports several modes:

```text
tcp-listen
http-listen
tcp-proxy
apk-scan
auto
```

---

### TCP listener

Listen for raw TCP connections and log every packet sent by the client.

Useful for:

- custom socket protocols;
- Flash XMLSocket;
- binary protocols;
- old Java clients;
- old Unity clients;
- custom Android game protocols.

---

### HTTP mock listener

Start a simple HTTP server and respond with local mock files.

Useful when the client first requests files such as:

```text
/config.json
/server.xml
/gateway.php
/cdn_live_scm_ref.json
```

Example:

```text
mocks/
├─ default.json
├─ config.json
└─ cdn_live_scm_ref.json
```

If the client requests:

```http
GET /config.json
```

OpenTheDoor responds with:

```text
mocks/config.json
```

---

### TCP proxy

Place OpenTheDoor between the game client and a real server.

```text
game client -> OpenTheDoor -> real server
```

The proxy logs both directions:

```text
CLIENT_TO_SERVER
SERVER_TO_CLIENT
```

This mode is useful when the original server still exists and you want to observe the protocol without breaking the client.

---

### APK scanner

Scan an Android APK and extract useful network-related information:

- URLs;
- domains;
- paths;
- ports;
- protocol hints;
- interesting strings such as `socket`, `connect`, `mqtt`, `server`, `cdn`, `gateway`, and `api`.

This is useful before starting the server because it helps identify which domains or endpoints must be redirected.

---

### Automatic protocol detection

OpenTheDoor tries to identify common payload types:

```text
HTTP
JSON
XML
XMLSocket
TLS handshake
WebSocket handshake
MQTT
AMF
gzip
zlib
length-prefixed packets
plain text
binary
```

The detection system is not meant to fully reverse-engineer the protocol automatically. It gives you a first hint to understand what the client is sending.

---

## Project structure

```text
openthedoor_v2/
├─ src/openthedoor/
│  ├─ Main.java
│  ├─ config/
│  │  └─ ServerConfig.java
│  ├─ detect/
│  │  ├─ PacketTypeDetector.java
│  │  ├─ PayloadFormatter.java
│  │  └─ ProtocolGuess.java
│  ├─ log/
│  │  ├─ SessionLogger.java
│  │  └─ TrafficDirection.java
│  ├─ net/
│  │  ├─ tcp/
│  │  ├─ http/
│  │  └─ proxy/
│  ├─ scan/
│  │  ├─ ApkScanner.java
│  │  ├─ ScanResult.java
│  │  └─ UrlExtractor.java
│  └─ util/
│     └─ HexDump.java
├─ config.properties
├─ mocks/
│  └─ default.json
├─ logs/
├─ reports/
├─ build.sh
├─ build.bat
└─ README.md
```

---

## Requirements

OpenTheDoor requires:

- Java 8 or newer;
- Netty `netty-all-4.1.68.Final.jar`.

Place Netty here:

```text
libs/netty-all-4.1.68.Final.jar
```

---

## Build

### Linux / macOS

```bash
chmod +x build.sh
./build.sh
```

### Windows

```bat
build.bat
```

After building, you should get:

```text
OpenTheDoor.jar
```

---

## Run

### Desktop interface

On Windows, double-click:

```bat
OpenTheDoor.bat
```

Or run the executable JAR directly:

```bash
java -jar OpenTheDoor.jar
```

The desktop interface lets you edit `config.properties`, run an APK scan, read the generated report, and prepare listener/proxy commands.

### Command line

### Linux / macOS

```bash
java -cp "OpenTheDoor.jar:libs/netty-all-4.1.68.Final.jar" openthedoor.Main --cli config.properties
```

### Windows

```bat
java -cp "OpenTheDoor.jar;libs\netty-all-4.1.68.Final.jar" openthedoor.Main --cli config.properties
```

---

## Configuration

Example `config.properties`:

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

---

## Modes

### `tcp-listen`

Listen for raw TCP traffic.

```properties
mode=tcp-listen
host=0.0.0.0
port=8080
logDir=logs
savePackets=true
```

Use this when the game client already connects to your machine.

---

### `http-listen`

Start an HTTP mock server.

```properties
mode=http-listen
host=0.0.0.0
port=80
mockDir=mocks
logDir=logs
savePackets=true
```

If the client requests:

```text
/server_config.json
```

OpenTheDoor tries to return:

```text
mocks/server_config.json
```

If no matching file exists, it returns:

```text
mocks/default.json
```

---

### `tcp-proxy`

Forward traffic to a real server while logging everything.

```properties
mode=tcp-proxy
host=0.0.0.0
port=8080

targetHost=real.game.server.com
targetPort=80

logDir=logs
savePackets=true
```

Traffic flow:

```text
client -> OpenTheDoor -> real server
```

---

### `apk-scan`

Scan an APK for endpoints and protocol hints.

```properties
mode=apk-scan
apkPath=game.apk
scanOutput=reports/game-scan.md
```

The output report includes:

- URLs found;
- domains found;
- interesting paths;
- protocol hints;
- suggested redirects;
- suggested OpenTheDoor configuration.

---

### `auto`

Start a TCP listener and automatically guess incoming payload types.

```properties
mode=auto
host=0.0.0.0
port=8080
```

---

## Logs

Each run creates a session folder:

```text
logs/session-YYYY-MM-DD_HH-mm-ss/
```

Example:

```text
logs/session-2026-05-08_16-10-44/
├─ session.log
├─ summary.md
├─ 0001-client-to-server.bin
├─ 0001-client-to-server.txt
├─ 0002-server-to-client.bin
└─ 0002-server-to-client.txt
```

The `.txt` files contain readable output:

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

The `.bin` files contain the raw packet data.

Always keep the `.bin` files when reverse-engineering binary, compressed, encrypted, or custom protocols.

---

## Redirecting a game client

OpenTheDoor only works if the client sends traffic to it.

Common redirection methods:

- temporarily patch the client;
- edit a hosts file;
- use a local DNS server;
- configure an Android proxy;
- use an emulator;
- redirect traffic from a router;
- use a VPN-based capture tool;
- hook the client.

### Android emulator note

Inside the default Android emulator:

```text
127.0.0.1
```

means the emulator itself.

To reach your host computer, use:

```text
10.0.2.2
```

So if OpenTheDoor is running on your PC on port `8080`, the client inside the emulator should connect to:

```text
10.0.2.2:8080
```

---

## Typical workflow

```text
1. Scan the APK.
2. Extract domains and URLs.
3. Redirect the game domain to your machine.
4. Start http-listen to mock the first config files.
5. Start tcp-listen to capture socket packets.
6. Use tcp-proxy if the real server still exists.
7. Read the logs.
8. Reproduce server responses step by step.
```

---

## Flash / Adobe AIR games

Old Flash and Adobe AIR games often use:

- HTTP config files;
- XMLSocket;
- null-terminated XML packets;
- `crossdomain.xml`;
- AMF;
- custom binary packets.

A very common first packet is:

```xml
<policy-file-request/>
```

The server may need to answer with something like:

```xml
<cross-domain-policy>
  <allow-access-from domain="*" to-ports="*" />
</cross-domain-policy>
```

Some clients require this response before sending login or game packets.

---

## Limitations

OpenTheDoor does not automatically bypass:

- TLS encryption;
- certificate pinning;
- custom encryption;
- compression with unknown framing;
- authentication logic;
- anti-cheat logic;
- obfuscation;
- server-side game logic.

It helps you observe, log, and prototype.

The actual protocol still needs to be understood progressively.

---

## Legal and ethical use

OpenTheDoor is intended for:

- preservation of abandoned games;
- research;
- interoperability;
- personal learning;
- debugging your own clients;
- open-source server prototyping.

Do not use it to attack active services, steal credentials, bypass paid services, or disrupt live games.

---

## License
- MIT

---

## Credits

Created by BlackPearlsDev.
