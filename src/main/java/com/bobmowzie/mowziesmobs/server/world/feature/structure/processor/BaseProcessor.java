package com.bobmowzie.mowziesmobs.server.world.feature.structure.processor;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class BaseProcessor extends StructureProcessor {
    public static final BaseProcessor INSTANCE = new BaseProcessor();
    public static final MapCodec<BaseProcessor> CODEC = MapCodec.unit(() -> INSTANCE);

    @Override
    protected StructureProcessorType<?> getType() {
        return ProcessorHandler.BASE_PROCESSOR.value();
    }

    @Override
    public StructureTemplate.StructureBlockInfo process(LevelReader levelReader, BlockPos jigsawPiecePos, BlockPos jigsawPieceBottomCenterPos, StructureTemplate.StructureBlockInfo blockInfoLocal, StructureTemplate.StructureBlockInfo blockInfoGlobal, StructurePlaceSettings structurePlacementData, StructureTemplate template) {
        if (blockInfoGlobal.state().is(Blocks.COBBLED_DEEPSLATE)) {
            if (levelReader instanceof WorldGenRegion worldGenRegion && !worldGenRegion.getCenter().equals(ChunkPos.containing(blockInfoGlobal.pos()))) {
                return blockInfoGlobal;
            }

            BlockPos.MutableBlockPos mutable = blockInfoGlobal.pos().mutable().move(Direction.DOWN);
            BlockState currBlockState = levelReader.getBlockState(mutable);
            RandomSource random = structurePlacementData.getRandom(blockInfoGlobal.pos());

            blockInfoGlobal = new StructureTemplate.StructureBlockInfo(blockInfoGlobal.pos(), chooseRandomState(random), blockInfoGlobal.nbt());
            // PORTING NOTE (1.21.1 -> 26.1.2): LevelHeightAccessor#getMinBuildHeight/getMaxBuildHeight were renamed
            // to getMinY/getMaxY, and ChunkAccess#setBlockState's 3rd param changed from a boolean "isMoving" flag
            // to an int block-update-flags bitmask (confirmed against real 26.1.2 sources) - Block.UPDATE_ALL (3)
            // matches the old normal (non-piston-moved) placement semantics.
            while (mutable.getY() > levelReader.getMinY()
                    && mutable.getY() < levelReader.getMaxY()
                    && !currBlockState.isSolid()) {
                levelReader.getChunk(mutable).setBlockState(mutable, chooseRandomState(random), Block.UPDATE_ALL);

                // Update to next position
                mutable.move(Direction.DOWN);
                currBlockState = levelReader.getBlockState(mutable);
            }
        }
        return blockInfoGlobal;
    }

    public BlockState chooseRandomState(RandomSource random) {
        float v = random.nextFloat();
        if (v > 0.7) return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        else return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
    }

}
