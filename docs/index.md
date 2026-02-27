---
title: Live Pid
---

# Live Pid

`Live Pid` is a RuneLite PvP plugin by **Noid** that displays your PID state in real time.

## Features

- `Overlay` mode: compact square `PID` box.
- `Above Head` mode: `PID` label above your local player.
- Out-of-combat hiding after 25 ticks without own attack animation detection.

PID colors:

- Green: on PID
- Red: off PID
- Yellow: unknown

## Detection Model

- Uses your own recognized attack animations.
- Supports melee, ranged, and magic timing buckets.
- Resolves status using attack-to-hitsplat timing and target distance.
- Handles both animation-first and hitsplat-first event order.

## Known Limitations

- PID inference is timing-based and can return `unknown` in edge cases.
- Rapid target swaps and crowded multi-combat scenes can reduce matching reliability.
- Animation buckets are maintained manually and need updates when combat animations change.

## Configuration

- `Display Mode`
- `Text Size`
- `Hide Out Of Combat`

`Hide Out Of Combat` hides the indicator after 25 ticks with no own attack animation detected.

## Compliance

- No automation or injected inputs.
- No credential storage logic.
- Java 11 compatible.

## Repository

- Source: https://github.com/Noid-Plugins/live-pid-plugin
