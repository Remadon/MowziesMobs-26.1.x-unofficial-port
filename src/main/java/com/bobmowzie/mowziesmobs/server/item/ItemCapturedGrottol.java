package com.bobmowzie.mowziesmobs.server.item;

import com.bobmowzie.mowziesmobs.server.entity.EntityHandler;
import com.bobmowzie.mowziesmobs.server.entity.grottol.EntityGrottol;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import java.util.UUID;

public class ItemCapturedGrottol extends Item {
    // ProblemReporter.ScopedCollector wants an org.slf4j.Logger (matches vanilla's own LogUtils.getLogger() usage
    // in Entity#restoreFrom) - NOT MMCommon.LOGGER, which is a log4j Logger and isn't assignment-compatible.
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    public ItemCapturedGrottol(Item.Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        Direction facing = context.getClickedFace();
        InteractionHand hand = context.getHand();
        Level world = context.getLevel();
        if (context.getClickedFace() == Direction.DOWN) {
            return InteractionResult.FAIL;
        }
        BlockPos location = pos.relative(facing);
        ItemStack stack = player.getItemInHand(hand);
        if (!player.mayUseItemAt(location, facing, stack)) {
            return InteractionResult.FAIL;
        }
        if (!world.isClientSide()) {
            EntityGrottol grottol = new EntityGrottol(EntityHandler.GROTTOL.get(), world);
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null) {
                setData(grottol, data.copyTag().getCompoundOrEmpty("EntityTag"));
            }
            grottol.snapTo(location, 0, 0);
            lookAtPlayer(grottol, player);
            grottol.finalizeSpawn((ServerLevelAccessor) world, ((ServerLevelAccessor) world).getCurrentDifficultyAt(location), EntitySpawnReason.MOB_SUMMONED, null);
            world.addFreshEntity(grottol);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }

    // PORTING NOTE (1.21.1 -> 26.1.2): Entity#serializeNBT(RegistryAccess)/#deserializeNBT(RegistryAccess,
    // CompoundTag) no longer exist (confirmed: grepped IEntityExtension and the whole vanilla Entity.java for any
    // surviving "NBT" convenience method - only the split ValueInput/ValueOutput-based save/load pipeline remains).
    // Replaced with vanilla's own TagValueOutput/TagValueInput round-trip, mirroring the real
    // Entity#restoreFrom(Entity) pattern (see vanilla Entity.java) which does the exact same
    // save-to-CompoundTag-then-reload dance this method needs.
    private void setData(EntityGrottol grottol, CompoundTag compound) {
        UUID id = grottol.getUUID();
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(grottol.problemPath(), LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, grottol.registryAccess());
            grottol.saveWithoutId(output);
            CompoundTag data = output.buildResult();
            data.merge(compound);
            grottol.load(TagValueInput.create(reporter, grottol.registryAccess(), data));
        }
        grottol.setUUID(id);
    }

    private void lookAtPlayer(EntityGrottol grottol, Player player) {
        LookControl helper = new LookControl(grottol);
        helper.setLookAt(player, 180, 90);
        helper.tick();
        /*GoalSelector ai = grottol.goalSelector;
        Set<GoalSelector.EntityAITaskEntry> tasks = Sets.newLinkedHashSet(ai..taskEntries);
        ai.taskEntries.removeIf(entry -> !(entry.action instanceof LookAtGoal));
        grottol.getRNG().setSeed("IS MATh RElatEd tO ScIENCe?".hashCode());
        ai.onUpdateTasks();
        grottol.getRNG().setSeed(new Random().nextLong());
        ai.taskEntries.clear();
        ai.taskEntries.addAll(tasks);*/
    }

    public ItemStack create(EntityGrottol grottol) {
        ItemStack stack = new ItemStack(this);
        // FIXME 1.21 :: will need to use item component in the future
        CompoundTag entityData;
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(grottol.problemPath(), LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, grottol.registryAccess());
            grottol.saveWithoutId(output);
            entityData = output.buildResult();
        }
        stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).update(tag -> tag.put("EntityTag", entityData));
        return stack;
    }
}
