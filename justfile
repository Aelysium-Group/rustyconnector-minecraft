# RustyConnector task runner.
# Build recipes need the dev shell (direnv, or `nix develop`); harness recipes need Docker.

set shell := ["bash", "-uc"]

# List available recipes
default:
    @just --list

# Build all platform artifacts (velocity / paper / fabric jars)
build:
    ./gradlew build

# Remove build outputs
clean:
    ./gradlew clean

# Build, stand up the integration cluster, and run the bot scenarios
test: build
    test-harness/harness up
    test-harness/harness test

# Stand up the integration cluster (boot, wire, verify registration)
up:
    test-harness/harness up

# Tear the cluster down and wipe its data
down:
    test-harness/harness down

# Follow cluster logs
logs:
    test-harness/harness logs

# Run a single bot scenario (default: fallback)
bot scenario="fallback":
    test-harness/harness bot {{scenario}}
