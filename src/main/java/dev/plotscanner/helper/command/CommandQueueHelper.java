package dev.plotscanner.helper.command;

import dev.plotscanner.Mod;
import dev.plotscanner.features.commands.CommandHider;
import dev.plotscanner.utils.ChatUtils;
import dev.plotscanner.Mod;
import dev.plotscanner.features.commands.CommandEntry;
import dev.plotscanner.utils.ChatUtils;

import java.util.ArrayList;

public class CommandQueueHelper {
    public static ArrayList<CommandEntry> commandQueue = new ArrayList<>();
    private static long nextTimestamp = -1;

    public static void setTimestamp(long timestamp) { nextTimestamp = timestamp; }
    public static void addCurrentTimestamp(long addTimestamp) { nextTimestamp = System.currentTimeMillis() + addTimestamp; }

    public static void addCommand(CommandEntry command) {
        commandQueue.add(command);
    }


    public static void tick() {
        long currentTimestamp = System.currentTimeMillis();
        if (currentTimestamp > nextTimestamp && nextTimestamp != -1 && !commandQueue.isEmpty() && Mod.MC.getNetworkHandler() != null) {
            CommandEntry entry = commandQueue.removeFirst();
            ChatUtils.sendMessage(entry.command());
            nextTimestamp = currentTimestamp + entry.delay();
        }
    }
}
