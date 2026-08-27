package com.bobmowzie.mowziesmobs.client.particle;

import com.bobmowzie.mowziesmobs.client.particle.types.AdvancedParticleType;
import com.bobmowzie.mowziesmobs.client.particle.types.DecalParticleType;
import com.bobmowzie.mowziesmobs.client.particle.util.AdvancedParticleBase;
import com.bobmowzie.mowziesmobs.client.particle.util.ParticleComponent;
import com.bobmowzie.mowziesmobs.client.particle.util.ParticleRotation;
import com.bobmowzie.mowziesmobs.client.render.MMRenderType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

// PORTING NOTE (26.1.2): the original render() projected an arbitrary quad per underlying block position (an
// AABB region can span several blocks), clipped to each block's own collision shape - true per-block ground
// conforming, needed if a decal ever spans uneven terrain. The new particle rendering pipeline
// (QuadParticleRenderState#add(), reached via SingleQuadParticle#extract()) only accepts "world position +
// rotation quaternion + one uniform scale" per call - i.e. exactly one rotated *square*, with no raw
// VertexConsumer access - so that per-block clipping can't be expressed. In practice every caller of
// spawnDecal (footprints, the geomancy ground crack) uses a decal small enough to land on a single block almost
// all the time, so this is now just a flat single quad instead: rotation is fixed EulerAngles (yaw = the
// caller's angle, pitch = 90 degrees to lay the vertical billboard quad down flat) rather than FaceCamera, so
// it doesn't billboard to the camera like a normal particle - it lies on the ground and stays there.
// getQuadSize() reproduces the original's exact world-space half-extent formula (particleScale * spriteScale *
// sqrt(2)) so the visible size matches the pre-port mod. MMRenderType.PARTICLE_LAYER_DECAL (cull disabled) is
// used instead of the shared default particle layer so the quad is visible from below too, since there's no
// need to work out which winding order faces "up" when cull is just off. If a decal ever needs to conform to
// genuinely uneven multi-block terrain, that would require a custom net.minecraft.client.particle.ParticleGroup
// (registered via NeoForge's RegisterParticleGroupsEvent) with its own renderer that can write arbitrary quads -
// out of scope here since no current caller needs it.
public class ParticleDecal extends AdvancedParticleBase {
    protected int spriteSize = 8;
    protected int bufferSize = 32;
    private final SpriteSet sprites;

    protected ParticleDecal(ClientLevel worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double motionX, double motionY, double motionZ, ParticleRotation rotation, double scale, double r, double g, double b, double a, double drag, double duration, boolean emissive, SpriteSet sprites, ParticleComponent[] components) {
        this(worldIn, xCoordIn, yCoordIn, zCoordIn, motionX, motionY, motionZ, rotation, scale, r, g, b, a, drag, duration, emissive, sprites, 8, 32, components);
    }

    protected ParticleDecal(ClientLevel worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double motionX, double motionY, double motionZ, ParticleRotation rotation, double scale, double r, double g, double b, double a, double drag, double duration, boolean emissive, SpriteSet sprites, int spriteSize, int bufferSize, ParticleComponent[] components) {
        super(worldIn, xCoordIn, yCoordIn, zCoordIn, motionX, motionY, motionZ, rotation, scale, r, g, b, a, drag, duration, emissive, false, components);
        this.spriteSize = spriteSize;
        this.bufferSize = bufferSize;
        this.layer = MMRenderType.PARTICLE_LAYER_DECAL;
        this.setSpriteFromAge(sprites);
        this.sprites = sprites;
    }

    @Override
    public void tick() {
        super.tick();
        this.setSprite(sprites.get(Math.min(this.age, 5), 5));
    }

    // The original render() only ever drew the central (spriteSize/bufferSize)-sized crop of the texture (e.g.
    // strix_footprint.png is a 32x32 canvas with the actual footprint mark drawn in roughly an 8x8 patch at its
    // center - confirmed by inspecting the PNG's alpha bounding box) at a world half-extent of
    // particleScale * spriteScale * sqrt(2). extractRotatedQuad() always maps the *whole* sprite (u0..u1/v0..v1,
    // i.e. the full padded canvas) onto getQuadSize(), not just that inner crop, so to make the visible mark
    // come out at the same world size as before, the world-units-per-uv-fraction ratio has to be preserved
    // instead of the crop's absolute size: world_width(crop) / uv_fraction_width(crop) works out to a constant
    // 2 * particleScale independent of spriteScale (it cancels out - both the crop's world size and its uv
    // fraction scale by the same spriteScale * sqrt(2) term), so mapping the full uv range (fraction = 1) at
    // that same ratio gives a half-extent of exactly particleScale, not particleScale * spriteScale * sqrt(2).
    @Override
    public float getQuadSize(float partialTicks) {
        return this.particleScale;
    }

    public static class Provider implements ParticleProvider<DecalParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet sprite) {
            this.spriteSet = sprite;
        }

        @Override
        public Particle createParticle(DecalParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, RandomSource random) {
            ParticleDecal particle = new ParticleDecal(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, typeIn.rotation(), typeIn.scale(), typeIn.red(), typeIn.green(), typeIn.blue(), typeIn.alpha(), typeIn.airDrag(), typeIn.duration(), typeIn.emissive(), spriteSet, typeIn.spriteSize(), typeIn.bufferSize(), typeIn.components());
            particle.setColor(typeIn.red(), typeIn.green(), typeIn.blue());
            return particle;
        }
    }

    public static void spawnDecal(Level world, Holder<ParticleType<?>> particle, double x, double y, double z, double motionX, double motionY, double motionZ, double angle, double scale, double red, double green, double blue, double alpha, double airDrag, double duration, boolean emissive, int spriteSize, int bufferSize, ParticleComponent[] components) {
        // yaw = angle (matches the ground-projection "decalRot" the original used this value for), pitch = 90
        // degrees to lay the normally-vertical billboard quad flat on the ground - see class-level comment.
        ParticleRotation rotation = new ParticleRotation.EulerAngles((float) angle, (float) (Math.PI / 2.0), 0.0F);
        AdvancedParticleType base = new AdvancedParticleType(particle, rotation, components, (float) red, (float) green, (float) blue, (float) alpha, (float) scale, (float) duration, (float) airDrag, emissive, false);
        world.addParticle(new DecalParticleType(base, spriteSize, bufferSize), x, y, z, motionX, motionY, motionZ);
    }
}
