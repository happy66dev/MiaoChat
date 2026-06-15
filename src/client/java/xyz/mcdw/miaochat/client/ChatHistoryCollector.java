package xyz.mcdw.miaochat.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 从客户端聊天 HUD 中提取玩家消息作为 AI 上下文。
 * <p>
 * 由于不同 Minecraft 版本的 ChatHud API 存在差异（方法名、返回类型等），
 * 本类采用多策略反射方式获取消息列表，以保证跨版本兼容性（>=1.21.4）。
 */
public class ChatHistoryCollector {

    /**
     * 获取聊天窗口中最近 n 条消息（包含所有玩家和系统消息，保留完整对话流）
     * <p>
     * 不再只筛选玩家消息，而是保留聊天窗口中最近的 N 条完整消息，
     * 让 LLM 自行判断哪些是玩家对话，从而获得更完整的上下文。
     *
     * @param maxCount 最大条数，从配置读取
     * @return 按时间顺序排列的消息列表
     */
    public static List<String> getRecentPlayerMessages(int maxCount) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud == null) return Collections.emptyList();

        ChatHud chatHud = client.inGameHud.getChatHud();

        // 使用多策略反射获取聊天消息列表
        List<?> messages = getChatMessages(chatHud);
        if (messages == null || messages.isEmpty()) return Collections.emptyList();

        List<String> result = new ArrayList<>();
        // ChatHud 列表顺序：index 0 = 最新消息，末尾 = 最旧消息
        // 从 index 0 开始取最近 N 条
        for (int i = 0; i < messages.size() && result.size() < maxCount; i++) {
            String text = extractText(messages.get(i));
            if (text != null && !text.isBlank()) {
                result.add(text);
            }
        }

        // 反转为时间正序（旧→新）
        Collections.reverse(result);
        return result;
    }

    /**
     * 多策略反射获取 ChatHud 中的消息列表。
     * <p>
     * 按优先级依次尝试：
     * 1. 查找类型为 List 且元素为 ChatHudLine 的字段（适用于大多数版本）
     * 2. 查找名为 messages/messageLog 等常见名称的字段
     * 3. 尝试调用返回 List 的无参方法
     */
    private static List<?> getChatMessages(ChatHud chatHud) {
        // 策略1：遍历所有字段，查找包含 ChatHudLine 的 List 字段
        try {
            for (Field field : chatHud.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(chatHud);
                if (value instanceof List<?> list && !list.isEmpty()) {
                    Object first = list.get(0);
                    if (isChatHudLine(first)) {
                        return list;
                    }
                }
            }
        } catch (Exception e) {
            MiaochatClient.LOGGER.warn("[MiaoChat] 策略1反射获取聊天消息失败", e);
        }

        // 策略2：尝试常见方法名获取消息
        String[] methodNames = {"messages", "getMessages", "messages", "getVisibleMessages"};
        for (String methodName : methodNames) {
            try {
                Method method = chatHud.getClass().getDeclaredMethod(methodName);
                method.setAccessible(true);
                Object result = method.invoke(chatHud);
                if (result instanceof List<?> list && !list.isEmpty()) {
                    return list;
                }
            } catch (NoSuchMethodException ignored) {
                // 方法不存在，继续尝试下一个
            } catch (Exception e) {
                MiaochatClient.LOGGER.warn("[MiaoChat] 策略2调用方法 {} 失败", methodName, e);
            }
        }

        MiaochatClient.LOGGER.warn("[MiaoChat] 无法通过反射获取聊天消息列表，所有策略均失败");
        return Collections.emptyList();
    }

    /**
     * 从消息对象中提取文本内容。
     * 支持 ChatHudLine 及其包装类型（如 ChatHudLine.Visible）。
     */
    private static String extractText(Object messageObj) {
        if (messageObj == null) return null;

        try {
            // 如果是 ChatHudLine 直接调用 content()
            if (messageObj instanceof ChatHudLine line) {
                Text content = line.content();
                return content != null ? content.getString() : null;
            }

            // 尝试调用 inner() 获取内部 ChatHudLine（适用于 ChatHudLine.Visible 等包装类型）
            try {
                Method innerMethod = messageObj.getClass().getMethod("inner");
                innerMethod.setAccessible(true);
                Object inner = innerMethod.invoke(messageObj);
                if (inner instanceof ChatHudLine line) {
                    Text content = line.content();
                    return content != null ? content.getString() : null;
                }
            } catch (NoSuchMethodException ignored) {
                // 没有 inner() 方法
            }

            // 尝试直接调用 content() 方法
            try {
                Method contentMethod = messageObj.getClass().getMethod("content");
                contentMethod.setAccessible(true);
                Object textObj = contentMethod.invoke(messageObj);
                if (textObj instanceof Text text) {
                    return text.getString();
                }
            } catch (NoSuchMethodException ignored) {
                // 没有 content() 方法
            }
        } catch (Exception e) {
            MiaochatClient.LOGGER.debug("[MiaoChat] 提取消息文本失败: {}", messageObj.getClass().getName(), e);
        }

        return null;
    }

    /**
     * 判断对象是否为 ChatHudLine 或其包装类型
     */
    private static boolean isChatHudLine(Object obj) {
        if (obj instanceof ChatHudLine) return true;
        // 检查是否有 inner() 方法返回 ChatHudLine（包装类型如 ChatHudLine.Visible）
        try {
            Method innerMethod = obj.getClass().getMethod("inner");
            return ChatHudLine.class.isAssignableFrom(innerMethod.getReturnType());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * 判断一条消息是否为玩家发送的聊天消息
     * 排除系统公告、插件消息、死亡通知等
     */
    private static boolean isPlayerMessage(String text) {
        if (text == null || text.isBlank()) return false;
//
//        // 排除以 [ 开头的系统/插件消息（如 [Server], [Admin] 等）
//        if (text.startsWith("[")) return false;
//
//        // 排除死亡消息（包含 "被", "杀死了", "掉入", "死于" 等关键词）
//        if (text.contains("被") && (text.contains("杀死") || text.contains("击毙"))) return false;
//        if (text.contains("掉入") || text.contains("死于") || text.contains("试图")) return false;
//
//        // 排除成就/进度消息
//        if (text.contains("获得了进度") || text.contains("达成了目标") || text.contains("完成了挑战")) return false;
//
//        // 排除以 > 开头的引用消息
//        if (text.startsWith(">")) return false;
//
//        // 排除纯数字或纯符号
//        if (text.matches("^[\\d\\s]+$")) return false;

        return true;
    }
}
