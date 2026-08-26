package com.bobmowzie.mowziesmobs.datagen;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.server.block.BlockHandler;
import com.bobmowzie.mowziesmobs.server.item.ItemHandler;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class MMRecipes extends RecipeProvider {
    public MMRecipes(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        RecipeOutput output = this.output;

        // Shaped
        shaped(RecipeCategory.COMBAT, ItemHandler.SPEAR.value())
                .pattern(" F ")
                .pattern("ES ")
                .pattern(" S ")
                .define('F', Items.FLINT)
                .define('S', Tags.Items.RODS_WOODEN)
                .define('E', Tags.Items.FEATHERS)
                .unlockedBy(getHasName(Items.FLINT), has(Items.FLINT))
                .save(output);

        shaped(RecipeCategory.COMBAT, ItemHandler.BLOWGUN.value())
                .pattern(" B ")
                .pattern("SBS")
                .pattern(" B ")
                .define('B', Items.BAMBOO)
                .define('S', Tags.Items.STRINGS)
                .unlockedBy(getHasName(Items.BAMBOO), has(Items.BAMBOO))
                .save(output);

        shaped(RecipeCategory.COMBAT, ItemHandler.DART.value(), 8)
                .pattern("F")
                .pattern("S")
                .pattern("E")
                .define('F', ItemHandler.NAGA_FANG.value())
                .define('S', Tags.Items.RODS_WOODEN)
                .define('E', Tags.Items.FEATHERS)
                .unlockedBy(getHasName(ItemHandler.NAGA_FANG.value()), has(ItemHandler.NAGA_FANG.value()))
                .save(output);

        shaped(RecipeCategory.COMBAT, ItemHandler.NAGA_FANG_DAGGER.value())
                .pattern("F")
                .pattern("I")
                .pattern("S")
                .define('F', ItemHandler.NAGA_FANG.value())
                .define('I', Tags.Items.INGOTS_IRON)
                .define('S', Tags.Items.RODS_WOODEN)
                .unlockedBy(getHasName(ItemHandler.NAGA_FANG.value()), has(ItemHandler.NAGA_FANG.value()))
                .save(output);

        shaped(RecipeCategory.BUILDING_BLOCKS, BlockHandler.PAINTED_ACACIA.value())
                .pattern("S")
                .pattern("S")
                .define('S', BlockHandler.PAINTED_ACACIA_SLAB.value())
                .unlockedBy(getHasName(BlockHandler.PAINTED_ACACIA_SLAB.value()), has(BlockHandler.PAINTED_ACACIA_SLAB.value()))
                .save(output, net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, MMCommon.resource("painted_acacia_block_from_slab")));

        shaped(RecipeCategory.BUILDING_BLOCKS, BlockHandler.PAINTED_ACACIA_SLAB.value())
                .pattern("###")
                .define('#', BlockHandler.PAINTED_ACACIA.value())
                .unlockedBy(getHasName(BlockHandler.PAINTED_ACACIA.value()), has(BlockHandler.PAINTED_ACACIA.value()))
                .save(output);

        // Shapeless
        shapeless(RecipeCategory.BUILDING_BLOCKS, Items.JUNGLE_PLANKS, 4)
                .requires(BlockHandler.CLAWED_LOG.value())
                .unlockedBy(getHasName(BlockHandler.CLAWED_LOG.value()), has(BlockHandler.CLAWED_LOG.value()))
                .save(output, net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, MMCommon.resource("jungle_planks_from_clawed_log")));

        shapeless(RecipeCategory.BUILDING_BLOCKS, BlockHandler.PAINTED_ACACIA.value(), 4)
                .requires(Items.ACACIA_PLANKS, 4)
                .requires(Items.YELLOW_DYE)
                .requires(Items.WHITE_DYE)
                .requires(Items.CYAN_DYE)
                .unlockedBy("has_painted_acacia_materials", has(
                        BlockHandler.PAINTED_ACACIA.value(),
                        Items.YELLOW_DYE,
                        Items.WHITE_DYE,
                        Items.CYAN_DYE)
                )
                .save(output);

        shapeless(RecipeCategory.BUILDING_BLOCKS, BlockHandler.THATCH.value())
                .requires(Ingredient.of(Items.SHORT_GRASS, Items.TALL_GRASS), 9)
                .unlockedBy("has_thatch_material", has(Items.SHORT_GRASS, Items.TALL_GRASS))
                .save(output);

        // Smelting
        // PORTING NOTE (1.21.1 -> 26.1.2): SimpleCookingRecipeBuilder.smelting/blasting now take an explicit
        // CookingBookCategory in addition to the crafting RecipeCategory (confirmed against real 26.1.2 source) -
        // using MISC to match these recipes' RecipeCategory.MISC crafting category.
        SimpleCookingRecipeBuilder
                .smelting(Ingredient.of(BlockHandler.CLAWED_LOG.value()), RecipeCategory.MISC, CookingBookCategory.MISC, Items.CHARCOAL, 0.15f, 200)
                .unlockedBy(getHasName(BlockHandler.CLAWED_LOG.value()), has(BlockHandler.CLAWED_LOG.value()))
                .save(output, net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, MMCommon.resource("charcoal")));

        SimpleCookingRecipeBuilder
                .smelting(Ingredient.of(ItemHandler.CAPTURED_GROTTOL.value()), RecipeCategory.MISC, CookingBookCategory.MISC, Items.DIAMOND, 1, 200)
                .unlockedBy(getHasName(ItemHandler.CAPTURED_GROTTOL.value()), has(ItemHandler.CAPTURED_GROTTOL.value()))
                .save(output, net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, MMCommon.resource("grottol_smelt")));

        // Blasting
        SimpleCookingRecipeBuilder
                .blasting(Ingredient.of(ItemHandler.CAPTURED_GROTTOL.value()), RecipeCategory.MISC, CookingBookCategory.MISC, Items.DIAMOND, 1, 200)
                .unlockedBy(getHasName(ItemHandler.CAPTURED_GROTTOL.value()), has(ItemHandler.CAPTURED_GROTTOL.value()))
                .save(output, net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, MMCommon.resource("grottol_blast")));
    }

    // PORTING NOTE (1.21.1 -> 26.1.2): ItemPredicate.Builder#of(...) now needs an explicit HolderGetter<Item>
    // lookup as its first argument (confirmed against real 26.1.2 source) - use the HolderGetter<Item> already
    // held by the RecipeProvider superclass (this.items), so this can no longer be static.
    protected @NotNull Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike... items) {
        return inventoryTrigger(ItemPredicate.Builder.item().of(this.items, items));
    }

    /**
     * PORTING NOTE (1.21.1 -> 26.1.2): RecipeProvider itself is no longer constructed directly with
     * (PackOutput, CompletableFuture&lt;HolderLookup.Provider&gt;) - that pair is now consumed by a
     * RecipeProvider.Runner (a DataProvider) which builds the HolderLookup.Provider/RecipeOutput and then
     * constructs the actual RecipeProvider (now (HolderLookup.Provider, RecipeOutput)) via
     * createRecipeProvider(...). This Runner is what actually gets registered with the DataGenerator - see
     * NeoForge's own net.neoforged.neoforge.common.data.internal.NeoForgeRecipeProvider.Runner (real 26.1.2
     * source) for the reference pattern this mirrors. DataGenerators.java now registers
     * "new MMRecipes.Runner(output, provider)" instead of "new MMRecipes(output, provider)".
     */
    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
            super(output, lookupProvider);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider lookupProvider, RecipeOutput output) {
            return new MMRecipes(lookupProvider, output);
        }

        @Override
        public @NotNull String getName() {
            return "Mowzie's Mobs Recipes";
        }
    }
}
