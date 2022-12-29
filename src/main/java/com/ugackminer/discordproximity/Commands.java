package com.ugackminer.discordproximity;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SentMessage;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class Commands {
    
    public static void initalize() 
    {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("foo").executes(context -> {
            context.getSource().sendMessage(Text.literal("Called /foo with no arguments"));
            // Stolen directly from MessageCommand class
            String username = MinecraftClient.getInstance().player.getEntityName();
            long exampleID = 560549175457087488L;
            String command = String.format("/w %s %d", username, exampleID)
            MinecraftClient.getInstance().player.networkHandler.sendChatCommand();
            /*
            MessageType.Parameters parameters = MessageType.params(MessageType.MSG_COMMAND_INCOMING, source);
            SentMessage sentMessage = SentMessage.of(message);
            boolean bl = false;
            for (ServerPlayerEntity serverPlayerEntity : targets) {
                MessageType.Parameters parameters2 = MessageType.params(MessageType.MSG_COMMAND_OUTGOING, source).withTargetName(serverPlayerEntity.getDisplayName());
                source.sendChatMessage(sentMessage, false, parameters2);
                boolean bl2 = source.shouldFilterText(serverPlayerEntity);
                serverPlayerEntity.sendChatMessage(sentMessage, bl2, parameters);
                bl |= bl2 && message.isFullyFiltered();
            }
            if (bl) {
                source.sendMessage(PlayerManager.FILTERED_FULL_TEXT);
            }
            */
            return 1;
        })));
    }

}
