package net.togyk.tog_yks_cards.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.PrimitiveCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.togyk.tog_yks_cards.component.ModComponents;

public record EmptyProperty() implements SelectItemModelProperty<Boolean>
{
    public static final PrimitiveCodec<Boolean> VALUE_CODEC = Codec.BOOL;
    public static final SelectItemModelProperty.Type<EmptyProperty, Boolean> TYPE = SelectItemModelProperty.Type.create(MapCodec.unit(new EmptyProperty()), VALUE_CODEC);

    public Boolean get(ItemStack stack, ClientLevel clientLevel, LivingEntity livingEntity, int i, ItemDisplayContext itemDisplayContext) {
        return (Boolean)stack.getOrDefault(ModComponents.EMPTY, false);
    }

    public
    SelectItemModelProperty.Type<EmptyProperty, Boolean> type() {
        return TYPE;
    }

    public Codec<Boolean> valueCodec() {
        return VALUE_CODEC;
    }
}

