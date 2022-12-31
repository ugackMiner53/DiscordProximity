package com.ugackminer.discordproximity;
import static  com.ugackminer.discordproximity.DiscordProximity.LOGGER;

import java.util.UUID;
import java.util.stream.Collectors;

import de.jcm.discordgamesdk.DiscordEventAdapter;

public class DiscordEventListener extends DiscordEventAdapter 
{

    @Override
    public void onMemberUpdate(long lobbyId, long userId) 
    {
        // TODO: Add a check here to make sure that the metadata updated was actually the uuid and not just some random thing
        LOGGER.info(userId + " updated their metadata.\nTrying to update our uuidmap...");
        UUID uuid = UUID.fromString(DiscordProximity.core.lobbyManager().getMemberMetadataValue(DiscordProximity.lobby, userId, "uuid"));
        LOGGER.info("Got UUID! (" + uuid.toString() + ")");
        DiscordProximity.uuidMap.putIfAbsent(uuid, userId);

        // Set their volume to zero here (if they come into render distance, it will be updated later)
        DiscordProximity.core.voiceManager().setLocalVolume(userId, 0);
    }

    @Override
    public void onMemberConnect(long lobbyId, long userId) 
    {
        LOGGER.info("Userid " + userId + " connected");
    }

    @Override
    public void onMemberDisconnect(long lobbyId, long userId) {
        LOGGER.info("Userid " + userId + " disconnected");
        // Attempt to remove them from the uuidMap
        DiscordProximity.uuidMap.entrySet().stream().filter(e -> !e.getValue().equals(userId)).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }

}
