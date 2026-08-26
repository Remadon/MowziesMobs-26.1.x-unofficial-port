package com.bobmowzie.mowziesmobs.client.render.entity.layer;

import com.bobmowzie.mowziesmobs.client.model.entity.ModelWroughtnaut;
import com.bobmowzie.mowziesmobs.server.entity.wroughtnaut.EntityWroughtnaut;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * PORTING NOTE (26.1.2): same fix as {@link LanternGelLayer} in this package (read that class's javadoc for the
 * full architectural reasoning) - {@link ModelWroughtnaut} is an LLibrary {@code AdvancedModelBase} subclass, which
 * can no longer satisfy vanilla {@code RenderLayer<S extends EntityRenderState, M extends EntityModel<? super S>>}'s
 * bound at all, so this is now a plain helper class with an explicit {@code render(...)} method instead of a real
 * {@code RenderLayer} subclass. Also dropped {@code copyPropertiesTo} (removed from vanilla, and never load-bearing
 * here - see {@code LanternGelLayer}'s javadoc) and {@code getTextureLocation()}/{@code
 * LivingEntityRenderer.getOverlayCoords(LivingEntity, float)} (the latter now takes a {@code LivingEntityRenderState}
 * rather than a live entity - not available in this plain-entity-driven helper, so overlay is now an explicit
 * caller-supplied parameter, same convention as {@code BlockLayer.processModelRenderer}'s {@code packedOverlayIn}).
 * <p>
 * {@code MMRenderType.eyes(...)} (used by the old body) was removed from {@code MMRenderType} by whoever ported
 * that class - grep-confirmed it no longer has an {@code eyes} method. Replacement: vanilla itself now ships an
 * equivalent factory, {@code net.minecraft.client.renderer.rendertype.RenderTypes#eyes(Identifier)} (confirmed by
 * reading the real {@code RenderTypes.java}), used directly below instead.
 * <p>
 * <b>Cross-scope note</b>: {@code RenderWroughtnaut.java} (out of this agent's scope) is still on the old
 * {@code MobRenderer<EntityWroughtnaut, ModelWroughtnaut<EntityWroughtnaut>>}/{@code addLayer(new
 * WroughtnautEyesLayer<>(this))} shape as of this writing and will need the same plain-{@code EntityRenderer}
 * treatment as {@code RenderLantern.java} (see {@code LanternGelLayer}'s javadoc) before it can call {@link #render}
 * from its own {@code submit(...)}.
 */
public class WroughtnautEyesLayer<T extends EntityWroughtnaut> {
    private final ModelWroughtnaut<T> model = new ModelWroughtnaut<>(true);

    public void render(PoseStack matrixStackIn, VertexConsumer vertexConsumer, int packedLightIn, int packedOverlayIn, T entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!entitylivingbaseIn.isInvisible()) {
            model.setupAnim(entitylivingbaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            model.renderToBuffer(matrixStackIn, vertexConsumer, packedLightIn, packedOverlayIn);
        }
    }

    /** Convenience matching the old {@code getTextureLocation()} call site - build the {@code RenderType} to pass to {@code submitCustomGeometry}. */
    public static RenderType renderType(Identifier texture) {
        return RenderTypes.eyes(texture);
    }
}
