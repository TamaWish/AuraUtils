# AuraUtils 1.2.2

Fixes the Spigot watchdog crash caused by `/rtp` calling `getHighestBlockAt` on unloaded chunks (main-thread terrain generation). Also tightens Folia teleport/TPA thread use.

Same single JAR: Spigot, CraftBukkit, Paper, Purpur, Folia.

## Upgrade
Replace the old jar, then delete `plugins/AuraUtils/config.yml` **or** add the new `rtp` keys:

```yaml
rtp:
  max-search-ticks: 200
  generate-unloaded: true
  max-sync-generations: 3
```

`attemptsPerTick` is unused and can be removed.
