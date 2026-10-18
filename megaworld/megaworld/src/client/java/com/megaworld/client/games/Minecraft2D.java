package com.megaworld.client.games;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

/**
 * Minecraft 2D: боковая песочница. ЛКМ — ломать, ПКМ — ставить, 1-8 — выбор блока,
 * A/D — ходить, W/Пробел — прыжок. Мир генерируется случайно (кнопка «Новый мир»).
 */
public class Minecraft2D extends MiniGame {
    private static final int W = 96;
    private static final int H = 40;
    private static final int TS = 8;           // размер клетки в пикселях GUI
    private static final int HOTBAR_H = 18;
    private static final double HALF_W = 0.35D;
    private static final double BODY_H = 1.8D;
    private static final double REACH = 5.5D;

    private static final int AIR = 0, GRASS = 1, DIRT = 2, STONE = 3, WOOD = 4, LEAVES = 5,
            COAL = 6, GOLD = 7, PLANKS = 8, SAND = 9, GLASS = 10, BEDROCK = 11;
    private static final int[] HOTBAR = {DIRT, STONE, PLANKS, WOOD, LEAVES, SAND, GLASS, GOLD};
    private static final String[] NAMES = {"Воздух", "Трава", "Земля", "Камень", "Дерево", "Листва",
            "Уголь", "Золото", "Доски", "Песок", "Стекло", "Бедрок"};

    private final int[][] world = new int[W][H];
    private final Random random = new Random();
    private double px, py, vx, vy;
    private boolean onGround;
    private int selected = 0;
    private double cooldown = 0.0D;
    private int ax, ay, aw, ah; // область последнего кадра (для кликов по хотбару)

    public Minecraft2D() {
        reset();
    }

    @Override
    public String title() { return "Minecraft 2D"; }

    @Override
    public String help() { return "2D-песочница: копай, строй, исследуй"; }

    @Override
    public int color() { return 0xFF4CAF50; }

    @Override
    public String resetLabel() { return "Новый мир"; }

    @Override
    public void reset() {
        double p1 = random.nextDouble() * 6.28D;
        double p2 = random.nextDouble() * 6.28D;
        int[] surface = new int[W];
        for (int x = 0; x < W; x++) {
            surface[x] = 16 + (int) Math.round(3.0D * Math.sin(x * 0.15D + p1) + 2.0D * Math.sin(x * 0.07D + p2));
        }
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                int id;
                if (y < surface[x]) {
                    id = AIR;
                } else if (y == surface[x]) {
                    id = GRASS;
                } else if (y < surface[x] + 4) {
                    id = DIRT;
                } else {
                    id = STONE;
                    double q = random.nextDouble();
                    if (y > surface[x] + 6 && q < 0.05D) {
                        id = COAL;
                    } else if (y > surface[x] + 12 && q > 0.99D) {
                        id = GOLD;
                    }
                }
                world[x][y] = id;
            }
            world[x][H - 1] = BEDROCK;
        }
        // деревья
        for (int x = 4; x < W - 4; x += random.nextInt(5) + 5) {
            int ground = surface[x];
            for (int i = 1; i <= 4; i++) {
                world[x][ground - i] = WOOD;
            }
            int top = ground - 4;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 0; dy++) {
                    int tx = x + dx;
                    int ty = top + dy;
                    if (tx >= 0 && tx < W && ty >= 0 && world[tx][ty] == AIR) {
                        world[tx][ty] = LEAVES;
                    }
                }
            }
        }
        px = W / 2 + 0.5D;
        py = surface[W / 2] - 0.01D;
        vx = 0;
        vy = 0;
        onGround = false;
    }

    private boolean solid(int tx, int ty) {
        if (tx < 0 || tx >= W) {
            return true;
        }
        if (ty < 0) {
            return false;
        }
        if (ty >= H) {
            return true;
        }
        int id = world[tx][ty];
        return id != AIR && id != LEAVES;
    }

    private boolean collides(double x, double y) {
        int x0 = (int) Math.floor(x - HALF_W);
        int x1 = (int) Math.floor(x + HALF_W - 1.0E-6D);
        int y0 = (int) Math.floor(y - BODY_H);
        int y1 = (int) Math.floor(y - 1.0E-6D);
        for (int ty = y0; ty <= y1; ty++) {
            for (int tx = x0; tx <= x1; tx++) {
                if (solid(tx, ty)) {
                    return true;
                }
            }
        }
        return false;
    }

    private double camX(int w) {
        return Math.max(0.0D, Math.min(px * TS - w / 2.0D, W * TS - w));
    }

    private double camY(int viewH) {
        return Math.max(0.0D, Math.min((py - 1.0D) * TS - viewH / 2.0D, H * TS - viewH));
    }

    private boolean inReach(int tx, int ty) {
        double dx = tx + 0.5D - px;
        double dy = ty + 0.5D - (py - 0.9D);
        return dx * dx + dy * dy <= REACH * REACH;
    }

    private void update(double dt, long window, int mx, int my, int x, int y, int w, int viewH) {
        boolean left = keyDown(window, GLFW.GLFW_KEY_A) || keyDown(window, GLFW.GLFW_KEY_LEFT);
        boolean right = keyDown(window, GLFW.GLFW_KEY_D) || keyDown(window, GLFW.GLFW_KEY_RIGHT);
        boolean jump = keyDown(window, GLFW.GLFW_KEY_W) || keyDown(window, GLFW.GLFW_KEY_UP)
                || keyDown(window, GLFW.GLFW_KEY_SPACE);

        vx = ((right ? 1 : 0) - (left ? 1 : 0)) * 5.0D;
        if (jump && onGround) {
            vy = -9.5D;
            onGround = false;
        }
        vy = Math.min(vy + 30.0D * dt, 20.0D);

        double nx = Math.max(HALF_W, Math.min(px + vx * dt, W - HALF_W));
        if (!collides(nx, py)) {
            px = nx;
        }
        double ny = py + vy * dt;
        onGround = false;
        if (!collides(px, ny)) {
            py = ny;
        } else {
            if (vy > 0) {
                onGround = true;
                py = Math.floor(ny - 1.0E-6D);
            }
            vy = 0;
        }

        cooldown = Math.max(0.0D, cooldown - dt);
        boolean inView = mx >= x && mx < x + w && my >= y && my < y + viewH;
        if (inView && cooldown <= 0.0D) {
            boolean lmb = mouseDown(window, GLFW.GLFW_MOUSE_BUTTON_LEFT);
            boolean rmb = mouseDown(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT);
            if (lmb || rmb) {
                int tx = (int) Math.floor((mx - x + (int) camX(w)) / (double) TS);
                int ty = (int) Math.floor((my - y + (int) camY(viewH)) / (double) TS);
                if (tx >= 0 && tx < W && ty >= 0 && ty < H && inReach(tx, ty)) {
                    if (lmb) {
                        if (world[tx][ty] != AIR && world[tx][ty] != BEDROCK) {
                            world[tx][ty] = AIR;
                            cooldown = 0.12D;
                        }
                    } else if (world[tx][ty] == AIR) {
                        world[tx][ty] = HOTBAR[selected];
                        if (collides(px, py)) {
                            world[tx][ty] = AIR; // нельзя ставить блок внутрь себя
                        } else {
                            cooldown = 0.12D;
                        }
                    }
                }
            }
        }
    }

    private static int baseColor(int id) {
        return switch (id) {
            case GRASS, DIRT -> 0xFF8B5A2B;
            case STONE, COAL, GOLD -> 0xFF7D7D7D;
            case WOOD -> 0xFF6B4A2B;
            case LEAVES -> 0xFF2E7D32;
            case PLANKS -> 0xFFB8863B;
            case SAND -> 0xFFE6D690;
            case GLASS -> 0x80CFEFFF;
            case BEDROCK -> 0xFF2B2B2B;
            default -> 0xFFFF00FF;
        };
    }

    private void drawTile(DrawContext ctx, int sx, int sy, int id, int cx1, int cy1, int cx2, int cy2) {
        clippedFill(ctx, sx, sy, sx + TS, sy + TS, cx1, cy1, cx2, cy2, baseColor(id));
        if (id == GRASS) {
            clippedFill(ctx, sx, sy, sx + TS, sy + 2, cx1, cy1, cx2, cy2, 0xFF4CAF50);
        } else if (id == COAL || id == GOLD) {
            int dot = id == COAL ? 0xFF222222 : 0xFFFFD700;
            clippedFill(ctx, sx + 1, sy + 1, sx + 3, sy + 3, cx1, cy1, cx2, cy2, dot);
            clippedFill(ctx, sx + 5, sy + 2, sx + 7, sy + 4, cx1, cy1, cx2, cy2, dot);
            clippedFill(ctx, sx + 2, sy + 5, sx + 4, sy + 7, cx1, cy1, cx2, cy2, dot);
        } else if (id == LEAVES) {
            clippedFill(ctx, sx + 2, sy + 2, sx + 4, sy + 4, cx1, cy1, cx2, cy2, 0xFF3E9B44);
        } else if (id == PLANKS) {
            clippedFill(ctx, sx, sy + 3, sx + TS, sy + 4, cx1, cy1, cx2, cy2, 0xFF9B6F2B);
        }
        if (id != GLASS) {
            clippedFill(ctx, sx, sy + TS - 1, sx + TS, sy + TS, cx1, cy1, cx2, cy2, 0x22000000);
        }
    }

    @Override
    public void render(DrawContext ctx, TextRenderer font, int x, int y, int w, int h,
                       int mouseX, int mouseY, long window) {
        ax = x;
        ay = y;
        aw = w;
        ah = h;
        int viewH = h - HOTBAR_H;
        update(frameDt(), window, mouseX, mouseY, x, y, w, viewH);

        int icx = (int) camX(w);
        int icy = (int) camY(viewH);
        int cx2 = x + w;
        int cy2 = y + viewH;

        ctx.fillGradient(x, y, cx2, cy2, 0xFF6EC1FF, 0xFFCDEBFF);

        int tx0 = Math.max(0, icx / TS);
        int tx1 = Math.min(W - 1, (icx + w) / TS);
        int ty0 = Math.max(0, icy / TS);
        int ty1 = Math.min(H - 1, (icy + viewH) / TS);
        for (int tx = tx0; tx <= tx1; tx++) {
            for (int ty = ty0; ty <= ty1; ty++) {
                int id = world[tx][ty];
                if (id != AIR) {
                    drawTile(ctx, x + tx * TS - icx, y + ty * TS - icy, id, x, y, cx2, cy2);
                }
            }
        }

        // подсветка клетки под курсором
        if (mouseX >= x && mouseX < cx2 && mouseY >= y && mouseY < cy2) {
            int tx = (int) Math.floor((mouseX - x + icx) / (double) TS);
            int ty = (int) Math.floor((mouseY - y + icy) / (double) TS);
            if (tx >= 0 && tx < W && ty >= 0 && ty < H && inReach(tx, ty)) {
                int sx = x + tx * TS - icx;
                int sy = y + ty * TS - icy;
                if (sx >= x && sy >= y && sx + TS <= cx2 && sy + TS <= cy2) {
                    ctx.drawBorder(sx, sy, TS, TS, 0xFFFFFFFF);
                }
            }
        }

        // игрок
        int feetX = x + (int) Math.round(px * TS) - icx;
        int feetY = y + (int) Math.round(py * TS) - icy;
        int left = feetX - 3;
        int top = feetY - 14;
        clippedFill(ctx, left, top, left + 6, top + 6, x, y, cx2, cy2, 0xFFE0B48A);
        clippedFill(ctx, left, top + 6, left + 6, top + 11, x, y, cx2, cy2, 0xFF3A6FD8);
        clippedFill(ctx, left, top + 11, left + 6, feetY, x, y, cx2, cy2, 0xFF3B4A8A);
        clippedFill(ctx, left + 4, top + 2, left + 5, top + 3, x, y, cx2, cy2, 0xFF222222);

        ctx.drawText(font, font.trimToWidth("ЛКМ ломать | ПКМ ставить | 1-8 блок | A/D идти | W прыжок", w - 6),
                x + 3, y + 3, 0xFFFFFFFF, true);

        // хотбар
        int hbY = y + viewH;
        ctx.fill(x, hbY, cx2, y + h, 0xCC101010);
        int slotW = 18;
        int hx = x + (w - HOTBAR.length * slotW) / 2;
        for (int i = 0; i < HOTBAR.length; i++) {
            int sx = hx + i * slotW;
            boolean sel = i == selected;
            ctx.fill(sx + 1, hbY + 1, sx + slotW - 1, hbY + HOTBAR_H - 1, sel ? 0xFF555555 : 0xFF2A2A2A);
            if (sel) {
                ctx.drawBorder(sx + 1, hbY + 1, slotW - 2, HOTBAR_H - 2, 0xFFFFFFFF);
            }
            ctx.fill(sx + 5, hbY + 5, sx + slotW - 5, hbY + HOTBAR_H - 5, baseColor(HOTBAR[i]));
            ctx.drawText(font, String.valueOf(i + 1), sx + 3, hbY + 2, 0xFFFFFFFF, false);
        }
        ctx.drawText(font, NAMES[HOTBAR[selected]], x + 4, hbY + 5, 0xFFFFFFFF, false);
    }

    @Override
    public boolean keyPressed(int keyCode) {
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode < GLFW.GLFW_KEY_1 + HOTBAR.length) {
            selected = keyCode - GLFW.GLFW_KEY_1;
            return true;
        }
        return keyCode == GLFW.GLFW_KEY_A || keyCode == GLFW.GLFW_KEY_D || keyCode == GLFW.GLFW_KEY_W
                || keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_LEFT
                || keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_UP;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0 && mouseY >= ay + ah - HOTBAR_H && mouseY < ay + ah) {
            int slotW = 18;
            int hx = ax + (aw - HOTBAR.length * slotW) / 2;
            int i = (mouseX - hx) / slotW;
            if (mouseX >= hx && i >= 0 && i < HOTBAR.length) {
                selected = i;
                return true;
            }
        }
        return false;
    }
}
