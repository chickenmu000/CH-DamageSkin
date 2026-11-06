package com.ch.damageskin;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** 플레이어가 몹에게 준 피해를 TextDisplay 숫자로 띄움 */
public class DamageListener implements Listener {

    private final CHDamageSkin plugin;
    private final Set<TextDisplay> active = new HashSet<>();

    public DamageListener(CHDamageSkin plugin) {
        this.plugin = plugin;
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
        long value = Math.max(1, Math.round(dmg));

        String font = plugin.skins().activeFont(attacker.getUniqueId());
        if (font == null) return;

        show(attacker, victim, value, SkinManager.fontKey(font));
    }

    private void show(Player attacker, LivingEntity victim, long value, Key font) {
        var cfg = plugin.getConfig();
        int dur = Math.max(5, cfg.getInt("duration-ticks", 25));
        float rise = (float) cfg.getDouble("rise", 1.2);
        float scale = (float) cfg.getDouble("scale", 1.5);
        double off = cfg.getDouble("random-offset", 0.4);
        boolean onlyAttacker = cfg.getBoolean("only-attacker", true);

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Location loc = victim.getLocation().add(
                (rnd.nextDouble() - 0.5) * 2 * off,
                victim.getHeight() + 0.1,
                (rnd.nextDouble() - 0.5) * 2 * off);

        Component text = Component.text(String.valueOf(value)).font(font).color(NamedTextColor.WHITE);
        Vector3f scaleVec = new Vector3f(scale, scale, scale);

        TextDisplay td = victim.getWorld().spawn(loc, TextDisplay.class, d -> {
            d.text(text);
            d.setBillboard(Display.Billboard.CENTER);
            d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            d.setShadowed(false);
            d.setPersistent(false);
            d.setInterpolationDuration(dur);
            d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), scaleVec, new AxisAngle4f()));
            if (onlyAttacker) d.setVisibleByDefault(false);
        });
        if (onlyAttacker) attacker.showEntity(plugin, td);
        active.add(td);

        // 1틱 뒤에 위로 올라가는 보간 시작
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!td.isValid()) return;
            td.setInterpolationDelay(0);
            td.setInterpolationDuration(dur);
            td.setTransformation(new Transformation(new Vector3f(0, rise, 0), new AxisAngle4f(), scaleVec, new AxisAngle4f()));
        }, 1L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            td.remove();
            active.remove(td);
        }, dur + 2L);
    }

    /** 플러그인 종료 / 리로드 시 남은 숫자 제거 */
    public void clearDisplays() {
        for (TextDisplay td : new HashSet<>(active)) td.remove();
        active.clear();
    }
}
