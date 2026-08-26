package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.client.model.entity.ModelRockSling;
import com.bobmowzie.mowziesmobs.server.entity.effects.geomancy.EntityRockSling;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * PORTING NOTE: old `render(...)` override (push pose / translate for a "rising" animation / super.render / pop
 * pose) doesn't exist any more (see PORTING_NOTES.md architecture section) - moved to overriding
 * {@code adjustRenderPose}, which is specifically the hook for PoseStack translations that should apply to the
 * model. `entity.tickCount + partialTick` is exactly what the base RenderState's `ageInTicks` field already holds,
 * so no custom RenderState subclass is needed here.
 */
public class RenderRockSling extends GeoEntityRenderer<EntityRockSling, EntityRenderState> {

    public RenderRockSling(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ModelRockSling());
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<EntityRenderState> renderPassInfo) {
        super.adjustRenderPose(renderPassInfo);

        float risingAnim = 2 * (float) (Math.pow(0.6 * (renderPassInfo.renderState().ageInTicks + 1), -3));

        renderPassInfo.poseStack().translate(0, 0.25 - risingAnim, 0);
    }
}
