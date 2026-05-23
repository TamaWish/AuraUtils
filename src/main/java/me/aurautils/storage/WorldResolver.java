package me.aurautils.storage;

import org.bukkit.World;

@FunctionalInterface
public interface WorldResolver {

    World getWorld(String name);
}
