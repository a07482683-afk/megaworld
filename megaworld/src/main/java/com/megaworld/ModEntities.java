package com.megaworld;

import com.megaworld.entity.GroxDwellerEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModEntities {
    public static final EntityType<GroxDwellerEntity> GROX_DWELLER = Registry.register(
            Registries.ENTITY_TYPE, MegaWorld.id("grox_dweller"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, GroxDwellerEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6F, 1.7F)).build());

    public static void init() {
        FabricDefaultAttributeRegistry.register(GROX_DWELLER, GroxDwellerEntity.createAttributes());
    }
}
