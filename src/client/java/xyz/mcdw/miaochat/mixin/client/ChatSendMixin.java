package xyz.mcdw.miaochat.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.mcdw.miaochat.client.AiService;
import xyz.mcdw.miaochat.client.ChatBypass;
import xyz.mcdw.miaochat.client.ChatHistoryCollector;
import xyz.mcdw.miaochat.client.MiaoChatConfig;
import xyz.mcdw.miaochat.client.MiaochatClient;
import xyz.mcdw.miaochat.client.NyanText;
import xyz.mcdw.miaochat.client.TransformResult;

import java.util.List;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ChatSendMixin {

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void miaochat$onSendChatMessage(String content, CallbackInfo ci) {
        // 防止重入导致无限循环
        if (ChatBypass.isBypassing()) return;
        // 命令直接放行
        if (content.startsWith("/")) return;

        switch (MiaochatClient.getMode()) {
            case NORMAL -> {
                String transformed = NyanText.transform(content);
                ci.cancel();
                ChatBypass.setBypassing(true);
                try {
                    ((ClientPlayNetworkHandler) (Object) this).sendChatMessage(transformed);
                } finally {
                    ChatBypass.setBypassing(false);
                }
            }
            case AI -> {
                ci.cancel();
                ClientPlayNetworkHandler self = (ClientPlayNetworkHandler) (Object) this;
                MinecraftClient client = MinecraftClient.getInstance();

                // 在客户端显示提示
                if (client.player != null) {
                    client.player.sendMessage(
                            Text.literal("正在润色语言喵~").formatted(Formatting.LIGHT_PURPLE),
                            true
                    );
                }

                // 异步调用 AI API
                Thread.startVirtualThread(() -> {
                    List<String> context = ChatHistoryCollector.getRecentPlayerMessages(
                            MiaoChatConfig.getContextSize()
                    );
                    TransformResult result = AiService.transform(content, context);
                    // 回到主线程发送消息
                    client.execute(() -> {
                        if (client.player != null) {
                            // 清除 action bar
                            client.player.sendMessage(Text.literal(""), true);
                        }

                        // debug 模式：输出调试日志到聊天栏
                        if (MiaoChatConfig.isDebug() && !result.debugLogs().isEmpty()) {
                            if (client.player != null) {
                                client.player.sendMessage(
                                        Text.literal("\u2500\u2500\u2500\u2500 [MiaoChat Debug] \u2500\u2500\u2500\u2500").formatted(Formatting.DARK_GRAY),
                                        false
                                );
                                for (String log : result.debugLogs()) {
                                    client.player.sendMessage(
                                            Text.literal(log).formatted(Formatting.GRAY),
                                            false
                                    );
                                }
                                client.player.sendMessage(
                                        Text.literal("\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500").formatted(Formatting.DARK_GRAY),
                                        false
                                );
                            }
                        }

                        // 思考模式：输出思考过程到聊天栏
//                        if (MiaoChatConfig.isThinking() && result.reasoningContent() != null && !result.reasoningContent().isEmpty()) {
//                            if (client.player != null) {
//                                client.player.sendMessage(
//                                        Text.literal("\u2550\u2550 [\u601d\u8003\u8fc7\u7a0b] \u2550\u2550").formatted(Formatting.DARK_PURPLE),
//                                        false
//                                );
//                                // 按行输出思考过程
//                                String[] lines = result.reasoningContent().split("\n");
//                                for (String line : lines) {
//                                    if (!line.isBlank()) {
//                                        client.player.sendMessage(
//                                                Text.literal("\u2582 " + line).formatted(Formatting.LIGHT_PURPLE),
//                                                false
//                                        );
//                                    }
//                                }
//                                client.player.sendMessage(
//                                        Text.literal("\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550").formatted(Formatting.DARK_PURPLE),
//                                        false
//                                );
//                            }
//                        }

                        if (result.isSuccess()) {
                            ChatBypass.setBypassing(true);
                            try {
                                self.sendChatMessage(result.content());
                            } finally {
                                ChatBypass.setBypassing(false);
                            }
                        } else {
                            if (client.player != null) {
                                client.player.sendMessage(
                                        Text.literal("[MiaoChat] " + result.error()).formatted(Formatting.RED),
                                        false
                                );
                            }
                        }
                    });
                });
            }
            case NONE -> {
                // 不做任何处理，直接放行
            }
        }
    }
}
