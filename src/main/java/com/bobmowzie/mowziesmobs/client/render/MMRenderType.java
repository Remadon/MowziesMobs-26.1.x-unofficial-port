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
    // FOLLOW-UP FIX: confirmed against the actual pre-port 1.21.1 source (MMRenderType#getGlowingEffect) that this
    // pipeline should test depth normally but not write it - the old code called
    // RenderType.CompositeState.builder()...setWriteMaskState(COLOR_WRITE)... with no setDepthTestState override
    // at all, i.e. depth test stays at the builder's default (normal LESS_THAN_OR_EQUAL) while only the depth
    // *write* is disabled (COLOR_WRITE = color yes, depth no). Without overriding the depth state at all, this
    // pipeline had instead inherited BEACON_BEAM_SNIPPET's DepthStencilState.DEFAULT (test LEQUAL, write *true*),
    // so each shell's near faces stamped the depth buffer and occluded shells/faces drawn after them instead of
    // blending - translucency looked broken/patchy. (A prior pass here tried CompareOp.ALWAYS_PASS - i.e.
    // disabling the depth test entirely - which is wrong: it matches SOLAR_FLARE_PIPELINE's old NO_DEPTH_TEST
    // behavior, not this pipeline's. With the test genuinely disabled, translucent geometry no longer gets
    // properly occluded by solid terrain/entities in front of it either - confirmed as the actual root cause of a
    // separate "screen-filling wash" bug on the particle pipeline below, which had the same wrong CompareOp.)
    private static final RenderPipeline GLOW_EFFECT_PIPELINE = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
            .withLocation("pipeline/mowziesmobs_glow_effect")
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
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
    // FOLLOW-UP FIX: confirmed against the actual pre-port 1.21.1 source (MMRenderType.PARTICLE_SHEET_TRANSLUCENT_NO_DEPTH's
    // ParticleRenderType#begin) that "NO_DEPTH" in this pipeline's name refers only to depth *write* being off -
    // the old code explicitly called RenderSystem.enableDepthTest() followed by RenderSystem.depthMask(false).
    // This was ported as CompareOp.ALWAYS_PASS (test disabled entirely) instead of the actual old behavior
    // (normal LESS_THAN_OR_EQUAL test, write disabled) - a reasonable misreading of the "NO_DEPTH" name, but wrong.
    // With the test genuinely disabled, ItemElokosaPaw's ground burst/glow particles (which legitimately grow
    // larger than the camera's distance from them, by design, unchanged since 1.21) drew straight through the
    // player's own body and the ground instead of being occluded by them like solid geometry - confirmed live via
    // a debug-instrumented build of the actual 1.21.1 client running side-by-side (git worktree), which reproduces
    // the exact same oversized-quad-vs-camera-distance numbers but renders a correctly occluded, gradiented ring
    // instead of a screen-filling flat wash. Restoring the normal depth test fixes both symptoms without touching
    // any of the mod's own scale/color values.
    private static final RenderPipeline PARTICLE_NO_DEPTH_PIPELINE = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
            .withLocation("pipeline/mowziesmobs_particle_no_depth")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false)) // depth test on, depth write off
            .build();
    public static final SingleQuadParticle.Layer PARTICLE_LAYER_TRANSLUCENT_NO_DEPTH =
            new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, PARTICLE_NO_DEPTH_PIPELINE);

    // FOLLOW-UP FIX: 26.1.2 composites translucent content from several separate render targets (main scene,
    // translucent entities, item entities, particles, weather, clouds - see LevelTargetBundle/transparency.json/
    // transparency.fsh in the vanilla tree) - a per-pixel depth-sort in that final compositing shader decides
    // whether each target's content blends over or under the others, using each target's own depth buffer. This
    // didn't exist at all in 1.21.1, which rendered particles directly into the single main framebuffer with
    // immediate alpha blending - there was no separate sort step to get wrong. Because PARTICLE_NO_DEPTH_PIPELINE
    // (correctly, per the fix above) never writes depth, the "particles" target's depth buffer holds only its
    // clear value wherever a particle draws color, instead of that particle's real distance from the camera -
    // giving the compositor's sort nothing meaningful to place it against the background with. For particles that
    // stay small and never get very close to the camera this goes unnoticed; ItemElokosaPaw's ground burst/glow
    // effects, which by design (unchanged since 1.21) grow larger than the camera's own distance from the player,
    // exposed it as the translucent effect blending as if it were flatly stamped over the background rather than
    // properly composited into the scene. Used only for those specific particle spawns (not the shared layer
    // above, which every other particle in the mod also uses) to avoid changing how unrelated, unaffected
    // particles blend against each other.
    private static final RenderPipeline PARTICLE_DEPTH_WRITE_PIPELINE = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
            .withLocation("pipeline/mowziesmobs_particle_depth_write")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true)) // depth test on, depth write on
            // Particles using this layer (e.g. the Paw ability's ring/burst) spawn with a fixed orientation
            // (faceCamera=false) rather than billboarding toward the camera, so they present a consistent
            // winding order regardless of view angle - viewed from the "back" side (e.g. looking straight
            // down at a horizontal disc) they'd otherwise be backface-culled and vanish entirely.
            .withCull(false)
            .build();
    public static final SingleQuadParticle.Layer PARTICLE_LAYER_TRANSLUCENT_DEPTH_WRITE =
            new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, PARTICLE_DEPTH_WRITE_PIPELINE);

    private static final RenderPipeline TERRAIN_NO_CULL_PIPELINE = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
            .withLocation("pipeline/mowziesmobs_terrain_no_cull")
            .withCull(false)
            .build();
    public static final SingleQuadParticle.Layer TERRAIN_LAYER_NO_CULL =
            new SingleQuadParticle.Layer(false, TextureAtlas.LOCATION_BLOCKS, TERRAIN_NO_CULL_PIPELINE);

    // Used by ParticleDecal (footprints, ground cracks): a flat quad laid on the ground, viewed from above in
    // normal play but still correct if the camera ever ends up below it (underground, spectator, etc) - unlike
    // the terrain/glow pipelines above, cull is disabled specifically so the quad doesn't need to guess which
    // winding order faces "up".
    private static final RenderPipeline DECAL_PIPELINE = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
            .withLocation("pipeline/mowziesmobs_decal")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .build();
    public static final SingleQuadParticle.Layer PARTICLE_LAYER_DECAL =
            new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, DECAL_PIPELINE);
}
