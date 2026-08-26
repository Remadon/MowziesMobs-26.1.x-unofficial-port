package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.client.model.entity.ModelElokosa;
import com.bobmowzie.mowziesmobs.client.render.entity.layer.ElokosaHandSymbolGeoLayer;
import com.bobmowzie.mowziesmobs.client.render.entity.layer.ElokosaTransformGeoLayer;
import com.bobmowzie.mowziesmobs.server.entity.elokosa.EntityElokosa;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * PORTING NOTE: old `render(...)`/`renderUpdates(...)` overrides removed - the former added nothing beyond the
 * default `super.render(...)` call, and the latter (copying each "droolPosN" bone's rendered world position onto
 * the matching entry of `entity.droolPositions`) is now done via {@link #registerBonePositionListeners}, the
 * GeckoLib-5-correct replacement for reading a bone's live world position - see MowzieGeoEntityRenderer.java and
 * PORTING_NOTES.md "MowzieGeoBone is now a WRAPPER" section (bone pose data is only valid during the live render
 * traversal, not after `render()` returns any more). `getShadowRadius(EntityElokosa)` used to read
 * `entity.getNightForm()` directly - since `getShadowRadius` is now renderState-only, that flag is captured into a
 * custom {@link ElokosaRenderState} during {@link #addRenderData} instead.
 */
public class RenderElokosa extends MowzieGeoEntityRenderer<EntityElokosa, RenderElokosa.ElokosaRenderState> {
    public RenderElokosa(EntityRendererProvider.Context mgr) {
        super(mgr, new ModelElokosa());
        renderLayers.addLayer(new AutoGlowingGeoLayer<>(this));
        renderLayers.addLayer(new ElokosaTransformGeoLayer(this));
        renderLayers.addLayer(new ElokosaHandSymbolGeoLayer(this));
        this.shadowRadius = 0.9f;
    }

    @Override
    public ElokosaRenderState createRenderState(EntityElokosa animatable, @Nullable Void relatedObject) {
        return new ElokosaRenderState();
    }

    @Override
    public void addRenderData(EntityElokosa animatable, @Nullable Void relatedObject, ElokosaRenderState renderState, float partialTick) {
        super.addRenderData(animatable, relatedObject, renderState, partialTick);

        renderState.nightForm = animatable.getNightForm();
    }

    @Override
    protected void registerBonePositionListeners(RenderPassInfo<ElokosaRenderState> renderPassInfo, EntityElokosa entity) {
        int index = 1;

        for (Vec3[] droolPos : entity.droolPositions) {
            if (droolPos != null && droolPos.length > 0) {
                String boneName = "droolPos" + index;

                renderPassInfo.addBonePositionListener(boneName, (worldPos, modelPos, localPos) -> {
                    if (worldPos != null) {
                        droolPos[0] = worldPos;
                    }
                });
            }

            index++;
        }
    }

    @Override
    public boolean shouldRender(EntityElokosa entity, Frustum p_114492_, double p_114493_, double p_114494_, double p_114495_) {
        boolean result = super.shouldRender(entity, p_114492_, p_114493_, p_114494_, p_114495_);
        if (!result) {
            int index = 0;
            float[] offsets = { 0.1f, 0, -0.13f, 0.04f, 0.082f, -0.023f, -0.068f };
            for (Vec3[] droolPos : entity.droolPositions) {
                if (droolPos != null && droolPos.length > 0) {
                    Vec3 centerPos = new Vec3(entity.getX(), entity.getY() + entity.getBbHeight()/2.0 , entity.getZ());
                    Vec3 headPos = centerPos.add(new Vec3(0.72, 0, 0).yRot((float) Math.toRadians(-entity.yBodyRot - 100)));
                    Vec3 offsetPos = headPos.add(new Vec3(0.13 + offsets[index % offsets.length], 0, 0).yRot(index * Mth.TWO_PI / (float) entity.droolPositions.size()));
                    droolPos[0] = offsetPos;
                }
                index++;
            }
        }
        return result;
    }

    @Override
    protected float getShadowRadius(ElokosaRenderState renderState) {
        float whichForm = 0;
        if (getMowzieGeoModel().isInitialized()) {
            whichForm = -getMowzieGeoModel().getControllerValue("whichFormController");
        }
        if (whichForm <= 0.1f) {
            return renderState.nightForm ? 0.9f : 0.4f;
        }
        return Mth.lerp(whichForm, 0.4f, 0.9f);
    }

    public static class ElokosaRenderState extends LivingEntityRenderState {
        public boolean nightForm;
    }
}
