package net.togyk.tcards.shuffler;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.togyk.tog_yks_cards.block.ModBlocks;
import net.togyk.tog_yks_cards.block.custom.MatBlock;
import net.togyk.tog_yks_cards.blockentity.ModBlockEntities;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType;

public final class ShufflerRegistry {
    private ShufflerRegistry() {}

    public static ShufflerBlock SHUFFLER;
    public static BlockEntityType<ShufflerBlockEntity> SHUFFLER_BE;
    public static MatBlock BLACKJACK_MAT;
    public static DealerBellBlock DEALER_BELL;

    public static void init() {
        SHUFFLER = ModBlocks.registerBlockDefaultItem("card_shuffler", ShufflerBlock::new,
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.5f).sound(SoundType.METAL).noOcclusion());
        SHUFFLER_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath("tog_yks-cards", "card_shuffler"),
                FabricBlockEntityTypeBuilder.create(ShufflerBlockEntity::new, SHUFFLER).build());

        BLACKJACK_MAT = ModBlocks.registerBlockDefaultItem("blackjack_table_mat", p -> new MatBlock(p, "gold"),
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
        ((FabricBlockEntityType) ModBlockEntities.MAT).addValidBlock(BLACKJACK_MAT);
        DEALER_BELL = ModBlocks.registerBlockDefaultItem("dealer_bell", DealerBellBlock::new,
                BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(0.5f).sound(SoundType.METAL).noOcclusion());
        DealerBellBlock.init();

        PayloadTypeRegistry.serverboundPlay().register(ShufflerPayloads.Action.TYPE, ShufflerPayloads.Action.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ShufflerPayloads.State.TYPE, ShufflerPayloads.State.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ShufflerPayloads.Action.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    if (player.isSpectator() || !player.isWithinBlockInteractionRange(payload.pos(), 1.0)) return;
                    if (player.level().getBlockEntity(payload.pos()) instanceof ShufflerBlockEntity be) {
                        be.handleAction(player, payload.action(), payload.arg(), payload.text());
                    }
                }));

        ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                Identifier.fromNamespaceAndPath("tog_yks-cards", "tog_yks-cards"));
        CreativeModeTabEvents.modifyOutputEvent(tab).register(output -> {
            output.accept(new ItemStack(SHUFFLER));
            output.accept(new ItemStack(BLACKJACK_MAT));
            output.accept(new ItemStack(DEALER_BELL));
        });
    }
}
