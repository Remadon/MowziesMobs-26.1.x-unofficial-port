package com.bobmowzie.mowziesmobs.client.render.entity.layer;

import com.bobmowzie.mowziesmobs.client.render.entity.player.GeckoRenderPlayer;
import com.bobmowzie.mowziesmobs.server.ability.Ability;
import com.bobmowzie.mowziesmobs.server.ability.PlayerAbility;
import com.bobmowzie.mowziesmobs.server.capability.AbilityData;
import com.bobmowzie.mowziesmobs.server.capability.DataHandler;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * PORTING NOTE (26.1.2) - see the coordinator's brief and PORTING_NOTES.md's "GeckoPlayerItemInHandLayer.java /
 * SolarFlareLayer.java - confirmed cross-scope gap" section: this class hit the SAME root architectural wall as
 * {@code GeckoCapeLayer}/{@code GeckoElytraLayer}/{@code GeckoParrotOnShoulderLayer} in this package (the
 * redesigned {@code GeckoRenderPlayer} is no longer a {@code RenderLayerParent} and has no {@code addLayer}/{@code
 * layers} mechanism at all - see {@code GeckoCapeLayer}'s javadoc), PLUS a second, independent blocker specific to
 * this file: it called {@code MowzieGeoBone#getWorldSpaceMatrix()}/{@code #getWorldSpaceNormal()}, which were
 * dropped entirely with no replacement (see PORTING_NOTES.md "MowzieGeoBone is now a WRAPPER" section - GeckoLib 5
 * does not expose a bone's world transform outside of its own live render traversal at all any more).
 * <p>
 * Rather than inventing a new integration point on {@code GeckoRenderPlayer.java} itself (out of this agent's file
 * scope, and that file is a finished, dedicated-agent-owned redesign - reaching into it here would risk conflicting
 * with that design), this class is rewritten as a standalone helper using the EXACT bone-transform-recovery
 * technique {@code GeckoRenderPlayer}/{@code GeckoFirstPersonRenderer} themselves already established and documented
 * for this identical problem (recovering a bone's full world rotation despite {@code addBonePositionListener} being
 * position-only, by reading the live {@code PoseStack} from inside the listener callback while it is pushed to that
 * bone's transform - see {@code GeckoRenderPlayer}'s class javadoc, "Attachment points (held item / particle root)
 * recover full rotation" section, and {@code GeckoFirstPersonRenderer#registerHandListener} for a directly parallel
 * worked example rendering an item at a bone-attached hand). This is deliberately NOT a guess at a new design - it
 * reuses the other agent's own sanctioned, already-working mechanism verbatim.
 * <p>
 * <b>Wiring (cross-scope, NOT done here, matching this codebase's established "flag it, don't reach outside scope"
 * convention for exactly this situation)</b>: nothing currently calls {@link #registerListeners}. It needs to be
 * invoked once per frame from inside {@code GeckoRenderPlayer#preRenderPass(RenderPassInfo, SubmitNodeCollector)}
 * (the same place {@code registerAttachmentListener} is called for "LeftHeldItem"/"RightHeldItem"/
 * "ParticleEmitterRoot" today), passing that call's own {@code renderPassInfo}/{@code renderTasks} plus the live
 * player. That one-line addition is a change to {@code GeckoRenderPlayer.java}, which is out of this agent's 10-file
 * scope - flagged here rather than made, per the coordinator's explicit instruction not to guess at a design that
 * conflicts with the player-render agent's finished file. Until that wiring exists, this class is inert (constructed
 * by nobody, its listeners never registered) but fully implemented and ready to be called.
 * <p>
 * <b>Small behavioral note</b>: the old body also had a {@code this.getParentModel().young} check that scaled the
 * held item/arm down 0.5x and offset it, presumably for a shrunk/baby player state. There is no {@code
 * getParentModel()} any more (no {@code RenderLayerParent} - see above), and grepping for what could set a real
 * player's {@code PlayerModel.young} to true in this mod turned up nothing live - this appears to have been
 * boilerplate copied from a generic mob item-in-hand layer template rather than a reachable path for a player. Not
 * reproduced; flagged here rather than silently carried forward as an unreachable dead branch.
 */
public class GeckoPlayerItemInHandLayer {
    private final GeckoRenderPlayer renderPlayerAnimated;

    public GeckoPlayerItemInHandLayer(GeckoRenderPlayer renderPlayerAnimated) {
        this.renderPlayerAnimated = renderPlayerAnimated;
    }

    /**
     * Registers the bone-position listeners that render the held-item/arm geometry at the "RightHeldItem"/
     * "LeftHeldItem" bones for one frame. See class javadoc for the intended (cross-scope) call site.
     */
    public void registerListeners(RenderPassInfo<GeoRenderState> renderPassInfo, SubmitNodeCollector renderTasks, AbstractClientPlayer player) {
        registerHandListener(renderPassInfo, renderTasks, player, "RightHeldItem", HumanoidArm.RIGHT, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
        registerHandListener(renderPassInfo, renderTasks, player, "LeftHeldItem", HumanoidArm.LEFT, ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
    }

    private void registerHandListener(RenderPassInfo<GeoRenderState> renderPassInfo, SubmitNodeCollector renderTasks, AbstractClientPlayer player, String boneName, HumanoidArm boneSide, ItemDisplayContext transformType) {
        renderPassInfo.addBonePositionListener(boneName, (worldPos, modelPos, localPos) -> {
            if (!renderPlayerAnimated.getAnimatedPlayerModel().isInitialized()) return;

            ItemStack mainHandStack = player.getMainHandItem();
            ItemStack offHandStack = player.getOffhandItem();
            AbilityData abilityData = DataHandler.getData(player, DataHandler.ABILITY_DATA);
            if (abilityData != null && abilityData.getActiveAbility() != null) {
                Ability<?> ability = abilityData.getActiveAbility();
                if (ability instanceof PlayerAbility playerAbility) {
                    mainHandStack = playerAbility.heldItemMainHandOverride() != null ? playerAbility.heldItemMainHandOverride() : mainHandStack;
                    offHandStack = playerAbility.heldItemOffHandOverride() != null ? playerAbility.heldItemOffHandOverride() : offHandStack;
                }
            }

            boolean isMainHandBone = boneSide == player.getMainArm();
            ItemStack stack = isMainHandBone ? mainHandStack : offHandStack;
            if (stack.isEmpty()) return;

            PoseStack.Pose bonePose = renderPassInfo.poseStack().last();
            PoseStack newMatrixStack = new PoseStack();
            newMatrixStack.last().normal().mul(bonePose.normal());
            newMatrixStack.last().pose().mul(bonePose.pose());
            newMatrixStack.mulPose(Axis.XP.rotationDegrees(-90.0F));

            ItemInHandRenderer itemInHandRenderer = Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer();
            itemInHandRenderer.renderItem(player, stack, transformType, newMatrixStack, renderTasks, renderPassInfo.packedLight());
        });
    }
}
