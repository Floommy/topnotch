package net.togyk.tog_yks_cards.client.menu;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.togyk.tog_yks_cards.client.menu.SelectCardMenu;

public class ModMenuTypes {
    public static final MenuType<SelectCardMenu> CARD_SELECT = ModMenuTypes.register("card_select", SelectCardMenu::new);

    public static <T extends AbstractContainerMenu> MenuType<T> register(String name,
    MenuType.MenuSupplier<T> constructor) {
        return Registry.register(BuiltInRegistries.MENU, name, new MenuType<T>(constructor, FeatureFlagSet.of()));
    }

    public static void registerMenuTypes() {
    }
}

