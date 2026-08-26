package com.bobmowzie.mowziesmobs.client.render.entity.layer;

import com.bobmowzie.mowziesmobs.server.entity.sculptor.EntitySculptor;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * PORTING NOTE (GeckoLib 4 -> 5 full port): {@code renderedItem} used to be reassigned every frame from inside
 * {@code renderForBone} (called by the old framework's per-bone traversal, with live entity access). The new
 * {@link GeckoItemlayer} resolves the current item stack once per frame in {@code addRenderData} via
 * {@code getStackForBone} (live entity access), so this override moves the "which item is currently held" lookup
 * there instead. The disappearing-scale animation depended on {@code animatable.disappearController} (live entity
 * field, not available any more in {@code submitItemStackRender}, which only has the pre-resolved
 * {@code ItemStackRenderState}) - captured into a plain instance field during {@code addRenderData}, the same
 * "capture live data early into a field on the renderer/layer instance" idiom already used elsewhere in this port
 * (see {@code RenderUmvuthi}'s {@code showBurst}/{@code burstTicksInUse} fields).
 */
public class ItemLayerSculptorStaff<R extends GeoRenderState> extends GeckoItemlayer<EntitySculptor, R> {
    private float disappearController = 0;

    public ItemLayerSculptorStaff(GeoRenderer<EntitySculptor, Void, R> rendererIn, String boneName) {
        super(rendererIn, boneName, ItemStack.EMPTY);
    }

    @Override
    protected ItemStack getStackForBone(String boneName, EntitySculptor animatable) {
        return this.boneName.equals(boneName) ? animatable.heldStaff : null;
    }

    @Override
    public void addRenderData(EntitySculptor animatable, @Nullable Void relatedObject, R renderState, float partialTick) {
        this.disappearController = animatable.disappearController;
        super.addRenderData(animatable, relatedObject, renderState, partialTick);
    }

    @Override
    protected void submitItemStackRender(RenderPassInfo<R> renderPassInfo, ItemStackRenderState stackState, SubmitNodeCollector renderTasks) {
        PoseStack poseStack = renderPassInfo.poseStack();

        poseStack.pushPose();
        float disappearProgress = Math.min(disappearController * 2f, 1f);
        poseStack.scale(1.0f - disappearProgress, 1.0f - disappearProgress, 1.0f - disappearProgress);
        super.submitItemStackRender(renderPassInfo, stackState, renderTasks);
        poseStack.popPose();
    }
}
