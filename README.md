# Punch Boost

A Fabric client mod for **Minecraft 1.21.11** that does the Punch-bow boost for you with one key:
it picks your Punch bow from the hotbar, looks where you configured (straight up by default),
draws for the set number of ticks, releases, then puts your view and held item back.

The arrow inherits your momentum, so when it falls back onto you its Punch knockback pushes you
**in the direction you were already moving**. Each hit costs about a heart at the default draw time.

## Requirements
- Minecraft 1.21.11, Fabric Loader 0.18.4+, Fabric API
- Optional: Mod Menu (adds a *Configure* button)
- A bow in your **hotbar** and arrows in your inventory

## Controls (rebind in Options → Controls → Punch Boost)
| Key | Default | What it does |
|---|---|---|
| Boost | `R` | Fire one boost shot (or keep firing if *Repeat while held* is on) |
| Open settings | `K` | Opens the settings screen |
| Toggle | unbound | Turn the mod on/off |

## Settings
Change them in-game (K or Mod Menu) or in `.minecraft/config/punchboost.json`.

| Setting | Default | Notes |
|---|---|---|
| Aim pitch | -90° | -90 = straight up. This is the reliable setting. |
| Yaw offset | 0° | Turn before shooting. Only matters if pitch isn't straight up. |
| Draw time | 6 ticks | 3 = minimum that fires, 20 = full power. Full power sends the arrow ~70 blocks up and takes several seconds to return. 5–8 is the sweet spot. |
| Aim delay | 1 tick | Raise to 2–3 on laggy servers if the arrow goes the wrong way. |
| Hold after release | 2 ticks | |
| Cooldown | 4 ticks | |
| Min Punch level | 1 | 0 = any bow. Set 2 to only use Punch II. |
| Restore view / slot | on / on | |
| Repeat while held | off | |
| Action bar messages | on | |

**Tuning tips:** keep moving (sprint/jump) when you fire — no movement means no horizontal push.
If the arrow lands beside you instead of on you, lower the draw time.

## Server opt-out
Servers can switch the mod off for their players by sending an empty plugin message on
channel **`punchboost:disable`**. It stays off until the player disconnects.

Paper/Spigot example:
```java
// onEnable():
getServer().getMessenger().registerOutgoingPluginChannel(this, "punchboost:disable");

// in a PlayerJoinEvent / PlayerRegisterChannelEvent handler:
player.sendPluginMessage(this, "punchboost:disable", new byte[0]);
```
(Using `PlayerRegisterChannelEvent` is most reliable, since that fires once the client has announced the channel.)

## Building the .jar
**Easiest (no installs):** push this folder to a GitHub repo. The included workflow builds it;
download the jar from the run's *Artifacts* section on the Actions tab.

**Locally:** install JDK 21, then run in this folder:
```
./gradlew build        (Windows: gradlew.bat build)
```
The jar is `build/libs/punchboost-1.0.0+mc1.21.11.jar` (ignore the `-sources` jar).
To test in a dev client: `./gradlew runClient`.

## Publishing on Modrinth
1. Replace `YourName` in `fabric.mod.json` and `LICENSE`, and fix the links in `contact`.
2. Create a project: type **Mod**, loader **Fabric**, environment **Client: required / Server: unsupported**.
3. Upload the main jar, game version **1.21.11**, add **Fabric API** as a required dependency and **Mod Menu** as optional.
4. In the description, say plainly what it does and document the `punchboost:disable` opt-out.
   Don't call it a hack/cheat. Modrinth's content rules count client mods that give a multiplayer advantage
   *without* a server-side opt-out as cheats; the opt-out channel is what keeps this compliant.

## Fair play
Lots of servers ban macros or automated movement regardless of what Modrinth allows.
Check a server's rules before using this there.
