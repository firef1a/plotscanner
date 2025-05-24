package dev.plotscanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.plotscanner.features.Feature;
import dev.plotscanner.features.FeatureImpl;
import dev.plotscanner.features.Features;
import dev.plotscanner.features.dev.PlotScannerFeature;
import dev.plotscanner.helper.command.CommandQueueHelper;
import dev.plotscanner.utils.ChatUtils;
import dev.dfonline.flint.Flint;
import dev.dfonline.flint.hypercube.Mode;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;


public class Mod implements ClientModInitializer {
	public static final String MOD_NAME = "plotscanner";
	public static final String MOD_ID = "plotscanner";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final MinecraftClient MC = MinecraftClient.getInstance();
	public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	public static int tick = 0;

	public static String MOD_VERSION;

	@Override
	public void onInitializeClient() {
		try {
			Features.init();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		//KeyInputHandler.register();

		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			Features.implement(feature -> {feature.tick(tick);});
			CommandQueueHelper.tick();
			tick++;
		});
		ItemTooltipCallback.EVENT.register(((itemStack, tooltipContext, tooltipType, list) -> Features.implement(feature -> feature.tooltip(itemStack, tooltipContext, tooltipType, list))));
		HudRenderCallback.EVENT.register((draw, tickCounter) -> Features.implement(feature -> feature.renderHUD(draw, tickCounter)));
		WorldRenderEvents.LAST.register(event -> Features.implement(feature -> feature.renderWorld(event)));
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> Features.implement(feature -> feature.clientStart(client)));
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			Features.implement(feature -> feature.clientStop(client));
			Mod.clientStopping();
		});

		ServerPlayConnectionEvents.INIT.register((networkHandler, minecraftServer) -> Features.implement(feature -> feature.serverConnectInit(networkHandler, minecraftServer)));
		ServerPlayConnectionEvents.JOIN.register((event, sender, minecraftServer) -> Features.implement(feature -> feature.serverConnectJoin(event, sender, minecraftServer)));
		ServerPlayConnectionEvents.DISCONNECT.register((networkHandler, minecraftServer) -> Features.implement(feature -> feature.serverConnectDisconnect(networkHandler, minecraftServer)));


		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("dfplotscan").executes(Mod::startScan));
		});



		CommandRegistrationCallback.EVENT.register(((CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) -> {

		}));

		System.setProperty("java.awt.headless", "false");

		MOD_VERSION = FabricLoader.getInstance().getModContainer(MOD_ID).isPresent() ? FabricLoader.getInstance().getModContainer(MOD_ID).get().getMetadata().getVersion().getFriendlyString() : null;

		LOGGER.info("plotscanner startup");
	}


	public static String getPlayerName() { return Mod.MC.getSession().getUsername(); }
	public static UUID getPlayerUUID() { return Mod.MC.getSession().getUuidOrNull(); }

	private static int startScan(CommandContext<FabricClientCommandSource> context) {
		if (Flint.getUser().getMode().equals(Mode.DEV)) {
			PlotScannerFeature.startScan();
		} else {
			ChatUtils.displayMessage(Text.literal("You must be in dev mode to use this."));
		}
		return 1;
	}

	public static void clientStopping() {
		log("stopping");
	}

	public static void log(String msg) { Mod.LOGGER.info(msg); }

}