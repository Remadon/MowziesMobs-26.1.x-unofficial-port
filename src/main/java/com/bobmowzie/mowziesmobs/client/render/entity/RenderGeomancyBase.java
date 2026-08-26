package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.client.model.tools.geckolib.MowzieGeoModel;
import com.bobmowzie.mowziesmobs.server.entity.effects.geomancy.EntityGeomancyBase;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * PORTING NOTE: `MultiBufferSource` is no longer reachable from renderer-side code during a render pass in the new
 * deferred `SubmitNodeCollector` pipeline (see MowzieGeoEntityRenderer.java / porting report), so the old
 * `multiBufferSource` field/getter/setter (used only by {@code RenderPillar#renderCube}, which itself no longer has
 * a valid extension point - see that class) has been dropped. `preRender(...)` no longer has live-entity access at
 * all (see GeoRenderer architecture notes) - the live entity is now captured via {@link #addRenderData} instead,
 * which still runs once per render pass before rendering, same timing as the old `preRender` capture.
 */
public class RenderGeomancyBase<T extends EntityGeomancyBase & GeoEntity, R extends EntityRenderState> extends GeoEntityRenderer<T, R> {
    private static final Identifier TEXTURE_STONE = Identifier.withDefaultNamespace("textures/blocks/stone.png");
    private T entity;

    protected RenderGeomancyBase(EntityRendererProvider.Context context, MowzieGeoModel<T> modelProvider) {
        super(context, modelProvider);
    }

    @Override
    public Identifier getTextureLocation(R renderState) {
        return TEXTURE_STONE;
    }

    @Override
    public void addRenderData(T animatable, @Nullable Void relatedObject, R renderState, float partialTick) {
        this.entity = animatable;
    }

    public T getEntity() {
        return entity;
    }
}
