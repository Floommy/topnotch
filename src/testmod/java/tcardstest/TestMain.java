package tcardstest;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.togyk.tcards.TableActions;
import net.togyk.tog_yks_cards.block.ModBlocks;
import net.togyk.tog_yks_cards.block.custom.CardpileBlock;
import net.togyk.tog_yks_cards.blockentity.entities.MatBlockEntity;
import net.togyk.tog_yks_cards.item.ModItems;
import net.togyk.tog_yks_cards.item.custom.CardpileItem;
import net.togyk.tog_yks_cards.mat_item.CardPileMatItem;
import net.togyk.tog_yks_cards.mat_item.MatItem;

public class TestMain implements ModInitializer {
    static int pass = 0, fail = 0;

    static void check(String name, boolean ok, Object info) {
        if (ok) pass++; else fail++;
        System.out.println("TCARDS-TEST " + (ok ? "PASS " : "FAIL ") + name + " -> " + info);
    }

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                ShufflerTests.run(server.overworld());
                run(server.overworld());
                if (server.overworld().getBlockState(AtmTests.KEEP).is(net.togyk.tcards.atm.AtmRegistry.ATM)) AtmTests.checkPersisted(server.overworld());
                AtmTests.run(server.overworld());
                if (server.overworld().getBlockState(RestaurantTests.KEEP_TRAY).is(net.togyk.tcards.restaurant.RestaurantRegistry.TRAY)) RestaurantTests.checkPersisted(server.overworld());
                RestaurantTests.run(server.overworld());
                FoodTests.run(server.overworld());
                NameTests.run();
                FunTests.start(server.overworld());
                if (System.getProperty("tcards.elbowtest") != null) ElbowTests.run();
            } catch (Throwable t) {
                fail++;
                System.out.println("TCARDS-TEST EXCEPTION " + t);
                t.printStackTrace(System.out);
            }
            System.out.println("TCARDS-TEST SUMMARY pass=" + pass + " fail=" + fail);
        });
    }

    static void moveTo(FakePlayer p, BlockPos pos) {
        p.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 2.5);
    }

    static List<ItemStack> deck(int n) {
        List<ItemStack> all = ModItems.RED_CARDBOX.getCards();
        return new ArrayList<>(all.subList(0, n));
    }

    static String order(List<ItemStack> cards) {
        StringBuilder sb = new StringBuilder();
        for (ItemStack c : cards) sb.append(ModItems.RED_PLAYING_CARD.getCardData(c).get("rank")).append(ModItems.RED_PLAYING_CARD.getCardData(c).get("suit"), 0, 1).append(' ');
        return sb.toString();
    }

    static void run(ServerLevel level) {
        int y = -60;
        for (int x = -2; x < 20; x++) for (int z = -2; z < 6; z++) {
            level.setBlock(new BlockPos(x, y - 1, z), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        }
        FakePlayer p = FakePlayer.get(level);

        // 1) fresh box: no jokers, sneaking: with jokers
        ItemStack box = new ItemStack(ModItems.RED_CARDBOX);
        p.setItemInHand(InteractionHand.MAIN_HAND, box);
        p.setShiftKeyDown(false);
        ModItems.RED_CARDBOX.use(level, p, InteractionHand.MAIN_HAND);
        List<ItemStack> boxCards = ModItems.RED_CARDBOX.getCards(box);
        check("normal box opens with 54 cards (jokers back)", boxCards.size() == 54 && boxCards.stream().filter(TableActions::isJoker).count() == 2, boxCards.size());
        check("box is open", ModItems.RED_CARDBOX.isOpen(box), "");
        ItemStack box2 = new ItemStack(ModItems.RED_CARDBOX);
        p.setItemInHand(InteractionHand.MAIN_HAND, box2);
        p.setShiftKeyDown(true);
        ModItems.RED_CARDBOX.use(level, p, InteractionHand.MAIN_HAND);
        check("sneak-open box also 54", ModItems.RED_CARDBOX.getCards(box2).size() == 54, ModItems.RED_CARDBOX.getCards(box2).size());
        p.setShiftKeyDown(false);

        // 2) ground pile: shuffle with table key
        BlockPos pilePos = new BlockPos(0, y, 0);
        moveTo(p, pilePos);
        level.setBlock(pilePos, ModBlocks.RED_CARDPILE.defaultBlockState().setValue(CardpileBlock.WATERLOGGED, false), 3);
        ModBlocks.RED_CARDPILE.setCards(level, pilePos, deck(52));
        String before = order(ModBlocks.RED_CARDPILE.getCards(level, pilePos));
        Vec3 pileHit = new Vec3(0.5, y + 0.003, 0.5);
        TableActions.tableKey(p, pilePos, pileHit);
        List<ItemStack> after = ModBlocks.RED_CARDPILE.getCards(level, pilePos);
        check("ground pile shuffled", after.size() == 52 && !order(after).equals(before), after.size());

        // 3) sneak + right click open box on pile: dump deck onto pile (shoe)
        ItemStack box3 = new ItemStack(ModItems.RED_CARDBOX);
        p.setItemInHand(InteractionHand.MAIN_HAND, box3);
        ModItems.RED_CARDBOX.use(level, p, InteractionHand.MAIN_HAND);
        p.setShiftKeyDown(true);
        ModItems.RED_CARDBOX.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(pileHit, Direction.UP, pilePos, false)));
        p.setShiftKeyDown(false);
        check("box dumped onto pile -> 106 cards", ModBlocks.RED_CARDPILE.getCards(level, pilePos).size() == 106, ModBlocks.RED_CARDPILE.getCards(level, pilePos).size());
        check("box empty after dump", ModItems.RED_CARDBOX.getCards(box3).isEmpty(), ModItems.RED_CARDBOX.getCards(box3).size());

        // 4) left click collect with empty hand
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        TableActions.collect(p, pilePos, pileHit);
        ItemStack hand = p.getMainHandItem();
        check("collect removes ground pile", level.getBlockState(pilePos).isAir(), level.getBlockState(pilePos));
        check("collect puts pile in empty hand", hand.getItem() instanceof CardpileItem ci && ci.getCards(hand).size() == 106, hand);

        // 5) collect into existing pile in hand
        level.setBlock(pilePos, ModBlocks.RED_CARDPILE.defaultBlockState().setValue(CardpileBlock.WATERLOGGED, false), 3);
        ModBlocks.RED_CARDPILE.setCards(level, pilePos, deck(5));
        TableActions.collect(p, pilePos, pileHit);
        hand = p.getMainHandItem();
        check("collect merges into hand pile -> 111", ((CardpileItem) hand.getItem()).getCards(hand).size() == 111, ((CardpileItem) hand.getItem()).getCards(hand).size());

        // 6) collect with a single card in hand -> becomes pile
        ItemStack single = deck(1).get(0);
        p.setItemInHand(InteractionHand.MAIN_HAND, single);
        level.setBlock(pilePos, ModBlocks.RED_CARDPILE.defaultBlockState().setValue(CardpileBlock.WATERLOGGED, false), 3);
        ModBlocks.RED_CARDPILE.setCards(level, pilePos, deck(3));
        TableActions.collect(p, pilePos, pileHit);
        hand = p.getMainHandItem();
        check("card in hand + 3 collected -> pile of 4", hand.getItem() instanceof CardpileItem ci && ci.getCards(hand).size() == 4, hand);

        // 7) coins: take one by one, stacking in inventory
        BlockPos coinPos = new BlockPos(3, y, 0);
        moveTo(p, coinPos);
        p.getInventory().clearContent();
        level.setBlock(coinPos, ModBlocks.COIN_PIlE.defaultBlockState().setValue(net.togyk.tog_yks_cards.block.custom.CoinPileblock.WATERLOGGED, false), 3);
        ModBlocks.COIN_PIlE.setCoins(level, coinPos, List.of(new ItemStack(ModItems.RED_CHIP), new ItemStack(ModItems.RED_CHIP), new ItemStack(ModItems.RED_CHIP)));
        Vec3 coinHit = new Vec3(3.5, y + 0.05, 0.5);
        TableActions.tableKey(p, coinPos, coinHit);
        TableActions.tableKey(p, coinPos, coinHit);
        int chips = p.getInventory().countItem(ModItems.RED_CHIP);
        int left = ModBlocks.COIN_PIlE.getCoins(level, coinPos).size();
        check("2 chips taken, stacked; 1 left", chips == 2 && left == 1, "inv=" + chips + " left=" + left);
        int slotsWithChips = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).is(ModItems.RED_CHIP)) slotsWithChips++;
        check("chips stack in one slot", slotsWithChips == 1, slotsWithChips);
        TableActions.tableKey(p, coinPos, coinHit);
        check("last chip taken removes coin pile", level.getBlockState(coinPos).isAir(), level.getBlockState(coinPos));

        // 8) mats: shuffle, clear table, collect
        BlockPos matA = new BlockPos(10, y, 0), matB = new BlockPos(11, y, 0), matC = new BlockPos(12, y, 0);
        moveTo(p, matB);
        for (BlockPos m : List.of(matA, matB, matC)) level.setBlock(m, ModBlocks.RED_IRON_GAME_MAT.defaultBlockState().setValue(net.togyk.tog_yks_cards.block.custom.MatBlock.WATERLOGGED, false), 3);
        BlockState pileState = ModBlocks.RED_CARDPILE.defaultBlockState();
        CardpileItem pileItem = (CardpileItem) ModBlocks.RED_CARDPILE.asItem();
        CardPileMatItem pA = new CardPileMatItem(pileState, 0, 0, deck(10), pileItem);
        CardPileMatItem pB = new CardPileMatItem(pileState, 0, 0, deck(4), pileItem);
        CardPileMatItem pC = new CardPileMatItem(pileState, 0, 0, deck(2), pileItem);
        ModBlocks.RED_IRON_GAME_MAT.addMatItem(level, matA, pA);
        ModBlocks.RED_IRON_GAME_MAT.addMatItem(level, matB, pB);
        ModBlocks.RED_IRON_GAME_MAT.addMatItem(level, matC, pC);
        Vec3 hitA = new Vec3(10.5, y + 0.065, 0.5), hitB = new Vec3(11.5, y + 0.065, 0.5);
        check("mat item hit detection", TableActions.findMatItem(level, matA, hitA) == pA, TableActions.findMatItem(level, matA, hitA));

        String beforeMat = order(pA.getCards());
        TableActions.tableKey(p, matA, hitA);
        check("mat pile shuffled", pA.getCards().size() == 10 && !order(pA.getCards()).equals(beforeMat), pA.getCards().size());

        p.setShiftKeyDown(true);
        TableActions.tableKey(p, matA, hitA);
        p.setShiftKeyDown(false);
        int itemsB = ((MatBlockEntity) level.getBlockEntity(matB)).getMatItems().size();
        int itemsC = ((MatBlockEntity) level.getBlockEntity(matC)).getMatItems().size();
        check("clear table into existing pile -> 16", pA.getCards().size() == 16 && itemsB == 0 && itemsC == 0, "A=" + pA.getCards().size() + " B items=" + itemsB + " C items=" + itemsC);

        p.setShiftKeyDown(true);
        TableActions.tableKey(p, matB, hitB);
        p.setShiftKeyDown(false);
        List<MatItem> onB = ((MatBlockEntity) level.getBlockEntity(matB)).getMatItems();
        int itemsA = ((MatBlockEntity) level.getBlockEntity(matA)).getMatItems().size();
        check("clear table onto empty spot -> new pile with 16", onB.size() == 1 && onB.get(0) instanceof CardPileMatItem np && np.getCards().size() == 16 && itemsA == 0, "B=" + onB.size() + " A items=" + itemsA);

        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        check("mat pile is collect target", TableActions.isCollectTarget(level, matB, hitB), "");
        TableActions.collect(p, matB, hitB);
        hand = p.getMainHandItem();
        check("collect mat pile into hand", hand.getItem() instanceof CardpileItem ci && ci.getCards(hand).size() == 16
                && ((MatBlockEntity) level.getBlockEntity(matB)).getMatItems().isEmpty(), hand);
        check("empty mat spot is not collect target", !TableActions.isCollectTarget(level, matB, hitB), "");

        // 9) table key on OPEN cards picks them up instead of shuffling
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        moveTo(p, pilePos);
        level.setBlock(pilePos, ModBlocks.RED_CARDPILE.defaultBlockState().setValue(CardpileBlock.WATERLOGGED, false), 3);
        ModBlocks.RED_CARDPILE.setCards(level, pilePos, deck(2));
        ModBlocks.RED_CARDPILE.setFaceDown(level, pilePos, false);
        TableActions.tableKey(p, pilePos, pileHit);
        hand = p.getMainHandItem();
        check("middle click on open ground cards collects", level.getBlockState(pilePos).isAir() && hand.getItem() instanceof CardpileItem ci && ci.getCards(hand).size() == 2, hand);

        moveTo(p, matB);
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        CardPileMatItem open = new CardPileMatItem(pileState.setValue(CardpileBlock.FACE_DOWN, false), 0, 0, deck(1), pileItem);
        ModBlocks.RED_IRON_GAME_MAT.addMatItem(level, matB, open);
        TableActions.tableKey(p, matB, hitB);
        hand = p.getMainHandItem();
        check("middle click on open mat card collects", ((MatBlockEntity) level.getBlockEntity(matB)).getMatItems().isEmpty() && hand.getItem() instanceof CardpileItem ci && ci.getCards(hand).size() == 1, hand);
    }
}
