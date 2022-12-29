package com.ugackminer.discordproximity;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.Result;
import de.jcm.discordgamesdk.lobby.Lobby;
import de.jcm.discordgamesdk.lobby.LobbySearchQuery;
import de.jcm.discordgamesdk.lobby.LobbyTransaction;
import de.jcm.discordgamesdk.lobby.LobbyType;
import de.jcm.discordgamesdk.user.DiscordUser;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.listener.ClientPlayPacketListener;

public class DiscordProximity implements ModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger("discordproximity");
	public static Core core;
	public static Lobby lobby;

	public static Map<String, Long> usernameMap;

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

		// Commands.initalize();

		ClientPlayConnectionEvents.JOIN.register(this::setupLobbies);
		ClientPlayConnectionEvents.DISCONNECT.register(this::leaveLobby);
	}

	private void setupLobbies(ClientPlayPacketListener handler, PacketSender sender, MinecraftClient client) 
	{
		String serverIP = client.getCurrentServerEntry().address;
		LOGGER.info("Checking for lobbies on " + serverIP);
		LobbySearchQuery query = core.lobbyManager().getSearchQuery();
		query.filter("metadata.serverip", LobbySearchQuery.Comparison.EQUAL, LobbySearchQuery.Cast.STRING, serverIP);
		core.lobbyManager().search(query, result -> {
			if (result != Result.OK) {
				LOGGER.error("Error searching for lobbies (Discord response not OK)");
				// Close the listener here?
				return;
			}
			List<Lobby> lobbies = core.lobbyManager().getLobbies();
			if (lobbies.size() > 0) {
				ListIterator<Lobby> lobbiesLoop = lobbies.listIterator();
				while (lobbiesLoop.hasNext()) {
					LOGGER.info(lobbiesLoop.nextIndex() + ": " + lobbiesLoop.next().getOwnerId());
				}
				LOGGER.info("Connecting to the first lobby (Currently there's no system for duplicate lobbies...)");
				core.lobbyManager().connectLobby(lobbies.get(0), this::handleVoice);
			} else {
				LOGGER.info("No lobby found, so we're creating one...");
				LobbyTransaction transaction = core.lobbyManager().getLobbyCreateTransaction();
				transaction.setType(LobbyType.PUBLIC);
				transaction.setMetadata("serverip", serverIP);
				core.lobbyManager().createLobby(transaction, this::handleVoice);
			}
		});
	}

	private void handleVoice(Result result, Lobby lobby) 
	{
		if (result != Result.OK) {
			System.out.println("Something went wrong! (Discord response isn't OK)");
			System.out.println(result.toString());
			return;
		}
		DiscordProximity.lobby = lobby;
		LOGGER.info("Lobby joined!");
		LOGGER.info("ID: " + lobby.getId());
		
		ListIterator<DiscordUser> lobbyUsers = core.lobbyManager().getMemberUsers(lobby).listIterator();
		while (lobbyUsers.hasNext()) {
			LOGGER.info("User " + lobbyUsers.nextIndex() + ": " + lobbyUsers.next().getUsername());
		}
		
		

		System.out.println("Connecting to voice...");
		core.lobbyManager().connectVoice(lobby, _result -> {
			if (result != Result.OK) {
				LOGGER.error("Error connecting to voice! (Response not OK)");
			} else {
				LOGGER.info("Connected to voice!");
				core.overlayManager().openVoiceSettings(); 	// This is a DEBUG LINE in order to show that it is, in fact, connected to discord.
															// Either remove this or change it to be in the mod config settings.
			}
		});
	}

	private void leaveLobby(ClientPlayPacketListener handler, MinecraftClient client) {
		LOGGER.info("Disconnecting from voice and lobby...");
		core.lobbyManager().disconnectVoice(lobby, res -> {
			if (res != Result.OK) {
				LOGGER.error("Error disconnecting from voice channel (Discord Response not OK)");
			} else {
				LOGGER.info("Disconnected from voice!");
			}
		});
		core.lobbyManager().disconnectLobby(lobby, res -> {
			if (res != Result.OK) {
				LOGGER.error("Error disconnecting from lobby (Discord Response not OK)");
			} else {
				LOGGER.info("Disconnected from lobby!");
			}
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
