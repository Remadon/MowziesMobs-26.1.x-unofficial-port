package com.bobmowzie.mowziesmobs.client.render.entity.layer;

import com.bobmowzie.mowziesmobs.client.model.tools.geckolib.MowzieGeoBone;
import com.bobmowzie.mowziesmobs.server.entity.umvuthana.EntityUmvuthana;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * PORTING NOTE (GeckoLib 4 -> 5 full port): the previous port of this file routed the mob-worn mask through
 * vanilla's {@code EquipmentLayerRenderer} (the texture-layer armor system driven by {@code assets/<ns>/equipment/
 * *.json}) - the wrong subsystem entirely, and the actual cause of the "mask doesn't appear correctly, broken
 * texture" bug on live Umvuthana mobs. This mod's masks are {@code ItemUmvuthanaMask}, a {@code GeoItem} with its
 * own 3D GeckoLib model ({@code UmvuthanaMaskModel}) - the same model already used correctly for the item's in-hand/
 * GUI render. On the PLAYER, GeckoLib 5's own armor-layer mixin ({@code GeoArmorRenderer#tryRenderGeoArmorPiece})
 * substitutes this automatically (which is why masks render fine when worn by the player) - but that mixin only
 * hooks vanilla's {@code HumanoidArmorLayer} (entities rendered via {@code HumanoidRenderState}). EntityUmvuthana is
 * a {@code LivingEntityRenderState}-based GeckoLib mob, never touched by that mixin, so it needs a manual render
 * call. Rather than going through {@code GeoArmorRenderer} (built around copying a wearer's {@code HumanoidModel}
 * per-limb pose onto matching {@code armorXxx} bones - not applicable to a non-humanoid mob), this renders the mask
 * via its own {@code GeoItemRenderer} directly ({@code GeoRenderProvider#getGeoItemRenderer}, resolved dynamically
 * so this also covers {@code ItemSolVisage}), mirroring the exact
 * {@code createRenderState}/{@code fillRenderState}/{@code submit} sequence GeckoLib's own internal
 * {@code GeckolibItemSpecialRenderer} uses to render GeoItems everywhere else in the game. The manual rotate/scale/
 * translate below is unchanged from the pre-port code (same model, same bone) - it converts the item renderer's
 * item-space origin onto the "maskTwitcher"/"maskHand" bone.
 * <p>
 * The old {@code head.getPose()}-based positioning (see {@link UmvuthanaSunLayer}'s javadoc - that side table is
 * never written anywhere in this codebase) is replaced with the {@code addPerBoneRender} per-bone mechanism, same
 * as the other bone-positioned layers in this package. The live entity's head-slot {@code ItemStack} and the entity
 * itself are captured into the render state via DataTickets in {@code addRenderData} since the per-bone callback
 * has no live-entity access.
 */
public class UmvuthanaArmorLayer<R extends LivingEntityRenderState & GeoRenderState> extends GeoRenderLayer<EntityUmvuthana, Void, R> {
    private static final DataTicket<ItemStack> HEAD_ITEM = DataTicket.create("mowziesmobs_umvuthana_head_item", ItemStack.class);
    private static final DataTicket<EntityUmvuthana> WEARER = DataTicket.create("mowziesmobs_umvuthana_mask_wearer", EntityUmvuthana.class);

    private final String boneName;

    public UmvuthanaArmorLayer(GeoRenderer<EntityUmvuthana, Void, R> entityRendererIn, EntityRendererProvider.Context context, String boneName) {
        super(entityRendererIn);
        this.boneName = boneName;
    }

    @Override
    public void addRenderData(EntityUmvuthana animatable, @Nullable Void relatedObject, R renderState, float partialTick) {
        renderState.addGeckolibData(HEAD_ITEM, animatable.getItemBySlot(EquipmentSlot.HEAD));
        renderState.addGeckolibData(WEARER, animatable);
    }

    @Override
    public void addPerBoneRender(RenderPassInfo<R> renderPassInfo, BiConsumer<GeoBone, PerBoneRender<R>> consumer) {
        renderPassInfo.model().getBone(boneName).ifPresent(bone -> consumer.accept(bone, this::renderArmorAtBone));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void renderArmorAtBone(RenderPassInfo<R> renderPassInfo, GeoBone bone, SubmitNodeCollector renderTasks) {
        if (bone.frameSnapshot != null && bone.frameSnapshot.isHidden()) return;

        // Only "heal_start"/"heal_loop"/"heal_end" animate maskHand's transform (moving it up to the hand for that
        // gesture) or maskPlacementSwitchController (its position.x runs 0 -> 1 across that same animation) - no
        // idle/walk/other animation touches either bone, and nothing in the geo model itself hides maskHand by
        // default, so without this check it renders at its bind-pose rest position (down near the hand) at all
        // times in addition to the head mask. Gate each instance to its own side of that 0/1 switch value.
        boolean isHandBone = boneName.equals("maskHand");
        double placement = renderPassInfo.model().getBone("maskPlacementSwitchController")
                .map(b -> (double) new MowzieGeoBone(b).getPosX())
                .orElse(0.0);
        if (isHandBone != (placement >= 0.5)) return;

        ItemStack itemStack = renderPassInfo.getGeckolibData(HEAD_ITEM);
        if (itemStack == null || itemStack.isEmpty()) return;

        Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
        if (equippable == null || equippable.slot() != EquipmentSlot.HEAD || equippable.assetId().isEmpty()) return;

        Item item = itemStack.getItem();
        if (!(item instanceof GeoAnimatable)) return;
        GeoAnimatable animatable = (GeoAnimatable) item;

        GeoItemRenderer maskRenderer = GeoRenderProvider.of(item).getGeoItemRenderer();
        if (maskRenderer == null) return;

        EntityUmvuthana wearer = renderPassInfo.getGeckolibData(WEARER);
        ClientLevel level = wearer != null ? (ClientLevel) wearer.level() : null;

        GeoItemRenderer.RenderData renderData = new GeoItemRenderer.RenderData(
                itemStack, new ItemStackRenderState(), ItemDisplayContext.HEAD, level, wearer);
        GeoRenderState maskRenderState = maskRenderer.fillRenderState(animatable, renderData,
                maskRenderer.createRenderState(animatable, renderData), renderPassInfo.renderState().partialTick);
        maskRenderState.addGeckolibData(DataTickets.PACKED_LIGHT, renderPassInfo.packedLight());

        // The mask's own geo model (mask_fury.geo.json etc.) is built in vanilla's biped-armor convention: its
        // "mask" bone sits at an absolute pivot of [0,28,-4] pixels (= 1.75 blocks up, 0.25 blocks forward), with
        // its cubes' raw Y coordinates spanning ~21.75-39.57px around that same height - i.e. the geometry already
        // sits ~1.75 blocks *above* local origin on its own, unlike a normal item built centered at origin. Since
        // this render path (GeoItemRenderer, not the full GeoArmorRenderer pipeline) applies no mirror/flip, the fix
        // is a plain downward translate by that same 1.75 blocks to bring the mesh back down onto the bone pivot,
        // not an upward one (an earlier pass added +1.5 here on the mistaken assumption a since-removed mirror step
        // would flip its sign - it didn't, so it only pushed the mask further away). translate.z partially (not
        // fully) cancels the mask's own +0.25-forward bake-in, per feedback that it should sit a little closer to
        // the face without going fully flush.
        PoseStack poseStack = renderPassInfo.poseStack();
        poseStack.pushPose();
        // CONFIRMED CORRECT (live feedback): no extra rotation is needed here at all - maskTwitcher's own local
        // axes already line up with the mask model's vanilla-armor-convention axes without any manual correction.
        // (Was Axis.YP.rotationDegrees(90f), before that -90f, before that -90f + a +90 Z roll - all wrong; the
        // fix was to remove the rotation entirely, not find the "right" angle.)
        //
        // FOLLOW-UP FIX (mask rendering too small vs. the 1.21.1 reference): nothing here ever compensated for the
        // fact that this whole poseStack is already carrying the entity rig's root.multiplyScale(0.83/0.93, ...)
        // (see ModelUmvuthana#setCustomAnimations) by the time we're positioned at maskTwitcher - the reference
        // client's mask (rendered through the real armor pipeline on a player, which does NOT carry the wearer's own
        // custom model-shrink onto worn armor) is at natural 1:1 size, while ours inherits that shrink since this
        // path reuses the entity's own already-scaled-down poseStack. Reading "root"'s live scale directly (rather
        // than hardcoding the 0.83/0.93 constants a second time) and applying its inverse here cancels that shrink
        // out exactly, for both mask tiers, without needing to duplicate/guess a magic number.
        //
        // BUG (first attempt at the above): PoseStack composes each new call so it acts on the raw vertex data
        // *before* whatever was already on the stack (the standard "last-called transform hits the vertex first"
        // stack convention) - so calling .scale(...) *after* the two .translate(...) calls scaled the mask's own
        // internal offset (its cubes sit ~1.75-2.4 blocks up from ITS OWN local origin, per the note above) before
        // the fixed position-tuning translate got added on top. Moving .scale(...) to be the *first* call (so it's
        // last to act on the vertex) and dividing the translate amounts by the same factor kept the *net* translate
        // mathematically unchanged - but that still left the underlying problem: it scales the mesh around its own
        // raw (0,0,0), which per the note above sits well *below* where the geometry actually lives (~1.75 blocks
        // up). Scaling around a point that low still drags the visible mesh upward as the mesh grows, away from that
        // low point - confirmed live ("anchored at the bottom... offset up too far").
        // FIX: scale around the mask model's own "mask" bone pivot ([0,28,-4]px = [0,1.75,-0.25] blocks - see note
        // above) instead of around raw (0,0,0), so the mesh grows from a point that's actually near its own visual
        // center. Standard "scale around point P" is translate(+P) -> scale(k) -> translate(-P); folding the "-P"
        // and "+P" into the existing translate constants (rather than adding two more calls) simplifies to shifting
        // just the first translate's Y from -1.75 to -1.75*scale, and Z from 0.3 to 0.05+0.25*scale (worked out by
        // expanding k*(v-P)+P algebraically - reduces to the original untouched values when scale=1).
        MowzieGeoBone rootBone = renderPassInfo.model().getBone("root").map(MowzieGeoBone::new).orElse(null);
        float rootScale = rootBone != null ? rootBone.getScaleX() : 1f;
        float scale = rootScale > 0f ? 1f / rootScale : 1f;
        poseStack.translate(0, -1.75f * scale, 0.05f + 0.25f * scale);
        poseStack.translate(-0.5, -0.51, -0.5);
        poseStack.scale(scale, scale, scale);

        int outlineColor = renderPassInfo.renderState().outlineColor;
        maskRenderer.submit(maskRenderState, poseStack, renderTasks, outlineColor);

        poseStack.popPose();
    }
}
