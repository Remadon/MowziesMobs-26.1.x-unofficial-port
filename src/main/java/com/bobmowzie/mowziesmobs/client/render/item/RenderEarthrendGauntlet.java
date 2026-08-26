package com.bobmowzie.mowziesmobs.client.render.item;

import com.bobmowzie.mowziesmobs.client.model.item.ModelEarthrendGauntlet;
import com.bobmowzie.mowziesmobs.client.model.tools.geckolib.MowzieGeoBone;
import com.bobmowzie.mowziesmobs.server.item.ItemEarthrendGauntlet;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * PORTING NOTE (GeckoLib 4 -> 5): the old {@code renderByItem(ItemStack, ItemDisplayContext, PoseStack,
 * MultiBufferSource, int, int)} override point is GONE - {@code GeoItemRenderer} no longer has it at all (see
 * {@code com.geckolib.renderer.GeoItemRenderer}, real source read directly: no {@code renderByItem} method exists
 * any more, rendering is driven entirely through the {@code createRenderState}/{@code addRenderData}/{@code submit}
 * render-state pipeline like every other GeckoLib 5 renderer - see PORTING_NOTES.md architecture section).
 * {@code addRenderData(T, GeoItemRenderer.RenderData, GeoRenderState, float)} is the correct new hook for per-frame
 * logic that used to live in {@code renderByItem} - {@code relatedObject.renderPerspective()} carries the
 * {@code ItemDisplayContext} the old method received directly as a parameter.
 * <p>
 * Real {@code com.geckolib.cache.model.GeoBone} no longer has a {@code setHidden(boolean)} method (GeckoLib 5's
 * {@code GeoBone} is a shared, structurally-immutable singleton - see MowzieGeoBone.java's porting notes) - bone
 * visibility is set through the {@link MowzieGeoBone} wrapper instead, which persists the "hidden" flag on the same
 * {@code GeoBone#frameSnapshot} slot GeckoLib's own renderer reads (confirmed against the real
 * {@code CuboidGeoBone#render(...)} source: it checks {@code this.frameSnapshot.isHidden()} before drawing cubes).
 * Since neither "root" nor "rootFlipped" is targeted by any animation, nothing else ever recreates/clears that
 * snapshot, so setting it here every frame (both branches, unconditionally, matching the original code) is
 * sufficient - no extra lifecycle wiring needed.
 * <p>
 * Compile fix: {@code @Nullable GeoItemRenderer.RenderData} is invalid Java (a TYPE_USE-only annotation like
 * jspecify's {@code @Nullable} must attach to the rightmost segment of a qualified type name) - changed to
 * {@code GeoItemRenderer.@Nullable RenderData}.
 */
public class RenderEarthrendGauntlet extends GeoItemRenderer<ItemEarthrendGauntlet> {

    public RenderEarthrendGauntlet() {
        super(new ModelEarthrendGauntlet());
    }

    @Override
    public void addRenderData(ItemEarthrendGauntlet animatable, GeoItemRenderer.@Nullable RenderData relatedObject, GeoRenderState renderState, float partialTick) {
        if (relatedObject == null) return;

        BakedGeoModel bakedModel = getGeoModel().getBakedModel(getGeoModel().getModelResource(renderState));
        Optional<GeoBone> root = bakedModel.getBone("root");
        Optional<GeoBone> rootFlipped = bakedModel.getBone("rootFlipped");

        if (root.isPresent() && rootFlipped.isPresent()) {
            ItemDisplayContext transformType = relatedObject.renderPerspective();
            boolean flipped = transformType == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;

            new MowzieGeoBone(root.get()).setHidden(flipped);
            new MowzieGeoBone(rootFlipped.get()).setHidden(!flipped);
        }
    }
}
