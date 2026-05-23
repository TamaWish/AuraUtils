package me.aurautils.storage;

@FunctionalInterface
public interface Cancellable {

    void cancel();

    Cancellable NOOP = () -> {
    };
}
