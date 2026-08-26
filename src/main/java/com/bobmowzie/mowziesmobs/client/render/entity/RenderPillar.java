package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.client.model.entity.ModelPillar;
import com.bobmowzie.mowziesmobs.client.model.tools.geckolib.MowzieGeoBone;
import com.bobmowzie.mowziesmobs.client.model.tools.geckolib.MowzieGeoModel;
import com.bobmowzie.mowziesmobs.server.entity.effects.geomancy.EntityGeomancyBase;
import com.bobmowzie.mowziesmobs.server.entity.effects.geomancy.EntityPillar;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * PORTING NOTE: two old override points from the GeckoLib-4-era per-bone/per-cube renderer pipeline have NO
 * replacement in GeckoLib 5 (see MowzieGeoEntityRenderer.java / porting report):
 * - old `renderRecursively(...)` here was actually just reproducing the DEFAULT per-bone walk (translate/pivot/
 *   rotate/scale/render-cubes/render-children) with no Pillar-specific behavior of its own - that walk is now built
 *   directly into `GeoBone#positionAndRender`/`BakedGeoModel#render`, so this override has simply been deleted (no
 *   behavior loss - it did nothing different from the default walk).
 * - old `renderCube(...)` (replaced each model cube with a LIVE vanilla block render, so the pillar visually looked
 *   like a stack of real world blocks rather than static baked-model geometry) has NO surviving extension point -
 *   there is no per-cube renderer hook any more, and no live `MultiBufferSource` reachable at this point in the new
 *   deferred `SubmitNodeCollector` pipeline either. **This visual effect is NOT preserved** - the pillar will now
 *   render using the model's own baked cube geometry/texture instead of live block models. Flagged, not silently
 *   worked around; a real fix would need a bone-level rendering hook added to `MowzieGeoBone`/`CuboidGeoBone` (out
 *   of this agent's scope, client/model).
 * `MowzieGeoBone` also dropped `setChildrenHidden(boolean)` (present on the old GeckoLib-4-era bone API) - it's
 * reimplemented locally here directly against the wrapped `GeoBone`'s `BoneSnapshot`, mirroring how `MowzieGeoBone`
 * itself implements `setHidden`, since it's only needed in this one file.
 */
public class RenderPillar extends RenderGeomancyBase<EntityPillar, RenderPillar.PillarRenderState> {
    private static final Identifier TEXTURE_DIRT = Identifier.withDefaultNamespace("textures/block/dirt.png");

    public RenderPillar(EntityRendererProvider.Context mgr) {
        super(mgr, new ModelPillar());
    }

    @Override
    public Identifier getTextureLocation(PillarRenderState renderState) {
        return TEXTURE_DIRT;
    }

    @Override
    public PillarRenderState createRenderState(EntityPillar animatable, @Nullable Void relatedObject) {
        return new PillarRenderState();
    }

    @Override
    public void addRenderData(EntityPillar animatable, @Nullable Void relatedObject, PillarRenderState renderState, float partialTick) {
        super.addRenderData(animatable, relatedObject, renderState, partialTick);

        renderState.tier = animatable.getTier();
        renderState.height = animatable.prevPrevHeight + (animatable.prevHeight - animatable.prevPrevHeight) * partialTick;
        renderState.numRenders = (int) Math.ceil(animatable.getHeight()) + 1;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void preRenderPass(RenderPassInfo<PillarRenderState> renderPassInfo, SubmitNodeCollector renderTasks) {
        super.preRenderPass(renderPassInfo, renderTasks);

        EntityGeomancyBase.GeomancyTier tier = renderPassInfo.renderState().tier;
        MowzieGeoModel<EntityPillar> mowzieModel = (MowzieGeoModel<EntityPillar>) getGeoModel();
        MowzieGeoBone tier1 = mowzieModel.getMowzieBone("tier1");
        MowzieGeoBone tier2 = mowzieModel.getMowzieBone("tier2");
        MowzieGeoBone tier3 = mowzieModel.getMowzieBone("tier3");
        MowzieGeoBone tier4 = mowzieModel.getMowzieBone("tier4");
        MowzieGeoBone tier5 = mowzieModel.getMowzieBone("tier5");

        setChildrenHidden(tier1, true);
        setChildrenHidden(tier2, true);
        setChildrenHidden(tier3, true);
        setChildrenHidden(tier4, true);
        setChildrenHidden(tier5, true);

        if (tier == EntityGeomancyBase.GeomancyTier.NONE) setChildrenHidden(tier1, false);
        else if (tier == EntityGeomancyBase.GeomancyTier.SMALL) setChildrenHidden(tier2, false);
        else if (tier == EntityGeomancyBase.GeomancyTier.MEDIUM) setChildrenHidden(tier3, false);
        else if (tier == EntityGeomancyBase.GeomancyTier.LARGE) setChildrenHidden(tier4, false);
        else setChildrenHidden(tier5, false);
    }

    private static void setChildrenHidden(MowzieGeoBone bone, boolean hidden) {
        if (bone.bone.frameSnapshot == null) {
            bone.bone.frameSnapshot = BoneSnapshot.create(bone.bone);
        }

        bone.bone.frameSnapshot.skipChildrenRender(hidden);
    }

    @Override
    public void submit(PillarRenderState renderState, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState cameraState) {
        poseStack.pushPose();
        poseStack.translate(0, renderState.height - 0.5f, 0);

        for (int i = 0; i < renderState.numRenders; i++) {
            poseStack.translate(0, -1, 0);
            super.submit(renderState, poseStack, renderTasks, cameraState);
        }

        poseStack.popPose();
    }

    public static class PillarRenderState extends EntityRenderState {
        public EntityGeomancyBase.GeomancyTier tier;
        public float height;
        public int numRenders;
    }
}
