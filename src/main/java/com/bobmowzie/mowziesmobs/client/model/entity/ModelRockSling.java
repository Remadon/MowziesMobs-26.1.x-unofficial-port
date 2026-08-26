package com.bobmowzie.mowziesmobs.client.model.entity;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.server.entity.effects.geomancy.EntityRockSling;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.constant.DataTickets;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;
import java.util.TreeMap;

public class ModelRockSling extends GeoModel<EntityRockSling> {
    static Map<String, Identifier> texMap;
    private static final Identifier TEXTURE_DIRT = Identifier.withDefaultNamespace("textures/block/dirt.png");
    private static final Identifier TEXTURE_STONE = Identifier.withDefaultNamespace("textures/block/stone.png");
    private static final Identifier TEXTURE_SANDSTONE = Identifier.withDefaultNamespace("textures/block/sandstone.png");
    private static final Identifier TEXTURE_CLAY = Identifier.withDefaultNamespace("textures/block/clay.png");

    // PORTING NOTE: getTextureResource(GeoRenderState) no longer receives the live animatable (needed here to read
    // entity.storedBlock), so the resolved texture is captured up front via addAdditionalStateData - same pattern
    // as ModelElokosa/ModelUmvuthana.
    private static final DataTicket<Identifier> RESOLVED_TEXTURE = DataTickets.create("mowziesmobs_rock_sling_texture", Identifier.class);

    public ModelRockSling(){
        super();
        texMap = new TreeMap<>();
        texMap.put(Blocks.STONE.getDescriptionId(), TEXTURE_STONE);
        texMap.put(Blocks.DIRT.getDescriptionId(), TEXTURE_DIRT);
        texMap.put(Blocks.CLAY.getDescriptionId(), TEXTURE_CLAY);
        texMap.put(Blocks.SANDSTONE.getDescriptionId(), TEXTURE_SANDSTONE);
    }

    @Override
    public void addAdditionalStateData(EntityRockSling animatable, Object relatedObject, GeoRenderState renderState) {
        Identifier tex = TEXTURE_DIRT;
        if (animatable.storedBlock != null) {
            Identifier mapped = texMap.get(animatable.storedBlock.getBlock().getDescriptionId());
            if (mapped != null) tex = mapped;
        }
        renderState.addGeckolibData(RESOLVED_TEXTURE, tex);
    }

    @Override
    public Identifier getAnimationResource(EntityRockSling entity) {
        return Identifier.fromNamespaceAndPath(MMCommon.MODID, "rock_sling");
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(MMCommon.MODID, "rock_sling");

    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        Identifier tex = renderState.getGeckolibData(RESOLVED_TEXTURE);
        return tex != null ? tex : TEXTURE_DIRT;
    }

    // PORTING NOTE: no longer @Override - GeoModel has no getRenderType method anymore in GeckoLib 5 (this moved to
    // the renderer, e.g. GeoEntityRenderer#getRenderType(T, Identifier, ...) - see MowzieGeoEntityRenderer/
    // RenderRockSling in client/render/**, out of this agent's scope). Kept as a plain method with the same
    // name/logic so the render agent can delegate to it if useful.
    public RenderType getRenderType(EntityRockSling animatable, Identifier texture) {
        return RenderTypes.entityCutoutCull(texture);
    }
}
