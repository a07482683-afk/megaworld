package com.megaworld.ai;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Разбор команд игрока для жителей Грокса. Команда = глагол (десятки синонимов) + необязательные
 * детали: предмет (словарь из ~60 групп предметов), направление, число блоков, «всем».
 * Из-за комбинаций получаются тысячи рабочих фраз: «принеси железо», «собери дерево»,
 * «иди вперёд 15 блоков», «всем жителям танцевать» и т.д.
 */
public final class GroxCommands {
    public enum Type {
        STOP, FREE, HOME_SET, HOME_GO, FOLLOW, COME, STAY, GO, COLLECT, FETCH, GIVE, DROP, STATUS,
        DANCE, WAVE, SPIN, JUMP, SPEED, GLOW, HIDE, SING, JOKE, TIME, WEATHER, COORDS
    }

    public enum Dir { FORWARD, BACK, LEFT, RIGHT }

    /** filter == null означает «любые предметы». toAll — команда всем жителям рядом. */
    public record Command(Type type, Dir dir, int amount, String filterName, String[] filter, boolean toAll) {}

    private record ItemWord(String label, String[] stems, String[] filter) {}

    private static ItemWord w(String label, String[] stems, String... filter) {
        return new ItemWord(label, stems, filter);
    }

    private static String[] s(String... a) {
        return a;
    }

    // Фильтры: "=имя" — точное совпадение id предмета, "@food" — любая еда, иначе — «содержит».
    private static final ItemWord[] ITEMS = {
            w("дерево", s("дерев", "бревн", "древес", " wood ", " log ", " logs "), "_log", "_wood", "planks", "stick"),
            w("доски", s("доск", "planks"), "planks"),
            w("палки", s("палк", "stick"), "stick"),
            w("камень", s("камн", "булыжн", "cobble", " stone "), "=stone", "cobblestone", "cobbled_deepslate", "andesite", "diorite", "granite"),
            w("землю", s("земл", " dirt "), "=dirt", "grass_block", "coarse_dirt", "podzol"),
            w("песок", s("песок", "песк", " sand "), "=sand", "=red_sand", "sandstone"),
            w("гравий", s("гравий", "гравия", "gravel"), "=gravel"),
            w("уголь", s("угол", "угля", " coal "), "coal"),
            w("изумруды", s("изумруд", "emerald"), "emerald"),
            w("алмазы", s("алмаз", "diamond"), "diamond"),
            w("железо", s("желез", " iron "), "iron"),
            w("золото", s("золот", " gold "), "gold_ingot", "gold_nugget", "raw_gold", "gold_ore", "gold_block"),
            w("лазурит", s("лазурит", "lapis"), "lapis"),
            w("редстоун", s("редстоун", "redstone"), "redstone"),
            w("медь", s("медь", "медн", "copper"), "copper"),
            w("аметист", s("аметист", "amethyst"), "amethyst"),
            w("незерит", s("незерит", "netherite"), "netherite", "ancient_debris"),
            w("кварц", s("кварц", "quartz"), "quartz"),
            w("слитки", s("слитк", "слиток", "ingot"), "_ingot"),
            w("руду", s(" руд", " ore "), "_ore", "raw_"),
            w("еду", s(" еда ", " еду ", " еды ", " еде ", "food", "съедоб"), "@food"),
            w("яблоки", s("яблок", "apple"), "apple"),
            w("хлеб", s("хлеб", "bread"), "bread"),
            w("морковь", s("морков", "carrot"), "carrot"),
            w("картошку", s("картоф", "potato"), "potato"),
            w("пшеницу", s("пшениц", "wheat"), "wheat"),
            w("семена", s("семен", "семя", "seeds"), "seeds"),
            w("мясо", s("мяс", "стейк", "говядин", "свинин", "курин", "баранин", "кролик"), "beef", "porkchop", "chicken", "mutton", "rabbit"),
            w("рыбу", s(" рыб", "fish"), "cod", "salmon", "fish"),
            w("яйца", s("яйц", " egg "), "egg"),
            w("цветы", s("цвет", "flower"), "tulip", "poppy", "dandelion", "orchid", "allium", "azure", "cornflower", "lily", "rose", "peony", "lilac", "sunflower", "oxeye"),
            w("саженцы", s("саженц", "sapling"), "sapling"),
            w("листву", s("листв", "leaves"), "leaves"),
            w("шерсть", s("шерст", "wool"), "wool"),
            w("кожу", s("кожа", "кожу", "кожи", "leather"), "leather"),
            w("кости", s("кост", "bone"), "bone"),
            w("стрелы", s("стрел", "arrow"), "arrow"),
            w("порох", s("порох", "gunpowder"), "gunpowder"),
            w("слизь", s("слизь", "слизн", "slime"), "slime"),
            w("жемчуг", s("жемчуг", "pearl"), "pearl"),
            w("стекло", s("стекл", "glass"), "glass"),
            w("кирпичи", s("кирпич", "brick"), "brick"),
            w("факелы", s("факел", "torch"), "torch"),
            w("монеты Грокса", s("монет", "grox coin"), "grox_coin"),
            w("кристаллы", s("кристалл", "crystal"), "grox_crystal", "amethyst"),
            w("инструменты", s("инструмент", " tool "), "_pickaxe", "_axe", "_shovel", "_hoe"),
            w("оружие", s("оружи", " меч", "weapon", "sword"), "_sword", "_axe", "bow", "trident"),
            w("броню", s("брон", "armor"), "_helmet", "_chestplate", "_leggings", "_boots"),
    };

    private static final String[][] NUMBER_WORDS = {
            {" один ", "1"}, {" одну ", "1"}, {" два ", "2"}, {" две ", "2"}, {" три ", "3"}, {" четыре ", "4"},
            {" пять ", "5"}, {" шесть ", "6"}, {" семь ", "7"}, {" восемь ", "8"}, {" девять ", "9"},
            {" десять ", "10"}, {" двадцать ", "20"}, {" тридцать ", "30"}, {" пятьдесят ", "50"}, {" сто ", "100"}
    };

    private static final Pattern DIGITS = Pattern.compile("(\\d{1,3})");

    private GroxCommands() {}

    private static String norm(String message) {
        String t = message.toLowerCase(Locale.ROOT).replace('ё', 'е');
        return " " + t.replaceAll("[^\\p{L}\\p{N}]+", " ").trim() + " ";
    }

    private static boolean has(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private static int parseAmount(String t) {
        Matcher m = DIGITS.matcher(t);
        if (m.find()) {
            return Math.max(1, Math.min(64, Integer.parseInt(m.group(1))));
        }
        for (String[] nw : NUMBER_WORDS) {
            if (t.contains(nw[0])) {
                return Math.max(1, Math.min(64, Integer.parseInt(nw[1])));
            }
        }
        return 5;
    }

    private static Dir parseDir(String t) {
        if (has(t, "вперед", "прямо", " forward ", " ahead ")) {
            return Dir.FORWARD;
        }
        if (has(t, "назад", "обратно", " back ")) {
            return Dir.BACK;
        }
        if (has(t, "влево", "налево", "слева", " left ")) {
            return Dir.LEFT;
        }
        if (has(t, "вправо", "направо", "справа", " right ")) {
            return Dir.RIGHT;
        }
        return null;
    }

    private static ItemWord findItem(String t) {
        for (ItemWord iw : ITEMS) {
            for (String stem : iw.stems()) {
                if (t.contains(stem)) {
                    return iw;
                }
            }
        }
        return null;
    }

    /** Возвращает команду или null, если в сообщении нет команды. */
    public static Command parse(String message) {
        String t = norm(message);
        boolean all = has(t, " всем ", " все жители ", " жители ", " жителей ", " ребята ", " народ ", " всех ");
        int amount = parseAmount(t);
        ItemWord iw = findItem(t);
        String label = iw == null ? "всё" : iw.label();
        String[] filter = iw == null ? null : iw.filter();

        Type type = null;
        Dir dir = null;

        if (has(t, " стоп ", "хватит", "отмена", "отмени", "прекрати", "перестань", " stop ")) {
            type = Type.STOP;
        } else if (has(t, "отстан", "гуляй", "свободен", "делай что хочешь", "можешь идти", "отдыхай", "свободно")) {
            type = Type.FREE;
        } else if (has(t, "это твой дом", "твой дом здесь", "твой дом тут", "здесь твой дом", "тут твой дом",
                "запомни это место", "запомни дом", "запомни свой дом", "живи здесь")) {
            type = Type.HOME_SET;
        } else if (has(t, "домой", "иди в дом", "возвращайся", "вернись домой")) {
            type = Type.HOME_GO;
        } else if (has(t, "что у тебя", "что в рюкзаке", "рюкзак", "инвентар", "что собрал", "сколько собрал",
                "что нашел", " inventory ")) {
            type = Type.STATUS;
        } else if (has(t, "принеси", "притащи", "достань", "добудь", "раздобудь", " bring ", " fetch ")) {
            type = Type.FETCH;
        } else if (has(t, "собери", "собирай", "подбери", "подбирай", "набери", "собрать", " collect ", " pick up ")) {
            type = Type.COLLECT;
        } else if (has(t, "отдай", "отдавай", "верни", "выдай", " дай ", "подай", " give ")) {
            type = Type.GIVE;
        } else if (has(t, "выбрось", "выброси", "брось", "сбрось", "высыпь", " drop ")) {
            type = Type.DROP;
        } else if (has(t, "за мной", "следуй", "иди со мной", "идем со мной", "пойдем", "пошли", "не отставай",
                "сопровождай", " follow ")) {
            type = Type.FOLLOW;
        } else if (has(t, "иди сюда", "подойди", "ко мне", "приди", "подходи", " сюда ", " come ")) {
            type = Type.COME;
        } else if (has(t, " стой ", "стоять", "жди", "подожди", "не двигайся", "замри", "оставайся", "останься", " stay ", " wait ")) {
            type = Type.STAY;
        } else if (has(t, "иди", "пройди", "шагай", "двигайся", "отойди", "сдвинься", "подвинься", "пройдись", " go ", " walk ")
                && (dir = parseDir(t)) != null) {
            type = Type.GO;
        } else if (has(t, "танцуй", "потанцуй", "станцуй", "спляши", "пляши", "танец", " dance ", "танцевать")) {
            type = Type.DANCE;
        } else if (has(t, "помаши", "махни", "маши", "поприветствуй", "помахай", " wave ", "приветствуй")) {
            type = Type.WAVE;
        } else if (has(t, "крутись", "покрутись", "вертись", "повернись", "кружись", "завертись", " spin ", "оборот")) {
            type = Type.SPIN;
        } else if (has(t, "прыгай", "прыгни", "попрыгай", "подпрыгни", "скачи", " jump ", "прыжок")) {
            type = Type.JUMP;
        } else if (has(t, "спрячься", "прячься", "убегай", "убеги", "беги от меня", "беги прочь", " hide ")) {
            type = Type.HIDE;
        } else if (has(t, "беги", "побежали", "быстрее", "ускорься", "поспеши", "бегом", " run ", " faster ")) {
            type = Type.SPEED;
        } else if (has(t, "свети", "посвети", "подсвети", "светись", "засветись", "засияй", "сияй", " glow ")) {
            type = Type.GLOW;
        } else if (has(t, "спой", " пой ", "песн", "петь", " sing ")) {
            type = Type.SING;
        } else if (has(t, "шутк", "анекдот", "рассмеши", "пошути", "смешное", " joke ")) {
            type = Type.JOKE;
        } else if (has(t, "который час", "сколько времени", "сколько сейчас", "время суток", "ночь или день", "день или ночь", " time ")) {
            type = Type.TIME;
        } else if (has(t, "погода", "погоду", "дождь ли", " weather ")) {
            type = Type.WEATHER;
        } else if (has(t, "где мы", "где ты", "координат", " where are we ", " coordinates ")) {
            type = Type.COORDS;
        }

        if (type == null) {
            return null;
        }
        return new Command(type, dir, amount, label, filter, all);
    }
}
