package com.bobmowzie.mowziesmobs.client.render.entity.layer;

import com.bobmowzie.mowziesmobs.server.entity.MowzieGeckoEntity;
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
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * PORTING NOTE (GeckoLib 4 -> 5 full port): reimplemented against the {@code addPerBoneRender}/{@code PerBoneRender}
 * per-bone-callback mechanism, same rationale/pattern as {@link GeckoBlockLayer} (see its javadoc) - a
 * {@link ItemModelResolver} is obtained directly from {@code Minecraft.getInstance()} rather than requiring an
 * {@code EntityRendererProvider.Context} constructor argument, to match this class's existing 2/3-arg constructor
 * call sites ({@code RenderSculptor.java}: {@code new ItemLayerSculptorStaff(this, "backItem")}, no context passed).
 * The old dead/commented-out {@code render(...)}/{@code renderRecursively(...)} body (superseded by
 * {@code renderForBone} even before this port) was dropped entirely rather than translated.
 */
public class GeckoItemlayer<T extends MowzieGeckoEntity, R extends GeoRenderState> extends GeoRenderLayer<T, Void, R> {
    private static final DataTicket<List<GeckoItemlayer.BoneItem>> CONTENTS = DataTicket.create(
            "mowziesmobs_geckoitemlayer_contents", new TypeToken<List<GeckoItemlayer.BoneItem>>() {});

    protected final ItemModelResolver itemModelResolver = Minecraft.getInstance().getItemModelResolver();
    protected String boneName;
    protected ItemStack renderedItem;

    public GeckoItemlayer(GeoRenderer<T, Void, R> rendererIn, String boneName, ItemStack renderedItem) {
        super(rendererIn);
        this.boneName = boneName;
        this.renderedItem = renderedItem;
    }

    protected ItemStack getStackForBone(String boneName, T animatable) {
        return this.boneName.equals(boneName) ? renderedItem : null;
    }

    protected ItemDisplayContext getTransformTypeForStack(String boneName, ItemStack stack, T animatable) {
        return switch (boneName) {
            default -> ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        };
    }

    @Override
    public void addRenderData(T animatable, @Nullable Void relatedObject, R renderState, float partialTick) {
        BakedGeoModel bakedModel = getDefaultBakedModel(renderState);
        List<GeckoItemlayer.BoneItem> contents = new ArrayList<>();

        for (GeoBone bone : bakedModel.boneLookup().get().values()) {
            if (bone.frameSnapshot != null && bone.frameSnapshot.isHidden()) continue;

            ItemStack stack = getStackForBone(bone.name(), animatable);
            if (stack != null && !stack.isEmpty()) {
                ItemDisplayContext displayContext = getTransformTypeForStack(bone.name(), stack, animatable);
                ItemStackRenderState stackState = RenderUtil.createRenderStateForItem(stack, itemModelResolver, displayContext, animatable);
                contents.add(new BoneItem(bone.name(), stackState));
            }
        }

        if (!contents.isEmpty()) {
            renderState.addGeckolibData(CONTENTS, contents);
        }
    }

    @Override
    public void addPerBoneRender(RenderPassInfo<R> renderPassInfo, BiConsumer<GeoBone, PerBoneRender<R>> consumer) {
        for (GeckoItemlayer.BoneItem boneItem : renderPassInfo.getOrDefaultGeckolibData(CONTENTS, List.of())) {
            renderPassInfo.model().getBone(boneItem.boneName())
                    .ifPresent(bone -> consumer.accept(bone, (rpi, b, renderTasks) -> submitItemStackRender(rpi, boneItem.stackState(), renderTasks)));
        }
    }

    protected void submitItemStackRender(RenderPassInfo<R> renderPassInfo, ItemStackRenderState stackState, SubmitNodeCollector renderTasks) {
        PoseStack poseStack = renderPassInfo.poseStack();
        int outlineColor = renderPassInfo.renderState() instanceof EntityRenderState entityRenderState ? entityRenderState.outlineColor : 0;
        stackState.submit(poseStack, renderTasks, renderPassInfo.packedLight(), OverlayTexture.NO_OVERLAY, outlineColor);
    }

    public Vec3 getRenderOffset(MowzieGeckoEntity p_114483_, float p_114484_) {
        return Vec3.ZERO;
    }

    protected record BoneItem(String boneName, ItemStackRenderState stackState) {
    }
}
