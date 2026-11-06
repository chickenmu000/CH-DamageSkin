package com.ch.damageskin;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** 서버 TextDisplay 엔티티로 표시 (ProtocolLib 이 없을 때 / 패킷 방식 실패 시 대체) */
public class EntityRenderer implements Renderer {

    private final CHDamageSkin plugin;
    private final Set<TextDisplay> active = new HashSet<>();

    public EntityRenderer(CHDamageSkin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void show(Player attacker, Collection<Player> others, double otherOpacity,
                     Location loc, long value, Key font, Anim anim) {
        anim.play(plugin, new T(loc, value, font, anim, -1, java.util.List.of(attacker)));
        if (!others.isEmpty()) {
            byte op = (byte) Math.round(Math.max(0, Math.min(1, otherOpacity)) * 255);
            anim.play(plugin, new T(loc, value, font, anim, op, java.util.List.copyOf(others)));
        }
    }

    @Override
    public void clear() {
        for (TextDisplay td : new HashSet<>(active)) td.remove();
        active.clear();
    }

    private final class T implements Anim.Target {
        private final Location loc;
        private final long value;
        private final Key font;
        private final Anim anim;
        private final byte opacity;
        private final java.util.List<Player> viewers;
        private TextDisplay td;

        T(Location loc, long value, Key font, Anim anim, int opacity, java.util.List<Player> viewers) {
            this.loc = loc;
            this.value = value;
            this.font = font;
            this.anim = anim;
            this.opacity = (byte) opacity;
            this.viewers = viewers;
        }

        private Component text(String hex) {
            return Component.text(String.valueOf(value)).font(font).color(TextColor.fromHexString(hex));
        }

        private Transformation tf(float rise, float s) {
            return new Transformation(new Vector3f(0, rise, 0), new AxisAngle4f(), new Vector3f(s, s, s), new AxisAngle4f());
        }

        @Override
        public void spawn() {
            td = loc.getWorld().spawn(loc, TextDisplay.class, d -> {
                d.text(text(anim.startColor));
                d.setBillboard(Display.Billboard.CENTER);
                d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                d.setShadowed(false);
                d.setPersistent(false);
                d.setTextOpacity(opacity);
                d.setTransformation(tf(0, anim.startScale));
                d.setVisibleByDefault(false);
            });
            for (Player p : viewers) if (p.isOnline()) p.showEntity(plugin, td);
            active.add(td);
        }

        @Override
        public void apply(Anim.Frame f) {
            if (td == null || !td.isValid()) return;
            td.setInterpolationDelay(0);
            td.setInterpolationDuration(f.interp());
            td.setTransformation(tf(f.rise(), f.scale()));
            if (f.color() != null) td.text(text(f.color()));
        }

        @Override
        public void destroy() {
            if (td == null) return;
            td.remove();
            active.remove(td);
        }
    }
}
