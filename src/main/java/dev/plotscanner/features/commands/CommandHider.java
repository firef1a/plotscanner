package dev.plotscanner.features.commands;

import dev.plotscanner.features.Feature;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class CommandHider extends Feature {
    public static ArrayList<String> singleHiderList;

    public CommandHider() {
        init("commandhider", "Automated Command Hider", "Automatically hides internally run commands.");
        singleHiderList = new ArrayList<>();
    }

    public static void addSingleHiddenCommand(CommandEntry text ) { singleHiderList.add(text.command()); }

    @Override
    public void onChatMessage(Text message, CallbackInfo ci) {
        String text = message.getString();

        for (int i = singleHiderList.size()-1; i <= 0; i++) {
            String match = singleHiderList.get(i);
            if (Pattern.compile(match, Pattern.CASE_INSENSITIVE).matcher(text).find()) {
                ci.cancel();
                singleHiderList.remove(i);
                break;
            }
        }
    }
}
