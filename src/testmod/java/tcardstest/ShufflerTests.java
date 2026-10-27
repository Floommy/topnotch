package tcardstest;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.togyk.tcards.shuffler.ShufflerBlock;
import net.togyk.tcards.shuffler.ShufflerBlockEntity;
import net.togyk.tcards.shuffler.ShufflerLogic;
import net.togyk.tcards.shuffler.ShufflerPayloads;
import net.togyk.tcards.shuffler.ShufflerRegistry;
import net.togyk.tog_yks_cards.block.ModBlocks;
import net.togyk.tog_yks_cards.block.custom.CardpileBlock;
import net.togyk.tog_yks_cards.item.ModItems;
import net.togyk.tog_yks_cards.item.custom.CardboxItem;
import net.togyk.tog_yks_cards.item.custom.CardpileItem;

public class ShufflerTests {
    static final BlockPos PERSIST_PILE = new BlockPos(5, -60, 12);
    static final BlockPos PERSIST_SHUFFLER = new BlockPos(7, -60, 12);

    static void check(String n, boolean ok, Object info) {
        TestMain.check(n, ok, info);
    }

    static List<ItemStack> decks(CardboxItem box, int n) {
        List<ItemStack> all = new ArrayList<>();
        for (int i = 0; i < n; i++) all.addAll(box.getCards());
        return all;
    }

    static int pileCards(FakePlayer p) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.getItem() instanceof CardpileItem ci) n += ci.getCards(s).size();
        }
        return n;
    }

    static int piles(FakePlayer p) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).getItem() instanceof CardpileItem) n++;
        return n;
    }

    static void tick(ServerLevel level, BlockPos pos, ShufflerBlockEntity be) {
        for (int i = 0; i < 20; i++) ShufflerBlockEntity.serverTick(level, pos, level.getBlockState(pos), be);
    }

    static ShufflerBlockEntity place(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, ShufflerRegistry.SHUFFLER.defaultBlockState(), 3);
        return (ShufflerBlockEntity) level.getBlockEntity(pos);
    }

    static void run(ServerLevel level) {
        FakePlayer p = FakePlayer.get(level);
        BlockPos pos = new BlockPos(2, -60, 3);
        p.setPos(2.5, -60, 5.0);

        // ---- persistence (second boot checks what the first boot left behind)
        if (level.getBlockState(PERSIST_PILE).getBlock() instanceof CardpileBlock) {
            int n = ModBlocks.RED_CARDPILE.getCards(level, PERSIST_PILE).size();
            check("PERSIST: 312-card pile survived restart", n == 312, n);
            if (level.getBlockEntity(PERSIST_SHUFFLER) instanceof ShufflerBlockEntity be) {
                check("PERSIST: shuffler kept its cards", be.total() == 162 && be.getMode() == ShufflerBlockEntity.TABLE, be.total() + " mode " + be.getMode());
            } else {
                check("PERSIST: shuffler kept its cards", false, "no shuffler");
            }
            return;
        }

        // ---- sorter mode
        ShufflerBlockEntity be = place(level, pos);
        be.insert(decks(ModItems.RED_CARDBOX, 2));
        be.insert(decks(ModItems.PREMIUM_CARDBOX, 1));
        check("shuffler holds 162 cards", be.total() == 162, be.total());

        p.getInventory().clearContent();
        be.handleAction(p, ShufflerPayloads.PRESET, 2, ""); // Blackjack 1 deck
        int got = pileCards(p);
        check("sorter: blackjack 1 deck -> 52 cards in inventory", got == 52, got);
        check("sorter: 110 cards left", be.total() == 110, be.total());

        boolean bj6 = ShufflerLogic.canCompose(be.getPool(), ShufflerLogic.PRESETS.get(5), "");
        check("blackjack 6 not available with 110 cards", !bj6, bj6);
        boolean skat = ShufflerLogic.canCompose(be.getPool(), ShufflerLogic.PRESETS.get(7), "");
        check("skat available", skat, skat);

        p.getInventory().clearContent();
        be.handleAction(p, ShufflerPayloads.PRESET, 7, ""); // Skat
        check("skat -> 32 cards", pileCards(p) == 32, pileCards(p));

        // one-colour filter
        p.getInventory().clearContent();
        be.handleAction(p, ShufflerPayloads.SET_COLOR, 0, "tog_yks-cards:premium_playing_card");
        be.handleAction(p, ShufflerPayloads.PRESET, 1, ""); // 54 with jokers, premium only
        ItemStack first = p.getInventory().getItem(0);
        boolean allPremium = first.getItem() instanceof CardpileItem ci && ci.getCards(first).size() == 54
                && ci.getCards(first).stream().allMatch(c -> c.getItem() == ModItems.PREMIUM_PLAYING_CARD);
        check("colour filter: 54 premium cards only", allPremium, first);
        be.handleAction(p, ShufflerPayloads.SET_COLOR, 0, "");

        // sort by colour
        be.insert(decks(ModItems.PREMIUM_CARDBOX, 1));
        p.getInventory().clearContent();
        int before = be.total();
        be.handleAction(p, ShufflerPayloads.SORT_BY_COLOR, 0, "");
        check("sort by colour -> 2 piles, all cards", piles(p) == 2 && pileCards(p) == before && be.total() == 0, piles(p) + " piles, " + pileCards(p) + "/" + before);

        // custom: 7..A, 1 deck, no jokers = 32
        be.insert(decks(ModItems.RED_CARDBOX, 1));
        be.handleAction(p, ShufflerPayloads.CUSTOM_RANKS, 0b1111111100000, "");
        be.handleAction(p, ShufflerPayloads.CUSTOM_DECKS, 1, "");
        p.getInventory().clearContent();
        be.handleAction(p, ShufflerPayloads.PRESET, ShufflerLogic.CUSTOM, "");
        check("custom 7-A -> 32", pileCards(p) == 32, pileCards(p));

        // uno
        be.insert(decks(ModItems.UNO_CARDBOX, 1));
        p.getInventory().clearContent();
        be.handleAction(p, ShufflerPayloads.PRESET, 12, "");
        check("uno 1 deck -> 108", pileCards(p) == 108, pileCards(p));

        // eject all
        p.getInventory().clearContent();
        int left = be.total();
        be.handleAction(p, ShufflerPayloads.EJECT_ALL, 0, "");
        check("eject all", be.total() == 0 && pileCards(p) == left, left + " -> " + pileCards(p));

        // sorted output in sorter mode
        be.insert(decks(ModItems.RED_CARDBOX, 1));
        be.handleAction(p, ShufflerPayloads.SORTED, 1, "");
        p.getInventory().clearContent();
        be.handleAction(p, ShufflerPayloads.PRESET, 0, "");
        ItemStack sortedPile = p.getInventory().getItem(0);
        List<ItemStack> sc = ((CardpileItem) sortedPile.getItem()).getCards(sortedPile);
        List<String> keys = new ArrayList<>();
        for (ItemStack c : sc) keys.add(ShufflerLogic.key(c));
        List<String> expect = new ArrayList<>();
        for (ItemStack c : ShufflerLogic.sorted(sc)) expect.add(ShufflerLogic.key(c));
        check("sorter: sorted deck comes out in order", sc.size() == 52 && keys.equals(expect) && keys.get(0).contains("rank=2"), keys.get(0));
        be.handleAction(p, ShufflerPayloads.SORTED, 0, "");
        be.handleAction(p, ShufflerPayloads.EJECT_ALL, 0, "");

        // ---- table mode (pile): next deck instantly if enough cards
        be.handleAction(p, ShufflerPayloads.SET_MODE, ShufflerBlockEntity.TABLE, "");
        be.handleAction(p, ShufflerPayloads.PRESET, 0, ""); // poker 52
        be.insert(decks(ModItems.RED_CARDBOX, 2));
        check("pile: poker deck ready", be.getReady().size() == 52, be.getReady().size());
        p.getInventory().clearContent();
        be.takePile(p);
        check("pile: took 52 + next deck ready instantly", pileCards(p) == 52 && be.getReady().size() == 52, pileCards(p) + " / next " + be.getReady().size());
        tick(level, pos, be);
        check("pile: chute shows pile, light green", level.getBlockState(pos).getValue(ShufflerBlock.CHUTE) == ShufflerBlock.Chute.PILE
                && level.getBlockState(pos).getValue(ShufflerBlock.STATUS) == ShufflerBlock.Status.READY, level.getBlockState(pos));
        p.getInventory().clearContent();
        be.takePile(p);
        tick(level, pos, be);
        check("pile: no more deck -> no pile + blinking red", !be.hasReady() && level.getBlockState(pos).getValue(ShufflerBlock.CHUTE) == ShufflerBlock.Chute.NONE
                && level.getBlockState(pos).getValue(ShufflerBlock.STATUS) == ShufflerBlock.Status.EMPTY, level.getBlockState(pos));
        be.handleAction(p, ShufflerPayloads.EJECT_ALL, 0, "");

        // ---- continuous table mode: one card per click
        be.handleAction(p, ShufflerPayloads.SET_MODE, ShufflerBlockEntity.DEALER, "");
        be.handleAction(p, ShufflerPayloads.PRESET, 0, "");
        be.insert(decks(ModItems.RED_CARDBOX, 1));
        check("dealer: shoe of 52 ready", be.getReady().size() == 52, be.getReady().size());
        p.getInventory().clearContent();
        List<ItemStack> dealt = new ArrayList<>();
        List<ItemStack> orderBefore = new ArrayList<>(be.getReady());
        boolean sawDealing = false, sawLow = false;
        for (int i = 0; i < 52; i++) {
            be.takeCard(p);
            ShufflerBlockEntity.serverTick(level, pos, level.getBlockState(pos), be);
            if (level.getBlockState(pos).getValue(ShufflerBlock.CHUTE) == ShufflerBlock.Chute.DEALING) sawDealing = true;
            if (be.getReady().size() == 9 && be.status() == ShufflerBlock.Status.LOW) sawLow = true;
            dealt.add(p.getMainHandItem().copy());
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); // "put it on the table"
        }
        check("dealer: card slides out of the chute when dealt", sawDealing, sawDealing);
        check("dealer: under 10 cards -> flickering green (LOW)", sawLow, sawLow);
        boolean oneEach = dealt.stream().allMatch(c -> c.getItem() == ModItems.RED_PLAYING_CARD && c.getCount() == 1);
        boolean noReshuffle = true;
        for (int i = 0; i < 52; i++) if (!ItemStack.isSameItemSameComponents(dealt.get(i), orderBefore.get(51 - i))) noReshuffle = false;
        check("dealer: 52 clicks -> 52 single cards, no reshuffle while taking", oneEach && noReshuffle, dealt.size());
        tick(level, pos, be);
        check("dealer: shoe used up -> solid red, chute empty", level.getBlockState(pos).getValue(ShufflerBlock.STATUS) == ShufflerBlock.Status.OUT
                && level.getBlockState(pos).getValue(ShufflerBlock.CHUTE) == ShufflerBlock.Chute.NONE, level.getBlockState(pos));
        be.takeCard(p);
        check("dealer: no card when empty", p.getMainHandItem().isEmpty(), p.getMainHandItem());
        // put back 3 cards -> not enough for a full deck -> blinking red
        be.insert(new ArrayList<>(dealt.subList(0, 3)));
        tick(level, pos, be);
        check("dealer: cards back but not enough -> blinking red", !be.hasReady() && level.getBlockState(pos).getValue(ShufflerBlock.STATUS) == ShufflerBlock.Status.EMPTY,
                level.getBlockState(pos));
        // put back the rest -> fresh full shoe, green, cards in chute
        be.insert(new ArrayList<>(dealt.subList(3, 52)));
        tick(level, pos, be);
        check("dealer: all back -> new full shoe, green, cards in chute", be.getReady().size() == 52
                && level.getBlockState(pos).getValue(ShufflerBlock.STATUS) == ShufflerBlock.Status.READY
                && level.getBlockState(pos).getValue(ShufflerBlock.CHUTE) == ShufflerBlock.Chute.CARDS, be.getReady().size());
        // deal 5, then put back an unrelated uno card while the 5 are still on the table
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            be.takeCard(p);
            out.add(p.getMainHandItem().copy());
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        List<ItemStack> beforeUno = new ArrayList<>(be.getReady());
        be.insert(new ArrayList<>(List.of(ModItems.UNO_CARDBOX.getCards().get(0))));
        tick(level, pos, be);
        check("dealer: card in while 5 are out -> blinking red, old shoe kept (47)",
                be.getReady().size() == 47 && be.getReady().equals(beforeUno)
                && level.getBlockState(pos).getValue(ShufflerBlock.STATUS) == ShufflerBlock.Status.EMPTY, be.getReady().size());
        be.takeCard(p);
        check("dealer: can keep dealing from the old shoe", !p.getMainHandItem().isEmpty(), p.getMainHandItem());
        out.add(p.getMainHandItem().copy());
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        // the 6 cards come back -> fresh full shoe, green
        be.insert(out);
        tick(level, pos, be);
        check("dealer: cards back -> fresh full shoe (52), green", be.getReady().size() == 52
                && level.getBlockState(pos).getValue(ShufflerBlock.STATUS) == ShufflerBlock.Status.READY, be.getReady().size());
        be.handleAction(p, ShufflerPayloads.EJECT_ALL, 0, "");

        // blackjack 6 decks (continuous)
        be.handleAction(p, ShufflerPayloads.PRESET, 5, "");
        be.insert(decks(ModItems.RED_CARDBOX, 6));
        check("dealer: blackjack 6-deck shoe ready (312)", be.getReady().size() == 312
                && be.getReady().stream().noneMatch(ShufflerLogic::isJoker), be.getReady().size());

        // breaking: the block entity hands all cards out for dropping (item entities are not visible in this headless test world)
        int inside = be.total();
        be.preRemoveSideEffects(pos, level.getBlockState(pos));
        check("breaking empties the machine into drops", inside == 324 && be.total() == 0, inside + " -> " + be.total());
        level.removeBlock(pos, false);

        // ---- leave things behind for the restart check
        level.setBlock(PERSIST_PILE.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(PERSIST_PILE, ModBlocks.RED_CARDPILE.defaultBlockState().setValue(CardpileBlock.WATERLOGGED, false), 3);
        ModBlocks.RED_CARDPILE.setCards(level, PERSIST_PILE, new ArrayList<>(decks(ModItems.RED_CARDBOX, 6)).subList(0, 312));
        ShufflerBlockEntity keep = place(level, PERSIST_SHUFFLER);
        keep.insert(decks(ModItems.RED_CARDBOX, 3));
        keep.handleAction(p, ShufflerPayloads.SET_MODE, ShufflerBlockEntity.TABLE, "");
        System.out.println("TCARDS-TEST INFO persistence fixtures placed");
    }
}
