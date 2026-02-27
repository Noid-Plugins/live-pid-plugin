# RuneLite Plugin Hub Submission Guide

Last verified: **February 27, 2026**

This is the release checklist for `Live Pid`.

## Official Policy Sources

- RuneLite Plugin Hub README: https://github.com/runelite/plugin-hub
- Rejected / rolled-back features: https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features
- Jagex third-party client guidelines: https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1

## 1. Repository Requirements

Repository must be:

- Public on GitHub
- Java-based plugin source with RuneLite-compatible Gradle build
- Properly attributed and documented
- Licensed (BSD-2-Clause recommended by RuneLite maintainers)

Required metadata file:

- `runelite-plugin.properties` with:
- `displayName`
- `author`
- `description`
- `tags`
- `plugins`

Optional metadata:

- Root `icon.png` (maximum `48x72`)

## 2. Behavioral Compliance Requirements

Before PR submission, verify:

- No gameplay automation
- No credential handling
- No prohibited implementation mechanisms:
- reflection
- JNI/native loading
- subprocess execution
- runtime source/binary download and execution
- No behavior that violates Jagex third-party client policies
- No feature on the rejected/rolled-back feature list

## 3. Build And Validation

Run locally before creating Plugin Hub PR:

```bash
./gradlew clean build
```

Confirm:

- Build passes with no test failures
- `runelite-plugin.properties` values match published plugin details
- README and docs describe current behavior accurately

## 4. Publish Plugin Repository

1. Push this repository to GitHub
2. Ensure default branch is `main`
3. Keep full commit history available

## 5. GitHub Pages Setup

1. Go to repository `Settings` -> `Pages`
2. Set `Source` to `Deploy from a branch`
3. Choose branch `main` and folder `/docs`
4. Save

Expected site URL pattern:

- `https://<owner>.github.io/<repo>/`

## 6. Submit To RuneLite Plugin Hub

1. Fork `runelite/plugin-hub`
2. Create a new branch in your fork
3. Add one manifest file under `plugins/` in plugin-hub with:
- `repository=<your plugin repo .git URL>`
- `commit=<full 40-char commit hash>`
Use [PLUGIN_HUB_MANIFEST_EXAMPLE.txt](./PLUGIN_HUB_MANIFEST_EXAMPLE.txt) as a template.
4. Open PR against `runelite/plugin-hub`
5. Pass all CI checks, then address maintainer feedback

## 7. Updating An Existing Plugin Hub Entry

1. Push new plugin changes to this repository
2. Copy the new 40-character commit hash
3. Update only `commit=` in the Plugin Hub manifest PR
4. Submit PR to plugin-hub

## 8. Live Pid Release Checklist

- Plugin name is `Live Pid`
- Author is `Noid`
- PID indicator supports `Overlay` and `Above head` modes
- Hide-out-of-combat behavior is based on your own attack animations
- No unrelated credential/profile code exists in this plugin repository
