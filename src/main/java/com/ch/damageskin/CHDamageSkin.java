package com.ch.damageskin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public class CHDamageSkin extends JavaPlugin {

    private static CHDamageSkin inst;
    /** 치명타 표시 대기: 공격자 UUID + 피해자 UUID -> 만료 시각(ms) */
    private final java.util.Map<String, Long> crits = new java.util.concurrent.ConcurrentHashMap<>();
    private SkinManager skins;
    private DamageListener damageListener;

    @Override
    public void onEnable() {
        inst = this;
        saveDefaultConfig();
        skins = new SkinManager(this);
        skins.load();

        registerDamageListener();
        getServer().getPluginManager().registerEvents(new SkinListener(this), this);

        SkinCommand cmd = new SkinCommand(this);
        bind("데미지스킨", cmd);
        bind("데미지스킨관리", cmd);
        getLogger().info("CH-DamageSkin 활성화 (등록된 스킨 " + skins.fonts().size() + "개)");
    }

    @Override
    public void onDisable() {
        if (damageListener != null) damageListener.clearDisplays();
        if (skins != null) skins.save();
    }

    /**
     * 다른 플러그인/스크립트(skript-reflect)에서 호출: 이번 타격을 치명타로 표시.
     * 피해 이벤트가 처리될 때(같은 틱) 치명타 스타일로 숫자를 띄웁니다.
     */
    public static void markCrit(org.bukkit.entity.Player attacker, org.bukkit.entity.Entity victim) {
        if (inst == null || attacker == null || victim == null) return;
        if (inst.crits.size() > 256) {
            long now = System.currentTimeMillis();
            inst.crits.values().removeIf(t -> t < now);
        }
        inst.crits.put(attacker.getUniqueId() + ":" + victim.getUniqueId(), System.currentTimeMillis() + 500);
    }

    /** 표시된 치명타를 확인하고 소모 */
    boolean consumeCrit(java.util.UUID attacker, java.util.UUID victim) {
        Long t = crits.remove(attacker + ":" + victim);
        return t != null && t >= System.currentTimeMillis();
    }

    private void registerDamageListener() {
        damageListener = new DamageListener(this);
        getServer().getPluginManager().registerEvents(damageListener, this);
    }

    /** config.yml / 스킨 목록 다시 읽기 (packet-mode 포함) */
    public void reload() {
        skins.save();
        reloadConfig();
        skins.load();
        if (damageListener != null) {
            damageListener.clearDisplays();
            HandlerList.unregisterAll(damageListener);
        }
        registerDamageListener();
    }

    private void bind(String name, SkinCommand cmd) {
        PluginCommand pc = getCommand(name);
        if (pc == null) return;
        pc.setExecutor(cmd);
        pc.setTabCompleter(cmd);
    }

    public SkinManager skins() {
        return skins;
    }

    /** &색코드 문자열 -> 컴포넌트 */
    public static Component c(String legacy) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(legacy);
    }

    /** config 의 messages.<key> (없으면 기본값) 에서 {키} 치환 후 반환. kv = 키1, 값1, 키2, 값2 ... */
    public static String tr(String key, String def, String... kv) {
        String s = inst == null ? def : inst.getConfig().getString("messages." + key, def);
        for (int i = 0; i + 1 < kv.length; i += 2) s = s.replace("{" + kv[i] + "}", kv[i + 1]);
        return s;
    }

    public static String prefix() {
        return tr("prefix", "&d[데미지스킨] &f");
    }

    /** 접두사 + messages.<key> 메시지 전송 */
    public static void msg(CommandSender to, String key, String def, String... kv) {
        to.sendMessage(c(prefix() + tr(key, def, kv)));
    }

    /** 메시지 안의 {name} 을 스킨 이름 컴포넌트로 바꿔서 전송 */
    public static void msgSkin(CommandSender to, String key, String def, Component name, String... kv) {
        to.sendMessage(c(prefix() + tr(key, def, kv)).replaceText(
                TextReplacementConfig.builder().matchLiteral("{name}").replacement(name).build()));
    }

    /** 접두사 없이 {name} 치환 */
    public static void msgSkinPlain(CommandSender to, String key, String def, Component name, String... kv) {
        to.sendMessage(c(tr(key, def, kv)).replaceText(
                TextReplacementConfig.builder().matchLiteral("{name}").replacement(name).build()));
    }
}
