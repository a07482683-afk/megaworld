package com.megaworld.entity;

import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.random.Random;

/**
 * Типы жителей Грокса. У каждого свой характер, своё лицо (рисуется поверх обычного жителя)
 * и свой голос (звуки подобраны из игры: осёл, кошка, курица, попугай и т.д.).
 */
public enum Variant {
    NORMAL("normal", "Житель Грокса", Emotion.NEUTRAL, 1.0F, 40,
            SoundEvents.ENTITY_VILLAGER_AMBIENT, SoundEvents.ENTITY_VILLAGER_HURT, SoundEvents.ENTITY_VILLAGER_DEATH,
            SoundEvents.ENTITY_VILLAGER_YES, SoundEvents.ENTITY_VILLAGER_NO),
    DUMB("dumb", "Житель-придурок", Emotion.NEUTRAL, 0.75F, 12,
            SoundEvents.ENTITY_DONKEY_AMBIENT, SoundEvents.ENTITY_GOAT_HURT, SoundEvents.ENTITY_DONKEY_DEATH,
            SoundEvents.ENTITY_DONKEY_AMBIENT, SoundEvents.ENTITY_GOAT_HURT),
    CRYBABY("crybaby", "Житель-плакса", Emotion.SADNESS, 1.0F, 12,
            SoundEvents.ENTITY_CAT_STRAY_AMBIENT, SoundEvents.ENTITY_CAT_HURT, SoundEvents.ENTITY_CAT_DEATH,
            SoundEvents.ENTITY_CAT_AMBIENT, SoundEvents.ENTITY_CAT_HURT),
    GRUMPY("grumpy", "Житель-злюка", Emotion.ANGER, 0.95F, 12,
            SoundEvents.ENTITY_PIGLIN_BRUTE_AMBIENT, SoundEvents.ENTITY_PIGLIN_BRUTE_HURT, SoundEvents.ENTITY_PIGLIN_BRUTE_DEATH,
            SoundEvents.ENTITY_PIGLIN_BRUTE_AMBIENT, SoundEvents.ENTITY_PIGLIN_BRUTE_ANGRY),
    COWARD("coward", "Житель-трусишка", Emotion.FEAR, 1.2F, 12,
            SoundEvents.ENTITY_CHICKEN_AMBIENT, SoundEvents.ENTITY_CHICKEN_HURT, SoundEvents.ENTITY_CHICKEN_DEATH,
            SoundEvents.ENTITY_CHICKEN_AMBIENT, SoundEvents.ENTITY_CHICKEN_HURT),
    JOLLY("jolly", "Житель-весельчак", Emotion.JOY, 1.1F, 12,
            SoundEvents.ENTITY_PARROT_AMBIENT, SoundEvents.ENTITY_PARROT_HURT, SoundEvents.ENTITY_PARROT_DEATH,
            SoundEvents.ENTITY_VILLAGER_CELEBRATE, SoundEvents.ENTITY_PARROT_HURT);

    /** Часть имени файла лица: textures/entity/face/<id>_<эмоция>.png */
    public final String id;
    public final String title;
    /** Эмоция «по умолчанию», которую показывает лицо, пока ничего не происходит. */
    public final Emotion baseline;
    public final float speed;
    public final int weight;
    public final SoundEvent ambient;
    public final SoundEvent hurt;
    public final SoundEvent death;
    public final SoundEvent yes;
    public final SoundEvent no;

    Variant(String id, String title, Emotion baseline, float speed, int weight,
            SoundEvent ambient, SoundEvent hurt, SoundEvent death, SoundEvent yes, SoundEvent no) {
        this.id = id;
        this.title = title;
        this.baseline = baseline;
        this.speed = speed;
        this.weight = weight;
        this.ambient = ambient;
        this.hurt = hurt;
        this.death = death;
        this.yes = yes;
        this.no = no;
    }

    public static Variant byOrdinal(int i) {
        Variant[] all = values();
        return i >= 0 && i < all.length ? all[i] : NORMAL;
    }

    /** Случайный тип с учётом весов (обычный встречается чаще). */
    public static Variant random(Random random) {
        int total = 0;
        for (Variant v : values()) {
            total += v.weight;
        }
        int roll = random.nextInt(total);
        for (Variant v : values()) {
            roll -= v.weight;
            if (roll < 0) {
                return v;
            }
        }
        return NORMAL;
    }
}
