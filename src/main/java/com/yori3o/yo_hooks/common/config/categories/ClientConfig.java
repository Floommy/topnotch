package com.yori3o.yo_hooks.common.config.categories;

import com.yori3o.yo_hooks.common.YoHooks;
import com.yori3o.yo_hooks.common.config.ConfigFile;

public class ClientConfig {
    public static ConfigFile<ClientConfig> HANDLER = new ConfigFile<>(ClientConfig.class, () -> YoHooks.CONFIG_FOLDER.resolve("yo_hooks-client.json"), ClientConfig::new);
    public boolean holdHookTightly = false;
    public boolean climbWithMouseWheelScroll = false;
    public boolean usingWhileHoldingFood = false;
    public boolean showDamageOnHit = false;
}
