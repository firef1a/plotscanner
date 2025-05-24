package dev.plotscanner.features;

import dev.plotscanner.features.commands.CommandHider;
import dev.plotscanner.features.dev.PlotScannerFeature;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Features {
    public static Map<String, Feature> featureMap = new HashMap<>();

    public static void init() throws IOException {
        // general
        add(new PlotScannerFeature());
        add(new CommandHider());

    }

    private static void add(Feature feature) {
        featureMap.put(feature.getFeatureID(), feature);
    }

    public static void implement(Consumer<FeatureImpl> consumer) { featureMap.values().forEach((feature -> {if (feature.isEnabled()) consumer.accept(feature);})); }
    public static Text editChatMessage(Text base) {
        Text modified = base;
        for (Feature feature : featureMap.values()) {
            if (feature.isEnabled()) {
                modified = feature.modifyChatMessage(base, modified);
            }
        }
        return modified;
    }
}
