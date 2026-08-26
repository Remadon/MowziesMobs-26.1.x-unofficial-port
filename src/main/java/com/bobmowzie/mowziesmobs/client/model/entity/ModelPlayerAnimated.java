package com.bobmowzie.mowziesmobs.client.model.entity;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * PORTING NOTE (1.21.1 -> 26.1.2): this class's ORIGINAL job was to swap vanilla {@code PlayerModel}'s layer parts
 * ({@code jacket}/{@code leftSleeve}/{@code rightSleeve}/{@code leftPants}/{@code rightPants}) for
 * {@code ModelPartMatrix} stand-ins whose world transform was slaved to a GeckoLib-animated pose each frame
 * ({@code copyFromGeckoModel}), so that vanilla's own decorative render layers (armor, cape, elytra, etc, which all
 * read pose data off a shared {@code PlayerModel} instance) would visually follow the GeckoLib third-person player
 * animation used during abilities.
 * <p>
 * That mechanism is no longer possible AND no longer needed - see the architecture writeup in
 * {@code client/render/entity/player/GeckoRenderPlayer.java} (this mod's actual redesign) and the "GeckoRenderPlayer
 * / third-person ability rendering redesign" section of PORTING_NOTES.md for the full reasoning. Summary:
 * <ul>
 *   <li>NOT POSSIBLE: {@code PlayerModel}'s layer-part fields are now {@code public final ModelPart} (confirmed by
 *       reading {@code net/minecraft/client/model/player/PlayerModel.java} in the real 26.1.2 source) - they can no
 *       longer be reassigned to a custom {@code ModelPartMatrix} subclass. GeckoLib 5 also no longer exposes a
 *       bone's world transform matrix outside of its own live render traversal in any form richer than a position
 *       vector (confirmed by reading the real GeckoLib 5.5.2 source - {@code RenderPassInfo.BonePositionListener}
 *       is position-only, and {@code GeoBone#frameSnapshot} is nulled again immediately after that traversal), so
 *       there is no longer a reliable data source to slave a stand-in model to even if the fields were mutable.</li>
 *   <li>NOT NEEDED: {@code GeckoRenderPlayer} no longer tries to make vanilla's {@code PlayerModel} imitate the
 *       GeckoLib pose at all. Instead, during an active ability, it renders the dedicated third-person GeckoLib rig
 *       ({@code ModelGeckoPlayerThirdPerson}, which already includes its own body/clothing-layer geometry, e.g. the
 *       {@code BodyLayer} bone) directly in place of vanilla's body, via GeckoLib's own {@code GeoObjectRenderer}
 *       pipeline. Vanilla's {@code AvatarRenderer}/{@code PlayerModel} plumbing is left completely untouched.</li>
 * </ul>
 * Consequently this class no longer needs any custom behaviour - it is kept as a plain, fully-functional
 * {@code PlayerModel} subclass (not a stub) purely so the old call sites' TYPE still resolves if something else ever
 * wants a distinctly-typed "animated" player model; nothing in this mod constructs one any more today (grepped the
 * whole {@code src/main/java} tree to confirm - the only remaining reference was this file itself and the old,
 * removed {@code GeckoRenderPlayer} constructor call). The {@code ModelPartMatrix}-based static helpers
 * ({@code copyPropertiesTo}, {@code setUseMatrixMode}, {@code copyFromGeckoModel}) have been removed rather than
 * stubbed, since nothing calls them any more and their whole premise (swappable layer-part references) no longer
 * exists on {@code PlayerModel}.
 */
public class ModelPlayerAnimated extends PlayerModel {

    public ModelPlayerAnimated(ModelPart root, boolean smallArmsIn) {
        super(root, smallArmsIn);
    }

    @Override
    public void setupAnim(AvatarRenderState state) {
        super.setupAnim(state);
    }
}
