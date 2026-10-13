package com.megaworld;

import com.megaworld.client.ComputerScreen;
import com.megaworld.computer.ComputerHooks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;

public class MegaWorldClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.GROX_DWELLER, GroxDwellerRenderer::new);
        ComputerHooks.openComputer = () -> MinecraftClient.getInstance().setScreen(new ComputerScreen());
    }
}
