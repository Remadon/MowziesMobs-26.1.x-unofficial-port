package com.bobmowzie.mowziesmobs.client.render.item;

import com.bobmowzie.mowziesmobs.client.model.armor.ModelGeomancerArmor;
import com.bobmowzie.mowziesmobs.client.render.entity.MowzieGeoArmorRenderer;
import com.bobmowzie.mowziesmobs.server.item.ItemGeomancerArmor;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.List;
import java.util.Objects;

/**
 * PORTING NOTE (26.1.2): the old GeckoLib-4-era `grabRelevantBones`/`setAllVisible`/`applyBoneVisibilityBySlot`
 * override system doesn't exist any more - GeckoLib 5's `GeoArmorRenderer` handles per-slot bone visibility natively
 * via {@link #getSegmentsForSlot} (overridden below to match the old per-slot bone list exactly, including the
 * body/`ArmorSegment.CHEST` bone that used to be shown for the HEAD and LEGS slots too - the default
 * `getSegmentsForSlot` doesn't include it for those slots). The three EXTRA decorative bones this armor has beyond
 * the standard `ArmorSegment`s (prayer beads, belt, robe) have no equivalent in that enum (it's a fixed vanilla/
 * GeckoLib enum, not extensible) - their slot-based visibility is now driven from
 * {@link #adjustModelBonesForRender}, GeckoLib 5's designated per-render-pass bone-adjustment hook, using
 * `BoneSnapshots#ifPresent(String, Consumer)` to toggle each bone's `skipRender` flag directly.
 */
public class RenderGeomancerArmor extends MowzieGeoArmorRenderer<ItemGeomancerArmor, HumanoidRenderState> {
    public RenderGeomancerArmor() {
        super(new ModelGeomancerArmor());
    }

    @Override
    public List<ArmorSegment> getSegmentsForSlot(HumanoidRenderState renderState, EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> List.of(ArmorSegment.HEAD, ArmorSegment.CHEST);
            case CHEST -> List.of(ArmorSegment.CHEST, ArmorSegment.LEFT_ARM, ArmorSegment.RIGHT_ARM);
            case LEGS -> List.of(ArmorSegment.CHEST, ArmorSegment.LEFT_LEG, ArmorSegment.RIGHT_LEG);
            case FEET -> List.of(ArmorSegment.LEFT_FOOT, ArmorSegment.RIGHT_FOOT);
            default -> List.of();
        };
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<HumanoidRenderState> renderPassInfo, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(renderPassInfo, snapshots);

        EquipmentSlot slot = Objects.requireNonNull(renderPassInfo.renderState().getGeckolibData(GeoArmorRenderer.CURRENT_SLOT));

        snapshots.ifPresent("prayer_beads", snapshot -> snapshot.skipRender(slot != EquipmentSlot.HEAD));
        snapshots.ifPresent("robes", snapshot -> snapshot.skipRender(slot != EquipmentSlot.CHEST));
        snapshots.ifPresent("belt", snapshot -> snapshot.skipRender(slot != EquipmentSlot.LEGS));
    }
}
