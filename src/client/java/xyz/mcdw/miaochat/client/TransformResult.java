package xyz.mcdw.miaochat.client;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 转换结果：成功时包含内容，失败时包含错误信息。
 * debug 开启时额外记录请求/响应的详细日志。
 */
public class TransformResult {

    private final String content;
    private final String reasoningContent;
    private final String error;
    private final List<String> debugLogs;

    private TransformResult(String content, String reasoningContent, String error, List<String> debugLogs) {
        this.content = content;
        this.reasoningContent = reasoningContent;
        this.error = error;
        this.debugLogs = debugLogs != null ? debugLogs : new ArrayList<>();
    }

    public static TransformResult success(String content) {
        return new TransformResult(content, null, null, null);
    }

    public static TransformResult success(String content, String reasoningContent, List<String> debugLogs) {
        return new TransformResult(content, reasoningContent, null, debugLogs);
    }

    public static TransformResult fail(String error) {
        return new TransformResult(null, null, error, null);
    }

    public static TransformResult fail(String error, List<String> debugLogs) {
        return new TransformResult(null, null, error, debugLogs);
    }

    public boolean isSuccess() {
        return error == null;
    }

    public String content() {
        return content;
    }

    public String reasoningContent() {
        return reasoningContent;
    }

    public String error() {
        return error;
    }

    public List<String> debugLogs() {
        return debugLogs;
    }
}
