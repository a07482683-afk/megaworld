package com.megaworld.computer;

import java.util.Random;

public final class VideoIdeaGenerator {
    public record Idea(String title, String thumbnail, String hook) {}

    private static final String[] WHAT = {
            "Выживание на одном блоке", "Спидран прохождения", "Хардкор-выживание", "Строю город",
            "Паркур-челлендж", "Прячусь от охотника", "Побег из тюрьмы", "Мини-игра на сервере",
            "Эксперимент", "Битва двух команд", "Строю базу", "Исследую неизвестный мир"
    };
    private static final String[] CONDITION = {
            "в мире без деревьев", "только с инструментами из одного материала",
            "с жителями Грокса в союзниках", "на карте из чекпоинтов и батутов", "на 100 дней",
            "за 24 часа", "без получения урона", "в мире из плит скорости",
            "с рандомным дропом из блоков", "с одним сердцем", "вслепую", "с ограничением в 10 блоков"
    };
    private static final String[] TWIST = {
            "и вот что вышло", "(почти невозможно)", "— зрители в шоке", "но есть подвох",
            "(первый раз в истории)", "и ничего не пошло по плану", "— финал неожиданный"
    };
    private static final String[] THUMB = {
            "крупный план лица и красная стрелка", "сравнение «до / после»",
            "огромная цифра («24 ЧАСА»)", "предмет в центре и знак вопроса",
            "ты на фоне взрыва", "две команды лицом к лицу"
    };
    private static final String[] HOOK = {
            "Покажи самый яркий момент в первые 3 секунды",
            "Начни с провала, а потом объясни, как всё было",
            "Задай зрителю вопрос в самом начале",
            "Скажи «Это невозможно» и сразу докажи обратное",
            "Объяви правила и ставку: что будет при проигрыше"
    };

    private VideoIdeaGenerator() {}

    private static String pick(String[] arr, Random r) {
        return arr[r.nextInt(arr.length)];
    }

    public static Idea generate(Random r) {
        String title = pick(WHAT, r) + " " + pick(CONDITION, r) + " " + pick(TWIST, r);
        return new Idea(title, pick(THUMB, r), pick(HOOK, r));
    }
}
