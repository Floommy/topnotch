package net.togyk.tog_yks_cards.client.data.models.blockstates;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.blockstates.PropertyValueList;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelDispatcher;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import net.togyk.tog_yks_cards.client.data.models.blockstates.PropertyDispatch8;

@Environment(value=EnvType.CLIENT)
public class MultiVariantGenerator
implements BlockModelDefinitionGenerator {
    private final Block block;
    private final List<Entry> entries;
    private final Set<Property<?>> seenProperties;

    public MultiVariantGenerator(Block block, List<Entry> list, Set<Property<?>> set) {
        this.block = block;
        this.entries = list;
        this.seenProperties = set;
    }

    static Set<Property<?>> validateAndExpandProperties(Set<Property<?>> set, Block block, PropertyDispatch8<?, ?, ?, ?, ?, ?, ?, ?, ?> propertyDispatch) {
        List<Property<?>> list = propertyDispatch.getDefinedProperties();
        list.forEach(property -> {
            if (block.getStateDefinition().getProperty(property.getName()) != property) {
                throw new IllegalStateException("Property " + String.valueOf(property) + " is not defined for block " + String.valueOf(block));
            }
            if (set.contains(property)) {
                throw new IllegalStateException("Values of property " + String.valueOf(property) + " already defined for block " + String.valueOf(block));
            }
        });
        HashSet set2 = new HashSet(set);
        set2.addAll(list);
        return set2;
    }

    public MultiVariantGenerator with(PropertyDispatch8<?, ?, ?, ?, ?, ?, ?, ?, ?> propertyDispatch) {
        Set<Property<?>> set = MultiVariantGenerator.validateAndExpandProperties(this.seenProperties, this.block, propertyDispatch);
        List<Entry> list = this.entries.stream().flatMap(entry -> entry.apply(propertyDispatch)).toList();
        return new MultiVariantGenerator(this.block, list, set);
    }

    public MultiVariantGenerator with(VariantMutator variantMutator) {
        List<Entry> list = this.entries.stream().flatMap(entry -> entry.apply(variantMutator)).toList();
        return new MultiVariantGenerator(this.block, list, this.seenProperties);
    }

    public BlockStateModelDispatcher create() {
        HashMap<String, BlockStateModel.Unbaked> map = new HashMap<String, BlockStateModel.Unbaked>();
        for (Entry entry : this.entries) {
            map.put(entry.properties.getKey(), entry.variant.toUnbaked());
        }
        return new BlockStateModelDispatcher(Optional.of(new BlockStateModelDispatcher.SimpleModelSelectors(map)), Optional.empty());
    }

    public Block block() {
        return this.block;
    }

    public static Empty dispatch(Block block) {
        return new Empty(block);
    }

    public static MultiVariantGenerator dispatch(Block block, MultiVariant multiVariant) {
        return new MultiVariantGenerator(block, List.of(new Entry(PropertyValueList.EMPTY, multiVariant)), Set.of());
    }

    @Environment(value=EnvType.CLIENT)
    record Entry(PropertyValueList properties, MultiVariant variant) {
        public Stream<Entry> apply(PropertyDispatch8<?, ?, ?, ?, ?, ?, ?, ?, ?> propertyDispatch) {
            return propertyDispatch.getEntries().entrySet().stream().map(entry -> {
                PropertyValueList propertyValueList = this.properties.extend((PropertyValueList)entry.getKey());
                MultiVariant multiVariant = this.variant.with((VariantMutator)entry.getValue());
                return new Entry(propertyValueList, multiVariant);
            });
        }

        public Stream<Entry> apply(VariantMutator variantMutator) {
            return Stream.of(new Entry(this.properties, this.variant.with(variantMutator)));
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class Empty {
        private final Block block;

        public Empty(Block block) {
            this.block = block;
        }

        public MultiVariantGenerator with(PropertyDispatch8<?, ?, ?, ?, ?, ?, ?, ?, ?> propertyDispatch) {
            Set<Property<?>> set = MultiVariantGenerator.validateAndExpandProperties(Set.of(), this.block, propertyDispatch);
            List<Entry> list = propertyDispatch.getEntries().entrySet().stream().map(entry -> new Entry((PropertyValueList)entry.getKey(), (MultiVariant)entry.getValue())).toList();
            return new MultiVariantGenerator(this.block, list, set);
        }
    }
}

