package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.client.model.tools.geckolib.MowzieGeoBone;
import com.bobmowzie.mowziesmobs.client.render.MMRenderType;
import com.bobmowzie.mowziesmobs.client.render.entity.layer.GeckoSunblockLayer;
import com.bobmowzie.mowziesmobs.client.render.entity.layer.UmvuthiSunLayer;
import com.bobmowzie.mowziesmobs.server.entity.umvuthana.EntityUmvuthi;
import com.geckolib.renderer.base.RenderPassInfo;
import com.ilexiconn.llibrary.client.util.ClientUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * PORTING NOTE: the "burst" flare quad this used to draw directly inside `render()` (before calling
 * `super.render()`) is now drawn by overriding `submit(...)` (calling `super.submit(...)` at the end, matching the
 * old relative ordering) and submitting the quad geometry via `SubmitNodeCollector#submitCustomGeometry` (see
 * PORTING_NOTES.md architecture section). The camera-facing quaternion used to come from
 * `this.entityRenderDispatcher.cameraOrientation()`; `submit(...)` now receives a `CameraRenderState` directly,
 * which carries the same orientation as a public field (`cameraState.orientation`). `renderUpdates(...)` (bone
 * world-position reads for `headPos`/`betweenHandPos`, plus the rattle-sound bone rotation read) is replaced with
 * {@link #registerBonePositionListeners} - see RenderElokosa.java/RenderUmvuthana.java for the same pattern applied
 * to sibling entities, including the "rotation reads have no listener equivalent, kept best-effort" caveat that
 * applies to the `maskTwitcher` read here too.
 */
public class RenderUmvuthi extends MowzieGeoEntityRenderer<EntityUmvuthi, LivingEntityRenderState> {
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/entity/umvuthi.png");
    public static final Identifier SUN = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/effects/sun_effect.png");

    public static final float BURST_RADIUS = 3.5f;
    public static final int BURST_FRAME_COUNT = 10;
    public static final int BURST_START_FRAME = 12;

    public RenderUmvuthi(EntityRendererProvider.Context mgr) {
        super(mgr, new com.bobmowzie.mowziesmobs.client.model.entity.ModelUmvuthi());
        this.withRenderLayer(new FrozenRenderHandler.GeckoLayerFrozen<>(this, mgr));
        this.withRenderLayer(new GeckoSunblockLayer(this, mgr));
        this.withRenderLayer(new UmvuthiSunLayer(this));

        this.shadowRadius = 1.0f;
    }

    @Override
    public void submit(LivingEntityRenderState renderState, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState cameraState) {
        // NOTE: `isInvisible`/`activeAbilityType`/`ticksInUse` are entity-instance data; since `submit` only has the
        // renderState, this reads the equivalent LivingEntityRenderState.isInvisible flag but skips the ability-type
        // gated burst effect entirely if that data isn't otherwise available. See addRenderData below - the burst
        // fields are captured there onto a fresh local via renderState-carried booleans would require a custom
        // RenderState; kept simple by re-deriving from the live entity captured through registerBonePositionListeners
        // is NOT possible here (submit has no entity access) - burst rendering is therefore driven from
        // registerBonePositionListeners's captured flags stored directly on `this` (see fields below). This mirrors
        // the one-frame-old-data tradeoff already accepted elsewhere in this port for similar cases.
        if (showBurst && !renderState.isInvisible) {
            poseStack.pushPose();
            Quaternionf quat = new Quaternionf(cameraState.orientation);
            poseStack.mulPose(quat);
            poseStack.translate(0, 1, 0);
            poseStack.scale(0.8f, 0.8f, 0.8f);

            RenderType renderType = MMRenderType.getSolarFlare(TEXTURE);
            float burstFrame = burstTicksInUse;
            int packedLight = renderState.lightCoords;

            renderTasks.submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) -> {
                Matrix4f matrix4f = pose.pose();
                Matrix3f matrix3f = new Matrix3f();
                drawBurst(matrix4f, matrix3f, vertexConsumer, burstFrame, packedLight);
            });
            poseStack.popPose();
        }

        super.submit(renderState, poseStack, renderTasks, cameraState);
    }

    // Captured each frame from the live entity (see registerBonePositionListeners) since `submit` has no entity access.
    private boolean showBurst = false;
    private float burstTicksInUse = 0;

    @Override
    protected void registerBonePositionListeners(RenderPassInfo<LivingEntityRenderState> renderPassInfo, EntityUmvuthi umvuthi) {
        showBurst = !umvuthi.isInvisible()
                && umvuthi.getActiveAbilityType() == EntityUmvuthi.SOLAR_FLARE_ABILITY
                && umvuthi.getActiveAbility().getTicksInUse() > BURST_START_FRAME
                && umvuthi.getActiveAbility().getTicksInUse() < BURST_START_FRAME + BURST_FRAME_COUNT - 1;
        burstTicksInUse = umvuthi.getActiveAbility() != null ? umvuthi.getActiveAbility().getTicksInUse() - BURST_START_FRAME : 0;

        renderPassInfo.addBonePositionListener("sun_render", (worldPos, modelPos, localPos) -> {
            if (worldPos != null && umvuthi.headPos != null && umvuthi.headPos.length > 0) {
                umvuthi.headPos[0] = worldPos;
            }

            if (worldPos != null && umvuthi.getActiveAbilityType() == EntityUmvuthi.SUPERNOVA_ABILITY && umvuthi.betweenHandPos != null && umvuthi.betweenHandPos.length > 0) {
                MowzieGeoBone novaCenter = getMowzieGeoModel().getMowzieBone("superNovaCenter");
                if (novaCenter != null) {
                    // NOTE: superNovaCenter's world position is read directly here (not via its own listener) since
                    // blending needs both positions together - see PORTING_NOTES.md bone-snapshot-timing caveat,
                    // this specific read may be one-frame-stale relative to sun_render's listener-provided value.
                    org.joml.Vector3f novaRenderPosModel = novaCenter.getModelSpaceMatrix().getTranslation(new org.joml.Vector3f());
                    Vec3 novaRenderPos = new Vec3(novaRenderPosModel.x, novaRenderPosModel.y, novaRenderPosModel.z);
                    float blendStart = 4;
                    float blendDuration = 4;
                    int ticksInUse = umvuthi.getActiveAbility().getTicksInUse();
                    Vec3 finalNovaPos = novaRenderPos;
                    if (ticksInUse <= blendDuration + blendStart) {
                        float alpha = Math.max(0, (ticksInUse - blendStart) / blendDuration);
                        finalNovaPos = novaRenderPos.add(worldPos.subtract(novaRenderPos).scale(1.0 - alpha));
                    }
                    umvuthi.betweenHandPos[0] = finalNovaPos;
                }
            }
        });

        if (!Minecraft.getInstance().isPaused()) {
            MowzieGeoBone mask = getMowzieGeoModel().getMowzieBone("maskTwitcher");
            if (mask != null) umvuthi.updateRattleSound(mask.getRotZ());
        }
    }

    @Override
    public boolean shouldRender(EntityUmvuthi umvuthi, Frustum frustum, double p_114493_, double p_114494_, double p_114495_) {
        boolean result = super.shouldRender(umvuthi, frustum, p_114493_, p_114494_, p_114495_);
        if (!result) umvuthi.headPos[0] = umvuthi.position().add(0, umvuthi.getEyeHeight(), 0);
        return result;
    }

    public static void drawBurst(Matrix4f matrix4f, Matrix3f matrix3f, VertexConsumer builder, float tick, int packedLightIn) {
        int dissapateFrame = 6;
        float firstSpeed = 2f;
        float secondSpeed = 1f;
        int frame = ((int) (tick * firstSpeed) <= dissapateFrame) ? (int) (tick * firstSpeed) : (int) (dissapateFrame + (tick - dissapateFrame / firstSpeed) * secondSpeed);
        if (frame > BURST_FRAME_COUNT) {
            frame = BURST_FRAME_COUNT;
        }
        float minU = 0.0625f * frame;
        float maxU = minU + 0.0625f;
        float minV = 0.5f;
        float maxV = minV + 0.5f;
        float offset = 0.219f * (frame % 2);
        float opacity = (tick < 8) ? 0.8f : 0.4f;
        drawVertex(matrix4f, matrix3f, builder, -BURST_RADIUS + offset, -BURST_RADIUS + offset, 0, minU, minV, opacity, packedLightIn);
        drawVertex(matrix4f, matrix3f, builder, -BURST_RADIUS + offset, BURST_RADIUS + offset, 0, minU, maxV, opacity, packedLightIn);
        drawVertex(matrix4f, matrix3f, builder, BURST_RADIUS + offset, BURST_RADIUS + offset, 0, maxU, maxV, opacity, packedLightIn);
        drawVertex(matrix4f, matrix3f, builder, BURST_RADIUS + offset, -BURST_RADIUS + offset, 0, maxU, minV, opacity, packedLightIn);
    }

    public static void drawVertex(Matrix4f matrix, Matrix3f normals, VertexConsumer vertexBuilder, float offsetX, float offsetY, float offsetZ, float textureX, float textureY, float alpha, int packedLightIn) {
        VertexConsumer vertex = vertexBuilder.addVertex(matrix, offsetX, offsetY, offsetZ).setColor(1, 1, 1, 1 * alpha).setUv(textureX, textureY).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLightIn);
        ClientUtils.transformNormals(vertex, normals, 1, 0, 1);
    }
}
