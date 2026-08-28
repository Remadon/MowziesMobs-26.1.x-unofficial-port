package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.client.model.tools.MathUtils;
import com.bobmowzie.mowziesmobs.client.render.MMRenderType;
import com.bobmowzie.mowziesmobs.server.entity.effects.EntitySunstrike;
import com.ilexiconn.llibrary.client.util.ClientUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * PORTING NOTE: this renderer needs a large amount of live-entity/level state (block queries around the entity,
 * variant seed, lingering/striking timers) - the render state captures a live entity reference, matching the
 * established pattern used throughout this scope. Raw-vertex geometry building goes through the `pose` snapshot
 * handed to the `submitCustomGeometry` callback rather than the (by-then mutated/popped) outer `poseStack` - see
 * PORTING_NOTES.md's "submitCustomGeometry deferred pose" finding.
 * <p>
 * Also resolves the {@code Entity#noCulling} FIXME left in {@code EntitySunstrike}'s constructor (see
 * PORTING_NOTES.md "Entity#noCulling field REMOVED ENTIRELY" section): overrides {@link #affectedByCulling} to
 * always render regardless of the entity's computed bounding box, matching the old {@code noCulling = true} behavior
 * (this entity draws a beam that can be much taller/wider than its actual hitbox).
 * <p>
 * NEW finding (add to PORTING_NOTES.md watch-list): {@code Entity#getCommandSenderWorld()} (the old
 * {@code CommandSource}-inherited accessor, equivalent to {@code level()}) no longer exists anywhere in the 26.1.2
 * source (grep-confirmed zero hits across the vanilla + NeoForge decompiled trees) - replaced with the plain
 * {@code Entity#level()} call it always delegated to.
 */
public class RenderSunstrike extends EntityRenderer<EntitySunstrike, RenderSunstrike.SunstrikeRenderState> {
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/effects/sunstrike.png");
    private static final Random RANDOMIZER = new Random(0);
    private static final float TEXTURE_WIDTH = 256;
    private static final float TEXTURE_HEIGHT = 32;
    private static final float BEAM_MIN_U = 224f / TEXTURE_WIDTH;
    private static final float BEAM_MAX_U = 1;
    private static final float PIXEL_SCALE = 1 / 16f;
    private static final int MAX_HEIGHT = 256;
    private static final float DRAW_FADE_IN_RATE = 2;
    private static final float DRAW_FADE_IN_POINT = 1 / DRAW_FADE_IN_RATE;
    private static final float DRAW_OPACITY_MULTIPLER = 0.7f;
    private static final float RING_RADIUS = 1.6f;
    private static final int RING_FRAME_SIZE = 16;
    private static final int RING_FRAME_COUNT = 10;
    private static final int BREAM_FRAME_COUNT = 31;
    private static final float BEAM_DRAW_START_RADIUS = 2f;
    private static final float BEAM_DRAW_END_RADIUS = 0.25f;
    private static final float BEAM_STRIKE_RADIUS = 1f;
    private static final float LINGER_RADIUS = 1.2f;
    private static final float SCORCH_MIN_U = 192f / TEXTURE_WIDTH;
    private static final float SCORCH_MAX_U = SCORCH_MIN_U + RING_FRAME_SIZE / TEXTURE_WIDTH;
    private static final float SCORCH_MIN_V = 16f / TEXTURE_HEIGHT;
    private static final float SCORCH_MAX_V = SCORCH_MIN_V + RING_FRAME_SIZE / TEXTURE_HEIGHT;

    public RenderSunstrike(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

    @Override
    protected boolean affectedByCulling(EntitySunstrike entity) {
        return false;
    }

    @Override
    public SunstrikeRenderState createRenderState() {
        return new SunstrikeRenderState();
    }

    @Override
    public void extractRenderState(EntitySunstrike entity, SunstrikeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        state.entity = entity;
        state.delta = partialTicks;
    }

    @Override
    public void submit(SunstrikeRenderState state, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState cameraState) {
        EntitySunstrike sunstrike = state.entity;
        float delta = state.delta;

        float maxY = (float) (MAX_HEIGHT - sunstrike.getY());
        if (maxY < 0) {
            super.submit(state, poseStack, renderTasks, cameraState);
            return;
        }
        boolean isLingering = sunstrike.isLingering(delta);
        boolean isStriking = sunstrike.isStriking(delta);
        int packedLight = state.lightCoords;
        RenderType renderType = MMRenderType.getGlowingEffect(TEXTURE);

        if (isLingering) {
            renderTasks.submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) ->
                    drawScorch(sunstrike, delta, pose, vertexConsumer, packedLight));
        } else if (isStriking) {
            renderTasks.submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) ->
                    drawStrike(sunstrike, maxY, delta, pose, vertexConsumer, packedLight));
        }

        super.submit(state, poseStack, renderTasks, cameraState);
    }

    // FOLLOW-UP FIX (scorch mark visibly "spinning"/flickering with more than ~2 Sunstrikes lingering at once):
    // RANDOMIZER.setSeed(sunstrike.getVariant()) used to be called synchronously in submit() - the submit phase -
    // while the RANDOMIZER.nextFloat()/nextBoolean() calls that actually consume it (opacityMultiplier, mirrorX,
    // mirrorZ below) only run later, inside this method, deferred via submitCustomGeometry's callback (the draw
    // phase). RANDOMIZER is a single shared static field: with only one lingering Sunstrike that timing gap didn't
    // matter (only one submit() call, always immediately followed - eventually - by only one drawScorch() call
    // sharing the same seed), but with multiple entities lingering simultaneously, ALL of their submit() calls
    // (each re-seeding the SAME shared RANDOMIZER to their own variant) run to completion before ANY of their
    // deferred drawScorch() callbacks do - so by draw time the seed is whatever the last entity's submit() left
    // it at, and each entity's random draws consume further into that same shared sequence rather than its own -
    // producing different, frame-to-frame-unstable mirrorX/mirrorZ/opacity values per scorch mark, which reads as
    // flickering/spinning. Moving the seed reset to right here, immediately before it's consumed, keeps each
    // entity's random draws self-contained and deterministic regardless of how many other entities are rendering
    // or what order their callbacks run in.
    private void drawScorch(EntitySunstrike sunstrike, float delta, PoseStack.Pose parentPose, VertexConsumer builder, int packedLightIn) {
        RANDOMIZER.setSeed(sunstrike.getVariant());
        Level world = sunstrike.level();
        double ex = sunstrike.xOld + (sunstrike.getX() - sunstrike.xOld) * delta;
        double ey = sunstrike.yOld + (sunstrike.getY() - sunstrike.yOld) * delta;
        double ez = sunstrike.zOld + (sunstrike.getZ() - sunstrike.zOld) * delta;
        int minX = Mth.floor(ex - LINGER_RADIUS);
        int maxX = Mth.floor(ex + LINGER_RADIUS);
        int minY = Mth.floor(ey - LINGER_RADIUS);
        int maxY = Mth.floor(ey);
        int minZ = Mth.floor(ez - LINGER_RADIUS);
        int maxZ = Mth.floor(ez + LINGER_RADIUS);
        float opacityMultiplier = (0.6F + RANDOMIZER.nextFloat() * 0.2F) * world.getMaxLocalRawBrightness(new BlockPos((int) ex, (int) ey, (int) ez));
        byte mirrorX = (byte) (RANDOMIZER.nextBoolean() ? -1 : 1);
        byte mirrorZ = (byte) (RANDOMIZER.nextBoolean() ? -1 : 1);
        Matrix4f matrix4f = parentPose.pose();
        Matrix3f matrix3f = parentPose.normal();
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ))) {
            BlockState block = world.getBlockState(pos.below());
            if (!block.isAir() && world.getMaxLocalRawBrightness(pos) > 3) {
                drawScorchBlock(world, block, pos, ex, ey, ez, opacityMultiplier, mirrorX, mirrorZ, matrix4f, matrix3f, builder, packedLightIn);
            }
        }
    }

    private void drawScorchBlock(Level world, BlockState block, BlockPos pos, double ex, double ey, double ez, float opacityMultiplier, byte mirrorX, byte mirrorZ, Matrix4f matrix4f, Matrix3f matrix3f, VertexConsumer builder, int packedLightIn) {
        if (block.isRedstoneConductor(world, pos)) {
            int bx = pos.getX(), by = pos.getY(), bz = pos.getZ();
            float opacity = (float) ((1 - (ey - by) / 2) * opacityMultiplier);
            if (opacity >= 0) {
                if (opacity > 1) {
                    opacity = 1;
                }
                VoxelShape shape = block.getBlockSupportShape(world, pos);
                if (!shape.isEmpty()) {
                    AABB aabb = shape.bounds();
                    float minX = (float) (bx + aabb.minX - ex);
                    float maxX = (float) (bx + aabb.maxX - ex);
                    float y = (float) (by + aabb.minY - ey + 0.015625f);
                    float minZ = (float) (bz + aabb.minZ - ez);
                    float maxZ = (float) (bz + aabb.maxZ - ez);
                    float minU = (mirrorX * minX / 2f / LINGER_RADIUS + 0.5f) * (SCORCH_MAX_U - SCORCH_MIN_U) + SCORCH_MIN_U;
                    float maxU = (mirrorX * maxX / 2f / LINGER_RADIUS + 0.5f) * (SCORCH_MAX_U - SCORCH_MIN_U) + SCORCH_MIN_U;
                    float minV = (mirrorZ * minZ / 2f / LINGER_RADIUS + 0.5f) * (SCORCH_MAX_V - SCORCH_MIN_V) + SCORCH_MIN_V;
                    float maxV = (mirrorZ * maxZ / 2f / LINGER_RADIUS + 0.5f) * (SCORCH_MAX_V - SCORCH_MIN_V) + SCORCH_MIN_V;
                    drawVertex(matrix4f, matrix3f, builder, minX, y, minZ, minU, minV, opacity, packedLightIn);
                    drawVertex(matrix4f, matrix3f, builder, minX, y, maxZ, minU, maxV, opacity, packedLightIn);
                    drawVertex(matrix4f, matrix3f, builder, maxX, y, maxZ, maxU, maxV, opacity, packedLightIn);
                    drawVertex(matrix4f, matrix3f, builder, maxX, y, minZ, maxU, minV, opacity, packedLightIn);
                }
            }
        }
    }

    private void drawStrike(EntitySunstrike sunstrike, float maxY, float delta, PoseStack.Pose parentPose, VertexConsumer builder, int packedLightIn) {
        float drawTime = sunstrike.getStrikeDrawTime(delta);
        float strikeTime = sunstrike.getStrikeDamageTime(delta);
        boolean drawing = sunstrike.isStrikeDrawing(delta);
        float opacity = drawing && drawTime < DRAW_FADE_IN_POINT ? drawTime * DRAW_FADE_IN_RATE : 1;
        if (drawing) {
            opacity *= DRAW_OPACITY_MULTIPLER;
        }
        drawRing(drawing, drawTime, strikeTime, opacity, parentPose.pose(), parentPose.normal(), builder, packedLightIn);

        PoseStack beamStack = new PoseStack();
        beamStack.last().set(parentPose);
        beamStack.mulPose(MathUtils.quatFromRotationXYZ(0, -Minecraft.getInstance().gameRenderer.getMainCamera().yRot(), 0, true));
        drawBeam(drawing, drawTime, strikeTime, opacity, maxY, beamStack, builder, packedLightIn);
    }

    private void drawRing(boolean drawing, float drawTime, float strikeTime, float opacity, Matrix4f matrix4f, Matrix3f matrix3f, VertexConsumer builder, int packedLightIn) {
        int frame = (int) (((drawing ? drawTime : strikeTime) * (RING_FRAME_COUNT + 1)));
        if (frame > RING_FRAME_COUNT) {
            frame = RING_FRAME_COUNT;
        }
        float minU = frame * RING_FRAME_SIZE / TEXTURE_WIDTH;
        float maxU = minU + RING_FRAME_SIZE / TEXTURE_WIDTH;
        float minV = drawing ? 0 : RING_FRAME_SIZE / TEXTURE_HEIGHT;
        float maxV = minV + RING_FRAME_SIZE / TEXTURE_HEIGHT;
        float offset = PIXEL_SCALE * RING_RADIUS * (frame % 2);
        drawVertex(matrix4f, matrix3f, builder, -RING_RADIUS + offset, 0, -RING_RADIUS + offset, minU, minV, opacity, packedLightIn);
        drawVertex(matrix4f, matrix3f, builder, -RING_RADIUS + offset, 0, RING_RADIUS + offset, minU, maxV, opacity, packedLightIn);
        drawVertex(matrix4f, matrix3f, builder, RING_RADIUS + offset, 0, RING_RADIUS + offset, maxU, maxV, opacity, packedLightIn);
        drawVertex(matrix4f, matrix3f, builder, RING_RADIUS + offset, 0, -RING_RADIUS + offset, maxU, minV, opacity, packedLightIn);
    }

    private void drawBeam(boolean drawing, float drawTime, float strikeTime, float opacity, float maxY, PoseStack matrixStack, VertexConsumer builder, int packedLightIn) {
        int frame = drawing ? 0 : (int) (strikeTime * (BREAM_FRAME_COUNT + 1));
        if (frame > BREAM_FRAME_COUNT) {
            frame = BREAM_FRAME_COUNT;
        }
        float radius = BEAM_STRIKE_RADIUS;
        if (drawing) {
            radius = (BEAM_DRAW_END_RADIUS - BEAM_DRAW_START_RADIUS) * drawTime + BEAM_DRAW_START_RADIUS;
        }
        float minV = frame / TEXTURE_HEIGHT;
        float maxV = (frame + 1) / TEXTURE_HEIGHT;
        Matrix4f matrix4f = matrixStack.last().pose();
        Matrix3f matrix3f = matrixStack.last().normal();
        drawVertex(matrix4f, matrix3f, builder, -radius, 0, 0, BEAM_MIN_U, minV, opacity, packedLightIn);
        drawVertex(matrix4f, matrix3f, builder, -radius, maxY, 0, BEAM_MIN_U, maxV, opacity, packedLightIn);
        drawVertex(matrix4f, matrix3f, builder, radius, maxY, 0, BEAM_MAX_U, maxV, opacity, packedLightIn);
        drawVertex(matrix4f, matrix3f, builder, radius, 0, 0, BEAM_MAX_U, minV, opacity, packedLightIn);
    }

    public void drawVertex(Matrix4f matrix, Matrix3f normals, VertexConsumer vertexBuilder, float offsetX, float offsetY, float offsetZ, float textureX, float textureY, float alpha, int packedLightIn) {
        VertexConsumer vertex = vertexBuilder.addVertex(matrix, offsetX, offsetY, offsetZ).setColor(1, 1, 1, 1 * alpha).setUv(textureX, textureY).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLightIn);
        ClientUtils.transformNormals(vertex, normals, 1, 0, 1);
    }

    public static class SunstrikeRenderState extends EntityRenderState {
        public EntitySunstrike entity;
        public float delta;
    }
}
