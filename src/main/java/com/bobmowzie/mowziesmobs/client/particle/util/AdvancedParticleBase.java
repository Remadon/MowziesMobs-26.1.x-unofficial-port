package com.bobmowzie.mowziesmobs.client.particle.util;

import com.bobmowzie.mowziesmobs.client.model.tools.MathUtils;
import com.bobmowzie.mowziesmobs.client.particle.ParticleRibbon;
import com.bobmowzie.mowziesmobs.client.particle.types.AdvancedParticleType;
import com.bobmowzie.mowziesmobs.client.render.MMRenderType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class AdvancedParticleBase extends SingleQuadParticle {
    public boolean doRender;

    public float airDrag;
    public float red, green, blue, alpha;
    public float prevRed, prevGreen, prevBlue, prevAlpha;
    public float scale, prevScale, particleScale;
    public ParticleRotation rotation;
    public boolean emissive;
    public double prevMotionX, prevMotionY, prevMotionZ;
    // Replaces the old `ParticleRenderType renderType` field: rendering group selection (SINGLE_QUADS vs.
    // ITEM_PICKUP/ELDER_GUARDIANS/NO_RENDER) and pipeline/blend-state selection (the old MMRenderType constant)
    // are two separate hooks now (getGroup()/getLayer()) - see those overrides below.
    public SingleQuadParticle.Layer layer = MMRenderType.PARTICLE_LAYER_TRANSLUCENT_NO_DEPTH;

    public ParticleComponent[] components;

    public ParticleRibbon ribbon;

    // NOTE: "screen space" particles (HUD-anchored quads built from a raw PoseStack transform, see
    // applyTransformToVerticesScreenSpace below and ParticleComponent.ScreenSpace) have no supported
    // rendering path in this MC version - see the extract() override for details. The fields/flag are
    // preserved so ParticleComponent.ScreenSpace and any dependent logic keep compiling and behaving the
    // same way up until the point of rendering; the particle will simply not draw anything while
    // isScreenSpace is true.
    public boolean isScreenSpace;
    public float screenX;
    public float screenY;
    public float screenZ;
    public float screenXo;
    public float screenYo;
    public float screenZo;

    protected AdvancedParticleBase(ClientLevel worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double motionX, double motionY, double motionZ, ParticleRotation rotation, double scale, double r, double g, double b, double a, double drag, double duration, boolean emissive, boolean canCollide, ParticleComponent[] components) {
        super(worldIn, xCoordIn, yCoordIn, zCoordIn, 0.0D, 0.0D, 0.0D, null);
        this.xd = motionX;
        this.yd = motionY;
        this.zd = motionZ;
        this.red = (float) (r);
        this.green = (float) (g);
        this.blue = (float) (b);
        this.alpha = (float) (a);
        this.scale = (float)scale;
        this.lifetime = (int)duration;
        airDrag = (float)drag;
        this.rotation = rotation;
        this.components = components;
        this.emissive = emissive;
        this.ribbon = null;

        doRender = true;

        for (ParticleComponent component : components) {
            component.init(this);
        }

        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.prevRed = this.red;
        this.prevGreen = this.green;
        this.prevBlue = this.blue;
        this.prevAlpha = this.alpha;
        this.rotation.setPrevValues();
        this.prevScale = this.scale;
        this.hasPhysics = canCollide;
        this.gravity = 0;
    }

    @Override
    public @NotNull ParticleRenderType getGroup() {
        return ParticleRenderType.SINGLE_QUADS;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return layer;
    }

    // The old render() computed its quad half-size locally as `particleScale * 0.1f` without ever consulting
    // getQuadSize(); the new extractRotatedQuad()/extract() pipeline reads the size through getQuadSize(), so
    // that formula now has to live here instead. `particleScale` is refreshed at the top of extract() before
    // this is consulted.
    @Override
    public float getQuadSize(float partialTicks) {
        return this.particleScale * 0.1f;
    }

    @Override
    public void setSprite(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
        super.setSprite(sprite);
    }

    public int getLightCoords(float partialTick)
    {
        int i = super.getLightCoords(partialTick);
        if (emissive) {
            int k = i >> 16 & 255;
            return 240 | k << 16;
        }
        else return i;
    }

    public ClientLevel getLevel() {
        return level;
    }

    @Override
    public void tick() {
        prevRed = red;
        prevGreen = green;
        prevBlue = blue;
        prevAlpha = alpha;
        prevScale = scale;
        rotation.setPrevValues();
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        prevMotionX = xd;
        prevMotionY = yd;
        prevMotionZ = zd;

        for (ParticleComponent component : components) {
            component.preUpdate(this);
        }

        if (this.age++ >= this.lifetime)
        {
            this.remove();
        }

        updatePosition();

        for (ParticleComponent component : components) {
            component.postUpdate(this);
        }

        if (ribbon != null) {
            ribbon.setPos(x, y, z);
            ribbon.positions[0] = new Vec3(x, y, z);
            ribbon.prevPositions[0] = getPrevPos();
        }
    }

    protected void updatePosition() {
        this.yd -= 0.04D * (double)this.gravity;
        this.move(this.xd, this.yd, this.zd);

        if (this.onGround && hasPhysics)
        {
            this.xd *= 0.699999988079071D;
            this.zd *= 0.699999988079071D;
        }

        this.xd *= airDrag;
        this.yd *= airDrag;
        this.zd *= airDrag;
    }

    public void setGravity(float gravity) {
        this.gravity = gravity;
    }

    // NB: Particle#render(VertexConsumer, Camera, float) and Particle#getRenderBoundingBox(float) no longer
    // exist in this MC version. Frustum culling of particles is now done directly against the particle's
    // (x, y, z) point (see QuadParticleGroup#extractRenderState), so there is no longer a bounding-box hook
    // for "always visible" (e.g. screen-space) particles to override - this is consistent with screen-space
    // particles not having a supported rendering path at all right now (see class-level note above).
    //
    // The quaternion-selection logic below (FaceCamera / EulerAngles / OrientVector) is preserved verbatim
    // from the old render() override; only the final vertex-emission step changed, from manually writing 4
    // vertices into a VertexConsumer to delegating to SingleQuadParticle#extractRotatedQuad, which performs
    // the equivalent rotate+scale+translate math internally from the same (quaternion, position, quadSize)
    // inputs.
    @Override
    public void extract(QuadParticleRenderState particleTypeRenderState, Camera renderInfo, float partialTicks) {
        alpha = prevAlpha + (alpha - prevAlpha) * partialTicks;
        rCol = prevRed + (red - prevRed) * partialTicks;
        gCol = prevGreen + (green - prevGreen) * partialTicks;
        bCol = prevBlue + (blue - prevBlue) * partialTicks;
        particleScale = prevScale + (scale - prevScale) * partialTicks;

        for (ParticleComponent component : components) {
            component.preRender(this, partialTicks);
        }
        if (alpha < 0.01) alpha = 0.0f;

        if (!doRender) return;

        // TODO: screen-space particles have no supported rendering path anymore (see class-level note) -
        // skip drawing rather than emitting an incorrect world-space quad.
        if (isScreenSpace) return;

        Quaternionf quaternion = new Quaternionf(0.0F, 0.0F, 0.0F, 1.0F);
        if (rotation instanceof ParticleRotation.FaceCamera faceCameraRot) {
            if (faceCameraRot.angle == 0.0F && faceCameraRot.prevAngle == 0.0F) {
                quaternion = renderInfo.rotation();
            } else {
                quaternion = new Quaternionf(renderInfo.rotation());
                float f3 = Mth.lerp(partialTicks, faceCameraRot.prevAngle, faceCameraRot.angle);
                quaternion.mul(Axis.ZP.rotation(f3));
            }
        }
        else if (rotation instanceof ParticleRotation.EulerAngles eulerRot) {
            float rotX = eulerRot.prevPitch + (eulerRot.pitch - eulerRot.prevPitch) * partialTicks;
            float rotY = eulerRot.prevYaw + (eulerRot.yaw - eulerRot.prevYaw) * partialTicks;
            float rotZ = eulerRot.prevRoll + (eulerRot.roll - eulerRot.prevRoll) * partialTicks;
            Quaternionf quatX = MathUtils.quatFromRotationXYZ(rotX, 0, 0, false);
            Quaternionf quatY = MathUtils.quatFromRotationXYZ(0, rotY, 0, false);
            Quaternionf quatZ = MathUtils.quatFromRotationXYZ(0, 0, rotZ, false);
            quaternion.mul(quatZ);
            quaternion.mul(quatY);
            quaternion.mul(quatX);
        }
        if (rotation instanceof ParticleRotation.OrientVector orientRot) {
            double x = orientRot.prevOrientation.x + (orientRot.orientation.x - orientRot.prevOrientation.x) * partialTicks;
            double y = orientRot.prevOrientation.y + (orientRot.orientation.y - orientRot.prevOrientation.y) * partialTicks;
            double z = orientRot.prevOrientation.z + (orientRot.orientation.z - orientRot.prevOrientation.z) * partialTicks;
            float pitch = (float) Math.asin(-y);
            float yaw = (float) (Mth.atan2(x, z));
            Quaternionf quatX = MathUtils.quatFromRotationXYZ(pitch, 0, 0, false);
            Quaternionf quatY = MathUtils.quatFromRotationXYZ(0, yaw, 0, false);
            quaternion.mul(quatY);
            quaternion.mul(quatX);
        }

        Vec3 cameraPos = renderInfo.position();
        float f = (float)(Mth.lerp(partialTicks, this.xo, this.x) - cameraPos.x());
        float f1 = (float)(Mth.lerp(partialTicks, this.yo, this.y) - cameraPos.y());
        float f2 = (float)(Mth.lerp(partialTicks, this.zo, this.z) - cameraPos.z());

        this.extractRotatedQuad(particleTypeRenderState, quaternion, f, f1, f2, partialTicks);

        // No VertexConsumer is available in the new extract()-based pipeline (see class-level note), so
        // ParticleComponent#postRender is invoked with `null` for it. As of this port, no ParticleComponent
        // implementation actually touches the buffer parameter, so this is safe; if a future component does,
        // it will need a different hook, since raw vertex-buffer access is no longer available here.
        for (ParticleComponent component : components) {
            component.postRender(this, null, renderInfo, partialTicks, this.getLightCoords(partialTicks));
        }
    }

    // Preserved for reference / potential future use: this was the old per-vertex screen-space transform used
    // when isScreenSpace was true. It is no longer invoked from extract() because a PoseStack-based transform
    // (which includes a (1,1,-1) reflection, not just rotation+uniform scale) cannot be represented by
    // QuadParticleRenderState#add(), which only accepts a world position + rotation quaternion + uniform
    // scale per quad. See the class-level note above.
    protected void applyTransformToVerticesScreenSpace(Camera renderInfo, float partialTicks, Vector3f[] avector3f, Quaternionf quaternion, float scale) {
        PoseStack posestack = new PoseStack();
        posestack.mulPose(renderInfo.rotation());
        posestack.scale(1.0F, 1.0F, -1.0F);
        posestack.translate(0.0F, -1.101F, 1.5F);
        float f = Mth.lerp(partialTicks, this.screenXo, this.screenX);
        float f1 = Mth.lerp(partialTicks, this.screenYo, this.screenY);
        float f2 = Mth.lerp(partialTicks, this.screenZo, this.screenZ);
        posestack.translate(f, f1, f2);
        for(int i = 0; i < 4; ++i) {
            Vector3f vector3f = avector3f[i];
//            quaternion.transform(vector3f);
            vector3f.mul(scale);
            Vector4f vec4 = new Vector4f(vector3f.x, vector3f.y, vector3f.z, 1);
            vec4.mul(posestack.last().pose());
            vector3f.set(vec4.x, vec4.y, vec4.z);
//            vector3f.add(f, f1, f2);
        }
    }

    public float getAge() {
        return age;
    }

    public double getPosX() {
        return x;
    }

    public void setPosX(double posX) {
        setPos(posX, this.y, this.z);
    }

    public double getPosY() {
        return y;
    }

    public void setPosY(double posY) {
        setPos(this.x, posY, this.z);
    }

    public double getPosZ() {
        return z;
    }

    public void setPosZ(double posZ) {
        setPos(this.x, this.y, posZ);
    }

    public double getMotionX() {
        return xd;
    }

    public void setMotionX(double motionX) {
        this.xd = motionX;
    }

    public double getMotionY() {
        return yd;
    }

    public void setMotionY(double motionY) {
        this.yd = motionY;
    }

    public double getMotionZ() {
        return zd;
    }

    public void setMotionZ(double motionZ) {
        this.zd = motionZ;
    }

    public Vec3 getPrevPos() {
        return new Vec3(xo, yo, zo);
    }

    public double getPrevPosX() {
        return xo;
    }

    public double getPrevPosY() {
        return yo;
    }

    public double getPrevPosZ() {
        return zo;
    }

    public static class Factory implements ParticleProvider<AdvancedParticleType> {
        private final SpriteSet spriteSet;

        public Factory(SpriteSet sprite) {
            this.spriteSet = sprite;
        }

        @Override
        public Particle createParticle(AdvancedParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, RandomSource random) {
            AdvancedParticleBase particle = new AdvancedParticleBase(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, typeIn.rotation(), typeIn.scale(), typeIn.red(), typeIn.green(), typeIn.blue(), typeIn.alpha(), typeIn.airDrag(), typeIn.duration(), typeIn.emissive(), typeIn.canCollide(), typeIn.components());
            particle.setColor(typeIn.red(), typeIn.green(), typeIn.blue());
            particle.setAlpha(typeIn.alpha());
            particle.setSprite(spriteSet.get(random));
            return particle;
        }
    }

    public static void spawnParticle(Level world, Holder<ParticleType<?>> particle, double x, double y, double z, double motionX, double motionY, double motionZ, boolean faceCamera, double yaw, double pitch, double roll, double faceCameraAngle, double scale, double r, double g, double b, double a, double drag, double duration, boolean emissive, boolean canCollide) {
        spawnParticle(world, particle, x, y, z, motionX, motionY, motionZ, faceCamera, yaw, pitch, roll, faceCameraAngle, scale, r, g, b, a, drag, duration, emissive, canCollide, new ParticleComponent[]{});
    }

    public static void spawnParticle(Level world, Holder<ParticleType<?>> particle, double x, double y, double z, double motionX, double motionY, double motionZ, boolean faceCamera, double yaw, double pitch, double roll, double faceCameraAngle, double scale, double red, double green, double blue, double alpha, double airDrag, double duration, boolean emissive, boolean canCollide, ParticleComponent[] components) {
        ParticleRotation rotation = faceCamera ? new ParticleRotation.FaceCamera((float) faceCameraAngle) : new ParticleRotation.EulerAngles((float)yaw, (float)pitch, (float)roll);
        world.addParticle(new AdvancedParticleType(particle, rotation, components, (float) red, (float) green, (float) blue, (float) alpha, (float) scale, (float) duration, (float) airDrag, emissive, canCollide), x, y, z, motionX, motionY, motionZ);
    }

    public static void spawnParticle(Level world, Holder<ParticleType<?>> particle, double x, double y, double z, double motionX, double motionY, double motionZ, ParticleRotation rotation, double scale, double red, double green, double blue, double alpha, double airDrag, double duration, boolean emissive, boolean canCollide, ParticleComponent[] components) {
        world.addParticle(new AdvancedParticleType(particle, rotation, components, (float) red, (float) green, (float) blue, (float) alpha, (float) scale, (float) duration, (float) airDrag, emissive, canCollide), x, y, z, motionX, motionY, motionZ);
    }

    public static void spawnAlwaysVisibleParticle(Level world, Holder<ParticleType<?>> particle, double distanceLimit, double x, double y, double z, double motionX, double motionY, double motionZ, ParticleRotation rotation, double scale, double red, double green, double blue, double alpha, double airDrag, double duration, boolean emissive, boolean canCollide, ParticleComponent[] components) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        boolean overrideLimiter = camera.position().distanceToSqr(x, y, z) < distanceLimit * distanceLimit;
        world.addAlwaysVisibleParticle(new AdvancedParticleType(particle, rotation, components, (float) red, (float) green, (float) blue, (float) alpha, (float) scale, (float) duration, (float) airDrag, emissive, canCollide), overrideLimiter, x, y, z, motionX, motionY, motionZ);
    }

    public static void spawnAlwaysVisibleParticle(Level world, Holder<ParticleType<?>> particle, double distanceLimit, double x, double y, double z, double motionX, double motionY, double motionZ, boolean faceCamera, double yaw, double pitch, double roll, double faceCameraAngle, double scale, double red, double green, double blue, double alpha, double airDrag, double duration, boolean emissive, boolean canCollide, ParticleComponent[] components) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        boolean overrideLimiter = camera.position().distanceToSqr(x, y, z) < distanceLimit * distanceLimit;
        ParticleRotation rotation = faceCamera ? new ParticleRotation.FaceCamera((float) faceCameraAngle) : new ParticleRotation.EulerAngles((float)yaw, (float)pitch, (float)roll);
        world.addAlwaysVisibleParticle(new AdvancedParticleType(particle, rotation, components, (float) red, (float) green, (float) blue, (float) alpha, (float) scale, (float) duration, (float) airDrag, emissive, canCollide), overrideLimiter, x, y, z, motionX, motionY, motionZ);
    }
}
