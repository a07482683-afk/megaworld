package com.megaworld;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

public class MegaWorld implements ModInitializer {
    public static final String MOD_ID = "megaworld";

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModBlocks.init();
        ModEntities.init();
        ModItems.init();
        ModItemGroup.init();
        com.megaworld.tube.TubeNetworking.register();
        com.megaworld.ai.MegaConfig.get();
        com.megaworld.ai.GroxChat.register();
    }
}
