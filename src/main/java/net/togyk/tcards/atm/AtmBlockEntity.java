package net.togyk.tcards.atm;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.togyk.tcards.TableSounds;

/**
 * An ATM: sells chips (and optionally coins) of its casino (linked Central Bank) for diamonds and buys them back.
 * Chips are "printed" - there is no chip stock. The diamonds go straight into the linked storage;
 * whatever does not fit waits in the ATM's own vault (hoppers / storage cables can pull them out too).
 */
public class AtmBlockEntity extends BlockEntity {
    public static final int VAULT_CAPACITY = 27 * 64;
    public static final int MAX_QTY = 36 * 64;
    public static final int LINK_RANGE = 64;
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("tog_yks-cards");

    private UUID owner;
    private String ownerName = "";
    private boolean coins = true;
    private BlockPos link;
    private Direction linkSide = Direction.UP;
    private long turnover;
    private long paidOut;
    private UUID bank;
    private int flushTimer;

    /** Diamonds waiting in the ATM. Others may only take out (hopper, Tom's storage...), never put in. */
    public final SingleVariantStorage<ItemVariant> vault = new SingleVariantStorage<>() {
        @Override
        protected ItemVariant getBlankVariant() {
            return ItemVariant.blank();
        }

        @Override
        protected long getCapacity(ItemVariant variant) {
            return VAULT_CAPACITY;
        }

        @Override
        protected boolean canInsert(ItemVariant variant) {
            return false;
        }

        @Override
        protected void onFinalCommit() {
            setChanged();
        }
    };

    public AtmBlockEntity(BlockPos pos, BlockState state) {
        super(AtmRegistry.ATM_BE, pos, state);
    }

    // ------------------------------------------------------------------ owner

    public void setOwner(Player player) {
        owner = player.getUUID();
        ownerName = player.getName().getString();
        setChanged();
        sync();
    }

    public UUID getOwner() {
        return owner;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public static boolean isAdmin(Player player) {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    /** Owner and ops may change settings, take the diamonds, link storage and break the ATM. */
    public boolean canManage(Player player) {
        return owner == null || owner.equals(player.getUUID()) || isAdmin(player);
    }

    public boolean coinsEnabled() {
        return coins;
    }

    public BlockPos getLink() {
        return link;
    }

    public long getTurnover() {
        return turnover;
    }

    public int vaultAmount() {
        return (int) vault.amount;
    }

    public UUID getBank() {
        return bank;
    }

    /** The casino this ATM belongs to (null = no bank linked or the bank is gone -> out of service). */
    public CasinoBanks.Bank bankData() {
        if (bank == null || level == null || level.getServer() == null) return null;
        return CasinoBanks.get(level.getServer()).get(bank);
    }

    /** Linking needs the right to manage the bank too, so nobody can print chips of someone else's casino. */
    public String linkBank(ServerPlayer player, CentralBankBlockEntity bankBlock) {
        CasinoBanks.Bank b = bankBlock.data();
        if (b == null) return "That bank is not set up";
        if (!b.canManage(player)) return "Only the owner of " + b.name() + " can link ATMs to it";
        bank = bankBlock.bank();
        setChanged();
        return null;
    }

    public long getPaidOut() {
        return paidOut;
    }

    // ------------------------------------------------------------------ the transaction

    public record Result(boolean ok, int code, String message, int qty, int cost) {
        static Result fail(String message) {
            return new Result(false, AtmPayloads.RESULT_ERROR, message, 0, 0);
        }
    }

    public static int countDiamonds(Player player) {
        int n = 0;
        for (ItemStack s : player.getInventory().getNonEquipmentItems()) if (s.is(Items.DIAMOND)) n += s.getCount();
        return n;
    }

    /** Slot order used to pay: main inventory first, hotbar last (keeps diamonds in hand as long as possible). */
    private static int[] payOrder(int size) {
        int[] order = new int[size];
        int k = 0;
        for (int i = 9; i < size; i++) order[k++] = i;
        for (int i = 0; i < Math.min(9, size); i++) order[k++] = i;
        return order;
    }

    private static void takeDiamonds(List<ItemStack> slots, long cost) {
        long left = cost;
        for (int i : payOrder(slots.size())) {
            if (left <= 0) break;
            ItemStack s = slots.get(i);
            if (!s.is(Items.DIAMOND)) continue;
            int take = (int) Math.min(left, s.getCount());
            s.shrink(take);
            left -= take;
        }
    }

    /** How many of the item fit into the 36 slots. */
    private static long room(List<ItemStack> slots, ItemStack sample) {
        long room = 0;
        int max = sample.getMaxStackSize();
        for (ItemStack s : slots) {
            if (s.isEmpty()) room += max;
            else if (ItemStack.isSameItemSameComponents(s, sample)) room += Math.max(0, Math.min(max, s.getMaxStackSize()) - s.getCount());
        }
        return room;
    }

    public Result buy(ServerPlayer player, String itemId, int qty, int expectDiamonds, int expectAmount) {
        AtmCatalog.Entry entry = AtmCatalog.byId(itemId);
        if (entry == null) return Result.fail("Unknown item");
        if (entry.coin() && !coins) return Result.fail("This ATM does not sell coins");
        CasinoBanks.Bank b = bankData();
        if (b == null) return Result.fail("Out of service: no bank linked");
        AtmPrices.Price price = b.price(itemId);
        if (!price.onSale()) return Result.fail("Not available at the moment");
        if (price.diamonds() != expectDiamonds || price.amount() != expectAmount) {
            return new Result(false, AtmPayloads.RESULT_PRICE_CHANGED, "The price was just changed. Please check it again.", 0, 0);
        }
        if (qty < 1) return Result.fail("Please enter an amount");
        if (qty > MAX_QTY) return Result.fail("Maximum is " + MAX_QTY + " per transaction");

        long cost = price.cost(qty);
        int held = countDiamonds(player);
        if (held < cost) return Result.fail("Not enough diamonds (need " + cost + ", you have " + held + ")");

        // would everything fit into the inventory once the diamonds are paid?
        List<ItemStack> real = player.getInventory().getNonEquipmentItems();
        List<ItemStack> sim = new ArrayList<>(real.size());
        for (ItemStack s : real) sim.add(s.copy());
        takeDiamonds(sim, cost);
        ItemStack sample = new ItemStack(entry.item());
        sample.set(CasinoMark.TYPE, new CasinoMark(bank, b.name()));
        if (room(sim, sample) < qty) return Result.fail("No space in your inventory");

        // is there room for the diamonds (linked storage + own vault)?
        long toLink = pushToLink(cost, true);
        if (toLink + (VAULT_CAPACITY - vault.amount) < cost) {
            notifyOwnerFull(player);
            return Result.fail("Out of service: the vault is full");
        }

        // --- commit
        takeDiamonds(real, cost);
        long linked = pushToLink(cost, false);
        addToVault(cost - linked);
        int left = qty;
        while (left > 0) {
            int n = Math.min(left, sample.getMaxStackSize());
            ItemStack stack = sample.copyWithCount(n);
            if (!player.getInventory().add(stack) && !stack.isEmpty()) player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
            left -= n;
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        turnover += cost;
        setChanged();
        if (level instanceof ServerLevel server) {
            server.playSound(null, worldPosition, TableSounds.ATM_DISPENSE, SoundSource.BLOCKS, 0.9f, 1.0f);
        }
        LOG.info("ATM at {}: {} bought {}x {} for {} diamonds", worldPosition.toShortString(), player.getName().getString(), qty, itemId, cost);
        return new Result(true, AtmPayloads.RESULT_DISPENSED, "", qty, (int) cost);
    }

    /** Chips / coins of this casino in the player's 36 slots. */
    public int countMarked(Player player, net.minecraft.world.item.Item item) {
        if (bank == null) return 0;
        int n = 0;
        for (ItemStack s : player.getInventory().getNonEquipmentItems()) {
            if (s.is(item) && bank.equals(CasinoMark.bankOf(s))) n += s.getCount();
        }
        return n;
    }

    /** Diamonds the ATM can pay out right now: own vault + linked storage. */
    public long cashAvailable() {
        long n = vault.amount;
        Storage<ItemVariant> s = linkedStorage();
        if (s != null && !Transaction.isOpen()) {
            try (Transaction tx = Transaction.openOuter()) {
                n += s.extract(ItemVariant.of(Items.DIAMOND), Integer.MAX_VALUE, tx);
            }
        }
        return n;
    }

    /** Takes n diamonds out of the vault first, then out of the linked storage. Returns how many it got. */
    private long takeCash(long n) {
        long fromVault = Math.min(n, vault.amount);
        vault.amount -= fromVault;
        if (vault.amount == 0) vault.variant = ItemVariant.blank();
        long got = fromVault;
        if (got < n) {
            Storage<ItemVariant> s = linkedStorage();
            if (s != null && !Transaction.isOpen()) {
                try (Transaction tx = Transaction.openOuter()) {
                    got += s.extract(ItemVariant.of(Items.DIAMOND), n - got, tx);
                    tx.commit();
                }
            }
        }
        setChanged();
        return got;
    }

    /**
     * Sells chips / coins of this casino back. Pays whole diamonds at the bank price (rounded down);
     * chips that do not add up to a full diamond stay with the player.
     */
    public Result sell(ServerPlayer player, String itemId, int qty, int expectDiamonds, int expectAmount) {
        AtmCatalog.Entry entry = AtmCatalog.byId(itemId);
        if (entry == null) return Result.fail("Unknown item");
        CasinoBanks.Bank b = bankData();
        if (b == null) return Result.fail("Out of service: no bank linked");
        AtmPrices.Price price = b.price(itemId);
        if (!price.onSale()) return Result.fail("This casino does not take these right now");
        if (price.diamonds() != expectDiamonds || price.amount() != expectAmount) {
            return new Result(false, AtmPayloads.RESULT_PRICE_CHANGED, "The price was just changed. Please check it again.", 0, 0);
        }
        int held = countMarked(player, entry.item());
        if (qty < 1) return Result.fail("Please enter an amount");
        if (qty > held) return Result.fail("You only have " + held + " from " + b.name());
        long payout = (long) qty * price.diamonds() / price.amount();
        if (payout < 1) return Result.fail("Too few for 1 diamond (need " + (price.amount() + price.diamonds() - 1) / price.diamonds() + ")");
        long used = (payout * price.amount() + price.diamonds() - 1) / price.diamonds();   // chips that make up the payout
        if (cashAvailable() < payout) {
            notifyOwnerEmpty(player);
            return Result.fail("Out of cash: this ATM cannot pay " + payout + " diamonds right now");
        }
        // room for the diamonds after the chips are gone
        List<ItemStack> real = player.getInventory().getNonEquipmentItems();
        List<ItemStack> sim = new ArrayList<>(real.size());
        for (ItemStack s : real) sim.add(s.copy());
        takeMarked(sim, entry.item(), used);
        if (room(sim, new ItemStack(Items.DIAMOND)) < payout) return Result.fail("No space in your inventory");

        long got = takeCash(payout);
        if (got < payout) {   // someone emptied the chest in between: give back what we took
            addToVault(got);
            return Result.fail("Out of cash: this ATM cannot pay " + payout + " diamonds right now");
        }
        takeMarked(real, entry.item(), used);
        long left = payout;
        while (left > 0) {
            ItemStack stack = new ItemStack(Items.DIAMOND, (int) Math.min(64, left));
            left -= stack.getCount();
            if (!player.getInventory().add(stack) && !stack.isEmpty()) player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        paidOut += payout;
        setChanged();
        if (level instanceof ServerLevel server) {
            server.playSound(null, worldPosition, TableSounds.ATM_DISPENSE, SoundSource.BLOCKS, 0.9f, 1.15f);
        }
        LOG.info("ATM at {}: {} sold {}x {} for {} diamonds", worldPosition.toShortString(), player.getName().getString(), used, itemId, payout);
        return new Result(true, AtmPayloads.RESULT_SOLD, "", (int) used, (int) payout);
    }

    private void takeMarked(List<ItemStack> slots, net.minecraft.world.item.Item item, long n) {
        long left = n;
        for (int i : payOrder(slots.size())) {
            if (left <= 0) break;
            ItemStack s = slots.get(i);
            if (!s.is(item) || !bank.equals(CasinoMark.bankOf(s))) continue;
            int take = (int) Math.min(left, s.getCount());
            s.shrink(take);
            left -= take;
        }
    }

    private void notifyOwnerEmpty(ServerPlayer customer) {
        if (owner == null || owner.equals(customer.getUUID()) || level == null || level.getServer() == null) return;
        ServerPlayer o = level.getServer().getPlayerList().getPlayer(owner);
        if (o != null) o.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§eYour ATM at " + worldPosition.toShortString() + " ran out of diamonds - " + customer.getName().getString() + " could not cash out."));
    }

    private void addToVault(long n) {
        if (n <= 0) return;
        if (vault.amount == 0) vault.variant = ItemVariant.of(Items.DIAMOND);
        vault.amount += n;
        setChanged();
    }

    private void notifyOwnerFull(ServerPlayer customer) {
        if (owner == null || owner.equals(customer.getUUID()) || level == null || level.getServer() == null) return;
        ServerPlayer o = level.getServer().getPlayerList().getPlayer(owner);
        if (o != null) o.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§eYour ATM at " + worldPosition.toShortString() + " is full - " + customer.getName().getString() + " could not buy anything. Please empty it."));
    }

    // ------------------------------------------------------------------ linked storage

    private Storage<ItemVariant> linkedStorage() {
        if (link == null || level == null || !level.isLoaded(link)) return null;
        return ItemStorage.SIDED.find(level, link, linkSide);
    }

    public int linkStatus() {
        if (link == null) return AtmPayloads.LINK_NONE;
        if (level == null || !level.isLoaded(link)) return AtmPayloads.LINK_UNLOADED;
        Storage<ItemVariant> s = linkedStorage();
        if (s == null || !s.supportsInsertion()) return AtmPayloads.LINK_MISSING;
        try (Transaction tx = Transaction.openOuter()) {
            if (s.insert(ItemVariant.of(Items.DIAMOND), 1, tx) < 1) return AtmPayloads.LINK_FULL;
        }
        return AtmPayloads.LINK_OK;
    }

    /** Puts up to n diamonds into the linked storage; returns how many fit. simulate = only check. */
    private long pushToLink(long n, boolean simulate) {
        if (n <= 0 || Transaction.isOpen()) return 0;
        Storage<ItemVariant> s = linkedStorage();
        if (s == null) return 0;
        try (Transaction tx = Transaction.openOuter()) {
            long done = s.insert(ItemVariant.of(Items.DIAMOND), n, tx);
            if (!simulate) tx.commit();
            return done;
        }
    }

    /** Moves waiting diamonds from the vault into the linked storage. */
    public void flush() {
        if (vault.amount <= 0 || Transaction.isOpen()) return;
        long moved = pushToLink(vault.amount, false);
        if (moved > 0) {
            vault.amount -= moved;
            if (vault.amount == 0) vault.variant = ItemVariant.blank();
            setChanged();
        }
    }

    public String link(ServerLevel l, BlockPos pos, Direction side) {
        if (l != level) return "The storage must be in the same dimension";
        if (pos.equals(worldPosition) || pos.equals(worldPosition.above())) return "That is the ATM itself";
        if (l.getBlockState(pos).getBlock() instanceof AtmBlock) return "Cannot link an ATM to another ATM";
        if (!pos.closerThan(worldPosition, LINK_RANGE)) return "Too far away (max " + LINK_RANGE + " blocks)";
        Storage<ItemVariant> s = ItemStorage.SIDED.find(l, pos, side);
        if (s == null || !s.supportsInsertion()) return "This block has no storage. Click a chest, barrel or similar";
        link = pos.immutable();
        linkSide = side;
        setChanged();
        flush();
        showLink(l);
        return null;
    }

    public void unlink() {
        link = null;
        setChanged();
    }

    public void showLink(ServerLevel l) {
        if (link == null) return;
        Vec3 a = Vec3.atCenterOf(worldPosition), b = Vec3.atCenterOf(link);
        int steps = (int) Math.min(160, Math.max(4, a.distanceTo(b) * 3));
        DustParticleOptions dust = new DustParticleOptions(0x44FF66, 1.0f);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = a.lerp(b, i / (double) steps);
            l.sendParticles(dust, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        l.sendParticles(ParticleTypes.HAPPY_VILLAGER, b.x, b.y + 0.3, b.z, 12, 0.35, 0.35, 0.35, 0);
    }

    public String linkText() {
        if (link == null) return "";
        String name = level != null && level.isLoaded(link) ? level.getBlockState(link).getBlock().getName().getString() : "Storage";
        return name + " " + link.getX() + " " + link.getY() + " " + link.getZ();
    }

    // ------------------------------------------------------------------ menu

    public void handleAction(ServerPlayer player, int action, String item, int a, int b, int c) {
        int result = AtmPayloads.RESULT_NONE, qty = 0, cost = 0;
        String message = "", resultItem = "";
        boolean manage = canManage(player);
        switch (action) {
            case AtmPayloads.BUY -> {
                Result r = buy(player, item, a, b, c);
                result = r.code();
                message = r.message();
                resultItem = item;
                qty = r.qty();
                cost = r.cost();
            }
            case AtmPayloads.SELL -> {
                Result r = sell(player, item, a, b, c);
                result = r.code();
                message = r.message();
                resultItem = item;
                qty = r.qty();
                cost = r.cost();
            }
            case AtmPayloads.LINK_BANK -> {
                if (!manage) break;
                AtmRegistry.startLinking(player, this, true);
                return;
            }
            case AtmPayloads.EMPTY_VAULT -> {
                if (!manage) break;
                long taken = 0;
                while (vault.amount > 0) {
                    int n = (int) Math.min(64, vault.amount);
                    ItemStack stack = new ItemStack(Items.DIAMOND, n);
                    player.getInventory().add(stack);
                    int moved = n - stack.getCount();
                    vault.amount -= moved;
                    taken += moved;
                    if (moved < n) break;
                }
                if (vault.amount == 0) vault.variant = ItemVariant.blank();
                setChanged();
                result = AtmPayloads.RESULT_INFO;
                message = taken == 0 ? (vault.amount == 0 ? "The vault is empty" : "No space in your inventory")
                        : taken + " diamonds taken" + (vault.amount > 0 ? " (" + vault.amount + " left - inventory full)" : "");
            }
            case AtmPayloads.LINK_START -> {
                if (!manage) break;
                AtmRegistry.startLinking(player, this, false);
                return;   // the screen closes on the client
            }
            case AtmPayloads.UNLINK -> {
                if (!manage) break;
                unlink();
                result = AtmPayloads.RESULT_INFO;
                message = "Storage unlinked";
            }
            case AtmPayloads.TOGGLE_COINS -> {
                if (!manage) break;
                coins = !coins;
                setChanged();
            }
            case AtmPayloads.SHOW_LINK -> {
                if (manage && level instanceof ServerLevel l) showLink(l);
            }
            default -> {}
        }
        sendState(player, false, result, resultItem, qty, cost, message);
    }

    public void sendState(ServerPlayer player, boolean open, int result, String resultItem, int qty, int cost, String message) {
        if (!ServerPlayNetworking.canSend(player, AtmPayloads.AtmState.TYPE)) return;
        CasinoBanks.Bank b = bankData();
        List<AtmPayloads.PriceEntry> list = new ArrayList<>();
        List<Integer> held = new ArrayList<>();
        for (AtmCatalog.Entry e : AtmCatalog.all()) {
            AtmPrices.Price p = b == null ? new AtmPrices.Price(0, 0) : b.price(e.id());
            list.add(new AtmPayloads.PriceEntry(e.id(), p.diamonds(), p.amount()));
            held.add(countMarked(player, e.item()));
        }
        boolean manage = canManage(player);
        ServerPlayNetworking.send(player, new AtmPayloads.AtmState(worldPosition, open, ownerName, manage, coins,
                countDiamonds(player), manage ? vaultAmount() : 0, VAULT_CAPACITY,
                manage ? linkStatus() : 0, manage ? linkText() : "", manage ? turnover : 0,
                list, result, resultItem, qty, cost, message, b == null ? "" : b.name(), held, manage ? paidOut : 0));
    }

    // ------------------------------------------------------------------ tick, drops, saving

    public static void serverTick(Level level, BlockPos pos, BlockState state, AtmBlockEntity be) {
        if (++be.flushTimer >= 20) {
            be.flushTimer = 0;
            be.flush();
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null || level.isClientSide()) return;
        while (vault.amount > 0) {
            int n = (int) Math.min(64, vault.amount);
            net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(Items.DIAMOND, n));
            vault.amount -= n;
        }
    }

    private void sync() {
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (owner != null) tag.store("owner", UUIDUtil.CODEC, owner);
        tag.putString("owner_name", ownerName);
        return tag;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (owner != null) output.store("owner", UUIDUtil.CODEC, owner);
        output.putString("owner_name", ownerName);
        output.putBoolean("coins", coins);
        output.putLong("diamonds", vault.amount);
        output.putLong("turnover", turnover);
        output.putLong("paid_out", paidOut);
        if (bank != null) output.store("bank", UUIDUtil.CODEC, bank);
        if (link != null) {
            output.store("link", BlockPos.CODEC, link);
            output.store("link_side", Direction.CODEC, linkSide);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        ownerName = input.getStringOr("owner_name", "");
        coins = input.getBooleanOr("coins", true);
        vault.amount = Math.max(0, Math.min(VAULT_CAPACITY, input.getLongOr("diamonds", 0)));
        vault.variant = vault.amount > 0 ? ItemVariant.of(Items.DIAMOND) : ItemVariant.blank();
        turnover = input.getLongOr("turnover", 0);
        paidOut = input.getLongOr("paid_out", 0);
        bank = input.read("bank", UUIDUtil.CODEC).orElse(null);
        link = input.read("link", BlockPos.CODEC).orElse(null);
        linkSide = input.read("link_side", Direction.CODEC).orElse(Direction.UP);
    }
}
