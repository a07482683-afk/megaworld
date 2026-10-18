package com.megaworld.client.games;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Random;

/** Змейка: стрелки или WASD. Съедай красные блоки и расти. */
public class SnakeGame extends MiniGame {
    private static final int COLS = 30;
    private static final int ROWS = 14;
    private static final int CELL = 10;

    private final ArrayDeque<int[]> body = new ArrayDeque<>(); // голова — первый элемент
    private final Random random = new Random();
    private int dirX, dirY, nextX, nextY;
    private int foodX, foodY;
    private boolean dead;
    private int score, best;
    private double acc;

    public SnakeGame() {
        reset();
    }

    @Override
    public String title() { return "Змейка"; }

    @Override
    public String help() { return "Классика: собирай еду и расти"; }

    @Override
    public int color() { return 0xFF3A96DD; }

    @Override
    public void reset() {
        body.clear();
        body.addLast(new int[]{10, 7});
        body.addLast(new int[]{9, 7});
        body.addLast(new int[]{8, 7});
        dirX = 1;
        dirY = 0;
        nextX = 1;
        nextY = 0;
        dead = false;
        score = 0;
        acc = 0.0D;
        spawnFood();
    }

    private void spawnFood() {
        for (int tries = 0; tries < 500; tries++) {
            int fx = random.nextInt(COLS);
            int fy = random.nextInt(ROWS);
            boolean free = true;
            for (int[] c : body) {
                if (c[0] == fx && c[1] == fy) {
                    free = false;
                    break;
                }
            }
            if (free) {
                foodX = fx;
                foodY = fy;
                return;
            }
        }
    }

    private void step() {
        dirX = nextX;
        dirY = nextY;
        int[] head = body.peekFirst();
        int nx = head[0] + dirX;
        int ny = head[1] + dirY;
        if (nx < 0 || nx >= COLS || ny < 0 || ny >= ROWS) {
            die();
            return;
        }
        boolean eating = nx == foodX && ny == foodY;
        int i = 0;
        int size = body.size();
        for (int[] c : body) {
            i++;
            if (!eating && i == size) {
                break; // хвост освободит клетку
            }
            if (c[0] == nx && c[1] == ny) {
                die();
                return;
            }
        }
        body.addFirst(new int[]{nx, ny});
        if (eating) {
            score++;
            best = Math.max(best, score);
            spawnFood();
        } else {
            body.removeLast();
        }
    }

    private void die() {
        dead = true;
        best = Math.max(best, score);
    }

    @Override
    public void render(DrawContext ctx, TextRenderer font, int x, int y, int w, int h,
                       int mouseX, int mouseY, long window) {
        double dt = frameDt();
        if (!dead) {
            acc += dt;
            double interval = Math.max(0.06D, 0.14D - score * 0.003D);
            while (acc >= interval && !dead) {
                acc -= interval;
                step();
            }
        }

        int bw = COLS * CELL;
        int bh = ROWS * CELL;
        int ox = x + (w - bw) / 2;
        int oy = y + (h - bh) / 2 + 4;
        ctx.fill(x, y, x + w, y + h, 0xFF101418);
        ctx.fill(ox, oy, ox + bw, oy + bh, 0xFF1B2229);
        ctx.drawBorder(ox - 1, oy - 1, bw + 2, bh + 2, 0xFF3A4753);

        ctx.fill(ox + foodX * CELL + 1, oy + foodY * CELL + 1, ox + foodX * CELL + CELL - 1, oy + foodY * CELL + CELL - 1, 0xFFE5484D);

        boolean first = true;
        for (int[] c : body) {
            int color = first ? 0xFF8BE28B : 0xFF4CAF50;
            ctx.fill(ox + c[0] * CELL + 1, oy + c[1] * CELL + 1, ox + c[0] * CELL + CELL - 1, oy + c[1] * CELL + CELL - 1, color);
            first = false;
        }

        ctx.drawText(font, "Счёт: " + score + "   Рекорд: " + best, ox, oy - 11, 0xFFFFFFFF, true);
        if (dead) {
            ctx.fill(ox, oy + bh / 2 - 14, ox + bw, oy + bh / 2 + 14, 0xAA000000);
            ctx.drawCenteredTextWithShadow(font, "Игра окончена", ox + bw / 2, oy + bh / 2 - 9, 0xFFFFFFFF);
            ctx.drawCenteredTextWithShadow(font, "Пробел или клик — заново", ox + bw / 2, oy + bh / 2 + 2, 0xFFB0B0B0);
        }
    }

    @Override
    public boolean keyPressed(int k) {
        if (dead && (k == GLFW.GLFW_KEY_SPACE || k == GLFW.GLFW_KEY_ENTER)) {
            reset();
            return true;
        }
        if ((k == GLFW.GLFW_KEY_UP || k == GLFW.GLFW_KEY_W) && dirY == 0) {
            nextX = 0;
            nextY = -1;
            return true;
        }
        if ((k == GLFW.GLFW_KEY_DOWN || k == GLFW.GLFW_KEY_S) && dirY == 0) {
            nextX = 0;
            nextY = 1;
            return true;
        }
        if ((k == GLFW.GLFW_KEY_LEFT || k == GLFW.GLFW_KEY_A) && dirX == 0) {
            nextX = -1;
            nextY = 0;
            return true;
        }
        if ((k == GLFW.GLFW_KEY_RIGHT || k == GLFW.GLFW_KEY_D) && dirX == 0) {
            nextX = 1;
            nextY = 0;
            return true;
        }
        return k == GLFW.GLFW_KEY_UP || k == GLFW.GLFW_KEY_DOWN || k == GLFW.GLFW_KEY_LEFT
                || k == GLFW.GLFW_KEY_RIGHT || k == GLFW.GLFW_KEY_W || k == GLFW.GLFW_KEY_A
                || k == GLFW.GLFW_KEY_S || k == GLFW.GLFW_KEY_D;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (dead) {
            reset();
            return true;
        }
        return false;
    }
}
