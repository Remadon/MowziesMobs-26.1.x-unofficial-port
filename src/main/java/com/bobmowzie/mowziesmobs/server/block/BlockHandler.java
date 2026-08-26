package com.bobmowzie.mowziesmobs.server.block;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.server.item.ItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class BlockHandler {

    public static final DeferredRegister<Block> REG = DeferredRegister.create(Registries.BLOCK, MMCommon.MODID);

    // PORTING NOTE (1.21.1 -> 26.1.2): FireBlock#setFlammable(Block, int igniteOdds, int burnOdds) is now private
    // (confirmed against real 26.1.2 FireBlock source - it centrally registers only vanilla blocks in its own
    // static init, with a comment pointing at "IForgeBlockState.getFlammability"/"getFireSpreadSpeed" for anyone
    // else). The NeoForge replacement is net.neoforged.neoforge.common.extensions.IBlockExtension's
    // getFlammability(state, getter, pos, face) (== old burnOdds) and getFireSpreadSpeed(state, getter, pos, face)
    // (== old igniteOdds, confirmed by reading FireBlock's own getFlammability()/getFireSpreadSpeed() delegation
    // methods) - both default methods on Block itself now, meant to be overridden per-block instead of registered
    // centrally with FireBlock. Overridden inline on each flammable block below; init()'s old central registration
    // is gone.
    public static final DeferredHolder<Block, Block> PAINTED_ACACIA = registerBlockAndItem("painted_acacia", () -> new Block(Block.Properties.ofFullCopy(Blocks.ACACIA_PLANKS).setId(ResourceKey.create(Registries.BLOCK, MMCommon.resource("painted_acacia"))).strength(2.0F, 3.0F).sound(SoundType.WOOD)) {
        @Override
        public int getFlammability(BlockState state, BlockGetter getter, BlockPos pos, Direction face) {
            return 20;
        }

        @Override
        public int getFireSpreadSpeed(BlockState state, BlockGetter getter, BlockPos pos, Direction face) {
            return 5;
        }
    });
    public static final DeferredHolder<Block, Block> PAINTED_ACACIA_SLAB = registerBlockAndItem("painted_acacia_slab", () -> new SlabBlock(Block.Properties.ofFullCopy(PAINTED_ACACIA.get()).setId(ResourceKey.create(Registries.BLOCK, MMCommon.resource("painted_acacia_slab")))) {
        @Override
        public int getFlammability(BlockState state, BlockGetter getter, BlockPos pos, Direction face) {
            return 20;
        }

        @Override
        public int getFireSpreadSpeed(BlockState state, BlockGetter getter, BlockPos pos, Direction face) {
            return 5;
        }
    });
    public static final DeferredHolder<Block, Block> THATCH = registerBlockAndItem("thatch_block", () -> new HayBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.HAY_BLOCK).setId(ResourceKey.create(Registries.BLOCK, MMCommon.resource("thatch_block")))) {
        @Override
        public int getFlammability(BlockState state, BlockGetter getter, BlockPos pos, Direction face) {
            return 20;
        }

        @Override
        public int getFireSpreadSpeed(BlockState state, BlockGetter getter, BlockPos pos, Direction face) {
            return 60;
        }
    });
    public static final DeferredHolder<Block, Block> GONG = registerBlockAndItem("gong", () -> new GongBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_BLOCK).setId(ResourceKey.create(Registries.BLOCK, MMCommon.resource("gong"))).requiresCorrectToolForDrops().strength(5.0F).sound(SoundType.ANVIL)));
    public static final DeferredHolder<Block, Block> GONG_PART = REG.register("gong_part", () -> new GongBlock.GongPartBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_BLOCK).setId(ResourceKey.create(Registries.BLOCK, MMCommon.resource("gong_part"))).requiresCorrectToolForDrops().strength(5.0F).sound(SoundType.ANVIL)));
    public static final DeferredHolder<Block, Block> RAKED_SAND = registerBlockAndItem("raked_sand", () -> new RakedSandBlock(new ColorRGBA(14406560), BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).setId(ResourceKey.create(Registries.BLOCK, MMCommon.resource("raked_sand"))), Blocks.SAND.defaultBlockState()));
    public static final DeferredHolder<Block, Block> RED_RAKED_SAND = registerBlockAndItem("red_raked_sand", () -> new RakedSandBlock(new ColorRGBA(11098145), BlockBehaviour.Properties.ofFullCopy(Blocks.RED_SAND).setId(ResourceKey.create(Registries.BLOCK, MMCommon.resource("red_raked_sand"))), Blocks.RED_SAND.defaultBlockState()));
    public static final DeferredHolder<Block, Block> CLAWED_LOG = registerBlockAndItem("clawed_log", () -> new Block(Block.Properties.ofFullCopy(Blocks.ACACIA_PLANKS).setId(ResourceKey.create(Registries.BLOCK, MMCommon.resource("clawed_log")))) {
        @Override
        public int getFlammability(BlockState state, BlockGetter getter, BlockPos pos, Direction face) {
            return 5;
        }

        @Override
        public int getFireSpreadSpeed(BlockState state, BlockGetter getter, BlockPos pos, Direction face) {
            return 5;
        }
    });
    //public static final RegistryObject<BlockGrottol> GROTTOL = REG.register("grottol", () -> new BlockGrottol(Block.Properties.copy(Material.STONE).noDrops()));

    public static DeferredHolder<Block, Block> registerBlockAndItem(String name, Supplier<Block> block){
        DeferredHolder<Block, Block> blockObj = REG.register(name, block);
        ItemHandler.REG.register(name, () -> new BlockItem(blockObj.get(), new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(Registries.ITEM, MMCommon.resource(name)))));
        return blockObj;
    }

    public static void init() {
        // Flammability is now registered per-block via IBlockExtension#getFlammability/getFireSpreadSpeed
        // overrides above rather than centrally through FireBlock#setFlammable (see the porting note above).
    }
}