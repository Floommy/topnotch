# TopNotch

A Fabric mod for Minecraft 26.3 that bundles everything our server plays with into one jar:

- **Cards & Blackjack**: card boxes, piles, holders and game mats, Blackjack table mat, card shuffler, dealer bell, gestures
- **Casino**: ATMs (2 blocks tall), central banks per casino, chips and coins with casino marks, Tom's Storage support
- **Restaurant**: order kiosk with receipt printer, cash register, kitchen / preparing / ready screens, serving trays (carried like a map), tray rack and waste station
- **Food**: burgers, fries, drinks, McFlurry, crops (no Balm needed, salt is crafted, nothing from structure chests)
- **Grappling hooks**: copper to netherite (no YACL needed, nothing from structure chests)

## Building

```
./gradlew build
```

The jar ends up in `build/libs/`. Every push is built by GitHub Actions; tags like `v1.0.0` create a release.

## Updating to a new Minecraft version

1. Update `minecraft_version`, `loader_version`, `loom_version` and `fabric_api_version` in `gradle.properties` (values: https://fabricmc.net/develop).
2. Run `./gradlew build` and fix what Minecraft renamed.
3. Test on a server and in game.

## Credits and licenses

TopNotch is licensed under **GPL-3.0-or-later** (see `LICENSE`). It contains work from:

| Part | Original | License |
|---|---|---|
| Cards (`net.togyk.tog_yks_cards`) | tog_yk's cards by Tog_yk! | CC0-1.0 (`licenses/LICENSE-tog_yks-cards.txt`) |
| Food (`net.togyk.tcards.food`, `better_mcdonalds_mod` assets/data) | [Better McDonald's Mod](https://github.com/moritz-htk/better-mcdonalds-mod) by Kyronis and contributors | GPL-3.0 |
| Grappling hooks (`com.yori3o.yo_hooks`) | [Yori3o's Grappling Hooks](https://github.com/yori3o/Yori3osGrapplingHooks) by Yori3o | MIT (`licenses/LICENSE-YoHooks-MIT.txt`) |

Changes against the originals: built into one mod, Balm and YACL removed, salt no longer generates in the world, no food or hooks in structure chests, one creative tab.
