package tcardstest;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.togyk.tcards.TableTricks;
import net.togyk.tcards.shuffler.DealerBellBlock;
import net.togyk.tcards.shuffler.ShufflerRegistry;
import net.togyk.tog_yks_cards.item.ModItems;

/** Bell, coin flip, card flourish. Runs over several server ticks. */
public class FunTests {
    static int tick = -1;
    static FakePlayer p;
    static ServerLevel level;
    static final BlockPos BELL = new BlockPos(14, -60, 3);

    static void check(String n, boolean ok, Object info) {
        TestMain.check(n, ok, info);
    }

    static long fx() {
        return level.getEntities((Entity) null, new AABB(-20, -80, -20, 40, 0, 40),
                e -> e.entityTags().contains(TableTricks.FX_TAG)).size();
    }

    static long fxOfType(Class<?> c) {
        return level.getEntities((Entity) null, new AABB(-20, -80, -20, 40, 0, 40),
                e -> e.entityTags().contains(TableTricks.FX_TAG) && c.isInstance(e)).size();
    }

    static void start(ServerLevel l) {
        level = l;
        level.setChunkForced(0, 0, true);   // like a real player standing at the table: block ticks run
        p = FakePlayer.get(level);
        p.setPos(10.5, -60, 2.5);
        level.setBlock(BELL.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(BELL, ShufflerRegistry.DEALER_BELL.defaultBlockState(), 3);
        DealerBellBlock.ring(level, BELL, level.getBlockState(BELL));
        check("bell pressed after ring", level.getBlockState(BELL).getValue(DealerBellBlock.PRESSED), level.getBlockState(BELL));
        check("bell gives redstone signal", level.hasNeighborSignal(BELL.east()), level.getBestNeighborSignal(BELL.east()));

        p.getInventory().clearContent();
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RED_CHIP, 3));
        p.setShiftKeyDown(true);
        boolean started = TableTricks.start(p, InteractionHand.MAIN_HAND);
        check("coin flip starts", started && TableTricks.running() == 1, TableTricks.running());
        check("coin leaves the hand during the flip", p.getMainHandItem().getCount() == 2, p.getMainHandItem());
        check("coin display entity spawned", fxOfType(Display.ItemDisplay.class) == 1, fx());
        check("second flip right away is ignored", !TableTricks.start(p, InteractionHand.MAIN_HAND) && p.getMainHandItem().getCount() == 2, p.getMainHandItem());
        tick = 0;
        ServerTickEvents.END_SERVER_TICK.register(server -> step());
    }

    static void step() {
        if (tick < 0) return;
        tick++;
        if (tick > 95) {
            if (Tower.step(tick - 95)) {
                System.out.println("TCARDS-TEST SUMMARY2 pass=" + TestMain.pass + " fail=" + TestMain.fail);
                tick = -1;
                if (System.getProperty("tcards.ci") != null) {   // CI: stop the server, exit code = result
                    System.out.flush();
                    Runtime.getRuntime().halt(TestMain.fail == 0 ? 0 : 1);
                }
            }
            return;
        }
        if (tick == 30) {
            DealerBellBlock.ring(level, BELL, level.getBlockState(BELL).setValue(DealerBellBlock.PRESSED, false));
        }
        if (tick == 40) System.out.println("TCARDS-DEBUG bell at 40: " + level.getBlockState(BELL) + " forced=" + level.getForceLoadedChunks());
        if (tick == 34) check("bell still pressed right after ring", level.getBlockState(BELL).getValue(DealerBellBlock.PRESSED), level.getBlockState(BELL));
        if (tick == 45) {
            check("bell released after a moment", !level.getBlockState(BELL).getValue(DealerBellBlock.PRESSED), level.getBlockState(BELL));
        }
        if (tick == 5 || tick == 21 || tick == 23) System.out.println("TCARDS-DEBUG tick " + tick + " running=" + TableTricks.running() + " fx=" + fx());
        if (tick == 30) {
            check("coin result label shown", fxOfType(Display.TextDisplay.class) == 1, fx());
        }
        if (tick == 70) {
            check("coin flip finished", TableTricks.running() == 0, TableTricks.running());
            check("coin back in hand", p.getMainHandItem().is(ModItems.RED_CHIP) && p.getMainHandItem().getCount() == 3, p.getMainHandItem());
            check("no coin effects left", fx() == 0, fx());
            // card flourish through the real item-use path (sneak + right click into the air)
            ItemStack card = TestMain.deck(1).get(0);
            p.setItemInHand(InteractionHand.MAIN_HAND, card);
            InteractionResult r = p.gameMode.useItem(p, level, p.getMainHandItem(), InteractionHand.MAIN_HAND);
            check("sneak + use with card starts flourish", r.consumesAction() && TableTricks.running() == 1, r + " running=" + TableTricks.running());
            check("card leaves the hand", p.getMainHandItem().isEmpty(), p.getMainHandItem());
        }
        if (tick == 95) {
            check("card flourish finished", TableTricks.running() == 0, TableTricks.running());
            check("card caught again", p.getMainHandItem().getItem() == ModItems.RED_PLAYING_CARD, p.getMainHandItem());
            check("no card effects left", fx() == 0, fx());
            p.setShiftKeyDown(false);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RED_CHIP, 1));
            InteractionResult r = p.gameMode.useItem(p, level, p.getMainHandItem(), InteractionHand.MAIN_HAND);
            check("without sneaking nothing happens", TableTricks.running() == 0 && p.getMainHandItem().getCount() == 1, r);
            Tower.start(level, p);
        }
    }
}
