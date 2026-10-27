package tcardstest;

import java.util.ArrayList;
import java.util.List;

import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.togyk.tcards.ChipTower;
import net.togyk.tcards.TableTricks;
import net.togyk.tcards.shuffler.ShufflerRegistry;
import net.togyk.tog_yks_cards.block.ModBlocks;
import net.togyk.tog_yks_cards.block.custom.CardpileBlock;
import net.togyk.tog_yks_cards.block.custom.CoinPileblock;
import net.togyk.tog_yks_cards.block.custom.MatBlock;
import net.togyk.tog_yks_cards.blockentity.entities.CoinPileBlockEntity;
import net.togyk.tog_yks_cards.item.ModItems;
import net.togyk.tog_yks_cards.item.custom.CardpileItem;
import net.togyk.tog_yks_cards.mat_item.CardPileMatItem;
import net.togyk.tog_yks_cards.mat_item.CoinPileMatItem;
import net.togyk.tog_yks_cards.mat_item.MatItem;

/** Chip tower (cap 30, falls 20..30, coins conserved) and card throwing. */
public class Tower {
    static ServerLevel level;
    static FakePlayer p;
    static final BlockPos GROUND_TOWER = new BlockPos(32, -60, 32);
    static final BlockPos MAT_TOWER = new BlockPos(39, -60, 33);
    static final BlockPos THROW_MAT = new BlockPos(38, -60, 40);
    static final BlockPos THROW_GROUND = new BlockPos(35, -61, 41);
    static CoinPileMatItem matPile;
    static CoinPileMatItem SMALL;
    static MatItem matBell;

    static void check(String n, boolean ok, Object info) {
        TestMain.check(n, ok, info);
    }

    static List<ItemStack> chips(int n) {
        List<ItemStack> l = new ArrayList<>();
        for (int i = 0; i < n; i++) l.add(new ItemStack(ModItems.RED_CHIP));
        return l;
    }

    static int coinsIn(int x0, int z0, int x1, int z1) {
        int n = 0;
        for (BlockPos pos : BlockPos.betweenClosed(x0, -60, z0, x1, -60, z1)) {
            if (level.getBlockEntity(pos) instanceof CoinPileBlockEntity be) n += be.getCoins().size();
            if (level.getBlockState(pos).getBlock() instanceof MatBlock mat) {
                for (MatItem it : mat.getMatItems(level, pos)) if (it instanceof CoinPileMatItem c) n += c.getCoins().size();
            }
        }
        for (Entity e : level.getEntities((Entity) null, new AABB(x0, -62, z0, x1 + 1, -55, z1 + 1), e -> e instanceof ItemEntity)) {
            n += ((ItemEntity) e).getItem().getCount();
        }
        return n;
    }

    static void start(ServerLevel l, FakePlayer player) {
        level = l;
        p = player;
        for (int cx = 1; cx <= 2; cx++) for (int cz = 1; cz <= 2; cz++) level.setChunkForced(cx, cz, true);
        for (int x = 28; x < 47; x++) for (int z = 28; z < 47; z++) {
            level.setBlock(new BlockPos(x, -61, z), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(new BlockPos(x, -60, z), Blocks.AIR.defaultBlockState(), 3);
        }
        for (Entity e : level.getEntities((Entity) null, new AABB(26, -64, 26, 48, -50, 48), e -> e instanceof ItemEntity)) e.discard();
        // ground tower: 29 coins, one more by right click -> 30 (old limit was 16) -> must fall
        level.setBlock(GROUND_TOWER, ModBlocks.COIN_PIlE.defaultBlockState().setValue(CoinPileblock.WATERLOGGED, false), 3);
        ModBlocks.COIN_PIlE.setCoins(level, GROUND_TOWER, chips(29));
        p.setShiftKeyDown(false);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RED_CHIP, 1));
        level.getBlockState(GROUND_TOWER).useItemOn(p.getMainHandItem(), level, p, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(GROUND_TOWER), Direction.UP, GROUND_TOWER, false));
        int n = ModBlocks.COIN_PIlE.getCoins(level, GROUND_TOWER).size();
        check("ground pile grows past 16 up to 30", n == 30, n);
        check("30 high tower is about to fall", ChipTower.pending() == 1, ChipTower.pending());

        // mat tower
        for (int x = 38; x <= 40; x++) for (int z = 32; z <= 34; z++) {
            level.setBlock(new BlockPos(x, -60, z), ShufflerRegistry.BLACKJACK_MAT.defaultBlockState(), 3);
        }
        List<Pair<ItemStack, Float>> pairs = new ArrayList<>();
        for (ItemStack c : chips(29)) pairs.add(new Pair<>(c, 0f));
        matPile = new CoinPileMatItem(ModBlocks.COIN_PIlE.defaultBlockState(), 0, 0, pairs);
        MatBlock mat = ShufflerRegistry.BLACKJACK_MAT;
        matPile.setMat(mat.getMat(level, MAT_TOWER));
        mat.addMatItem(level, MAT_TOWER, matPile);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RED_CHIP, 1));
        matPile.interact(p.getMainHandItem(), level.getBlockState(MAT_TOWER), level, MAT_TOWER, p, InteractionHand.MAIN_HAND,
                new BlockHitResult(new Vec3(39.5, -59.9, 33.5), Direction.UP, MAT_TOWER, false));
        check("mat pile grows past 15 up to 30", matPile.getCoins().size() == 30, matPile.getCoins().size());
        check("mat tower is about to fall too", ChipTower.pending() == 2, ChipTower.pending());

        // a small pile never falls
        List<Pair<ItemStack, Float>> few = new ArrayList<>();
        for (ItemStack c : chips(10)) few.add(new Pair<>(c, 0f));
        CoinPileMatItem small = SMALL = new CoinPileMatItem(ModBlocks.COIN_PIlE.defaultBlockState(), -6, -6, few);
        small.setMat(mat.getMat(level, new BlockPos(38, -60, 32)));
        mat.addMatItem(level, new BlockPos(38, -60, 32), small);
        small.addCoin(new ItemStack(ModItems.RED_CHIP));
        check("11 coins never fall", ChipTower.pending() == 2, ChipTower.pending());
    }

    /** Returns true when done. */
    static boolean step(int t) {
        if (t == 15) {
            int left = ModBlocks.COIN_PIlE.getCoins(level, GROUND_TOWER).size();
            check("ground tower fell, 2-4 coins stay", left >= 2 && left <= 4, left);
            int total = coinsIn(28, 28, 36, 36);
            check("no coin lost (ground)", total == 30, total);
            check("mat tower fell, 2-4 coins stay", matPile.getCoins().size() >= 2 && matPile.getCoins().size() <= 4, matPile.getCoins().size());
            int matTotal = coinsIn(37, 31, 41, 35);
            check("no coin lost (mat), incl. small pile", matTotal == 30 + 11, matTotal);
            int piles = 0;
            for (BlockPos pos : BlockPos.betweenClosed(37, -60, 31, 41, -60, 35))
                if (level.getBlockState(pos).getBlock() instanceof MatBlock m)
                    for (MatItem it : m.getMatItems(level, pos)) if (it instanceof CoinPileMatItem) piles++;
            check("fallen coins lie on the mats as small piles", piles > 3, piles);

            boolean near = true;
            for (BlockPos pos : BlockPos.betweenClosed(37, -60, 31, 41, -60, 35))
                if (level.getBlockState(pos).getBlock() instanceof MatBlock m)
                    for (MatItem it : m.getMatItems(level, pos)) if (it instanceof CoinPileMatItem c && c != SMALL) {
                        double cx = pos.getX() + 0.5 + c.getX() / 16.0, cz = pos.getZ() + 0.5 + c.getZ() / 16.0;
                        if (Math.hypot(cx - 39.5, cz - 33.5) > 1.05) near = false;
                    }
            check("fallen mat coins stay within 1 block", near, "");
            int sameMat = 0;
            for (MatItem it : ShufflerRegistry.BLACKJACK_MAT.getMatItems(level, MAT_TOWER)) if (it instanceof CoinPileMatItem) sameMat++;
            StringBuilder sizes = new StringBuilder();
            for (MatItem it : ShufflerRegistry.BLACKJACK_MAT.getMatItems(level, MAT_TOWER)) if (it instanceof CoinPileMatItem c) sizes.append(c.getCoins().size()).append(' ');
            check("most fallen coins stay on the tower's own mat", sameMat >= 6, sameMat + " piles: " + sizes);
            boolean groundNear = true;
            for (BlockPos pos : BlockPos.betweenClosed(28, -60, 28, 36, -60, 36))
                if (level.getBlockState(pos).getBlock() instanceof CoinPileblock && (Math.abs(pos.getX() - 32) > 1 || Math.abs(pos.getZ() - 32) > 1)) groundNear = false;
            check("fallen ground coins stay within 1 block", groundNear, "");

            // dealer bell on a mat (decorative mat item)
            BlockPos bellMat = new BlockPos(43, -60, 43);
            level.setBlock(bellMat, ShufflerRegistry.BLACKJACK_MAT.defaultBlockState(), 3);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ShufflerRegistry.DEALER_BELL));
            BlockHitResult bellHit = new BlockHitResult(new Vec3(43.5, -59.95, 43.5), Direction.UP, bellMat, false);
            level.getBlockState(bellMat).useItemOn(p.getMainHandItem(), level, p, InteractionHand.MAIN_HAND, bellHit);
            matBell = null;
            for (MatItem it : ShufflerRegistry.BLACKJACK_MAT.getMatItems(level, bellMat))
                if (it.getState().getBlock() == ShufflerRegistry.DEALER_BELL) matBell = it;
            check("bell can be placed on a mat", matBell != null, ShufflerRegistry.BLACKJACK_MAT.getMatItems(level, bellMat).size());
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            level.getBlockState(bellMat).useItemOn(ItemStack.EMPTY, level, p, InteractionHand.MAIN_HAND,
                    new BlockHitResult(new Vec3(43.5, -59.7, 43.5), Direction.UP, bellMat, false));
            check("bell on mat rings (pressed)", matBell != null && matBell.getState().getValue(net.togyk.tcards.shuffler.DealerBellBlock.PRESSED), matBell == null ? "" : matBell.getState());
        }
        if (t == 30) {
            check("bell on mat springs back", matBell != null && !matBell.getState().getValue(net.togyk.tcards.shuffler.DealerBellBlock.PRESSED), matBell == null ? "" : matBell.getState());
            p.setPos(43.5, -60, 41.5);
            p.getInventory().clearContent();
            BlockPos bellMat = new BlockPos(43, -60, 43);
            net.togyk.tcards.TableActions.tableKey(p, bellMat, new Vec3(43.5, -59.7, 43.5));
            boolean gone = ShufflerRegistry.BLACKJACK_MAT.getMatItems(level, bellMat).stream().noneMatch(i -> i.getState().getBlock() == ShufflerRegistry.DEALER_BELL);
            check("middle click picks the bell up from the mat", gone && p.getInventory().countItem(ShufflerRegistry.DEALER_BELL.asItem()) == 1,
                    gone + " " + p.getInventory().countItem(ShufflerRegistry.DEALER_BELL.asItem()));
            BlockPos ground = new BlockPos(44, -60, 41);
            level.setBlock(ground, ShufflerRegistry.DEALER_BELL.defaultBlockState(), 3);
            net.togyk.tcards.TableActions.tableKey(p, ground, new Vec3(44.5, -59.8, 41.5));
            check("middle click picks up a bell standing on a block", level.getBlockState(ground).isAir()
                    && p.getInventory().countItem(ShufflerRegistry.DEALER_BELL.asItem()) == 2, level.getBlockState(ground));
            int items = level.getEntities((Entity) null, new AABB(26, -64, 26, 48, -50, 48), e -> e instanceof ItemEntity).size();
            check("no coins dropped as items", items == 0, items);
            return true;
        }
        return false;
    }

    static boolean ok2;

    static CardpileItem pileItem() {
        return (CardpileItem) ModBlocks.RED_CARDPILE.asItem();
    }
}
