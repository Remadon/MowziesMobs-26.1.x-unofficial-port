package com.bobmowzie.mowziesmobs.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;

/**
 * PORTING NOTE (see PORTING_NOTES.md "Plain vanilla EntityRenderer<T> subclasses" section): the old override
 * completely replaced vanilla's default render behavior with a no-op (not even a nametag) - {@link #submit} is left
 * as a no-op override (not calling {@code super.submit}) to preserve that exactly.
 */
public class RenderNothing extends EntityRenderer<Entity, EntityRenderState> {
    public RenderNothing(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState cameraState) {
        // Intentionally empty - matches the old render() override, which rendered nothing at all (no nametag either).
    }
}
