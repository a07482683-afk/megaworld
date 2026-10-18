package com.megaworld.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** ПКМ — сохраняет точку возрождения (чекпоинт паркура). */
public class CheckpointBlock extends Block {
    public CheckpointBlock(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient && player instanceof ServerPlayerEntity sp) {
            sp.setSpawnPoint(world.getRegistryKey(), pos.up(), 0.0F, true, false);
            sp.sendMessage(Text.translatable("block.megaworld.checkpoint.saved"), true);
        }
        return ActionResult.success(world.isClient);
    }
}
