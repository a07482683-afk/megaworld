package com.megaworld.ai;

import com.megaworld.entity.Emotion;

import java.util.Locale;
import java.util.Random;

/** Встроенные ответы жителя Грокса: работают без интернета и без ИИ-модели. */
public final class GroxBrain {
    public enum Action { NONE, FOLLOW, STAY }

    public record Reply(String text, Emotion emotion, Action action) {}

    private GroxBrain() {}

    private static boolean has(String text, String... words) {
        for (String w : words) {
            if (text.contains(w)) {
                return true;
            }
        }
        return false;
    }

    private static String pick(Random r, String... options) {
        return options[r.nextInt(options.length)];
    }

    /** Есть ли в сообщении угроза (тогда команды не выполняются, житель пугается). */
    public static boolean isThreat(String message) {
        String t = " " + message.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim() + " ";
        return has(t, "убью", "убить", "убей", "ударю", "побью", "зарежу", "умри", " kill ", " die ");
    }

    public static Reply respond(String message, Emotion current, Random r) {
        String t = " " + message.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim() + " ";

        if (has(t, "убью", "убить", "убей", "ударю", "побью", "зарежу", "умри", " kill ", " die ")) {
            return new Reply(pick(r, "Не бей меня! Пожалуйста!", "Ааа! Только не это!", "Я ничего плохого не сделал!"),
                    Emotion.FEAR, Action.NONE);
        }
        if (has(t, "дурак", "тупой", "тупица", "глупый", "идиот", "урод", " stupid ", " dumb ", " idiot ")) {
            if (current == Emotion.ANGER || r.nextInt(100) < 40) {
                return new Reply(pick(r, "Сам ты такой! Я обиделся!", "Хмф! Не груби мне!"), Emotion.ANGER, Action.NONE);
            }
            return new Reply(pick(r, "Мне обидно это слышать...", "Зачем ты так? Мне грустно."), Emotion.SADNESS, Action.NONE);
        }
        if (has(t, "за мной", "иди сюда", "иди ко мне", "следуй", "пойдем", "пойдём", "иди за", " follow ", " come ")) {
            return new Reply(pick(r, "Иду за тобой!", "Веди, я следом!", "Хорошо, пойдём вместе!"), Emotion.JOY, Action.FOLLOW);
        }
        if (has(t, "стой", "стоять", "жди", "подожди", "не двигайся", " stay ", " wait ")) {
            return new Reply(pick(r, "Хорошо, буду ждать здесь.", "Стою и не двигаюсь!"), Emotion.NEUTRAL, Action.STAY);
        }
        if (has(t, "привет", "здравствуй", "здорово", "добрый день", "добрый вечер", " hello ", " hi ", " hey ")) {
            return new Reply(pick(r, "Привет! Рад тебя видеть!", "О, привет, друг!", "Здравствуй!"), Emotion.JOY, Action.NONE);
        }
        if (has(t, "как дела", "как ты", "как жизнь", " how are you ")) {
            String line;
            switch (current) {
                case JOY -> line = "Отлично, особенно когда ты рядом!";
                case SADNESS -> line = "Грустно... меня недавно обидели.";
                case ANGER -> line = "Злюсь! Но ты тут ни при чём.";
                case FEAR -> line = "Мне страшно... не нервируй меня.";
                default -> line = "Нормально! Хожу, смотрю по сторонам.";
            }
            return new Reply(line, current, Action.NONE);
        }
        if (has(t, "как тебя зовут", "кто ты", " your name ", " who are you ")) {
            return new Reply("Я житель Грокса! Просто мирный житель.", Emotion.NEUTRAL, Action.NONE);
        }
        if (has(t, "изумруд", "emerald", "кристалл")) {
            return new Reply(pick(r, "Изумруд! Моя любимая вещь!", "Ого, блестящее! Дашь посмотреть?"), Emotion.JOY, Action.NONE);
        }
        if (has(t, "бедный", "не плачь", "прости", "извини", " sorry ")) {
            return new Reply(pick(r, "Спасибо... мне уже лучше.", "Ладно, прощаю."), Emotion.JOY, Action.NONE);
        }
        if (has(t, "спасибо", "молодец", "люблю", "друг", "классный", "круто", " thanks ", " thank you ", " love ")) {
            return new Reply(pick(r, "Ой, как приятно!", "Спасибо! Ты тоже хороший!", "Это лучший день!"), Emotion.JOY, Action.NONE);
        }
        return new Reply(pick(r, "Хм, интересно...", "Не всё понял, но звучит неплохо.", "Расскажи ещё!", "Ммм?"),
                Emotion.NEUTRAL, Action.NONE);
    }
}
