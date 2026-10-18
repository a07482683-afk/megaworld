package com.megaworld.entity;

/** Эмоции жителя Грокса. Приоритет решает, какая эмоция перебивает другую. */
public enum Emotion {
    NEUTRAL("neutral", 0),
    JOY("joy", 1),
    SADNESS("sadness", 2),
    ANGER("anger", 3),
    FEAR("fear", 4);

    /** Часть имени файла текстуры лица: grox_dweller_<id>.png */
    public final String id;
    public final int priority;

    Emotion(String id, int priority) {
        this.id = id;
        this.priority = priority;
    }

    public static Emotion byOrdinal(int i) {
        Emotion[] all = values();
        return i >= 0 && i < all.length ? all[i] : NEUTRAL;
    }

    public static Emotion byName(String name) {
        for (Emotion e : values()) {
            if (e.name().equalsIgnoreCase(name)) {
                return e;
            }
        }
        return null;
    }
}
