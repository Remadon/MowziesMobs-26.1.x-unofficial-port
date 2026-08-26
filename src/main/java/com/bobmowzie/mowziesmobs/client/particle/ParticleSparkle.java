package com.bobmowzie.mowziesmobs.client.particle;

import com.bobmowzie.mowziesmobs.client.render.MMRenderType;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * Created by BobMowzie on 6/2/2017.
 */
public class ParticleSparkle extends SingleQuadParticle {
    private final float red;
    private final float green;
    private final float blue;
    private final float scale;

    public ParticleSparkle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, double r, double g, double b, double scale, int duration) {
        super(world, x, y, z, vx, vy, vz, null);
        this.scale = (float) scale * 1f;
        lifetime = duration;
        xd = vx;
        yd = vy;
        zd = vz;
        red = (float) r;
        green = (float) g;
        blue = (float) b;
        hasPhysics = false;
    }

    @Override
    protected float getU1() {
        return super.getU1() - (super.getU1() - super.getU0())/16f;
    }

    @Override
    protected float getV1() {
        return super.getV1() - (super.getV1() - super.getV0())/16f;
    }

    // NB: Particle#render(VertexConsumer, Camera, float) no longer exists in this MC version - particle
    // geometry is now pushed into a QuadParticleRenderState via extract(...) instead of being written
    // directly into a VertexConsumer. This override computes the same per-frame alpha/quadSize the old
    // render() override did, then defers to SingleQuadParticle#extract for the actual quad emission.
    @Override
    public void extract(QuadParticleRenderState particleTypeRenderState, Camera renderInfo, float partialTicks) {
        float a = ((float)age + partialTicks)/lifetime;
        alpha = -4 * a * a + 4 * a;
        if (alpha < 0.01) alpha = 0.01f;
        quadSize = (-4 * a * a + 4 * a) * scale;

        super.extract(particleTypeRenderState, renderInfo, partialTicks);
    }

    @Override
    public ParticleRenderType getGroup() {
        return ParticleRenderType.SINGLE_QUADS;
    }

    // TODO(out of scope): MMRenderType (com.bobmowzie.mowziesmobs.client.render.MMRenderType) needs to expose
    // a `SingleQuadParticle.Layer PARTICLE_LAYER_TRANSLUCENT_NO_DEPTH` field (built from a custom RenderPipeline
    // with depth-testing disabled) to replace the old ParticleRenderType-based PARTICLE_SHEET_TRANSLUCENT_NO_DEPTH.
    // See final report for details - this will not compile until MMRenderType is updated.
    @Override
    public SingleQuadParticle.Layer getLayer() {
        return MMRenderType.PARTICLE_LAYER_TRANSLUCENT_NO_DEPTH;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet spriteSet;

        public Provider(SpriteSet sprite) {
            this.spriteSet = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, RandomSource random) {
            ParticleSparkle particle = new ParticleSparkle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, 1, 1, 1, 0.4d, 13);
            particle.setSprite(spriteSet.get(random));
            return particle;
        }
    }
}
