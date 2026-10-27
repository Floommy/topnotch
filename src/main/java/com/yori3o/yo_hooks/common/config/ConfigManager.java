package com.yori3o.yo_hooks.common.config;

import com.yori3o.yo_hooks.common.config.categories.ClientConfig;
import com.yori3o.yo_hooks.common.config.categories.CommonConfig;
import com.yori3o.yo_hooks.common.config.categories.OverlapConfig;
import com.yori3o.yo_hooks.common.config.categories.ServerConfig;
import com.yori3o.yo_hooks.common.config.categories.VrConfig;
import com.yori3o.yo_hooks.common.hookregistry.HookDefinition;
import com.yori3o.yo_hooks.common.util.PhysicVariables;
import com.yori3o.yo_hooks.impl.PlatformUtil;

public class ConfigManager {
    public static CommonConfig common() {
        return (CommonConfig)CommonConfig.HANDLER.instance();
    }

    public static ClientConfig client() {
        return (ClientConfig)ClientConfig.HANDLER.instance();
    }

    public static ServerConfig server() {
        return (ServerConfig)ServerConfig.HANDLER.instance();
    }

    public static OverlapConfig overlap() {
        return (OverlapConfig)OverlapConfig.HANDLER.instance();
    }

    public static VrConfig vr() {
        return (VrConfig)VrConfig.HANDLER.instance();
    }

    public static void loadCommon() {
        CommonConfig.HANDLER.load();
        PhysicVariables.updateCommonVariables(ConfigManager.common().softHook, ConfigManager.common().stiffness, ConfigManager.common().climbSpeed);
        PhysicVariables.updateFunnyModeConfig(ConfigManager.common().funnyMode);
    }

    public static void loadClient() {
        ClientConfig.HANDLER.load();
        if (PlatformUtil.isModLoaded("vivecraft")) {
            VrConfig.HANDLER.load();
        }
    }

    public static void loadServer() {
        ServerConfig.HANDLER.load();
    }

    public static void loadOverlap() {
        OverlapConfig.HANDLER.load();
    }

    public static void commonConfigUpdate(CommonConfig a) {
        ConfigManager.common().softHook = a.softHook;
        ConfigManager.common().stiffness = a.stiffness;
        ConfigManager.common().climbSpeed = a.climbSpeed;
        ConfigManager.common().funnyMode = a.funnyMode;
        PhysicVariables.updateCommonVariables(a.softHook, a.stiffness, a.climbSpeed);
        PhysicVariables.updateFunnyModeConfig(a.funnyMode);
    }

    public static void addNewHookToOverlaps(String material, int durability, int range, int damage) {
        OverlapConfig ocv = (OverlapConfig)OverlapConfig.HANDLER.instance();
        if (!ocv.durabilityOverlap.containsKey(material)) {
            ocv.durabilityOverlap.put(material, durability);
        }
        if (!ocv.rangeOverlap.containsKey(material)) {
            ocv.rangeOverlap.put(material, range);
        }
        if (!ocv.damageOverlap.containsKey(material)) {
            ocv.damageOverlap.put(material, damage);
        }
    }

    public static HookDefinition setOverlapValuesToHook(HookDefinition def) {
        String material = def.id;
        OverlapConfig ocv = (OverlapConfig)OverlapConfig.HANDLER.instance();
        if (ocv.durabilityOverlap.containsKey(material)) {
            def.durabilityOverlap = ocv.durabilityOverlap.get(material);
        }
        if (ocv.rangeOverlap.containsKey(material)) {
            def.lengthOverlap = ocv.rangeOverlap.get(material);
        }
        if (ocv.damageOverlap.containsKey(material)) {
            def.damageOverlap = ocv.damageOverlap.get(material);
        }
        return def;
    }

    public static void saveOverlap() {
        OverlapConfig.HANDLER.save();
    }
}

