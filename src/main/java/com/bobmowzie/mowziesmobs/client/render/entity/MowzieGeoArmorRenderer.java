package com.bobmowzie.mowziesmobs.client.render.entity;

import com.geckolib.animatable.GeoItem;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoArmorRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.item.Item;

/**
 * PORTING NOTE (GeckoLib 4 -> 5): GeckoLib 4's {@code GeoArmorRenderer} extended vanilla's {@code Model} and relied
 * on this class manually copying the wearer's base {@code HumanoidModel} pose onto matching armor bones each frame
 * (the old {@code applyBaseTransformations}/{@code copyFrom}/{@code renderRecursively}/{@code renderToBuffer}
 * overrides, driven by the {@code usingCustomPlayerAnimations} flag set from
 * {@code client/render/entity/layer/GeckoArmorLayer.java}, which existed specifically to bypass a GeckoLib-4-era
 * mixin limitation in armor rendering).
 * <p>
 * GeckoLib 5's {@code GeoArmorRenderer} is a first-class {@code GeoRenderer} in its own right (no longer a
 * {@code Model}) and does this pose-copying NATIVELY via {@link GeoArmorRenderer#adjustModelBonesForRender} +
 * {@link GeoArmorRenderer.ArmorSegment} (matching the exact same conventional bone names this mod's armor models
 * already use: {@code armorHead}, {@code armorBody}, {@code armorLeftArm}, {@code armorRightArm},
 * {@code armorLeftLeg}, {@code armorRightLeg}, {@code armorLeftBoot}, {@code armorRightBoot}), and is wired up via
 * its own internal mixin ({@code GeoArmorRenderer#tryRenderGeoArmorPiece}, called by GeckoLib's own armor-layer
 * mixin) rather than needing a custom vanilla-armor-layer-bypassing subclass. All of the old manual
 * matrix-copying/visibility machinery in this class has therefore been removed as redundant/obsolete - see the
 * porting report for what still needs verification (namely: whether `GeckoArmorLayer` is still needed at all now
 * that GeckoLib handles this natively).
 * <p>
 * PORTING NOTE: {@code net.minecraft.world.item.ArmorItem} no longer exists as a distinct class in 26.1.2 - armor
 * behavior is now driven entirely by the {@code Equippable}/{@code ArmorMaterial} data components on a plain
 * {@link Item} (confirmed via {@code com.geckolib.renderer.GeoArmorRenderer}'s own real bound,
 * {@code <T extends Item & GeoItem, ...>}, and by every armor {@code ItemXxx} class in this mod's
 * {@code server/item/} package now extending {@code Item} directly). Bound updated accordingly.
 */
public class MowzieGeoArmorRenderer<T extends Item & GeoItem, R extends HumanoidRenderState> extends GeoArmorRenderer<T, R> {
    public MowzieGeoArmorRenderer(GeoModel<T> modelProvider) {
        super(modelProvider);
    }
}
