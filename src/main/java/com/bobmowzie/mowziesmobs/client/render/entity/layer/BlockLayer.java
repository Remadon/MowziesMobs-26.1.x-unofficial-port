package com.bobmowzie.mowziesmobs.client.render.entity.layer;

import com.bobmowzie.mowziesmobs.client.model.tools.BlockModelRenderer;
import com.ilexiconn.llibrary.client.model.tools.AdvancedModelRenderer;
import com.ilexiconn.llibrary.client.model.tools.BasicModelRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;

/**
 * PORTING NOTE (26.1.2): this was never actually wired up via {@code addLayer(...)} - the only real call site
 * ({@code RenderBoulder#render}) always called {@link #processModelRenderer} directly as a static utility, so the
 * old {@code extends RenderLayer<T, M>} inheritance (with an unused {@code M} type parameter - the method body never
 * touched {@code getParentModel()}) was dead weight even before this port. It has been dropped entirely rather than
 * chasing the new vanilla bound, since a {@code RenderLayer<S extends EntityRenderState, M extends EntityModel<? super
 * S>>} could never be satisfied here anyway (there is no real {@code EntityModel} involved - {@code AdvancedModelRenderer}
 * is an LLibrary bone tree, see PORTING_NOTES.md's "BasicModelBase/AdvancedModelBase can no longer extend vanilla
 * EntityModel<T>" section for the general reason).
 * <p>
 * Block-rendering API changed shape too: {@code net.minecraft.client.renderer.block.BlockRenderDispatcher} (and its
 * {@code renderSingleBlock(BlockState, PoseStack, MultiBufferSource, int, int)} method) no longer exist at all in
 * 26.1.2 - confirmed by grepping the whole decompiled vanilla tree for both names with zero hits. The replacement is
 * {@link BlockModelResolver#update(BlockModelRenderState, net.minecraft.world.level.block.state.BlockState,
 * BlockDisplayContext)} (obtainable via {@code Minecraft.getInstance().getBlockModelResolver()}) to fill a
 * {@link BlockModelRenderState}, then {@code BlockModelRenderState#submit(PoseStack, SubmitNodeCollector, int, int,
 * int)} to actually draw it - no {@code MultiBufferSource}/{@code VertexConsumer} handling needed, it resolves its
 * own render type(s) internally. This matches the pattern GeckoLib 5's own {@code BlockAndItemGeoLayer} uses
 * internally (see PORTING_NOTES.md "Real com.geckolib.renderer.layer.builtin.BlockAndItemGeoLayer" section) and the
 * one already applied to this package's {@code GeckoBlockLayer.java} (already-ported sibling, different scope).
 * The unused {@code red/green/blue/alpha} tint parameters from the old signature (never referenced in the method
 * body - dead even pre-port) were dropped rather than carried forward as more dead parameters.
 * <p>
 * <b>Cross-scope note for whoever finishes {@code RenderBoulder.java}</b> (still on the pre-port {@code
 * EntityRenderer<EntityBoulderBase>}/{@code render(...)} override as of this writing, out of this agent's scope):
 * the call site needs updating from {@code BlockLayer.processModelRenderer(root, matrixStackIn, bufferIn,
 * packedLightIn, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1, blockrendererdispatcher)} to this method's new signature,
 * called from inside a {@code renderTasks.submitCustomGeometry(...)}-free context since block render states submit
 * directly to a {@code SubmitNodeCollector} (no lambda/{@code VertexConsumer} needed at all for this part).
 */
public class BlockLayer {
    public static void processModelRenderer(AdvancedModelRenderer modelRenderer, PoseStack matrixStackIn, SubmitNodeCollector renderTasks, int packedLightIn, int packedOverlayIn, BlockModelResolver blockModelResolver) {
        if (modelRenderer.showModel) {
            if (modelRenderer instanceof BlockModelRenderer || !modelRenderer.childModels.isEmpty()) {
                matrixStackIn.pushPose();

                modelRenderer.translateRotate(matrixStackIn);
                if (!modelRenderer.isHidden() && modelRenderer instanceof BlockModelRenderer blockModelRendererBox) {
                    BlockModelRenderState renderState = new BlockModelRenderState();
                    blockModelResolver.update(renderState, blockModelRendererBox.getBlockState(), BlockDisplayContext.create());
                    renderState.submit(matrixStackIn, renderTasks, packedLightIn, packedOverlayIn, 0);
                }

                // Render children
                for (BasicModelRenderer child : modelRenderer.childModels) {
                    if (child instanceof AdvancedModelRenderer) {
                        processModelRenderer((AdvancedModelRenderer) child, matrixStackIn, renderTasks, packedLightIn, packedOverlayIn, blockModelResolver);
                    }
                }

                matrixStackIn.popPose();
            }
        }
    }
}
