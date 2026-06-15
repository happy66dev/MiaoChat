package xyz.mcdw.miaochat.client;

/**
 * 用于防止 mixin 重入导致无限循环的标志位
 */
public class ChatBypass {
    private static boolean bypassing = false;

    public static boolean isBypassing() {
        return bypassing;
    }

    public static void setBypassing(boolean value) {
        bypassing = value;
    }
}
