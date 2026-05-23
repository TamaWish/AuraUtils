package me.aurautils.managers;

import me.aurautils.config.AuraConfig;

/**
 * Tracks recent RTP outcomes and applies temporary search widening when failure rate is high.
 */
public final class RtpAdaptiveState {

    private static final int WINDOW_SIZE = 20;

    private int windowAttempts;
    private int windowFailures;

    public synchronized void recordSuccess() {
        recordOutcome(false);
    }

    public synchronized void recordFailure() {
        recordOutcome(true);
    }

    private void recordOutcome(boolean failed) {
        if (windowAttempts >= WINDOW_SIZE) {
            windowAttempts = (int) Math.ceil(WINDOW_SIZE * 0.5);
            windowFailures = failed ? windowAttempts / 2 : 0;
        }
        windowAttempts++;
        if (failed) {
            windowFailures++;
        }
    }

    public synchronized boolean shouldAdapt(AuraConfig config) {
        if (!config.rtpAdaptiveEnabled() || windowAttempts < config.rtpAdaptiveMinSamples()) {
            return false;
        }
        int failPercent = (windowFailures * 100) / windowAttempts;
        return failPercent >= config.rtpAdaptiveFailThresholdPercent();
    }

    public int adjustedRadius(int baseRadius, AuraConfig config) {
        if (!shouldAdapt(config)) {
            return baseRadius;
        }
        int widened = baseRadius + config.rtpAdaptiveRadiusBonus();
        if (config.rtpAdaptiveMaxRadius() > 0) {
            return Math.min(widened, config.rtpAdaptiveMaxRadius());
        }
        return widened;
    }

    public int adjustedMinDistance(int baseMinDistance, AuraConfig config) {
        if (!shouldAdapt(config)) {
            return baseMinDistance;
        }
        return Math.max(0, baseMinDistance - config.rtpAdaptiveMinDistanceReduction());
    }
}
