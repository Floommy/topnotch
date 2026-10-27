package net.togyk.tcards.restaurant;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.togyk.tcards.TableSounds;
import net.togyk.tcards.restaurant.Restaurants.Order;
import net.togyk.tcards.restaurant.Restaurants.Restaurant;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Kiosk (lower half), register, kitchen screen and pickup screen: all belong to one restaurant.
 * Screens draw their content with text display entities; the kiosk prints receipts.
 */
public class RestaurantBlockEntity extends BlockEntity {
    /** PICKUP = "ready for pickup" screen, PREPARING = "being prepared" screen. */
    public enum Kind { KIOSK, REGISTER, KITCHEN, PICKUP, PREPARING }

    private UUID restaurant;
    private UUID owner;
    private String ownerName = "";
    private int timer;

    // screen content (not saved: rebuilt from the restaurant data)
    private final Display.TextDisplay[] screens = new Display.TextDisplay[2];
    private final String[] shown = new String[2];

    // receipt printing (kiosk)
    private int printTicks;
    private int printLength;
    private UUID printFor;
    private ServerPlayer printPlayer;
    private Receipt printing;
    private Display.ItemDisplay strip;
    public static final int PRINT_TICKS = 50, MAX_LENGTH = 60;

    public RestaurantBlockEntity(BlockPos pos, BlockState state) {
        super(RestaurantRegistry.RESTAURANT_BE, pos, state);
    }

    public Kind kind() {
        return getBlockState().getBlock() instanceof RestaurantBlock b ? b.kind : Kind.REGISTER;
    }

    public UUID restaurantId() {
        return restaurant;
    }

    public Restaurant restaurant() {
        if (restaurant == null || level == null || level.getServer() == null) return null;
        return Restaurants.get(level.getServer()).get(restaurant);
    }

    public void setRestaurant(UUID id) {
        restaurant = id;
        setChanged();
        refreshNow();
    }

    public void setOwner(Player player) {
        owner = player.getUUID();
        ownerName = player.getName().getString();
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    public UUID owner() {
        return owner;
    }

    /** Settings, linking and breaking: block owner, restaurant owner and ops. */
    public boolean canManage(Player player) {
        if (owner == null || owner.equals(player.getUUID()) || net.togyk.tcards.atm.AtmBlockEntity.isAdmin(player)) return true;
        if (level != null && !level.isClientSide()) {
            Restaurant r = restaurant();
            return r != null && r.canManage(player);
        }
        return false;
    }

    public Direction facing() {
        BlockState s = getBlockState();
        return s.hasProperty(HorizontalDirectionalBlock.FACING) ? s.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
    }

    // ------------------------------------------------------------------ receipt printer

    public boolean isPrinting() {
        return printTicks > 0;
    }

    public int printLength() {
        return printLength;
    }

    public void startPrinting(ServerPlayer customer, Receipt receipt) {
        if (printing != null) finishPrinting();       // previous receipt is done right away
        printing = receipt;
        printFor = customer.getUUID();
        printPlayer = customer;
        printTicks = PRINT_TICKS;
        printLength = 0;
        if (level instanceof ServerLevel l) {
            l.playSound(null, worldPosition, TableSounds.RECEIPT_PRINT, SoundSource.BLOCKS, 0.8f, 1.0f);
            Direction f = facing();
            Vec3 slot = Vec3.atBottomCenterOf(worldPosition).add(f.getStepX() * 0.265, 0.80, f.getStepZ() * 0.265);
            ItemStack paper = new ItemStack(Receipt.ITEM);
            paper.set(net.minecraft.core.component.DataComponents.ITEM_MODEL,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("tog_yks-cards", "receipt_strip"));
            ScreenFx.kill(strip);
            strip = ScreenFx.item(l, slot, ScreenFx.yaw(f), paper);
            updateStrip(0);
        }
        setChanged();
    }

    /** Right click while printing: the paper gets longer. */
    public void stretch() {
        if (!isPrinting()) return;
        printLength = Math.min(MAX_LENGTH, printLength + 1);
        printTicks = Math.min(PRINT_TICKS * 3, printTicks + 4);
        if (level instanceof ServerLevel l) {
            l.playSound(null, worldPosition, TableSounds.RECEIPT_PRINT, SoundSource.BLOCKS, 0.5f, 1.2f + l.getRandom().nextFloat() * 0.3f);
        }
    }

    private float stripHeight() {
        float base = 0.18f * Math.min(1f, (PRINT_TICKS - Math.min(PRINT_TICKS, printTicks)) / (float) PRINT_TICKS + 0.2f);
        return base + printLength * 0.03f;
    }

    private void updateStrip(int interpolate) {
        if (!ScreenFx.alive(strip)) return;
        float h = stripHeight();
        // the paper hangs down out of the slot: top stays at the slot
        ScreenFx.transform(strip, new Vector3f(0, -h / 2f, 0.02f), new Quaternionf(), new Vector3f(0.22f, h, 1f), interpolate);
    }

    private void finishPrinting() {
        Receipt r = printing;
        printing = null;
        printTicks = 0;
        ScreenFx.kill(strip);
        strip = null;
        if (r == null || level == null || level.getServer() == null) return;
        Receipt done = new Receipt(r.restaurant(), r.number(), r.lines(), r.total(), r.currency(), printLength);
        ItemStack stack = done.toStack();
        ServerPlayer p = printFor == null ? null : level.getServer().getPlayerList().getPlayer(printFor);
        if (p == null && printPlayer != null && !printPlayer.isRemoved()) p = printPlayer;
        printPlayer = null;
        if (p != null && p.distanceToSqr(Vec3.atCenterOf(worldPosition)) < 64 && p.getInventory().add(stack)) return;
        Direction f = facing();
        net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(level,
                worldPosition.getX() + 0.5 + f.getStepX() * 0.7, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5 + f.getStepZ() * 0.7, stack);
        level.addFreshEntity(drop);
    }

    // ------------------------------------------------------------------ screens

    public void refreshNow() {
        timer = 1000;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RestaurantBlockEntity be) {
        if (be.printTicks > 0) {
            be.printTicks--;
            be.updateStrip(1);
            if (be.printTicks == 0) be.finishPrinting();
        }
        if (++be.timer >= 10) {
            be.timer = 0;
            be.updateScreens((ServerLevel) level);
        }
    }

    private void updateScreens(ServerLevel l) {
        Kind kind = kind();
        if (kind != Kind.KITCHEN && kind != Kind.PICKUP && kind != Kind.PREPARING) return;
        Restaurant r = restaurant();
        Direction f = facing();
        Direction right = f.getCounterClockWise();
        // the screen panel is the back 3 px of the block; the text sits just in front of it
        Vec3 face = Vec3.atBottomCenterOf(worldPosition).add(f.getStepX() * (-5 / 16.0 + 0.012), 1.2 / 16.0, f.getStepZ() * (-5 / 16.0 + 0.012));
        if (kind == Kind.KITCHEN) {
            show(l, 0, face, f, 0.25f, 132, kitchenText(r, l.getServer().overworld().getGameTime()));
            hide(1);
        } else {
            show(l, 0, face, f, 0.36f, 92, statusText(r, kind == Kind.PICKUP));
            hide(1);
        }
    }

    private void show(ServerLevel l, int i, Vec3 at, Direction f, float scale, int width, String text) {
        if (!ScreenFx.alive(screens[i])) {
            screens[i] = ScreenFx.text(l, at, f, scale, width, 0);
            shown[i] = null;
        }
        if (screens[i] != null && !text.equals(shown[i])) {
            ScreenFx.setText(screens[i], Component.literal(text));
            shown[i] = text;
        }
    }

    private void hide(int i) {
        ScreenFx.kill(screens[i]);
        screens[i] = null;
        shown[i] = null;
    }

    /** Fixed number of lines so the text block always has the same height on the panel. */
    private static String pad(List<String> lines, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append('\n');
            sb.append(i < lines.size() ? lines.get(i) : " ");
        }
        return sb.toString();
    }

    public static final int KITCHEN_LINES = 13, STATUS_LINES = 9;

    static String kitchenText(Restaurant r, long now) {
        List<String> lines = new ArrayList<>();
        if (r == null) {
            lines.add("§c§lNot linked");
            lines.add("§7Sneak + right-click,");
            lines.add("§7then click a register");
            return pad(lines, KITCHEN_LINES);
        }
        List<Order> open = r.orders.stream().filter(o -> o.status == Restaurants.KITCHEN)
                .sorted(Comparator.comparingLong(o -> o.created)).toList();
        lines.add("§e§lKITCHEN §r§7(" + open.size() + ")");
        int shownOrders = 0;
        for (Order o : open) {
            int need = 1 + o.lines.size();
            if (lines.size() + need > KITCHEN_LINES - (open.size() - shownOrders > 1 ? 1 : 0)) break;
            long min = Math.max(0, (now - o.created) / 1200);
            String age = min >= 5 ? "§c" + min + " min" : "§7" + min + " min";
            lines.add("§f§l#" + o.number + " " + age);
            for (Restaurants.Line l : o.lines) lines.add("§f" + l.qty() + "x " + RestaurantMenu.shortName(l.item()));
            shownOrders++;
        }
        if (shownOrders < open.size()) lines.add("§7+" + (open.size() - shownOrders) + " more");
        if (open.isEmpty()) lines.add("§7No orders");
        return pad(lines, KITCHEN_LINES);
    }

    /** Ready screen (ready = true) or preparing screen: big numbers, two per line. */
    static String statusText(Restaurant r, boolean ready) {
        List<String> lines = new ArrayList<>();
        if (r == null) {
            lines.add("§c§lNot linked");
            lines.add("§7Sneak + right-click,");
            lines.add("§7then click a register");
            return pad(lines, STATUS_LINES);
        }
        lines.add(ready ? "§a§lReady" : "§e§lPreparing");
        List<Order> list = r.orders.stream().filter(o -> o.status == (ready ? Restaurants.READY : Restaurants.KITCHEN))
                .sorted(ready ? Comparator.comparingLong((Order o) -> o.readyAt) : Comparator.comparingInt((Order o) -> o.number)).toList();
        String color = ready ? "§a§l" : "§f§l";
        int perLine = 2, maxLines = STATUS_LINES - 1;
        int capacity = perLine * maxLines;
        int show = list.size() > capacity ? capacity - 1 : list.size();
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < show; i++) {
            if (!line.isEmpty()) line.append("   ");
            line.append(color).append(list.get(i).number);
            if ((i + 1) % perLine == 0) {
                lines.add(line.toString());
                line = new StringBuilder();
            }
        }
        if (list.size() > capacity) {
            if (!line.isEmpty()) line.append("   ");
            line.append("§7+").append(list.size() - show);
        }
        if (!line.isEmpty()) lines.add(line.toString());
        if (list.isEmpty()) lines.add("§8-");
        return pad(lines, STATUS_LINES);
    }

    public String screenText(int i) {
        return shown[i];
    }

    // ------------------------------------------------------------------ removal & saving

    public void removeFx() {
        hide(0);
        hide(1);
        if (printing != null) finishPrinting();
        ScreenFx.kill(strip);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !level.isClientSide()) removeFx();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // chunk unload: entities are saved with the chunk and thrown away on load, just forget them
        screens[0] = screens[1] = null;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (owner != null) tag.store("owner", UUIDUtil.CODEC, owner);
        return tag;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (restaurant != null) output.store("restaurant", UUIDUtil.CODEC, restaurant);
        if (owner != null) output.store("owner", UUIDUtil.CODEC, owner);
        output.putString("owner_name", ownerName);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        restaurant = input.read("restaurant", UUIDUtil.CODEC).orElse(null);
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        ownerName = input.getStringOr("owner_name", "");
    }
}
