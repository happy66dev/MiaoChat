package xyz.mcdw.miaochat.client;

public enum ChatMode {
    AI("ai"),
    NORMAL("normal"),
    NONE("none");

    private final String name;

    ChatMode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ChatMode fromString(String s) {
        for (ChatMode mode : values()) {
            if (mode.name.equalsIgnoreCase(s)) return mode;
        }
        return NONE;
    }
}
