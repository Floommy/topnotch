package net.togyk.tog_yks_cards.property;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.togyk.tog_yks_cards.component.ModComponents;

public record DeckSelectProperty(String name) implements SelectItemModelProperty<String>
{
    public static final PrimitiveCodec<String> VALUE_CODEC = Codec.STRING;
    public static final SelectItemModelProperty.Type<DeckSelectProperty, String> TYPE = SelectItemModelProperty.Type.create(RecordCodecBuilder.mapCodec(instance -> instance.group(Codec.STRING.optionalFieldOf("name", "").forGetter(DeckSelectProperty::name)).apply(instance, DeckSelectProperty::new)), VALUE_CODEC);

    public String get(ItemStack stack, ClientLevel clientLevel, LivingEntity livingEntity, int i, ItemDisplayContext itemDisplayContext) {
        Map<String, String> data = stack.getOrDefault(ModComponents.CARD_DATA, new HashMap<String, String>());
        return data.getOrDefault(this.name, "");
    }

    public
    SelectItemModelProperty.Type<DeckSelectProperty, String> type() {
        return TYPE;
    }

    public Codec<String> valueCodec() {
        return VALUE_CODEC;
    }
}

