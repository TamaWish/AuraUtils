package me.aurautils.managers;

import me.aurautils.platform.ChunkLoadPolicy;
import me.aurautils.util.MessagePlaceholders;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Parameters for {@link TeleportService#teleport}.
 */
public final class TeleportOptions {

    private final int countdownSeconds;
    private final boolean horizontalMovementOnly;
    private final String successMessageKey;
    private final MessagePlaceholders successPlaceholders;
    private final Runnable onSuccess;
    private final boolean skipBackRecord;
    private final PlayerTeleportEvent.TeleportCause cause;
    private final ChunkLoadPolicy chunkPolicyOverride;
    private final Boolean generateChunksOverride;
    private final Boolean asyncUrgentOverride;
    private final boolean sendChunkFailureMessage;

    private TeleportOptions(Builder builder) {
        this.countdownSeconds = builder.countdownSeconds;
        this.horizontalMovementOnly = builder.horizontalMovementOnly;
        this.successMessageKey = builder.successMessageKey;
        this.successPlaceholders = builder.successPlaceholders;
        this.onSuccess = builder.onSuccess;
        this.skipBackRecord = builder.skipBackRecord;
        this.cause = builder.cause;
        this.chunkPolicyOverride = builder.chunkPolicyOverride;
        this.generateChunksOverride = builder.generateChunksOverride;
        this.asyncUrgentOverride = builder.asyncUrgentOverride;
        this.sendChunkFailureMessage = builder.sendChunkFailureMessage;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int countdownSeconds() {
        return countdownSeconds;
    }

    public boolean horizontalMovementOnly() {
        return horizontalMovementOnly;
    }

    public String successMessageKey() {
        return successMessageKey;
    }

    public MessagePlaceholders successPlaceholders() {
        return successPlaceholders;
    }

    public Runnable onSuccess() {
        return onSuccess;
    }

    public boolean skipBackRecord() {
        return skipBackRecord;
    }

    public PlayerTeleportEvent.TeleportCause cause() {
        return cause;
    }

    public ChunkLoadPolicy chunkPolicyOverride() {
        return chunkPolicyOverride;
    }

    public Boolean generateChunksOverride() {
        return generateChunksOverride;
    }

    public Boolean asyncUrgentOverride() {
        return asyncUrgentOverride;
    }

    public boolean sendChunkFailureMessage() {
        return sendChunkFailureMessage;
    }

    public static final class Builder {
        private int countdownSeconds;
        private boolean horizontalMovementOnly;
        private String successMessageKey = "teleport.success-default";
        private MessagePlaceholders successPlaceholders = MessagePlaceholders.empty();
        private Runnable onSuccess;
        private boolean skipBackRecord = false;
        private PlayerTeleportEvent.TeleportCause cause = PlayerTeleportEvent.TeleportCause.COMMAND;
        private ChunkLoadPolicy chunkPolicyOverride;
        private Boolean generateChunksOverride;
        private Boolean asyncUrgentOverride;
        private boolean sendChunkFailureMessage = true;

        public Builder countdownSeconds(int countdownSeconds) {
            this.countdownSeconds = countdownSeconds;
            return this;
        }

        public Builder horizontalMovementOnly(boolean horizontalMovementOnly) {
            this.horizontalMovementOnly = horizontalMovementOnly;
            return this;
        }

        public Builder successMessageKey(String successMessageKey) {
            this.successMessageKey = successMessageKey;
            return this;
        }

        public Builder noSuccessMessage() {
            this.successMessageKey = null;
            return this;
        }

        public Builder successPlaceholders(MessagePlaceholders successPlaceholders) {
            this.successPlaceholders = successPlaceholders;
            return this;
        }

        public Builder onSuccess(Runnable onSuccess) {
            this.onSuccess = onSuccess;
            return this;
        }

        public Builder skipBackRecord(boolean skipBackRecord) {
            this.skipBackRecord = skipBackRecord;
            return this;
        }

        public Builder cause(PlayerTeleportEvent.TeleportCause cause) {
            this.cause = cause;
            return this;
        }

        public Builder chunkPolicyOverride(ChunkLoadPolicy chunkPolicyOverride) {
            this.chunkPolicyOverride = chunkPolicyOverride;
            return this;
        }

        public Builder generateChunksOverride(boolean generateChunksOverride) {
            this.generateChunksOverride = generateChunksOverride;
            return this;
        }

        public Builder asyncUrgentOverride(boolean asyncUrgentOverride) {
            this.asyncUrgentOverride = asyncUrgentOverride;
            return this;
        }

        public Builder sendChunkFailureMessage(boolean sendChunkFailureMessage) {
            this.sendChunkFailureMessage = sendChunkFailureMessage;
            return this;
        }

        public TeleportOptions build() {
            return new TeleportOptions(this);
        }
    }
}
