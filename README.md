![Aelysium Wordmark Image](https://github.com/Aelysium-Group/rustyconnector-minecraft/blob/development/blob/images/banner.png?raw=true)

# 👋 Welcome to RustyConnector
![Velocity](https://flat.badgen.net/badge/Velocity/3.4.0/1197d1?icon=dockbit)
![PaperMC](https://flat.badgen.net/badge/Paper/1.16%20-%201.21.5/F96854?icon=telegram)
![Folia](https://flat.badgen.net/badge/Folia/Supported/E004BC?icon=flow)
![Fabric](https://flat.badgen.net/badge/Fabric/1.16%20-%201.21.5/bf9b49?icon=telegram)
[![Aelysium](https://flat.badgen.net/badge/Discord/Aelysium/5865F2?icon=discord)](https://join.aelysium.group/)
[![Build](https://flat.badgen.net/github/release/Aelysium-Group/rustyconnector-minecraft?label=Latest%20Stable%20Release&icon=maven)](https://github.com/Aelysium-Group/rustyconnector-minecraft/releases)
[![License](https://flat.badgen.net/badge/License/GPL-V3/5865F2)](https://github.com/Aelysium-Group/rustyconnector-minecraft/blob/main/LICENSE)

[RustyConnector] is a load-balancing plugin that can run on [Velocity]/[Paper]/[Folia] networks, which allows your proxy to automatically register or unregister sub-servers while it's running.

<!-- Table-1 -->
<table>
<thead>
<h2>🧭 Links</h2>
</thead>
<tbody>
<tr>
  <td width="80" align="center" valign="top">
    <br>
    <a href="https://wiki.aelysium.group/rusty-connector/"><img src="https://github.com/Aelysium-Group/rustyconnector-minecraft/blob/development/blob/images/logo.webp?raw=true"></a>
  </td>
  <td valign="top">
    <h3>Wiki</h3>
    <p>
      Wanna learn how to get started? <a href="https://wiki.aelysium.group/rusty-connector/">Look here</a>!
    </p>
  </td>
</tr>
<tr>
  <td width="80" align="center" valign="top">
    <br>
    <a href="https://github.com/Aelysium-Group/rustyconnector-minecraft/releases"><img src="https://github.com/Aelysium-Group/rustyconnector-minecraft/blob/development/blob/images/logo.webp?raw=true"></a>
  </td>
  <td valign="top">
    <h3>Download</h3>
    <p>
      <a href="https://github.com/Aelysium-Group/rustyconnector-minecraft/releases">Download from GitHub</a>, or check out the plugin release pages.
    </p>
  </td>
</tr>
<tr>
  <td width="80" align="center" valign="top">
    <br>
    <a href="https://join.aelysium.group"><img src="https://github.com/Aelysium-Group/rustyconnector-minecraft/blob/development/blob/images/logo.webp?raw=true"></a>
  </td>
  <td>
    <h3>Support</h3>
    <p>
      Join our Discord community <a href="https://join.aelysium.group">here</a> for support!
      <br>
      <sup>(Support is offered to paying server members only)</sup>
    </p>
  </td>
</tr>
<tr>
  <td width="80" align="center" valign="top">
    <br>
    <a href="https://github.com/Aelysium-Group/rustyconnector-minecraft/issues"><img src="https://github.com/Aelysium-Group/rustyconnector-minecraft/blob/development/blob/images/logo.webp?raw=true"></a>
  </td>
  <td>
    <h3>Report Bugs</h3>
    <p>
    Experiencing annoying bugs while using the plugin? Report them <a href="https://github.com/Aelysium-Group/rustyconnector-minecraft/issues"> here</a> to help us squash them.
    </p>
  </td>
</tr>
</tbody>
</table>

<!-- Table-2 -->
<table>
<thead>
<h2>🚀 Plugin Release Page</h2>
</thead>
<tbody>
<tr>
  <td width="80" align="center" valign="center">
    <br>
    <a href="https://modrinth.com/plugin/rustyconnector"><img src="https://github.com/Aelysium-Group/rustyconnector-minecraft/blob/development/blob/images/modrinth.svg?raw=true"></a>
  </td>
  <td valign="top">
    <h3>Modrinth</h3>
    <p>
      <a href="https://modrinth.com/plugin/rustyconnector">Click to go to Modrinth plugin release page.</a>
    </p>
  </td>
</tr>
<tr>
  <td width="80" align="center" valign="top">
    <br>
    <a href="https://hangar.papermc.io/nathan-i-martin/RustyConnector"><img src="https://github.com/Aelysium-Group/rustyconnector-minecraft/blob/development/blob/images/hangar.svg?raw=true"></a>
  </td>
  <td valign="top">
    <h3>Hangar</h3>
    <p>
      <a href="https://hangar.papermc.io/nathan-i-martin/RustyConnector">Click to go to Hangar plugin release page.</a>
    </p>
  </td>
</tr>
</tbody>
</table>

---
# ⭐ Core Features
- ### ✅ Register new servers to the proxy during runtime
- ### ✅ Organize servers into Families
- ### ✅ Integrated Load Balancing
- ### ✅ Set soft and hard player limits for servers
- ### ✅ Supports Kubernetes/Docker
- ### ✅ End-to-end packet encryption (AES-256)`
- ### ✅ Comprehensive API
- ### ✅ Fully Extensive Plugin System

# 🤔 Returning Features
The v0.9.0 rewrite has removed many "extra" features in previous versions.
These features are being incrementally released as external modules you can install if you need them.
- ### [✅ Whitelist](https://github.com/Aelysium-Group/rcm-whitelists)
- ### 🔳 Static Family
- ### 🔳 Anchors/Hub
- ### 🔳 Player Registry (If a haze provider exists, we can persist player data to the database.)
- ### 🔳 Parties
- ### 🔳 Friends
- ### 🔳 TPA
- ### 🔳 Ranked Family
- ### 🔳 Forced Hosts (RC natively supports Velocity's forced hosts via "virtual family servers")
- ### 🔳 Discord Webhook
- ### 🔳 Redis Magic Link (RC v0.9.0 now uses websockets instead of Redis by default)

# 🤔 Upcoming Features
- ### ✅ Absolute Redundancy Architecture (v0.9)
- ### ✅ Stateful packet communication (v0.9)
- ### ✅ Native Websocket Support (v0.9)
- ### 🔳 Kubernetes-Native Dynamic Scaling
- ### 🔳 Comprehensive Git-Ops
- ### 🔳 RabbitMQ Support
- ### 🔳 REST API Module

- ### 🔳 Viewport Integrated Dashboard (Release Version TBD)
---
# 🛠️ Building & Testing

RustyConnector builds three artifacts from one Gradle (Kotlin DSL) build: the
**Velocity** proxy plugin, the **Paper** backend plugin, and the **Fabric**
backend mod.

## Setup

The build's only prerequisite is **JDK 21**. Everything runs through the Gradle
wrapper (`./gradlew`), which pins **Gradle 9.6.0** (required by the Fabric Loom
toolchain); Gradle uses a JDK 21 it finds on the system but won't download one.
Get JDK 21 either way:

- **With Nix** — `nix develop` (or `direnv allow`) opens a shell with JDK 21,
  Gradle 9, Maven, and [`just`](https://just.systems), and sets `JAVA_HOME`.
- **Without Nix** — install JDK 21 yourself. Add [`just`](https://just.systems)
  too for the task recipes below, or call `./gradlew` directly.

## Common tasks (`just`)

| Recipe | What it does |
|---|---|
| `just build` | Build the velocity / paper / fabric jars (`./gradlew build`). |
| `just clean` | Remove build outputs. |
| `just test` | Build, stand up the integration cluster, run the bot scenarios. |
| `just up` | Stand up the integration cluster (boot, wire, verify registration). |
| `just down` | Tear the cluster down and wipe its data. |
| `just logs` | Follow cluster logs. |
| `just bot [scenario]` | Run a single bot scenario (default `fallback`). |

`just --list` shows the full set. Build artifacts land in
`<platform>/build/libs/rustyconnector-<platform>-<version>.jar`.

## Integration test harness (Docker)

`just test` / `just up` stand up a real cluster in Docker — a Velocity proxy with
Paper and Fabric backends — and drive a [mineflayer](https://github.com/PrismarineJS/mineflayer)
bot through it to assert connect / fallback / transfer behaviour against the
freshly-built jars. This path needs **Docker** (with the Compose plugin); it does
**not** need the Nix shell.

See **[`test-harness/README.md`](test-harness/README.md)** for the cluster
topology, the bot scenarios, the wiring details, and troubleshooting.

---
# 🎨 Statistics

## 🌌 Networks Served ([Click to view](https://bstats.org/plugin/velocity/RustyConnector/17972)):
[![RustyConnector bstats graph](https://bstats.org/signatures/velocity/RustyConnector.svg)](https://bstats.org/signatures/velocity/RustyConnector.svg)

2025 © [Aelysium Group LLC](https://www.aelysium.group)

<!-- URL LIST -->
[Folia]:https://github.com/PaperMC/Folia
[Paper]: https://papermc.io
[Velocity]: https://velocitypowered.com
[RustyConnector]: https://github.com/Aelysium-Group/rustyconnector-minecraft
