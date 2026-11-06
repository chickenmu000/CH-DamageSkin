package com.ch.damageskin;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/** 데미지 숫자 애니메이션 타임라인 (커지고 -> 줄어들고 -> 위로 올라가며 색이 어두워짐) */
public final class Anim {

    /** at 틱에 interp 틱 동안 보간하며 scale/rise/color 로 변경. color 가 null 이면 색 유지 */
    public record Frame(int at, int interp, float scale, float rise, String color) {
    }

    /** 표시 대상 하나 (패킷 / 엔티티) */
    public interface Target {
        void spawn();

        void apply(Frame f);

        void destroy();
    }

    public final float startScale;
    public final String startColor;
    public final List<Frame> frames = new ArrayList<>();
    public final int endAt;

    private Anim(float startScale, String startColor, int endAt) {
        this.startScale = startScale;
        this.startColor = startColor;
        this.endAt = endAt;
    }

    public static Anim read(FileConfiguration c) {
        return read(c, false);
    }

    /** crit = true 이면 크기 배율(crit.scale-multiplier)과 색(crit.colors)을 치명타용으로 바꿈 */
    public static Anim read(FileConfiguration c, boolean crit) {
        float base = (float) c.getDouble("scale", 1.5);
        if (crit) base *= (float) c.getDouble("crit.scale-multiplier", 1.5);
        float start = (float) c.getDouble("animation.start-scale", 0.5) * base;
        float pop = (float) c.getDouble("animation.pop-scale", 1.35) * base;
        float settle = (float) c.getDouble("animation.settle-scale", 0.85) * base;
        float end = (float) c.getDouble("animation.end-scale", 0.75) * base;
        float rise = (float) c.getDouble("animation.rise", 1.5);
        int stay = Math.max(0, c.getInt("animation.stay-ticks", 5));
        int riseTicks = Math.max(1, c.getInt("animation.rise-ticks", 10));
        int fadeOut = Math.max(1, c.getInt("animation.fade-out-ticks", 5));

        List<String> colors = c.getStringList("animation.colors");
        if (crit) {
            List<String> cc = c.getStringList("crit.colors");
            if (!cc.isEmpty()) colors = cc;
        }
        String c0 = color(colors, 0, "#ffffff");
        String c1 = color(colors, 1, c0);
        String c2 = color(colors, 2, c1);

        int t2 = 4;
        int t3 = 7 + stay;
        int t4 = t3 + riseTicks;
        Anim a = new Anim(start, c0, t4 + fadeOut);
        a.frames.add(new Frame(1, 3, pop, 0f, c0));
        a.frames.add(new Frame(t2, 3, settle, 0f, c0));
        a.frames.add(new Frame(t3, riseTicks, end, rise, c1));
        a.frames.add(new Frame(t4, 0, end, rise, c2));
        return a;
    }

    private static String color(List<String> l, int i, String def) {
        if (i < l.size()) {
            String s = l.get(i);
            if (s != null && s.matches("#[0-9a-fA-F]{6}")) return s;
        }
        return def;
    }

    /** 타임라인 재생 */
    public void play(CHDamageSkin plugin, Target t) {
        t.spawn();
        for (Frame f : frames) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> t.apply(f), f.at());
        }
        Bukkit.getScheduler().runTaskLater(plugin, t::destroy, endAt);
    }
}
