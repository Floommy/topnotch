package net.togyk.tog_yks_cards.property;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.togyk.tog_yks_cards.component.ModComponents;

public record HasProperty(DataComponentType<?> componentType) implements SelectItemModelProperty<Boolean>
{
    public static final PrimitiveCodec<Boolean> VALUE_CODEC = Codec.BOOL;
    public static final SelectItemModelProperty.Type<HasProperty, Boolean> TYPE = SelectItemModelProperty.Type.create(RecordCodecBuilder.mapCodec(instance -> instance.group(DataComponentType.CODEC.optionalFieldOf("component", ModComponents.CARDS).forGetter(HasProperty::componentType)).apply(instance, HasProperty::new)), VALUE_CODEC);

    public Boolean get(ItemStack stack, ClientLevel clientLevel, LivingEntity livingEntity, int i, ItemDisplayContext itemDisplayContext) {
        return stack.has(this.componentType);
    }

    public
    SelectItemModelProperty.Type<HasProperty, Boolean> type() {
        return TYPE;
    }

    public Codec<Boolean> valueCodec() {
        return VALUE_CODEC;
    }
}

