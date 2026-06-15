package xyz.mcdw.miaochat.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class MiaoChatConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("miaochat.json");

    private static String apiUrl = "https://api.openai.com/v1/chat/completions";
    private static String apiKey = "";
    private static String model = "gpt-3.5-turbo";
    private static int contextSize = 15;
    private static int maxTokens = 2048;
    private static boolean thinking = false;
    private static boolean debug = false;
    private static String systemPrompt = "你是一个可爱的猫娘，正在和朋友聊天。" +
            "请根据下面的聊天上下文，将最后一条消息改写成猫娘风格的发言，注意上下文可能并不与发言相关。" +
            "要求：在句中适当位置加入'喵'、'喵~'等语气词，语气要可爱撒娇，如果信息包含他人或者隐式包含，则追加主人，称别人为主人" +
            "保持原意不变，注意上下文中的话题和语境。" +
            "只输出改写后的文本，不要加任何解释、引号或前缀。";

    public static String getApiUrl() {
        return apiUrl;
    }

    public static String getApiKey() {
        return apiKey;
    }

    public static String getModel() {
        return model;
    }

    public static String getSystemPrompt() {
        return systemPrompt;
    }

    public static int getContextSize() {
        return contextSize;
    }

    public static int getMaxTokens() {
        return maxTokens;
    }

    public static boolean isThinking() {
        return thinking;
    }

    public static boolean isDebug() {
        return debug;
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        try {
            String json = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("api_url")) apiUrl = obj.get("api_url").getAsString();
            if (obj.has("api_key")) apiKey = obj.get("api_key").getAsString();
            if (obj.has("model")) model = obj.get("model").getAsString();
            if (obj.has("system_prompt")) systemPrompt = obj.get("system_prompt").getAsString();
            if (obj.has("context_size")) contextSize = obj.get("context_size").getAsInt();
            if (obj.has("max_tokens")) maxTokens = obj.get("max_tokens").getAsInt();
            if (obj.has("thinking")) thinking = obj.get("thinking").getAsBoolean();
            if (obj.has("debug")) debug = obj.get("debug").getAsBoolean();
        } catch (Exception e) {
            MiaochatClient.LOGGER.error("[MiaoChat] 读取配置文件失败", e);
        }
    }

    public static void save() {
        try {
            JsonObject obj = new JsonObject();
            obj.addProperty("api_url", apiUrl);
            obj.addProperty("api_key", apiKey);
            obj.addProperty("model", model);
            obj.addProperty("system_prompt", systemPrompt);
            obj.addProperty("context_size", contextSize);
            obj.addProperty("max_tokens", maxTokens);
            obj.addProperty("thinking", thinking);
            obj.addProperty("debug", debug);
            Files.writeString(CONFIG_PATH, obj.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            MiaochatClient.LOGGER.error("[MiaoChat] 保存配置文件失败", e);
        }
    }
}
