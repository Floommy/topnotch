package com.yori3o.yo_hooks.common.config.client;

import com.yori3o.yo_hooks.common.config.categories.ClientConfig;
import com.yori3o.yo_hooks.common.config.categories.CommonConfig;
import com.yori3o.yo_hooks.common.config.categories.OverlapConfig;
import com.yori3o.yo_hooks.common.config.categories.ServerConfig;
import com.yori3o.yo_hooks.common.config.categories.VrConfig;
import com.yori3o.yo_hooks.common.event.ServerEvents;
import com.yori3o.yo_hooks.common.util.PhysicVariables;
import com.yori3o.yo_hooks.impl.PlatformUtil;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerPlayer;

public class ClientConfigManager {
    public static void save() {
        ClientConfig.HANDLER.save();
        CommonConfig.HANDLER.save();
        ServerConfig.HANDLER.save();
        OverlapConfig.HANDLER.save();
        if (PlatformUtil.isModLoaded("vivecraft")) {
            VrConfig.HANDLER.save();
        }
        if (Minecraft.getInstance().isLocalServer()) {
            PhysicVariables.updateCommonVariables(((CommonConfig)CommonConfig.HANDLER.instance()).softHook, ((CommonConfig)CommonConfig.HANDLER.instance()).stiffness, ((CommonConfig)CommonConfig.HANDLER.instance()).climbSpeed);
            PhysicVariables.updateFunnyModeConfig(((CommonConfig)CommonConfig.HANDLER.instance()).funnyMode);
            ClientConfigManager.sendCommonConfigToAllPlayersInMultiplayer();
        }
    }

    public static void sendCommonConfigToAllPlayersInMultiplayer() {
        IntegratedServer server;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer().isPublished() && (server = mc.getSingleplayerServer()) != null) {
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            ServerPlayer host = server.getPlayerList().getPlayer(mc.player.getUUID());
            for (ServerPlayer p : players) {
                if (host != null && p.getUUID().equals(host.getUUID())) continue;
                ServerEvents.sendConfigToNewPlayer(p);
            }
        }
    }
}

