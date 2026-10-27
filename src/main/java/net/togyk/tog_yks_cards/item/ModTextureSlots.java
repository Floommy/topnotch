package net.togyk.tog_yks_cards.item;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import net.minecraft.client.data.models.model.TextureSlot;

public class ModTextureSlots {
    public static final TextureSlot SUIT = ModTextureSlots.create("card_suit");

    public static TextureSlot create(String string) {
        Constructor ctor = null;
        try {
            ctor = TextureSlot.class.getDeclaredConstructor(String.class, TextureSlot.class);
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        ctor.setAccessible(true);
        try {
            return (TextureSlot)ctor.newInstance(string, null);
        }
        catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}

