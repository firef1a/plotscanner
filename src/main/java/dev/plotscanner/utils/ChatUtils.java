package dev.plotscanner.utils;

import dev.plotscanner.Mod;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

import java.util.Objects;

public class ChatUtils {
    public static void sendMessage(String content) {
        if (content.charAt(0) == '/') {
            Objects.requireNonNull(Mod.MC.getNetworkHandler()).sendChatCommand(content.substring(1));
        } else {
            Objects.requireNonNull(Mod.MC.getNetworkHandler()).sendChatMessage(content);
        }
    }

    public static void displayMessage(Text content) {
        if (Mod.MC.player != null) {
            Mod.MC.player.sendMessage(
                Text.literal("PSCANNER").withColor(0x61fffa)
                        .append(Text.literal(" » ").withColor(Colors.GRAY))
                        .append(Text.empty().append(content).withColor(Colors.LIGHT_GRAY)),
                false);
        }
    }
}
