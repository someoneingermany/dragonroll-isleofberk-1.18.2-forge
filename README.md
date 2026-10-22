# Dragon Roll (Forge 1.18.2)

Do-a-Barrel-Roll style flight for modded dragons: free 360° rotation, loops, barrel rolls,
rolled camera, and vanilla elytra glide physics, plus wing thrust so dragons can climb.
It hooks only generic Minecraft/Forge systems (riding, input, camera, rendering), so it doesn't
depend on any specific dragon mod's code and should work with Claws of Berk and most other dragon mods.

## Build the jar (no coding needed)
**Option A: GitHub (works from a phone)**
1. Create a free GitHub account, then make a new repository.
2. Upload everything in this folder, keeping the folder structure, including the hidden `.github` folder.
   (On a phone, use "Add file → Upload files" in a desktop-mode browser.)
3. Open the **Actions** tab. The "Build mod jar" workflow runs automatically (or press "Run workflow").
4. When it shows a green check, open the run and download **dragonroll-jar** from Artifacts.
   Unzip it and put `dragonroll-1.0.0.jar` in your `mods` folder.

**Option B: PC**
Install Java 17 and Gradle 8.8, then run `gradle build` in this folder. The jar is in `build/libs/`.

## Use it in game
1. Mount your dragon and take off normally.
2. Press **V** (rebindable under Controls → Dragon Roll) to switch to barrel-roll flight.

| Input | Action |
|---|---|
| Mouse up/down | Pitch |
| Mouse left/right | Roll (set `mouseXRolls=false` to make it yaw instead) |
| A / D | Yaw |
| W | Flap: forward thrust |
| S | Brake |
| Space | Flap upward (relative to the dragon's back) |
| Shift | Dismount, as normal |
| V again | Return to the dragon mod's own controls |

Landing switches back automatically after a moment (`autoOffOnLanding`).

## Config
`config/dragonroll-common.toml` controls physics and which mounts count as dragons.
`config/dragonroll-client.toml` controls sensitivity, banking, inversion, and model tilt.
By default, any modded mount works. To limit it to dragons, set the whitelist, e.g.
`whitelist = ["clawsofberk:*"]` (look up the real namespace with F3+H, then hover over a spawn egg).

## Isle of Berk / Claws of Berk
Built and checked against what's public about Isle of Berk 1.2.0 (mod id `isleofberk`, GeckoLib 3.0.57):
- GeckoLib 3 doesn't fire Forge's render event, so Dragon Roll hooks GeckoLib's renderer directly
  to tilt the dragon models. If GeckoLib is missing, the hook simply does nothing.
- Riders drawn inside the dragon model by the IoB Variant Loader aren't rotated twice.
- In first person, your camera swings around with the saddle, so upside down you're still on the dragon.
- To limit barrel-roll flight to IoB dragons: `whitelist = ["isleofberk:*"]`. If an addon
  (e.g. Archipelago Additions) adds dragons under its own mod id, add that too.

## Multiplayer
Put it on the server too. Other players will see your dragon roll, and dragons that the dragon
mod moves server-side will fly smoothly. Client-only use works in singleplayer, and on servers
for dragons that the client steers.

## Known limits
- If a dragon mod reads its keys directly (e.g. its own ascend/descend keys), those still fire.
- If the dragon mod already tilts its model when diving, set `rotateModelPitch=false`.
- Model tilt works for GeckoLib dragons and normal renderers. A dragon mod with a fully custom
  renderer may not tilt visually, but the camera and physics still work.
- Wing animations are still the dragon mod's own.
