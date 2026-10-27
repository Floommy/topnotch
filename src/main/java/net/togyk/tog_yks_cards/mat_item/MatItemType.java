package net.togyk.tog_yks_cards.mat_item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Locale;
import net.togyk.tog_yks_cards.mat_item.CardBoxMatItem;
import net.togyk.tog_yks_cards.mat_item.CardHolderMatItem;
import net.togyk.tog_yks_cards.mat_item.CardPileMatItem;
import net.togyk.tog_yks_cards.mat_item.CoinPileMatItem;
import net.togyk.tog_yks_cards.mat_item.MatItem;
import net.togyk.tog_yks_cards.mat_item.SimpleMatItem;

public enum MatItemType {
    SIMPLE(SimpleMatItem.CODEC),
    CARD_HOLDER(CardHolderMatItem.CODEC),
    CARD_PILE(CardPileMatItem.CODEC),
    CARD_BOX(CardBoxMatItem.CODEC),
    COIN_PILE(CoinPileMatItem.CODEC);

    public static final Codec<MatItemType> CODEC;
    private final MapCodec<? extends MatItem> codec;

    private MatItemType(MapCodec<? extends MatItem> codec) {
        this.codec = codec;
    }

    public MapCodec<? extends MatItem> getCodec() {
        return this.codec;
    }

    static {
        CODEC = Codec.STRING.xmap(s -> MatItemType.valueOf(s.toUpperCase(Locale.ROOT)), type -> type.name().toLowerCase(Locale.ROOT));
    }
}

