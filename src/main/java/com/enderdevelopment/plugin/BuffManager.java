package com.enderdevelopment.plugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

final class BuffManager {
    private static final Map<String, PotionEffectType> EFFECTS = createEffects();

    private final Main plugin;
    private final NamespacedKey activeBuffKey;
    private final Map<String, Buff> buffs = new LinkedHashMap<>();

    BuffManager(Main plugin) {
        this.plugin = plugin;
        this.activeBuffKey = new NamespacedKey(plugin, "active_buff");
        reloadFromConfig();
    }

    void reloadFromConfig() {
        buffs.clear();
        for (Map.Entry<String, PotionEffectType> entry : EFFECTS.entrySet()) {
            String id = entry.getKey();
            String path = "buffs." + id;
            int amplifier = Math.max(0, plugin.getConfig().getInt(path + ".amplifier", 0));
            boolean enabled = plugin.getConfig().getBoolean(path + ".enabled", true);
            buffs.put(id, new Buff(id, entry.getValue(), amplifier, enabled));
        }
    }

    List<Buff> getBuffs() {
        return List.copyOf(buffs.values());
    }

    Buff getBuff(String id) {
        return buffs.get(id.toLowerCase(java.util.Locale.ROOT));
    }

    boolean setEnabled(String id, boolean enabled) {
        Buff buff = getBuff(id);
        if (buff == null) {
            return false;
        }
        plugin.getConfig().set("buffs." + buff.id + ".enabled", enabled);
        plugin.saveConfig();
        reloadFromConfig();
        return true;
    }

    String applyRandomBuff(Player player) {
        List<Buff> enabledBuffs = new ArrayList<>();
        for (Buff buff : buffs.values()) {
            if (buff.enabled) {
                enabledBuffs.add(buff);
            }
        }
        if (enabledBuffs.isEmpty()) {
            return null;
        }

        Buff selected = enabledBuffs.get(ThreadLocalRandom.current().nextInt(enabledBuffs.size()));
        clearBuff(player);
        player.addPotionEffect(new PotionEffect(selected.effect, Integer.MAX_VALUE, selected.amplifier, false, true, true), true);
        player.getPersistentDataContainer().set(activeBuffKey, PersistentDataType.STRING, selected.id);
        return selected.id;
    }

    void clearBuff(Player player) {
        String activeId = player.getPersistentDataContainer().get(activeBuffKey, PersistentDataType.STRING);
        if (activeId != null) {
            Buff buff = getBuff(activeId);
            if (buff != null) {
                player.removePotionEffect(buff.effect);
            }
            player.getPersistentDataContainer().remove(activeBuffKey);
        }
    }

    private static Map<String, PotionEffectType> createEffects() {
        Map<String, PotionEffectType> effects = new LinkedHashMap<>();
        effects.put("speed", PotionEffectType.SPEED);
        effects.put("strength", PotionEffectType.STRENGTH);
        effects.put("haste", PotionEffectType.HASTE);
        effects.put("jump_boost", PotionEffectType.JUMP_BOOST);
        effects.put("regeneration", PotionEffectType.REGENERATION);
        effects.put("resistance", PotionEffectType.RESISTANCE);
        effects.put("fire_resistance", PotionEffectType.FIRE_RESISTANCE);
        effects.put("water_breathing", PotionEffectType.WATER_BREATHING);
        effects.put("night_vision", PotionEffectType.NIGHT_VISION);
        effects.put("health_boost", PotionEffectType.HEALTH_BOOST);
        effects.put("absorption", PotionEffectType.ABSORPTION);
        return effects;
    }

    static final class Buff {
        final String id;
        final PotionEffectType effect;
        final int amplifier;
        final boolean enabled;

        Buff(String id, PotionEffectType effect, int amplifier, boolean enabled) {
            this.id = id;
            this.effect = effect;
            this.amplifier = amplifier;
            this.enabled = enabled;
        }
    }
}
