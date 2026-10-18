package com.megaworld.client;

import java.util.ArrayList;
import java.util.List;

/** Последние данные канала Craft Tube, присланные сервером. */
public final class TubeClientState {
    public record RemoteVideo(String title, long views, long likes) {}

    public static List<RemoteVideo> videos = new ArrayList<>();
    public static int unclaimed = 0;
    public static int claimed = 0;

    private TubeClientState() {}

    public static void update(List<RemoteVideo> list, int unclaimedCoins, int claimedCoins) {
        videos = list;
        unclaimed = unclaimedCoins;
        claimed = claimedCoins;
    }
}
