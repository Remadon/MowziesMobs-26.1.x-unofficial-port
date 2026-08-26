package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.client.model.tools.MathUtils;
import com.bobmowzie.mowziesmobs.client.render.MMRenderType;
import com.bobmowzie.mowziesmobs.server.entity.effects.EntitySolarBeam;
import com.ilexiconn.llibrary.client.util.ClientUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * PORTING NOTE: this renderer reads a large number of live-entity-only fields (interpolated collide/appear/render
 * position and angle pairs, {@code caster}, {@code blockSide}, {@code appear.getTimer()}) that have no equivalent on
 * the base render state - the render state captures a live entity reference, matching the established LLibrary-model
 * pattern used throughout this scope (this renderer has no LLibrary model, but the same rationale applies: too much
 * bespoke live-entity state to decompose into individual fields). All raw-vertex geometry building goes through the
 * `pose` snapshot handed to the `submitCustomGeometry` callback (a `PoseStack.Pose`, captured at submission time -
 * NOT a live view of the outer, later-mutated `poseStack` - see PORTING_NOTES.md's "submitCustomGeometry deferred
 * pose" finding) rather than reading `poseStack.last()` from within the deferred callback.
 */
public class RenderSolarBeam extends EntityRenderer<EntitySolarBeam, RenderSolarBeam.SolarBeamRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/effects/solar_beam.png");
    private static final float TEXTURE_WIDTH = 256;
    private static final float TEXTURE_HEIGHT = 32;
    private static final float START_RADIUS = 1.3f;
    private static final float BEAM_RADIUS = 1;

    public RenderSolarBeam(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

    @Override
    public SolarBeamRenderState createRenderState() {
        return new SolarBeamRenderState();
    }

    @Override
    public void extractRenderState(EntitySolarBeam entity, SolarBeamRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        state.entity = entity;
        state.delta = partialTicks;
        state.clearerView = entity.caster instanceof Player
                && Minecraft.getInstance().player == entity.caster
                && Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON;
    }

    @Override
    public void submit(SolarBeamRenderState state, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState cameraState) {
        EntitySolarBeam solarBeam = state.entity;
        float delta = state.delta;
        boolean clearerView = state.clearerView;

        double collidePosX = solarBeam.prevCollidePosX + (solarBeam.collidePosX - solarBeam.prevCollidePosX) * delta;
        double collidePosY = solarBeam.prevCollidePosY + (solarBeam.collidePosY - solarBeam.prevCollidePosY) * delta;
        double collidePosZ = solarBeam.prevCollidePosZ + (solarBeam.collidePosZ - solarBeam.prevCollidePosZ) * delta;
        double posX = solarBeam.xo + (solarBeam.getX() - solarBeam.xo) * delta;
        double posY = solarBeam.yo + (solarBeam.getY() - solarBeam.yo) * delta;
        double posZ = solarBeam.zo + (solarBeam.getZ() - solarBeam.zo) * delta;
        float yaw = solarBeam.prevYaw + (solarBeam.renderYaw - solarBeam.prevYaw) * delta;
        float pitch = solarBeam.prevPitch + (solarBeam.renderPitch - solarBeam.prevPitch) * delta;

        float length = (float) Math.sqrt(Math.pow(collidePosX - posX, 2) + Math.pow(collidePosY - posY, 2) + Math.pow(collidePosZ - posZ, 2));
        int frame = Mth.floor((solarBeam.appear.getTimer() - 1 + delta) * 2);
        if (frame < 0) {
            frame = 6;
        }
        int finalFrame = frame;
        int packedLight = state.lightCoords;
        RenderType renderType = MMRenderType.getGlowingEffect(TEXTURE);

        renderTasks.submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) -> {
            renderStart(clearerView, finalFrame, pose, vertexConsumer, packedLight);
            renderBeam(clearerView, length, 180f / (float) Math.PI * yaw, 180f / (float) Math.PI * pitch, finalFrame, pose, vertexConsumer, packedLight);

            PoseStack endStack = new PoseStack();
            endStack.last().set(pose);
            endStack.translate(collidePosX - posX, collidePosY - posY, collidePosZ - posZ);
            renderEnd(finalFrame, solarBeam.blockSide, endStack, vertexConsumer, packedLight);
        });

        super.submit(state, poseStack, renderTasks, cameraState);
    }

    private void renderFlatQuad(int frame, Matrix4f matrix4f, Matrix3f matrix3f, VertexConsumer builder, int packedLightIn) {
        float minU = 0 + 16F / TEXTURE_WIDTH * frame;
        float minV = 0;
        float maxU = minU + 16F / TEXTURE_WIDTH;
        float maxV = minV + 16F / TEXTURE_HEIGHT;
        drawVertex(matrix4f, matrix3f, builder, -START_RADIUS, -START_RADIUS, 0, minU, minV, 1, packedLightIn);
        drawVertex(matrix4f, matrix3f, builder, -START_RADIUS, START_RADIUS, 0, minU, maxV, 1, packedLightIn);
        drawVertex(matrix4f, matrix3f, builder, START_RADIUS, START_RADIUS, 0, maxU, maxV, 1, packedLightIn);
        drawVertex(matrix4f, matrix3f, builder, START_RADIUS, -START_RADIUS, 0, maxU, minV, 1, packedLightIn);
    }

    private void renderStart(boolean clearerView, int frame, PoseStack.Pose parentPose, VertexConsumer builder, int packedLightIn) {
        if (clearerView) {
            return;
        }
        PoseStack matrixStackIn = new PoseStack();
        matrixStackIn.last().set(parentPose);
        Quaternionf quat = this.entityRenderDispatcher.camera.rotation();
        matrixStackIn.mulPose(quat);
        renderFlatQuad(frame, matrixStackIn.last().pose(), matrixStackIn.last().normal(), builder, packedLightIn);
    }

    private void renderEnd(int frame, Direction side, PoseStack matrixStackIn, VertexConsumer builder, int packedLightIn) {
        matrixStackIn.pushPose();
        Quaternionf quat = this.entityRenderDispatcher.camera.rotation();
        matrixStackIn.mulPose(quat);
        renderFlatQuad(frame, matrixStackIn.last().pose(), matrixStackIn.last().normal(), builder, packedLightIn);
        matrixStackIn.popPose();
        if (side == null) {
            return;
        }
        matrixStackIn.pushPose();
        Quaternionf sideQuat = side.getRotation();
        sideQuat.mul(MathUtils.quatFromRotationXYZ(90, 0, 0, true));
        matrixStackIn.mulPose(sideQuat);
        matrixStackIn.translate(0, 0, -0.01f);
        renderFlatQuad(frame, matrixStackIn.last().pose(), matrixStackIn.last().normal(), builder, packedLightIn);
        matrixStackIn.popPose();
    }

    private void drawBeam(boolean clearerView, float length, int frame, Matrix4f matrix4f, Matrix3f matrix3f, VertexConsumer builder, int packedLightIn) {
        float minU = 0;
        float minV = 16 / TEXTURE_HEIGHT + 1 / TEXTURE_HEIGHT * frame;
        float maxU = minU + 20 / TEXTURE_WIDTH;
        float maxV = minV + 1 / TEXTURE_HEIGHT;
        float offset = clearerView ? -1 : 0;
        drawVertex(matrix4f, matrix3f, builder, -BEAM_RADIUS, offset, 0, minU, minV, 1, packedLightIn);
        drawVertex(matrix4f, matrix3f, builder, -BEAM_RADIUS, length, 0, minU, maxV, 1, packedLightIn);
        drawVertex(matrix4f, matrix3f, builder, BEAM_RADIUS, length, 0, maxU, maxV, 1, packedLightIn);
        drawVertex(matrix4f, matrix3f, builder, BEAM_RADIUS, offset, 0, maxU, minV, 1, packedLightIn);
    }

    private void renderBeam(boolean clearerView, float length, float yaw, float pitch, int frame, PoseStack.Pose parentPose, VertexConsumer builder, int packedLightIn) {
        PoseStack matrixStackIn = new PoseStack();
        matrixStackIn.last().set(parentPose);
        matrixStackIn.mulPose(MathUtils.quatFromRotationXYZ(90, 0, 0, true));
        matrixStackIn.mulPose(MathUtils.quatFromRotationXYZ(0, 0, yaw - 90f, true));
        matrixStackIn.mulPose(MathUtils.quatFromRotationXYZ(-pitch, 0, 0, true));
        matrixStackIn.pushPose();
        if (!clearerView) {
            matrixStackIn.mulPose(MathUtils.quatFromRotationXYZ(0, Minecraft.getInstance().gameRenderer.getMainCamera().xRot() + 90, 0, true));
        }
        drawBeam(clearerView, length, frame, matrixStackIn.last().pose(), matrixStackIn.last().normal(), builder, packedLightIn);
        matrixStackIn.popPose();

        if (!clearerView) {
            matrixStackIn.pushPose();
            matrixStackIn.mulPose(MathUtils.quatFromRotationXYZ(0, -Minecraft.getInstance().gameRenderer.getMainCamera().xRot() - 90, 0, true));
            drawBeam(clearerView, length, frame, matrixStackIn.last().pose(), matrixStackIn.last().normal(), builder, packedLightIn);
            matrixStackIn.popPose();
        }
    }

    public void drawVertex(Matrix4f matrix, Matrix3f normals, VertexConsumer vertexBuilder, float offsetX, float offsetY, float offsetZ, float textureX, float textureY, float alpha, int packedLightIn) {
        VertexConsumer vertex = vertexBuilder.addVertex(matrix, offsetX, offsetY, offsetZ).setColor(1, 1, 1, 1 * alpha).setUv(textureX, textureY).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLightIn);
        ClientUtils.transformNormals(vertex, normals, 1, 0, 1);
    }

    public static class SolarBeamRenderState extends EntityRenderState {
        public EntitySolarBeam entity;
        public float delta;
        public boolean clearerView;
    }
}
