package com.bobmowzie.mowziesmobs.client.render.entity.layer;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.context.ContextKey;

/**
 * PORTING NOTE (26.1.2): {@code RenderLayer<S,M>} is now keyed on the render STATE type, not the live entity, and
 * its only method is {@code submit(PoseStack, SubmitNodeCollector, int lightCoords, S state, float yRot, float
 * xRot)} - no live entity access any more (see PORTING_NOTES.md architecture sections). Whether an entity
 * "hasSunblock" is capability data ({@code DataHandler.getData(entity, DataHandler.LIVING_DATA).getHasSunblock()})
 * that needs a live entity to look up, so - following the exact precedent already established in this codebase for
 * the structurally-identical problem in {@code client/render/entity/FrozenRenderHandler.java}'s {@code LayerFrozen}
 * (a sibling layer added to every {@code LivingEntityRenderer} the same way via {@code ClientLayerRegistry}, read
 * that class's javadoc for the full writeup) - it is captured into the render state via NeoForge's generic
 * render-state data mechanism: {@code net.neoforged.neoforge.client.extensions.IRenderStateExtension#getRenderData
 * (ContextKey)}/{@code #setRenderData(ContextKey, Object)}, which {@code EntityRenderState} now implements via its
 * new {@code net.neoforged.neoforge.client.renderstate.BaseRenderState} supertype (verified via {@code javap}
 * against the real NeoForge 26.1.2.95 classes - no sources jar was available, same approach {@code
 * FrozenRenderHandler}'s porting note documents).
 * <p>
 * **KNOWN GAP, matching {@code LayerFrozen}'s identical documented gap**: nothing in this agent's scope currently
 * WRITES {@link #SUNBLOCK_RENDER_DATA_KEY} into any render state. That needs a
 * {@code net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent#registerEntityModifier(...)}
 * listener (a mod-bus event) registered against a broad enough renderer class to cover every {@code
 * LivingEntityRenderer} this layer gets attached to via {@code ClientLayerRegistry} (including the real player
 * renderer) - that registration most naturally lives in a central client event-bus file, out of this single-layer
 * file's scope. Until it exists somewhere, this layer will compile and run but the sunblock glow simply will not
 * show - flagged prominently in the porting report, not silently dropped.
 * <p>
 * Also renamed: {@code net.minecraft.client.renderer.RenderType.energySwirl(...)} -> {@code
 * net.minecraft.client.renderer.rendertype.RenderTypes.energySwirl(...)} (see PORTING_NOTES.md's RenderType/
 * RenderTypes split), and {@code FastColor.ARGB32.colorFromFloat(...)} -> {@code
 * net.minecraft.util.ARGB.colorFromFloat(...)} (also already documented). {@code EntityModel#copyPropertiesTo}/
 * {@code #prepareMobModel} and {@code Model#renderToBuffer} being called directly no longer apply either - the
 * modern idiom for "re-render the parent model with an alt texture/tint" is {@code
 * SubmitNodeCollector#submitModel(Model, S, PoseStack, RenderType, int, int, int, TextureAtlasSprite, int,
 * CrumblingOverlay)}, which internally calls {@code setupAnim(state)} and submits the geometry - this mirrors the
 * base vanilla {@code RenderLayer#renderColoredCutoutModel} helper (same idea, fixed to {@code entityCutout} and no
 * scroll offsets - not reusable here since sunblock needs {@code energySwirl}'s scrolling UV offsets and a
 * non-default tint).
 */
public class SunblockLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
    private static final Identifier SUNBLOCK_ARMOR = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/entity/sunblock_glow.png");

    public static final ContextKey<Boolean> SUNBLOCK_RENDER_DATA_KEY = new ContextKey<>(Identifier.fromNamespaceAndPath(MMCommon.MODID, "sunblock"));

    public SunblockLayer(RenderLayerParent<S, M> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot) {
        if (Boolean.TRUE.equals(state.getRenderData(SUNBLOCK_RENDER_DATA_KEY))) {
            float f = state.ageInTicks;
            int overlayCoords = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
            int tintColor = ARGB.colorFromFloat(1, 1, 1, 0.1f);

            submitNodeCollector.submitModel(getParentModel(), state, poseStack, RenderTypes.energySwirl(getTextureLocation(), xOffset(f), f * 0.01F),
                    lightCoords, overlayCoords, tintColor, null, state.outlineColor, null);
        }
    }

    protected float xOffset(float p_225634_1_) {
        return p_225634_1_ * 0.02F;
    }

    protected Identifier getTextureLocation() {
        return SUNBLOCK_ARMOR;
    }
}
