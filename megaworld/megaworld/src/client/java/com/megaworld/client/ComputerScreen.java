package com.megaworld.client;

import com.megaworld.client.games.Minecraft2D;
import com.megaworld.client.games.MiniGame;
import com.megaworld.client.games.RunnerGame;
import com.megaworld.client.games.SnakeGame;
import com.megaworld.computer.VideoIdeaGenerator;
import com.megaworld.tube.TubeNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * Рабочий стол в стиле Windows 11: Пуск, окна, Блокнот, Идеи для видео (генерация и запись),
 * Craft Tube (лента, свой канал, Grox Coins), Настройки.
 */
public class ComputerScreen extends Screen {
    private enum App {
        NOTES("Блокнот", "Блокнот", 0xFFF2C94C, "Б"),
        IDEAS("Идеи для видео", "Идеи", 0xFFF2994A, "И"),
        CRAFTTUBE("Craft Tube", "Craft Tube", 0xFFE5272D, "C"),
        GAMES("Игры", "Игры", 0xFF2ECC71, "Г"),
        SETTINGS("Настройки", "Настройки", 0xFF8E8E93, "Н");

        final String title;
        final String shortName;
        final int color;
        final String letter;

        App(String title, String shortName, int color, String letter) {
            this.title = title;
            this.shortName = shortName;
            this.color = color;
            this.letter = letter;
        }
    }

    private enum TubeView { HOME, CHANNEL, WATCH }

    /** Видео из общей ленты (просмотры растут со временем). */
    private static final class Video {
        final String title;
        final String channel;
        final int durationSec;
        final int color;
        final long baseViews;
        final double rate;
        final long createdAt;
        long extraViews;
        boolean liked;

        Video(String title, String channel, int durationSec, int color, long baseViews, double rate) {
            this.title = title;
            this.channel = channel;
            this.durationSec = durationSec;
            this.color = color;
            this.baseViews = baseViews;
            this.rate = rate;
            this.createdAt = System.currentTimeMillis();
        }

        long views() {
            return baseViews + extraViews + (long) ((System.currentTimeMillis() - createdAt) / 1000.0 * rate);
        }

        long likes() {
            return views() / 12 + (liked ? 1 : 0);
        }
    }

    private static final App[] APPS = App.values();
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    // состояние живёт, пока запущена игра
    private static final StringBuilder NOTES_TEXT = new StringBuilder();
    private static final Random RANDOM = new Random();
    private static VideoIdeaGenerator.Idea idea = null;
    private static boolean dark = true;

    // запись видео: идея -> запись (4 сек) -> готово к выкладке
    private static final long RECORD_MS = 4000L;
    private static VideoIdeaGenerator.Idea recordingIdea = null;
    private static long recordStart = 0L;
    private static VideoIdeaGenerator.Idea readyVideo = null;

    private static final String[] CHANNELS = {"BlockMaster", "PixelRider", "Алмазный Стив", "VillagerLife", "ПаркурПро", "Creeper Daily", "Эндер Шоу"};
    private static final int[] COLORS = {0xFF3A6FD8, 0xFF4CAF50, 0xFFE5484D, 0xFFF2C94C, 0xFF9B59B6, 0xFF1ABC9C};
    private static final String[] COMMENTS = {
            "Первый!", "Жители Грокса такое не пережили бы", "Это невозможно...", "Лайк, если смотришь в 2026",
            "Где взять такие паркур-блоки?", "Не бей жителя, он плачет", "Автору респект", "Монтаж огонь"
    };
    private static final List<Video> FEED = new ArrayList<>();
    private static final Set<String> SUBS = new HashSet<>();

    // мини-игры (состояние сохраняется, пока запущена игра)
    private static final MiniGame[] GAME_LIST = {new Minecraft2D(), new SnakeGame(), new RunnerGame()};

    private static final int TB = 22;      // высота панели задач
    private static final int TITLE = 14;   // высота заголовка окна
    private static final int MENU_W = 140;

    private App app = null;
    private boolean startOpen = false;
    private int winX = -1, winY = -1;
    private boolean dragging = false;
    private int dragOffX, dragOffY;
    private long copiedUntil = 0;

    // Craft Tube
    private TubeView tubeView = TubeView.HOME;
    private TubeView backView = TubeView.HOME;
    private int page = 0;
    private Video current = null;
    private double progress = 0;
    private boolean playing = false;
    private boolean counted = false;
    private long lastTime = System.currentTimeMillis();
    private long lastRequest = 0L;
    private int gameIndex = -1; // -1 = меню игр

    public ComputerScreen() {
        super(Text.literal("Computer"));
    }

    // ---------- тема ----------
    private int cTaskbar() { return dark ? 0xF01C1C1C : 0xF0F3F3F3; }
    private int cWindow() { return dark ? 0xFF2B2B2B : 0xFFF9F9F9; }
    private int cTitle() { return dark ? 0xFF1F1F1F : 0xFFE9E9E9; }
    private int cText() { return dark ? 0xFFFFFFFF : 0xFF1A1A1A; }
    private int cSub() { return dark ? 0xFFB0B0B0 : 0xFF666666; }
    private int cField() { return dark ? 0xFF1B1B1B : 0xFFFFFFFF; }
    private int cButton(boolean hover) {
        return dark ? (hover ? 0xFF4A4A4A : 0xFF3A3A3A) : (hover ? 0xFFD0D0D0 : 0xFFE1E1E1);
    }

    // ---------- геометрия ----------
    private int winW() { return Math.min((app == App.CRAFTTUBE || app == App.GAMES) ? 360 : 280, width - 16); }
    private int winH() { return Math.min((app == App.CRAFTTUBE || app == App.GAMES) ? 210 : 180, height - TB - 16); }
    private int tbTop() { return height - TB; }
    private int taskCount() { return APPS.length + 1; }
    private int taskX(int i) { return (width - (taskCount() * 20 + (taskCount() - 1) * 2)) / 2 + i * 22; }
    private int menuH() { return APPS.length * 16 + 28; }
    private int menuX() { return (width - MENU_W) / 2; }
    private int menuY() { return tbTop() - menuH() - 4; }
    private int rowY(int i) { return menuY() + 4 + i * 16 + (i == APPS.length ? 4 : 0); }
    private int cx() { return winX + 6; }
    private int cy() { return winY + TITLE + 6; }
    private int cw() { return winW() - 12; }
    private int ch() { return winH() - TITLE - 12; }

    private int[] iconRect(int i) {
        int perCol = Math.max(1, (height - TB - 16) / 44);
        return new int[]{8 + (i / perCol) * 60, 8 + (i % perCol) * 44, 56, 40};
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static boolean in(int mx, int my, int[] r) {
        return in(mx, my, r[0], r[1], r[2], r[3]);
    }

    /** Кнопки внутри окна: Идеи — 3 кнопки (слоты 0..2), Настройки — 2 кнопки. */
    private int[] btn(int slot) {
        if (app == App.IDEAS) {
            int w = (cw() - 8) / 3;
            return new int[]{cx() + slot * (w + 4), cy() + ch() - 16, w, 16};
        }
        return new int[]{cx() + slot * 86, cy() + 12, 80, 16};
    }

    // Craft Tube: геометрия
    private int ctTop() { return cy() + 18; }
    private int rowsPerPage() { return Math.max(1, (ch() - 36) / 38); }
    private int chRowsPerPage() { return Math.max(1, (ch() - 58) / 38); }
    private int playerH() { return Math.max(40, Math.min(100, ch() - 84)); }
    private int[] ctRow(int r) { return new int[]{cx(), ctTop() + r * 38, cw(), 36}; }
    private int[] ctChRow(int r) { return new int[]{cx(), ctTop() + 40 + r * 38, cw(), 36}; }
    private int[] ctTab(int i) { return new int[]{cx() + cw() - 142 + i * 72, cy(), 70, 14}; }
    private int[] ctPrev() { return new int[]{cx() + cw() - 36, cy() + ch() - 14, 16, 14}; }
    private int[] ctNext() { return new int[]{cx() + cw() - 18, cy() + ch() - 14, 16, 14}; }
    private int[] ctUpload() { return new int[]{cx(), ctTop(), 120, 16}; }
    private int[] ctClaim() { return new int[]{cx() + 124, ctTop(), 128, 16}; }
    private int[] ctPlayer() { return new int[]{cx(), ctTop(), cw(), playerH()}; }
    private int[] ctWatchBtn(int i) {
        int[] xs = {0, 80, 188};
        int[] ws = {76, 104, 56};
        return new int[]{cx() + xs[i], cy() + ch() - 16, ws[i], 16};
    }

    private void clampWindow() {
        winX = Math.max(0, Math.min(winX, width - winW()));
        winY = Math.max(0, Math.min(winY, height - TB - winH()));
    }

    private void openApp(App a) {
        app = a;
        clampWindow();
    }

    @Override
    protected void init() {
        if (winX < 0) {
            winX = (width - winW()) / 2;
            winY = (height - TB - winH()) / 2;
        }
        clampWindow();
    }

    // ---------- отрисовка ----------
    private static void tickRecording() {
        if (recordingIdea != null && System.currentTimeMillis() - recordStart >= RECORD_MS) {
            readyVideo = recordingIdea;
            recordingIdea = null;
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        tickRecording();
        if (dark) {
            ctx.fillGradient(0, 0, width, height, 0xFF0A1633, 0xFF1B4FA8);
        } else {
            ctx.fillGradient(0, 0, width, height, 0xFF7DB9F5, 0xFFE6F2FF);
        }

        for (int i = 0; i < APPS.length; i++) {
            int[] r = iconRect(i);
            if (in(mouseX, mouseY, r)) {
                ctx.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], 0x30FFFFFF);
            }
            ctx.fill(r[0] + 16, r[1] + 3, r[0] + 40, r[1] + 27, APPS[i].color);
            ctx.drawCenteredTextWithShadow(textRenderer, APPS[i].letter, r[0] + 28, r[1] + 11, 0xFFFFFFFF);
            ctx.drawCenteredTextWithShadow(textRenderer, APPS[i].shortName, r[0] + 28, r[1] + 30, 0xFFFFFFFF);
        }

        if (app != null) {
            drawWindow(ctx, mouseX, mouseY);
        }
        drawTaskbar(ctx, mouseX, mouseY);
        if (startOpen) {
            drawStartMenu(ctx, mouseX, mouseY);
        }
    }

    private void drawTaskbar(DrawContext ctx, int mx, int my) {
        int ty = tbTop();
        ctx.fill(0, ty, width, height, cTaskbar());
        ctx.fill(0, ty, width, ty + 1, dark ? 0x40FFFFFF : 0x30000000);

        for (int i = 0; i < taskCount(); i++) {
            int bx = taskX(i), by = ty + 1;
            boolean active = (i == 0 && startOpen) || (i > 0 && app == APPS[i - 1]);
            if (active) {
                ctx.fill(bx, by, bx + 20, by + 20, dark ? 0x40FFFFFF : 0x30000000);
            } else if (in(mx, my, bx, by, 20, 20)) {
                ctx.fill(bx, by, bx + 20, by + 20, dark ? 0x20FFFFFF : 0x15000000);
            }
            if (i == 0) {
                int ox = bx + 4, oy = by + 4;
                int c = 0xFF3A96DD;
                ctx.fill(ox, oy, ox + 5, oy + 5, c);
                ctx.fill(ox + 6, oy, ox + 11, oy + 5, c);
                ctx.fill(ox, oy + 6, ox + 5, oy + 11, c);
                ctx.fill(ox + 6, oy + 6, ox + 11, oy + 11, c);
            } else {
                App a = APPS[i - 1];
                ctx.fill(bx + 4, by + 4, bx + 16, by + 16, a.color);
                ctx.drawCenteredTextWithShadow(textRenderer, a.letter, bx + 10, by + 6, 0xFFFFFFFF);
                if (app == a) {
                    ctx.fill(bx + 7, by + 18, bx + 13, by + 20, 0xFF4CC2FF);
                }
            }
        }

        LocalDateTime now = LocalDateTime.now();
        String time = now.format(TIME);
        String date = now.format(DATE);
        ctx.drawText(textRenderer, time, width - 6 - textRenderer.getWidth(time), ty + 3, cText(), false);
        ctx.drawText(textRenderer, date, width - 6 - textRenderer.getWidth(date), ty + 12, cText(), false);
    }

    private void drawStartMenu(DrawContext ctx, int mx, int my) {
        int x = menuX(), y = menuY();
        ctx.fill(x, y, x + MENU_W, y + menuH(), dark ? 0xF0262626 : 0xF0F3F3F3);
        ctx.drawBorder(x, y, MENU_W, menuH(), dark ? 0x40FFFFFF : 0x30000000);
        for (int i = 0; i <= APPS.length; i++) {
            int ry = rowY(i);
            if (in(mx, my, x + 4, ry, MENU_W - 8, 16)) {
                ctx.fill(x + 4, ry, x + MENU_W - 4, ry + 16, dark ? 0x30FFFFFF : 0x20000000);
            }
            if (i < APPS.length) {
                ctx.fill(x + 8, ry + 3, x + 18, ry + 13, APPS[i].color);
                ctx.drawText(textRenderer, APPS[i].title, x + 24, ry + 4, cText(), false);
            } else {
                ctx.drawText(textRenderer, "Выключить", x + 24, ry + 4, cText(), false);
            }
        }
    }

    private void drawWindow(DrawContext ctx, int mx, int my) {
        int x = winX, y = winY, w = winW(), h = winH();
        ctx.fill(x + 2, y + 2, x + w + 2, y + h + 2, 0x55000000);
        ctx.fill(x, y, x + w, y + h, cWindow());
        ctx.fill(x, y, x + w, y + TITLE, cTitle());
        ctx.drawBorder(x, y, w, h, dark ? 0x50FFFFFF : 0x40000000);
        ctx.fill(x + 4, y + 3, x + 12, y + 11, app.color);
        ctx.drawText(textRenderer, app.title, x + 16, y + 3, cText(), false);

        boolean hoverClose = in(mx, my, x + w - 18, y, 18, TITLE);
        if (hoverClose) {
            ctx.fill(x + w - 18, y, x + w, y + TITLE, 0xFFC42B1C);
        }
        ctx.drawCenteredTextWithShadow(textRenderer, "x", x + w - 9, y + 3, hoverClose ? 0xFFFFFFFF : cText());

        switch (app) {
            case NOTES -> drawNotes(ctx);
            case IDEAS -> drawIdeas(ctx, mx, my);
            case CRAFTTUBE -> drawTube(ctx, mx, my);
            case GAMES -> drawGames(ctx, mx, my);
            case SETTINGS -> drawSettings(ctx, mx, my);
        }
    }

    private void drawNotes(DrawContext ctx) {
        int x = cx(), y = cy(), w = cw(), h = ch();
        ctx.fill(x, y, x + w, y + h, cField());
        ctx.drawBorder(x, y, w, h, dark ? 0xFF444444 : 0xFFCCCCCC);
        String s = NOTES_TEXT.toString();
        if (s.isEmpty()) {
            ctx.drawText(textRenderer, "Начни печатать...", x + 4, y + 4, cSub(), false);
            return;
        }
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        List<OrderedText> lines = textRenderer.wrapLines(Text.literal(s + (blink ? "_" : " ")), w - 8);
        int maxLines = Math.max(1, (h - 8) / 9);
        int from = Math.max(0, lines.size() - maxLines);
        for (int i = from; i < lines.size(); i++) {
            ctx.drawText(textRenderer, lines.get(i), x + 4, y + 4 + (i - from) * 9, cText(), false);
        }
    }

    private int drawWrapped(DrawContext ctx, String s, int x, int y, int w, int color) {
        for (OrderedText line : textRenderer.wrapLines(Text.literal(s), w)) {
            ctx.drawText(textRenderer, line, x, y, color, false);
            y += 9;
        }
        return y;
    }

    private void drawButton(DrawContext ctx, int[] r, String label, boolean hover, boolean accent) {
        int bg = accent ? (hover ? 0xFF1A8AE6 : 0xFF0078D4) : cButton(hover);
        ctx.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], bg);
        ctx.drawCenteredTextWithShadow(textRenderer, label, r[0] + r[2] / 2, r[1] + (r[3] - 8) / 2, accent ? 0xFFFFFFFF : cText());
    }

    private void drawIdeas(DrawContext ctx, int mx, int my) {
        int x = cx(), w = cw();
        int y = cy();
        ctx.drawText(textRenderer, "Идея для видео:", x, y, cSub(), false);
        y += 11;
        if (idea == null) {
            drawWrapped(ctx, "Нажми «Сгенерировать», потом «Записать», затем выложи видео в Craft Tube.", x, y, w, cText());
        } else {
            y = drawWrapped(ctx, idea.title(), x, y, w, cText()) + 4;
            ctx.drawText(textRenderer, "Превью:", x, y, cSub(), false);
            y += 11;
            y = drawWrapped(ctx, idea.thumbnail(), x, y, w, cText()) + 4;
            ctx.drawText(textRenderer, "Начало видео:", x, y, cSub(), false);
            y += 11;
            drawWrapped(ctx, idea.hook(), x, y, w, cText());
        }

        int btnY = btn(0)[1];
        if (recordingIdea != null) {
            double p = Math.min(1.0, (System.currentTimeMillis() - recordStart) / (double) RECORD_MS);
            ctx.fill(x, btnY - 8, x + w, btnY - 4, dark ? 0xFF444444 : 0xFFCCCCCC);
            ctx.fill(x, btnY - 8, x + (int) (w * p), btnY - 4, 0xFFE5272D);
        } else if (readyVideo != null) {
            ctx.drawText(textRenderer, textRenderer.trimToWidth("Записано: " + readyVideo.title(), w), x, btnY - 11, cSub(), false);
        }

        boolean copied = System.currentTimeMillis() < copiedUntil;
        drawButton(ctx, btn(0), "Сгенерировать", in(mx, my, btn(0)), true);
        drawButton(ctx, btn(1), recordingIdea != null ? "Запись..." : "Записать", in(mx, my, btn(1)), false);
        drawButton(ctx, btn(2), copied ? "Скопировано!" : "Копировать", in(mx, my, btn(2)), false);
    }

    private void drawSettings(DrawContext ctx, int mx, int my) {
        int x = cx(), y = cy();
        ctx.drawText(textRenderer, "Тема оформления:", x, y, cSub(), false);
        drawButton(ctx, btn(0), "Тёмная", in(mx, my, btn(0)), dark);
        drawButton(ctx, btn(1), "Светлая", in(mx, my, btn(1)), !dark);
        ctx.drawText(textRenderer, "Mega OS 11 (стиль Windows 11)", x, y + 40, cText(), false);
        ctx.drawText(textRenderer, "Версия 0.8.0", x, y + 52, cSub(), false);
    }

    // ---------- Игры ----------
    private int[] gmMenuBtn() { return new int[]{cx(), cy(), 56, 14}; }
    private int[] gmResetBtn() { return new int[]{cx() + cw() - 70, cy(), 70, 14}; }
    private int[] gmArea() { return new int[]{cx(), cy() + 18, cw(), ch() - 18}; }
    private int[] gmCard(int i) { return new int[]{cx(), cy() + 18 + i * 40, cw(), 36}; }

    private void drawGames(DrawContext ctx, int mx, int my) {
        if (gameIndex < 0) {
            ctx.drawText(textRenderer, "Выбери игру:", cx(), cy() + 3, cSub(), false);
            for (int i = 0; i < GAME_LIST.length; i++) {
                int[] r = gmCard(i);
                ctx.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], cButton(in(mx, my, r)));
                ctx.fill(r[0] + 4, r[1] + 4, r[0] + 32, r[1] + 32, GAME_LIST[i].color());
                ctx.drawCenteredTextWithShadow(textRenderer, String.valueOf(i + 1), r[0] + 18, r[1] + 14, 0xFFFFFFFF);
                ctx.drawText(textRenderer, GAME_LIST[i].title(), r[0] + 38, r[1] + 6, cText(), false);
                ctx.drawText(textRenderer, textRenderer.trimToWidth(GAME_LIST[i].help(), r[2] - 44), r[0] + 38, r[1] + 20, cSub(), false);
            }
            return;
        }
        MiniGame g = GAME_LIST[gameIndex];
        drawButton(ctx, gmMenuBtn(), "< Меню", in(mx, my, gmMenuBtn()), false);
        ctx.drawText(textRenderer, g.title(), cx() + 62, cy() + 3, cText(), false);
        drawButton(ctx, gmResetBtn(), g.resetLabel(), in(mx, my, gmResetBtn()), false);
        int[] a = gmArea();
        long handle = client != null ? client.getWindow().getHandle() : 0L;
        g.render(ctx, textRenderer, a[0], a[1], a[2], a[3], mx, my, handle);
    }

    private void clickGames(int mx, int my, int button) {
        if (gameIndex < 0) {
            for (int i = 0; i < GAME_LIST.length; i++) {
                if (in(mx, my, gmCard(i))) {
                    gameIndex = i;
                    GAME_LIST[i].onOpen();
                    return;
                }
            }
            return;
        }
        if (in(mx, my, gmMenuBtn())) {
            gameIndex = -1;
            return;
        }
        if (in(mx, my, gmResetBtn())) {
            GAME_LIST[gameIndex].reset();
            return;
        }
        GAME_LIST[gameIndex].mouseClicked(mx, my, button);
    }

    // ---------- Craft Tube ----------
    private static void ensureFeed() {
        if (!FEED.isEmpty()) {
            return;
        }
        Random r = new Random(12345L);
        for (int i = 0; i < 9; i++) {
            VideoIdeaGenerator.Idea id = VideoIdeaGenerator.generate(r);
            FEED.add(new Video(id.title(), CHANNELS[r.nextInt(CHANNELS.length)], 60 + r.nextInt(600),
                    COLORS[r.nextInt(COLORS.length)], 1000 + r.nextInt(900000), 0.5 + r.nextDouble() * 3.0));
        }
    }

    private static String fmtTime(int sec) {
        return (sec / 60) + ":" + String.format(Locale.ROOT, "%02d", sec % 60);
    }

    private static String fmtViews(long v) {
        if (v < 1000) {
            return v + " просм.";
        }
        if (v < 1000000) {
            return (v / 1000) + " тыс. просм.";
        }
        return String.format(Locale.ROOT, "%.1f", v / 1000000.0) + " млн просм.";
    }

    private void watch(Video v, TubeView from) {
        current = v;
        backView = from;
        tubeView = TubeView.WATCH;
        progress = 0;
        playing = true;
        counted = false;
        lastTime = System.currentTimeMillis();
    }

    // --- обмен с сервером ---
    private void requestTube() {
        if (ClientPlayNetworking.canSend(TubeNetworking.REQUEST)) {
            ClientPlayNetworking.send(TubeNetworking.REQUEST, PacketByteBufs.create());
        }
    }

    private void sendUpload(String title) {
        if (ClientPlayNetworking.canSend(TubeNetworking.UPLOAD)) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString(title);
            ClientPlayNetworking.send(TubeNetworking.UPLOAD, buf);
        }
    }

    private void sendClaim() {
        if (ClientPlayNetworking.canSend(TubeNetworking.CLAIM)) {
            ClientPlayNetworking.send(TubeNetworking.CLAIM, PacketByteBufs.create());
        }
    }

    private void drawVideoRow(DrawContext ctx, int[] r, Video v, int mx, int my) {
        if (in(mx, my, r)) {
            ctx.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], dark ? 0x25FFFFFF : 0x15000000);
        }
        ctx.fill(r[0] + 2, r[1] + 3, r[0] + 54, r[1] + 33, v.color);
        String d = fmtTime(v.durationSec);
        int dw = textRenderer.getWidth(d);
        ctx.fill(r[0] + 52 - dw - 2, r[1] + 24, r[0] + 54, r[1] + 33, 0xB0000000);
        ctx.drawText(textRenderer, d, r[0] + 52 - dw, r[1] + 25, 0xFFFFFFFF, false);
        int tx = r[0] + 60;
        int tw = r[2] - 64;
        ctx.drawText(textRenderer, textRenderer.trimToWidth(v.title, tw), tx, r[1] + 4, cText(), false);
        ctx.drawText(textRenderer, textRenderer.trimToWidth(v.channel, tw), tx, r[1] + 15, cSub(), false);
        ctx.drawText(textRenderer, textRenderer.trimToWidth(fmtViews(v.views()), tw), tx, r[1] + 25, cSub(), false);
    }

    private void drawRemoteRow(DrawContext ctx, int[] r, TubeClientState.RemoteVideo v) {
        ctx.fill(r[0] + 2, r[1] + 3, r[0] + 54, r[1] + 33, COLORS[Math.floorMod(v.title().hashCode(), COLORS.length)]);
        ctx.drawCenteredTextWithShadow(textRenderer, "Я", r[0] + 28, r[1] + 14, 0xFFFFFFFF);
        int tx = r[0] + 60;
        int tw = r[2] - 64;
        ctx.drawText(textRenderer, textRenderer.trimToWidth(v.title(), tw), tx, r[1] + 4, cText(), false);
        ctx.drawText(textRenderer, textRenderer.trimToWidth(fmtViews(v.views()), tw), tx, r[1] + 15, cSub(), false);
        ctx.drawText(textRenderer, textRenderer.trimToWidth(v.likes() + " лайков", tw), tx, r[1] + 25, cSub(), false);
    }

    private void drawScene(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fillGradient(x, y, x + w, y + h, 0xFF4FA3E8, 0xFFBFE3FF);
        int groundY = y + h - 22;
        int blockW = Math.max(18, w / 8);
        for (int bx = x; bx < x + w; bx += blockW * 2) {
            int bx2 = Math.min(bx + blockW, x + w);
            ctx.fill(bx, groundY, bx2, y + h, 0xFF6B4A2B);
            ctx.fill(bx, groundY, bx2, groundY + 4, 0xFF4CAF50);
        }
        double lap = (progress * 8.0) % 1.0;
        int px = x + 4 + (int) (progress * (w - 40));
        int jump = (int) (Math.sin(lap * Math.PI) * 18);
        int py = groundY - 12 - jump;
        ctx.fill(px, py, px + 6, py + 6, 0xFFE0B48A);
        ctx.fill(px, py + 6, px + 6, py + 12, 0xFF3A6FD8);
        int vx = Math.min(px + 22, x + w - 8);
        int vy = groundY - 12 - (int) (Math.abs(Math.sin(progress * 60.0)) * 3);
        ctx.fill(vx, vy, vx + 6, vy + 6, 0xFFC9A07A);
        ctx.fill(vx, vy + 6, vx + 6, vy + 12, 0xFF8B5A2B);
        if (!playing) {
            ctx.fill(x, y, x + w, y + h, 0x66000000);
            String label = progress >= 1.0 ? "Повторить" : (progress > 0 ? "Пауза" : "Смотреть");
            ctx.drawCenteredTextWithShadow(textRenderer, label, x + w / 2, y + h / 2 - 4, 0xFFFFFFFF);
        }
    }

    private void drawTube(DrawContext ctx, int mx, int my) {
        ensureFeed();
        long now = System.currentTimeMillis();
        if (tubeView == TubeView.WATCH && playing) {
            progress += (now - lastTime) / 20000.0;
            if (progress >= 1.0) {
                progress = 1.0;
                playing = false;
                if (!counted && current != null) {
                    current.extraViews++;
                    counted = true;
                }
            }
        }
        lastTime = now;

        int x = cx(), y = cy(), w = cw();
        ctx.fill(x, y + 1, x + 14, y + 13, 0xFFE5272D);
        ctx.drawText(textRenderer, ">", x + 5, y + 3, 0xFFFFFFFF, false);
        ctx.drawText(textRenderer, "Craft Tube", x + 18, y + 3, cText(), false);
        boolean homeActive = tubeView == TubeView.HOME || tubeView == TubeView.WATCH;
        drawButton(ctx, ctTab(0), "Главная", in(mx, my, ctTab(0)), homeActive);
        drawButton(ctx, ctTab(1), "Мой канал", in(mx, my, ctTab(1)), !homeActive);

        if (tubeView == TubeView.HOME) {
            int per = rowsPerPage();
            int pages = Math.max(1, (FEED.size() + per - 1) / per);
            page = Math.max(0, Math.min(page, pages - 1));
            for (int r = 0; r < per; r++) {
                int idx = page * per + r;
                if (idx >= FEED.size()) {
                    break;
                }
                drawVideoRow(ctx, ctRow(r), FEED.get(idx), mx, my);
            }
            drawButton(ctx, ctPrev(), "<", in(mx, my, ctPrev()), false);
            drawButton(ctx, ctNext(), ">", in(mx, my, ctNext()), false);
            String pt = (page + 1) + "/" + pages;
            ctx.drawText(textRenderer, pt, x + w - 40 - textRenderer.getWidth(pt), y + ch() - 11, cSub(), false);
        } else if (tubeView == TubeView.CHANNEL) {
            if (now - lastRequest > 2000L) {
                requestTube();
                lastRequest = now;
            }
            List<TubeClientState.RemoteVideo> mine = TubeClientState.videos;
            long totalViews = 0;
            long totalLikes = 0;
            for (TubeClientState.RemoteVideo v : mine) {
                totalViews += v.views();
                totalLikes += v.likes();
            }
            drawButton(ctx, ctUpload(), "Выложить видео", in(mx, my, ctUpload()), readyVideo != null);
            drawButton(ctx, ctClaim(), "Забрать монеты: " + TubeClientState.unclaimed, in(mx, my, ctClaim()), TubeClientState.unclaimed > 0);
            ctx.drawText(textRenderer, textRenderer.trimToWidth("Видео: " + mine.size() + " | Просмотры: " + totalViews + " | Лайки: " + totalLikes, w), x, ctTop() + 20, cText(), false);
            String ready = readyVideo != null ? readyVideo.title() : "нет (запиши видео в «Идеи»)";
            ctx.drawText(textRenderer, textRenderer.trimToWidth("К выкладке: " + ready, w), x, ctTop() + 29, cSub(), false);
            int per = chRowsPerPage();
            for (int r = 0; r < per; r++) {
                int idx = mine.size() - 1 - r;
                if (idx < 0) {
                    break;
                }
                drawRemoteRow(ctx, ctChRow(r), mine.get(idx));
            }
        } else if (current != null) {
            int[] p = ctPlayer();
            drawScene(ctx, p[0], p[1], p[2], p[3]);
            int by = p[1] + p[3] + 3;
            ctx.fill(x, by, x + w, by + 4, dark ? 0xFF444444 : 0xFFCCCCCC);
            ctx.fill(x, by, x + (int) (w * progress), by + 4, 0xFFE5272D);
            String tm = fmtTime((int) (current.durationSec * progress)) + " / " + fmtTime(current.durationSec);
            int tmw = textRenderer.getWidth(tm);
            ctx.drawText(textRenderer, tm, x + w - tmw, by + 7, cSub(), false);
            ctx.drawText(textRenderer, textRenderer.trimToWidth(current.title, w - tmw - 6), x, by + 7, cText(), false);
            String meta = current.channel + " | " + fmtViews(current.views()) + " | " + current.likes() + " лайков";
            ctx.drawText(textRenderer, textRenderer.trimToWidth(meta, w), x, by + 17, cSub(), false);
            int btnY = y + ch() - 16;
            if (btnY - 11 > by + 27) {
                String c = "Комментарий: " + COMMENTS[Math.floorMod(current.title.hashCode(), COMMENTS.length)];
                ctx.drawText(textRenderer, textRenderer.trimToWidth(c, w), x, btnY - 11, cSub(), false);
            }
            drawButton(ctx, ctWatchBtn(0), current.liked ? "Лайк +" : "Лайк", in(mx, my, ctWatchBtn(0)), current.liked);
            boolean sub = SUBS.contains(current.channel);
            drawButton(ctx, ctWatchBtn(1), sub ? "Вы подписаны" : "Подписаться", in(mx, my, ctWatchBtn(1)), !sub);
            drawButton(ctx, ctWatchBtn(2), "Назад", in(mx, my, ctWatchBtn(2)), false);
        }
    }

    private void clickTube(int mx, int my) {
        ensureFeed();
        if (in(mx, my, ctTab(0))) {
            tubeView = TubeView.HOME;
            playing = false;
            return;
        }
        if (in(mx, my, ctTab(1))) {
            tubeView = TubeView.CHANNEL;
            playing = false;
            lastRequest = 0L;
            return;
        }
        if (tubeView == TubeView.HOME) {
            int per = rowsPerPage();
            int pages = Math.max(1, (FEED.size() + per - 1) / per);
            if (in(mx, my, ctPrev())) {
                page = Math.max(0, page - 1);
                return;
            }
            if (in(mx, my, ctNext())) {
                page = Math.min(pages - 1, page + 1);
                return;
            }
            for (int r = 0; r < per; r++) {
                int idx = page * per + r;
                if (idx < FEED.size() && in(mx, my, ctRow(r))) {
                    watch(FEED.get(idx), TubeView.HOME);
                    return;
                }
            }
        } else if (tubeView == TubeView.CHANNEL) {
            if (in(mx, my, ctUpload())) {
                if (readyVideo != null) {
                    sendUpload(readyVideo.title());
                    readyVideo = null;
                    lastRequest = 0L;
                }
                return;
            }
            if (in(mx, my, ctClaim())) {
                if (TubeClientState.unclaimed > 0) {
                    sendClaim();
                }
            }
        } else if (current != null) {
            if (in(mx, my, ctPlayer())) {
                if (progress >= 1.0) {
                    progress = 0;
                    counted = false;
                    playing = true;
                } else {
                    playing = !playing;
                }
                lastTime = System.currentTimeMillis();
            } else if (in(mx, my, ctWatchBtn(0))) {
                current.liked = !current.liked;
            } else if (in(mx, my, ctWatchBtn(1))) {
                if (!SUBS.add(current.channel)) {
                    SUBS.remove(current.channel);
                }
            } else if (in(mx, my, ctWatchBtn(2))) {
                tubeView = backView;
                playing = false;
            }
        }
    }

    // ---------- ввод ----------
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX, my = (int) mouseY;
        if (button == 1 && app == App.GAMES && gameIndex >= 0
                && in(mx, my, winX, winY + TITLE, winW(), winH() - TITLE)) {
            GAME_LIST[gameIndex].mouseClicked(mx, my, 1);
            return true;
        }
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (startOpen && in(mx, my, menuX(), menuY(), MENU_W, menuH())) {
            for (int i = 0; i <= APPS.length; i++) {
                if (in(mx, my, menuX() + 4, rowY(i), MENU_W - 8, 16)) {
                    if (i < APPS.length) {
                        openApp(APPS[i]);
                        startOpen = false;
                    } else {
                        close();
                    }
                    return true;
                }
            }
            return true;
        }

        if (my >= tbTop()) {
            for (int i = 0; i < taskCount(); i++) {
                if (in(mx, my, taskX(i), tbTop() + 1, 20, 20)) {
                    if (i == 0) {
                        startOpen = !startOpen;
                    } else {
                        App a = APPS[i - 1];
                        if (app == a) {
                            app = null;
                        } else {
                            openApp(a);
                        }
                        startOpen = false;
                    }
                    return true;
                }
            }
            startOpen = false;
            return true;
        }

        startOpen = false;

        if (app != null && in(mx, my, winX, winY, winW(), winH())) {
            if (in(mx, my, winX + winW() - 18, winY, 18, TITLE)) {
                app = null;
                playing = false;
            } else if (my < winY + TITLE) {
                dragging = true;
                dragOffX = mx - winX;
                dragOffY = my - winY;
            } else {
                clickContent(mx, my);
            }
            return true;
        }

        for (int i = 0; i < APPS.length; i++) {
            if (in(mx, my, iconRect(i))) {
                openApp(APPS[i]);
                return true;
            }
        }
        return true;
    }

    private void clickContent(int mx, int my) {
        if (app == App.IDEAS) {
            if (in(mx, my, btn(0))) {
                idea = VideoIdeaGenerator.generate(RANDOM);
            } else if (in(mx, my, btn(1))) {
                if (idea != null && recordingIdea == null) {
                    recordingIdea = idea;
                    recordStart = System.currentTimeMillis();
                }
            } else if (in(mx, my, btn(2)) && idea != null && client != null) {
                client.keyboard.setClipboard(idea.title() + "\nПревью: " + idea.thumbnail() + "\nНачало: " + idea.hook());
                copiedUntil = System.currentTimeMillis() + 1500;
            }
        } else if (app == App.SETTINGS) {
            if (in(mx, my, btn(0))) {
                dark = true;
            } else if (in(mx, my, btn(1))) {
                dark = false;
            }
        } else if (app == App.CRAFTTUBE) {
            clickTube(mx, my);
        } else if (app == App.GAMES) {
            clickGames(mx, my, 0);
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            winX = (int) mouseX - dragOffX;
            winY = (int) mouseY - dragOffY;
            clampWindow();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private static void append(String s) {
        for (char c : s.toCharArray()) {
            if (NOTES_TEXT.length() >= 2000) {
                break;
            }
            if (c == '\n' || SharedConstants.isValidChar(c)) {
                NOTES_TEXT.append(c);
            }
        }
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (app == App.NOTES && SharedConstants.isValidChar(chr)) {
            append(String.valueOf(chr));
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (app == App.GAMES && gameIndex >= 0 && keyCode != GLFW.GLFW_KEY_ESCAPE
                && GAME_LIST[gameIndex].keyPressed(keyCode)) {
            return true;
        }
        if (app == App.NOTES) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (NOTES_TEXT.length() > 0) {
                    NOTES_TEXT.deleteCharAt(NOTES_TEXT.length() - 1);
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                append("\n");
                return true;
            }
            if (Screen.isPaste(keyCode) && client != null) {
                append(client.keyboard.getClipboard());
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
