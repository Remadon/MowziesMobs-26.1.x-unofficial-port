package com.bobmowzie.mowziesmobs.client.render.entity.layer;

import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.function.Function;

/**
 * Renders the player's equipped vanilla armor (helmet/chestplate/leggings/boots) while a GeckoLib ability
 * animation is driving the body pose. GeckoRenderPlayer's own class javadoc documents this as intentionally
 * dropped: vanilla's HumanoidArmorLayer needs a live ModelPart tree to copy the current pose onto (via
 * HumanoidModel#setupAnim), and GeckoLib 5 doesn't expose a bone's world transform outside its own live render
 * traversal, so there was no way to keep a *separately posed* vanilla armor model in sync with the GeckoLib body.
 * <p>
 * This sidesteps that problem instead of solving it: each armor ModelPart is positioned using the player bone's
 * live, current-frame transform, reconstructed via {@code RenderUtil.transformToBone} (walking the bone's parent
 * chain and applying each ancestor's baked keyframe pose) rather than posing an independent armor model and hoping
 * it matches.
 * <p>
 * <b>CRASH HISTORY - do not "simplify" this back to addBonePositionListener</b>: an earlier version registered a
 * {@code RenderPassInfo#addBonePositionListener} callback per bone and called {@code submitCustomGeometry} from
 * inside it (the same technique GeckoPlayerItemInHandLayer still uses for held items). That crashed intermittently
 * with a {@code ConcurrentModificationException} in {@code CustomFeatureRenderer.renderSolid}. Root cause, found by
 * decompiling GeckoLib's own render pipeline: the default {@code GeoRenderer#submitRenderTasks} queues the
 * player's own mesh render via {@code submitCustomGeometry}, and that queued callback is what eventually calls
 * {@code renderPassInfo.renderPosed(...)} to walk the bone tree and fire bone-position listeners - meaning those
 * listeners fire from *inside* {@code CustomFeatureRenderer.renderSolid}'s iteration over its
 * {@code solidCustomGeometrySubmits} map, during the draw phase, not the submit phase. Calling
 * {@code submitCustomGeometry} again from in there inserts a new key into that same map while it's being iterated
 * - a classic add-during-iteration bug, intermittent because Java's HashMap only throws when the fail-fast check
 * in the outer iterator's next() actually runs again afterwards (i.e. only when hash-bucket order happens to leave
 * more entries to visit after the mutated one - so it depends on what else is rendering that frame).
 * <p>
 * The fix, copied from GeckoLib's own built-in {@code ItemArmorGeoLayer} (which solves this exact "vanilla armor
 * riding a GeckoLib bone" problem for its own armor-slot rendering): call {@code renderPassInfo.renderPosed(...)}
 * directly and synchronously, once per bone/slot here, from {@code preRenderPass} - i.e. during the submit phase,
 * before the draw phase (and its live map iteration) has even begun. {@code renderPosed} is safe to call multiple
 * times per frame (its {@code boneUpdates} bake is memoized via {@code DeferredCache}, so repeat calls are cheap),
 * and {@code renderTasks.submitCustomGeometry(...)} calls made from within it land squarely in the submit phase,
 * with no nested nesting inside anyone else's draw callback.
 * <p>
 * Texture/dye-tint resolution is hand-rolled from EquipmentAssetManager/IClientItemExtensions rather than reusing
 * EquipmentLayerRenderer#renderLayers, since that method operates on one whole (multi-part) Model per call and
 * defers Model#setupAnim to an unspecified later point in the frame - both incompatible with rendering one
 * independent part at a time via the lower-level SubmitNodeCollector#submitModelPart. Armor trims and enchantment
 * glint are NOT reproduced here (out of scope for this pass, base texture + dye tint covers the reported case) -
 * if trims/glint are needed later, extend the loop in renderBone the same way EquipmentLayerRenderer#renderLayers
 * does (see that class for the trim/glint block to port over).
 */
public class GeckoPlayerArmorLayer {
    private final ArmorModelSet<PlayerModel> normalModels;
    private final ArmorModelSet<PlayerModel> slimModels;
    private final EquipmentAssetManager equipmentAssets;

    public GeckoPlayerArmorLayer(EntityModelSet modelSet, EquipmentAssetManager equipmentAssets) {
        this.equipmentAssets = equipmentAssets;
        this.normalModels = ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, modelSet, part -> new PlayerModel(part, false));
        this.slimModels = ArmorModelSet.bake(ModelLayers.PLAYER_SLIM_ARMOR, modelSet, part -> new PlayerModel(part, true));
    }

    // FOLLOW-UP FIX (chestplate sags down into the leggings during the swing; separately, the leggings' own "Body"
    // mapping - the waist/belt strip below the beltline - was confirmed via live diagnostic (disabling it made a
    // reported "phantom boot" disappear, at the cost of losing that strip's real coverage) to need the exact same
    // treatment): both CHEST's and LEGS' "Body" mappings ride the same "Body" GeoBone with the identical transform,
    // and use an essentially identical vanilla "body" cube shape (leggings' is only very slightly shrunk by
    // vanilla's own inner-armor deformation) - so this isn't the pivot-frame bug fixed above, it's that GeckoLib's
    // rig splits the torso into a Waist -> Body -> Neck chain for bendable-spine animation, and "Body"'s own pivot
    // sits low (near the waist, where vanilla's single rigid body box has no equivalent joint). Anchoring either
    // piece's full-height mesh there without correction leaves it hanging well below where it should be - for the
    // chestplate that reads as sagging into the leggings as the bone rotates; for the leggings' waist strip
    // (which barely rotates day-to-day) it reads as a static, boot-shaped mass sitting near the ground, since the
    // piece is anchored so far below its intended position that it clears the actual legs entirely. Lifting each
    // piece's own mesh origin (not the leg bones') compensates by making it behave as if anchored higher, closer to
    // where vanilla expects.
    // TRIED AND REVERTED: mirroring GeckoLib's own ItemArmorGeoLayer#getScaleFactorForBone (comparing the GeckoLib
    // bone's own cube size to the vanilla armor cube's size, and scaling the pose stack by the ratio) looked like
    // the more "correct", less-hacky fix for this and for a separate boots-tracking bug - but live-tested worse on
    // both: the chestplate ballooned to several times its normal size. This custom player rig is built to closely
    // match vanilla human proportions (it renders the player's own vanilla skin texture), so the two rigs' cube
    // sizes should already be near 1:1 - GeckoLib's formula is designed for genuinely differently-proportioned
    // custom mobs, and something about how it reads this rig's bone cube geometry (units, or which cube got
    // selected off a multi-cube bone) didn't hold here. Reverted to this hand-tuned constant; magnitude is a
    // visual-fit value, not derived from a fixed geometric ratio - retune here if the overlap direction reverses.
    private static final float BODY_PIVOT_LIFT_SIXTEENTHS = 12f;

    public void registerListeners(RenderPassInfo<GeoRenderState> renderPassInfo, SubmitNodeCollector renderTasks, AbstractClientPlayer player) {
        boolean slim = player.getSkin().model().name().equals("SLIM");
        ArmorModelSet<PlayerModel> models = slim ? slimModels : normalModels;

        renderBone(renderPassInfo, renderTasks, player, "Head", models, EquipmentSlot.HEAD, m -> m.head, 0f);
        renderBone(renderPassInfo, renderTasks, player, "Body", models, EquipmentSlot.CHEST, m -> m.body, BODY_PIVOT_LIFT_SIXTEENTHS);
        renderBone(renderPassInfo, renderTasks, player, "RightArm", models, EquipmentSlot.CHEST, m -> m.rightArm, 0f);
        renderBone(renderPassInfo, renderTasks, player, "LeftArm", models, EquipmentSlot.CHEST, m -> m.leftArm, 0f);
        renderBone(renderPassInfo, renderTasks, player, "Body", models, EquipmentSlot.LEGS, m -> m.body, BODY_PIVOT_LIFT_SIXTEENTHS);
        renderBone(renderPassInfo, renderTasks, player, "RightLeg", models, EquipmentSlot.LEGS, m -> m.rightLeg, 0f);
        renderBone(renderPassInfo, renderTasks, player, "LeftLeg", models, EquipmentSlot.LEGS, m -> m.leftLeg, 0f);
        renderBone(renderPassInfo, renderTasks, player, "RightLeg", models, EquipmentSlot.FEET, m -> m.rightLeg, 0f);
        renderBone(renderPassInfo, renderTasks, player, "LeftLeg", models, EquipmentSlot.FEET, m -> m.leftLeg, 0f);
    }

    private void renderBone(RenderPassInfo<GeoRenderState> renderPassInfo, SubmitNodeCollector renderTasks, AbstractClientPlayer player, String boneName, ArmorModelSet<PlayerModel> models, EquipmentSlot slot, Function<PlayerModel, ModelPart> partGetter, float pivotLiftSixteenths) {
        ItemStack stack = player.getItemBySlot(slot);
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null || equippable.slot() != slot || equippable.assetId().isEmpty()) return;

        EquipmentClientInfo.LayerType layerType = slot == EquipmentSlot.LEGS ? EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS : EquipmentClientInfo.LayerType.HUMANOID;
        List<EquipmentClientInfo.Layer> layers = equipmentAssets.get(equippable.assetId().get()).getLayers(layerType);
        if (layers.isEmpty()) return;

        renderPassInfo.model().getBone(boneName).ifPresent(bone -> renderPassInfo.renderPosed(() -> {
            ModelPart part = partGetter.apply(models.get(slot));
            part.xRot = 0;
            part.yRot = 0;
            part.zRot = 0;
            // GeckoLib's own ItemArmorGeoLayer#submitVanillaEquippableRender does the equivalent of this when
            // bridging a GeoBone onto a vanilla ModelPart - transformToBone leaves the pose stack AT the bone's
            // pivot (see its javadoc), but ModelPart#render applies its OWN x/y/z translate as its first step
            // (vanilla ModelParts are defined relative to their parent's local origin, not "at a pivot"). So the
            // pose stack needs to be walked back to the bone's local origin (translateAwayFromPivotPoint) and the
            // part's x/y/z set to represent that same pivot instead, the way ModelPart itself expects it - and
            // since GeckoLib bone space is Y-up (Blockbench convention) while vanilla ModelPart space is Y-down,
            // an axis flip is needed at the same point GeckoLib's own reference applies it. Omitting this (the
            // original bug here) still looks approximately right at rest - the pivot offset is small relative to
            // the whole part - but blows up into the pieces flying away from the body once the bone actually
            // rotates during an ability, since the un-translated-away pivot offset gets carried along for the ride.
            part.x = -bone.pivotX();
            // Subtracting here (not adding) is correct despite lifting the mesh upward: this happens inside the
            // pose stack's Y-flip (see the scale(-1,-1,1) below), so a smaller/more-negative part.y ends up higher
            // in the final rendered frame - see CHEST_PIVOT_LIFT_SIXTEENTHS's own comment for why this exists.
            part.y = -bone.pivotY() - pivotLiftSixteenths;
            part.z = bone.pivotZ();

            PoseStack poseStack = renderPassInfo.poseStack();
            poseStack.pushPose();
            RenderUtil.transformToBone(poseStack, bone);
            bone.translateAwayFromPivotPoint(poseStack);
            poseStack.scale(-1.0F, -1.0F, 1.0F);

            IClientItemExtensions extensions = IClientItemExtensions.of(stack);
            int dyeColor = extensions.getDefaultDyeColor(stack);
            boolean renderFoil = stack.hasFoil();
            int lightCoords = renderPassInfo.packedLight();

            int idx = 0;
            for (EquipmentClientInfo.Layer layer : layers) {
                int color = extensions.getArmorLayerTintColor(stack, layer, idx, dyeColor);
                if (color != 0) {
                    Identifier texture = ClientHooks.getArmorTexture(stack, layerType, layer, layer.getTextureLocation(layerType));
                    // Rendered via submitCustomGeometry (manually driving ModelPart#render from inside the callback,
                    // mirroring what ModelPartFeatureRenderer#render does internally) rather than the more obvious
                    // SubmitNodeCollector#submitModelPart, and using RenderTypes.entityCutout rather than the more
                    // obviously-correct RenderTypes.armorCutoutNoCull - both substitutions were confirmed necessary
                    // live: submitModelPart submitted successfully (correct position/scale/visibility/tint every
                    // frame) but never actually drew anything, and armorCutoutNoCull has the same problem specifically
                    // because of its .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING) call - a pipeline
                    // feature entityCutout/submitCustomGeometry (already proven working for UmvuthanaSunLayer's sun
                    // square through this exact GeckoLib render context) doesn't use. Losing armor's usual slight
                    // anti-Z-fighting nudge against the body mesh underneath is an acceptable trade for it being
                    // visible at all during abilities - revisit if z-fighting flicker shows up.
                    renderTasks.submitCustomGeometry(poseStack, RenderTypes.entityCutout(texture), (pose, buffer) -> {
                        PoseStack tempStack = new PoseStack();
                        tempStack.last().set(pose);
                        part.render(tempStack, buffer, lightCoords, OverlayTexture.NO_OVERLAY, color);
                    });
                    if (renderFoil) {
                        renderTasks.submitCustomGeometry(poseStack, RenderTypes.armorEntityGlint(), (pose, buffer) -> {
                            PoseStack tempStack = new PoseStack();
                            tempStack.last().set(pose);
                            part.render(tempStack, buffer, lightCoords, OverlayTexture.NO_OVERLAY, color);
                        });
                        renderFoil = false;
                    }
                }
                idx++;
            }

            poseStack.popPose();
        }));
    }
}
