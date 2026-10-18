package com.megaworld.client.games;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Паркур-бегун: персонаж бежит сам, прыгай через блоки (Пробел, W, стрелка вверх или клик). */
public class RunnerGame extends MiniGame {
    private static final double GRAVITY = 900.0D;
    private static final double JUMP_V = 330.0D;
    private static final int PLAYER_W = 10;
    private static final int PLAYER_H = 16;
    private static final int PLAYER_X = 40;
    private static final int GROUND_H = 24;

    private final List<double[]> obstacles = new ArrayList<>(); // {x, ширина, высота}
    private final Random random = new Random();
    private double jumpY;
    private double vy;
    private double speed;
    private double distance;
    private double gap;
    private boolean dead;
    private int best;

    public RunnerGame() {
        reset();
    }

    @Override
    public String title() { return "Паркур-бегун"; }

    @Override
    public String help() { return "Беги по паркуру и прыгай через блоки"; }

    @Override
    public int color() { return 0xFFF2994A; }

    @Override
    public void reset() {
        obstacles.clear();
        jumpY = 0;
        vy = 0;
        speed = 130.0D;
        distance = 0;
        gap = 200.0D;
        dead = false;
    }

    private void jump() {
        if (dead) {
            reset();
        } else if (jumpY <= 0.0D) {
            vy = JUMP_V;
        }
    }

    private void update(double dt, int areaW) {
        speed = Math.min(320.0D, 130.0D + distance * 0.012D);
        distance += speed * dt;

        vy -= GRAVITY * dt;
        jumpY += vy * dt;
        if (jumpY <= 0.0D) {
            jumpY = 0.0D;
            vy = 0.0D;
        }

        gap -= speed * dt;
        if (gap <= 0.0D) {
            double ow = 8 + random.nextInt(13);
            double oh = 10 + random.nextInt(22);
            obstacles.add(new double[]{areaW + 10, ow, oh});
            gap = 120.0D + random.nextDouble() * 160.0D + speed * 0.4D;
        }

        for (int i = obstacles.size() - 1; i >= 0; i--) {
            double[] o = obstacles.get(i);
            o[0] -= speed * dt;
            if (o[0] + o[1] < 0) {
                obstacles.remove(i);
                continue;
            }
            boolean overlapX = PLAYER_X + 2 < o[0] + o[1] && PLAYER_X + PLAYER_W - 2 > o[0];
            if (overlapX && jumpY < o[2] - 1.0D) {
                dead = true;
                best = Math.max(best, (int) (distance / 10.0D));
            }
        }
    }

    @Override
    public void render(DrawContext ctx, TextRenderer font, int x, int y, int w, int h,
                       int mouseX, int mouseY, long window) {
        double dt = frameDt();
        if (!dead) {
            update(dt, w);
        }

        int gy = y + h - GROUND_H;
        ctx.fillGradient(x, y, x + w, gy, 0xFF4FA3E8, 0xFFBFE3FF);
        ctx.fill(x, gy, x + w, y + h, 0xFF6B4A2B);
        ctx.fill(x, gy, x + w, gy + 3, 0xFF4CAF50);
        for (int sx = -(int) (distance % 16.0D); sx < w; sx += 16) {
            clippedFill(ctx, x + sx, gy + 10, x + sx + 8, gy + 13, x, y, x + w, y + h, 0xFF5A3C20);
        }

        for (double[] o : obstacles) {
            int ox = x + (int) o[0];
            int ow = (int) o[1];
            int oh = (int) o[2];
            clippedFill(ctx, ox, gy - oh, ox + ow, gy, x, y, x + w, y + h, 0xFF7D7D7D);
            clippedFill(ctx, ox, gy - oh, ox + ow, gy - oh + 2, x, y, x + w, y + h, 0xFFA8A8A8);
        }

        int px = x + PLAYER_X;
        int bottom = gy - (int) jumpY;
        int top = bottom - PLAYER_H;
        ctx.fill(px, top, px + PLAYER_W, top + 6, 0xFFE0B48A);
        ctx.fill(px, top + 6, px + PLAYER_W, top + 12, 0xFF3A6FD8);
        ctx.fill(px, top + 12, px + PLAYER_W, bottom, 0xFF3B4A8A);
        ctx.fill(px + 6, top + 2, px + 8, top + 4, 0xFF222222);

        ctx.drawText(font, "Счёт: " + (int) (distance / 10.0D) + "   Рекорд: " + best, x + 4, y + 4, 0xFFFFFFFF, true);
        if (dead) {
            ctx.fill(x, y + h / 2 - 16, x + w, y + h / 2 + 16, 0xAA000000);
            ctx.drawCenteredTextWithShadow(font, "Игра окончена", x + w / 2, y + h / 2 - 10, 0xFFFFFFFF);
            ctx.drawCenteredTextWithShadow(font, "Пробел или клик — заново", x + w / 2, y + h / 2 + 2, 0xFFB0B0B0);
        }
    }

    @Override
    public boolean keyPressed(int k) {
        if (k == GLFW.GLFW_KEY_SPACE || k == GLFW.GLFW_KEY_W || k == GLFW.GLFW_KEY_UP || k == GLFW.GLFW_KEY_ENTER) {
            jump();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0) {
            jump();
            return true;
        }
        return false;
    }
}
