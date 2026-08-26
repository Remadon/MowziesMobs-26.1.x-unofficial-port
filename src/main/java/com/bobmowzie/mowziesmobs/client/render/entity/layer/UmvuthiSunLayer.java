package com.bobmowzie.mowziesmobs.client.render.entity.layer;

import com.bobmowzie.mowziesmobs.client.render.entity.RenderUmvuthi;
import com.bobmowzie.mowziesmobs.server.entity.umvuthana.EntityUmvuthi;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.ilexiconn.llibrary.client.util.ClientUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.function.BiConsumer;

/**
 * PORTING NOTE (GeckoLib 4 -> 5 full port): {@code renderForBone} (an old {@code GeoRenderLayer} per-bone override
 * point, automatically called by the framework for every bone with the PoseStack positioned there) has no direct
 * equivalent - reimplemented via {@code addPerBoneRender}/{@code PerBoneRender}, matching
 * {@link UmvuthanaSunLayer}/{@link GeckoBlockLayer}/{@link GeckoItemlayer} (see their javadocs). Live-entity data
 * needed at render time ({@code shouldRenderSun()}, active-ability supernova blend progress) is captured into the
 * render state via DataTickets in {@code addRenderData}, since {@code PerBoneRender} callbacks only have
 * {@code RenderPassInfo}/{@code GeoBone} access, no live entity.
 */
public class UmvuthiSunLayer<R extends GeoRenderState> extends GeoRenderLayer<EntityUmvuthi, Void, R> {
    private static final DataTicket<Boolean> SHOULD_RENDER_SUN = DataTicket.create("mowziesmobs_umvuthi_should_render_sun", Boolean.class);
    private static final DataTicket<Float> SCALE_MULT = DataTicket.create("mowziesmobs_umvuthi_sun_scale_mult", Float.class);

    private final Vec3 v1 = new Vec3(-2,1,-1);
    private final Vec3 v2 = new Vec3(0,1,-1);
    private final Vec3 v3 = new Vec3(-1,0,1);
    private final Vec3 v4 = new Vec3(-1,0,-3);
    private final Vec3 v5 = new Vec3(-3,-1,0);
    private final Vec3 v6 = new Vec3(-3,-1,-2);
    private final Vec3 v7 = new Vec3(1,-1,0);
    private final Vec3 v8 = new Vec3(1,-1,-2);
    private final Vec3 v9 = new Vec3(0,-3,-1);
    private final Vec3 v10 = new Vec3(-2,-3,-1);
    private final Vec3 v11 = new Vec3(-1,-2,1);
    private final Vec3 v12 = new Vec3(-1,-2,-3);
    private final Vec3[] POS = {
            // Face 1
            v1,
            v2,
            v3,
            v1,
            // Face 2
            v1,
            v2,
            v4,
            v1,
            // Face 3
            v1,
            v5,
            v6,
            v1,
            // Face 4
            v2,
            v7,
            v8,
            v2,
            // Face 5
            v2,
            v8,
            v4,
            v2,
            // Face 6
            v1,
            v4,
            v6,
            v1,
            // Face 7
            v1,
            v5,
            v3,
            v1,
            // Face 8
            v2,
            v3,
            v7,
            v2,
            // Face 9
            v9,
            v7,
            v8,
            v9,
            // Face 10
            v5,
            v6,
            v10,
            v5,
            // Face 11
            v9,
            v10,
            v11,
            v9,
            // Face 12
            v9,
            v10,
            v12,
            v9,
            // Face 13
            v4,
            v6,
            v12,
            v4,
            // Face 14
            v4,
            v8,
            v12,
            v4,
            // Face 15
            v3,
            v5,
            v11,
            v3,
            // Face 16
            v3,
            v7,
            v11,
            v3,
            // Face 17
            v5,
            v10,
            v11,
            v5,
            // Face 18
            v7,
            v9,
            v11,
            v7,
            // Face 19
            v8,
            v9,
            v12,
            v8,
            // Face 20
            v6,
            v10,
            v12,
            v6,
    };

    public UmvuthiSunLayer(GeoRenderer<EntityUmvuthi, Void, R> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void addRenderData(EntityUmvuthi umvuthi, @Nullable Void relatedObject, R renderState, float partialTick) {
        renderState.addGeckolibData(SHOULD_RENDER_SUN, umvuthi.shouldRenderSun());

        float scaleMult = 1f;
        if (umvuthi.getActiveAbilityType() == EntityUmvuthi.SUPERNOVA_ABILITY && umvuthi.getActiveAbility().getTicksInUse() > 90) {
            scaleMult = (umvuthi.getActiveAbility().getTicksInUse() + partialTick - 90f) / 10f;
            scaleMult = Mth.clamp(scaleMult, 0f, 1f);
        }
        renderState.addGeckolibData(SCALE_MULT, scaleMult);
    }

    @Override
    public void addPerBoneRender(RenderPassInfo<R> renderPassInfo, BiConsumer<GeoBone, PerBoneRender<R>> consumer) {
        if (!Boolean.TRUE.equals(renderPassInfo.getGeckolibData(SHOULD_RENDER_SUN))) return;

        renderPassInfo.model().getBone("sun_render").ifPresent(bone -> consumer.accept(bone, this::renderSunAtBone));
    }

    private void renderSunAtBone(RenderPassInfo<R> renderPassInfo, GeoBone bone, SubmitNodeCollector renderTasks) {
        if (bone.frameSnapshot != null && bone.frameSnapshot.isHidden()) return;
        if (!"sun_render".equals(bone.name())) return;

        PoseStack poseStack = renderPassInfo.poseStack();
        poseStack.pushPose();
        poseStack.translate(0.06d, 0d, -0.0d);
        poseStack.scale(0.06f, 0.06f, 0.06f);

        RenderType renderType = RenderTypes.entityTranslucent(RenderUmvuthi.SUN, true);
        int packedLight = renderPassInfo.packedLight();
        float time = currentTime(renderPassInfo);
        float scaleMult = renderPassInfo.getOrDefaultGeckolibData(SCALE_MULT, 1f);

        renderTasks.submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) -> {
            Matrix4f matrix4f = new Matrix4f(pose.pose());
            Matrix3f matrix3f = new Matrix3f(pose.normal());
            drawSun(matrix4f, matrix3f, vertexConsumer, time, scaleMult);
        });

        poseStack.popPose();
    }

    private float currentTime(RenderPassInfo<R> renderPassInfo) {
        return (float) renderPassInfo.renderState().getAnimatableAge();
    }

    private void drawSun(Matrix4f matrix4f, Matrix3f matrix3f, VertexConsumer builder, float time, float scaleMultiplier) {
        float scale = (0.9f + (float) Math.sin(time * 4) * 0.07f) * scaleMultiplier;
        for(int i = 0; i < 4; i++) {
            for (Vec3 vec : POS) {
                vec = vec.multiply(1f + (scale * i), 1f + (scale * i), 1f + (scale * i));
                VertexConsumer consumer = builder.addVertex(matrix4f, (float) vec.x + (scale * i), (float) vec.y + (scale * i), (float) vec.z + (scale * i))
                        .setColor(1f, 1f, .4f, 0.2f)
                        .setUv(0.0f, 0.5f)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(15728880);
                ClientUtils.transformNormals(consumer, matrix3f, 1, 1, 1);
            }
        }
        for (Vec3 vec : POS) {
            VertexConsumer consumer = builder.addVertex(matrix4f, (float) vec.x * 1.2f * scaleMultiplier, (float) vec.y * 1.2f * scaleMultiplier, (float) vec.z * 1.2f * scaleMultiplier)
                    .setColor(1f, 1f, 1f, 1f)
                    .setUv(0.0f, 0.5f)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(15728880);
            ClientUtils.transformNormals(consumer, matrix3f, 1, 1, 1);
        }
    }
}
