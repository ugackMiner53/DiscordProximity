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
        LOGGER.debug(userId + " updated their metadata.\nTrying to update our uuidmap...");
        if (DiscordProximity.core.lobbyManager().getMemberMetadataValue(DiscordProximity.lobby, userId, "uuid") != null) {
            UUID uuid = UUID.fromString(DiscordProximity.core.lobbyManager().getMemberMetadataValue(DiscordProximity.lobby, userId, "uuid"));
            LOGGER.info("Got UUID! (" + uuid.toString() + ")");
            if (userId != DiscordProximity.currentUserID)
                DiscordProximity.uuidMap.putIfAbsent(uuid, userId);
        }
    }

    @Override
    public void onMemberConnect(long lobbyId, long userId) 
    {
        LOGGER.info("Userid " + userId + " connected");
    }

    @Override
    public void onMemberDisconnect(long lobbyId, long userId) {
        LOGGER.info("UserID " + userId + " disconnected");
        // Remove them from the uuidMap
        DiscordProximity.uuidMap.entrySet().stream().filter(e -> !e.getValue().equals(userId)).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }

}
