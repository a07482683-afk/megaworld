package com.megaworld.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Подбрасывает вверх и гасит урон от падения. Присядь (Shift), чтобы не прыгать. */
public class BouncePadBlock extends Block {
    public BouncePadBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (entity.isSneaking()) {
            super.onLandedUpon(world, state, pos, entity, fallDistance);
        } else {
            entity.handleFallDamage(fallDistance, 0.0F, world.getDamageSources().fall());
        }
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (entity.isSneaking()) {
            return;
        }
        Vec3d v = entity.getVelocity();
        entity.setVelocity(v.x, 1.1D, v.z);
        entity.velocityModified = true;
        entity.fallDistance = 0;
    }
}
