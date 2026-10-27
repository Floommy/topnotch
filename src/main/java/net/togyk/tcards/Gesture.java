package net.togyk.tcards;

import net.minecraft.ChatFormatting;

/** Order = network id. Only append new gestures at the end. */
public enum Gesture {
    // Blackjack
    HIT("HIT", ChatFormatting.GREEN, 0x55FF55, 1600),
    STAND("STAND", ChatFormatting.RED, 0xFF5555, 1600),
    DOUBLE("DOUBLE", ChatFormatting.GOLD, 0xFFAA00, 1600),
    SPLIT("SPLIT", ChatFormatting.AQUA, 0x55FFFF, 1600),
    // Fun
    WAVE("Wave", ChatFormatting.WHITE, 0xDDDDDD, 2000),
    CHEER("Cheer", ChatFormatting.WHITE, 0xDDDDDD, 2000),
    FACEPALM("Facepalm", ChatFormatting.WHITE, 0xDDDDDD, 2200),
    CLAP("Clap", ChatFormatting.WHITE, 0xDDDDDD, 2000),
    POINT("Point", ChatFormatting.WHITE, 0xDDDDDD, 1800),
    SHRUG("Shrug", ChatFormatting.WHITE, 0xDDDDDD, 1600),
    DRUM("Drum", ChatFormatting.WHITE, 0xDDDDDD, 2500),
    BECKON("Come here", ChatFormatting.WHITE, 0xDDDDDD, 2000),
    SWEAT("Sweat", ChatFormatting.WHITE, 0xDDDDDD, 2600),
    ANGRY("Angry", ChatFormatting.RED, 0xFF5555, 2200);

    public final String label;
    public final ChatFormatting color;
    public final int rgb;
    public final long durationMs;

    Gesture(String label, ChatFormatting color, int rgb, long durationMs) {
        this.label = label;
        this.color = color;
        this.rgb = rgb;
        this.durationMs = durationMs;
    }

    public static Gesture byId(int id) {
        Gesture[] values = values();
        return id >= 0 && id < values.length ? values[id] : null;
    }
}
