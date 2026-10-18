package com.megaworld.client.games;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

/** Базовый класс мини-игры внутри компьютера. Игра рисуется в прямоугольнике (x, y, w, h). */
public abstract class MiniGame {
    private long lastNanos = 0L;

    public abstract String title();

    /** Короткое описание для карточки в меню. */
    public abstract String help();

    public abstract int color();

    public String resetLabel() {
        return "Заново";
    }

    public abstract void reset();

    public void onOpen() {
        lastNanos = 0L;
    }

    /** window — дескриптор окна GLFW (для опроса зажатых клавиш и кнопок мыши). */
    public abstract void render(DrawContext ctx, TextRenderer font, int x, int y, int w, int h,
                                int mouseX, int mouseY, long window);

    public boolean keyPressed(int keyCode) {
        return false;
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        return false;
    }

    /** Время с прошлого кадра в секундах (не больше 0.05, чтобы физика не «взрывалась»). */
    protected double frameDt() {
        long now = System.nanoTime();
        double dt = lastNanos == 0L ? 0.0D : (now - lastNanos) / 1.0E9D;
        lastNanos = now;
        return Math.min(dt, 0.05D);
    }

    protected static boolean keyDown(long window, int key) {
        return window != 0L && GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }

    protected static boolean mouseDown(long window, int button) {
        return window != 0L && GLFW.glfwGetMouseButton(window, button) == GLFW.GLFW_PRESS;
    }

    /** Заливка, обрезанная по рамке (cx1, cy1, cx2, cy2). */
    protected static void clippedFill(DrawContext ctx, int x1, int y1, int x2, int y2,
                                      int cx1, int cy1, int cx2, int cy2, int color) {
        int a = Math.max(x1, cx1);
        int b = Math.max(y1, cy1);
        int c = Math.min(x2, cx2);
        int d = Math.min(y2, cy2);
        if (c > a && d > b) {
            ctx.fill(a, b, c, d, color);
        }
    }
}
