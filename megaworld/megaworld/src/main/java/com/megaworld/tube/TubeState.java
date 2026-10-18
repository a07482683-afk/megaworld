package com.megaworld.tube;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Данные Craft Tube на сервере (сохраняются вместе с миром).
 * Просмотры и лайки считает СЕРВЕР по игровому времени, поэтому подделать их с клиента нельзя.
 */
public class TubeState extends PersistentState {
    public static final String KEY = "megaworld_tube";
    public static final int MAX_VIDEOS = 30;
    /** Сколько лайков нужно на одну монету Grox Coin. */
    public static final int LIKES_PER_COIN = 5;

    public static final class Video {
        public final String title;
        public final long createdAt;   // игровое время в тиках
        public final double rate;      // просмотров в секунду
        public final double likeRatio; // доля лайков от просмотров

        public Video(String title, long createdAt, double rate, double likeRatio) {
            this.title = title;
            this.createdAt = createdAt;
            this.rate = rate;
            this.likeRatio = likeRatio;
        }

        public long views(long now) {
            return (long) (Math.max(0L, now - createdAt) / 20.0D * rate);
        }

        public long likes(long now) {
            return (long) (views(now) * likeRatio);
        }
    }

    public static final class Channel {
        public final List<Video> videos = new ArrayList<>();
        public int claimedCoins = 0;
        public long lastUploadTick = -1000L;

        public long totalLikes(long now) {
            long sum = 0;
            for (Video v : videos) {
                sum += v.likes(now);
            }
            return sum;
        }

        public int unclaimedCoins(long now) {
            long earned = totalLikes(now) / LIKES_PER_COIN;
            return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, earned) - claimedCoins);
        }
    }

    private final Map<UUID, Channel> channels = new HashMap<>();

    public static TubeState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager()
                .getOrCreate(TubeState::fromNbt, TubeState::new, KEY);
    }

    public Channel channel(UUID player) {
        return channels.computeIfAbsent(player, id -> new Channel());
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (Map.Entry<UUID, Channel> e : channels.entrySet()) {
            NbtCompound c = new NbtCompound();
            c.putUuid("Id", e.getKey());
            c.putInt("Claimed", e.getValue().claimedCoins);
            c.putLong("Last", e.getValue().lastUploadTick);
            NbtList vids = new NbtList();
            for (Video v : e.getValue().videos) {
                NbtCompound vc = new NbtCompound();
                vc.putString("Title", v.title);
                vc.putLong("Created", v.createdAt);
                vc.putDouble("Rate", v.rate);
                vc.putDouble("Ratio", v.likeRatio);
                vids.add(vc);
            }
            c.put("Videos", vids);
            list.add(c);
        }
        nbt.put("Channels", list);
        return nbt;
    }

    public static TubeState fromNbt(NbtCompound nbt) {
        TubeState state = new TubeState();
        NbtList list = nbt.getList("Channels", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound c = list.getCompound(i);
            Channel ch = new Channel();
            ch.claimedCoins = c.getInt("Claimed");
            ch.lastUploadTick = c.getLong("Last");
            NbtList vids = c.getList("Videos", NbtElement.COMPOUND_TYPE);
            for (int j = 0; j < vids.size(); j++) {
                NbtCompound vc = vids.getCompound(j);
                ch.videos.add(new Video(vc.getString("Title"), vc.getLong("Created"),
                        vc.getDouble("Rate"), vc.getDouble("Ratio")));
            }
            state.channels.put(c.getUuid("Id"), ch);
        }
        return state;
    }
}
