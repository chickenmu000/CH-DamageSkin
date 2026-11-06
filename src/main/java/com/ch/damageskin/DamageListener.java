package com.ch.damageskin;

import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** 플레이어가 몹에게 준 피해를 숫자 스킨으로 띄움 */
public class DamageListener implements Listener {

    private final CHDamageSkin plugin;
    private final EntityRenderer entityRenderer;
    private final HeightCalc height;
    private Renderer packetRenderer;

    public DamageListener(CHDamageSkin plugin) {
        this.plugin = plugin;
        this.entityRenderer = new EntityRenderer(plugin);
        this.height = new HeightCalc(plugin);

        if (plugin.getConfig().getBoolean("packet-mode", true)) {
            if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
                try {
                    packetRenderer = new PacketRenderer(plugin);
                    plugin.getLogger().info("패킷 방식으로 데미지를 표시합니다. (ProtocolLib)");
                } catch (Throwable t) {
                    plugin.getLogger().warning("패킷 방식 준비 실패 - 엔티티 방식 사용: " + t);
                }
            } else {
                plugin.getLogger().warning("packet-mode 가 켜져 있지만 ProtocolLib 이 없어 엔티티 방식으로 표시합니다.");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof LivingEntity victim)) return;
        if (victim instanceof Player || victim instanceof ArmorStand) return;

        Player attacker = null;
        if (e.getDamager() instanceof Player direct) {
            attacker = direct;
        } else if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player shooter) {
            attacker = shooter;
        }
        if (attacker == null) return;

        double dmg = e.getFinalDamage();
        if (dmg <= 0) return;

        String font = plugin.skins().activeFont(attacker.getUniqueId());
        if (font == null) return;
        Key key = SkinManager.fontKey(font);

        if (plugin.getConfig().getBoolean("actual-damage-mode", false)) {
            // 피해가 적용된 뒤(1틱 후) 체력 차이로 실제 피해량 계산
            double before = victim.getHealth();
            Player atk = attacker;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                double after = (victim.isDead() || !victim.isValid()) ? 0 : victim.getHealth();
                long actual = Math.round(before - after);
                if (actual <= 0) return;
                show(atk, victim, actual, key);
            }, 1L);
        } else {
            show(attacker, victim, Math.max(1, Math.round(dmg)), key);
        }
    }

    private void show(Player attacker, LivingEntity victim, long value, Key font) {
        var cfg = plugin.getConfig();
        double off = cfg.getDouble("random-offset", 0.4);
        double yOff = cfg.getDouble("offset", 0.0);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        Location base = victim.isValid() ? victim.getLocation() : attacker.getLocation();
        Location loc = base.clone().add(
                (rnd.nextDouble() - 0.5) * 2 * off,
                height.height(victim) + 0.1 + yOff,
                (rnd.nextDouble() - 0.5) * 2 * off);

        List<Player> others = new ArrayList<>();
        double opacity = cfg.getDouble("other-opacity", 0.5);
        if (!cfg.getBoolean("only-attacker", true) && opacity > 0) {
            double range = cfg.getDouble("view-distance", 24.0);
            for (Player p : loc.getWorld().getPlayers()) {
                if (p.equals(attacker)) continue;
                if (p.getLocation().distanceSquared(loc) <= range * range) others.add(p);
            }
        }

        Anim anim = Anim.read(cfg);
        if (packetRenderer != null) {
            try {
                packetRenderer.show(attacker, others, opacity, loc, value, font, anim);
                return;
            } catch (Throwable t) {
                plugin.getLogger().warning("패킷 표시 실패 - 이후 엔티티 방식으로 전환합니다: " + t);
                t.printStackTrace();
                packetRenderer.clear();
                packetRenderer = null;
            }
        }
        entityRenderer.show(attacker, others, opacity, loc, value, font, anim);
    }

    /** 플러그인 종료 시 남은 숫자 제거 */
    public void clearDisplays() {
        entityRenderer.clear();
        if (packetRenderer != null) packetRenderer.clear();
    }
}
