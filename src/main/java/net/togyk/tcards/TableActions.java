package net.togyk.tcards;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.togyk.tog_yks_cards.block.custom.CardpileBlock;
import net.togyk.tog_yks_cards.block.custom.CoinPileblock;
import net.togyk.tog_yks_cards.block.custom.MatBlock;
import net.togyk.tog_yks_cards.blockentity.entities.CardPileBlockEntity;
import net.togyk.tog_yks_cards.blockentity.entities.MatBlockEntity;
import net.togyk.tog_yks_cards.item.custom.CardItem;
import net.togyk.tog_yks_cards.item.custom.CardboxItem;
import net.togyk.tog_yks_cards.item.custom.CardpileItem;
import net.togyk.tog_yks_cards.mat_item.CardPileMatItem;
import net.togyk.tog_yks_cards.mat_item.CoinPileMatItem;
import net.togyk.tog_yks_cards.mat_item.MatItem;
import net.togyk.tog_yks_cards.mat_item.SimpleMatItem;
import net.togyk.tcards.shuffler.DealerBellBlock;

/** Shared target checks (client + server) and the server-side table actions. */
public final class TableActions {
    private TableActions() {}

    private static final int MAX_MATS = 512;


    // ---------------------------------------------------------------- targets

    public static MatItem findMatItem(Level level, BlockPos pos, Vec3 hit) {
        if (hit == null) return null;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof MatBlock mat)) return null;
        for (MatItem item : mat.getAllMatItems(level, pos)) {
            if (item.isHit(hit)) return item;
        }
        return null;
    }

    /** Left click on this would collect cards. */
    public static boolean isCollectTarget(Level level, BlockPos pos, Vec3 hit) {
        Block block = level.getBlockState(pos).getBlock();
        if (block instanceof CardpileBlock) return true;
        return block instanceof MatBlock && findMatItem(level, pos, hit) instanceof CardPileMatItem;
    }

    /** Table key on this would do something (shuffle, take coin, clear table). */
    public static boolean isTableKeyTarget(Level level, BlockPos pos, Vec3 hit, boolean sneaking) {
        Block block = level.getBlockState(pos).getBlock();
        if (block instanceof CardpileBlock || block instanceof CoinPileblock || block instanceof DealerBellBlock) return true;
        if (block instanceof MatBlock) {
            if (sneaking) return true;
            MatItem item = findMatItem(level, pos, hit);
            return item instanceof CardPileMatItem || item instanceof CoinPileMatItem || isBell(item);
        }
        return false;
    }

    static boolean isBell(MatItem item) {
        return item instanceof SimpleMatItem && item.getState().getBlock() instanceof DealerBellBlock;
    }

    public static boolean isJoker(ItemStack card) {
        if (card.getItem() instanceof CardItem cardItem) {
            Map<String, String> data = cardItem.getCardData(card);
            return "joker".equals(data.get("joker_or_card"));
        }
        return false;
    }

    // ---------------------------------------------------------------- left click: collect

    public static void collect(ServerPlayer player, BlockPos pos, Vec3 hit) {
        Level level = player.level();
        if (!player.isWithinBlockInteractionRange(pos, 1.0)) return;
        BlockState state = level.getBlockState(pos);
        List<ItemStack> cards = new ArrayList<>();
        Item pileItem;

        if (state.getBlock() instanceof CardpileBlock cardpile) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof CardPileBlockEntity pileEntity)) return;
            for (ItemStack c : pileEntity.getCards()) cards.add(c.copyWithCount(1));
            pileItem = cardpile.asItem();
            level.removeBlock(pos, false);
        } else if (findMatItem(level, pos, hit) instanceof CardPileMatItem matPile) {
            for (ItemStack c : matPile.getCards()) cards.add(c.copyWithCount(1));
            pileItem = matPile.getCardPile();
            MatBlockEntity owner = matPile.getMat();
            if (owner != null) owner.removeItem(matPile);
        } else {
            return;
        }
        if (cards.isEmpty()) return;

        giveCards(player, cards, pileItem);
        TableSounds.play(level, pos, TableSounds.CARD_TAKE, 0.8f);
    }

    private static void giveCards(ServerPlayer player, List<ItemStack> cards, Item fallbackPileItem) {
        ItemStack hand = player.getMainHandItem();
        Item handItem = hand.getItem();

        if (handItem instanceof CardpileItem pileItem) {
            ItemStack target = hand;
            if (hand.getCount() > 1) {
                target = hand.split(1);
            }
            List<ItemStack> merged = new ArrayList<>(pileItem.getCards(target));
            merged.addAll(cards);
            pileItem.setCards(merged, target);
            if (target != hand) giveOrDrop(player, target);
            return;
        }
        if (handItem instanceof CardItem cardItem && cardItem.getBlock().asItem() instanceof CardpileItem pileItem) {
            List<ItemStack> merged = new ArrayList<>();
            for (int i = 0; i < hand.getCount(); i++) merged.add(hand.copyWithCount(1));
            merged.addAll(cards);
            ItemStack pile = pileItem.getDefaultInstance();
            pileItem.setCards(merged, pile);
            player.setItemInHand(InteractionHand.MAIN_HAND, pile);
            return;
        }
        if (handItem instanceof CardboxItem box && box.isOpen(hand)) {
            box.addCards(hand, cards);
            return;
        }
        if (!(fallbackPileItem instanceof CardpileItem pileItem)) return;
        ItemStack pile = pileItem.getDefaultInstance();
        pileItem.setCards(cards, pile);
        if (hand.isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, pile);
        } else {
            giveOrDrop(player, pile);
        }
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
    }

    // ---------------------------------------------------------------- table key

    public static void tableKey(ServerPlayer player, BlockPos pos, Vec3 hit) {
        Level level = player.level();
        if (!player.isWithinBlockInteractionRange(pos, 1.0)) return;
        boolean sneaking = player.isShiftKeyDown();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (block instanceof CardpileBlock cardpile) {
            if (sneaking) return;
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof CardPileBlockEntity pileEntity)) return;
            if (!pileEntity.isFaceDown()) { // open cards: pick them up
                collect(player, pos, hit);
                return;
            }
            List<ItemStack> cards = new ArrayList<>(pileEntity.getCards());
            if (cards.size() < 2) return;
            Collections.shuffle(cards);
            cardpile.setCards(level, pos, cards);
            shuffleSound(level, pos);
            return;
        }
        if (block instanceof CoinPileblock coinPile) {
            ItemStack coin = coinPile.getCoin(level, pos).getFirst();
            if (coin.isEmpty()) return;
            ItemStack taken = coin.copyWithCount(1);
            coinPile.removeLastCoin(level, pos);
            giveOrDrop(player, taken);
            return;
        }
        if (block instanceof DealerBellBlock) {   // pick the bell up
            level.removeBlock(pos, false);
            giveOrDrop(player, new ItemStack(block));
            TableSounds.play(level, pos, TableSounds.CHIP_CLINK, 0.5f);
            return;
        }
        if (block instanceof MatBlock mat) {
            MatItem item = findMatItem(level, pos, hit);
            if (isBell(item)) {
                MatBlockEntity owner = item.getMat();
                if (owner != null) owner.removeItem(item);
                mat.setChanged(level, pos);
                for (ItemStack drop : item.getDrops()) giveOrDrop(player, drop.copy());
                TableSounds.play(level, pos, TableSounds.CHIP_CLINK, 0.5f);
                return;
            }
            if (sneaking) {
                clearTable(player, level, pos, hit, mat, item);
                return;
            }
            if (item instanceof CardPileMatItem matPile) {
                if (!matPile.getState().getValueOrElse(CardpileBlock.FACE_DOWN, true)) { // open cards: pick them up
                    collect(player, pos, hit);
                    return;
                }
                List<ItemStack> cards = new ArrayList<>(matPile.getCards());
                if (cards.size() < 2) return;
                Collections.shuffle(cards);
                matPile.setCards(cards);
                shuffleSound(level, pos);
            } else if (item instanceof CoinPileMatItem matCoins) {
                ItemStack coin = matCoins.getCoin().getFirst();
                if (coin.isEmpty()) return;
                ItemStack taken = coin.copyWithCount(1);
                matCoins.removeLastCoin();
                giveOrDrop(player, taken);
            }
        }
    }

    /** Collects every card pile on all connected mats into the targeted pile (or a new pile at the hit spot). */
    private static void clearTable(ServerPlayer player, Level level, BlockPos start, Vec3 hit, MatBlock startMat, MatItem target) {
        CardPileMatItem targetPile = target instanceof CardPileMatItem p ? p : null;
        if (target != null && targetPile == null) return; // looking at coins / holder etc.

        List<CardPileMatItem> piles = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());
        seen.add(start.immutable());
        while (!queue.isEmpty() && seen.size() <= MAX_MATS) {
            BlockPos pos = queue.poll();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MatBlockEntity matEntity) {
                for (MatItem item : matEntity.getMatItems()) {
                    if (item instanceof CardPileMatItem pile && pile != targetPile) piles.add(pile);
                }
            }
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos next = pos.relative(dir);
                if (!seen.contains(next) && level.getBlockState(next).getBlock() instanceof MatBlock) {
                    seen.add(next);
                    queue.add(next);
                }
            }
        }
        if (piles.isEmpty()) return;

        List<ItemStack> cards = new ArrayList<>();
        CardpileItem pileItem = null;
        for (CardPileMatItem pile : piles) {
            for (ItemStack c : pile.getCards()) cards.add(c.copyWithCount(1));
            if (pileItem == null) pileItem = pile.getCardPile();
            MatBlockEntity owner = pile.getMat();
            if (owner != null) owner.removeItem(pile);
        }
        if (cards.isEmpty()) return;

        if (targetPile != null) {
            targetPile.addCards(cards);
        } else {
            BlockState pileState = pileItem.getBlock().defaultBlockState()
                    .setValue(CardpileBlock.FACING, player.getDirection())
                    .setValue(CardpileBlock.FACE_DOWN, true);
            int x = (int) ((hit.x - start.getX()) * 16.0) - 8;
            int z = (int) ((hit.z - start.getZ()) * 16.0) - 8;
            CardPileMatItem newPile = new CardPileMatItem(pileState, x, z, cards, pileItem);
            MatBlockEntity matEntity = startMat.getMat(level, start);
            newPile.setMat(matEntity);
            if (matEntity != null && pileItem.canPlace(newPile, startMat, level, start)) {
                startMat.addMatItem(level, start, newPile);
                startMat.setChanged(level, start);
            } else {
                ItemStack stack = pileItem.getDefaultInstance();
                pileItem.setCards(cards, stack);
                giveOrDrop(player, stack);
            }
        }
        TableSounds.play(level, start, TableSounds.CARD_PLACE, 0.9f);
    }

    private static void shuffleSound(Level level, BlockPos pos) {
        TableSounds.play(level, pos, TableSounds.SHUFFLE, 0.8f);
    }

}
