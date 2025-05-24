package dev.plotscanner.features.dev;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import dev.dfonline.flint.Flint;
import dev.dfonline.flint.hypercube.Plot;
import dev.plotscanner.FileManager;
import dev.plotscanner.Mod;
import dev.plotscanner.features.Feature;
import dev.plotscanner.features.commands.CommandEntry;
import dev.plotscanner.helper.command.CommandQueueHelper;
import dev.plotscanner.utils.Base64Utils;
import dev.plotscanner.utils.ChatUtils;
import dev.plotscanner.utils.GzipUtils;
import dev.dfonline.flint.FlintAPI;
import dev.plotscanner.utils.MathUtils;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.io.*;
import java.util.*;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;

import static java.util.Map.entry;

public class PlotScannerFeature extends Feature {
    private static RequestingMode requestingMode = RequestingMode.NONE;
    private static Plot plot;

    private static ArrayList<String> rawTemplateData;
    private static ArrayList<String> jsonTemplateData;

    private static long lastTeleportTimestamp;
    private static int totalCodeScan;

    private static ArrayList<String> ctpResult;
    private static ArrayList<String> completedCTP;

    private static boolean shouldGrab = false;

    public static final Map<Integer, String> idMap = new HashMap<>(Map.ofEntries(
            entry(5000001, "event"),
            entry(5000002, "function"),
            entry(5000003, "process")
    ));

    public PlotScannerFeature() throws IOException {
        init("plotscanner", "scan in your plot just like in pantheon", "powers your plot with vibes");
        this.isEnabled = true;
    }

    public static void startScan() {
        FlintAPI.confirmLocationWithLocate();
        if (Mod.MC.getNetworkHandler() == null) return;
        ChatUtils.displayMessage(Text.literal("Starting scan of DF Plot ID: " + Flint.getUser().getPlot().getId()));

        requestingMode = RequestingMode.REQUESTING;
        rawTemplateData = new ArrayList<>();
        jsonTemplateData = new ArrayList<>();
        ctpResult = new ArrayList<>();
        completedCTP = new ArrayList<>();

        totalCodeScan = 0;
        shouldGrab = false;

        ArrayList<RequestCommandCompletionsC2SPacket> requestCommandCompletionsC2SPackets = new ArrayList<>(List.of(
                new RequestCommandCompletionsC2SPacket(5000001, "/ctp event "),
                new RequestCommandCompletionsC2SPacket(5000002, "/ctp function "),
                new RequestCommandCompletionsC2SPacket(5000003, "/ctp process ")
        ));
        for (RequestCommandCompletionsC2SPacket p : requestCommandCompletionsC2SPackets) { Mod.MC.getNetworkHandler().sendPacket(p); }
    }

    public void sendPacket(Packet<?> packet, CallbackInfo ci) {
        if (Mod.MC.getNetworkHandler() == null) return;
        if (packet instanceof TeleportConfirmC2SPacket) {
            if (requestingMode.equals(RequestingMode.GRABBING)) {
                shouldGrab = true;
            }
        }
    }

    @Override
    public void handlePacket(Packet<?> packet, CallbackInfo ci) throws IOException {
        if (requestingMode.equals(RequestingMode.REQUESTING)) {
            if (packet instanceof CommandSuggestionsS2CPacket(int id, int start, int length, List<CommandSuggestionsS2CPacket.Suggestion> suggestions)) {
                if (idMap.containsKey(id)) {
                    String eventId = idMap.get(id);
                    Mod.log(eventId);

                    for (CommandSuggestionsS2CPacket.Suggestion suggestion : suggestions) {
                        String text = "/ctp " + eventId + " " + suggestion.text();
                        Mod.log(suggestion.text());
                        ctpResult.add(text);
                        completedCTP.add(eventId);
                    }

                    if (completedCTP.contains("event") && completedCTP.contains("function") && completedCTP.contains("process")) {
                        ChatUtils.displayMessage(Text.literal("Plot scan started for " + ctpResult.size() + " code lines."));
                        requestingMode = RequestingMode.GRABBING;
                        totalCodeScan = 0;

                        long totalDelay = 0L;
                        for (String command : ctpResult) {
                            CommandEntry entry = new CommandEntry(command, 1000L);
                            CommandQueueHelper.addCommand(entry);
                            totalDelay += entry.delay();
                        }

                        ChatUtils.displayMessage(Text.literal("This will take an estimated: " + MathUtils.roundToDecimalPlaces((double)(totalDelay) / (1000L), 2) + "s"));


                        totalCodeScan = ctpResult.size();
                        ctpResult = new ArrayList<>();
                        completedCTP = new ArrayList<>();
                    }
                }
            }
        }

        if (Mod.MC.getNetworkHandler() != null && Mod.MC.player != null && requestingMode.equals(RequestingMode.GRABBING)) {
            if (packet instanceof ScreenHandlerSlotUpdateS2CPacket slot) {
                ItemStack itemStack = slot.getStack();
                NbtCompound nbt = encodeStack(itemStack, NbtOps.INSTANCE);
                //Mod.log(nbt.toString());

                NbtCompound mcData = nbt.getCompound("minecraft:custom_data");
                NbtCompound bukkitValues = mcData.getCompound("PublicBukkitValues");
                if (bukkitValues != null) {
                    Set<String> keys = bukkitValues.getKeys();
                    for (String key : keys) {
                        if (key.equals("hypercube:codetemplatedata")) {
                            NbtElement element = bukkitValues.get(key);
                            assert element != null;
                            String value = element.toString();
                            String pre = ",\"version\":1,\"code\":";

                            String codedData = value.substring(value.indexOf(pre)+pre.length()+1, value.length()-3);
                            String decodedData = GzipUtils.decompress(Base64Utils.decodeBase64Bytes(codedData));

                            if (!jsonTemplateData.contains(decodedData)) {
                                jsonTemplateData.add(decodedData);
                                rawTemplateData.add(codedData);

                                ChatUtils.displayMessage(Text.literal("Grabbed " + jsonTemplateData.size() + "/" + totalCodeScan));

                                Mod.MC.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(slot.getSlot(), ItemStack.EMPTY));

                                if (jsonTemplateData.size() >= totalCodeScan) {
                                    requestingMode = RequestingMode.NONE;

                                    ChatUtils.displayMessage(Text.literal("Finished scanning " + jsonTemplateData.size() + "/" + totalCodeScan + " lines of code."));

                                    String path;
                                    String folder = "plotscan";

                                    FileManager.createFolder(folder);

                                    path = folder + "/" + plot.getId() + "_source.txt";
                                    String content = "";
                                    int i = 0;
                                    for (String v : rawTemplateData) {
                                        content += v;
                                        if (i != rawTemplateData.size()-1) {
                                            content += "\n";
                                        }
                                        i++;
                                    }
                                    FileManager.writeFile(FileManager.getPath(path), content);
                                    ChatUtils.displayMessage(Text.literal("Saving to " + path));

                                    path = folder + "/" + plot.getId() + ".txt";
                                    content = "";
                                    i = 0;
                                    for (String v : jsonTemplateData) {
                                        content += v;
                                        if (i != jsonTemplateData.size()-1) {
                                            content += "\n";
                                        }
                                        i++;
                                    }
                                    FileManager.writeFile(FileManager.getPath(path), content);
                                    ChatUtils.displayMessage(Text.literal("Saving to " + path));
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private static NbtCompound encodeStack(ItemStack stack, DynamicOps<NbtElement> ops) {
        DataResult<NbtElement> result = ComponentChanges.CODEC.encodeStart(ops, stack.getComponentChanges());
        NbtElement nbtElement = result.getOrThrow();
        return (NbtCompound) nbtElement;
    }

    @Override
    public void tick(int tick) {
        if (Mod.MC.player == null) return;
        if (Mod.MC.getNetworkHandler() == null) return;
        plot = Flint.getUser().getPlot();
        if (plot == null) {
            FlintAPI.confirmLocationWithLocate();
        }

        if (requestingMode.equals(RequestingMode.GRABBING)) {
            if (shouldGrab) {
                Vec3d mcPos = Mod.MC.player.getPos();
                Vec3d pos = new Vec3d(mcPos.x, (mcPos.y-2.0), mcPos.z);
                BlockHitResult hit = new BlockHitResult(pos, Direction.WEST, new BlockPos(new Vec3i((int) pos.x, (int) pos.y, (int) pos.z)), false);

                Mod.MC.getNetworkHandler().sendPacket((new ClientCommandC2SPacket(Mod.MC.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY)));
                Mod.MC.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hit, 10));
                Mod.MC.getNetworkHandler().sendPacket((new ClientCommandC2SPacket(Mod.MC.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY)));
                shouldGrab = false;
            }
        }
    }
}
