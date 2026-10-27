package com.yori3o.yo_hooks.common.config.categories;

import com.yori3o.yo_hooks.common.YoHooks;
import com.yori3o.yo_hooks.common.config.ConfigFile;

public class CommonConfig {
    public static ConfigFile<CommonConfig> HANDLER = new ConfigFile<>(CommonConfig.class, () -> YoHooks.CONFIG_FOLDER.resolve("yo_hooks-common.json"), CommonConfig::new);
    public float climbSpeed = 0.115f;
    public boolean softHook = false;
    public float stiffness = 0.1f;
    public boolean funnyMode = false;
}
