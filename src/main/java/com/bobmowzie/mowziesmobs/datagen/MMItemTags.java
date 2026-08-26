package com.bobmowzie.mowziesmobs.datagen;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.server.block.BlockHandler;
import com.bobmowzie.mowziesmobs.server.item.ItemHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ItemTagsProvider;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class MMItemTags extends ItemTagsProvider {
    public static final TagKey<Item> CAN_HIT_GROTTOL = key("can_hit_grottol");
    public static final TagKey<Item> HAND_WEAPONS = key("hand_weapons");

    public MMItemTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, MMCommon.MODID);
    }

    @Override
    protected void addTags(@NotNull HolderLookup.Provider provider) {
        addToVanillaTags();
        addToCommonTags();

        // PORTING NOTE (1.21.1 -> 26.1.2): TagAppender#addOptional(E) now takes an already-resolved element (Item
        // here), not an Identifier - it no longer means "this reference to a possibly-absent item/mod is optional".
        // That old by-id-optional-reference behavior is now expressed via TagEntry.optionalElement(Identifier) fed
        // into TagAppender#add(TagEntry) (confirmed against real 26.1.2 net.minecraft.tags.TagEntry /
        // net.minecraft.data.tags.TagAppender source).
        tag(CAN_HIT_GROTTOL)
                .add(net.minecraft.tags.TagEntry.optionalElement(Identifier.fromNamespaceAndPath("cagedmobs", "dnasamplerdiamond")))
                .add(net.minecraft.tags.TagEntry.optionalElement(Identifier.fromNamespaceAndPath("cagedmobs", "dnasamplernetherite")));

        tag(HAND_WEAPONS).add(ItemHandler.EARTHREND_GAUNTLET.value());
    }

    private void addToVanillaTags() {
        tag(ItemTags.DURABILITY_ENCHANTABLE)
                .add(ItemHandler.SPEAR.value())
                .add(ItemHandler.NAGA_FANG_DAGGER.value());

        // PORTING NOTE (1.21.1 -> 26.1.2): ItemTags.SWORD_ENCHANTABLE ("enchantable/sword") no longer exists as a
        // separate constant - confirmed against real 26.1.2 net.minecraft.tags.ItemTags source, its items were
        // folded into the broader SHARP_WEAPON_ENCHANTABLE ("enchantable/sharp_weapon") tag, which is already
        // applied to these same items just above. The old duplicate SWORD_ENCHANTABLE call is dropped as redundant.
        tag(ItemTags.SHARP_WEAPON_ENCHANTABLE)
                .add(ItemHandler.SPEAR.value())
                .add(ItemHandler.NAGA_FANG_DAGGER.value());

        tag(ItemTags.FIRE_ASPECT_ENCHANTABLE)
                .add(ItemHandler.SPEAR.value())
                .add(ItemHandler.NAGA_FANG_DAGGER.value())
                .add(ItemHandler.WROUGHT_AXE.value());

        tag(ItemTags.AXES)
                .add(ItemHandler.WROUGHT_AXE.value());

        tag(ItemTags.BOW_ENCHANTABLE)
                .add(ItemHandler.BLOWGUN.value());

        tag(ItemTags.BOW_ENCHANTABLE)
                .add(ItemHandler.BLOWGUN.value());

        tag(ItemTags.HEAD_ARMOR)
                .add(ItemHandler.GEOMANCER_BEADS.value())
                .add(ItemHandler.WROUGHT_HELMET.value())
                .add(ItemHandler.SOL_VISAGE.value())
                .add(ItemHandler.UMVUTHANA_MASK_FURY.value())
                .add(ItemHandler.UMVUTHANA_MASK_FEAR.value())
                .add(ItemHandler.UMVUTHANA_MASK_RAGE.value())
                .add(ItemHandler.UMVUTHANA_MASK_BLISS.value())
                .add(ItemHandler.UMVUTHANA_MASK_MISERY.value())
                .add(ItemHandler.UMVUTHANA_MASK_FAITH.value());

        tag(ItemTags.CHEST_ARMOR).add(ItemHandler.GEOMANCER_ROBE.value());
        tag(ItemTags.LEG_ARMOR).add(ItemHandler.GEOMANCER_BELT.value());
        tag(ItemTags.FOOT_ARMOR).add(ItemHandler.GEOMANCER_SANDALS.value());
        tag(ItemTags.PIGLIN_LOVED).add(ItemHandler.SOL_VISAGE.value());

        tag(ItemTags.PLANKS).add(BlockHandler.PAINTED_ACACIA.value().asItem());
        tag(ItemTags.WOODEN_SLABS).add(BlockHandler.PAINTED_ACACIA_SLAB.value().asItem());
    }

    private void addToCommonTags() {
        // PORTING NOTE (1.21.1 -> 26.1.2): Tags.Items.TOOLS_SPEAR no longer exists - confirmed via javap against
        // the real 26.1.2.95 neoforge jar (net.neoforged.neoforge.common.Tags$Items has no SPEAR-related field or
        // "spear" tag path at all anymore; the c:tools/spear convention tag was dropped/never re-added). Using
        // TOOLS_TRIDENT as the closest surviving common tag for a thrown melee polearm weapon - a judgment call,
        // flagged here in case the common-tags convention re-adds a dedicated spear tag later.
        tag(Tags.Items.TOOLS_TRIDENT).add(ItemHandler.SPEAR.value());
        tag(Tags.Items.MUSIC_DISCS).add(ItemHandler.PETIOLE_MUSIC_DISC.value());
        tag(Tags.Items.SEEDS).add(ItemHandler.FOLIAATH_SEED.value());
        tag(Tags.Items.SLIME_BALLS).add(ItemHandler.GLOWING_JELLY.value());
    }

    private static TagKey<Item> key(String path) {
        return ItemTags.create(Identifier.fromNamespaceAndPath(MMCommon.MODID, path));
    }
}
