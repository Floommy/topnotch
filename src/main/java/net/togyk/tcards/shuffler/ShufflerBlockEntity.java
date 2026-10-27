package net.togyk.tcards.shuffler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.togyk.tcards.shuffler.ShufflerLogic.Preset;
import net.togyk.tog_yks_cards.item.custom.CardItem;
import net.togyk.tog_yks_cards.item.custom.CardboxItem;
import net.togyk.tog_yks_cards.item.custom.CardpileItem;

/**
 * Modes:
 * SORTER  - menu presets put a deck (shuffled or sorted) into the inventory.
 * TABLE   - "pile" table mode: a click hands out the whole ready deck; the next one is built right away if possible.
 * DEALER  - continuous table mode: a click hands out ONE card of the ready shoe. Taking never reshuffles.
 *           Putting any card back reshuffles everything into a fresh full shoe.
 */
public class ShufflerBlockEntity extends BlockEntity {
    public static final int SORTER = 0, TABLE = 1, DEALER = 2;
    private static final int SHUFFLE_TICKS = 18;

    private List<ItemStack> pool = new ArrayList<>();
    private List<ItemStack> ready = new ArrayList<>();
    private int mode = SORTER;
    private int preset = 0;
    private String color = "";
    private int customRanks = ShufflerLogic.ALL_RANKS;
    private int customDecks = 1;
    private boolean customJokers = false;
    private boolean sortedOutput = false;
    /** Last attempt to build a shoe failed (not enough cards) -> blinking red. */
    private boolean composeFailed = false;
    private int shuffleTicks = 0;
    /** Card visibly slides out of the chute for a moment after dealing. */
    private int dealTicks = 0;
    private static final int DEAL_TICKS = 7;
    public static final int LOW_SHOE = 10;

    public ShufflerBlockEntity(BlockPos pos, BlockState state) {
        super(ShufflerRegistry.SHUFFLER_BE, pos, state);
    }

    public int getMode() {
        return mode;
    }

    public boolean hasReady() {
        return !ready.isEmpty();
    }

    public int total() {
        return pool.size() + ready.size();
    }

    private Preset currentPreset() {
        return ShufflerLogic.preset(preset, customRanks, customDecks, customJokers);
    }

    private boolean tableLike() {
        return mode == TABLE || mode == DEALER;
    }

    // ------------------------------------------------------------------ inserting

    public static boolean isInsertable(ItemStack stack) {
        return stack.getItem() instanceof CardItem || stack.getItem() instanceof CardpileItem || stack.getItem() instanceof CardboxItem;
    }

    public void insertFrom(Player player, InteractionHand hand, ItemStack stack) {
        List<ItemStack> cards = new ArrayList<>();
        if (stack.getItem() instanceof CardItem) {
            for (int i = 0; i < stack.getCount(); i++) cards.add(stack.copyWithCount(1));
            player.setItemInHand(hand, ItemStack.EMPTY);
        } else if (stack.getItem() instanceof CardpileItem pileItem) {
            for (ItemStack c : pileItem.getCards(stack)) cards.add(c.copyWithCount(1));
            stack.shrink(1);
        } else if (stack.getItem() instanceof CardboxItem box) {
            for (ItemStack c : box.getCards(stack)) cards.add(c.copyWithCount(1));
            box.setCards(stack, new ArrayList<>());
            box.setOpen(stack, true);
        }
        if (cards.isEmpty()) return;
        insert(cards);
        if (player instanceof ServerPlayer sp) {
            String info = "+" + cards.size() + " cards  (" + total() + " in the shuffler)";
            if (tableLike()) info += composeFailed ? "  |  §cnot enough cards for " + currentPreset().label() : "  |  §areshuffled";
            sp.sendOverlayMessage(Component.literal(info));
        }
    }

    /** Adds cards. In both table modes every insert rebuilds a fresh shuffled shoe from everything inside. */
    public void insert(List<ItemStack> cards) {
        pool.addAll(cards);
        net.togyk.tcards.TableSounds.play(level, worldPosition, net.togyk.tcards.TableSounds.CARD_PLACE, 0.8f);
        if (tableLike()) reshuffleShoe();
        changed();
    }

    /**
     * Tries to build a new full shoe from everything inside (pool + rest of the current shoe).
     * If that is not possible, the continuous mode keeps dealing from the old shoe (light blinks red),
     * the pile mode has nothing ready.
     */
    private void reshuffleShoe() {
        List<ItemStack> all = new ArrayList<>(pool);
        all.addAll(ready);
        List<ItemStack> deck = ShufflerLogic.compose(all, currentPreset(), color);
        if (deck != null) {
            pool = all;
            ready = deck;
            composeFailed = false;
            startShuffle();
        } else {
            composeFailed = true;
            if (mode != DEALER) {
                pool = all;
                ready = new ArrayList<>();
            }
        }
    }

    // ------------------------------------------------------------------ handing out

    /** Empty-hand click in a table mode. */
    public void dispense(ServerPlayer player) {
        if (mode == TABLE) takePile(player);
        else if (mode == DEALER) takeCard(player);
    }

    /** Pile mode: whole deck; the next one is built right away if enough cards are left. */
    public void takePile(ServerPlayer player) {
        if (ready.isEmpty()) {
            player.sendOverlayMessage(Component.literal("§cNo deck ready - not enough cards for " + currentPreset().label()));
            return;
        }
        ItemStack pile = ShufflerLogic.toPile(ready);
        ready = new ArrayList<>();
        give(player, pile, true);
        net.togyk.tcards.TableSounds.play(level, worldPosition, net.togyk.tcards.TableSounds.CARD_TAKE, 0.8f);
        List<ItemStack> next = ShufflerLogic.compose(pool, currentPreset(), color);
        if (next != null) {
            ready = next;
            composeFailed = false;
            startShuffle();
        } else {
            composeFailed = true;
        }
        changed();
    }

    /** Continuous mode: one card off the shoe, no reshuffle. */
    public void takeCard(ServerPlayer player) {
        if (ready.isEmpty()) {
            player.sendOverlayMessage(Component.literal(composeFailed
                    ? "§cNot enough cards for " + currentPreset().label()
                    : "§cShoe empty - put cards back to reshuffle"));
            return;
        }
        ItemStack card = ready.remove(ready.size() - 1);
        give(player, card, true);
        net.togyk.tcards.TableSounds.play(level, worldPosition, net.togyk.tcards.TableSounds.CARD_SLIDE, 0.8f);
        dealTicks = DEAL_TICKS;
        changed();
    }

    // ------------------------------------------------------------------ menu actions

    public void handleAction(ServerPlayer player, int action, int arg, String text) {
        String message = "";
        switch (action) {
            case ShufflerPayloads.SET_MODE -> {
                mode = (arg == TABLE || arg == DEALER) ? arg : SORTER;
                if (tableLike()) {
                    reshuffleShoe();
                    if (!hasReady()) message = "Not enough cards for " + currentPreset().label();
                } else {
                    pool.addAll(ready);
                    ready = new ArrayList<>();
                    composeFailed = false;
                }
            }
            case ShufflerPayloads.PRESET -> {
                if (arg < 0 || arg >= ShufflerLogic.PRESETS.size()) break;
                preset = arg;
                if (tableLike()) {
                    reshuffleShoe();
                    if (!hasReady()) message = "Not enough cards for " + currentPreset().label();
                } else {
                    List<ItemStack> deck = ShufflerLogic.compose(pool, currentPreset(), color);
                    if (deck == null) {
                        message = "Not enough cards for " + currentPreset().label();
                    } else {
                        if (sortedOutput) deck = ShufflerLogic.sorted(deck);
                        give(player, ShufflerLogic.toPile(deck), false);
                        startShuffle();
                        message = currentPreset().label() + ": " + deck.size() + " cards " + (sortedOutput ? "sorted" : "shuffled");
                    }
                }
            }
            case ShufflerPayloads.SET_COLOR -> {
                color = text == null ? "" : text;
                if (tableLike()) reshuffleShoe();
            }
            case ShufflerPayloads.SORTED -> sortedOutput = arg != 0;
            case ShufflerPayloads.SORT_BY_COLOR -> {
                List<ItemStack> all = takeAll();
                int piles = 0;
                for (Map.Entry<Item, List<ItemStack>> e : ShufflerLogic.sortByColor(all).entrySet()) {
                    give(player, ShufflerLogic.toPile(e.getValue()), false);
                    piles++;
                }
                if (piles > 0) startShuffle();
                message = piles == 0 ? "The shuffler is empty" : piles + " piles sorted by back colour";
            }
            case ShufflerPayloads.EJECT_ALL -> {
                List<ItemStack> all = takeAll();
                if (!all.isEmpty()) give(player, ShufflerLogic.toPile(all), false);
                message = all.isEmpty() ? "The shuffler is empty" : all.size() + " cards ejected";
            }
            case ShufflerPayloads.CUSTOM_RANKS -> customRanks = arg & ShufflerLogic.ALL_RANKS;
            case ShufflerPayloads.CUSTOM_DECKS -> customDecks = Math.max(1, Math.min(8, arg));
            case ShufflerPayloads.CUSTOM_JOKERS -> customJokers = arg != 0;
            case ShufflerPayloads.TAKE -> {
                if (tableLike()) dispense(player);
            }
            case ShufflerPayloads.RESHUFFLE -> {
                if (tableLike()) reshuffleShoe();
                else startShuffle();
            }
            default -> {}
        }
        changed();
        sendState(player, false, message);
    }

    private List<ItemStack> takeAll() {
        List<ItemStack> all = new ArrayList<>(pool);
        all.addAll(ready);
        pool = new ArrayList<>();
        ready = new ArrayList<>();
        if (tableLike()) composeFailed = false;
        return all;
    }

    private void give(ServerPlayer player, ItemStack stack, boolean toHand) {
        if (stack.isEmpty()) return;
        if (toHand && player.getMainHandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            return;
        }
        if (!player.getInventory().add(stack)) player.drop(stack, false, Prediction.SERVER_ONLY);
    }

    public void sendState(ServerPlayer player, boolean open, String message) {
        if (!ServerPlayNetworking.canSend(player, ShufflerPayloads.State.TYPE)) return;
        int available = 0;
        List<ItemStack> source = new ArrayList<>(pool);
        if (tableLike()) source.addAll(ready);
        for (int i = 0; i < ShufflerLogic.PRESETS.size(); i++) {
            Preset p = ShufflerLogic.preset(i, customRanks, customDecks, customJokers);
            if (ShufflerLogic.canCompose(source, p, color)) available |= 1 << i;
        }
        List<ItemStack> all = new ArrayList<>(pool);
        all.addAll(ready);
        Map<String, Integer> colors = ShufflerLogic.colors(all);
        ServerPlayNetworking.send(player, new ShufflerPayloads.State(worldPosition, open, mode, preset, color, total(), ready.size(),
                available, customRanks, customDecks, customJokers, sortedOutput,
                new ArrayList<>(colors.keySet()), new ArrayList<>(colors.values()), message));
    }

    // ------------------------------------------------------------------ look & sound

    private void startShuffle() {
        shuffleTicks = SHUFFLE_TICKS;
        if (level != null) net.togyk.tcards.TableSounds.play(level, worldPosition, net.togyk.tcards.TableSounds.SHUFFLE, 0.9f);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ShufflerBlockEntity be) {
        if (be.shuffleTicks > 0) be.shuffleTicks--;
        if (be.dealTicks > 0) be.dealTicks--;
        BlockState wanted = be.wantedState(state);
        if (wanted != state) level.setBlock(pos, wanted, Block.UPDATE_CLIENTS);
    }

    /** Light: green = shoe ready, solid red = shoe used up, blinking red = not enough cards. Chute shows cards / pile. */
    public ShufflerBlock.Status status() {
        if (!tableLike()) return ShufflerBlock.Status.OFF;
        if (composeFailed) return ShufflerBlock.Status.EMPTY;          // blinking red: shuffle not possible
        if (mode == DEALER && !ready.isEmpty() && ready.size() < LOW_SHOE) return ShufflerBlock.Status.LOW; // flickering green
        if (!ready.isEmpty()) return ShufflerBlock.Status.READY;       // green
        return mode == DEALER ? ShufflerBlock.Status.OUT : ShufflerBlock.Status.EMPTY; // solid red: shoe used up

    }

    public ShufflerBlock.Chute chute() {
        if (mode == DEALER && dealTicks > 0) return ShufflerBlock.Chute.DEALING;
        if (ready.isEmpty()) return ShufflerBlock.Chute.NONE;
        return mode == DEALER ? ShufflerBlock.Chute.CARDS : mode == TABLE ? ShufflerBlock.Chute.PILE : ShufflerBlock.Chute.NONE;
    }

    private BlockState wantedState(BlockState state) {
        return state.setValue(ShufflerBlock.STATUS, status())
                .setValue(ShufflerBlock.CHUTE, chute())
                .setValue(ShufflerBlock.SHUFFLING, shuffleTicks > 0)
                .setValue(ShufflerBlock.FILLED, total() > 0);
    }

    private void changed() {
        setChanged();
    }

    // ------------------------------------------------------------------ drops & saving

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null || level.isClientSide()) return;
        for (List<ItemStack> group : ShufflerLogic.sortByColor(takeAll()).values()) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ShufflerLogic.toPile(group));
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ValueOutput.TypedOutputList<ItemStack> p = output.list("Pool", ItemStack.CODEC);
        for (ItemStack c : pool) if (!c.isEmpty()) p.add(c);
        ValueOutput.TypedOutputList<ItemStack> r = output.list("Ready", ItemStack.CODEC);
        for (ItemStack c : ready) if (!c.isEmpty()) r.add(c);
        output.store("mode", Codec.INT, mode);
        output.store("preset", Codec.INT, preset);
        output.store("color", Codec.STRING, color);
        output.store("custom_ranks", Codec.INT, customRanks);
        output.store("custom_decks", Codec.INT, customDecks);
        output.store("custom_jokers", Codec.BOOL, customJokers);
        output.store("sorted_output", Codec.BOOL, sortedOutput);
        output.store("compose_failed", Codec.BOOL, composeFailed);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        pool = new ArrayList<>();
        for (ItemStack c : input.listOrEmpty("Pool", ItemStack.CODEC)) pool.add(c);
        ready = new ArrayList<>();
        for (ItemStack c : input.listOrEmpty("Ready", ItemStack.CODEC)) ready.add(c);
        mode = input.read("mode", Codec.INT).orElse(SORTER);
        preset = input.read("preset", Codec.INT).orElse(0);
        color = input.read("color", Codec.STRING).orElse("");
        customRanks = input.read("custom_ranks", Codec.INT).orElse(ShufflerLogic.ALL_RANKS);
        customDecks = input.read("custom_decks", Codec.INT).orElse(1);
        customJokers = input.read("custom_jokers", Codec.BOOL).orElse(false);
        sortedOutput = input.read("sorted_output", Codec.BOOL).orElse(false);
        composeFailed = input.read("compose_failed", Codec.BOOL).orElse(false);
    }

    // test helpers
    public List<ItemStack> getPool() {
        return pool;
    }

    public List<ItemStack> getReady() {
        return ready;
    }
}
