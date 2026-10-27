package net.togyk.tcards.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.togyk.tcards.Gesture;

/** Hold the gesture key, move the mouse toward a gesture, release (or click) to show it. */
@Environment(EnvType.CLIENT)
public class GestureWheelScreen extends Screen {
    private static final int DEAD_ZONE = 20;
    private static final double STRETCH = 1.4; // a bit wider than tall, so labels never overlap
    // inner ring: blackjack gestures on the four main directions (clockwise from the top)
    private static final Gesture[] INNER = {Gesture.HIT, Gesture.STAND, Gesture.DOUBLE, Gesture.SPLIT};
    // outer ring: fun gestures, clockwise from the top
    private static final Gesture[] OUTER = {
            Gesture.WAVE, Gesture.FACEPALM, Gesture.ANGRY, Gesture.DRUM, Gesture.POINT, Gesture.SWEAT,
            Gesture.CLAP, Gesture.BECKON, Gesture.SHRUG, Gesture.CHEER};

    private int mouseX, mouseY;
    private boolean done;

    public GestureWheelScreen() {
        super(Component.literal("Gestures"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int radius() {
        return Math.max(60, Math.min(105, Math.min(height / 2 - 25, (int) ((width / 2 - 90) / 1.4))));
    }

    private static double angle(double dx, double dy) {
        double a = Math.toDegrees(Math.atan2(dx, -dy));
        return a < 0 ? a + 360 : a;
    }

    private Gesture selected() {
        double dx = (mouseX - width / 2) / STRETCH, dy = mouseY - height / 2;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < DEAD_ZONE) return null;
        Gesture[] ring = dist < radius() * 0.72 ? INNER : OUTER;
        return ring[(int) Math.round(angle(dx, dy) / (360.0 / ring.length)) % ring.length];
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mx, int my, float partialTick) {
        graphics.fill(0, 0, width, height, 0x30000000); // light tint, world stays visible
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mx, int my, float partialTick) {
        super.extractRenderState(graphics, mx, my, partialTick);
        this.mouseX = mx;
        this.mouseY = my;
        int cx = width / 2, cy = height / 2, r = radius();
        Gesture sel = selected();

        graphics.fill(cx - 2, cy - 2, cx + 2, cy + 2, 0xC0FFFFFF);
        drawRing(graphics, INNER, (int) (r * 0.42), cx, cy, sel);
        drawRing(graphics, OUTER, r, cx, cy, sel);
        graphics.centeredText(font, Component.literal("Release the key or click"), cx, cy + r + 14, 0xA0FFFFFF);
    }

    private void drawRing(GuiGraphicsExtractor graphics, Gesture[] ring, int r, int cx, int cy, Gesture sel) {
        for (int i = 0; i < ring.length; i++) {
            Gesture g = ring[i];
            double a = Math.toRadians(i * 360.0 / ring.length);
            int x = cx + (int) Math.round(Math.sin(a) * r * STRETCH);
            int y = cy - (int) Math.round(Math.cos(a) * r);
            int w = font.width(g.label) + 8;
            boolean active = g == sel;
            boolean blackjack = g.ordinal() <= Gesture.SPLIT.ordinal();
            if (active) {
                graphics.fill(x - w / 2 - 1, y - 7, x + w / 2 + 1, y + 7, 0xFFFFFFFF);
                graphics.fill(x - w / 2, y - 6, x + w / 2, y + 6, 0xF0000000 | g.rgb);
            } else {
                graphics.fill(x - w / 2, y - 6, x + w / 2, y + 6, 0xB0000000);
            }
            int textColor = active ? 0xFF000000 : (blackjack ? 0xFF000000 | g.rgb : 0xFFFFFFFF);
            graphics.centeredText(font, Component.literal(g.label), x, y - 4, textColor);
        }
    }

    private void confirm() {
        if (done) return;
        done = true;
        Gesture g = selected();
        if (g != null) GestureClient.send(g);
        onClose();
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (TableClient.GESTURE_KEY != null && TableClient.GESTURE_KEY.matches(event)) {
            confirm();
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (TableClient.GESTURE_KEY != null && TableClient.GESTURE_KEY.matchesMouse(event)) {
            confirm();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        this.mouseX = (int) event.x();
        this.mouseY = (int) event.y();
        confirm();
        return true;
    }
}
