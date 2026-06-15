package xyz.mcdw.miaochat.client;

import java.util.regex.Pattern;

public class NyanText {

    // 匹配中文字符
    private static final Pattern CJK = Pattern.compile("[\\u4e00-\\u9fff\\u3400-\\u4dbf]");
    // 匹配标点符号（中英文标点 + 常见符号）
    // 使用 Unicode 转义避免源文件编码问题
    private static final Pattern PUNCTUATION = Pattern.compile(
            "[\\p{Punct}"
            // ！。，、；：？
            + "\uFF01\u3002\uFF0C\u3001\uFF1B\uFF1A\uFF1F"
            // \u201C\u201D\u2018\u2019
            + "\u201C\u201D\u2018\u2019"
            // 【】《》（）
            + "\u3010\u3011\u300A\u300B\uFF08\uFF09"
            // …—～·
            + "\u2026\u2014\uFF5E\u00B7"
            // 「」『』〈〉
            + "\u300C\u300D\u300E\u300F\u3008\u3009"
            + "]"
    );

    /**
     * 将普通文本转换为猫娘风格文本
     * 规则：
     * 1. 在文字→符号方向插入"喵"（符号→文字不插入）
     * 2. 非符号结尾追加"喵~"
     */
    public static String transform(String input) {
        if (input == null || input.isEmpty()) return input;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            sb.append(c);

            if (i < input.length() - 1) {
                char next = input.charAt(i + 1);
                boolean cIsCjk = isCjk(c);
                boolean nextIsPunct = isPunctuation(next);
                boolean cIsPunct = isPunctuation(c);
                boolean nextIsCjk = isCjk(next);

                // 仅 文字→符号 方向插入"喵"，符号→文字不插入
                if (cIsCjk && nextIsPunct) {
                    sb.append("喵");
                }
            }
        }

        String result = sb.toString();

        // 非符号结尾追加"喵~"
        if (!result.isEmpty() && !isPunctuation(result.charAt(result.length() - 1))) {
            result += "喵~";
        }

        return result;
    }

    private static boolean isCjk(char c) {
        return CJK.matcher(String.valueOf(c)).matches();
    }

    private static boolean isPunctuation(char c) {
        return PUNCTUATION.matcher(String.valueOf(c)).matches();
    }
}
