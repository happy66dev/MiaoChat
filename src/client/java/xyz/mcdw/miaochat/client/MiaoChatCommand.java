package xyz.mcdw.miaochat.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class MiaoChatCommand {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(MiaoChatCommand::registerCommands);
    }

    private static void registerCommands(
            CommandDispatcher<FabricClientCommandSource> dispatcher,
            CommandRegistryAccess registryAccess
    ) {
        dispatcher.register(
                ClientCommandManager.literal("miaochat")
                        .then(ClientCommandManager.literal("mode")
                                // /miaochat mode ai
                                .then(ClientCommandManager.literal("ai")
                                        .executes(ctx -> setMode(ctx, ChatMode.AI)))
                                // /miaochat mode normal
                                .then(ClientCommandManager.literal("normal")
                                        .executes(ctx -> setMode(ctx, ChatMode.NORMAL)))
                                // /miaochat mode none
                                .then(ClientCommandManager.literal("none")
                                        .executes(ctx -> setMode(ctx, ChatMode.NONE)))
                                // /miaochat mode <string> (带提示)
                                .then(ClientCommandManager.argument("mode", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String modeStr = StringArgumentType.getString(ctx, "mode");
                                            ChatMode mode = ChatMode.fromString(modeStr);
                                            if (mode == ChatMode.NONE && !modeStr.equalsIgnoreCase("none")) {
                                                ctx.getSource().sendFeedback(
                                                        Text.literal("[MiaoChat] 未知模式: " + modeStr + "，可用: ai, normal, none")
                                                                .formatted(Formatting.RED)
                                                );
                                                return 0;
                                            }
                                            return setMode(ctx, mode);
                                        }))
                        )
                        // /miaochat (显示当前状态)
                        .executes(ctx -> {
                            ctx.getSource().sendFeedback(
                                    Text.literal("[MiaoChat] 当前模式: ")
                                            .formatted(Formatting.GRAY)
                                            .append(Text.literal(MiaochatClient.getMode().getName())
                                                    .formatted(getModeColor(MiaochatClient.getMode())))
                            );
                            return 1;
                        })
        );
    }

    private static int setMode(CommandContext<FabricClientCommandSource> ctx, ChatMode mode) {
        MiaochatClient.setMode(mode);
        ctx.getSource().sendFeedback(
                Text.literal("[MiaoChat] 模式已切换为: ")
                        .formatted(Formatting.GREEN)
                        .append(Text.literal(mode.getName()).formatted(getModeColor(mode)))
        );
        return 1;
    }

    private static Formatting getModeColor(ChatMode mode) {
        return switch (mode) {
            case AI -> Formatting.LIGHT_PURPLE;
            case NORMAL -> Formatting.GOLD;
            case NONE -> Formatting.GRAY;
        };
    }
}
