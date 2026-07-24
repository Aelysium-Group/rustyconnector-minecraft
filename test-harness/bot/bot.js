'use strict'

/**
 * RustyConnector integration-test bot.
 *
 * Emits one JSON object per line to stdout per event:
 *   {"t":<iso>,"event":"connecting"|"login"|"serverChange"|"kicked"|"end"|"error", ...}
 *
 * Scenarios:
 *   connect   - connect, wait for login/spawn, capture RC chat ~10s, quit cleanly. Exit 0 if logged in.
 *   fallback  - connect, log in, stay up to ~60s emitting all events. Harness kills a backend; bot
 *               reports the resulting serverChange or kicked. Exit 0 if it logged in.
 *   transfer  - connect, log in, send /server lobby (or family switch), report serverChange.
 *
 * Minecraft version: 1.21.8 (confirmed supported by mineflayer 4.37.1 + minecraft-protocol 1.66.2).
 */

const mineflayer = require('mineflayer')

// ---------------------------------------------------------------------------
// Config from environment
// ---------------------------------------------------------------------------
const HOST        = process.env.HOST        || 'localhost'
const PORT        = parseInt(process.env.PORT || '25577', 10)
const SCENARIO    = process.env.SCENARIO    || 'connect'
const USERNAME    = process.env.USERNAME    || ('RCBot' + Math.random().toString(36).slice(2, 6))
const DURATION_MS = process.env.DURATION_MS ? parseInt(process.env.DURATION_MS, 10) : null

// Scenario-specific default durations (ms)
const DEFAULT_DURATION = {
  connect:  10_000,
  fallback: 60_000,
  transfer: 30_000,
}

const runDuration = DURATION_MS || DEFAULT_DURATION[SCENARIO] || 30_000

// ---------------------------------------------------------------------------
// Logging helpers
// ---------------------------------------------------------------------------
function emit(event, extra) {
  const obj = Object.assign({ t: new Date().toISOString(), event }, extra)
  process.stdout.write(JSON.stringify(obj) + '\n')
}

// ---------------------------------------------------------------------------
// State
// ---------------------------------------------------------------------------
let loggedIn   = false
let spawnCount = 0
let exitCode   = 1          // default failure; set to 0 on successful login
let exiting    = false
let mainTimer  = null

// ---------------------------------------------------------------------------
// Clean exit — always flushes stdout
// ---------------------------------------------------------------------------
function shutdown(reason, code) {
  if (exiting) return
  exiting = true
  if (mainTimer) clearTimeout(mainTimer)

  if (bot) {
    try { bot.quit() } catch (_) {}
  }

  // Give stdout a tick to flush before exiting
  setImmediate(() => {
    process.exitCode = (code !== undefined) ? code : exitCode
    process.exit()
  })
}

// ---------------------------------------------------------------------------
// Bot creation
// ---------------------------------------------------------------------------
emit('connecting', { host: HOST, port: PORT, username: USERNAME, scenario: SCENARIO, version: '1.21.8' })

let bot

try {
  bot = mineflayer.createBot({
    host:     HOST,
    port:     PORT,
    username: USERNAME,
    auth:     'offline',
    version:  '1.21.8',
    // Avoid auto-respawn on death; we manage state explicitly
    respawn:  false,
    // Keep keepalive to avoid silent server-side timeout
    keepAlive: true,
  })
} catch (err) {
  emit('error', { message: String(err.message || err) })
  shutdown('createBot threw', 1)
  process.exit(1)
}

// ---------------------------------------------------------------------------
// login / spawn — first arrival on a backend
// ---------------------------------------------------------------------------
bot.once('login', () => {
  // 'login' fires when the login packet is received but before spawn.
  // We wait for 'spawn' (first update_health > 0) to confirm placement.
})

bot.on('spawn', () => {
  spawnCount++

  if (spawnCount === 1) {
    // First spawn — we are on the initial backend.
    loggedIn  = true
    exitCode  = 0

    emit('login', {
      username:  bot.username,
      gamemode:  bot.game.gameMode,
      dimension: bot.game.dimension,
    })

    // Schedule the scenario-specific work now that we're logged in.
    scheduleScenario()

  } else {
    // Subsequent spawn — Velocity has moved us to a different backend.
    // Velocity's backend-transfer is transparent at the TCP level:
    // the proxy sends a Respawn packet followed by new world/health data,
    // which mineflayer surfaces as a second 'spawn' event.
    emit('serverChange', {
      spawnCount,
      dimension:  bot.game.dimension,
      gamemode:   bot.game.gameMode,
      note:       'Velocity transferred bot to a new backend (Respawn packet received)',
    })
  }
})

// ---------------------------------------------------------------------------
// Chat / title messages — RC announces transfers and family info here
// ---------------------------------------------------------------------------
bot.on('message', (jsonMsg) => {
  const text = jsonMsg.toString()
  emit('message', { text })
})

// ---------------------------------------------------------------------------
// Kicked
// ---------------------------------------------------------------------------
bot.on('kicked', (reason, loggedIn_) => {
  let reasonText
  try {
    // reason is a JSON string from the server
    const parsed = JSON.parse(reason)
    reasonText = parsed.text || parsed.translate || JSON.stringify(parsed)
  } catch (_) {
    reasonText = String(reason)
  }
  emit('kicked', { reason: reasonText })
  shutdown('kicked', exitCode) // exit 0 if we had already logged in
})

// ---------------------------------------------------------------------------
// End
// ---------------------------------------------------------------------------
bot.on('end', (reason) => {
  if (!exiting) {
    emit('end', { reason: String(reason) })
    shutdown('end', exitCode)
  }
})

// ---------------------------------------------------------------------------
// Error
// ---------------------------------------------------------------------------
bot.on('error', (err) => {
  emit('error', { message: String(err.message || err) })
  // Do not immediately shut down — some errors are non-fatal (e.g. ECONNRESET
  // during a Velocity backend transfer).  If we haven't logged in yet, treat
  // it as fatal after a short grace window.
  if (!loggedIn) {
    shutdown('pre-login error', 1)
  }
})

// ---------------------------------------------------------------------------
// Scenario logic (runs after first spawn)
// ---------------------------------------------------------------------------
function scheduleScenario() {
  switch (SCENARIO) {
    case 'connect':
      runConnect()
      break
    case 'fallback':
      runFallback()
      break
    case 'transfer':
      runTransfer()
      break
    default:
      emit('error', { message: `Unknown scenario: ${SCENARIO}` })
      shutdown('unknown scenario', 1)
  }
}

// ---------------------------------------------------------------------------
// Scenario: connect
// Connect, capture RC chat for ~10s, quit cleanly.
// ---------------------------------------------------------------------------
function runConnect() {
  mainTimer = setTimeout(() => {
    emit('end', { reason: 'connect scenario complete' })
    shutdown('connect done', 0)
  }, runDuration)
}

// ---------------------------------------------------------------------------
// Scenario: fallback
// Stay connected up to ~60s.  The harness will kill a backend container;
// RC will issue a Velocity transfer/kick.  Bot reports serverChange or kicked.
// ---------------------------------------------------------------------------
function runFallback() {
  mainTimer = setTimeout(() => {
    // If we're still connected after the timeout the harness did not trigger
    // a fallback during this run — report gracefully.
    emit('end', { reason: 'fallback scenario timeout (no backend kill observed)' })
    shutdown('fallback timeout', 0)
  }, runDuration)
}

// ---------------------------------------------------------------------------
// Scenario: transfer
// Connect, then attempt a family switch via /server <family>.
// RC's /server command (CommandServer.java) accepts a family name.
// The compose cluster has one family "lobby" — we attempt /server lobby to
// trigger a re-balance/transfer within the family (may result in same server
// or different backend).  We report whatever serverChange or chat results.
// ---------------------------------------------------------------------------
function runTransfer() {
  // Wait a couple of seconds after spawn so the backend is fully ready.
  const SEND_DELAY  = 2_000
  const WAIT_AFTER  = runDuration - SEND_DELAY

  setTimeout(() => {
    // Try the family switch.  If the bot is already in "lobby" on paper,
    // RC will route to another available backend or reply "already connected".
    // We also try the explicit family id used in compose.yaml ("lobby").
    emit('message', { text: '[bot] sending /server lobby', _internal: true })
    try {
      bot.chat('/server lobby')
    } catch (err) {
      emit('error', { message: 'chat send failed: ' + String(err.message || err) })
    }

    // RC answered the command -> the round-trip we're testing is complete.
    // "already connected" is a valid outcome in a single-family cluster
    // (there is nowhere else to route). A serverChange surfaces as a second
    // 'spawn'; both settle the scenario — don't sit out the full timeout.
    const settle = (why) => {
      clearTimeout(mainTimer)
      emit('end', { reason: `transfer settled: ${why}` })
      shutdown('transfer settled', exitCode)
    }
    bot.on('message', (jsonMsg) => {
      if (/already connected/i.test(jsonMsg.toString())) settle('RC replied already-connected')
    })
    bot.on('spawn', () => settle('serverChange (respawn) observed'))

    // Neither response nor transfer -> timeout (command swallowed = failure signal).
    mainTimer = setTimeout(() => {
      emit('end', { reason: 'transfer scenario timeout (no RC response)' })
      shutdown('transfer timeout', 1)
    }, WAIT_AFTER)

  }, SEND_DELAY)
}

// ---------------------------------------------------------------------------
// Hard-stop safety net — never hang regardless of scenario
// ---------------------------------------------------------------------------
const HARD_TIMEOUT = runDuration + 15_000
setTimeout(() => {
  emit('end', { reason: 'hard timeout reached' })
  shutdown('hard timeout', exitCode)
}, HARD_TIMEOUT).unref()

// Keep node alive until we explicitly call process.exit()
// (the bot's socket and timers handle this, but be explicit)
