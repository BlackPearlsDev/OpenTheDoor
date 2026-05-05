# OpenTheDoor

**OpenTheDoor** is a lightweight Java + Netty network listener designed for testing and inspecting traffic from game clients or custom applications.

The goal of this tool is simple: open a listening socket, accept incoming clients, print everything they send, and help identify whether the received data looks like HTTP, JSON, XMLSocket, plain text, binary data, or a proprietary protocol.

It is especially useful when analyzing old game clients, private protocol clients, Flash XMLSocket clients, custom TCP clients, or any application where you want to understand what is being sent before building a real server implementation.

---

## Features

- Pure Java project
- No Maven required
- Uses Netty for reliable asynchronous networking
- Configurable host and port
- Accepts multiple simultaneous clients
- TCP raw listener mode
- Prints incoming packets in both text and hexadecimal format
- Attempts to detect common packet types:
  - HTTP
  - JSON
  - XML
  - XMLSocket
  - Plain text / custom ASCII
  - Binary / proprietary packets
- Useful for protocol analysis and server prototyping

---

## Use Case

OpenTheDoor is not a complete game server.

It is a diagnostic and reverse-engineering helper tool that allows you to listen to a client and inspect the raw data being sent.

Typical use cases include:

- Checking whether a game client connects successfully
- Inspecting login packets
- Detecting whether a client uses HTTP, XMLSocket, JSON, binary, or a custom protocol
- Understanding packet structure before implementing a server
- Testing old Flash, Java, or custom TCP clients
- Building the first step of a custom private server prototype

---

## Project Structure

```txt
OpenTheDoor/
├─ libs/
│  └─ netty-all-4.1.68.Final.jar
├─ src/
│  └─ openthedoor/
│     ├─ Main.java
│     ├─ config/
│     │  └─ ServerConfig.java
│     └─ net/
│        ├─ HttpListenHandler.java
│        ├─ HttpListenInitializer.java
│        ├─ HttpListenServer.java
│        ├─ PacketTypeDetector.java
│        ├─ TcpListenHandler.java
│        ├─ TcpListenInitializer.java
│        └─ TcpListenServer.java
└─ config.properties
