package net.togyk.tog_yks_cards.mat_item;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.togyk.tog_yks_cards.block.custom.MatBlock;
import net.togyk.tog_yks_cards.blockentity.entities.MatBlockEntity;
import net.togyk.tog_yks_cards.mat_item.MatItemType;

public abstract class MatItem {
    public static Codec<MatItem> DISPATCH_CODEC = MatItemType.CODEC.dispatch(MatItem::getType, MatItemType::getCodec);
    private BlockState blockState;
    private int x;
    private int z;
    public MatBlockEntity mat;

    public MatItem(BlockState state, int x, int z) {
        this.blockState = state;
        this.setX(x);
        this.setZ(z);
    }

    public abstract MatItemType getType();

    public abstract List<ItemStack> getDrops();

    public BlockState getState() {
        return this.blockState;
    }

    public void setState(BlockState state) {
        this.blockState = state;
    }

    public int getX() {
        return this.x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getZ() {
        return this.z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public VoxelShape getShape() {
        return this.blockState.getShape((BlockGetter)EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    public VoxelShape getRelativeShape() {
        return this.getShape().move((double)this.getX() / 16.0, 0.0625, (double)this.getZ() / 16.0);
    }

    public VoxelShape getRelativeShape(BlockPos pos) {
        return this.getAbsoluteShape().move((Vec3i)pos.multiply(-1));
    }

    public VoxelShape getAbsoluteShape() {
        if (this.getMat() == null) {
            return null;
        }
        BlockPos pos = this.getMat().getBlockPos();
        return this.getShape().move((Vec3i)pos).move((double)this.getX() / 16.0, 0.0625, (double)this.getZ() / 16.0);
    }

    public void setChanged() {
        if (this.mat != null) {
            this.mat.setChanged();
        }
        this.updateState();
    }

    public void updateState() {
    }

    public void setMat(MatBlockEntity mat) {
        this.mat = mat;
    }

    public MatBlockEntity getMat() {
        return this.mat;
    }

    public boolean canOpenMenu(Player player) {
        return false;
    }

    public boolean isHit(Vec3 hit) {
        AABB box = this.getAbsoluteShape().bounds();
        return hit.x >= box.minX - 1.0E-7 && hit.x <= box.maxX + 1.0E-7 && hit.y >= box.minY - 1.0E-7 && hit.y <= box.maxY + 1.0E-7 && hit.z >= box.minZ - 1.0E-7 && hit.z <= box.maxZ + 1.0E-7;
    }

    public InteractionResult interact(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult blockHitResult) {
        return InteractionResult.PASS;
    }

    public MatItem getRenderState(Player player) {
        return this;
    }

    public List<BlockPos> getOverhangingBlocks() {
        ArrayList<BlockPos> overhanging = new ArrayList<BlockPos>();
        if (this.getMat() != null) {
            boolean ZP;
            BlockPos pos = this.getMat().getBlockPos();
            int x = this.getX();
            int z = this.getZ();
            AABB box = this.getShape().bounds();
            boolean XN = (double)x - box.getXsize() * 8.0 < -8.0;
            boolean XP = (double)x + box.getXsize() * 8.0 > 8.0;
            boolean ZN = (double)z - box.getZsize() * 8.0 < -8.0;
            boolean bl = ZP = (double)z + box.getZsize() * 8.0 > 8.0;
            if (XN) {
                overhanging.add(pos.west());
                if (ZN) {
                    overhanging.add(pos.west().north());
                }
                if (ZP) {
                    overhanging.add(pos.west().south());
                }
            }
            if (XP) {
                overhanging.add(pos.east());
                if (ZN) {
                    overhanging.add(pos.east().north());
                }
                if (ZP) {
                    overhanging.add(pos.east().south());
                }
            }
            if (ZN) {
                overhanging.add(pos.north());
            }
            if (ZP) {
                overhanging.add(pos.south());
            }
        }
        return overhanging;
    }

    public boolean overHangs(BlockPos pos) {
        return this.getOverhangingBlocks().contains(pos);
    }

    public boolean isFullySupported() {
        if (this.getMat() == null) {
            return false;
        }
        Level level = this.getMat().getLevel();
        if (level == null) {
            return false;
        }
        for (BlockPos pos : this.getOverhangingBlocks()) {
            if (level.getBlockState(pos).getBlock() instanceof MatBlock) continue;
            return false;
        }
        return true;
    }
}

