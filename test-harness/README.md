# RustyConnector integration test harness

A Docker-based harness that stands up a real RustyConnector cluster — a Velocity
proxy with Paper and Fabric backends — and drives a [mineflayer] bot through it to
assert end-to-end behaviour (connect, fallback, transfer) against the
freshly-built plugin/mod jars.

It is driven from the repo root through [`just`](../justfile); the recipes here
call the `harness` script in this directory.

## What it builds

```
                 ┌─────────────────────────────┐
   mineflayer    │  velocity  (itzg/mc-proxy)  │
   bot  ───────► │  RustyConnector-velocity    │
  (bot service)  └──────────────┬──────────────┘
                                │  Magic Link (WebSocket, http://velocity:8080)
                 ┌──────────────┴──────────────┐
                 ▼                             ▼
   ┌───────────────────────┐     ┌────────────────────────┐
   │ paper (itzg/mc-server)│     │ fabric (itzg/mc-server)│
   │ RustyConnector-paper  │     │ RustyConnector-fabric  │
   │ family: lobby         │     │ family: lobby          │
   └───────────────────────┘     └────────────────────────┘
        all on the `rcnet` Docker network · all Minecraft 1.21.8
```

- **Proxy** — `itzg/mc-proxy` running Velocity, with the RustyConnector Velocity
  plugin mounted in. Host `25565` → container `25577` (itzg binds Velocity to
  `25577`).
- **Backends** — two `itzg/minecraft-server` instances, Paper and Fabric, each
  with the matching RustyConnector artifact. Both register into the `lobby`
  family over Magic Link. Both run Minecraft **1.21.8** so a single bot client
  build can reach either.
- **Network** — a single Compose network, `rcnet`. Every container (including the
  bot) attaches to it and reaches the others by service name (`velocity`,
  `paper`, `fabric`).

## Requirements

- **Docker** with the Compose plugin (`docker compose`). Nothing else — the bot
  runs in a container, so no host Node.js is needed.
- The platform jars must be built first: `just build` from the repo root. `up`
  stages them automatically (see below), but the Gradle build itself needs the
  Nix dev shell / a JDK 21 — see the root [README](../README.md#-building--testing).

The harness does **not** need the Nix dev shell; only Docker.

## Usage

All recipes run from the repo root:

| Recipe | What it does |
|---|---|
| `just build` | Build the velocity/paper/fabric jars (prerequisite for `up`). |
| `just up` | Stage jars, boot the cluster, wire it, verify both backends registered. |
| `just test` | `build`, then `up`, then run the bot scenarios. |
| `just bot [scenario]` | Run a single bot scenario (default `fallback`). |
| `just logs` | Follow the cluster logs. |
| `just down` | Stop the cluster and wipe its data. |

The underlying `harness` script exposes a few more subcommands directly
(`test-harness/harness <cmd>`):

| Subcommand | What it does |
|---|---|
| `stage` | Copy freshly-built jars into `./jars` under stable names. |
| `up` | `stage` → `docker compose up -d` → wait healthy → `wire` → verify. |
| `wire` | (Re)apply cluster wiring (idempotent), restart backends, verify. |
| `verify [since]` | Check both backends (paper AND fabric, distinctly) registered into `lobby`. Optional `since` (unix epoch) scopes the log window — `wire` sets it automatically so pre-restart registrations can't satisfy the check. |
| `bot <scenario>` | Run one bot scenario as a one-off Compose container. |
| `fallback` | Connect a bot, kill its backend, assert failover; waits for the backend to re-register before returning. |
| `test` | Run `connect` → `fallback` → `transfer` (assumes a wired cluster). |
| `down` | `docker compose down -v`. |

> **Ordering:** `bot`, `fallback`, and the `test` subcommand assume the cluster
> is already up and wired. `just test` sequences `up` before them; if you call
> `test-harness/harness test` directly, run `up` first. (A bot run will create
> the `rcnet` network on its own, but it can't wire a cluster that isn't there.)

## Bot scenarios

The bot ([`bot/bot.js`](bot/bot.js)) emits one JSON object per line per event
(`connecting`, `login`, `serverChange`, `kicked`, `end`, `error`):

- **`connect`** — connect through the proxy, confirm login/spawn on a backend,
  capture chat for ~10 s, exit 0 if it logged in. The basic "the cluster routes a
  player" check.
- **`fallback`** — connect, then the harness gracefully stops the backend the bot
  landed on and expects RustyConnector to fail the player over to the surviving
  sibling (a second `spawn` → `serverChange`). The backend is restarted afterward
  so the scenario is re-runnable.
- **`transfer`** — connect, then send `/server lobby` and require a response:
  either a `serverChange` (re-balanced to a sibling) or RC's
  "already connected" reply (valid in this single-family cluster — there is
  nowhere else to route). Exits nonzero if the command gets *no* response —
  a swallowed command registration is the failure this scenario exists to catch.

> **Known limitation (RC behaviour, not the harness):** on a backend going down,
> current builds *kick* the connected player (`Server closed`) instead of
> redirecting to a sibling, so `fallback` sees a `kicked` event, not a
> `serverChange`. **Temporary:** until RC's fallback is fixed the harness treats
> that kick as the expected result (so `fallback` / `just test` pass on it); flip
> the scenario back to requiring `serverChange` once RC redirects.

## The bot container

The bot is a profile-gated Compose service (`profiles: [bot]`): a plain `up`
never starts it; the harness runs it on demand with `docker compose run bot`.
Dependencies are baked into the image (`bot/Dockerfile`, `npm ci`).

## Notes / gotchas

- **itzg user-namespace remap.** The backend data dirs are chown'd to host UID
  `100999`, so host-side edits of generated configs are `Permission denied`. Edit
  them through `docker compose exec` (which is what `wire` does), not on the host.
- **`data/` persists between runs.** `up` does not wipe it; use `just down`
  (`docker compose down -v`) for a clean slate if a run leaves stale world/config
  state.
- **What `wire` sets up:** Velocity modern forwarding + `online-mode=false`,
  Magic Link rebound to `0.0.0.0:8080` (reachable cross-container), itzg's
  predefined `[servers]` stripped so RC owns routing, the shared `aes.private`
  copied to both backends, and FabricProxy-Lite configured on Fabric.

[mineflayer]: https://github.com/PrismarineJS/mineflayer
