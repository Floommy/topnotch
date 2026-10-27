package net.togyk.tog_yks_cards.client.data.models.blockstates;

import com.mojang.datafixers.util.Function8;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.data.models.blockstates.PropertyValueList;
import net.minecraft.world.level.block.state.properties.Property;

@Environment(value=EnvType.CLIENT)
public class PropertyDispatch8<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>, T6 extends Comparable<T6>, T7 extends Comparable<T7>, T8 extends Comparable<T8>> {
    private final Property<T1> property1;
    private final Property<T2> property2;
    private final Property<T3> property3;
    private final Property<T4> property4;
    private final Property<T5> property5;
    private final Property<T6> property6;
    private final Property<T7> property7;
    private final Property<T8> property8;
    private final Map<PropertyValueList, V> values = new HashMap<PropertyValueList, V>();

    public PropertyDispatch8(Property<T1> property, Property<T2> property2, Property<T3> property3, Property<T4> property4, Property<T5> property5, Property<T6> property6, Property<T7> property7, Property<T8> property8) {
        this.property1 = property;
        this.property2 = property2;
        this.property3 = property3;
        this.property4 = property4;
        this.property5 = property5;
        this.property6 = property6;
        this.property7 = property7;
        this.property8 = property8;
    }

    protected void putValue(PropertyValueList propertyValueList, V object) {
        V object2 = this.values.put(propertyValueList, object);
        if (object2 != null) {
            throw new IllegalStateException("Value " + String.valueOf(propertyValueList) + " is already defined");
        }
    }

    public Map<PropertyValueList, V> getEntries() {
        this.verifyComplete();
        return Map.copyOf(this.values);
    }

    private void verifyComplete() {
        List<Property<?>> list = this.getDefinedProperties();
        Stream<PropertyValueList> stream = Stream.of(PropertyValueList.EMPTY);
        for (Property<?> property : list) {
            stream = stream.flatMap(propertyValueList -> property.getAllValues().map(arg_0 -> propertyValueList.extend(arg_0)));
        }
        List<PropertyValueList> list2 = stream.filter(propertyValueList -> !this.values.containsKey(propertyValueList)).toList();
        if (!list2.isEmpty()) {
            throw new IllegalStateException("Missing definition for properties: " + String.valueOf(list2));
        }
    }

    public List<Property<?>> getDefinedProperties() {
        return List.of(this.property1, this.property2, this.property3, this.property4, this.property5, this.property6, this.property7, this.property8);
    }

    public PropertyDispatch8<V, T1, T2, T3, T4, T5, T6, T7, T8> select(T1 comparable, T2 comparable2, T3 comparable3, T4 comparable4, T5 comparable5, T6 comparable6, T7 comparable7, T8 comparable8, V object) {
        PropertyValueList propertyValueList = PropertyValueList.of((Property.Value[])new Property.Value[]{this.property1.value(comparable), this.property2.value(comparable2), this.property3.value(comparable3), this.property4.value(comparable4), this.property5.value(comparable5), this.property6.value(comparable6), this.property7.value(comparable7), this.property8.value(comparable8)});
        this.putValue(propertyValueList, object);
        return this;
    }

    public PropertyDispatch8<V, T1, T2, T3, T4, T5, T6, T7, T8> generate(Function8<T1, T2, T3, T4, T5, T6, T7, T8, V> function8) {
        this.property1.getPossibleValues().forEach(comparable -> this.property2.getPossibleValues().forEach(comparable2 -> this.property3.getPossibleValues().forEach(comparable3 -> this.property4.getPossibleValues().forEach(comparable4 -> this.property5.getPossibleValues().forEach(comparable5 -> this.property6.getPossibleValues().forEach(comparable6 -> this.property7.getPossibleValues().forEach(comparable7 -> this.property8.getPossibleValues().forEach(comparable8 -> this.select(comparable, comparable2, comparable3, comparable4, comparable5, comparable6, comparable7, comparable8, function8.apply(comparable, comparable2, comparable3, comparable4, comparable5, comparable6, comparable7, comparable8))))))))));
        return this;
    }
}

