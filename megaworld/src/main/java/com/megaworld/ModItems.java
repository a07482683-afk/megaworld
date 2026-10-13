package com.megaworld;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModItems {
    public static final Item GROX_CRYSTAL = register("grox_crystal", new Item(new FabricItemSettings()));
    public static final Item GROX_DWELLER_SPAWN_EGG = register("grox_dweller_spawn_egg",
            new SpawnEggItem(ModEntities.GROX_DWELLER, 0x3FA7D6, 0xF2C94C, new FabricItemSettings()));

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, MegaWorld.id(name), item);
    }

    public static void init() {}
}
