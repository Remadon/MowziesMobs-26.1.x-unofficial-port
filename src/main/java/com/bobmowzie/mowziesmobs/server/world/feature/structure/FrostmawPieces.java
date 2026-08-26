package com.bobmowzie.mowziesmobs.server.world.feature.structure;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Map;

public class FrostmawPieces {

    private static final Identifier FROSTMAW = Identifier.fromNamespaceAndPath(MMCommon.MODID, "frostmaw_spawn");

    private static final Map<Identifier, BlockPos> OFFSET = ImmutableMap.of(
            FROSTMAW, new BlockPos(0, 1, 0)
    );

    public static void addPieces(StructureTemplateManager manager, BlockPos pos, Rotation rot, StructurePieceAccessor pieces, RandomSource rand) {
        BlockPos rotationOffset = new BlockPos(0, 0, 0).rotate(rot);
        BlockPos blockPos = rotationOffset.offset(pos);
        pieces.addPiece(new FrostmawPieces.FrostmawPiece(manager, FROSTMAW, blockPos, rot));
    }

    public static class FrostmawPiece extends TemplateStructurePiece {

        private static StructurePlaceSettings makeSettings(Rotation rotation, Identifier resourceLocation) {
            return (new StructurePlaceSettings()).setRotation(rotation).setMirror(Mirror.NONE).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
        }

        private static BlockPos makePosition(Identifier resourceLocation, BlockPos pos) {
            return pos.offset(FrostmawPieces.OFFSET.get(resourceLocation));
        }

        public FrostmawPiece(StructureTemplateManager templateManagerIn, Identifier resourceLocationIn, BlockPos pos, Rotation rotationIn) {
            super(StructureTypeHandler.FROSTMAW_PIECE.get(), 0, templateManagerIn, resourceLocationIn, resourceLocationIn.toString(), makeSettings(rotationIn, resourceLocationIn), makePosition(resourceLocationIn, pos));
        }


        // PORTING NOTE (1.21.1 -> 26.1.2): CompoundTag#getString now returns Optional<String> instead of a raw
        // String defaulting to "" (confirmed against real 26.1.2 CompoundTag source) - getStringOr(key, default)
        // is the direct raw-String replacement, defaulting to Rotation.NONE (as used elsewhere in this codebase,
        // e.g. WroughtnautChamberPieces) since "Rot" is always written by addAdditionalSaveData in practice.
        public FrostmawPiece(StructurePieceSerializationContext context, CompoundTag tag) {
            super(StructureTypeHandler.FROSTMAW_PIECE.get(), tag, context.structureTemplateManager(), (resourceLocation) -> makeSettings(Rotation.valueOf(tag.getStringOr("Rot", Rotation.NONE.name())), resourceLocation));
        }

        /**
         * (abstract) Helper method to read subclass data from NBT
         */
        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tagCompound) {
            super.addAdditionalSaveData(context, tagCompound);
            tagCompound.putString("Rot", this.placeSettings.getRotation().name());
        }


        /*
         * If you added any data marker structure blocks to your structure, you can access and modify them here. In this case,
         * our structure has a data maker with the string "chest" put into it. So we check to see if the incoming function is
         * "chest" and if it is, we now have that exact position.
         *
         * So what is done here is we replace the structure block with a chest and we can then set the loottable for it.
         *
         * You can set other data markers to do other behaviors such as spawn a random mob in a certain spot, randomize what
         * rare block spawns under the floor, or what item an Item Frame will have.
         */
        @Override
        protected void handleDataMarker(String function, BlockPos pos, ServerLevelAccessor worldIn, RandomSource rand, BoundingBox sbb) {

        }
    }
}
