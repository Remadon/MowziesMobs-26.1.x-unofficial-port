package com.bobmowzie.mowziesmobs.client.render.entity.layer;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.geckolib.util.RenderUtil;
import com.google.common.reflect.TypeToken;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * PORTING NOTE (GeckoLib 4 -> 5 full port): the old GeckoLib-4-era {@code BlockAndItemGeoLayer<T>} (per-bone
 * {@code getBlockForBone}/{@code renderForBone} override points, driven automatically by the framework walking the
 * bone tree every frame) no longer exists in that shape - GeckoLib 5's replacement
 * ({@code com.geckolib.renderer.layer.builtin.BlockAndItemGeoLayer}) is built around pre-resolving a
 * {@code List<RenderData>} once during {@code addRenderData} (live-entity access) and rendering it later via the
 * {@code addPerBoneRender}/{@code PerBoneRender} per-bone-callback mechanism (framework-positions the PoseStack at
 * each bone automatically before invoking the callback - see {@code GeoRendererInternals#submitPerBoneRenderTasks}).
 * This class is reimplemented directly against that same {@code addPerBoneRender} mechanism (without extending the
 * built-in {@code BlockAndItemGeoLayer}, since that class's constructor requires an
 * {@code EntityRendererProvider.Context} that this class's existing 2-arg constructor - matched to its real call
 * site in {@code RenderEarthSpike.java} - does not receive; a {@link BlockModelResolver} is instead obtained
 * directly from {@code Minecraft.getInstance()}, mirroring how GeckoLib's own {@code BlockAndItemGeoLayer} obtains
 * it from {@code Context#getBlockModelResolver()}), preserving the original per-bone-PREDICATE design
 * ({@code BiFunction<GeoBone, T, BlockState> blockForBone}, tested against every bone in the model, matching the old
 * behavior of {@code RenderEarthSpike}'s "any bone named *block*" usage) rather than adopting the new
 * fixed-bone-name {@code RenderData} list shape.
 */
public class GeckoBlockLayer<T extends GeoAnimatable, R extends GeoRenderState> extends GeoRenderLayer<T, Void, R> {
    private static final DataTicket<List<GeckoBlockLayer.BoneBlock>> CONTENTS = DataTicket.create(
            "mowziesmobs_geckoblocklayer_contents", new TypeToken<List<GeckoBlockLayer.BoneBlock>>() {});

    private final BiFunction<GeoBone, T, BlockState> blockForBone;
    private final BlockModelResolver blockModelResolver = Minecraft.getInstance().getBlockModelResolver();

    public GeckoBlockLayer(GeoRenderer<T, Void, R> rendererIn, BiFunction<GeoBone, T, BlockState> blockForBone) {
        super(rendererIn);
        this.blockForBone = blockForBone;
    }

    @Override
    public void addRenderData(T animatable, @Nullable Void relatedObject, R renderState, float partialTick) {
        BakedGeoModel bakedModel = getDefaultBakedModel(renderState);
        List<GeckoBlockLayer.BoneBlock> contents = new ArrayList<>();

        for (GeoBone bone : bakedModel.boneLookup().get().values()) {
            BlockState blockState = blockForBone.apply(bone, animatable);
            if (blockState != null && !blockState.isAir()) {
                contents.add(new BoneBlock(bone.name(), blockState));
            }
        }

        if (!contents.isEmpty()) {
            renderState.addGeckolibData(CONTENTS, contents);
        }
    }

    @Override
    public void addPerBoneRender(RenderPassInfo<R> renderPassInfo, BiConsumer<GeoBone, PerBoneRender<R>> consumer) {
        for (GeckoBlockLayer.BoneBlock boneBlock : renderPassInfo.getOrDefaultGeckolibData(CONTENTS, List.of())) {
            renderPassInfo.model().getBone(boneBlock.boneName())
                    .ifPresent(bone -> consumer.accept(bone, (rpi, b, renderTasks) -> renderBlockForBone(rpi, boneBlock.blockState(), renderTasks)));
        }
    }

    protected void renderBlockForBone(RenderPassInfo<R> renderPassInfo, BlockState blockState, SubmitNodeCollector renderTasks) {
        PoseStack poseStack = renderPassInfo.poseStack();

        poseStack.pushPose();
        poseStack.translate(-0.5F, 0, -0.5F);
        BlockModelRenderState blockModelRenderState = RenderUtil.createRenderStateForBlock(blockState, blockModelResolver);
        int outlineColor = renderPassInfo.renderState() instanceof EntityRenderState entityRenderState ? entityRenderState.outlineColor : 0;
        blockModelRenderState.submit(poseStack, renderTasks, renderPassInfo.packedLight(), OverlayTexture.NO_OVERLAY, outlineColor);
        poseStack.popPose();
    }

    private record BoneBlock(String boneName, BlockState blockState) {
    }
}
