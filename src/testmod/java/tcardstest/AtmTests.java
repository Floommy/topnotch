package tcardstest;

import java.util.UUID;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.togyk.tcards.atm.AtmBlockEntity;
import net.togyk.tcards.atm.AtmPayloads;
import net.togyk.tcards.atm.CasinoBanks;
import net.togyk.tcards.atm.CasinoMark;
import net.togyk.tcards.atm.CentralBankBlockEntity;
import net.togyk.tcards.atm.AtmRegistry;
import net.togyk.tog_yks_cards.item.ModItems;

/** ATM: buying, price checks, space checks, coins switch, owner rights, chest linking, vault, persistence. */
public class AtmTests {
    static final int Y = -60;
    static final BlockPos ATM = new BlockPos(40, Y, 10), CHEST = new BlockPos(43, Y, 10);
    static final BlockPos KEEP = new BlockPos(40, Y, 16), KEEP_CHEST = new BlockPos(41, Y, 16);
    static final String RED = "tog_yks-cards:red_chip", COPPER = "tog_yks-cards:copper_coin";

    static final BlockPos BANK = new BlockPos(46, Y, 12), BOB_BANK = new BlockPos(46, Y, 14);
    static CasinoBanks banks;
    static UUID bankId;
    static CentralBankBlockEntity bankBlock;

    static void setPrice(String id, int diamonds, int amount) {
        banks.setPrice(bankId, id, diamonds, amount);
    }

    static void check(String n, boolean ok, Object info) {
        TestMain.check("atm: " + n, ok, info);
    }

    static int count(FakePlayer p, net.minecraft.world.item.Item item) {
        int n = 0;
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) if (s.is(item)) n += s.getCount();
        return n;
    }

    static int countIn(Container c, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) if (c.getItem(i).is(item)) n += c.getItem(i).getCount();
        return n;
    }

    static InteractionResult click(FakePlayer p, ServerLevel level, BlockPos pos) {
        return UseBlockCallback.EVENT.invoker().interact(p, level, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    static AtmBlockEntity place(ServerLevel level, BlockPos pos, FakePlayer owner) {
        var lower = AtmRegistry.ATM.defaultBlockState();
        level.setBlock(pos, lower, 3);
        level.setBlock(pos.above(), lower.setValue(net.togyk.tcards.atm.AtmBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), 3);
        AtmBlockEntity be = (AtmBlockEntity) level.getBlockEntity(pos);
        be.setOwner(owner);
        return be;
    }

    static long dropped(ServerLevel level, BlockPos pos, net.minecraft.world.item.Item item) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3)).stream()
                .filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
    }

    static void run(ServerLevel level) {
        MinecraftServer server = level.getServer();
        level.setChunkForced(2, 0, true);
        level.setChunkForced(2, 1, true);
        for (int x = 36; x < 48; x++) for (int z = 6; z < 20; z++) {
            level.setBlock(new BlockPos(x, Y - 1, z), Blocks.STONE.defaultBlockState(), 3);
            for (int y = Y; y < Y + 3; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        }

        // recipes: chips and coins can no longer be crafted, ATM and bank can
        var recipes = server.getRecipeManager().getRecipes();
        long chipRecipes = recipes.stream().map(r -> r.id().identifier().toString())
                .filter(id -> id.startsWith("tog_yks-cards:") && (id.endsWith("_chip") || id.endsWith("_coin"))).count();
        boolean atmRecipe = recipes.stream().anyMatch(r -> r.id().identifier().toString().equals("tog_yks-cards:atm"));
        boolean bankRecipe = recipes.stream().anyMatch(r -> r.id().identifier().toString().equals("tog_yks-cards:central_bank"));
        check("no chip/coin crafting recipes left", chipRecipes == 0, chipRecipes);
        check("ATM and Central Bank recipes exist", atmRecipe && bankRecipe, atmRecipe + " " + bankRecipe);

        FakePlayer owner = FakePlayer.get(level, new GameProfile(UUID.fromString("00000000-0000-0000-0000-00000000a701"), "Owner"));
        FakePlayer bob = FakePlayer.get(level, new GameProfile(UUID.fromString("00000000-0000-0000-0000-00000000b0b0"), "Bob"));

        // --- casino: every Central Bank is its own casino with its own prices
        level.setBlock(BANK, AtmRegistry.CENTRAL_BANK.defaultBlockState(), 3);
        bankBlock = (CentralBankBlockEntity) level.getBlockEntity(BANK);
        bankBlock.setUp(owner);
        banks = CasinoBanks.get(server);
        bankId = bankBlock.bank();
        CasinoBanks.Bank bank = banks.get(bankId);
        check("bank created with owner and name", bank != null && bank.name().equals("Owner's Casino") && bank.canManage(owner) && !bank.canManage(bob),
                bank == null ? null : bank.name());
        check("default price is 1 chip for 1 diamond", bank.price(RED).amount() == 1 && bank.price(RED).diamonds() == 1
                && bank.price(COPPER).amount() == 1, bank.price(RED));
        setPrice(RED, 1, 10);
        setPrice(COPPER, 1, 64);
        level.setBlock(BOB_BANK, AtmRegistry.CENTRAL_BANK.defaultBlockState(), 3);
        CentralBankBlockEntity bobBank = (CentralBankBlockEntity) level.getBlockEntity(BOB_BANK);
        bobBank.setUp(bob);
        check("two banks, two casinos", bobBank.bank() != null && !bobBank.bank().equals(bankId), bobBank.bank());
        owner.setPos(40.5, Y, 12.5);
        bob.setPos(40.5, Y, 12.5);
        owner.getInventory().clearContent();
        bob.getInventory().clearContent();

        // real placement with the item: two blocks tall, placer becomes owner
        BlockPos placeAt = new BlockPos(38, Y, 18);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AtmRegistry.ATM));
        owner.setPos(38.5, Y, 15.5);
        var ctx = new net.minecraft.world.item.context.UseOnContext(owner, InteractionHand.MAIN_HAND,
                new BlockHitResult(new Vec3(38.5, Y, 18.5), Direction.UP, placeAt.below(), false));
        AtmRegistry.ATM.asItem().useOn(ctx);
        var lowerState = level.getBlockState(placeAt);
        var upperState = level.getBlockState(placeAt.above());
        check("placing builds 2 blocks (lower + upper)", lowerState.is(AtmRegistry.ATM) && upperState.is(AtmRegistry.ATM)
                && upperState.getValue(net.togyk.tcards.atm.AtmBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER,
                lowerState + " / " + upperState);
        AtmBlockEntity placed = net.togyk.tcards.atm.AtmBlock.entity(level, placeAt.above());
        check("upper half finds the ATM data, placer is owner", placed != null && owner.getUUID().equals(placed.getOwner())
                && level.getBlockEntity(placeAt.above()) == null, placed);
        // breaking the TOP with a pickaxe: both halves go, the ATM drops once
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
        placed.vault.amount = 5;
        placed.vault.variant = ItemVariant.of(Items.DIAMOND);
        level.getEntitiesOfClass(ItemEntity.class, new AABB(placeAt).inflate(3)).forEach(e -> e.discard());
        owner.gameMode.destroyBlock(placeAt.above());
        check("breaking the top removes both halves", level.getBlockState(placeAt).isAir() && level.getBlockState(placeAt.above()).isAir(),
                level.getBlockState(placeAt));
        check("ATM item dropped once + waiting diamonds", dropped(level, placeAt, AtmRegistry.ATM.asItem()) == 1 && dropped(level, placeAt, Items.DIAMOND) == 5,
                dropped(level, placeAt, AtmRegistry.ATM.asItem()) + " / " + dropped(level, placeAt, Items.DIAMOND));
        level.getEntitiesOfClass(ItemEntity.class, new AABB(placeAt).inflate(3)).forEach(e -> e.discard());
        // no room above -> cannot be placed
        level.setBlock(placeAt.above(), Blocks.STONE.defaultBlockState(), 3);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AtmRegistry.ATM));
        AtmRegistry.ATM.asItem().useOn(ctx);
        check("no room above -> not placed", level.getBlockState(placeAt).isAir() && owner.getMainHandItem().getCount() == 1, level.getBlockState(placeAt));
        level.setBlock(placeAt.above(), Blocks.AIR.defaultBlockState(), 3);
        owner.getInventory().clearContent();
        owner.setPos(40.5, Y, 12.5);

        AtmBlockEntity atm = place(level, ATM, owner);
        bob.getInventory().add(new ItemStack(Items.DIAMOND, 5));
        AtmBlockEntity.Result r0 = atm.buy(bob, RED, 10, 1, 10);
        check("without a bank the ATM is out of service", !r0.ok() && r0.message().contains("Out of service") && count(bob, Items.DIAMOND) == 5, r0);
        bob.getInventory().clearContent();
        String err0 = atm.linkBank(owner, bobBank);
        check("cannot link an ATM to someone else's bank", err0 != null && atm.getBank() == null, err0);
        String err1 = atm.linkBank(owner, bankBlock);
        check("owner links his ATM to his bank", err1 == null && bankId.equals(atm.getBank()), err1);
        check("owner set", owner.getUUID().equals(atm.getOwner()) && atm.getOwnerName().equals("Owner"), atm.getOwnerName());
        check("owner can manage, Bob not", atm.canManage(owner) && !atm.canManage(bob), "");

        // --- buying
        bob.getInventory().add(new ItemStack(Items.DIAMOND, 5));
        AtmBlockEntity.Result r = atm.buy(bob, RED, 25, 1, 10);
        ItemStack firstChip = bob.getInventory().getNonEquipmentItems().stream().filter(st -> st.is(ModItems.RED_CHIP)).findFirst().orElse(ItemStack.EMPTY);
        check("bought chips carry the casino mark", bankId.equals(CasinoMark.bankOf(firstChip)) && "Owner's Casino".equals(firstChip.get(CasinoMark.TYPE).name()),
                firstChip.get(CasinoMark.TYPE));
        check("buy 25 red chips (10 for 1) costs 3", r.ok() && r.cost() == 3 && count(bob, ModItems.RED_CHIP) == 25 && count(bob, Items.DIAMOND) == 2,
                r + " chips=" + count(bob, ModItems.RED_CHIP) + " dia=" + count(bob, Items.DIAMOND));
        check("diamonds wait in the vault (no link)", atm.vaultAmount() == 3 && atm.getTurnover() == 3, atm.vaultAmount());

        r = atm.buy(bob, RED, 10, 1, 5);
        check("old price on the client is rejected", !r.ok() && r.code() == AtmPayloads.RESULT_PRICE_CHANGED && count(bob, Items.DIAMOND) == 2, r);
        r = atm.buy(bob, RED, 100, 1, 10);
        check("insufficient funds", !r.ok() && r.message().startsWith("Not enough diamonds") && count(bob, ModItems.RED_CHIP) == 25, r);
        r = atm.buy(bob, RED, 0, 1, 10);
        check("amount 0 refused", !r.ok(), r);
        r = atm.buy(bob, RED, AtmBlockEntity.MAX_QTY + 1, 1, 10);
        check("amount above max refused", !r.ok(), r);
        r = atm.buy(bob, "minecraft:diamond", 1, 1, 1);
        check("unknown item refused", !r.ok(), r);

        // inventory space: the slot freed by paying counts
        bob.getInventory().clearContent();
        var slots = bob.getInventory().getNonEquipmentItems();
        for (int i = 0; i < slots.size(); i++) slots.set(i, new ItemStack(Items.DIRT, 64));
        slots.set(5, new ItemStack(Items.DIAMOND, 2));
        r = atm.buy(bob, RED, 20, 1, 10);
        check("chips go into the slot the diamonds came from", r.ok() && count(bob, ModItems.RED_CHIP) == 20 && count(bob, Items.DIAMOND) == 0, r);
        slots.set(5, new ItemStack(Items.DIAMOND, 5));
        r = atm.buy(bob, RED, 10, 1, 10);
        check("full inventory refused, nothing paid", !r.ok() && r.message().contains("space") && count(bob, Items.DIAMOND) == 5, r);
        bob.getInventory().clearContent();

        // coins switch per ATM
        bob.getInventory().add(new ItemStack(Items.DIAMOND, 10));
        r = atm.buy(bob, COPPER, 64, 1, 64);
        check("coins sold when enabled", r.ok() && count(bob, ModItems.COPPER_COIN) == 64, r);
        atm.handleAction(bob, AtmPayloads.TOGGLE_COINS, "", 0, 0, 0);
        check("Bob cannot switch coins off", atm.coinsEnabled(), atm.coinsEnabled());
        atm.handleAction(owner, AtmPayloads.TOGGLE_COINS, "", 0, 0, 0);
        r = atm.buy(bob, COPPER, 64, 1, 64);
        check("owner switched coins off -> not sold", !atm.coinsEnabled() && !r.ok() && r.message().contains("coins"), r);
        r = atm.buy(bob, RED, 10, 1, 10);
        check("chips still sold with coins off", r.ok(), r);
        atm.handleAction(owner, AtmPayloads.TOGGLE_COINS, "", 0, 0, 0);

        // not for sale
        setPrice(RED, 0, 10);
        r = atm.buy(bob, RED, 10, 0, 10);
        check("price 0 = not available", !r.ok() && r.message().contains("Not available"), r);
        setPrice(RED, 1, 10);

        // --- cash out: chips of this casino back to diamonds, paid from the ATM vault
        bob.getInventory().clearContent();
        atm.vault.amount = 6;
        atm.vault.variant = ItemVariant.of(Items.DIAMOND);
        bob.getInventory().add(new ItemStack(Items.DIAMOND, 3));
        atm.buy(bob, RED, 25, 1, 10);                                   // 25 marked chips, vault 9
        ItemStack plain = new ItemStack(ModItems.RED_CHIP, 7);
        bob.getInventory().add(plain);                                  // old chips without a mark
        ItemStack foreign = new ItemStack(ModItems.RED_CHIP, 9);
        foreign.set(CasinoMark.TYPE, new CasinoMark(bobBank.bank(), "Bob's Casino"));
        bob.getInventory().add(foreign);                                // chips of another casino
        check("only chips of this casino count", atm.countMarked(bob, ModItems.RED_CHIP) == 25, atm.countMarked(bob, ModItems.RED_CHIP));
        int vaultBeforeSell = atm.vaultAmount();
        r = atm.sell(bob, RED, 25, 1, 10);
        check("sell 25 at 10 for 1 -> 2 diamonds, 5 chips stay", r.ok() && r.cost() == 2 && r.qty() == 20
                && count(bob, Items.DIAMOND) == 2 && atm.countMarked(bob, ModItems.RED_CHIP) == 5, r + " dia=" + count(bob, Items.DIAMOND));
        check("payout taken from the vault", atm.vaultAmount() == vaultBeforeSell - 2 && atm.getPaidOut() == 2, atm.vaultAmount());
        check("unmarked and foreign chips untouched", count(bob, ModItems.RED_CHIP) == 5 + 7 + 9, count(bob, ModItems.RED_CHIP));
        r = atm.sell(bob, RED, 5, 1, 10);
        check("too few for one diamond", !r.ok() && r.message().startsWith("Too few"), r);
        r = atm.sell(bob, RED, 30, 1, 10);
        check("cannot sell more than you have", !r.ok() && r.message().startsWith("You only have"), r);
        setPrice(RED, 1, 5);
        atm.vault.amount = 0;
        r = atm.sell(bob, RED, 5, 1, 5);
        check("empty vault -> out of cash, chips kept", !r.ok() && r.message().startsWith("Out of cash") && atm.countMarked(bob, ModItems.RED_CHIP) == 5, r);
        setPrice(RED, 1, 10);
        bob.getInventory().clearContent();
        atm.vault.amount = 10;
        atm.vault.variant = ItemVariant.of(Items.DIAMOND);
        bob.getInventory().add(new ItemStack(Items.DIAMOND, 8));

        // --- owner rights
        boolean bobBreak = PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, bob, ATM, level.getBlockState(ATM), atm);
        boolean ownerBreak = PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, owner, ATM, level.getBlockState(ATM), atm);
        check("Bob cannot break the ATM, owner can", !bobBreak && ownerBreak, bobBreak + " " + ownerBreak);
        float progress = level.getBlockState(ATM).getDestroyProgress(bob, level, ATM);
        float progressTop = level.getBlockState(ATM.above()).getDestroyProgress(bob, level, ATM.above());
        check("mining progress for Bob is 0 (both halves)", progress == 0f && progressTop == 0f, progress + " " + progressTop);
        boolean bobBreakTop = PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, bob, ATM.above(), level.getBlockState(ATM.above()), null);
        check("Bob cannot break the top half either", !bobBreakTop, bobBreakTop);
        server.getPlayerList().op(bob.nameAndId());
        boolean opManage = atm.canManage(bob);
        server.getPlayerList().deop(bob.nameAndId());
        check("ops can manage every ATM", opManage && !atm.canManage(bob), opManage);
        int before = atm.vaultAmount();
        atm.handleAction(bob, AtmPayloads.EMPTY_VAULT, "", 0, 0, 0);
        check("Bob cannot empty the vault", atm.vaultAmount() == before, atm.vaultAmount());
        atm.handleAction(bob, AtmPayloads.LINK_START, "", 0, 0, 0);
        check("Bob cannot start linking", !AtmRegistry.isLinking(bob), "");

        // --- hoppers / storage networks may pull diamonds out, but never put anything in
        Storage<ItemVariant> side = ItemStorage.SIDED.find(level, ATM, Direction.DOWN);
        long pulled, pushed;
        try (Transaction tx = Transaction.openOuter()) {
            pulled = side.extract(ItemVariant.of(Items.DIAMOND), 2, tx);
            pushed = side.insert(ItemVariant.of(Items.DIAMOND), 5, tx);
            tx.commit();
        }
        check("transfer API: extract works, insert blocked", side != null && pulled == 2 && pushed == 0 && atm.vaultAmount() == before - 2,
                "pulled=" + pulled + " pushed=" + pushed + " vault=" + atm.vaultAmount());

        Storage<ItemVariant> top = ItemStorage.SIDED.find(level, ATM.above(), Direction.NORTH);
        check("storage cables on the top half reach the vault too", top != null && top.iterator().hasNext(), top);
        check("cannot link the ATM to itself (top half)", atm.link(level, ATM.above(), Direction.UP) != null, "");

        // --- linking
        level.setBlock(CHEST, Blocks.CHEST.defaultBlockState(), 3);
        level.setBlock(new BlockPos(45, Y, 10), Blocks.STONE.defaultBlockState(), 3);
        atm.handleAction(owner, AtmPayloads.LINK_START, "", 0, 0, 0);
        check("owner starts linking", AtmRegistry.isLinking(owner), "");
        InteractionResult res = click(owner, level, new BlockPos(45, Y, 10));
        check("clicking stone does not link", atm.getLink() == null && AtmRegistry.isLinking(owner) && res == InteractionResult.FAIL, res);
        int vaultBefore = atm.vaultAmount();
        res = click(owner, level, CHEST);
        Container chest = (Container) level.getBlockEntity(CHEST);
        check("clicking the chest links it", CHEST.equals(atm.getLink()) && !AtmRegistry.isLinking(owner) && res == InteractionResult.SUCCESS, res + " " + atm.getLink());
        check("vault moved into the chest", atm.vaultAmount() == 0 && countIn(chest, Items.DIAMOND) == vaultBefore, atm.vaultAmount() + " chest=" + countIn(chest, Items.DIAMOND));
        check("link status ok", atm.linkStatus() == AtmPayloads.LINK_OK, atm.linkStatus());
        int inChest = countIn(chest, Items.DIAMOND);
        r = atm.buy(bob, RED, 30, 1, 10);
        check("payment goes straight into the chest", r.ok() && countIn(chest, Items.DIAMOND) == inChest + 3 && atm.vaultAmount() == 0, countIn(chest, Items.DIAMOND));

        // chest full -> vault
        for (int i = 0; i < chest.getContainerSize(); i++) if (chest.getItem(i).isEmpty()) chest.setItem(i, new ItemStack(Items.DIRT, 64));
        for (int i = 0; i < chest.getContainerSize(); i++) if (chest.getItem(i).is(Items.DIAMOND)) chest.setItem(i, new ItemStack(Items.DIAMOND, 64));
        check("link status full", atm.linkStatus() == AtmPayloads.LINK_FULL, atm.linkStatus());
        r = atm.buy(bob, RED, 10, 1, 10);
        check("chest full -> diamonds wait in the vault", r.ok() && atm.vaultAmount() == 1, atm.vaultAmount());

        // vault full -> out of service, nothing paid
        atm.vault.amount = AtmBlockEntity.VAULT_CAPACITY;
        int dia = count(bob, Items.DIAMOND);
        r = atm.buy(bob, RED, 10, 1, 10);
        check("vault and chest full -> out of service", !r.ok() && r.message().contains("Out of service") && count(bob, Items.DIAMOND) == dia, r);

        // owner empties the vault into his inventory
        atm.vault.amount = 70;
        atm.handleAction(owner, AtmPayloads.EMPTY_VAULT, "", 0, 0, 0);
        check("owner empties the vault", atm.vaultAmount() == 0 && count(owner, Items.DIAMOND) == 70, count(owner, Items.DIAMOND));

        // chest emptied again -> the next flush goes there
        chest.clearContent();
        atm.vault.amount = 4;
        atm.vault.variant = ItemVariant.of(Items.DIAMOND);
        atm.flush();
        check("flush moves waiting diamonds once the chest has room", atm.vaultAmount() == 0 && countIn(chest, Items.DIAMOND) == 4, countIn(chest, Items.DIAMOND));

        // chest removed -> missing, payments go to the vault
        level.setBlock(CHEST, Blocks.AIR.defaultBlockState(), 3);
        check("link status missing after the chest is gone", atm.linkStatus() == AtmPayloads.LINK_MISSING, atm.linkStatus());
        r = atm.buy(bob, RED, 10, 1, 10);
        check("missing chest -> vault", r.ok() && atm.vaultAmount() == 1, atm.vaultAmount());

        // too far / unlink / cancel
        level.setBlock(new BlockPos(40, Y, 90), Blocks.CHEST.defaultBlockState(), 3);
        String err = atm.link(level, new BlockPos(40, Y, 90), Direction.UP);
        check("storage too far away is refused", err != null && err.contains("far"), err);
        level.setBlock(new BlockPos(40, Y, 90), Blocks.AIR.defaultBlockState(), 3);
        atm.handleAction(owner, AtmPayloads.UNLINK, "", 0, 0, 0);
        check("unlink", atm.getLink() == null && atm.linkStatus() == AtmPayloads.LINK_NONE, atm.getLink());
        atm.handleAction(owner, AtmPayloads.LINK_START, "", 0, 0, 0);
        boolean cancelled = AtmRegistry.cancelLinking(owner);
        check("clicking the ATM cancels linking", cancelled && !AtmRegistry.isLinking(owner), cancelled);

        // breaking drops the vault diamonds
        atm.vault.amount = 130;
        level.getEntitiesOfClass(ItemEntity.class, new AABB(ATM).inflate(3)).forEach(e -> e.discard());
        level.destroyBlock(ATM, false, owner);
        long dropped = dropped(level, ATM, Items.DIAMOND);
        check("breaking drops the waiting diamonds, top half goes too", level.getBlockState(ATM).isAir() && level.getBlockState(ATM.above()).isAir() && dropped == 130, dropped);
        level.getEntitiesOfClass(ItemEntity.class, new AABB(ATM).inflate(3)).forEach(e -> e.discard());

        // Central Bank: protected, keeps its casino when broken, can be renamed
        boolean bobBreaksBank = PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, bob, BANK, level.getBlockState(BANK), bankBlock);
        check("Bob cannot break the owner's bank", !bobBreaksBank, bobBreaksBank);
        var drops = net.minecraft.world.level.block.Block.getDrops(level.getBlockState(BANK), level, BANK, bankBlock, owner, new ItemStack(Items.IRON_PICKAXE));
        CasinoMark dropMark = drops.isEmpty() ? null : drops.get(0).get(CasinoMark.TYPE);
        check("broken bank item keeps its casino", dropMark != null && bankId.equals(dropMark.bank()), drops);
        banks.rename(bankId, "Lucky Seven");
        check("rename casino", "Lucky Seven".equals(banks.get(bankId).name()), banks.get(bankId).name());

        // --- persistence: an ATM that the second boot checks
        level.setBlock(KEEP_CHEST, Blocks.BARREL.defaultBlockState(), 3);
        AtmBlockEntity keep = place(level, KEEP, owner);
        keep.linkBank(owner, bankBlock);
        keep.handleAction(owner, AtmPayloads.TOGGLE_COINS, "", 0, 0, 0);
        keep.link(level, KEEP_CHEST, Direction.UP);
        keep.vault.amount = 7;
        keep.vault.variant = ItemVariant.of(Items.DIAMOND);
        setPrice("tog_yks-cards:white_chip", 2, 5);
        keep.setChanged();
    }

    /** Second boot on the same world. */
    static void checkPersisted(ServerLevel level) {
        level.getChunk(KEEP);
        if (!(level.getBlockEntity(KEEP) instanceof AtmBlockEntity keep)) {
            check("persist: ATM still there", false, level.getBlockState(KEEP));
            return;
        }
        CasinoBanks.Bank kb = CasinoBanks.get(level.getServer()).get(keep.getBank());
        check("persist: bank link, name and price kept", kb != null && "Lucky Seven".equals(kb.name())
                && kb.price("tog_yks-cards:white_chip").diamonds() == 2 && kb.price("tog_yks-cards:white_chip").amount() == 5,
                kb == null ? null : kb.name() + " " + kb.price("tog_yks-cards:white_chip"));
        check("persist: top half still there", level.getBlockState(KEEP.above()).is(net.togyk.tcards.atm.AtmRegistry.ATM), level.getBlockState(KEEP.above()));
        check("persist: owner, coins, link kept", "Owner".equals(keep.getOwnerName()) && !keep.coinsEnabled() && KEEP_CHEST.equals(keep.getLink()),
                keep.getOwnerName() + " " + keep.coinsEnabled() + " " + keep.getLink());
        check("persist: waiting diamonds kept (or flushed)", keep.vaultAmount() + countIn((Container) level.getBlockEntity(KEEP_CHEST), Items.DIAMOND) == 7,
                keep.vaultAmount());
    }
}
