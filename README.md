# MowziesMobs - 26.1.x NeoForge Unofficial Port
# For the Official Release: https://www.curseforge.com/minecraft/mc-mods/mowzies-mobs

The goal of this project is a feature parity port with the 1.21 release for newer Minecraft (and GeckoLib) versions.
This is an AI-assisted unofficial port to 26.1.x, currently this builds and runs, however there are many things that need a close eye and some game play review. 
Many issues have already been fixed, and many more likely exist and are not yet discovered.

##To Do
Mobs:
Nagas, and the Umvuthana have been looked at the most, and many issues have been fixed already. Everyone else looks right and animates right at least in their passive states, but needs more looking at. 

Items:
Many items have missing textures, or the texture doesn't load right / appear in the correct orientation or position when in the player's hand or floating in the world as a dropped item. 
Spawn eggs do not have new art to match the new minecraft way of making the spawn egg resemble the mob, and I'm not an artist so they're using the old spawn egg graphic and colors from the 1.21 release.

Worldgen:
Validating that all of the structures and blocks generate in the world is something to do.

Config testing:
Every aspect of the config file needs to be tested and have functionality verified.

Cleanup:
There are a lot of fix me notes in the source that need to be checked, potentially fixed, and removed.
A very large number of changes were made (~75000 lines added or changed, ~40000 removed). A large change log and process write up is in progress. Roughly 80% of those numbers was AI figuring out how to wire everything to newer MC and GeckoLib.
