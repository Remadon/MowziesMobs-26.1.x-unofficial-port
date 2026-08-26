package com.bobmowzie.mowziesmobs.client.model.tools;

import com.geckolib.loading.loader.GeckoLibGsonLoader;

/**
 * PORTING NOTE (GeckoLib 4 -> 5, the hardest file in this agent's scope - read carefully before touching):
 * <p>
 * <b>What this class used to do:</b> in GeckoLib 4.x, {@code BakedModelFactory} was a pluggable per-bone/per-cube
 * factory ({@code constructGeoModel}/{@code constructBone}/{@code constructCube}) that GeckoLib's JSON model loader
 * called into while baking a {@code .geo.json} file, so that a mod could substitute its own {@code GeoBone}
 * subclass (here, {@code MowzieGeoBone}) directly into the baked model tree. Mowzie's Mobs used this so that every
 * bone in every one of its models was, at the object level, actually a {@code MowzieGeoBone} - letting renderer code
 * do {@code if (bone instanceof MowzieGeoBone mowzieGeoBone) ...} on bones it received from the normal render walk,
 * and letting {@code MowzieGeoBone} itself carry live, directly-mutable pos/rot/scale fields that model code wrote
 * to and the renderer read back when positioning cubes.
 * <p>
 * <b>Why that mechanism is gone in GeckoLib 5.x:</b> confirmed by reading the real GeckoLib 5.5.2 source
 * (bernie-g/geckolib, branch {@code 26.1} on GitHub - the previously-provided local sources jar/extracted tree
 * disappeared mid-port when this environment's /tmp was wiped, so this was re-fetched from GitHub via `gh api`):
 * <ul>
 *   <li>{@code BakedModelFactory}/{@code BoneStructure}/{@code GeometryTree}/the raw-json {@code Bone}/{@code Cube}
 *       types no longer exist at all. JSON model baking now happens internally and non-pluggably: see
 *       {@code com.geckolib.loading.definition.geometry.Geometry#bake} and
 *       {@code com.geckolib.loading.definition.geometry.GeometryBone#bake}, which hardcode construction of
 *       {@code com.geckolib.cache.model.cuboid.CuboidGeoBone} - there is no factory seam to substitute a different
 *       bone class per-bone anymore.</li>
 *   <li>The one remaining pluggable seam is a much coarser one: {@code com.geckolib.loading.loader.GeckoLibLoader}
 *       (registered wholesale per-resource-predicate via {@code GeckoLibUtil.addResourceLoader}/
 *       {@code GeckoLibResources.addLoader}, replacing the old {@code GeckoLibUtil.addCustomBakedModelFactory}).
 *       That swaps out the *entire* deserialize+bake pipeline for matching resources, not just bone construction.</li>
 *   <li>More fundamentally, {@code GeoBone}/{@code CuboidGeoBone} are now immutable structural definitions baked
 *       <b>once</b> into a shared, cached {@code BakedGeoModel} singleton reused by every instance of that model
 *       (see {@code com.geckolib.cache.BakedModelCache}) - so even if bone-class substitution were still possible,
 *       a custom bone subclass could no longer carry live *per-instance* mutable transform state the way
 *       {@code MowzieGeoBone} used to (two Umvuthi entities on screen at once would fight over the same shared
 *       bone object's fields). GeckoLib 5 solves this instead with a separate, ephemeral per-render-frame
 *       {@code BoneSnapshot} object (see {@code GeoBone#frameSnapshot}), never by subclassing {@code GeoBone}.</li>
 * </ul>
 * <p>
 * <b>What replaces it:</b> {@code MowzieGeoBone} (in the sibling {@code geckolib} package) is no longer a
 * {@code GeoBone} subclass baked into the model tree. It's now a lightweight, disposable per-call <i>wrapper</i>
 * around a real (shared, immutable) {@code GeoBone} plus that bone's live {@code BoneSnapshot}, constructed on
 * demand by {@code MowzieGeoModel#getMowzieBone(String)}. That fully replaces the bone-substitution role this
 * factory used to play, without needing a custom loader at all - see {@code MowzieGeoBone}'s class javadoc for the
 * full explanation.
 * <p>
 * <b>Net result:</b> this class no longer has anything useful to do. It's kept as a real, valid, no-op
 * {@code GeckoLibLoader} (identical behavior to the default {@code GeckoLibGsonLoader}) purely so the existing
 * {@code MMCommon.java} call site (outside this agent's scope: {@code GeckoLibUtil.addCustomBakedModelFactory(MODID, new MowzieModelFactory())})
 * still has something valid to construct and register without needing this agent to touch server/top-level files.
 * <b>Recommendation for whoever owns {@code MMCommon.java}:</b> that registration call is very likely no longer
 * necessary at all now and could simply be deleted; if kept, it needs updating to
 * {@code GeckoLibUtil.addResourceLoader(predicate, new MowzieModelFactory())} (new method name/signature - the old
 * {@code addCustomBakedModelFactory(String modid, BakedModelFactory)} overload no longer exists).
 */
public class MowzieModelFactory extends GeckoLibGsonLoader {
}
