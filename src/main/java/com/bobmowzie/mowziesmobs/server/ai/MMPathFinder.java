package com.bobmowzie.mowziesmobs.server.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;

import javax.annotation.Nullable;
import java.util.Set;

// PORTING NOTE (1.21.1 -> 26.1.2): net.minecraft.world.level.pathfinder.Path is now `final` and can no longer be
// subclassed (confirmed in the real decompiled 26.1.2 source), so the old PatchedPath inner class (which only
// existed to override getEntityPosAtNode(Entity, int) to center wider entities on their path nodes) can no longer
// exist. Checking vanilla's own Path#getEntityPosAtNode(...) in the current source shows it now computes the exact
// same thing PatchedPath used to add (`node.x/z + (int)(entity.getBbWidth() + 1.0F) * 0.5`, matching this mod's
// `point.x/z + Mth.floor(entity.getBbWidth() + 1.0F) * 0.5D` almost bit-for-bit) - i.e. vanilla absorbed this
// mod's fix upstream at some point, so the patch is no longer necessary and the plain Path from
// PathFinder#findPath(...) is returned as-is.
public class MMPathFinder extends PathFinder {
    public MMPathFinder(NodeEvaluator processor, int maxVisitedNodes) {
        super(processor, maxVisitedNodes);
    }

    @Nullable
    @Override
    public Path findPath(PathNavigationRegion regionIn, Mob mob, Set<BlockPos> targetPositions, float maxRange, int accuracy, float searchDepthMultiplier) {
        return super.findPath(regionIn, mob, targetPositions, maxRange, accuracy, searchDepthMultiplier);
    }
}
