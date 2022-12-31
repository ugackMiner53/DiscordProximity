package com.ugackminer.discordproximity;
import static com.ugackminer.discordproximity.DiscordProximity.LOGGER;

import de.jcm.discordgamesdk.Result;

public class VolumeManager {

    static final double MAX_VOLUME = 200;   // The maximum volume that discord can go up to 
    static final double SCALING_FORCE = 8;  // A constant to scale the volumes


    public static void updateVolume(long discordId, float distance) 
    {
        DiscordProximity.core.voiceManager().setLocalVolume(discordId, calculateVolume(distance));
    }
    
    public static int calculateVolume(float distance) 
    {
        // Calculate the volume
        double volume = MAX_VOLUME;
        if (distance > 0) {
            volume = (volume * 8)/distance;
        }

        // Clamp the volume to be between 0 and 200, while rounding it to a nice integer
        return (int)Math.round(Math.max(0, Math.min(200, volume)));
    }


}
