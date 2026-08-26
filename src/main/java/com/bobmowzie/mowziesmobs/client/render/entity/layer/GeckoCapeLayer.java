package com.bobmowzie.mowziesmobs.client.render.entity.layer;

import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.model.EquipmentAssetManager;

/**
 * PORTING NOTE (26.1.2): this class's entire reason for existing - conditionally re-transforming the PoseStack to
 * match {@code PlayerModel.body} when it was a {@code ModelPartMatrix} (i.e. when the player renderer was in
 * GeckoLib "matrix mode", swapping vanilla {@code ModelPart}s for GeckoLib-bone-driven stand-ins) before delegating
 * to the real vanilla cape rendering - is gone, for two independent, both-confirmed reasons:
 * <ol>
 *   <li>"Matrix mode" itself no longer exists. {@code ModelPlayerAnimated.java} (see its own porting note and
 *   PORTING_NOTES.md's "client/model/entity/ModelPlayerAnimated.java - simplified rather than genuinely redesigned"
 *   section) dropped {@code setUseMatrixMode}/{@code copyFromGeckoModel} entirely - {@code PlayerModel.body} is now
 *   always a plain {@code ModelPart}, never a {@code ModelPartMatrix}, so the old {@code instanceof ModelPartMatrix}
 *   guard would be permanently, unconditionally false forever - dead code, not a working special case any more.</li>
 *   <li>Vanilla's {@code CapeLayer} itself was rewritten around the render-state-extraction overhaul: it's now
 *   {@code RenderLayer<AvatarRenderState, PlayerModel>} (render-state-keyed, {@code PlayerModel} no longer generic)
 *   with a {@code submit(PoseStack, SubmitNodeCollector, int, AvatarRenderState, float, float)} entry point - no
 *   live entity access at all any more (see PORTING_NOTES.md architecture sections), so even the "old" style of
 *   override this class used (a {@code render(...)} method with 8 raw floats + a live entity) no longer exists to
 *   override.</li>
 * </ol>
 * This is therefore reduced to a trivial passthrough with an updated constructor, matching the identical treatment
 * already applied to {@code GeckoArmorLayer.java} in this package for the same class of reason (read that file's
 * javadoc for the parallel).
 * <p>
 * <b>Currently unused / orphaned</b>: the redesigned {@code GeckoRenderPlayer} (see PORTING_NOTES.md "player-render
 * agent" section) no longer extends any vanilla renderer and has no {@code addLayer}/{@code layers} list at all -
 * it is not a {@code RenderLayerParent} and cannot host this layer any more. Nothing in the codebase currently
 * constructs a {@code GeckoCapeLayer} (grep-confirmed). {@code GeckoRenderPlayer}'s own class javadoc documents this
 * exact category of vanilla decorative-layer rendering (cape included) as intentionally dropped while a GeckoLib
 * ability animation is driving the player's pose, for the same underlying reason (no supported way left to slave a
 * vanilla {@code ModelPart} tree's rotation to a GeckoLib bone pose from outside GeckoLib's own live render pass -
 * see that class's "Why the old approach is dead" section). This class is left in a real, compiling, structurally
 * correct state (constructible against the actual vanilla {@code AvatarRenderer<AbstractClientPlayer>}, where it
 * would behave identically to vanilla's own cape layer, i.e. redundantly) rather than deleted, in case a future
 * redesign of the ability-animation player renderer restores a real GeckoLib-specific integration point - but no
 * such point exists today, and re-adding this to {@code GeckoRenderPlayer} is a design decision outside this
 * agent's file scope (would require editing that finished, dedicated-agent-owned file).
 */
public class GeckoCapeLayer extends CapeLayer {
    public GeckoCapeLayer(RenderLayerParent<AvatarRenderState, PlayerModel> playerModelIn, EntityModelSet modelSet, EquipmentAssetManager equipmentAssets) {
        super(playerModelIn, modelSet, equipmentAssets);
    }
}
