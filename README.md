# Live Pid

Live Pid is a RuneLite PvP plugin by **Noid** that displays your current PID state in real time.

## Features

- Two display modes: `Overlay` box or `Above Head` text.
- PID color states:
- Green: on PID
- Red: off PID
- Yellow: unknown
- Optional hide when out of combat (25 ticks with no own attack animation detected).

## Detection Logic

- Tracks your own recognized attack animations.
- Buckets animations into `MELEE`, `RANGED_STANDARD`, `RANGED_THROWN`, `RANGED_BALLISTA`, and `MAGIC`.
- Resolves PID from attack-to-hitsplat timing and attacker-target distance.
- Supports both match orders:
- animation first, then hitsplat
- hitsplat first, then animation

## Known Limitations

- PID inference is based on observable client-side timing and may be `UNKNOWN` in edge cases.
- Rapid target swaps, dead targets, and crowded multi-combat scenes can reduce match quality.
- Animation bucket coverage is maintained manually and may require updates after game/client changes.

## Configuration

- `Display Mode`
- `Text Size` (above-head mode)
- `Hide Out Of Combat`

## Compliance Notes

- This plugin does not automate gameplay, input, or menu actions.
- This plugin does not modify outgoing chat or account credentials.
- This plugin is Java 11 compatible and packaged for RuneLite Plugin Hub workflows.

## Development

```bash
./gradlew clean build
./gradlew run
```

Sideloaded jar output:

`build/libs/live-pid-plugin-1.0-SNAPSHOT.jar`

## Links

- Source: https://github.com/Noid-Plugins/live-pid-plugin
- Documentation page: https://noid-plugins.github.io/live-pid-plugin/
