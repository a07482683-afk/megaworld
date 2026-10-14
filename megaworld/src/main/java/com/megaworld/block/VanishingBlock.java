package com.megaworld.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Исчезающий блок: когда игрок встаёт на него, блок трескается и
 * через 10 тиков (0.5 сек) разрушается.
 */
public class VanishingBlock extends Block {
    /** true = блок уже запущен на исчезновение (показываем «треснувшую» текстуру). */
    public static final BooleanProperty VANISHING = BooleanProperty.of("vanishing");
    public static final int DELAY_TICKS = 10;

    public VanishingBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(VANISHING, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(VANISHING);
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (world.isClient || !(entity instanceof PlayerEntity) || state.get(VANISHING)) {
            return;
        }
        world.setBlockState(pos, state.with(VANISHING, true), Block.NOTIFY_ALL);
        world.scheduleBlockTick(pos, this, DELAY_TICKS);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (state.get(VANISHING)) {
            world.breakBlock(pos, false);
        }
    }
}
