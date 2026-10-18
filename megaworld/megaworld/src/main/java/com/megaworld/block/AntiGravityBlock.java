package com.megaworld.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Блок невесомости: при наступании даёт левитацию (поднимает вверх),
 * а затем плавное падение. Присядь (Shift), чтобы пройти по блоку без эффекта.
 */
public class AntiGravityBlock extends Block {
    public AntiGravityBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (world.isClient || entity.isSneaking() || !(entity instanceof LivingEntity living)) {
            return;
        }
        // 30 тиков (1.5 сек) подъёма, затем 8 сек плавного падения
        living.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 30, 0, false, false, true));
        living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 160, 0, false, false, true));
    }
}
