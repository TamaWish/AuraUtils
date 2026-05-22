package me.aurautils.platform;

import me.aurautils.AuraUtils;

public final class PlatformFactory {

    private static final String[] PAPER_MARKERS = {
            "io.papermc.paper.PaperBootstrap",
            "com.destroystokyo.paper.PaperConfig",
            "io.papermc.paper.ServerBuildInfo"
    };

    private PlatformFactory() {
    }

    public static PlatformAdapter create(AuraUtils plugin) {
        if (isPaperServer()) {
            return new PaperPlatformAdapter(plugin);
        }
        return new SpigotPlatformAdapter(plugin);
    }

    private static boolean isPaperServer() {
        for (String marker : PAPER_MARKERS) {
            try {
                Class.forName(marker);
                return true;
            } catch (ClassNotFoundException ignored) {
            }
        }
        return false;
    }
}
