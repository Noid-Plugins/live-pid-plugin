---
title: Live Pid
---

# Live Pid

`Live Pid` is a RuneLite PvP plugin by **Noid** that shows your PID state in real time.

## Indicator Modes

- `Overlay`: compact square `PID` box
- `Above head`: `PID` label above your local player

PID colors:

- Green: on PID
- Red: off PID
- Yellow: unknown

## Detection Summary

- Uses your own attack animation detections
- Supports melee, ranged, and magic attack timing buckets
- Resolves status from cast-to-hit timing and target distance

## Configuration

- Display Mode
- Text Size
- Hide Out Of Combat

`Hide Out Of Combat` hides the indicator after 25 ticks with no own attack animation detected.

## Compliance

`Live Pid` is prepared for RuneLite Plugin Hub review:

- Java-only plugin implementation
- No automation behavior
- No credential/profile management logic
- No prohibited runtime techniques (reflection/JNI/subprocess/runtime code download)

Review checklist: [RuneLite Submission Guide](../PLUGIN_HUB_SETUP.md)

## Repository

- Source: `https://github.com/Noid-Plugins/live-pid-plugin`
