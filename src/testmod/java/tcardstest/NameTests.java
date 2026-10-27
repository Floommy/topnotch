package tcardstest;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Every item, block and entity of TopNotch (incl. food + hooks) has an English name. */
public class NameTests {
    static final Set<String> NS = Set.of("tog_yks-cards", "better_mcdonalds_mod", "yo_hooks");

    static Map<String, String> lang() throws Exception {
        Map<String, String> all = new HashMap<>();
        for (String ns : NS) {
            var urls = NameTests.class.getClassLoader().getResources("assets/" + ns + "/lang/en_us.json");
            while (urls.hasMoreElements()) {
                try (var r = new InputStreamReader(urls.nextElement().openStream(), java.nio.charset.StandardCharsets.UTF_8)) {
                    JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                    for (var e : o.entrySet()) all.put(e.getKey(), e.getValue().getAsString());
                }
            }
        }
        return all;
    }

    static void run() throws Exception {
        Map<String, String> lang = lang();
        Set<String> missing = new TreeSet<>();
        int items = 0, blocks = 0, entities = 0;
        for (var item : BuiltInRegistries.ITEM) {
            var id = BuiltInRegistries.ITEM.getKey(item);
            if (!NS.contains(id.getNamespace())) continue;
            items++;
            String key = new ItemStack(item).getItem().getDescriptionId();
            if (!lang.containsKey(key)) missing.add(key);
        }
        for (var block : BuiltInRegistries.BLOCK) {
            var id = BuiltInRegistries.BLOCK.getKey(block);
            if (!NS.contains(id.getNamespace())) continue;
            blocks++;
            if (!lang.containsKey(block.getDescriptionId())) missing.add(block.getDescriptionId());
        }
        for (var type : BuiltInRegistries.ENTITY_TYPE) {
            var id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (!NS.contains(id.getNamespace())) continue;
            entities++;
            if (!lang.containsKey(type.getDescriptionId())) missing.add(type.getDescriptionId());
        }
        System.out.println("TCARDS-NAMES missing " + missing);
        TestMain.check("names: every item/block/entity has an English name (" + items + " items, " + blocks + " blocks, " + entities + " entities)",
                missing.isEmpty(), missing.size() + " missing");
    }
}
