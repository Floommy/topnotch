package net.togyk.tcards.mixin;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.togyk.tog_yks_cards.blockentity.entities.CardPileBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The original pile saves cards with a byte slot (max 255), so a 6-deck shoe (312 cards) lost cards on every save.
 * Cards are now stored as a plain list under "Cards"; the old "Items" format is still read.
 */
@Mixin(CardPileBlockEntity.class)
public abstract class CardPileBlockEntityMixin {
    @Shadow private List<ItemStack> cards;
    @Shadow private boolean faceDown;

    @Inject(method = "saveAdditional", at = @At("HEAD"), cancellable = true)
    private void tcards$saveAllCards(ValueOutput output, CallbackInfo ci) {
        ValueOutput.TypedOutputList<ItemStack> list = output.list("Cards", ItemStack.CODEC);
        for (ItemStack card : this.cards) {
            if (!card.isEmpty()) list.add(card);
        }
        if (list.isEmpty()) output.discard("Cards");
        output.store("face_down", Codec.BOOL, this.faceDown);
        ci.cancel();
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void tcards$loadAllCards(ValueInput input, CallbackInfo ci) {
        input.list("Cards", ItemStack.CODEC).ifPresent(list -> {
            List<ItemStack> loaded = new ArrayList<>();
            for (ItemStack card : list) loaded.add(card);
            this.cards = loaded;
        });
    }
}
