package com.bobmowzie.mowziesmobs.client.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

/**
 * PORTING NOTE: vanilla's `RenderType` construction API was completely rebuilt around a declarative
 * `RenderPipeline`/`RenderSetup` system (see PORTING_NOTES.md "RenderType/RenderTypes split" section) - the old
 * `RenderType.CompositeState`/`RenderStateShard` (`TextureStateShard`, `ShaderStateShard`, `TransparencyStateShard`,
 * etc) API this file used no longer exists at all. `highlight(...)` had an exact modern vanilla replacement
 * (`net.minecraft.client.renderer.rendertype.RenderTypes#energySwirl(Identifier, float, float)` matches it almost
 * field-for-field - offset-scrolling texture, lightmap, overlay, additive-ish energy shader) so callers of
 * `MMRenderType.highlight(...)` should be updated to call `RenderTypes.energySwirl(...)` directly (this method has
 * been removed from this class - grep for `MMRenderType.highlight` if any other file still references it and
 * update it to `RenderTypes.energySwirl`).
 * `getGlowingEffect`/`getSolarFlare` have no exact vanilla equivalent (both are bespoke unlit/translucent
 * beacon-beam-styled effects) - rebuilt here as custom `RenderPipeline`s cloned from vanilla's
 * `RenderPipelines.BEACON_BEAM_SNIPPET` (closest existing analog to the old `RENDERTYPE_BEACON_BEAM_SHADER`), with
 * the vertex format overridden to `DefaultVertexFormat.ENTITY` (closest modern equivalent of the old
 * `DefaultVertexFormat.NEW_ENTITY`, needed since the mod's own vertex-building code, e.g.
 * `RenderUmvuthi#drawVertex`, calls `.setOverlay(...)`/`.setLight(...)`, which BLOCK-format pipelines don't support).
 * **This is a best-effort reconstruction, not a verified 1:1 port - the exact GPU pipeline/shader compatibility of
 * pairing the beacon-beam shader with an overridden ENTITY vertex format could not be verified without running the
 * game; if the solar flare / glow effects render incorrectly or fail to draw, start here.**
 * <p>
 * Also note: this class used to `extends RenderType` (subclassing it purely to reach its protected/inherited
 * static shard/state constants like `TRANSLUCENT_TRANSPARENCY`, `NO_CULL`, etc, which no longer exist). The new
 * `RenderType`'s constructor is private (`RenderType.create(name, RenderSetup)` is the only way to build one), so
 * subclassing it is no longer possible or necessary - this is now a plain utility class.
 */
public abstract class MMRenderType {
    private static final RenderPipeline GLOW_EFFECT_PIPELINE = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
            .withLocation("pipeline/mowziesmobs_glow_effect")
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .build();

    private static final RenderPipeline SOLAR_FLARE_PIPELINE = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
            .withLocation("pipeline/mowziesmobs_solar_flare")
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withCull(false)
            .build();

    public static RenderType getGlowingEffect(Identifier locationIn) {
        RenderSetup renderSetup = RenderSetup.builder(GLOW_EFFECT_PIPELINE)
                .withTexture("Sampler0", locationIn)
                .useOverlay()
                .createRenderSetup();

        return RenderType.create("glow_effect", renderSetup);
    }

    public static RenderType getSolarFlare(Identifier locationIn) {
        RenderSetup renderSetup = RenderSetup.builder(SOLAR_FLARE_PIPELINE)
                .withTexture("Sampler0", locationIn)
                .createRenderSetup();

        return RenderType.create("solar_flare", renderSetup);
    }

    // PORTING NOTE (26.1.2, cross-agent request from the particle-subsystem agent): `ParticleRenderType` is now a
    // `public record ParticleRenderType(String name)` (see net/minecraft/client/particle/ParticleRenderType.java in
    // the vanilla tree) - implicitly final, can no longer be subclassed with `new ParticleRenderType() { ... }`.
    // Particle rendering itself moved to the same declarative RenderPipeline model as everything else in this port;
    // the modern equivalent of "a custom ParticleRenderType" is a `SingleQuadParticle.Layer(boolean translucent,
    // Identifier textureAtlasLocation, RenderPipeline pipeline)` record (see SingleQuadParticle.java in the vanilla
    // tree, and its own OPAQUE/TRANSLUCENT/OPAQUE_TERRAIN/TRANSLUCENT_TERRAIN constants for the pattern this
    // follows). Field names changed (PARTICLE_SHEET_TRANSLUCENT_NO_DEPTH -> PARTICLE_LAYER_TRANSLUCENT_NO_DEPTH,
    // TERRAIN_SHEET_NO_CULL -> TERRAIN_LAYER_NO_CULL) to match the new type; verified no other file under
    // client/render/** referenced the old names before renaming - the particle-subsystem files that consume these
    // (ParticleSparkle, ParticleSnowFlake, ParticleCloud, ParticleOrb, ParticleRing, AdvancedParticleBase,
    // AdvancedTerrainParticle) are out of this agent's scope and were updated by the particle-subsystem agent to use
    // the new names.
    private static final RenderPipeline PARTICLE_NO_DEPTH_PIPELINE = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
            .withLocation("pipeline/mowziesmobs_particle_no_depth")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false)) // no depth test, no depth write
            .build();
    public static final SingleQuadParticle.Layer PARTICLE_LAYER_TRANSLUCENT_NO_DEPTH =
            new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, PARTICLE_NO_DEPTH_PIPELINE);

    private static final RenderPipeline TERRAIN_NO_CULL_PIPELINE = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
            .withLocation("pipeline/mowziesmobs_terrain_no_cull")
            .withCull(false)
            .build();
    public static final SingleQuadParticle.Layer TERRAIN_LAYER_NO_CULL =
            new SingleQuadParticle.Layer(false, TextureAtlas.LOCATION_BLOCKS, TERRAIN_NO_CULL_PIPELINE);
}
