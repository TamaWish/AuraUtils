package me.aurautils.platform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkLoadCoordinatorTest {

    private static final UUID PLAYER_A = UUID.randomUUID();
    private static final UUID PLAYER_B = UUID.randomUUID();

    private final List<Runnable> mainQueue = new ArrayList<>();
    private ChunkLoadCoordinator coordinator;

    @BeforeEach
    void setUp() {
        mainQueue.clear();
        coordinator = new ChunkLoadCoordinator(2, 1, 4, mainQueue::add);
    }

    @Test
    void enforcesGlobalCap() {
        AtomicInteger started = new AtomicInteger();
        coordinator.schedule(PLAYER_A, started::incrementAndGet, () -> {
        }, ChunkLoadCoordinator.QueuePolicy.REJECT_IF_BUSY);
        coordinator.schedule(PLAYER_B, started::incrementAndGet, () -> {
        }, ChunkLoadCoordinator.QueuePolicy.REJECT_IF_BUSY);

        flushMain();
        assertEquals(2, started.get());
        assertEquals(2, coordinator.globalInFlight());
        assertFalse(coordinator.hasImmediateCapacity(PLAYER_A));

        AtomicInteger rejected = new AtomicInteger();
        coordinator.schedule(PLAYER_A, () -> {
        }, rejected::incrementAndGet, ChunkLoadCoordinator.QueuePolicy.REJECT_IF_BUSY);
        flushMain();
        assertEquals(1, rejected.get());
    }

    @Test
    void queuesTeleportsAndDrainsOnRelease() {
        AtomicInteger started = new AtomicInteger();
        coordinator.schedule(PLAYER_A, started::incrementAndGet, () -> {
        }, ChunkLoadCoordinator.QueuePolicy.QUEUE);
        coordinator.schedule(PLAYER_B, started::incrementAndGet, () -> {
        }, ChunkLoadCoordinator.QueuePolicy.QUEUE);
        coordinator.schedule(PLAYER_A, started::incrementAndGet, () -> {
        }, ChunkLoadCoordinator.QueuePolicy.QUEUE);
        flushMain();
        assertEquals(2, started.get());

        coordinator.release(PLAYER_A);
        flushMain();
        assertEquals(3, started.get());
        assertEquals(2, coordinator.globalInFlight());
    }

    @Test
    void perPlayerCapBlocksSecondSlot() {
        AtomicInteger started = new AtomicInteger();
        coordinator.schedule(PLAYER_A, started::incrementAndGet, () -> {
        }, ChunkLoadCoordinator.QueuePolicy.REJECT_IF_BUSY);
        flushMain();
        assertEquals(1, started.get());
        assertFalse(coordinator.hasImmediateCapacity(PLAYER_A));
        assertTrue(coordinator.hasImmediateCapacity(PLAYER_B));
    }

    private void flushMain() {
        while (!mainQueue.isEmpty()) {
            List<Runnable> batch = new ArrayList<>(mainQueue);
            mainQueue.clear();
            batch.forEach(Runnable::run);
        }
    }
}
