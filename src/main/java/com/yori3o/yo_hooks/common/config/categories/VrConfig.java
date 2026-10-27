package com.yori3o.yo_hooks.common.config.categories;

import com.yori3o.yo_hooks.common.YoHooks;
import com.yori3o.yo_hooks.common.config.ConfigFile;

public class VrConfig {
    public static ConfigFile<VrConfig> HANDLER = new ConfigFile<>(VrConfig.class, () -> YoHooks.CONFIG_FOLDER.resolve("yo_hooks-vr.json"), VrConfig::new);
    public boolean rememberHookHandPosition = true;
    public boolean moveAlongChain = true;
}
