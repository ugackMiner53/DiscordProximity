package com.ugackminer.discordproximity;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.Result;
import de.jcm.discordgamesdk.lobby.Lobby;
import de.jcm.discordgamesdk.lobby.LobbyMemberTransaction;
import de.jcm.discordgamesdk.lobby.LobbySearchQuery;
import de.jcm.discordgamesdk.lobby.LobbyTransaction;
import de.jcm.discordgamesdk.lobby.LobbyType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.network.listener.ClientPlayPacketListener;

public class DiscordProximity implements ModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger("discordproximity");
	public static final String VERSION = "0.0.0 BETA DEBUG RELEASE (please don't share)";
	public static Core core;
	public static Lobby lobby;

	long currentUserID;

	public static Map<UUID, Long> uuidMap = new LinkedHashMap<UUID, Long>();


/***
 * TODO:
 * - Add players to the uuidMap
 * - Remove players from the uuidMap when they leave
 * - Add proximity to the proximity chat mod
 * - Maybe make some easier way to test this dumb stuff?
 * - Multi-lobby support
 * - (Private lobbies)
 * 
 */


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
		params.registerEventHandler(new DiscordEventListener()); // Setup the listener for network messages
		core = new Core(params);

		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			core.runCallbacks();
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.world != null) {
				List<AbstractClientPlayerEntity> worldPlayers = client.world.getPlayers();
				boolean[] playerFound = new boolean[uuidMap.size()]; 	// This is a better way of keeping track of who's in render distance.
																		// If there's a player in our uuidMap whose index here is false, then they aren't in render distance...
				for (int i=0; i<worldPlayers.size(); i++) {
					AbstractClientPlayerEntity player = worldPlayers.get(i);
					if (uuidMap.containsKey(player.getUuid())) {
						playerFound[i] = true;
						// Update other users volumes
						VolumeManager.updateVolume(uuidMap.get(player.getUuid()), client.player.distanceTo(player));
					}
				}

				for (int i=0; i<playerFound.length; i++) {
					if (!playerFound[i]) {
						core.voiceManager().setLocalVolume(/* We need to get the discord ID using the index here, which doesn't work on hashmaps... */, 0);
					}
				}
			}
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			Commands.initalize(dispatcher);
		});

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
				core.lobbyManager().connectLobby(lobbies.get(0), this::lobbyJoined);
			} else {
				LOGGER.info("No lobby found, so we're creating one...");
				LobbyTransaction transaction = core.lobbyManager().getLobbyCreateTransaction();
				transaction.setType(LobbyType.PUBLIC);
				transaction.setMetadata("serverip", serverIP);
				core.lobbyManager().createLobby(transaction, this::lobbyJoined);
			}
		});
	}

	private void lobbyJoined(Result _result, Lobby _lobby) 
	{
		if (_result != Result.OK) {
			System.out.println("Something went wrong! (Discord response isn't OK)");
			System.out.println(_result.toString());
			return;
		}
		DiscordProximity.lobby = _lobby;
		LOGGER.info("Lobby joined!");
		LOGGER.info("ID: " + lobby.getId());

		currentUserID = core.userManager().getCurrentUser().getUserId();

		LOGGER.info("Setting uuid metadata...");
		LobbyMemberTransaction transaction = core.lobbyManager().getMemberUpdateTransaction(lobby, currentUserID);
		transaction.setMetadata("uuid", MinecraftClient.getInstance().player.getUuidAsString());
		core.lobbyManager().updateMember(lobby, currentUserID, transaction, result -> {
			if (result != Result.OK) {
				LOGGER.error("Error setting uuid (Discord response not OK)");
			}
		});


		// LOGGER.info("Connecting to networking...");
		// core.lobbyManager().connectNetwork(lobby);
        // core.lobbyManager().openNetworkChannel(DiscordProximity.lobby, (byte)0, true);


		LOGGER.info("Connecting to voice...");
		core.lobbyManager().connectVoice(lobby, result -> {
			if (result != Result.OK) {
				LOGGER.error("Error connecting to voice! (Response not OK)");
			} else {
				LOGGER.info("Connected to voice!");
			}
		});

		for (int i=0; i<core.lobbyManager().memberCount(lobby); i++) {
			long userID = core.lobbyManager().getMemberUserId(lobby, i);
			if (userID != currentUserID) {
				LOGGER.info("Getting uuid for " + userID);
				UUID uuid = UUID.fromString(core.lobbyManager().getMemberMetadataValue(lobby, userID, "uuid"));
				LOGGER.info("Got UUID! (" + uuid.toString() + ")");
				core.voiceManager().setLocalVolume(userID, 0); // Mute them when they join (we can unmute them if they're rendered)
				uuidMap.put(uuid, userID); // Add it to the map!
			}
		}

		// LOGGER.info("Asking network users for minecraft uuids...");	
		// core.lobbyManager().sendLobbyMessage(lobby, "lobbytest".getBytes()); // this is a test event
		// for (int i=0; i<core.lobbyManager().memberCount(lobby); i++) {
		// 	long userID = core.lobbyManager().getMemberUserId(lobby, i);
		// 	LOGGER.info("Asking user " + i + " (" + userID + ")");
		// 	core.lobbyManager().sendNetworkMessage(lobby, userID, (byte)0, "requestuuid".getBytes());
		// }
		
	}

	private void leaveLobby(ClientPlayPacketListener handler, MinecraftClient client) {
		// LOGGER.info("Disconnecting from network...");
		// core.lobbyManager().disconnectNetwork(lobby);

		LOGGER.info("Disconnecting from voice...");
		core.lobbyManager().disconnectVoice(lobby, result -> {
			if (result != Result.OK) {
				LOGGER.error("Error disconnecting from voice channel (Discord Response not OK)");
			} else {
				LOGGER.info("Disconnected from voice!");
			}
		});

		if (lobby.getOwnerId() == currentUserID) {
			if (core.lobbyManager().getMemberUsers(lobby).size() > 1) {
				// Transfer the ownership of the lobby if there are other people in it
				LOGGER.info("Transferring the lobby ownership...");
				LobbyTransaction transaction = core.lobbyManager().getLobbyUpdateTransaction(lobby);
				transaction.setOwner(core.lobbyManager().getMemberUsers(lobby).get(1).getUserId());
				core.lobbyManager().updateLobby(lobby, transaction, res -> {
					if (res != Result.OK) {
						LOGGER.error("Error transferring lobby to " + core.lobbyManager().getMemberUsers(lobby).get(1).getUserId() + "(Discord Response not OK)");
					}
				});
			} else {
				// If there are no other people, just delete the lobby
				LOGGER.info("Deleting the lobby...");
				core.lobbyManager().deleteLobby(lobby, result -> {
					if (result != Result.OK) {
						LOGGER.error("Error deleting lobby (Discord Response not OK)");
					}
				});
			}
		} else {
			// If we aren't the owner, then just disconnect from the lobby
			LOGGER.info("Disconnecting from the lobby...");
			core.lobbyManager().disconnectLobby(lobby, result -> {
				if (result != Result.OK) {
					LOGGER.error("Error disconnecting from lobby (Discord Response not OK)");
				} else {
					LOGGER.info("Disconnected from lobby!");
				}
			});
		}

		// Remember to delete the uuidMap once it isn't needed anymore!
		uuidMap.clear();
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
