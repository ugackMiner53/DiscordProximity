package com.ugackminer.discordproximity;

import java.util.UUID;

public class User {
    public UUID uuid;
    public long discordId;

    public User(UUID uuid, long discordId) 
    {
        this.uuid = uuid;
        this.discordId = discordId;
    }
}
