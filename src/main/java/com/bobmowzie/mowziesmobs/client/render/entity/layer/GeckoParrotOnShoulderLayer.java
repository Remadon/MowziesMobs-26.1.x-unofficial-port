package com.bobmowzie.mowziesmobs.client.render.entity.layer;

import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ParrotOnShoulderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * PORTING NOTE (26.1.2): same reasoning as {@link GeckoCapeLayer} in this package (read that file's javadoc for the
 * full writeup) - reduced to a trivial passthrough. Vanilla's {@code ParrotOnShoulderLayer} is no longer generic in
 * the entity type either (it's now {@code RenderLayer<AvatarRenderState, PlayerModel>} directly, not {@code
 * ParrotOnShoulderLayer<AbstractClientPlayer>}), so the old {@code implements IGeckoRenderLayer} (a vestigial,
 * GeckoLib-4-era per-bone-render-override marker interface - see {@code IGeckoRenderLayer.java}'s own porting note;
 * its {@code renderRecursively} hook is never called by anything any more) was dropped along with the {@code
 * render(...)} override it existed to support.
 * <p>
 * <b>Currently unused / orphaned</b>: see {@link GeckoCapeLayer}'s javadoc - identical situation. The old
 * constructor took a literal {@code GeckoRenderPlayer} (the pre-redesign {@code GeckoRenderPlayer} extended
 * {@code PlayerRenderer}, a valid {@code RenderLayerParent}); the redesigned {@code GeckoRenderPlayer} (see
 * PORTING_NOTES.md "player-render agent" section) is a standalone {@code GeoObjectRenderer<GeckoPlayer, Void,
 * GeoRenderState>} with no {@code addLayer}/{@code layers} mechanism at all and does not implement {@code
 * RenderLayerParent} - so a literal {@code GeckoRenderPlayer} parameter type would not even type-check any more
 * regardless of anything else. Changed to the generic {@code RenderLayerParent<AvatarRenderState, PlayerModel>}
 * (matching vanilla's own constructor shape, and this package's other now-orphaned {@code Gecko*Layer} siblings)
 * so the class is at least constructible against the real vanilla {@code AvatarRenderer<AbstractClientPlayer>} -
 * nothing currently does so (grep-confirmed no live call site), matching {@code GeckoRenderPlayer}'s own javadoc,
 * which documents parrot-on-shoulder rendering as one of the vanilla decorative layers intentionally dropped while
 * an ability animation is driving the pose.
 */
public class GeckoParrotOnShoulderLayer extends ParrotOnShoulderLayer {
    public GeckoParrotOnShoulderLayer(RenderLayerParent<AvatarRenderState, PlayerModel> rendererIn, EntityModelSet modelSet) {
        super(rendererIn, modelSet);
    }
}
