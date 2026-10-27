package net.togyk.tcards.restaurant;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** A tray with up to 6 menu items, each shown as its own 3D model. Keeps its food when picked up. */
public class TrayBlockEntity extends BlockEntity {
    public static final int SIZE = 6;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final Display.ItemDisplay[] shown = new Display.ItemDisplay[SIZE];
    private final String[] shownItem = new String[SIZE];
    private boolean dirty = true;
    private int timer;

    public TrayBlockEntity(BlockPos pos, BlockState state) {
        super(RestaurantRegistry.TRAY_BE, pos, state);
    }

    public NonNullList<ItemStack> items() {
        return items;
    }

    public int count() {
        int n = 0;
        for (ItemStack s : items) if (!s.isEmpty()) n++;
        return n;
    }

    /** Puts one item on the tray; false if full or not food from the menu. */
    public boolean add(ItemStack stack) {
        if (RestaurantMenu.pathOf(stack) == null) return false;
        for (int i = 0; i < SIZE; i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, stack.copyWithCount(1));
                changed();
                return true;
            }
        }
        return false;
    }

    /** Takes the item that was put on last. */
    public ItemStack takeLast() {
        for (int i = SIZE - 1; i >= 0; i--) {
            if (!items.get(i).isEmpty()) {
                ItemStack s = items.get(i);
                items.set(i, ItemStack.EMPTY);
                changed();
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    private void changed() {
        dirty = true;
        setChanged();
    }

    /** 3D model used on the tray: same item, different model (the hand keeps the normal look). */
    static ItemStack model(ItemStack food) {
        String path = RestaurantMenu.pathOf(food);
        ItemStack s = food.copyWithCount(1);
        if (path != null) s.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("tog_yks-cards", "tray/" + path));
        return s;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TrayBlockEntity be) {
        if (++be.timer < 10 && !be.dirty) return;
        be.timer = 0;
        be.dirty = false;
        be.updateFx((ServerLevel) level);
    }

    /** Slot positions on the tray (3 x 2), for a tray facing north. */
    static final float[][] SPOTS = {{-0.27f, -0.12f}, {0f, -0.12f}, {0.27f, -0.12f}, {-0.27f, 0.13f}, {0f, 0.13f}, {0.27f, 0.13f}};
    static final float FOOD_SCALE = 0.36f;

    private void updateFx(ServerLevel level) {
        Direction f = getBlockState().hasProperty(HorizontalDirectionalBlock.FACING) ? getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        Direction right = f.getCounterClockWise();
        for (int i = 0; i < SIZE; i++) {
            ItemStack s = items.get(i);
            String key = s.isEmpty() ? null : RestaurantMenu.pathOf(s);
            if (key == null) {
                ScreenFx.kill(shown[i]);
                shown[i] = null;
                shownItem[i] = null;
                continue;
            }
            if (ScreenFx.alive(shown[i]) && key.equals(shownItem[i])) continue;
            ScreenFx.kill(shown[i]);
            float side = SPOTS[i][0], depth = SPOTS[i][1];
            // depth: + is toward the front (facing side)
            Vec3 at = Vec3.atBottomCenterOf(worldPosition).add(
                    right.getStepX() * side + f.getStepX() * -depth, 1.1 / 16.0 + FOOD_SCALE / 2, right.getStepZ() * side + f.getStepZ() * -depth);
            float yaw = ScreenFx.yaw(f) + ((i * 37) % 5 - 2) * 6;   // a bit messy, like real food on a tray
            shown[i] = ScreenFx.item(level, at, yaw, model(s));
            if (shown[i] != null) ScreenFx.transform(shown[i], new Vector3f(), new Quaternionf(), new Vector3f(FOOD_SCALE), 0);
            shownItem[i] = key;
        }
    }

    public void removeFx() {
        for (int i = 0; i < SIZE; i++) {
            ScreenFx.kill(shown[i]);
            shown[i] = null;
            shownItem[i] = null;
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !level.isClientSide()) removeFx();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        for (int i = 0; i < SIZE; i++) {
            shown[i] = null;
            shownItem[i] = null;
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
        // which food sits on which spot, for the 3D tray model when the tray is held
        List<String> spots = new ArrayList<>();
        for (ItemStack s : items) {
            String path = RestaurantMenu.pathOf(s);
            spots.add(path == null ? "" : path);
        }
        components.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), spots, List.of()));
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        ItemContainerContents c = components.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        c.copyInto(items);
        dirty = true;
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("Items");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < SIZE; i++) items.set(i, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        dirty = true;
    }
}
