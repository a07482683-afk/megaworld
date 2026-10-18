package com.megaworld.ai;

import com.megaworld.entity.Emotion;
import com.megaworld.entity.GroxDwellerEntity;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Житель Грокса слышит чат игрока в радиусе 8 блоков. Команды выполняет (см. GroxCommands),
 * на обычную речь отвечает (встроенные правила или необязательный ИИ).
 * Команды «всем» / «жители ...» слышат все жители в радиусе 16 блоков.
 */
public final class GroxChat {
    private static final double RANGE = 8.0D;
    private static final double RANGE_ALL = 16.0D;
    private static final Random RANDOM = new Random();

    private GroxChat() {}

    public static void register() {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> onChat(sender, message.getSignedContent()));
    }

    private static List<GroxDwellerEntity> nearby(ServerPlayerEntity sender, double range) {
        return new ArrayList<>(sender.getServerWorld().getEntitiesByClass(
                GroxDwellerEntity.class, sender.getBoundingBox().expand(range), d -> d.isAlive()));
    }

    private static GroxDwellerEntity nearest(ServerPlayerEntity sender, List<GroxDwellerEntity> list) {
        GroxDwellerEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (GroxDwellerEntity e : list) {
            double dist = sender.squaredDistanceTo(e);
            if (dist < bestDist) {
                bestDist = dist;
                best = e;
            }
        }
        return best;
    }

    private static void onChat(ServerPlayerEntity sender, String text) {
        GroxCommands.Command cmd = GroxBrain.isThreat(text) ? null : GroxCommands.parse(text);
        if (cmd != null) {
            if (cmd.toAll()) {
                for (GroxDwellerEntity d : nearby(sender, RANGE_ALL)) {
                    d.execute(sender, cmd);
                }
            } else {
                GroxDwellerEntity d = nearest(sender, nearby(sender, RANGE));
                if (d != null) {
                    d.execute(sender, cmd);
                }
            }
            return;
        }

        final GroxDwellerEntity dweller = nearest(sender, nearby(sender, RANGE));
        if (dweller == null) {
            return;
        }
        final GroxBrain.Reply local = GroxBrain.respond(text, dweller.getEmotion(), RANDOM);

        MegaConfig cfg = MegaConfig.get();
        MinecraftServer server = sender.getServer();
        if (cfg.aiEnabled && server != null) {
            AiClient.ask(text, dweller.getEmotion()).thenAccept(ai -> server.execute(() -> {
                if (!dweller.isAlive()) {
                    return;
                }
                if (ai == null) {
                    dweller.hear(sender, local);
                } else {
                    Emotion emotion = local.emotion() == Emotion.FEAR ? Emotion.FEAR : ai.emotion();
                    dweller.hear(sender, new GroxBrain.Reply(ai.text(), emotion, local.action()));
                }
            }));
        } else {
            dweller.hear(sender, local);
        }
    }
}
