package net.togyk.tog_yks_cards.tag;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {

    public static class Blocks {
        public static final TagKey<Block> GAME_MAT = Blocks.createTag("game_mat");

        private static TagKey<Block> createTag(String name) {
            return TagKey.create(Registries.BLOCK, (Identifier)Identifier.fromNamespaceAndPath((String)"tog_yks-cards", (String)name));
        }
    }

    public static class Items {
        public static final TagKey<Item> CARDS = Items.createTag("cards");
        public static final TagKey<Item> DECORATIVE_MAT_ITEM = Items.createTag("decorative_mat_item");

        private static TagKey<Item> createTag(String name) {
            return TagKey.create(Registries.ITEM, (Identifier)Identifier.fromNamespaceAndPath((String)"tog_yks-cards", (String)name));
        }
    }
}

