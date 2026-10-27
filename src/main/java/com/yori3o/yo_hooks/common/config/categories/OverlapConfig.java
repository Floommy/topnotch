package com.yori3o.yo_hooks.common.config.categories;

import java.util.HashMap;
import java.util.Map;

import com.yori3o.yo_hooks.common.YoHooks;
import com.yori3o.yo_hooks.common.config.ConfigFile;

public class OverlapConfig {
    public static ConfigFile<OverlapConfig> HANDLER = new ConfigFile<>(OverlapConfig.class, () -> YoHooks.CONFIG_FOLDER.resolve("overlaps.json"), OverlapConfig::new);
    public Map<String, Integer> durabilityOverlap = new HashMap<>();
    public Map<String, Integer> rangeOverlap = new HashMap<>();
    public Map<String, Integer> damageOverlap = new HashMap<>();
}
