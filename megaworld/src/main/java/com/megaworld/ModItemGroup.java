package com.megaworld;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

public class ModItemGroup {
    public static final ItemGroup MEGA = Registry.register(Registries.ITEM_GROUP, MegaWorld.id("main"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ModItems.GROX_CRYSTAL))
                    .displayName(Text.translatable("itemGroup.megaworld.main"))
                    .entries((ctx, entries) -> {
                        entries.add(ModBlocks.BOUNCE_PAD);
                        entries.add(ModBlocks.SPEED_PAD);
                        entries.add(ModBlocks.CHECKPOINT);
                        entries.add(ModBlocks.COMPUTER);
                        entries.add(ModBlocks.ANTI_GRAVITY_BLOCK);
                        entries.add(ModBlocks.VANISHING_BLOCK);
                        entries.add(ModBlocks.MAGNET_BLOCK);
                        entries.add(ModItems.GROX_CRYSTAL);
                        entries.add(ModItems.GROX_COIN);
                        entries.add(ModItems.GROX_DWELLER_SPAWN_EGG);
                    }).build());

    public static void init() {}
}
