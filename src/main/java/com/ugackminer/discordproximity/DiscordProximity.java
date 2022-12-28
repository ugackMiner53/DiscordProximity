package com.ugackminer.discordproximity;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.lobby.Lobby;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

public class DiscordProximity implements ModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger("discordproximity");
	public static Core core;
	public static Lobby lobby;

	@Override
	public void onInitialize() {
		Optional<Path> test =  FabricLoader.getInstance().getModContainer("discordproximity").map(container -> container.findPath(getSDKString()).orElseThrow());
		LOGGER.info(test.get().toString());
		try {
			Core.init(test.get().toUri().toURL());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		CreateParams params = new CreateParams();
		params.setClientID(Constants.CLIENT_ID);
		params.setFlags(CreateParams.getDefaultFlags());
		core = new Core(params);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			core.runCallbacks();
		});

	}

	private String getSDKString() {
		String name = "discord_game_sdk";
		String suffix;

		String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
		String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);

		if (osName.contains("windows")) {
			suffix = ".dll";
		} else if (osName.contains("linux")) {
			suffix = ".so";
		} else if (osName.contains("mac os")) {
			suffix = ".dylib";
		} else {
			throw new RuntimeException("cannot determine OS type: "+osName);
		}
		if(arch.equals("amd64"))
			arch = "x86_64";

		return "lib/" + arch + "/" + name + suffix;
	}
	
}
