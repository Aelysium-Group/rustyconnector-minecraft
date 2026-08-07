# Reproducible mineflayer bot image for the RustyConnector integration harness.
#
# Dependencies are installed from the committed package-lock.json via `npm ci` at
# build time. A bot run therefore never resolves packages over the network and
# never writes node_modules back into a bind-mounted host directory.
FROM node:22-slim

WORKDIR /bot

# Install deps first so this layer is cached unless package*.json change.
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

# Bot source last — editing bot.js only rebuilds this cheap layer.
COPY bot.js ./

# Drop root: the bot only reads /bot and opens a client socket to the proxy.
USER node

ENTRYPOINT ["node", "bot.js"]
