package net.togyk.tog_yks_cards.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.togyk.tog_yks_cards.block.custom.MatBlock;
import net.togyk.tog_yks_cards.blockentity.entities.MatBlockEntity;
import net.togyk.tog_yks_cards.mixin.ServerPlayerGameModeAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={ServerPlayerGameMode.class})
public class ServerPlayerGameModeMixin {
    @Inject(method={"destroyBlock"}, at={@At(value="HEAD")}, cancellable=true)
    public void interceptBlockBreak(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerGameModeAccessor accessor = (ServerPlayerGameModeAccessor)(this);
        ServerLevel level = accessor.getLevel();
        BlockState state = level.getBlockState(blockPos);
        Block block = state.getBlock();
        if (block instanceof MatBlock) {
            MatBlock mat = (MatBlock)block;
            BlockEntity blockEntity = level.getBlockEntity(blockPos);
            if (blockEntity instanceof MatBlockEntity) {
                MatBlockEntity matEntity = (MatBlockEntity)blockEntity;
                if (mat.breakItem((Level)level, (Player)accessor.getPlayer(), blockPos, state, matEntity)) {
                    cir.setReturnValue(true);
                }
            }
        }
    }
}

