# Live Pid

Live Pid is a RuneLite PvP plugin by **Noid** that shows your current PID state in real time.

## What It Shows

- `Overlay` mode: a compact square `PID` box
- `Above Head` mode: `PID` text above your local player

PID colors:

- Green: on PID
- Red: off PID
- Yellow: unknown

## Detection Model

- Detects your own attack animation
- Buckets animation IDs into melee, ranged, and magic timing profiles
- Matches outgoing hitsplats against recent attack samples
- Computes PID state using cast-to-hit tick timing and target distance

## Plugin Config

- `Display Mode`: Overlay / Above head
- `Text Size`: above-head label font size
- `Hide Out Of Combat`: hides indicator after 25 ticks without your own detected attack animation

## Build And Run

```bash
./gradlew clean build
./gradlew run
```

## Project Links

- Repository documentation page: `https://noid-plugins.github.io/live-pid-plugin/`
