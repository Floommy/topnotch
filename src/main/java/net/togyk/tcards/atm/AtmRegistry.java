package net.togyk.tcards.atm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.togyk.tog_yks_cards.block.ModBlocks;

public final class AtmRegistry {
    private AtmRegistry() {}

    public static AtmBlock ATM;
    public static CentralBankBlock CENTRAL_BANK;
    public static BlockEntityType<AtmBlockEntity> ATM_BE;
    public static BlockEntityType<CentralBankBlockEntity> BANK_BE;

    /** Players that pressed "Link storage": the next block they right click becomes the ATM's storage. */
    private record Pending(ServerLevel level, BlockPos atm, long until, boolean bank) {}

    private static final Map<UUID, Pending> LINKING = new HashMap<>();
    public static final int LINK_TIMEOUT = 30 * 20;

    public static void init() {
        CasinoMark.register();
        ATM = ModBlocks.registerBlockDefaultItem("atm", AtmBlock::new,
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 1200.0f)
                        .sound(SoundType.METAL).pushReaction(PushReaction.IMMOVEABLE).requiresCorrectToolForDrops());
        ATM_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath("tog_yks-cards", "atm"),
                FabricBlockEntityTypeBuilder.create(AtmBlockEntity::new, ATM).build());
        CENTRAL_BANK = ModBlocks.registerBlockDefaultItem("central_bank", CentralBankBlock::new,
                BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(5.0f, 1200.0f)
                        .sound(SoundType.METAL).requiresCorrectToolForDrops());
        BANK_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath("tog_yks-cards", "central_bank"),
                FabricBlockEntityTypeBuilder.create(CentralBankBlockEntity::new, CENTRAL_BANK).build());

        // hoppers, pipes and storage networks (e.g. Tom's Simple Storage) may pull the diamonds out
        ItemStorage.SIDED.registerForBlocks((level, pos, state, be, side) -> {
            AtmBlockEntity atm = AtmBlock.entity(level, pos);
            return atm == null ? null : atm.vault;
        }, ATM);

        PayloadTypeRegistry.serverboundPlay().register(AtmPayloads.Action.TYPE, AtmPayloads.Action.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AtmPayloads.AtmState.TYPE, AtmPayloads.AtmState.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AtmPayloads.BankState.TYPE, AtmPayloads.BankState.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AtmPayloads.Action.TYPE, (payload, context) ->
                context.server().execute(() -> handle(context.player(), payload)));

        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, be) -> {
            if (be instanceof CentralBankBlockEntity bank && !bank.canManage(player)) {
                CasinoBanks.Bank b = bank.data();
                player.sendOverlayMessage(Component.literal("§cThis bank belongs to " + (b == null ? "someone else" : b.ownerName())));
                return false;
            }
            AtmBlockEntity atm = state.getBlock() instanceof AtmBlock ? AtmBlock.entity(level, pos) : null;
            if (atm != null && !atm.canManage(player)) {
                player.sendOverlayMessage(Component.literal("§cThis ATM belongs to " + atm.getOwnerName()));
                return false;
            }
            return true;
        });

        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
            Pending pending = LINKING.get(sp.getUUID());
            if (pending == null) return InteractionResult.PASS;
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            AtmBlockEntity clicked = AtmBlock.entity(level, hit.getBlockPos());
            if (clicked != null && clicked.getBlockPos().equals(pending.atm())) return InteractionResult.PASS;   // the ATM cancels (see AtmBlock)
            if (!(pending.level().getBlockEntity(pending.atm()) instanceof AtmBlockEntity atm)) {
                LINKING.remove(sp.getUUID());
                return InteractionResult.PASS;
            }
            if (pending.bank()) {
                if (!(level.getBlockEntity(hit.getBlockPos()) instanceof CentralBankBlockEntity bankBlock)) {
                    sp.sendOverlayMessage(Component.literal("§cThat is not a Central Bank"));
                    return InteractionResult.FAIL;
                }
                String err = atm.linkBank(sp, bankBlock);
                if (err != null) {
                    sp.sendOverlayMessage(Component.literal("§c" + err));
                    return InteractionResult.FAIL;
                }
                LINKING.remove(sp.getUUID());
                level.playSound(null, hit.getBlockPos(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.6f, 1.3f);
                sp.sendOverlayMessage(Component.literal("§aATM now belongs to " + atm.bankData().name()));
                return InteractionResult.SUCCESS;
            }
            String error = atm.link((ServerLevel) level, hit.getBlockPos(), hit.getDirection());
            if (error != null) {
                sp.sendOverlayMessage(Component.literal("§c" + error));
                return InteractionResult.FAIL;
            }
            LINKING.remove(sp.getUUID());
            level.playSound(null, hit.getBlockPos(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.6f, 1.3f);
            sp.sendOverlayMessage(Component.literal("§aATM linked to " + atm.linkText()));
            return InteractionResult.SUCCESS;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (LINKING.isEmpty()) return;
            List<UUID> expired = new ArrayList<>();
            for (Map.Entry<UUID, Pending> e : LINKING.entrySet()) {
                if (e.getValue().level().getGameTime() > e.getValue().until()) expired.add(e.getKey());
            }
            for (UUID id : expired) {
                LINKING.remove(id);
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null) p.sendOverlayMessage(Component.literal("§7Linking cancelled (time is up)"));
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LINKING.remove(handler.getPlayer().getUUID()));

        ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                Identifier.fromNamespaceAndPath("tog_yks-cards", "tog_yks-cards"));
        CreativeModeTabEvents.modifyOutputEvent(tab).register(output -> {
            output.accept(new ItemStack(ATM));
            output.accept(new ItemStack(CENTRAL_BANK));
        });
    }

    private static void handle(ServerPlayer player, AtmPayloads.Action payload) {
        if (player.isSpectator() || !player.isWithinBlockInteractionRange(payload.pos(), 1.0)) return;
        if (payload.action() == AtmPayloads.SET_PRICE || payload.action() == AtmPayloads.SET_NAME) {
            if (!(player.level().getBlockEntity(payload.pos()) instanceof CentralBankBlockEntity bankBlock)) return;
            CasinoBanks.Bank b = bankBlock.data();
            String msg = "";
            if (b == null) {
                msg = "This bank is not set up";
            } else if (!b.canManage(player)) {
                msg = "Only the owner can change this bank";
            } else if (payload.action() == AtmPayloads.SET_PRICE) {
                CasinoBanks.get(player.level().getServer()).setPrice(bankBlock.bank(), payload.item(), payload.a(), payload.b());
            } else {
                CasinoBanks.get(player.level().getServer()).rename(bankBlock.bank(), payload.item());
                bankBlock.data();
            }
            sendBank(player, payload.pos(), false, msg);
            return;
        }
        if (player.level().getBlockEntity(payload.pos()) instanceof AtmBlockEntity be) {
            be.handleAction(player, payload.action(), payload.item(), payload.a(), payload.b(), payload.c());
        }
    }

    public static void sendBank(ServerPlayer player, BlockPos pos, boolean open, String message) {
        if (!ServerPlayNetworking.canSend(player, AtmPayloads.BankState.TYPE)) return;
        if (!(player.level().getBlockEntity(pos) instanceof CentralBankBlockEntity bankBlock)) return;
        CasinoBanks.Bank b = bankBlock.data();
        List<AtmPayloads.PriceEntry> list = new ArrayList<>();
        for (AtmCatalog.Entry e : AtmCatalog.all()) {
            AtmPrices.Price p = b == null ? new AtmPrices.Price(0, 0) : b.price(e.id());
            list.add(new AtmPayloads.PriceEntry(e.id(), p.diamonds(), p.amount()));
        }
        ServerPlayNetworking.send(player, new AtmPayloads.BankState(pos, open, b != null && b.canManage(player), list, message,
                b == null ? "" : b.name()));
    }

    public static void startLinking(ServerPlayer player, AtmBlockEntity atm, boolean bank) {
        ServerLevel level = (ServerLevel) atm.getLevel();
        LINKING.put(player.getUUID(), new Pending(level, atm.getBlockPos(), level.getGameTime() + LINK_TIMEOUT, bank));
        player.sendOverlayMessage(Component.literal(bank
                ? "§eRight-click your Central Bank to link this ATM to it. Click the ATM to cancel."
                : "§eRight-click the chest (or other storage) to link it. Click the ATM to cancel."));
    }

    /** Clicking the ATM itself while linking cancels. Returns true if there was something to cancel. */
    public static boolean cancelLinking(ServerPlayer player) {
        if (LINKING.remove(player.getUUID()) == null) return false;
        player.sendOverlayMessage(Component.literal("§7Linking cancelled"));
        return true;
    }

    public static boolean isLinking(ServerPlayer player) {
        return LINKING.containsKey(player.getUUID());
    }
}
