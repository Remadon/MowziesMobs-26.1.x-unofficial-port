package com.bobmowzie.mowziesmobs.client.render.entity.layer;

import com.bobmowzie.mowziesmobs.client.render.MMRenderType;
import com.bobmowzie.mowziesmobs.client.render.entity.RenderSunstrike;
import com.bobmowzie.mowziesmobs.client.render.entity.RenderUmvuthi;
import com.bobmowzie.mowziesmobs.client.render.entity.player.GeckoRenderPlayer;
import com.bobmowzie.mowziesmobs.server.ability.AbilityHandler;
import com.bobmowzie.mowziesmobs.server.ability.abilities.player.heliomancy.SolarFlareAbility;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;

/**
 * PORTING NOTE (26.1.2) - see PORTING_NOTES.md "GeckoPlayerItemInHandLayer.java / SolarFlareLayer.java - confirmed
 * cross-scope gap" section and {@link GeckoPlayerItemInHandLayer}'s javadoc in this package (same root cause, same
 * fix approach, read that class's javadoc for the full architectural writeup): the old {@code RenderLayer<
 * AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>} parented directly to a literal {@code GeckoRenderPlayer}
 * no longer type-checks at all (the redesigned {@code GeckoRenderPlayer} is not a {@code RenderLayerParent} and has
 * no {@code addLayer} mechanism), and the old body read {@code MowzieGeoBone#getWorldSpaceMatrix()} - dropped with
 * no replacement (GeckoLib 5 exposes no bone world transform outside its own live render traversal any more).
 * <p>
 * Rewritten as a standalone helper using {@code RenderPassInfo#addBonePositionListener}, the sanctioned GeckoLib-5
 * replacement (see {@code GeckoRenderPlayer}/{@code GeckoFirstPersonRenderer}'s own class javadocs) - and actually a
 * closer match to the original intent than the old technique: the original only ever used the "Body" bone's
 * translation component ({@code Vector4f(0,0,0,1).mul(bone.getWorldSpaceMatrix())}), which is exactly what the
 * listener's {@code worldPos} parameter already gives directly, no matrix math needed.
 * <p>
 * <b>Wiring (cross-scope, NOT done here)</b>: nothing currently calls {@link #registerListener}. Per the same
 * reasoning as {@link GeckoPlayerItemInHandLayer}, it needs a one-line addition to {@code
 * GeckoRenderPlayer#preRenderPass(RenderPassInfo, SubmitNodeCollector)} (out of this agent's 10-file scope) to
 * actually register it each frame - flagged rather than made.
 */
public class SolarFlareLayer {
    private final GeckoRenderPlayer renderPlayerAnimated;

    public SolarFlareLayer(GeckoRenderPlayer renderPlayerAnimated) {
        this.renderPlayerAnimated = renderPlayerAnimated;
    }

    /**
     * Registers the bone-position listener that draws the solar flare burst effect anchored to the "Body" bone for
     * one frame, while {@link SolarFlareAbility} is mid-burst. See class javadoc for the intended (cross-scope) call
     * site.
     */
    public void registerListener(RenderPassInfo<GeoRenderState> renderPassInfo, SubmitNodeCollector renderTasks, AbstractClientPlayer player) {
        renderPassInfo.addBonePositionListener("Body", (worldPos, modelPos, localPos) -> {
            if (worldPos == null || !renderPlayerAnimated.getAnimatedPlayerModel().isInitialized()) return;

            SolarFlareAbility ability = (SolarFlareAbility) AbilityHandler.INSTANCE.getAbility(player, AbilityHandler.SOLAR_FLARE_ABILITY);
            if (ability == null || !ability.isUsing()) return;

            int ticksInUse = ability.getTicksInUse();
            if (ticksInUse <= RenderUmvuthi.BURST_START_FRAME || ticksInUse >= RenderUmvuthi.BURST_START_FRAME + RenderUmvuthi.BURST_FRAME_COUNT - 1) return;

            float partialTicks = renderPassInfo.renderState().getPartialTick();
            PoseStack newMatrixStack = new PoseStack();
            newMatrixStack.translate(worldPos.x, worldPos.y, worldPos.z);
            newMatrixStack.scale(0.8f, 0.8f, 0.8f);

            renderTasks.submitCustomGeometry(newMatrixStack, MMRenderType.getSolarFlare(RenderSunstrike.TEXTURE), (pose, vertexConsumer) ->
                    RenderUmvuthi.drawBurst(pose.pose(), pose.normal(), vertexConsumer, ticksInUse - RenderUmvuthi.BURST_START_FRAME + partialTicks, renderPassInfo.packedLight()));
        });
    }
}
