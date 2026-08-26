package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.client.model.entity.ModelFrostmaw;
import com.bobmowzie.mowziesmobs.client.render.MowzieRenderUtils;
import com.bobmowzie.mowziesmobs.client.render.entity.layer.ItemLayer;
import com.bobmowzie.mowziesmobs.server.entity.frostmaw.EntityFrostmaw;
import com.bobmowzie.mowziesmobs.server.item.ItemHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

/**
 * Created by BobMowzie on 5/8/2017.
 * <p>
 * PORTING NOTE (see PORTING_NOTES.md "MobRenderer-based ones using LLibrary models" section):
 * {@link ModelFrostmaw} extends LLibrary's {@code AdvancedModelBase}, which can no longer be the model type parameter
 * of {@code MobRenderer<T,S,M>} - ported to a plain {@code EntityRenderer<T,XRenderState>}. {@link ItemLayer} is no
 * longer a real {@code RenderLayer} so both ice-crystal instances are now submitted directly from {@link #submit}.
 * The socket-position capture (right hand/left hand/mouth world positions, fed back onto the live entity for other
 * systems - e.g. particle spawn points - to read) used {@link MowzieRenderUtils#getWorldPosFromModel}, which builds
 * its own fresh, self-contained {@code PoseStack} from the entity position + yaw + a manual bone-parent-chain walk -
 * it needs no live GeckoLib render pass and is unaffected by the "bone pose only valid during the live render
 * traversal" caveat documented elsewhere in PORTING_NOTES.md for real GeckoLib bones (this is an LLibrary
 * {@code AdvancedModelRenderer}, always addressable). Kept in {@link #submit} in the same relative position/timing
 * as the old post-{@code super.render()} call.
 */
public class RenderFrostmaw extends EntityRenderer<EntityFrostmaw, RenderFrostmaw.FrostmawRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/entity/frostmaw.png");

    private final ModelFrostmaw<EntityFrostmaw> model = new ModelFrostmaw<>();
    private final ItemLayer<FrostmawRenderState, EntityFrostmaw> iceCrystalHandLayer = new ItemLayer<>(
            state -> state.entity, model.iceCrystalHand, () -> ItemHandler.ICE_CRYSTAL.get().getDefaultInstance(), ItemDisplayContext.GROUND);
    private final ItemLayer<FrostmawRenderState, EntityFrostmaw> iceCrystalLayer = new ItemLayer<>(
            state -> state.entity, model.iceCrystal, () -> ItemHandler.ICE_CRYSTAL.get().getDefaultInstance(), ItemDisplayContext.GROUND);

    public RenderFrostmaw(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

    @Override
    public FrostmawRenderState createRenderState() {
        return new FrostmawRenderState();
    }

    @Override
    public void extractRenderState(EntityFrostmaw entity, FrostmawRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        state.entity = entity;
        state.yRot = entity.getYRot(partialTicks);
    }

    @Override
    public void submit(FrostmawRenderState state, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState cameraState) {
        EntityFrostmaw entity = state.entity;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        renderTasks.submitCustomGeometry(poseStack, model.renderType(TEXTURE), (pose, vertexConsumer) -> {
            poseStack.pushPose();
            poseStack.last().set(pose);
            model.setupAnim(entity, 0, 0, state.ageInTicks, 0, 0);
            model.renderToBuffer(poseStack, vertexConsumer, state.lightCoords, OverlayTexture.NO_OVERLAY, -1);
            poseStack.popPose();
        });

        iceCrystalHandLayer.submit(poseStack, renderTasks, state.lightCoords, state);
        iceCrystalLayer.submit(poseStack, renderTasks, state.lightCoords, state);

        poseStack.popPose();

        super.submit(state, poseStack, renderTasks, cameraState);

        if (entity.getAnimation() == EntityFrostmaw.SWIPE_ANIMATION || entity.getAnimation() == EntityFrostmaw.SWIPE_TWICE_ANIMATION || entity.getAnimation() == EntityFrostmaw.ICE_BREATH_ANIMATION || entity.getAnimation() == EntityFrostmaw.ICE_BALL_ANIMATION || !entity.getActive()) {
            // NOTE: re-pose the (renderer-instance-shared, so re-entrant-unsafe across simultaneous same-type
            // instances) LLibrary model synchronously here so the socket reads below reflect THIS entity's current
            // pose, since the actual draw's own setupAnim call above runs later (deferred, inside
            // submitCustomGeometry's callback) - see this class's javadoc. Matches the original's timing (read
            // immediately after the model was posed for rendering) for the common single-instance-on-screen case.
            model.setupAnim(entity, 0, 0, state.ageInTicks, 0, 0);
            Vec3 rightHandPos = MowzieRenderUtils.getWorldPosFromModel(entity, state.yRot, model.rightHandSocket);
            Vec3 leftHandPos = MowzieRenderUtils.getWorldPosFromModel(entity, state.yRot, model.leftHandSocket);
            Vec3 mouthPos = MowzieRenderUtils.getWorldPosFromModel(entity, state.yRot, model.mouthSocket);
            entity.setSocketPosArray(0, rightHandPos);
            entity.setSocketPosArray(1, leftHandPos);
            entity.setSocketPosArray(2, mouthPos);
        }
    }

    public static class FrostmawRenderState extends EntityRenderState {
        public EntityFrostmaw entity;
        public float yRot;
    }
}
