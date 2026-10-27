package com.yori3o.yo_hooks.common.config;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yori3o.yo_hooks.common.util.LoggerUtil;

/**
 * Small JSON config file (TopNotch build: replaces Yet Another Config Lib, same file names and field names,
 * so existing yo_hooks configs keep working).
 */
public final class ConfigFile<T> {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Class<T> type;
    private final Supplier<Path> path;
    private T instance;

    public ConfigFile(Class<T> type, Supplier<Path> path, Supplier<T> defaults) {
        this.type = type;
        this.path = path;
        this.instance = defaults.get();
    }

    public T instance() {
        return instance;
    }

    public void load() {
        Path p = path.get();
        try {
            if (Files.exists(p)) {
                try (Reader r = Files.newBufferedReader(p)) {
                    T read = GSON.fromJson(r, type);
                    if (read != null) instance = read;
                }
            }
            save();   // writes defaults / new fields
        } catch (Exception e) {
            LoggerUtil.errorWithException("Failed to load config " + p + ": ", e);
        }
    }

    public void save() {
        Path p = path.get();
        try {
            Files.createDirectories(p.getParent());
            try (Writer w = Files.newBufferedWriter(p)) {
                GSON.toJson(instance, type, w);
            }
        } catch (Exception e) {
            LoggerUtil.errorWithException("Failed to save config " + p + ": ", e);
        }
    }
}
