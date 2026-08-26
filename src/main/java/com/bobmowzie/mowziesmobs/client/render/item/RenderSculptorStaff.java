package com.bobmowzie.mowziesmobs.client.render.item;

import com.bobmowzie.mowziesmobs.client.model.item.ModelSculptorStaff;
import com.bobmowzie.mowziesmobs.client.model.tools.geckolib.MowzieGeoBone;
import com.bobmowzie.mowziesmobs.server.item.ItemSculptorStaff;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * PORTING NOTE (GeckoLib 4 -> 5): {@code renderByItem} no longer exists (see RenderEarthrendGauntlet.java's javadoc
 * for the general rationale) - the "read the disappearController bone's X position after rendering, to drive next
 * frame's render color/type" logic is moved into {@code addRenderData(T, RenderData, GeoRenderState, float)}, the
 * once-per-frame hook with live-entity(-ish; item renderer, no live entity but still runs once/frame) access. This
 * preserves the ORIGINAL timing relationship exactly: the old code also only updated {@code disappearController}
 * from inside {@code renderByItem} (i.e. AFTER {@code getRenderColor}/{@code getRenderType} had already been queried
 * for that same frame), so {@code getRenderColor}/{@code getRenderType} already only ever saw the *previous* frame's
 * value even before this port - not a new staleness this port introduces.
 * <p>
 * {@code com.geckolib.util.Color} (the old return type of {@code getRenderColor}) no longer exists - GeckoLib 5's
 * {@code GeoRenderer#getRenderColor(T, O, float)} returns a packed ARGB {@code int} instead (same convention
 * {@code RenderSculptor.java} already uses for the entity renderer - see its {@code getRenderColor} override).
 * Real {@code GeoBone} has no {@code getPosX()} any more (GeckoLib-5 bones are structural-only; per-frame position
 * lives on the ephemeral {@code frameSnapshot}) - reads go through the {@link MowzieGeoBone} wrapper instead, same
 * as {@code RenderSculptor.java}'s identical {@code disappearController}/{@code getMowzieBone(...).getPosX()} reads.
 * <p>
 * Compile fix: {@code @Nullable GeoItemRenderer.RenderData} is invalid Java (a TYPE_USE-only annotation like
 * jspecify's {@code @Nullable} must attach to the rightmost segment of a qualified type name) - changed to
 * {@code GeoItemRenderer.@Nullable RenderData} throughout this file.
 */
public class RenderSculptorStaff extends GeoItemRenderer<ItemSculptorStaff> {
    public float disappearController = 0;

    public RenderSculptorStaff() {
        super(new ModelSculptorStaff());
    }

    @Override
    public int getRenderColor(ItemSculptorStaff animatable, GeoItemRenderer.@Nullable RenderData relatedObject, float partialTick) {
        return ARGB.colorFromFloat(1.0f - disappearController, 1, 1, 1);
    }

    @Override
    public @Nullable RenderType getRenderType(GeoRenderState renderState, Identifier texture) {
        if (disappearController > 0) {
            return RenderTypes.entityTranslucent(texture);
        }
        else {
            return super.getRenderType(renderState, texture);
        }
    }

    @Override
    public void addRenderData(ItemSculptorStaff animatable, GeoItemRenderer.@Nullable RenderData relatedObject, GeoRenderState renderState, float partialTick) {
        BakedGeoModel bakedModel = getGeoModel().getBakedModel(getGeoModel().getModelResource(renderState));
        Optional<GeoBone> disappearControllerBone = bakedModel.getBone("disappearController");
        disappearControllerBone.ifPresent(geoBone -> disappearController = new MowzieGeoBone(geoBone).getPosX());
    }
}
