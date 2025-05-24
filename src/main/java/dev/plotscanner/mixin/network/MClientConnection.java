package dev.plotscanner.mixin.network;

import dev.plotscanner.features.Features;
import dev.plotscanner.helper.command.CommandQueueHelper;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(ClientConnection.class)
public class MClientConnection {
    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private static <T extends PacketListener> void handlePacket(Packet<T> packet, net.minecraft.network.listener.PacketListener listener, CallbackInfo ci) {
        if (packet instanceof GameMessageS2CPacket(Text content, boolean overlay)) {
            if (content.getString().equals("◆ Welcome back to DiamondFire! ◆")) { CommandQueueHelper.addCurrentTimestamp(1500L); }
            Features.implement(feature -> feature.onChatMessage(content, ci));
        }
        //Mod.log(packet.getPacketId().toString());
        Features.implement(feature -> {
            try {
                feature.handlePacket(packet, ci);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @ModifyVariable(method = "handlePacket", at = @At("HEAD"), argsOnly = true)
    private static <T extends PacketListener> Packet<T> handlePacket(Packet<T> packet) {
        if (packet instanceof GameMessageS2CPacket(Text content, boolean overlay)) {
            Text new_context = Features.editChatMessage(content);
            return (Packet<T>) new GameMessageS2CPacket(new_context, overlay);
        }
        return packet;
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;Z)V", at = @At("HEAD"), cancellable = true)
    private <T extends PacketListener> void handlePacket(Packet<?> packet, @Nullable PacketCallbacks callbacks, boolean flush, CallbackInfo ci) {
        Features.implement(feature -> feature.sendPacket(packet, ci));
    }
}