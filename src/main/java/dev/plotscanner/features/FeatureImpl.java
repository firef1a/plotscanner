package dev.plotscanner.features;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.List;

public interface FeatureImpl {
    boolean isEnabled();
    void setIsEnabled(boolean enabled);

    String getFeatureID();
    String getFeatureName();
    void tick(int tick);
    void tooltip(ItemStack item, Item.TooltipContext context, TooltipType type, List<Text> textList);
    void renderWorld(WorldRenderContext worldRenderContext);

    void renderHUD(DrawContext context, RenderTickCounter tickCounter);
    void handlePacket(Packet<?> packet, CallbackInfo ci) throws IOException;
    void sendPacket(Packet<?> packet, CallbackInfo ci);

    Text modifyChatMessage(Text base, Text modified);
    void onChatMessage(Text message, CallbackInfo ci);
    void clientStart(MinecraftClient minecraftClient);
    void clientStop(MinecraftClient minecraftClient);

    void serverConnectInit(ServerPlayNetworkHandler networkHandler, MinecraftServer minecraftServer);
    void serverConnectJoin(ServerPlayNetworkHandler networkHandler, PacketSender sender, MinecraftServer minecraftServer);
    void serverConnectDisconnect(ServerPlayNetworkHandler networkHandler, MinecraftServer minecraftServer);

    void saveConfig(JsonObject jsonObject);
}
