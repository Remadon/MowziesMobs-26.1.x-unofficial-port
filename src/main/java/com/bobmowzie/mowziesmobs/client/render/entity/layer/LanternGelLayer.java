package com.bobmowzie.mowziesmobs.client.render.entity.layer;

import com.bobmowzie.mowziesmobs.client.model.entity.ModelLantern;
import com.bobmowzie.mowziesmobs.server.entity.lantern.EntityLantern;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * PORTING NOTE (26.1.2): dropped {@code extends RenderLayer<T, ModelLantern<T>>} - can no longer be satisfied.
 * {@link ModelLantern} extends {@code MowzieEntityModel} -> LLibrary's {@code AdvancedModelBase}, which no longer
 * extends vanilla {@code EntityModel} at all (see PORTING_NOTES.md "BasicModelBase/AdvancedModelBase can no longer
 * extend vanilla EntityModel<T>" - a hard architectural fact: {@code Model#renderToBuffer} is {@code final} on the
 * vanilla base and {@code EntityModel<T>}'s bound requires a render state, not a live entity, so LLibrary's
 * live-entity-driven {@code setupAnim} API could not be kept while also satisfying vanilla's class hierarchy).
 * Consequently vanilla's new {@code RenderLayer<S extends EntityRenderState, M extends EntityModel<? super S>>}
 * bound can never be satisfied by a {@code ModelLantern} either, mirroring the same conclusion already reached for
 * the top-level LLibrary-model renderers (see PORTING_NOTES.md's "MobRenderer-based ones using LLibrary models"
 * section) - this is now a plain helper class with an explicit {@code render(...)} method, matching the
 * {@code BlockLayer}/{@code ItemLayer} sibling files' identical fix in this same package.
 * <p>
 * Also dropped: {@code EntityModel#copyPropertiesTo(EntityModel)} (a vanilla method that no longer exists on
 * anything in this call chain - {@code ModelLantern} was never really a vanilla {@code EntityModel} member in the
 * sense that method assumes). It only ever copied a couple of bookkeeping fields (e.g. attack/swim timers) that
 * {@code MowzieEntityModel#setupAnim}'s own {@code MMModelAnimator}-driven body never reads - the gel-layer model
 * gets its pose entirely from its own {@code setupAnim(entity, ...)} call below (LLibrary models are driven
 * directly off a live entity + swing params, not by copying state from a sibling model instance), so dropping the
 * copy step changes nothing observable.
 * <p>
 * {@code getTextureLocation(T)} is no longer a method on the new {@code EntityRenderer} base at all (see
 * PORTING_NOTES.md), so the texture is now an explicit parameter supplied by the caller.
 * <p>
 * <b>Cross-scope note</b>: {@code RenderLantern.java} (out of this agent's scope) is still on the old
 * {@code MobRenderer<EntityLantern, ModelLantern<EntityLantern>>}/{@code addLayer(new LanternGelLayer<>(this))}
 * shape as of this writing. Per the same architectural fact above, it cannot keep being a {@code MobRenderer}/
 * {@code LivingEntityRenderer} either (no {@code M} can satisfy the new bound) - once it's ported to a plain
 * {@code EntityRenderer<EntityLantern, XRenderState>} (the established pattern for LLibrary-model renderers), this
 * class's {@link #render} should be called directly from that renderer's own {@code submit(...)}, wrapped in
 * {@code renderTasks.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(texture), (pose, vertexConsumer)
 * -> gelLayer.render(...))} the same way the sibling LLibrary-model renderers are documented to do.
 */
public class LanternGelLayer<T extends EntityLantern> {
    private final ModelLantern<T> model = new ModelLantern<>(true);

    public void render(PoseStack matrixStackIn, VertexConsumer vertexConsumer, int packedLightIn, T entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!entitylivingbaseIn.isInvisible()) {
            model.setupAnim(entitylivingbaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            model.renderToBuffer(matrixStackIn, vertexConsumer, packedLightIn, OverlayTexture.NO_OVERLAY, -1);
        }
    }

    /** Convenience matching the old {@code getTextureLocation()} call site - build the {@code RenderType} to pass to {@code submitCustomGeometry}. */
    public static RenderType renderType(Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }
}
