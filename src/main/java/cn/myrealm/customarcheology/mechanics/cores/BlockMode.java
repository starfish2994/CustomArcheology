package cn.myrealm.customarcheology.mechanics.cores;

import java.util.Locale;

public enum BlockMode {
    LEGACY,
    CRAFTENGINE;

    public static BlockMode parse(String value) {
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Unknown archeology mode: " + value + " (expected legacy or craftengine)");
        }
    }

    public String configValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
