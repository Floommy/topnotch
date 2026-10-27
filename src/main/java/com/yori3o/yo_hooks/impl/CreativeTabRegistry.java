package com.yori3o.yo_hooks.impl;

import java.util.function.Supplier;

import com.yori3o.yo_hooks.common.init.ItemRegistry;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

/** TopNotch build: the grappling hooks go into the one TopNotch tab (no own tab). */
public class CreativeTabRegistry {
    public static final ResourceKey<CreativeModeTab> TOPNOTCH_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("tog_yks-cards", "tog_yks-cards"));

    public static void register() {
        CreativeModeTabEvents.modifyOutputEvent(TOPNOTCH_TAB).register(output -> {
            for (Supplier<? extends Item> supplier : ItemRegistry.ALL_ITEMS) output.accept(supplier.get());
        });
    }
}
