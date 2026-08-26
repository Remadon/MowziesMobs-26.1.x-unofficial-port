package com.bobmowzie.mowziesmobs.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber
public class DataGenerators {

    // PORTING NOTE (1.21.1 -> 26.1.2): GatherDataEvent lost includeServer()/includeClient() and is now abstract,
    // fired as one of two concrete subtypes (GatherDataEvent.Client / GatherDataEvent.Server) instead - confirmed
    // via javap against the real 26.1.2.95 neoforge jar. Mirrors the confirmed-working pattern from a real,
    // already-building 26.1.2 mod (Silent Gear's net.silentchaos512.gear.data.DataGenerators#gatherData): subscribe
    // to GatherDataEvent.Client (the variant NeoForge's runData task fires for a normal full data run covering both
    // client and server providers) and pass a literal `true` to DataGenerator#addProvider(boolean, T) - that
    // boolean's old "does this run's --server/--client flag include this provider" meaning is now handled entirely
    // by which event subtype fired, not a per-call flag.
    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = event.getGenerator().getPackOutput();
        CompletableFuture<HolderLookup.Provider> provider = event.getLookupProvider();

        // PORTING NOTE (1.21.1 -> 26.1.2): ExistingFileHelper was removed entirely from NeoForge (confirmed - no
        // longer exists in net.neoforged.neoforge.common.data, and GatherDataEvent#getExistingFileHelper() is gone
        // too), so all tag providers below dropped that constructor argument. MMItemTags's blockTags/TagLookup<Block>
        // constructor argument was also dropped since NeoForge's ItemTagsProvider no longer threads it through the
        // constructor (MMItemTags didn't use block-tag-copying anyway, so this is behavior-preserving).
        MMBlockTags blockTags = new MMBlockTags(output, provider);
        generator.addProvider(true, blockTags);
        generator.addProvider(true, new MMItemTags(output, provider));
        generator.addProvider(true, new MMEntityTypeTags(output, provider));
        generator.addProvider(true, new MMBiomeTags(output, provider));
        // MMRecipes is now registered via its RecipeProvider.Runner (see MMRecipes.Runner's javadoc for why).
        generator.addProvider(true, new MMRecipes.Runner(output, provider));
        generator.addProvider(true, new RegistryDataGenerator(output, provider));
    }
}
