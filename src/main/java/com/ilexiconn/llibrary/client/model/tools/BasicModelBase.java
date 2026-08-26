package com.ilexiconn.llibrary.client.model.tools;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.function.Function;

/**
 * NOTE (26.1.2 port): this used to extend {@code net.minecraft.client.model.EntityModel<T>}. As of 26.1.2,
 * {@code net.minecraft.client.model.EntityModel<T extends EntityRenderState>} requires its type parameter to be a
 * render state, not a live {@link Entity}, and {@code net.minecraft.client.model.Model#renderToBuffer(PoseStack,
 * VertexConsumer, int, int, int)} (the method every LLibrary-based ModelXxx subclass overrides to draw its
 * hand-built {@link BasicModelRenderer}/{@link AdvancedModelRenderer} box tree) is now declared {@code final} on
 * that vanilla base class - it can no longer be overridden at all. Both facts made it impossible to keep extending
 * the vanilla class hierarchy without breaking either the generic bound or the override this library depends on for
 * actual rendering, so this class no longer extends any vanilla Minecraft class; it is a standalone base again, and
 * every ModelXxx subclass's existing {@code @Override renderToBuffer(...)} keeps compiling unchanged since it is
 * now overriding a plain (non-final) method declared below instead of the vanilla final one. See PORTING_NOTES.md
 * for the full writeup, including what this means for MobRenderer-based renderers of these models.
 */
public abstract class BasicModelBase<T extends Entity> {
    public int textureWidth = 64;
    public int textureHeight = 32;
    public final List<BasicModelRenderer> boxList = Lists.newArrayList();
    private final Function<Identifier, RenderType> renderType;

    protected BasicModelBase() {
        this(RenderTypes::entityCutout);
    }

    protected BasicModelBase(Function<Identifier, RenderType> renderType) {
        this.renderType = renderType;
    }

    public Function<Identifier, RenderType> renderType() {
        return this.renderType;
    }

    public RenderType renderType(Identifier texture) {
        return this.renderType.apply(texture);
    }

    public void accept(BasicModelRenderer modelRenderer) {
        boxList.add(modelRenderer);
    }

    public abstract void setupAnim(T p_102618_, float p_102619_, float p_102620_, float p_102621_, float p_102622_, float p_102623_);

    public void prepareMobModel(T p_102614_, float p_102615_, float p_102616_, float p_102617_) {
    }

    /**
     * Renders this model's geometry. Every concrete ModelXxx subclass is expected to override this (as it always
     * has) to call {@code render(...)} on its own top-level {@link BasicModelRenderer}/{@link AdvancedModelRenderer}
     * fields (which cascade into their children themselves) - this base implementation is an intentional no-op
     * fallback, NOT a generic "render everything in boxList" implementation, because {@link #boxList} is a flat list
     * containing every box (parents AND children) registered during model construction, so rendering all of it here
     * would double-render every child box on top of the parent-driven cascade.
     */
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int lightCoords, int overlayCoords, int color) {
    }

    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int lightCoords, int overlayCoords) {
        this.renderToBuffer(poseStack, buffer, lightCoords, overlayCoords, -1);
    }
}
