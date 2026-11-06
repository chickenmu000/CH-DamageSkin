package com.ch.damageskin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class CHDamageSkin extends JavaPlugin {

    private SkinManager skins;
    private DamageListener damageListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        skins = new SkinManager(this);
        skins.load();

        damageListener = new DamageListener(this);
        getServer().getPluginManager().registerEvents(damageListener, this);
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

    public static void msg(CommandSender to, String legacy) {
        to.sendMessage(c("&d[데미지스킨] &f" + legacy));
    }
}
