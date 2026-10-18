package com.megaworld.client;

import com.megaworld.block.MagnetBlock;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;

/** Притягивает игрока к центру ближайшего магнитного блока (Shift — отключает притяжение). */
public final class MagnetClient {
    private static final int RADIUS = 5;
    private static final double MAX_DIST = 5.0D;

    private MagnetClient() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(MagnetClient::tick);
    }

    private static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || client.isPaused()) {
            return;
        }
        if (player.isSneaking() || player.isSpectator() || player.getAbilities().flying) {
            return;
        }
        BlockPos origin = player.getBlockPos();
        double bestDist = Double.MAX_VALUE;
        double bestDx = 0.0D;
        double bestDz = 0.0D;
        boolean found = false;
        for (BlockPos p : BlockPos.iterate(origin.add(-RADIUS, -2, -RADIUS), origin.add(RADIUS, 1, RADIUS))) {
            if (client.world.getBlockState(p).getBlock() instanceof MagnetBlock) {
                double dx = p.getX() + 0.5D - player.getX();
                double dz = p.getZ() + 0.5D - player.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestDx = dx;
                    bestDz = dz;
                    found = true;
                }
            }
        }
        if (!found || bestDist < 0.12D || bestDist > MAX_DIST) {
            return;
        }
        double strength = 0.05D * (1.0D - bestDist / MAX_DIST) + 0.015D;
        // только по горизонтали: так сервер не посчитает это «полётом»
        player.addVelocity(bestDx / bestDist * strength, 0.0D, bestDz / bestDist * strength);
    }
}
