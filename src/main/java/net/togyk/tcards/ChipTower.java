package net.togyk.tcards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.togyk.tog_yks_cards.block.ModBlocks;
import net.togyk.tog_yks_cards.block.custom.CoinPileblock;
import net.togyk.tog_yks_cards.block.custom.MatBlock;
import net.togyk.tog_yks_cards.blockentity.entities.CoinPileBlockEntity;
import net.togyk.tog_yks_cards.blockentity.entities.MatBlockEntity;
import net.togyk.tog_yks_cards.item.custom.MatPlaceable;
import net.togyk.tog_yks_cards.mat_item.CoinPileMatItem;
import net.togyk.tog_yks_cards.mat_item.MatItem;

/**
 * Coin / chip piles may grow up to 30 high. From 20 coins on, every added coin can make the tower fall
 * (evenly spread between 20 and 30, at 30 it always falls). It wobbles (client), then crashes:
 * a few coins stay, the rest scatter over the table in small piles.
 */
public final class ChipTower {
    private ChipTower() {}

    public static final int MIN_FALL = 20, MAX_HEIGHT = 30;
    private static final int FALL_DELAY = 8;

    private static final class Pending {
        final Object pile;
        int ticks = FALL_DELAY;

        Pending(Object pile) {
            this.pile = pile;
        }
    }

    private static final List<Pending> PENDING = new ArrayList<>();
    private static final Set<Object> QUEUED = Collections.newSetFromMap(new IdentityHashMap<>());

    /** Test hook: forces every tower of 20+ to fall. */
    public static boolean alwaysFall = false;

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());
    }

    static boolean rollFall(int coins, RandomSource random) {
        if (coins < MIN_FALL) return false;
        if (coins >= MAX_HEIGHT || alwaysFall) return true;
        return random.nextInt(MAX_HEIGHT + 1 - coins) == 0;   // hazard of an even 20..30 distribution
    }

    public static void coinAdded(CoinPileBlockEntity be) {
        Level level = be.getLevel();
        if (!(level instanceof ServerLevel) || QUEUED.contains(be)) return;
        if (rollFall(be.getCoins().size(), level.getRandom())) queue(be);
    }

    public static void coinAdded(CoinPileMatItem pile) {
        MatBlockEntity mat = pile.getMat();
        if (mat == null || !(mat.getLevel() instanceof ServerLevel level) || QUEUED.contains(pile)) return;
        if (rollFall(pile.getCoins().size(), level.getRandom())) queue(pile);
    }

    private static void queue(Object pile) {
        QUEUED.add(pile);
        PENDING.add(new Pending(pile));
    }

    public static int pending() {
        return PENDING.size();
    }

    private static void tick() {
        if (PENDING.isEmpty()) return;
        List<Pending> due = new ArrayList<>();
        for (Iterator<Pending> it = PENDING.iterator(); it.hasNext(); ) {
            Pending p = it.next();
            if (--p.ticks <= 0) {
                it.remove();
                QUEUED.remove(p.pile);
                due.add(p);
            }
        }
        for (Pending p : due) {
            if (p.pile instanceof CoinPileBlockEntity be) toppleBlock(be);
            else if (p.pile instanceof CoinPileMatItem mi) toppleMat(mi);
        }
    }

    private static void toppleBlock(CoinPileBlockEntity be) {
        if (!(be.getLevel() instanceof ServerLevel level) || be.isRemoved()) return;
        BlockPos pos = be.getBlockPos();
        if (!(level.getBlockState(pos).getBlock() instanceof CoinPileblock)) return;
        List<Pair<ItemStack, Float>> coins = new ArrayList<>(be.getCoins());
        if (coins.size() < MIN_FALL / 2) return; // somebody took coins in the meantime
        int keep = 2 + level.getRandom().nextInt(3);
        be.setCoinsAndRotation(new ArrayList<>(coins.subList(0, keep)));
        List<ItemStack> falling = new ArrayList<>();
        for (Pair<ItemStack, Float> c : coins.subList(keep, coins.size())) falling.add(c.getFirst().copy());
        crash(level, new Vec3(pos.getX() + 0.5, pos.getY() + 0.03, pos.getZ() + 0.5), pos, null, be, falling, coins.size(),
                back -> {
                    List<Pair<ItemStack, Float>> now = new ArrayList<>(be.getCoins());
                    now.addAll(rotated(back, level.getRandom()));
                    be.setCoinsAndRotation(now);
                });
    }

    private static void toppleMat(CoinPileMatItem pile) {
        MatBlockEntity mat = pile.getMat();
        if (mat == null || !(mat.getLevel() instanceof ServerLevel level) || !mat.getMatItems().contains(pile)) return;
        List<Pair<ItemStack, Float>> coins = new ArrayList<>(pile.getCoins());
        if (coins.size() < MIN_FALL / 2) return;
        int keep = 2 + level.getRandom().nextInt(3);
        pile.setCoinsAndRotation(new ArrayList<>(coins.subList(0, keep)));
        List<ItemStack> falling = new ArrayList<>();
        for (Pair<ItemStack, Float> c : coins.subList(keep, coins.size())) falling.add(c.getFirst().copy());
        BlockPos pos = mat.getBlockPos();
        Vec3 center = new Vec3(pos.getX() + 0.5 + pile.getX() / 16.0, pos.getY() + 0.07, pos.getZ() + 0.5 + pile.getZ() / 16.0);
        crash(level, center, null, pos, pile, falling, coins.size(),
                back -> {
                    List<Pair<ItemStack, Float>> now = new ArrayList<>(pile.getCoins());
                    now.addAll(rotated(back, level.getRandom()));
                    pile.setCoinsAndRotation(now);
                });
    }

    private static final double[] DISTANCES = {0.22, 0.3, 0.38, 0.46, 0.55, 0.65, 0.75, 0.87, 1.0};
    private static final double[] TURNS = {0, 0.35, -0.35, 0.7, -0.7, 1.1, -1.1, 1.6, -1.6, 2.2, -2.2, Math.PI};

    /**
     * The falling coins land in small piles along one direction (spreading sideways / further out when it is crowded).
     * Nothing becomes an item: if really no spot is free, the coins go back onto the old pile.
     */
    private static void crash(ServerLevel level, Vec3 center, BlockPos origin, BlockPos sameBlock, Object originPile, List<ItemStack> falling, int height,
                              java.util.function.Consumer<List<ItemStack>> putBack) {
        RandomSource r = level.getRandom();
        double angle = r.nextDouble() * Math.PI * 2;
        Vec3 dir = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        List<ItemStack> leftover = new ArrayList<>();
        int group = 0, i = 0;
        while (i < falling.size()) {
            int n = Math.min(falling.size() - i, 1 + r.nextInt(3));
            List<ItemStack> coins = new ArrayList<>(falling.subList(i, i + n));
            i += n;
            double jitter = (r.nextDouble() - 0.5) * 0.5;
            double start = Math.min(0.22 + group * 0.06, 0.6);
            boolean placed = false;
            // first try to stay on the tower's own mat, then anywhere within one block
            // free spots first (own mat, then within one block); only then onto other small piles
            search:
            for (int merge = 0; merge < 2; merge++)
            for (int pass = sameBlock != null ? 0 : 1; pass < 2; pass++) {
                for (double dist : DISTANCES) {
                    for (double turn : TURNS) {
                        double a = angle + turn + jitter * 0.5;
                        Vec3 spot = center.add(Math.cos(a) * dist, 0, Math.sin(a) * dist);
                        if (pass == 0 && !BlockPos.containing(spot.x, spot.y, spot.z).equals(sameBlock)) continue;
                        if (land(level, spot, origin, originPile, coins, merge == 1)) {
                            placed = true;
                            break search;
                        }
                    }
                }
            }
            if (!placed) leftover.addAll(coins);
            group++;
        }
        if (!leftover.isEmpty()) putBack.accept(leftover);
        level.playSound(null, center.x, center.y, center.z, TableSounds.TOWER_FALL, SoundSource.BLOCKS, 1.0f, 0.95f + r.nextFloat() * 0.1f);
        Vec3 top = center.add(dir.scale(0.4)).add(0, 0.15, 0);
        level.sendParticles(ParticleTypes.CRIT, top.x, top.y, top.z, 10, 0.25, 0.1, 0.25, 0.15);
        level.sendParticles(ParticleTypes.WHITE_SMOKE, center.x, center.y + 0.1, center.z, 6, 0.2, 0.05, 0.2, 0.01);
        Component msg = Component.literal("The chip tower (" + height + ") fell over!").withStyle(ChatFormatting.GOLD);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(center) < 12 * 12) p.sendOverlayMessage(msg);
        }
    }

    private static List<Pair<ItemStack, Float>> rotated(List<ItemStack> coins, RandomSource r) {
        List<Pair<ItemStack, Float>> out = new ArrayList<>();
        for (ItemStack c : coins) out.add(new Pair<>(c.copyWithCount(1), r.nextInt(16) * 22.5f));
        return out;
    }

    private static boolean land(ServerLevel level, Vec3 spot, BlockPos origin, Object originPile, List<ItemStack> coins, boolean merge) {
        BlockPos pos = BlockPos.containing(spot.x, spot.y, spot.z);
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof MatBlock mat) {
            MatBlockEntity entity = mat.getMat(level, pos);
            if (entity == null) return false;
            int x = (int) Math.floor((spot.x - pos.getX()) * 16.0) - 8;
            int z = (int) Math.floor((spot.z - pos.getZ()) * 16.0) - 8;
            CoinPileMatItem item = new CoinPileMatItem(ModBlocks.COIN_PIlE.defaultBlockState(), x, z, rotated(coins, level.getRandom()));
            item.setMat(entity);
            List<MatItem> others = mat.getAllMatItems(level, pos);
            if (MatPlaceable.canPlaceItem(item, others)) {
                mat.addMatItem(level, pos, item);
                mat.setChanged(level, pos);
                return true;
            }
            // crowded: onto a small coin pile lying there
            if (merge && TableActions.findMatItem(level, pos, spot) instanceof CoinPileMatItem small && small != originPile
                    && small.getCoins().size() + coins.size() <= 8) {
                for (ItemStack c : coins) small.addCoin(c.copyWithCount(1));
                mat.setChanged(level, pos);
                return true;
            }
            return false;
        }
        if (pos.equals(origin)) return false;
        if (state.getBlock() instanceof CoinPileblock pile && level.getBlockEntity(pos) instanceof CoinPileBlockEntity be) {
            if (!merge) return false;
            if (be == originPile || be.getCoins().size() + coins.size() > 8) return false;
            for (ItemStack c : coins) pile.addCoin(level, pos, c);
            return true;
        }
        if (state.isAir() && level.getBlockState(pos.below()).isSolid()) {
            BlockState pileState = ModBlocks.COIN_PIlE.defaultBlockState().setValue(CoinPileblock.WATERLOGGED, false);
            level.setBlock(pos, pileState, Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof CoinPileBlockEntity be) {
                be.setCoinsAndRotation(rotated(coins, level.getRandom()));
                return true;
            }
            level.removeBlock(pos, false);
        }
        return false;
    }
}
