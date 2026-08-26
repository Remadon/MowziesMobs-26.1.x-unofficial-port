package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.client.model.entity.ModelAxeAttack;
import com.bobmowzie.mowziesmobs.server.config.ConfigHandler;
import com.bobmowzie.mowziesmobs.server.entity.effects.EntityAxeAttack;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * PORTING NOTE: this renderer entirely depends on live entity data (caster identity, interpolated positions of both
 * the axe and the caster) not carried by the base render state - captures a live entity reference, matching the
 * established LLibrary-model pattern used throughout this scope.
 */
public class RenderAxeAttack extends EntityRenderer<EntityAxeAttack, RenderAxeAttack.AxeAttackRenderState> {
    public static Identifier TEXTURE = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/entity/wroughtnaut.png");

    ModelAxeAttack model;

    public RenderAxeAttack(EntityRendererProvider.Context mgr) {
        super(mgr);
        model = new ModelAxeAttack();
    }

    @Override
    public AxeAttackRenderState createRenderState() {
        return new AxeAttackRenderState();
    }

    @Override
    public void extractRenderState(EntityAxeAttack entity, AxeAttackRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        state.entity = entity;
        state.partialTicks = partialTicks;
    }

    @Override
    public void submit(AxeAttackRenderState state, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState cameraState) {
        EntityAxeAttack axe = state.entity;
        float delta = state.partialTicks;

        if (!ConfigHandler.CLIENT.customPlayerAnims.get()) {
            Player player = Minecraft.getInstance().player;
            if (player != null && player == axe.getCaster()) {
                poseStack.pushPose();
                Vec3 prevAxePos = new Vec3(axe.xOld, axe.yOld, axe.zOld);
                Vec3 prevPlayerPos = new Vec3(player.xOld, player.yOld, player.zOld);
                Vec3 axePos = prevAxePos.add(axe.position().subtract(prevAxePos).scale(delta));
                Vec3 playerPos = prevPlayerPos.add(player.position().subtract(prevPlayerPos).scale(delta));
                Vec3 deltaPos = axePos.subtract(playerPos).scale(-1);
                poseStack.translate(deltaPos.x(), deltaPos.y(), deltaPos.z());
                poseStack.mulPose(new Quaternionf(new AxisAngle4f(player.getYRot() * (float) Math.PI / 180f, new Vector3f(0, -1, 0))));

                // NOTE: submitCustomGeometry's callback runs later (deferred) - see RenderPoisonBall.java's javadoc /
                // PORTING_NOTES.md's "submitCustomGeometry deferred pose" finding for why this restores `pose`.
                renderTasks.submitCustomGeometry(poseStack, RenderTypes.entitySolid(TEXTURE), (pose, vertexConsumer) -> {
                    poseStack.pushPose();
                    poseStack.last().set(pose);
                    model.setupAnim(axe, 0, 0, axe.tickCount + delta, 0, 0);
                    model.renderToBuffer(poseStack, vertexConsumer, state.lightCoords, OverlayTexture.NO_OVERLAY, -1);
                    poseStack.popPose();
                });

                poseStack.popPose();
            }
        }

        super.submit(state, poseStack, renderTasks, cameraState);
    }

    public static class AxeAttackRenderState extends EntityRenderState {
        public EntityAxeAttack entity;
        public float partialTicks;
    }
}
