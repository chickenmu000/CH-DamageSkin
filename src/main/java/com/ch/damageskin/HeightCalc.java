package com.ch.damageskin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** 숫자가 뜰 높이 계산. ModelEngine 모델이 있으면 모델 본(bone) 중 가장 높은 곳을 기준으로 함 (리플렉션, 실패 시 엔티티 높이) */
public class HeightCalc {

    private final CHDamageSkin plugin;
    private boolean warned;

    public HeightCalc(CHDamageSkin plugin) {
        this.plugin = plugin;
    }

    public double height(Entity e) {
        double base = e.getHeight();
        if (!plugin.getConfig().getBoolean("modelengine.enabled", true)) return base;
        if (!Bukkit.getPluginManager().isPluginEnabled("ModelEngine")) return base;
        try {
            double me = modelHeight(e) * plugin.getConfig().getDouble("modelengine.height-scale", 1.0);
            return Math.max(base, me);
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                plugin.getLogger().warning("ModelEngine 높이 계산 실패 (엔티티 높이 사용): " + t);
            }
            return base;
        }
    }

    private double modelHeight(Entity e) throws Exception {
        Class<?> api = Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");
        Object modeled = null;
        for (Method m : api.getMethods()) {
            if (!m.getName().equals("getModeledEntity") || m.getParameterCount() != 1) continue;
            Class<?> pt = m.getParameterTypes()[0];
            if (pt == UUID.class) modeled = m.invoke(null, e.getUniqueId());
            else if (pt.isAssignableFrom(e.getClass())) modeled = m.invoke(null, e);
            else continue;
            break;
        }
        if (modeled == null) return 0;

        Object models = call(modeled, "getModels");
        Collection<?> list = models instanceof Map<?, ?> mp ? mp.values() : (Collection<?>) models;
        double max = 0;
        for (Object active : list) {
            Object bp = call(active, "getBlueprint");
            Object bones = call(bp, "getBones");
            Collection<?> bl = bones instanceof Map<?, ?> mp ? mp.values() : (Collection<?>) bones;
            for (Object bone : bl) max = Math.max(max, boneMax(bone));
        }
        return max;
    }

    private double boneMax(Object bone) throws Exception {
        double max = boneY(bone);
        Object ch = null;
        try {
            ch = call(bone, "getChildren");
        } catch (NoSuchMethodException ignored) {
        }
        if (ch != null) {
            Collection<?> cl = ch instanceof Map<?, ?> mp ? mp.values() : (Collection<?>) ch;
            for (Object c : cl) max = Math.max(max, boneMax(c));
        }
        return max;
    }

    private double boneY(Object bone) throws Exception {
        Object vec;
        try {
            vec = call(bone, "getGlobalPosition");
        } catch (NoSuchMethodException ex) {
            vec = call(bone, "getGlobalOrigin");
        }
        for (String n : new String[]{"getY", "y"}) {
            try {
                return ((Number) call(vec, n)).doubleValue();
            } catch (NoSuchMethodException ignored) {
            }
        }
        Field f = vec.getClass().getField("y");
        return ((Number) f.get(vec)).doubleValue();
    }

    private static Object call(Object o, String name) throws Exception {
        Method m = o.getClass().getMethod(name);
        m.setAccessible(true);
        return m.invoke(o);
    }
}
