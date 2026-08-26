package com.bobmowzie.mowziesmobs.client.render.entity.layer;

import com.bobmowzie.mowziesmobs.client.render.MowzieRenderUtils;
import com.ilexiconn.llibrary.client.model.tools.AdvancedModelRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * PORTING NOTE (26.1.2): dropped {@code extends RenderLayer<T, M>} entirely - it can no longer be satisfied here.
 * Vanilla's new bound is {@code RenderLayer<S extends EntityRenderState, M extends EntityModel<? super S>>}, but the
 * only two real call sites ({@code RenderFrostmaw}, {@code RenderWroughtnaut}) use LLibrary
 * {@code AdvancedModelBase}-family models, which no longer extend vanilla {@code EntityModel} at all (see
 * PORTING_NOTES.md "BasicModelBase/AdvancedModelBase can no longer extend vanilla EntityModel<T>" - a hard
 * architectural fact, not a design choice) - so no real {@code M} could ever satisfy that bound for this class
 * anyway. This mirrors the same conclusion already reached for the top-level renderers that host these models: they
 * are being ported away from {@code MobRenderer}/{@code addLayer} to plain {@code EntityRenderer<T, XRenderState>}
 * subclasses with a custom render state holding a live entity reference (see PORTING_NOTES.md "the actual chosen
 * path was to port them to plain EntityRenderer<T, XRenderState> subclasses instead").
 * <p>
 * <b>Cross-scope note</b>: as of this writing, neither {@code RenderFrostmaw.java} nor {@code RenderWroughtnaut.java}
 * (both out of this agent's scope - {@code client/render/entity/**}, not {@code layer/**}) has been ported yet -
 * both are still on the old {@code MobRenderer<T,M>}/{@code addLayer(new ItemLayer<>(this, ...))} shape. This class
 * is written against the render-state-driven shape they will need to adopt (a {@code Function<S, ? extends
 * LivingEntity>} extracting the live entity from whatever custom render state class they end up defining, since
 * different renderers will have different concrete state types) rather than guessing their exact future field names.
 * Once ported, the call sites become e.g. {@code new ItemLayer<>(state -> state.entity, getModel().iceCrystalHand,
 * ItemHandler.ICE_CRYSTAL.get().getDefaultInstance(), ItemDisplayContext.GROUND)} and {@link #submit} invoked
 * manually from that renderer's own {@code submit(...)} override (there is no {@code addLayer}/layers list on plain
 * {@code EntityRenderer} - only {@code LivingEntityRenderer} has one, and these renderers can no longer be
 * {@code LivingEntityRenderer}s either, per the same architectural fact above).
 * <p>
 * {@code ItemInHandRenderer#renderItem} also changed shape: dropped its {@code MultiBufferSource}/{@code boolean
 * leftHand} parameters in favor of {@code SubmitNodeCollector} (confirmed via the real 26.1.2 source - new signature
 * is {@code renderItem(LivingEntity, ItemStack, ItemDisplayContext, PoseStack, SubmitNodeCollector, int)}).
 */
public class ItemLayer<S extends EntityRenderState, T extends LivingEntity> {
    private final Function<S, T> entityGetter;
    private AdvancedModelRenderer modelRenderer;
    // PORTING NOTE (26.1.2): the ItemStack is now resolved lazily (cached on first use) instead of eagerly in the
    // constructor - callers construct these renderer helper fields as part of their own field initializers, which
    // run during EntityRenderers#createEntityRenderers (a resource-manager reload happening before item data
    // components are bound), so eagerly calling Item#getDefaultInstance()/ItemStack construction here at that point
    // throws "Components not bound yet" (confirmed at runtime). Deferring to first real use (render time, well after
    // startup) sidesteps this entirely.
    private Supplier<ItemStack> itemstackSupplier;
    private ItemStack itemstack;
    private final ItemDisplayContext transformType;

    public ItemLayer(Function<S, T> entityGetter, AdvancedModelRenderer modelRenderer, Supplier<ItemStack> itemstackSupplier, ItemDisplayContext transformType) {
        this.entityGetter = entityGetter;
        this.itemstackSupplier = itemstackSupplier;
        this.modelRenderer = modelRenderer;
        this.transformType = transformType;
    }

    public void submit(PoseStack matrixStackIn, SubmitNodeCollector renderTasks, int packedLightIn, S state) {
        if (!modelRenderer.showModel || modelRenderer.isHidden()) return;
        T entity = entityGetter.apply(state);
        if (entity == null) return;
        matrixStackIn.pushPose();
        MowzieRenderUtils.matrixStackFromModel(matrixStackIn, getModelRenderer());
        Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(entity, getItemstack(), transformType, matrixStackIn, renderTasks, packedLightIn);
        matrixStackIn.popPose();
    }

    public ItemStack getItemstack() {
        if (itemstack == null) {
            itemstack = itemstackSupplier.get();
            itemstackSupplier = null;
        }
        return itemstack;
    }

    public void setItemstack(ItemStack itemstack) {
        this.itemstack = itemstack;
        this.itemstackSupplier = null;
    }

    public AdvancedModelRenderer getModelRenderer() {
        return modelRenderer;
    }

    public void setModelRenderer(AdvancedModelRenderer modelRenderer) {
        this.modelRenderer = modelRenderer;
    }
}
