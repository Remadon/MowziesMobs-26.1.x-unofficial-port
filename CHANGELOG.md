# Changelog

All notable changes to this fork's NeoForge 26.1.2 / GeckoLib 5 port are documented here.

## Unreleased

### Fixed
- Player armor rendering crashed with a `ConcurrentModificationException` whenever an ability animation played. Armor now renders correctly positioned (helmet/chestplate/leggings/boots) during abilities.
- The player's held item (e.g. the Wrought Axe) would vanish the instant an ability's animation started.
- Ability animations (axe swing/slam, and all four Sun's Blessing abilities: Sunstrike, Solar Beam, Solar Flare, Supernova) would skip straight to their final frame instead of playing through, unless the previous animation had run very recently.
- Sun's Blessing abilities had major animation glitches: the player would disappear entirely during Sunstrike and Solar Flare, arms would flail uncontrollably during Solar Beam, and the player would flip upward and vanish during Supernova. Follow-up pass also fixed residual arm-flailing (Sunstrike/Solar Beam/Solar Flare) and extreme head-tilting (Solar Flare/Supernova).
- The player's legs/boots could visually detach after extended play sessions due to unbounded rotation accumulating in the walk-cycle animation.
- Scorch marks left by Sunstrike would flicker and appear to spin when more than two were on the ground at once.
- Spawn egg items for several mobs rendered as blank/missing icons.
- The Elokosa Paw ability's glow/burst/ring particles never faded out — they stayed at full opacity for their entire lifetime instead of fading over time, making the effect look like a solid opaque blob rather than a translucent glow.
- The Elokosa Paw ability's outward-extending runic ring was invisible from most camera angles (including the top-down view needed to see it clearly), due to backface culling on its non-billboarded flat quad.
- Equipping the Wrought Helm rendered a flat gray torso and legs on the player in addition to the helmet, because its custom armor model was built from the full humanoid mesh instead of the head-only mesh NeoForge's equipment renderer expects for a helmet slot.
- The Wrought Helm (and any other custom-modeled armor) lost its custom geometry and reverted to a plain vanilla armor shape for the duration of an axe swing/slam, because the separate armor renderer used during ability animations never consulted the item's custom armor model.
- In first person, the player's arm and held item vanished completely for the whole duration of an axe swing or slam, because the first-person hand renderer submitted their geometry from inside a bone-position-listener callback that fires during the draw phase, where newly submitted geometry is silently dropped instead of drawn.

### Removed
- The old runtime item-model-swapping system (`MMModels`) used for hand-held item models and mask/visage "frame" overlays, which had no equivalent API in NeoForge 26.1.2. Replaced with plain data-driven item model JSON.
