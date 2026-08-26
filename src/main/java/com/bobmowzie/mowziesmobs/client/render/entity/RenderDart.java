package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.server.entity.EntityDart;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;

/**
 * PORTING NOTE: {@code ArrowRenderer<T extends AbstractArrow, S extends ArrowRenderState>} gained a second render
 * state type parameter (see PORTING_NOTES.md architecture section) and {@code getTextureLocation} now takes the
 * render state instead of the live entity - {@code ArrowRenderState} itself is concrete/non-abstract and carries
 * nothing this renderer needs beyond the base class's own fields, so it's used directly as {@code S}.
 */
public class RenderDart extends ArrowRenderer<EntityDart, ArrowRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/entity/dart.png");

    public RenderDart(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

    @Override
    public ArrowRenderState createRenderState() {
        return new ArrowRenderState();
    }

    @Override
    protected Identifier getTextureLocation(ArrowRenderState state) {
        return TEXTURE;
    }
}
