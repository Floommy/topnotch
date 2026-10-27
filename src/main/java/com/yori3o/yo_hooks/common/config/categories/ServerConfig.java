package com.yori3o.yo_hooks.common.config.categories;

import java.util.LinkedList;
import java.util.List;

import com.yori3o.yo_hooks.common.YoHooks;
import com.yori3o.yo_hooks.common.config.ConfigFile;

public class ServerConfig {
    public static ConfigFile<ServerConfig> HANDLER = new ConfigFile<>(ServerConfig.class, () -> YoHooks.CONFIG_FOLDER.resolve("yo_hooks-server.json"), ServerConfig::new);
    public float decreaseSatiety = 1.1f;
    public boolean breakingFragileBlocks = true;
    public List<String> blocksBlacklist = new LinkedList<>();
    public boolean whitelistMode = false;
}
