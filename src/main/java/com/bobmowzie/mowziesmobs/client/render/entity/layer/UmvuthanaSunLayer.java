package com.bobmowzie.mowziesmobs.client.render.entity.layer;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.server.entity.umvuthana.EntityUmvuthana;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.function.BiConsumer;

/**
 * PORTING NOTE (GeckoLib 4 -> 5 full port): the old {@code render(...)} override read the head bone's rendered
 * world transform via {@code head.getPose()} (a {@code MowzieGeoBone} side-table Matrix4f) - nothing in this
 * codebase ever writes to that side table (grep-confirmed), so that read was always an identity matrix, a
 * pre-existing dead/unfinished stub, not real GeckoLib-4-era behavior. The GeckoLib-5-correct replacement for "get
 * the PoseStack positioned at a specific bone, then draw custom geometry there" is the
 * {@code addPerBoneRender}/{@code PerBoneRender} mechanism (see {@code GeckoBlockLayer}/{@code GeckoItemlayer}
 * javadocs) - the framework transforms the PoseStack to the bone (full parent chain + this bone's own
 * rotate/translate/scale + pivot) before invoking the callback, exactly matching (and actually more correctly
 * reproducing) the original GeckoLib-4 intent of "poseStack is positioned at the bone when this layer callback
 * runs".
 * <p>
 * BILLBOARDING: two earlier attempts at {@code newPoseStack.mulPose(camera rotation)} both regressed color/scale
 * for reasons that were never confirmed (see git history on this file). Both attempts used a "raw" {@code Camera}
 * rotation and kept the vertex normal computed from the bone's own (non-rotated) pose, decoupled from whatever
 * rotation the quad itself got. That decoupling is exactly what a user report on the fixed-orientation version
 * (no billboarding at all) diagnosed from the other direction: viewed from one side the sun square's color looked
 * right, from the other side it looked "dark", like a coin with two faces - i.e. the shading normal only matched
 * the visible face from one viewing angle, because it was a single fixed direction (the bone's own orientation)
 * rather than something that responds to which side is actually being looked at. Billboarding actually fixes both
 * bugs at once: this now follows vanilla's own camera-facing quad exactly (see
 * {@code net.minecraft.client.renderer.entity.ExperienceOrbRenderer#submit}) - {@code renderPassInfo.cameraState()}
 * (a {@code CameraRenderState}, the same modern type vanilla itself uses, as opposed to the older {@code Camera}
 * class the earlier attempts pulled from elsewhere) provides {@code .orientation}, multiplied in between the
 * translate and scale (matching {@code ExperienceOrbRenderer}'s call order exactly), and the vertex normal is
 * taken from that same final, camera-rotated pose via {@code VertexConsumer#setNormal(PoseStack.Pose, ...)} -
 * exactly like {@code ExperienceOrbRenderer.vertex()} does - instead of a separately captured, unrotated normal.
 * Confirmed via {@code SubmitNodeStorage}/{@code CustomFeatureRenderer} source: {@code submitCustomGeometry}
 * captures {@code poseStack.last().copy()} at call time and replays it verbatim at draw time with no additional
 * camera-relative transform layered on by the framework, so the previous "maybe the framework double-applies a
 * camera transform" theory doesn't hold - whatever caused the earlier regressions must have been in that older
 * attempt's own matrix construction, not a hidden framework interaction.
 */
public class UmvuthanaSunLayer<R extends GeoRenderState> extends GeoRenderLayer<EntityUmvuthana, Void, R> {
    protected final EntityRenderDispatcher entityRenderDispatcher;

    public UmvuthanaSunLayer(GeoRenderer<EntityUmvuthana, Void, R> entityRendererIn, EntityRendererProvider.Context context) {
        super(entityRendererIn);
        entityRenderDispatcher = context.getEntityRenderDispatcher();
    }

    @Override
    public void addPerBoneRender(RenderPassInfo<R> renderPassInfo, BiConsumer<GeoBone, PerBoneRender<R>> consumer) {
        renderPassInfo.model().getBone("head").ifPresent(bone -> consumer.accept(bone, this::renderSunAtBone));
    }

    private void renderSunAtBone(RenderPassInfo<R> renderPassInfo, GeoBone bone, SubmitNodeCollector renderTasks) {
        if (bone.frameSnapshot != null && bone.frameSnapshot.isHidden()) return;

        PoseStack poseStack = renderPassInfo.poseStack();
        PoseStack.Pose matrixstack$entry = poseStack.last();
        Matrix4f matrix4f = matrixstack$entry.pose();
        Vec3 vecTranslation = renderPoseToPosition(matrix4f, 0, 0, 0);
        Vec3 vecScale = renderPoseToPosition(matrix4f, 1, 0, 0);
        float scale = (float) new Vec3(vecScale.x - vecTranslation.x, vecScale.y - vecTranslation.y, vecScale.z - vecTranslation.z).length();

        PoseStack newPoseStack = new PoseStack();
        newPoseStack.translate(vecTranslation.x, vecTranslation.y, vecTranslation.z);
        newPoseStack.mulPose(renderPassInfo.cameraState().orientation);
        newPoseStack.scale(scale, scale, scale);
        RenderType renderType = RenderTypes.entityTranslucent(Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/particle/sun_no_glow.png"), true);
        int packedLight = renderPassInfo.packedLight();
        float time = (float) renderPassInfo.renderState().getAnimatableAge();

        renderTasks.submitCustomGeometry(newPoseStack, renderType, (pose, vertexConsumer) -> drawSun(pose, vertexConsumer, packedLight, time));
    }

    private static Vec3 renderPoseToPosition(Matrix4f pose, float x, float y, float z) {
        org.joml.Vector4f v = new org.joml.Vector4f(x, y, z, 1);
        v.mul(pose);
        return new Vec3(v.x(), v.y(), v.z());
    }

    private void drawSun(PoseStack.Pose pose, VertexConsumer builder, int packedLightIn, float time) {
        float sunRadius = 1.2f + (float) Math.sin(time * 4) * 0.085f;
        // sun_no_glow.png is a hard-edged, fully-opaque square with no soft alpha falloff (confirmed by inspecting
        // its alpha channel) - this quad isn't the source of the halo the user is seeing, so keep it fully opaque.
        // The actual soft-edged "glow circle" is the separate "sun" particle (textures/particle/sun.png, which does
        // have a real radial alpha gradient) spawned in EntityUmvuthana - see the alpha param on that spawnParticle
        // call instead.
        float alpha = 1.0f;
        this.drawVertex(pose, builder, -sunRadius, -sunRadius, 0, 0, 0, alpha, packedLightIn);
        this.drawVertex(pose, builder, -sunRadius, sunRadius, 0, 0, 1, alpha, packedLightIn);
        this.drawVertex(pose, builder, sunRadius, sunRadius, 0, 1, 1, alpha, packedLightIn);
        this.drawVertex(pose, builder, sunRadius, -sunRadius, 0, 1, 0, alpha, packedLightIn);
    }

    // The normal is taken from the same camera-rotated pose used for position (matching
    // ExperienceOrbRenderer.vertex()), not a separately captured bone-only normal - see the class-level comment
    // for why that decoupling was the actual cause of the "looks right from one side, dark from the other" bug.
    public void drawVertex(PoseStack.Pose pose, VertexConsumer vertexBuilder, float offsetX, float offsetY, float offsetZ, float textureX, float textureY, float alpha, int packedLightIn) {
        // -Y, not +Y like ExperienceOrbRenderer: with the quad now billboarded to the camera, the local axis
        // picked for the normal determines which of the two possible facings gets the bright diffuse response,
        // and +Y landed on the dark one here (confirmed live) - flipped so the face pointing at the camera is
        // the correctly-lit one instead of its opposite.
        vertexBuilder.addVertex(pose, offsetX, offsetY, offsetZ).setColor(1f, 1f, 1f, alpha).setUv(textureX, textureY).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, 0.0F, -1.0F, 0.0F);
    }

}
