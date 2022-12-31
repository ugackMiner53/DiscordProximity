package com.ugackminer.discordproximity;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import de.jcm.discordgamesdk.Result;
import de.jcm.discordgamesdk.lobby.Lobby;
import de.jcm.discordgamesdk.lobby.LobbyTransaction;
import de.jcm.discordgamesdk.lobby.LobbyType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;


public class Commands {
    
    public static void initalize(CommandDispatcher<FabricClientCommandSource> dispatcher) 
    {
        dispatcher.register(
            literal("discordprox").executes(context -> {
                context.getSource().sendFeedback(Text.translatable("commands.info", DiscordProximity.VERSION));
                return Command.SINGLE_SUCCESS;
            }).then(literal("config").executes(context -> {
                DiscordProximity.core.overlayManager().openVoiceSettings();
                return Command.SINGLE_SUCCESS;
            })).then(literal("lobby")
                .then(literal("list").executes(context -> {
                    DiscordProximity.searchLobbies(context.getSource().getClient(), lobbies -> {
                        context.getSource().sendFeedback(Text.translatable("commands.lobby.list", context.getSource().getClient().getCurrentServerEntry().address)); 
                        for (Lobby lobby : lobbies) 
                        {
                            DiscordProximity.core.userManager().getUser(lobby.getOwnerId(), (result, user) -> {
                                if (result != Result.OK) {
                                    DiscordProximity.LOGGER.error("Error getting username from ID (Discord response not OK)");
                                    return;
                                }
                                Text text = clickCommandText(Text.translatable("commands.lobby.list.user", user.getUsername(), user.getDiscriminator()), "/discordprox lobby join " + lobby.getId());
                                context.getSource().sendFeedback(text); 
                            });
                        }
                    });
                    return Command.SINGLE_SUCCESS;
                }))
                .then(literal("create")
                    .then(literal("private").executes(context -> {
                        if (DiscordProximity.core.lobbyManager().lobbyCount() > 0)
                            DiscordProximity.leaveLobby();
                        LobbyTransaction transaction = DiscordProximity.core.lobbyManager().getLobbyCreateTransaction();
                        transaction.setType(LobbyType.PRIVATE);
                        transaction.setMetadata("serverip", context.getSource().getClient().getCurrentServerEntry().address);
                        DiscordProximity.core.lobbyManager().createLobby(transaction, (result, lobby) -> {
                            DiscordProximity.lobbyJoined(result, lobby);

                            Text text = Text.translatable("commands.lobby.create.private")
                                .append(clickCopyText(Text.translatable("commands.lobby.create.private.id", lobby.getId()), "" + lobby.getId()))
                                .append(" ")
                                .append(clickCopyText(Text.translatable("commands.lobby.create.private.secret"), lobby.getSecret()));
                            context.getSource().sendFeedback(text);
                        });
                        return Command.SINGLE_SUCCESS;  
                    }))
                    .then(literal("public").executes(context -> {
                        if (DiscordProximity.core.lobbyManager().lobbyCount() > 0)
                            DiscordProximity.leaveLobby();
                        LobbyTransaction transaction = DiscordProximity.core.lobbyManager().getLobbyCreateTransaction();
                        transaction.setType(LobbyType.PUBLIC);
                        transaction.setMetadata("serverip", context.getSource().getClient().getCurrentServerEntry().address);
                        DiscordProximity.core.lobbyManager().createLobby(transaction, (result, lobby) -> {
                            DiscordProximity.lobbyJoined(result, lobby);
                            context.getSource().sendFeedback(Text.translatable("commands.lobby.create.public", lobby.getId()));
                        });
                        return Command.SINGLE_SUCCESS;  
                    }))
                )
                .then(literal("join").then(argument("lobbyid", LongArgumentType.longArg()).executes(context -> {
                    if (DiscordProximity.core.lobbyManager().lobbyCount() > 0)
                        DiscordProximity.leaveLobby();
                    long lobbyId = LongArgumentType.getLong(context, "lobbyid");
                    Lobby lobby = DiscordProximity.core.lobbyManager().getLobby(lobbyId);
                    DiscordProximity.core.lobbyManager().connectLobby(lobby, DiscordProximity::lobbyJoined);
                    DiscordProximity.core.userManager().getUser(lobby.getOwnerId(), (result, user) -> {
                        if (result != Result.OK) {
                            DiscordProximity.LOGGER.error("Could not get username (Discord result not OK)");
                            // return;
                        }
                        context.getSource().sendFeedback(Text.translatable("commands.lobby.join.public", user.getUsername(), user.getDiscriminator(), lobbyId));
                    });
                    return Command.SINGLE_SUCCESS;
                }).then(argument("secret", StringArgumentType.word()).executes(context -> {
                    if (DiscordProximity.core.lobbyManager().lobbyCount() > 0)
                        DiscordProximity.leaveLobby();
                    long lobbyId = LongArgumentType.getLong(context, "lobbyid");
                    String lobbySecret = StringArgumentType.getString(context, "secret");
                    DiscordProximity.core.lobbyManager().connectLobby(lobbyId, lobbySecret, (result, lobby) -> {
                        if (result != Result.OK) {
                            DiscordProximity.LOGGER.error("Could not join lobby (Discord result not OK)");
                            // return;
                        }
                        DiscordProximity.lobbyJoined(result, lobby);
                        DiscordProximity.core.userManager().getUser(lobby.getOwnerId(), (_result, user) -> {
                            if (_result != Result.OK) {
                                DiscordProximity.LOGGER.error("Could not get username (Discord result not OK)");
                                return;
                            }
                            context.getSource().sendFeedback(Text.translatable("commands.lobby.join.private", user.getUsername(), user.getDiscriminator(), lobbyId));
                        });
                    });
                    return Command.SINGLE_SUCCESS;  
                }))))
                .then(literal("leave").executes(context -> {
                    try {
                        DiscordProximity.leaveLobby();
                        context.getSource().sendFeedback(Text.translatable("commands.lobby.leave"));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        context.getSource().sendError(Text.translatable("commands.lobby.leave.error"));
                    }
                    return Command.SINGLE_SUCCESS;
                }))
            )
        );
    }

    public static Text clickCommandText(MutableText text, String command) 
    {
        return text.styled(style -> style.withFormatting(Formatting.UNDERLINE)
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(command))));
    }

    public static Text clickCopyText(MutableText text, String copyThis) 
    {
        return text.styled(style -> style.withFormatting(Formatting.UNDERLINE)
            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, copyThis))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("commands.click.copy"))));
    }

}
