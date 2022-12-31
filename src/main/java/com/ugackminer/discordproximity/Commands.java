package com.ugackminer.discordproximity;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;


public class Commands {
    
    public static void initalize(CommandDispatcher<FabricClientCommandSource> dispatcher) 
    {
        dispatcher.register(
            literal("discordprox").executes(context -> {
                context.getSource().sendFeedback(Text.literal("Discord Proximity Chat\nVersion " + DiscordProximity.VERSION));
                return Command.SINGLE_SUCCESS;
            }).then(literal("config").executes(context -> {
                DiscordProximity.core.overlayManager().openVoiceSettings();
                return Command.SINGLE_SUCCESS;
            }))
        );
    }

}
