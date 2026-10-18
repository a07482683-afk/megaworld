package com.megaworld;

import com.megaworld.client.ComputerScreen;
import com.megaworld.client.MagnetClient;
import com.megaworld.client.TubeClientState;
import com.megaworld.computer.ComputerHooks;
import com.megaworld.tube.TubeNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public class MegaWorldClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // рендерер жителя Грокса (обычная модель жителя + лица)
        EntityRendererRegistry.register(ModEntities.GROX_DWELLER, GroxDwellerRenderer::new);

        // компьютер
        ComputerHooks.openComputer = () -> MinecraftClient.getInstance().setScreen(new ComputerScreen());

        // магнитные блоки
        MagnetClient.register();

        // данные канала Craft Tube от сервера
        ClientPlayNetworking.registerGlobalReceiver(TubeNetworking.STATE, (client, handler, buf, responseSender) -> {
            int count = buf.readVarInt();
            List<TubeClientState.RemoteVideo> list = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String title = buf.readString();
                long views = buf.readLong();
                long likes = buf.readLong();
                list.add(new TubeClientState.RemoteVideo(title, views, likes));
            }
            int unclaimed = buf.readVarInt();
            int claimed = buf.readVarInt();
            client.execute(() -> TubeClientState.update(list, unclaimed, claimed));
        });
    }
}
