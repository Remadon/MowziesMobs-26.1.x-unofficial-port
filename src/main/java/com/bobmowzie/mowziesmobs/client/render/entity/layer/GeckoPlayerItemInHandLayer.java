package com.bobmowzie.mowziesmobs.client.render.entity.layer;

import com.bobmowzie.mowziesmobs.client.render.entity.player.GeckoRenderPlayer;
import com.bobmowzie.mowziesmobs.server.ability.Ability;
import com.bobmowzie.mowziesmobs.server.ability.PlayerAbility;
import com.bobmowzie.mowziesmobs.server.capability.AbilityData;
import com.bobmowzie.mowziesmobs.server.capability.DataHandler;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.util.RenderUtil;
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
 * Renders the player's held item (main and off hand) attached to the "RightHeldItem"/"LeftHeldItem" bones while a
 * GeckoLib ability animation is driving the body pose - {@code GeckoRenderPlayer#preRenderPass} calls
 * {@link #registerListeners} every frame.
 * <p>
 * The bone transform is reconstructed via {@code RenderUtil#transformToBone} (see {@code GeckoPlayerArmorLayer}'s
 * class javadoc for the full derivation of why this reproduces a bone's live world transform without needing
 * GeckoLib's own render traversal) rather than {@code RenderPassInfo#addBonePositionListener}, which an earlier
 * version of this class used: that callback fires from inside the draw-phase iteration over
 * {@code CustomFeatureRenderer.Storage.solidCustomGeometrySubmits} (again, see {@code GeckoPlayerArmorLayer} for the
 * full decompiled-source trace of why), and {@code itemInHandRenderer.renderItem} internally calls
 * {@code SubmitNodeCollector#submitModel}/{@code submitBlockModel}/{@code submitCustomGeometry} depending on the
 * item's render type - calling any of those from inside that draw-phase callback mutates the same collection
 * that's mid-iteration. For armor (a plain HashMap) that threw a {@code ConcurrentModificationException}; here it
 * evidently landed on a storage type that silently drops the add instead of throwing, so the held item just never
 * drew, with no error logged - reproduced live as "the axe disappears the instant the swing animation starts".
 * Fixed by looking the bone up directly and calling {@code RenderPassInfo#renderPosed} synchronously here, during
 * the submit phase, before any draw-phase iteration has begun - see {@code GeckoPlayerArmorLayer#renderBone} for
 * the sibling implementation of the same pattern.
 * <p>
 * <b>Behavioral note</b>: the old (pre-port) body also had a {@code this.getParentModel().young} check that scaled
 * the held item/arm down 0.5x and offset it, presumably for a shrunk/baby player state. There is no
 * {@code getParentModel()} any more (no {@code RenderLayerParent} in this redesign), and grepping for what could set
 * a real player's {@code PlayerModel.young} to true in this mod turned up nothing live - this appears to have been
 * boilerplate copied from a generic mob item-in-hand layer template rather than a reachable path for a player. Not
 * reproduced.
 */
public class GeckoPlayerItemInHandLayer {
    private final GeckoRenderPlayer renderPlayerAnimated;

    public GeckoPlayerItemInHandLayer(GeckoRenderPlayer renderPlayerAnimated) {
        this.renderPlayerAnimated = renderPlayerAnimated;
    }

    /**
     * Renders the held-item geometry at the "RightHeldItem"/"LeftHeldItem" bones for this frame. Called from
     * {@code GeckoRenderPlayer#preRenderPass}.
     */
    public void registerListeners(RenderPassInfo<GeoRenderState> renderPassInfo, SubmitNodeCollector renderTasks, AbstractClientPlayer player) {
        registerHandListener(renderPassInfo, renderTasks, player, "RightHeldItem", HumanoidArm.RIGHT, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
        registerHandListener(renderPassInfo, renderTasks, player, "LeftHeldItem", HumanoidArm.LEFT, ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
    }

    // FOLLOW-UP FIX (axe/held item silently stopped rendering the instant an ability's animation started): this
    // used to look up the bone's transform via RenderPassInfo#addBonePositionListener, whose callback - per the
    // same investigation documented on GeckoPlayerArmorLayer's class javadoc (see that file for the full
    // decompiled-source trace) - fires from *inside* the draw-phase iteration over
    // CustomFeatureRenderer.Storage.solidCustomGeometrySubmits, not the submit phase. itemInHandRenderer.renderItem
    // internally calls SubmitNodeCollector#submitModel/submitBlockModel/submitCustomGeometry depending on the
    // item's render type, so calling it from in there mutates the same collection structure that's mid-iteration -
    // for armor (a plain HashMap) that threw a ConcurrentModificationException; here it evidently lands on a
    // storage type that swallows the add instead of throwing, so the item just never draws, with no error logged.
    // Fixed the same way as the armor layer: look the bone up directly and call RenderPassInfo#renderPosed
    // synchronously from here (during preRenderPass, i.e. the submit phase, before any draw-phase iteration has
    // begun), then reconstruct the bone's transform via RenderUtil#transformToBone instead of relying on the live
    // listener firing mid-traversal - see GeckoPlayerArmorLayer's renderBone for the sibling implementation of the
    // exact same pattern.
    private void registerHandListener(RenderPassInfo<GeoRenderState> renderPassInfo, SubmitNodeCollector renderTasks, AbstractClientPlayer player, String boneName, HumanoidArm boneSide, ItemDisplayContext transformType) {
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

        renderPassInfo.model().getBone(boneName).ifPresent(bone -> renderPassInfo.renderPosed(() -> {
            PoseStack poseStack = renderPassInfo.poseStack();
            poseStack.pushPose();
            RenderUtil.transformToBone(poseStack, bone);
            PoseStack.Pose bonePose = poseStack.last();
            PoseStack newMatrixStack = new PoseStack();
            newMatrixStack.last().normal().mul(bonePose.normal());
            newMatrixStack.last().pose().mul(bonePose.pose());
            newMatrixStack.mulPose(Axis.XP.rotationDegrees(-90.0F));

            ItemInHandRenderer itemInHandRenderer = Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer();
            itemInHandRenderer.renderItem(player, stack, transformType, newMatrixStack, renderTasks, renderPassInfo.packedLight());
            poseStack.popPose();
        }));
    }
}
