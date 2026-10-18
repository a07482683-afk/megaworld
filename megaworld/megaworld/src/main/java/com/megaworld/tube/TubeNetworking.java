package com.megaworld.tube;

import com.megaworld.MegaWorld;
import com.megaworld.ModItems;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Сетевая часть Craft Tube.
 *  клиент -> сервер: REQUEST (дай данные), UPLOAD (выложи видео с названием), CLAIM (выдай монеты)
 *  сервер -> клиент: STATE (список видео, просмотры, лайки, монеты к выдаче)
 */
public final class TubeNetworking {
    public static final Identifier REQUEST = MegaWorld.id("tube_request");
    public static final Identifier UPLOAD = MegaWorld.id("tube_upload");
    public static final Identifier CLAIM = MegaWorld.id("tube_claim");
    public static final Identifier STATE = MegaWorld.id("tube_state");

    private static final long UPLOAD_COOLDOWN_TICKS = 40L;

    private TubeNetworking() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST,
                (server, player, handler, buf, sender) -> server.execute(() -> sendState(server, player)));

        ServerPlayNetworking.registerGlobalReceiver(UPLOAD, (server, player, handler, buf, sender) -> {
            String title = buf.readString(120);
            server.execute(() -> upload(server, player, title));
        });

        ServerPlayNetworking.registerGlobalReceiver(CLAIM,
                (server, player, handler, buf, sender) -> server.execute(() -> claim(server, player)));
    }

    private static void upload(MinecraftServer server, ServerPlayerEntity player, String rawTitle) {
        String title = rawTitle.replaceAll("\\p{Cntrl}", "").trim();
        if (title.isEmpty() || title.length() > 120) {
            return;
        }
        TubeState state = TubeState.get(server);
        TubeState.Channel ch = state.channel(player.getUuid());
        long now = server.getOverworld().getTime();
        if (now - ch.lastUploadTick < UPLOAD_COOLDOWN_TICKS) {
            sendState(server, player);
            return;
        }
        if (ch.videos.size() >= TubeState.MAX_VIDEOS) {
            ch.videos.remove(0);
        }
        double rate = 1.0D + player.getRandom().nextDouble() * 5.0D;
        double ratio = 0.05D + player.getRandom().nextDouble() * 0.15D;
        ch.videos.add(new TubeState.Video(title, now, rate, ratio));
        ch.lastUploadTick = now;
        state.markDirty();
        sendState(server, player);
    }

    private static void claim(MinecraftServer server, ServerPlayerEntity player) {
        TubeState state = TubeState.get(server);
        TubeState.Channel ch = state.channel(player.getUuid());
        long now = server.getOverworld().getTime();
        int amount = ch.unclaimedCoins(now);
        if (amount > 0) {
            ch.claimedCoins += amount;
            state.markDirty();
            int left = amount;
            while (left > 0) {
                int n = Math.min(left, 64);
                player.getInventory().offerOrDrop(new ItemStack(ModItems.GROX_COIN, n));
                left -= n;
            }
            player.sendMessage(Text.translatable("message.megaworld.coins_claimed", amount), true);
        }
        sendState(server, player);
    }

    public static void sendState(MinecraftServer server, ServerPlayerEntity player) {
        TubeState.Channel ch = TubeState.get(server).channel(player.getUuid());
        long now = server.getOverworld().getTime();
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(ch.videos.size());
        for (TubeState.Video v : ch.videos) {
            buf.writeString(v.title);
            buf.writeLong(v.views(now));
            buf.writeLong(v.likes(now));
        }
        buf.writeVarInt(ch.unclaimedCoins(now));
        buf.writeVarInt(ch.claimedCoins);
        ServerPlayNetworking.send(player, STATE, buf);
    }
}
