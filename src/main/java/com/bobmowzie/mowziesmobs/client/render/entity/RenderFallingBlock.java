package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.client.model.tools.MathUtils;
import com.bobmowzie.mowziesmobs.server.entity.effects.EntityFallingBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/**
 * PORTING NOTE: {@code BlockRenderDispatcher}/{@code renderSingleBlock(...)} no longer exist at all (see
 * PORTING_NOTES.md "NEW finding: BlockRenderDispatcher was removed entirely" section, and
 * {@code client/render/entity/layer/BlockLayer.java}'s javadoc for the same replacement already applied there):
 * {@code Minecraft.getInstance().getBlockModelResolver()} -> {@code BlockModelResolver#update(BlockModelRenderState,
 * BlockState, BlockDisplayContext)} -> {@code BlockModelRenderState#submit(PoseStack, SubmitNodeCollector, int, int,
 * int)}. This entity needs live per-frame fields (mode, yRotO/getYRot, xRotO/getXRot, prevAnimY/animY, getBlock()) so
 * the render state simply captures a live entity reference, matching the established LLibrary-model pattern used
 * throughout this scope even though this renderer has no LLibrary model at all.
 */
public class RenderFallingBlock extends EntityRenderer<EntityFallingBlock, RenderFallingBlock.FallingBlockRenderState> {
    public RenderFallingBlock(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

    @Override
    public FallingBlockRenderState createRenderState() {
        return new FallingBlockRenderState();
    }

    @Override
    public void extractRenderState(EntityFallingBlock entity, FallingBlockRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        state.entity = entity;
        state.partialTicks = partialTicks;
    }

    @Override
    public void submit(FallingBlockRenderState state, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState cameraState) {
        EntityFallingBlock entityIn = state.entity;
        float partialTicks = state.partialTicks;

        poseStack.pushPose();
        poseStack.translate(0, 0.5f, 0);
        if (entityIn.getMode() == EntityFallingBlock.EnumFallingBlockMode.MOBILE) {
            poseStack.mulPose(MathUtils.quatFromRotationXYZ(0, Mth.lerp(partialTicks, entityIn.yRotO, entityIn.getYRot()), 0, true));
            poseStack.mulPose(MathUtils.quatFromRotationXYZ(Mth.lerp(partialTicks, entityIn.xRotO, entityIn.getXRot()), 0, 0, true));
        }
        else {
            poseStack.translate(0, Mth.lerp(partialTicks, entityIn.prevAnimY, entityIn.animY), 0);
            poseStack.translate(0, -1, 0);
        }
        poseStack.translate(-0.5f, -0.5f, -0.5f);

        BlockState blockState = entityIn.getBlock();
        BlockModelRenderState blockRenderState = new BlockModelRenderState();
        Minecraft.getInstance().getBlockModelResolver().update(blockRenderState, blockState, BlockDisplayContext.create());
        blockRenderState.submit(poseStack, renderTasks, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        poseStack.popPose();

        super.submit(state, poseStack, renderTasks, cameraState);
    }

    public static class FallingBlockRenderState extends EntityRenderState {
        public EntityFallingBlock entity;
        public float partialTicks;
    }
}
