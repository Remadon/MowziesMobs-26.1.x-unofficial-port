package com.bobmowzie.mowziesmobs.client.particle;

import com.bobmowzie.mowziesmobs.client.model.tools.MathUtils;
import com.bobmowzie.mowziesmobs.client.render.MMRenderType;
import com.bobmowzie.mowziesmobs.server.message.NetworkHandler;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

/**
 * Created by BobMowzie on 6/2/2017.
 */
public class ParticleRing extends SingleQuadParticle {
    public float r, g, b;
    public float opacity;
    public boolean facesCamera;
    public float yaw, pitch;
    public float size;

    private final EnumRingBehavior behavior;

    public enum EnumRingBehavior implements StringRepresentable {
        SHRINK("shrink"),
        GROW("grow"),
        CONSTANT("constant"),
        GROW_THEN_SHRINK("grow_then_shrink");

        private final String key;

        EnumRingBehavior(final String key) {
            this.key = key;
        }

        @Override
        public String getSerializedName() {
            return key;
        }
    }

    public ParticleRing(ClientLevel world, double x, double y, double z, double motionX, double motionY, double motionZ, float yaw, float pitch, int duration, float r, float g, float b, float opacity, float size, boolean facesCamera, EnumRingBehavior behavior) {
        super(world, x, y, z, null);
        setSize(1, 1);
        this.size = size * 0.1f;
        lifetime = duration;
        alpha = 1;
        this.r = r;
        this.g = g;
        this.b = b;
        this.opacity = opacity;
        this.yaw = yaw;
        this.pitch = pitch;
        this.facesCamera = facesCamera;
        this.xd = motionX;
        this.yd = motionY;
        this.zd = motionZ;
        this.behavior = behavior;
    }

    @Override
    public int getLightCoords(float delta) {
        return 240 | super.getLightCoords(delta) & 0xFF0000;
    }

    @Override
    public void tick() {
        super.tick();
        if (age >= lifetime) {
            remove();
        }
        age++;
    }

    // NB: render(VertexConsumer, Camera, float) no longer exists in this MC version. The old override built
    // its own quad (rotated by a custom quaternion) and wrote vertices directly into a VertexConsumer; that
    // is now done by SingleQuadParticle#extractRotatedQuad, which takes the same "rotation quaternion + world
    // position" inputs the old code was computing by hand, so the quaternion/position math below is preserved
    // verbatim and simply handed off to that helper instead of building vertices manually.
    @Override
    public void extract(QuadParticleRenderState particleTypeRenderState, Camera renderInfo, float partialTicks) {
        float var = (age + partialTicks)/lifetime;
        if (behavior == EnumRingBehavior.GROW) {
            quadSize = size * var;
        }
        else if (behavior == EnumRingBehavior.SHRINK) {
            quadSize = size * (1 - var);
        }
        else if (behavior == EnumRingBehavior.GROW_THEN_SHRINK) {
            quadSize = (float) (size * (1 - var - Math.pow(2000, -var)));
        }
        else {
            quadSize = size;
        }
        alpha = opacity * 0.95f * (1 - (age + partialTicks)/lifetime) + 0.05f;
        rCol = r;
        gCol = g;
        bCol = b;

        Vec3 Vector3d = renderInfo.position();
        float f = (float)(Mth.lerp(partialTicks, this.xo, this.x) - Vector3d.x());
        float f1 = (float)(Mth.lerp(partialTicks, this.yo, this.y) - Vector3d.y());
        float f2 = (float)(Mth.lerp(partialTicks, this.zo, this.z) - Vector3d.z());
        Quaternionf quaternionf = new Quaternionf(0.0F, 0.0F, 0.0F, 1.0F);
        if (facesCamera) {
            if (this.roll == 0.0F) {
                quaternionf = renderInfo.rotation();
            } else {
                quaternionf = new Quaternionf(renderInfo.rotation());
                float f3 = Mth.lerp(partialTicks, this.oRoll, this.roll);
                quaternionf.mul(Axis.ZP.rotation(f3));
            }
        }
        else {
            Quaternionf quatX = MathUtils.quatFromRotationXYZ(pitch, 0, 0, false);
            Quaternionf quatY = MathUtils.quatFromRotationXYZ(0, yaw, 0, false);
            quaternionf.mul(quatY);
            quaternionf.mul(quatX);
        }

        this.extractRotatedQuad(particleTypeRenderState, quaternionf, f, f1, f2, partialTicks);
    }

    @Override
    public ParticleRenderType getGroup() {
        return ParticleRenderType.SINGLE_QUADS;
    }

    // TODO(out of scope): see MMRenderType.PARTICLE_LAYER_TRANSLUCENT_NO_DEPTH note in ParticleSparkle.java.
    @Override
    public SingleQuadParticle.Layer getLayer() {
        return MMRenderType.PARTICLE_LAYER_TRANSLUCENT_NO_DEPTH;
    }

    public static final class Provider implements ParticleProvider<Data> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet sprite) {
            this.spriteSet = sprite;
        }

        @Override
        public Particle createParticle(Data typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, RandomSource random) {
            ParticleRing particle = new ParticleRing(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, typeIn.yaw(), typeIn.pitch(), typeIn.duration(), typeIn.red(), typeIn.green(), typeIn.blue(), typeIn.alpha(), typeIn.scale(), typeIn.facesCamera(), typeIn.behavior());
            particle.setSpriteFromAge(spriteSet);
            return particle;
        }
    }

    public record Data(float red, float green, float blue, float alpha, float yaw, float pitch, float scale, int duration, boolean facesCamera, EnumRingBehavior behavior) implements ParticleOptions {
        public static final Codec<ParticleRing.EnumRingBehavior> BEHAVIOUR_CODEC = StringRepresentable.fromEnum(ParticleRing.EnumRingBehavior::values);

        public static final MapCodec<ParticleRing.Data> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        Codec.FLOAT.fieldOf("red").forGetter(ParticleRing.Data::red),
                        Codec.FLOAT.fieldOf("green").forGetter(ParticleRing.Data::green),
                        Codec.FLOAT.fieldOf("blue").forGetter(ParticleRing.Data::blue),
                        Codec.FLOAT.fieldOf("alpha").forGetter(ParticleRing.Data::alpha),
                        Codec.FLOAT.fieldOf("yaw").forGetter(ParticleRing.Data::yaw),
                        Codec.FLOAT.fieldOf("pitch").forGetter(ParticleRing.Data::pitch),
                        Codec.FLOAT.fieldOf("scale").forGetter(ParticleRing.Data::scale),
                        Codec.INT.fieldOf("duration").forGetter(ParticleRing.Data::duration),
                        Codec.BOOL.fieldOf("facesCamera").forGetter(ParticleRing.Data::facesCamera),
                        BEHAVIOUR_CODEC.fieldOf("behaviour").forGetter(ParticleRing.Data::behavior)
                ).apply(instance, ParticleRing.Data::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, ParticleRing.Data> STREAM_CODEC = NetworkHandler.composite(
                ByteBufCodecs.FLOAT, ParticleRing.Data::red,
                ByteBufCodecs.FLOAT, ParticleRing.Data::green,
                ByteBufCodecs.FLOAT, ParticleRing.Data::blue,
                ByteBufCodecs.FLOAT, ParticleRing.Data::alpha,
                ByteBufCodecs.FLOAT, ParticleRing.Data::yaw,
                ByteBufCodecs.FLOAT, ParticleRing.Data::pitch,
                ByteBufCodecs.FLOAT, ParticleRing.Data::scale,
                ByteBufCodecs.INT, ParticleRing.Data::duration,
                ByteBufCodecs.BOOL, ParticleRing.Data::facesCamera,
                NeoForgeStreamCodecs.enumCodec(EnumRingBehavior.class), ParticleRing.Data::behavior,
                ParticleRing.Data::new
        );

        @Override
        public @NotNull ParticleType<ParticleRing.Data> getType() {
            return ParticleHandler.RING.get();
        }
    }
}
