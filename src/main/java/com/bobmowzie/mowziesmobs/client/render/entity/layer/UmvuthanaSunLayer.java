package com.bobmowzie.mowziesmobs.client.render.entity.layer;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.server.entity.umvuthana.EntityUmvuthana;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.ilexiconn.llibrary.client.util.ClientUtils;
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
import org.joml.Matrix3f;
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
 * runs". The math inside {@code drawSun}/{@code drawVertex} is unchanged from the original.
 * <p>
 * BILLBOARDING - open problem, two failed attempts so far:
 * <p>
 * 1) An earlier pass added {@code newPoseStack.mulPose(camera.rotation())} so the quad would always face the camera,
 * but ALSO switched the lighting normal to track that same camera-relative pose - so the {@code PER_FACE_LIGHTING}
 * shading term (from {@code RenderTypes.entityTranslucent}'s pipeline) constantly changed as the camera moved,
 * causing every follow-up bug in this class (too dark, briefly invisible, still faintly too dark, wrong color).
 * <p>
 * 2) Reading the 1.20 source literally, it builds {@code newPoseStack} from scratch ({@code new PoseStack()}) with
 * only translate+scale, no rotation call at all - so billboarding was removed here entirely on the assumption the
 * original simply didn't billboard. That got color and scale confirmed correct against the 1.21.1 reference client,
 * but the reference client's sun square *does* always face the camera (confirmed by live comparison) and this
 * fixed-orientation version doesn't - so re-added {@code newPoseStack.mulPose(camera.rotation())} for position only,
 * this time leaving the lighting normal on the separate, non-rotated {@code matrixstack$entry.normal()} (computed
 * *before* {@code newPoseStack} even exists) specifically to rule out (1)'s mistake. CONFIRMED that wasn't enough:
 * live testing reproduced the exact same "too large and off color" regression anyway, even with the normal
 * genuinely decoupled from the rotation this time - meaning a shifting normal was never the whole story, and (since
 * a pure rotation can't change apparent size on its own) something about this specific
 * {@code submitCustomGeometry} call plus a manually-applied camera rotation isn't understood yet - possibly the
 * framework already composes its own camera-relative transform onto whatever poseStack gets passed in, and the
 * manual {@code mulPose} was compounding with that rather than being the only rotation applied. Not confirmed.
 * <p>
 * Currently reverted to the fixed-orientation version (2, no billboarding) since that's the only state confirmed to
 * have correct color/scale - the "always faces camera" behavior remains unsolved.
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
        newPoseStack.scale(scale, scale, scale);
        // BILLBOARD ATTEMPT REVERTED AGAIN: adding newPoseStack.mulPose(camera.rotation()) here reproduced the exact
        // same "too large and off color" regression as before, even with the lighting normal below fully decoupled
        // from newPoseStack (it reads matrixstack$entry.normal(), the ORIGINAL non-rotated poseStack, computed
        // before newPoseStack even exists) - so the earlier theory that a shifting normal alone explained the color
        // regression was wrong, or at least incomplete, and a pure rotation shouldn't affect size at all. Something
        // about this specific submitCustomGeometry call reacting to a manually-applied camera rotation isn't
        // understood yet (possibly the framework already composes its own camera-relative transform on top of
        // whatever poseStack is passed in here, and the manual mulPose was compounding with that rather than being
        // the only rotation applied) - not confirmed. Reverting to the last state confirmed correct (color and
        // scale both right) rather than guess a third time; the "always faces camera" behavior stays unresolved.
        RenderType renderType = RenderTypes.entityTranslucent(Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/particle/sun_no_glow.png"), true);
        int packedLight = renderPassInfo.packedLight();
        float time = (float) renderPassInfo.renderState().getAnimatableAge();
        Matrix3f boneNormal = new Matrix3f(matrixstack$entry.normal());

        renderTasks.submitCustomGeometry(newPoseStack, renderType, (pose, vertexConsumer) -> {
            Matrix4f matrix4f2 = new Matrix4f(pose.pose());
            drawSun(matrix4f2, boneNormal, vertexConsumer, packedLight, time);
        });
    }

    private static Vec3 renderPoseToPosition(Matrix4f pose, float x, float y, float z) {
        org.joml.Vector4f v = new org.joml.Vector4f(x, y, z, 1);
        v.mul(pose);
        return new Vec3(v.x(), v.y(), v.z());
    }

    private void drawSun(Matrix4f matrix4f, Matrix3f matrix3f, VertexConsumer builder, int packedLightIn, float time) {
        float sunRadius = 1.2f + (float) Math.sin(time * 4) * 0.085f;
        // sun_no_glow.png is a hard-edged, fully-opaque square with no soft alpha falloff (confirmed by inspecting
        // its alpha channel) - this quad isn't the source of the halo the user is seeing, so keep it fully opaque.
        // The actual soft-edged "glow circle" is the separate "sun" particle (textures/particle/sun.png, which does
        // have a real radial alpha gradient) spawned in EntityUmvuthana - see the alpha param on that spawnParticle
        // call instead.
        float alpha = 1.0f;
        this.drawVertex(matrix4f, matrix3f, builder, -sunRadius, -sunRadius, 0, 0, 0, alpha, packedLightIn);
        this.drawVertex(matrix4f, matrix3f, builder, -sunRadius, sunRadius, 0, 0, 1, alpha, packedLightIn);
        this.drawVertex(matrix4f, matrix3f, builder, sunRadius, sunRadius, 0, 1, 1, alpha, packedLightIn);
        this.drawVertex(matrix4f, matrix3f, builder, sunRadius, -sunRadius, 0, 1, 0, alpha, packedLightIn);
    }

    public void drawVertex(Matrix4f matrix, Matrix3f normals, VertexConsumer vertexBuilder, float offsetX, float offsetY, float offsetZ, float textureX, float textureY, float alpha, int packedLightIn) {
        VertexConsumer consumer = vertexBuilder.addVertex(matrix, offsetX, offsetY, offsetZ).setColor(1f, 1f, 1f, alpha).setUv(textureX, textureY).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880);
        ClientUtils.transformNormals(consumer, normals, 1, 1, 1);
    }

}
