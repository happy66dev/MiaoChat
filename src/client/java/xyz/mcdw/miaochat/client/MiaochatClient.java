package xyz.mcdw.miaochat.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiaochatClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("miaochat");

    private static ChatMode currentMode = ChatMode.NONE;

    public static ChatMode getMode() {
        return currentMode;
    }

    public static void setMode(ChatMode mode) {
        currentMode = mode;
    }

    @Override
    public void onInitializeClient() {
        // 加载配置
        MiaoChatConfig.load();

        // 注册指令
        MiaoChatCommand.register();

        LOGGER.info("[MiaoChat] 模组已加载喵~");
    }
}
