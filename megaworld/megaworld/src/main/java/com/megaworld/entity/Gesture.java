package com.megaworld.entity;

/** Жесты жителя Грокса (синхронизируются с клиентом, модель по ним двигает руки). */
public enum Gesture {
    NONE, WAVE, DANCE, SPIN, JUMP;

    public static Gesture byOrdinal(int i) {
        Gesture[] all = values();
        return i >= 0 && i < all.length ? all[i] : NONE;
    }
}
