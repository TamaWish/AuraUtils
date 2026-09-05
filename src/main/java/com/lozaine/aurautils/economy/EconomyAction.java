package com.lozaine.aurautils.economy;

/**
 * Paid AuraUtils actions. Config keys live under {@code economy.costs.<key>}.
 */
public enum EconomyAction {
    HOME("home"),
    SET_HOME("sethome"),
    WARP("warp"),
    SET_WARP("setwarp"),
    TPA("tpa"),
    RTP("rtp"),
    BACK("back");

    private final String configKey;

    EconomyAction(String configKey) {
        this.configKey = configKey;
    }

    public String configKey() {
        return configKey;
    }
}
