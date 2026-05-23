package me.aurautils.platform;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Server-wide async chunk load pipeline: global + per-player in-flight caps, FIFO queue, backpressure.
 */
public final class ChunkLoadCoordinator {

    public enum QueuePolicy {
        /** Wait in queue until a slot opens (teleports, single-shot loads). */
        QUEUE,
        /** Fail immediately when no slot is available (RTP search probes). */
        REJECT_IF_BUSY
    }

    private record QueuedRequest(UUID playerId, Runnable onAcquired) {
    }

    private final int maxGlobal;
    private final int maxPerPlayer;
    private final int maxQueueSize;
    private final Consumer<Runnable> mainThreadRunner;

    private final AtomicInteger globalInFlight = new AtomicInteger(0);
    private final ConcurrentHashMap<UUID, AtomicInteger> perPlayerInFlight = new ConcurrentHashMap<>();
    private final ArrayDeque<QueuedRequest> queue = new ArrayDeque<>();
    private final Object lock = new Object();

    public ChunkLoadCoordinator(int maxGlobal, int maxPerPlayer, int maxQueueSize,
                                Consumer<Runnable> mainThreadRunner) {
        this.maxGlobal = Math.max(1, maxGlobal);
        this.maxPerPlayer = Math.max(1, maxPerPlayer);
        this.maxQueueSize = Math.max(0, maxQueueSize);
        this.mainThreadRunner = mainThreadRunner;
    }

    public int globalInFlight() {
        return globalInFlight.get();
    }

    public int queuedCount() {
        synchronized (lock) {
            return queue.size();
        }
    }

    public boolean hasImmediateCapacity(UUID playerId) {
        synchronized (lock) {
            return canAcquireLocked(playerId);
        }
    }

    public boolean isQueueFull() {
        synchronized (lock) {
            return queue.size() >= maxQueueSize;
        }
    }

    /**
     * Acquires a slot and runs {@code onAcquired} on the main thread, or queues / rejects per {@code policy}.
     * The acquired task must call {@link #release(UUID)} exactly once when the underlying load finishes.
     */
    public void schedule(UUID playerId, Runnable onAcquired, Runnable onRejected, QueuePolicy policy) {
        synchronized (lock) {
            if (tryAcquireLocked(playerId)) {
                runOnMain(onAcquired);
                return;
            }
            if (policy == QueuePolicy.REJECT_IF_BUSY) {
                runOnMain(onRejected);
                return;
            }
            if (maxQueueSize == 0 || queue.size() >= maxQueueSize) {
                runOnMain(onRejected);
                return;
            }
            queue.addLast(new QueuedRequest(playerId, onAcquired));
        }
    }

    public void release(UUID playerId) {
        synchronized (lock) {
            decrementLocked(playerId);
            drainQueueLocked();
        }
    }

    public void clearQueue() {
        synchronized (lock) {
            queue.clear();
        }
    }

    private void drainQueueLocked() {
        for (Iterator<QueuedRequest> it = queue.iterator(); it.hasNext(); ) {
            if (globalInFlight.get() >= maxGlobal) {
                break;
            }
            QueuedRequest req = it.next();
            if (!canAcquireLocked(req.playerId())) {
                continue;
            }
            it.remove();
            tryAcquireLocked(req.playerId());
            runOnMain(req.onAcquired());
        }
    }

    private boolean canAcquireLocked(UUID playerId) {
        if (globalInFlight.get() >= maxGlobal) {
            return false;
        }
        AtomicInteger player = perPlayerInFlight.get(playerId);
        return player == null || player.get() < maxPerPlayer;
    }

    private boolean tryAcquireLocked(UUID playerId) {
        if (!canAcquireLocked(playerId)) {
            return false;
        }
        globalInFlight.incrementAndGet();
        perPlayerInFlight.computeIfAbsent(playerId, id -> new AtomicInteger(0)).incrementAndGet();
        return true;
    }

    private void decrementLocked(UUID playerId) {
        globalInFlight.updateAndGet(current -> Math.max(0, current - 1));
        AtomicInteger player = perPlayerInFlight.get(playerId);
        if (player == null) {
            return;
        }
        int remaining = player.decrementAndGet();
        if (remaining <= 0) {
            perPlayerInFlight.remove(playerId, player);
        }
    }

    private void runOnMain(Runnable task) {
        mainThreadRunner.accept(task);
    }
}
