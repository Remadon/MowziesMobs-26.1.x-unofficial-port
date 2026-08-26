package com.bobmowzie.mowziesmobs.client.model.tools.geckolib;

import com.bobmowzie.mowziesmobs.client.model.tools.MathUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.GeoLocator;
import com.geckolib.cache.model.cuboid.CuboidGeoBone;
import com.geckolib.cache.model.cuboid.GeoCube;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector4f;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * PORTING NOTE (GeckoLib 4 -> 5 architecture change):
 * In GeckoLib 4.x, {@code GeoBone} itself carried mutable, per-instance pos/rot/scale state that model code could
 * read/write directly (and which the renderer read back to position cubes). In GeckoLib 5.x, {@code GeoBone} (and
 * its concrete subclass {@code CuboidGeoBone}) is purely a structural/immutable definition shared by every instance
 * of a given model, and the actual per-frame animated transform now lives in a separate, ephemeral
 * {@link com.geckolib.animation.state.BoneSnapshot} object that GeckoLib stores on {@link GeoBone#frameSnapshot}
 * for the duration of a single render pass (see {@code BoneSnapshots}/{@code GeoRenderer#adjustModelBonesForRender}).
 * <p>
 * This class is adapted to be a lightweight per-call WRAPPER around a real (shared, immutable) {@link GeoBone} plus
 * that bone's current {@link BoneSnapshot} (read from / lazily created on {@link GeoBone#frameSnapshot}, exactly
 * where GeckoLib's own renderer looks for it). This means: as long as something calls into this wrapper's
 * getters/setters while {@code bone.frameSnapshot} is the object actually consumed by rendering for that frame
 * (i.e. from within/after GeckoLib's {@code adjustModelBonesForRender} hook - which lives on the renderer, out of
 * this agent's scope), the procedural bone edits below will have real, visible effect. Wiring the call into that
 * render-time hook is NOT done here (client/render/** is owned by another agent) - see the porting report.
 */
public class MowzieGeoBone {

    public final GeoBone bone;

    private BoneSnapshot initialSnapshot;

    // GeckoLib 5's GeoBone is a shared, immutable singleton (no per-instance fields at all beyond structural data),
    // and this wrapper itself is disposable (a fresh one is constructed on every MowzieGeoModel#getMowzieBone(name)
    // call). So that Mowzie-specific extra bone state set through one wrapper instance is still visible through a
    // later wrapper instance for the *same* underlying bone (e.g. procedural code setting it, then the renderer -
    // out of this agent's scope - reading it back), all of it is tracked in side tables keyed by bone identity,
    // exactly like GeckoLib's own BoneSnapshot is keyed via GeoBone#frameSnapshot.
    private static final Set<GeoBone> DYNAMIC_JOINT_BONES = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<GeoBone, Matrix4f> ROTATION_OVERRIDES = new WeakHashMap<>();
    private static final Map<GeoBone, Boolean> INHERIT_ROTATION = new WeakHashMap<>();
    private static final Map<GeoBone, Boolean> INHERIT_TRANSLATION = new WeakHashMap<>();
    private static final Map<GeoBone, Boolean> FORCE_MATRIX_TRANSFORM = new WeakHashMap<>();
    private static final Map<GeoBone, Matrix4f> POSE = new WeakHashMap<>();

    public MowzieGeoBone(GeoBone bone) {
        this.bone = bone;
    }

    /**
     * PORTING NOTE (RESOLVED, see PORTING_NOTES.md "GeoBone wrapper vs render() - RESOLVED"): thin delegating
     * passthrough to the real {@link GeoBone#positionAndRender}. {@code GeoBone.render(...)} is abstract in
     * GeckoLib 5 (per-bone cube-drawing logic lives in concrete subclasses like {@code CuboidGeoBone}), but since
     * this class WRAPS a real {@code GeoBone} instead of extending it, there is no abstract method to implement here
     * - callers that need to actually render a (possibly floating/dynamic-chain-clone) bone should call this method
     * (or {@code mowzieBone.bone.positionAndRender(...)} directly) rather than trying to make {@code MowzieGeoBone}
     * itself a {@code GeoBone} subclass.
     */
    public <R extends GeoRenderState> void positionAndRender(RenderPassInfo<R> renderPassInfo, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int renderColor) {
        bone.positionAndRender(renderPassInfo, vertexConsumer, packedLight, packedOverlay, renderColor);
    }

    /**
     * Clones the structure (cubes/children) of {@code source} into a brand new, independently-posable bone.
     * Used by {@link com.bobmowzie.mowziesmobs.client.model.tools.dynamics.GeckoDynamicChain} to build a
     * "dynamic" stand-in for a chain joint that can be given its own transform separate from the original bone.
     */
    public MowzieGeoBone(MowzieGeoBone source) {
        GeoBone src = source.bone;
        GeoCube[] cubes = src instanceof CuboidGeoBone cuboidGeoBone ? cuboidGeoBone.cubes : new GeoCube[0];
        GeoBone[] children = Arrays.stream(src.children())
                .filter(b -> !DYNAMIC_JOINT_BONES.contains(b))
                .toArray(GeoBone[]::new);

        this.bone = new CuboidGeoBone(null, src.name() + "_chain", children, cubes, src.locators(),
                src.pivotX(), src.pivotY(), src.pivotZ(), src.baseRotX(), src.baseRotY(), src.baseRotZ());

        this.setPos(source.getPos());
        this.setRot(source.getRot());
        this.setScale(source.getScale());
        this.saveInitialSnapshot();
    }

    /**
     * The live, per-render-frame transform for this bone. Lazily created on and stored directly on the shared
     * {@link GeoBone#frameSnapshot} field - the same slot GeckoLib's own rendering code reads from - rather than on
     * this (disposable) wrapper, so that edits made through this wrapper are visible to the real renderer.
     */
    public BoneSnapshot snapshot() {
        if (bone.frameSnapshot == null) {
            bone.frameSnapshot = BoneSnapshot.create(bone);
        }
        return bone.frameSnapshot;
    }

    public String getName() {
        return bone.name();
    }

    @Nullable
    public MowzieGeoBone getParent() {
        GeoBone parent = bone.parent();
        return parent == null ? null : new MowzieGeoBone(parent);
    }

    public List<GeoBone> getChildBones() {
        return Arrays.asList(bone.children());
    }

    public List<GeoCube> getCubes() {
        return bone instanceof CuboidGeoBone cuboidGeoBone ? Arrays.asList(cuboidGeoBone.cubes) : List.of();
    }

    public float getPivotX() {
        return bone.pivotX();
    }

    public float getPivotY() {
        return bone.pivotY();
    }

    public float getPivotZ() {
        return bone.pivotZ();
    }

    public void setHidden(boolean hidden) {
        snapshot().skipRender(hidden);
    }

    public boolean isHidden() {
        return snapshot().isHidden();
    }

    // Position utils
    public void addPos(Vec3 vec) {
        addPos((float) vec.x(), (float) vec.y(), (float) vec.z());
    }

    public void addPos(float x, float y, float z) {
        addPosX(x);
        addPosY(y);
        addPosZ(z);
    }

    public void addPos(double x, double y, double z) {
        addPosX(x);
        addPosY(y);
        addPosZ(z);
    }

    public void addPosX(float x) {
        setPosX(getPosX() + x);
    }

    public void addPosX(double x) {
        setPosX(getPosX() + (float) x);
    }

    public void addPosY(float y) {
        setPosY(getPosY() + y);
    }

    public void addPosY(double y) {
        setPosY(getPosY() + (float) y);
    }

    public void addPosZ(float z) {
        setPosZ(getPosZ() + z);
    }

    public void addPosZ(double z) {
        setPosZ(getPosZ() + (float) z);
    }

    public void setPos(Vec3 vec) {
        setPos((float) vec.x(), (float) vec.y(), (float) vec.z());
    }

    public void setPos(float x, float y, float z) {
        setPosX(x);
        setPosY(y);
        setPosZ(z);
    }

    public float getPosX() {
        return snapshot().getTranslateX();
    }

    public float getPosY() {
        return snapshot().getTranslateY();
    }

    public float getPosZ() {
        return snapshot().getTranslateZ();
    }

    public void setPosX(float x) {
        snapshot().setTranslateX(x);
    }

    public void setPosY(float y) {
        snapshot().setTranslateY(y);
    }

    public void setPosZ(float z) {
        snapshot().setTranslateZ(z);
    }

    public Vec3 getPos() {
        return new Vec3(getPosX(), getPosY(), getPosZ());
    }

    // Rotation utils
    public void addRot(Vec3 vec) {
        addRot((float) vec.x(), (float) vec.y(), (float) vec.z());
    }

    public void addRot(float x, float y, float z) {
        addRotX(x);
        addRotY(y);
        addRotZ(z);
    }

    public void addRotX(float x) {
        setRotX(getRotX() + x);
    }

    public void addRotY(float y) {
        setRotY(getRotY() + y);
    }

    public void addRotZ(float z) {
        setRotZ(getRotZ() + z);
    }

    public void addRot(double x, double y, double z) {
        addRotX(x);
        addRotY(y);
        addRotZ(z);
    }

    public void addRotX(double x) {
        setRotX(getRotX() + (float) x);
    }

    public void addRotY(double y) {
        setRotY(getRotY() + (float) y);
    }

    public void addRotZ(double z) {
        setRotZ(getRotZ() + (float) z);
    }

    public void setRot(Vector3d vec) {
        setRot((float) vec.x(), (float) vec.y(), (float) vec.z());
    }

    public void setRot(Vec3 vec) {
        setRot((float) vec.x(), (float) vec.y(), (float) vec.z());
    }

    public void setRot(float x, float y, float z) {
        setRotX(x);
        setRotY(y);
        setRotZ(z);
    }

    public float getRotX() {
        return snapshot().getRotX();
    }

    public float getRotY() {
        return snapshot().getRotY();
    }

    public float getRotZ() {
        return snapshot().getRotZ();
    }

    public void setRotX(float x) {
        snapshot().setRotX(x);
    }

    public void setRotY(float y) {
        snapshot().setRotY(y);
    }

    public void setRotZ(float z) {
        snapshot().setRotZ(z);
    }

    public Vector3d getRot() {
        return new Vector3d(getRotX(), getRotY(), getRotZ());
    }

    // Scale utils
    public void multiplyScale(Vec3 vec) {
        multiplyScale((float) vec.x(), (float) vec.y(), (float) vec.z());
    }

    public void multiplyScale(float x, float y, float z) {
        setScaleX(getScaleX() * x);
        setScaleY(getScaleY() * y);
        setScaleZ(getScaleZ() * z);
    }

    public void setScale(Vec3 vec) {
        setScale((float) vec.x(), (float) vec.y(), (float) vec.z());
    }

    public void setScale(Vector3d vec) {
        setScale((float) vec.x(), (float) vec.y(), (float) vec.z());
    }

    public void setScale(float x, float y, float z) {
        setScaleX(x);
        setScaleY(y);
        setScaleZ(z);
    }

    public void setScale(float scale) {
        setScale(scale, scale, scale);
    }

    public float getScaleX() {
        return snapshot().getScaleX();
    }

    public float getScaleY() {
        return snapshot().getScaleY();
    }

    public float getScaleZ() {
        return snapshot().getScaleZ();
    }

    public void setScaleX(float x) {
        snapshot().setScaleX(x);
    }

    public void setScaleY(float y) {
        snapshot().setScaleY(y);
    }

    public void setScaleZ(float z) {
        snapshot().setScaleZ(z);
    }

    public Vector3d getScale() {
        return new Vector3d(getScaleX(), getScaleY(), getScaleZ());
    }

    public void addRotationOffsetFromBone(MowzieGeoBone source) {
        setRotX(getRotX() + source.getRotX() - source.getInitialSnapshot().getRotX());
        setRotY(getRotY() + source.getRotY() - source.getInitialSnapshot().getRotY());
        setRotZ(getRotZ() + source.getRotZ() - source.getInitialSnapshot().getRotZ());
    }

    public void setForceMatrixTransform(boolean forceMatrixTransform) {
        FORCE_MATRIX_TRANSFORM.put(bone, forceMatrixTransform);
    }

    public boolean isForceMatrixTransform() {
        return FORCE_MATRIX_TRANSFORM.getOrDefault(bone, false);
    }

    @Nullable
    public Matrix4f getRotationOverride() {
        return ROTATION_OVERRIDES.get(bone);
    }

    public boolean isInheritRotation() {
        return INHERIT_ROTATION.getOrDefault(bone, true);
    }

    public void setInheritRotation(boolean inheritRotation) {
        INHERIT_ROTATION.put(bone, inheritRotation);
    }

    public boolean isInheritTranslation() {
        return INHERIT_TRANSLATION.getOrDefault(bone, true);
    }

    public void setInheritTranslation(boolean inheritTranslation) {
        INHERIT_TRANSLATION.put(bone, inheritTranslation);
    }

    /**
     * Reconstructs this bone's accumulated model-space transform by walking up its (structural, immutable) parent
     * chain and composing each ancestor's pivot/rotation/scale/translation, mirroring how
     * {@code com.geckolib.animation.state.BoneSnapshot#rotate/scale/translate} and
     * {@code GeoBone#translateToPivotPoint/translateAwayFromPivotPoint} position a bone during a normal render pass.
     * <p>
     * PORTING NOTE: GeckoLib 5 no longer exposes a queryable "world/model space matrix" on GeoBone outside of an
     * active render pass (that concept - and the {@code setTrackingMatrices} live-caching flag - was removed).
     * This is a best-effort reconstruction from structural + current-snapshot data only; it deliberately does NOT
     * include the entity's own world transform (position/yaw), which previously only entered via the PoseStack
     * during the renderer's recursive render traversal. Callers needing the true world-space matrix (as opposed to
     * model-space) still need renderer-side support - see the porting report.
     */
    public Matrix4f getModelSpaceMatrix() {
        Matrix4f mat = getParent() != null ? getParent().getModelSpaceMatrix() : new Matrix4f();

        mat.translate(-getPosX() / 16f, getPosY() / 16f, getPosZ() / 16f);
        mat.translate(getPivotX() / 16f, getPivotY() / 16f, getPivotZ() / 16f);
        if (getRotZ() != 0) mat.rotateZ(getRotZ());
        if (getRotY() != 0) mat.rotateY(getRotY());
        if (getRotX() != 0) mat.rotateX(getRotX());
        if (getScaleX() != 1 || getScaleY() != 1 || getScaleZ() != 1) mat.scale(getScaleX(), getScaleY(), getScaleZ());
        mat.translate(-getPivotX() / 16f, -getPivotY() / 16f, -getPivotZ() / 16f);

        return mat;
    }

    public Matrix4f getModelRotationMat() {
        Matrix4f matrix = new Matrix4f(getModelSpaceMatrix());
        removeMatrixTranslation(matrix);
        return matrix;
    }

    public static void removeMatrixTranslation(Matrix4f matrix) {
        matrix.m30(0);
        matrix.m31(0);
        matrix.m32(0);
    }

    public void setModelXformOverride(Matrix4f mat) {
        ROTATION_OVERRIDES.put(bone, mat);
    }

    /**
     * PORTING NOTE: GeckoLib 5 removed the old {@code setTrackingMatrices(boolean)} live-matrix-caching flag
     * entirely (see {@link #getModelSpaceMatrix()} javadoc) - {@link #getModelSpaceMatrix()} is now always computed
     * fresh on demand instead of being toggled on/off, so there is nothing left for this to do. Kept as a no-op so
     * existing call sites (e.g. {@code GeckoDynamicChain}) keep compiling unchanged.
     */
    public void setTrackingMatrices(boolean trackingMatrices) {
    }

    /**
     * Sets an override transform for this bone's render pose, expressed in the same space as
     * {@link #getModelSpaceMatrix()} (model space, not true world space - see that method's javadoc for why).
     * Named to match the old GeckoLib 4.x {@code GeoBone#setWorldSpaceMatrix} this replaces.
     */
    public void setWorldSpaceMatrix(Matrix4f mat) {
        setModelXformOverride(mat);
    }

    /**
     * PORTING NOTE: alias for {@link #getModelSpaceMatrix()} - GeckoLib 5 has no equivalent of the old
     * {@code GeoBone#getWorldSpaceMatrix()} (which included the entity's own world transform, composed in via the
     * PoseStack during the old render traversal). This does NOT include that entity world transform - see
     * {@link #getModelSpaceMatrix()}'s javadoc.
     */
    public Matrix4f getWorldSpaceMatrix() {
        return getModelSpaceMatrix();
    }

    /**
     * PORTING NOTE: derived from {@link #getWorldSpaceMatrix()} (see its caveats) rather than tracked live as it
     * was in GeckoLib 4.x.
     */
    public org.joml.Matrix3f getWorldSpaceNormal() {
        return new org.joml.Matrix3f(getWorldSpaceMatrix()).transpose().invert();
    }

    /**
     * Approximates this bone's current world-space position by taking its {@link #getModelSpaceMatrix()} pivot
     * position and forward-transforming it through the entity's world transform - the exact inverse of
     * {@link #setWorldPos(Entity, Vec3, float)}, which already performed the opposite (world -> model space)
     * conversion for this same entity/bone pair.
     */
    public Vector3d getWorldPosition(Entity entity, float delta) {
        Matrix4f modelMat = getModelSpaceMatrix();
        Vector4f pivotModelSpace = new Vector4f(getPivotX() / 16f, getPivotY() / 16f, getPivotZ() / 16f, 1);
        modelMat.transform(pivotModelSpace);

        PoseStack matrixStack = new PoseStack();
        float dx = (float) (entity.xOld + (entity.getX() - entity.xOld) * delta);
        float dy = (float) (entity.yOld + (entity.getY() - entity.yOld) * delta);
        float dz = (float) (entity.zOld + (entity.getZ() - entity.zOld) * delta);
        matrixStack.translate(dx, dy, dz);
        float dYaw = Mth.rotLerp(delta, entity.yRotO, entity.getYRot());
        matrixStack.mulPose(MathUtils.quatFromRotationXYZ(0, -dYaw + 180, 0, true));
        matrixStack.scale(-1, -1, 1);
        matrixStack.translate(0, -1.5f, 0);

        Vector4f worldSpace = new Vector4f(pivotModelSpace.x(), pivotModelSpace.y(), pivotModelSpace.z(), 1);
        worldSpace.mul(matrixStack.last().pose());

        return new Vector3d(worldSpace.x(), worldSpace.y(), worldSpace.z());
    }

    public void setWorldPos(Entity entity, Vec3 worldPos, float delta) {
        PoseStack matrixStack = new PoseStack();
        float dx = (float) (entity.xOld + (entity.getX() - entity.xOld) * delta);
        float dy = (float) (entity.yOld + (entity.getY() - entity.yOld) * delta);
        float dz = (float) (entity.zOld + (entity.getZ() - entity.zOld) * delta);
        matrixStack.translate(dx, dy, dz);
        float dYaw = Mth.rotLerp(delta, entity.yRotO, entity.getYRot());
        matrixStack.mulPose(MathUtils.quatFromRotationXYZ(0, -dYaw + 180, 0, true));
        matrixStack.scale(-1, -1, 1);
        matrixStack.translate(0, -1.5f, 0);
        PoseStack.Pose matrixEntry = matrixStack.last();
        Matrix4f matrix4f = matrixEntry.pose();
        matrix4f.invert();

        Vector4f vec = new Vector4f((float) worldPos.x(), (float) worldPos.y(), (float) worldPos.z(), 1);
        vec.mul(matrix4f);
        setPosX(vec.x() * 16);
        setPosY(vec.y() * 16);
        setPosZ(vec.z() * 16);
    }

    public void setDynamicJoint(boolean dynamicJoint) {
        if (dynamicJoint) {
            DYNAMIC_JOINT_BONES.add(bone);
        }
        else {
            DYNAMIC_JOINT_BONES.remove(bone);
        }
    }

    public boolean isDynamicJoint() {
        return DYNAMIC_JOINT_BONES.contains(bone);
    }

    public void setPose(Matrix4f pose) {
        POSE.put(bone, pose);
    }

    public Matrix4f getPose() {
        return POSE.computeIfAbsent(bone, b -> new Matrix4f());
    }

    public void saveInitialSnapshot() {
        initialSnapshot = BoneSnapshot.create(bone);
        initialSnapshot.setRotation(bone.baseRotX(), bone.baseRotY(), bone.baseRotZ());
    }

    public BoneSnapshot getInitialSnapshot() {
        if (initialSnapshot == null) saveInitialSnapshot();
        return initialSnapshot;
    }

    /** Resets this bone's live per-frame snapshot back to its saved initial (base) pose. */
    public void resetToInitialSnapshot() {
        BoneSnapshot initial = getInitialSnapshot();
        setRot(initial.getRotX(), initial.getRotY(), initial.getRotZ());
        setPos(initial.getTranslateX(), initial.getTranslateY(), initial.getTranslateZ());
        setScale(initial.getScaleX(), initial.getScaleY(), initial.getScaleZ());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MowzieGeoBone other)) return false;
        return this.bone.equals(other.bone);
    }

    @Override
    public int hashCode() {
        return bone.hashCode();
    }
}
