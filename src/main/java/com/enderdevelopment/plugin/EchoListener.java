package com.enderdevelopment.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.NamespacedKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

final class EchoListener implements Listener {
    private static final long PLAYER_HIT_WINDOW_MILLIS = 15_000L;
    private static final long ECHO_LIFETIME_TICKS = 10L * 60L * 20L;
    private static final double ECHO_MAX_HEALTH = 40.0;
    private static final double ECHO_ATTACK_DAMAGE = 4.0;

    private final Main plugin;
    private final BuffManager buffManager;
    private final NamespacedKey echoKey;
    private final Map<UUID, PlayerHit> recentPlayerHits = new HashMap<>();
    private final Map<UUID, UUID> lastEchoDamager = new HashMap<>();
    private final Map<UUID, Zombie> activeEchoes = new HashMap<>();
    private final Map<UUID, BukkitTask> despawnTasks = new HashMap<>();

    EchoListener(Main plugin, BuffManager buffManager) {
        this.plugin = plugin;
        this.buffManager = buffManager;
        this.echoKey = new NamespacedKey(plugin, "echo_entity");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        Player attacker = getPlayerAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }

        if (event.getEntity() instanceof Player victim && !attacker.getUniqueId().equals(victim.getUniqueId())) {
            recentPlayerHits.put(victim.getUniqueId(), new PlayerHit(attacker.getUniqueId(), System.currentTimeMillis()));
        }

        if (event.getEntity() instanceof Zombie zombie && isEcho(zombie)) {
            lastEchoDamager.put(zombie.getUniqueId(), attacker.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        buffManager.clearBuff(victim);

        PlayerHit hit = recentPlayerHits.remove(victim.getUniqueId());
        Player responsiblePlayer = victim.getKiller();
        if (responsiblePlayer == null && hit != null
                && System.currentTimeMillis() - hit.timestamp <= PLAYER_HIT_WINDOW_MILLIS) {
            responsiblePlayer = Bukkit.getPlayer(hit.playerId);
        }

        if (responsiblePlayer != null && !responsiblePlayer.getUniqueId().equals(victim.getUniqueId())) {
            spawnEcho(victim, responsiblePlayer);
        }
    }

    @EventHandler
    public void onEchoDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie) || !isEcho(zombie)) {
            return;
        }

        UUID echoId = zombie.getUniqueId();
        cancelDespawnTask(echoId);
        activeEchoes.remove(echoId);
        UUID damagerId = lastEchoDamager.remove(echoId);
        if (damagerId == null) {
            return;
        }

        Player winner = Bukkit.getPlayer(damagerId);
        if (winner == null || !winner.isOnline()) {
            return;
        }

        String buffId = buffManager.applyRandomBuff(winner);
        if (buffId == null) {
            winner.sendMessage(Component.text("You defeated the Echo, but no buffs are enabled.", NamedTextColor.YELLOW));
        } else {
            winner.sendMessage(Component.text("You defeated the Echo and gained " + buffId + " until your next death.", NamedTextColor.GOLD));
        }
    }

    void shutdown() {
        for (BukkitTask task : despawnTasks.values()) {
            task.cancel();
        }
        despawnTasks.clear();
        for (Zombie zombie : activeEchoes.values()) {
            if (zombie.isValid()) {
                zombie.remove();
            }
        }
        activeEchoes.clear();
        lastEchoDamager.clear();
        recentPlayerHits.clear();
    }

    private void spawnEcho(Player victim, Player target) {
        Location location = victim.getLocation();
        Zombie echo = location.getWorld().spawn(location, Zombie.class);
        echo.setCustomName(Component.text(victim.getName() + "'s Echo", NamedTextColor.DARK_PURPLE));
        echo.setCustomNameVisible(true);
        echo.setPersistent(true);
        echo.setRemoveWhenFarAway(false);
        echo.setCanPickupItems(false);
        echo.getPersistentDataContainer().set(echoKey, PersistentDataType.BYTE, (byte) 1);

        AttributeInstance maxHealth = echo.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(ECHO_MAX_HEALTH);
            echo.setHealth(ECHO_MAX_HEALTH);
        }
        AttributeInstance attackDamage = echo.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attackDamage != null) {
            attackDamage.setBaseValue(ECHO_ATTACK_DAMAGE);
        }
        if (target.isOnline() && target.getWorld().equals(echo.getWorld())) {
            echo.setTarget(target);
        }

        UUID echoId = echo.getUniqueId();
        activeEchoes.put(echoId, echo);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Zombie expiringEcho = activeEchoes.remove(echoId);
            despawnTasks.remove(echoId);
            lastEchoDamager.remove(echoId);
            if (expiringEcho != null && expiringEcho.isValid()) {
                expiringEcho.remove();
            }
        }, ECHO_LIFETIME_TICKS);
        despawnTasks.put(echoId, task);
    }

    private boolean isEcho(Zombie zombie) {
        return zombie.getPersistentDataContainer().has(echoKey, PersistentDataType.BYTE);
    }

    private void cancelDespawnTask(UUID echoId) {
        BukkitTask task = despawnTasks.remove(echoId);
        if (task != null) {
            task.cancel();
        }
    }

    private static Player getPlayerAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    private static final class PlayerHit {
        private final UUID playerId;
        private final long timestamp;

        private PlayerHit(UUID playerId, long timestamp) {
            this.playerId = playerId;
            this.timestamp = timestamp;
        }
    }
}
