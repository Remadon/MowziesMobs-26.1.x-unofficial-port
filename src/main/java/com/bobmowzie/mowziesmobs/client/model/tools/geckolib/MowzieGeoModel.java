package com.bobmowzie.mowziesmobs.client.model.tools.geckolib;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.model.GeoModel;

import java.util.Optional;

/**
 * PORTING NOTE (GeckoLib 4 -> 5): the old {@code GeoModel} was effectively stateful per render-instance (it exposed
 * {@code getBone(name)}, {@code getAnimationProcessor()}, and an overridable {@code applyMolangQueries}/
 * {@code setCustomAnimations} hook that ran once per animatable per frame). The new {@code GeoModel} is completely
 * stateless: it only knows how to resolve resource locations (model/texture/animation) and bake/cache a
 * {@link BakedGeoModel} from them. There is no more per-frame "custom animation" hook on GeoModel at all - that
 * responsibility moved to {@code GeoRenderer#adjustModelBonesForRender(RenderPassInfo, BoneSnapshots)}, which lives
 * in the renderer classes (client/render/**, out of this file's scope).
 * <p>
 * To keep every Model subclass in this mod compiling and behaviorally intact, this class keeps the old
 * {@code getBone}/{@code getMowzieBone} ergonomics working by resolving bones through
 * {@link #getBakedModel(net.minecraft.resources.Identifier)}. Every Model subclass in this mod implements
 * {@code getModelResource(GeoRenderState)} as a hardcoded constant (ignoring the parameter), so resolving it with a
 * {@code null} render state is safe today - see {@link #getBakedModelForBones()}.
 * <p>
 * Model subclasses should no longer {@code @Override setCustomAnimations}/{@code applyMolangQueries} (those methods
 * no longer exist on the supertype); keep the per-entity procedural-animation method as a plain method instead
 * (see e.g. {@code ModelSculptor#setCustomAnimations}) so it can be invoked from the renderer's
 * {@code adjustModelBonesForRender} once that's wired up.
 */
public abstract class MowzieGeoModel<T extends GeoAnimatable> extends GeoModel<T> {
    public MowzieGeoModel() {
    }

    /**
     * Resolves the baked model used for bone lookups. Passes {@code null} as the render state since every model
     * resource getter in this mod returns a constant path - see class javadoc.
     */
    protected BakedGeoModel getBakedModelForBones() {
        return getBakedModel(getModelResource(null));
    }

    public Optional<GeoBone> getBone(String boneName) {
        return getBakedModelForBones().getBone(boneName);
    }

    public MowzieGeoBone getMowzieBone(String boneName) {
        return getBone(boneName).map(MowzieGeoBone::new).orElse(null);
    }

    public boolean isInitialized() {
        return !getBakedModelForBones().isMissingno();
    }

    public void resetBoneToSnapshot(MowzieGeoBone bone) {
        bone.resetToInitialSnapshot();
    }

    /**
     * Walks every bone in this model and resets its live per-frame transform back to its initial (base) pose.
     * PORTING NOTE: previously this ran automatically every frame via an {@code applyMolangQueries} override; there
     * is no such automatic hook anymore. This must now be called explicitly (intended to be called from the
     * renderer's {@code adjustModelBonesForRender}, before any procedural bone adjustments) - see class javadoc.
     */
    public void resetAllBonesToInitialSnapshot() {
        for (GeoBone bone : getBakedModelForBones().topLevelBones()) {
            resetBoneAndChildrenToInitialSnapshot(bone);
        }
    }

    private void resetBoneAndChildrenToInitialSnapshot(GeoBone bone) {
        resetBoneToSnapshot(new MowzieGeoBone(bone));
        for (GeoBone child : bone.children()) {
            resetBoneAndChildrenToInitialSnapshot(child);
        }
    }

    public float getControllerValueInverted(String controllerName) {
        if (!isInitialized()) return 1.0f;
        MowzieGeoBone bone = getMowzieBone(controllerName);
        if (bone == null) return 1.0f;
        return 1.0f - bone.getPosX();
    }

    public float getControllerValue(String controllerName) {
        if (!isInitialized()) return 0.0f;
        MowzieGeoBone bone = getMowzieBone(controllerName);
        if (bone == null) return 0.0f;
        return bone.getPosX();
    }

    public double poweredWave(double x, double speed, double offset, double power) {
        return Math.pow(((Math.cos(x * speed + offset) + 1f) / 2.0f), power);
    }

    public double rootWave(double x, double speed, double offset, double power) {
        double baseValue = (Math.cos(x * speed + offset));
        if (baseValue < 0) {
            return (double) -Math.pow(-baseValue, power);
        }
        return (double) Math.pow(baseValue, power);
    }
}
