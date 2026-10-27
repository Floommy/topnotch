package net.togyk.tog_yks_cards.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.togyk.tog_yks_cards.block.custom.MatBlock;
import net.togyk.tog_yks_cards.blockentity.entities.MatBlockEntity;
import net.togyk.tog_yks_cards.mixin.MultiPlayerGameModeAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(value=EnvType.CLIENT)
@Mixin(value={MultiPlayerGameMode.class})
public class MultiPlayerGameModeMixin {
    @Inject(method={"destroyBlock"}, at={@At(value="HEAD")}, cancellable=true)
    public void interceptBlockBreak(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        Minecraft minecraft = ((MultiPlayerGameModeAccessor)(this)).getMinecraft();
        ClientLevel level = minecraft.level;
        BlockState state = level.getBlockState(blockPos);
        Block block = state.getBlock();
        if (block instanceof MatBlock) {
            MatBlockEntity matEntity;
            MatBlock mat = (MatBlock)block;
            BlockEntity blockEntity = level.getBlockEntity(blockPos);
            if (blockEntity instanceof MatBlockEntity && mat.breakItem((Level)level, (Player)minecraft.player, blockPos, state, matEntity = (MatBlockEntity)blockEntity)) {
                cir.setReturnValue(true);
            }
        }
    }
}

