package xyz.mcdw.miaochat.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class AiService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /**
     * 调用 AI API 将消息改写为猫娘风格
     *
     * @param message 当前要发送的消息
     * @param context 上下文聊天消息列表
     * @return TransformResult，成功时包含内容，失败时包含错误信息
     */
    public static TransformResult transform(String message, List<String> context) {
        boolean debug = MiaoChatConfig.isDebug();
        boolean thinking = MiaoChatConfig.isThinking();
        List<String> debugLogs = debug ? new ArrayList<>() : null;

        // 检查 API Key 是否配置
        String apiKey = MiaoChatConfig.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return TransformResult.fail("API Key 未配置，请编辑 config/miaochat.json 填写 api_key");
        }

        try {
            JsonObject body = new JsonObject();
            body.addProperty("model", MiaoChatConfig.getModel());

            JsonArray messages = new JsonArray();

            // 系统提示词 + 玩家游戏名
            String playerName = "Unknown";
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null && mc.player.getGameProfile() != null) {
                playerName = mc.player.getGameProfile().getName();
            }
            String systemContent = MiaoChatConfig.getSystemPrompt()
                    + "\n\n[系统信息] 当前你的游戏名是：" + playerName + "，聊天记录中这个游戏名是你说过的话。";
            // 思考模式：追加思考指令
            if (thinking) {
                systemContent += "\n思考使用中文。";
            }
            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content", systemContent);
            messages.add(systemMsg);

            // 上下文消息（作为 user 角色传入，让 AI 理解对话语境）
            if (context != null && !context.isEmpty()) {
                StringBuilder ctxBuilder = new StringBuilder();
                ctxBuilder.append("以下是最近的聊天记录：\n");
                for (String msg : context) {
                    ctxBuilder.append(msg).append("\n");
                }
                JsonObject ctxMsg = new JsonObject();
                ctxMsg.addProperty("role", "user");
                ctxMsg.addProperty("content", ctxBuilder.toString());
                messages.add(ctxMsg);
            }

            // 当前要改写的消息
            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", "请将以下消息改写为猫娘风格：" + message);
            messages.add(userMsg);

            body.add("messages", messages);
            body.addProperty("temperature", 0.8);
            body.addProperty("max_tokens", MiaoChatConfig.getMaxTokens());

            // 根据模型名称判断是否需要添加关闭思考模式的参数
            String model = MiaoChatConfig.getModel().toLowerCase();
            if (!thinking) {
                // 强制跳过规则：模型名称包含 thinking/reasoning/deepthink 后缀时无法关闭
                boolean forcedThinking = model.endsWith("thinking") || model.endsWith("reasoning") || model.endsWith("deepthink");
                if (!forcedThinking) {
                    if (model.contains("grok")) {
                        // Grok 系列
                        if (model.contains("grok-3-mini")) {
                            // grok-3-mini 特殊降级
                            JsonObject reasoningObj = new JsonObject();
                            reasoningObj.addProperty("effort", "low");
                            body.add("reasoning", reasoningObj);
                        } else if (model.matches(".*grok-4\\.2.*") || model.matches(".*grok-4\\.3.*")) {
                            // grok-4.20 / grok-4.3 降级方案
                            JsonObject reasoningObj = new JsonObject();
                            reasoningObj.addProperty("effort", "none");
                            body.add("reasoning", reasoningObj);
                        } else {
                            // 通用 Grok 关闭
                            JsonObject reasoningObj = new JsonObject();
                            reasoningObj.addProperty("enabled", false);
                            body.add("reasoning", reasoningObj);
                        }
                    } else if (model.contains("gemini")) {
                        // Gemini Flash/Lite
                        body.addProperty("thinking_budget", 0);
                    } else if (model.contains("qwen") || model.contains("vllm")) {
                        // Qwen3 / vLLM 部署模型
                        JsonObject templateKwargs = new JsonObject();
                        templateKwargs.addProperty("enable_thinking", false);
                        body.add("chat_template_kwargs", templateKwargs);
                    } else {
                        // Kimi / DeepSeek / MiMo / Doubao (通用)
                        JsonObject thinkingObj = new JsonObject();
                        thinkingObj.addProperty("type", "disabled");
                        body.add("thinking", thinkingObj);
                    }
                }
            }
            // 思考模式开启时不显式包含参数

            // debug: 记录请求详情
            if (debug) {
                debugLogs.add("══ [MiaoChat Debug] 请求详情 ══");
                debugLogs.add("▶ API: " + MiaoChatConfig.getApiUrl());
                debugLogs.add("▶ Model: " + MiaoChatConfig.getModel());
                debugLogs.add("▶ Player: " + playerName);
                debugLogs.add("▶ Temperature: 0.8 | Max Tokens: " + MiaoChatConfig.getMaxTokens());
                debugLogs.add("▶ Thinking: " + thinking);
                debugLogs.add("▶ 原始消息: " + message);
                debugLogs.add("▶ 上下文条数: " + (context != null ? context.size() : 0));
                debugLogs.add("▶ Messages 组成 (共" + messages.size() + "条):");
                // 上下文逐条展示，不截断
                if (context != null && !context.isEmpty()) {
                    debugLogs.add("  [1] role=user | 上下文聊天记录:");
                    for (int j = 0; j < context.size(); j++) {
                        debugLogs.add("    #" + (j + 1) + " " + context.get(j));
                    }
                }
                // 显示 system 和最后一条 user 消息
                for (int i = 0; i < messages.size(); i++) {
                    JsonObject m = messages.get(i).getAsJsonObject();
                    String role = m.get("role").getAsString();
                    // 跳过已单独展示的上下文消息
                    if (i == 1 && context != null && !context.isEmpty()) continue;
                    String contentStr = m.get("content").getAsString();
                    debugLogs.add("  [" + i + "] role=" + role + " | " + contentStr);
                }
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MiaoChatConfig.getApiUrl()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + MiaoChatConfig.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            long startTime = System.currentTimeMillis();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            long elapsed = System.currentTimeMillis() - startTime;

            // debug: 记录响应状态码和耗时
            if (debug) {
                debugLogs.add("══ [MiaoChat Debug] 响应详情 ══");
                debugLogs.add("◀ HTTP Status: " + response.statusCode());
                debugLogs.add("◀ 耗时: " + elapsed + "ms");
            }

            if (response.statusCode() != 200) {
                String errorBody = response.body();
                String errorMsg = "AI API 返回 HTTP " + response.statusCode();
                // 尝试从响应体提取错误信息
                try {
                    JsonObject errJson = JsonParser.parseString(errorBody).getAsJsonObject();
                    if (errJson.has("error")) {
                        JsonObject errObj = errJson.getAsJsonObject("error");
                        if (errObj.has("message")) {
                            errorMsg += ": " + errObj.get("message").getAsString();
                        }
                    }
                } catch (Exception ignored) {
                    // 无法解析错误体，使用原始响应
                    if (errorBody != null && !errorBody.isBlank()) {
                        errorMsg += ": " + errorBody.substring(0, Math.min(errorBody.length(), 200));
                    }
                }
                // debug: 记录错误响应体
                if (debug) {
                    String truncatedBody = errorBody != null ? errorBody.substring(0, Math.min(errorBody.length(), 500)) : "(empty)";
                    debugLogs.add("◀ 错误响应体: " + truncatedBody);
                }
                MiaochatClient.LOGGER.error("[MiaoChat] {}", errorMsg);
                return TransformResult.fail(errorMsg, debugLogs);
            }

            String responseBody = response.body();
            JsonObject respJson = JsonParser.parseString(responseBody).getAsJsonObject();

            // debug: 记录响应原始 JSON 结构
            if (debug) {
                debugLogs.add("◀ 响应 JSON keys: " + respJson.keySet());
                if (respJson.has("choices")) {
                    JsonArray choices = respJson.getAsJsonArray("choices");
                    debugLogs.add("◀ choices 数量: " + choices.size());
                    if (choices.size() > 0) {
                        JsonObject firstChoice = choices.get(0).getAsJsonObject();
                        debugLogs.add("◀ choices[0] keys: " + firstChoice.keySet());
                        if (firstChoice.has("message")) {
                            JsonObject msgObj = firstChoice.getAsJsonObject("message");
                            debugLogs.add("◀ choices[0].message keys: " + msgObj.keySet());
                            JsonElement contentElement = msgObj.get("content");
                            if (contentElement == null || contentElement.isJsonNull()) {
                                debugLogs.add("◀ choices[0].message.content = null");
                            } else {
                                debugLogs.add("◀ choices[0].message.content = \"" + contentElement.getAsString() + "\"");
                            }
                            // 思考过程
                            JsonElement reasoningElement = msgObj.get("reasoning_content");
                            if (reasoningElement != null && !reasoningElement.isJsonNull()) {
                                String reasoning = reasoningElement.getAsString();
                                debugLogs.add("◀ reasoning_content (" + reasoning.length() + "字): "
                                        + reasoning.substring(0, Math.min(reasoning.length(), 200))
                                        + (reasoning.length() > 200 ? "..." : ""));
                            }
                        }
                        // 某些 API 返回 finish_reason
                        if (firstChoice.has("finish_reason")) {
                            debugLogs.add("◀ finish_reason: " + firstChoice.get("finish_reason").getAsString());
                        }
                    }
                }
            }

            // 安全解析响应内容
            String result = "";
            String reasoningResult = null;
            JsonArray choices = respJson.getAsJsonArray("choices");
            if (choices != null && choices.size() > 0) {
                JsonObject firstChoice = choices.get(0).getAsJsonObject();
                if (firstChoice.has("message")) {
                    JsonObject msgObj = firstChoice.getAsJsonObject("message");
                    JsonElement contentElement = msgObj.get("content");
                    if (contentElement != null && !contentElement.isJsonNull()) {
                        result = contentElement.getAsString().trim();
                    }
                    // 解析思考过程
                    JsonElement reasoningElement = msgObj.get("reasoning_content");
                    if (reasoningElement != null && !reasoningElement.isJsonNull()) {
                        reasoningResult = reasoningElement.getAsString().trim();
                    }
                }
            }

            // debug: 记录 token 用量和模型
            if (debug) {
                debugLogs.add("◀ 解析后结果: " + (result.isEmpty() ? "(空)" : result));
                if (respJson.has("usage")) {
                    JsonObject usage = respJson.getAsJsonObject("usage");
                    debugLogs.add("◀ Token 用量: prompt=" + usage.get("prompt_tokens").getAsInt()
                            + " completion=" + usage.get("completion_tokens").getAsInt()
                            + " total=" + usage.get("total_tokens").getAsInt());
                }
                if (respJson.has("model")) {
                    debugLogs.add("◀ 实际模型: " + respJson.get("model").getAsString());
                }
            }

            if (result.isEmpty()) {
                return TransformResult.fail("AI 返回内容为空，请检查 debug 日志", debugLogs);
            }

            return TransformResult.success(result, reasoningResult, debugLogs);

        } catch (java.net.http.HttpTimeoutException e) {
            MiaochatClient.LOGGER.error("[MiaoChat] AI API 请求超时", e);
            if (debug) debugLogs.add("◀ 异常: 请求超时");
            return TransformResult.fail("AI API 请求超时，请检查网络连接或 API 地址", debugLogs);
        } catch (java.net.ConnectException e) {
            MiaochatClient.LOGGER.error("[MiaoChat] 无法连接 AI API", e);
            if (debug) debugLogs.add("◀ 异常: 无法连接");
            return TransformResult.fail("无法连接 AI API，请检查 api_url 配置", debugLogs);
        } catch (Exception e) {
            MiaochatClient.LOGGER.error("[MiaoChat] AI API 调用失败", e);
            if (debug) debugLogs.add("◀ 异常: " + e.getMessage());
            return TransformResult.fail("AI API 调用失败: " + e.getMessage(), debugLogs);
        }
    }
}
