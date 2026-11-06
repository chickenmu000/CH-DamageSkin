package com.ch.damageskin;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/** 스킨 목록(폰트 -> 이름) / 유저별 보유·장착 스킨 저장 */
public class SkinManager {

    public static final String PREVIEW = "1234567890";
    private static final Pattern FONT_RE = Pattern.compile("[a-z0-9._/\\-:]+");

    private final CHDamageSkin plugin;
    private final NamespacedKey itemKey;
    private final File skinFile;
    private final File playerFile;

    private final Map<String, String> skins = new LinkedHashMap<>();
    private final Map<UUID, Set<String>> owned = new HashMap<>();
    private final Map<UUID, String> equipped = new HashMap<>();

    public SkinManager(CHDamageSkin plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "skin");
        this.skinFile = new File(plugin.getDataFolder(), "skins.yml");
        this.playerFile = new File(plugin.getDataFolder(), "players.yml");
    }

    // ------------------------------------------------------------ 저장 / 불러오기

    public void load() {
        skins.clear();
        owned.clear();
        equipped.clear();
        if (skinFile.exists()) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(skinFile);
            var sec = y.getConfigurationSection("skins");
            if (sec != null) {
                for (String k : sec.getKeys(false)) skins.put(k, sec.getString(k, k));
            }
        }
        if (playerFile.exists()) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(playerFile);
            var sec = y.getConfigurationSection("players");
            if (sec != null) {
                for (String u : sec.getKeys(false)) {
                    UUID id;
                    try {
                        id = UUID.fromString(u);
                    } catch (IllegalArgumentException ex) {
                        continue;
                    }
                    owned.put(id, new LinkedHashSet<>(sec.getStringList(u + ".owned")));
                    String eq = sec.getString(u + ".equipped");
                    if (eq != null && !eq.isEmpty()) equipped.put(id, eq);
                }
            }
        }
    }

    public void save() {
        try {
            plugin.getDataFolder().mkdirs();
            YamlConfiguration s = new YamlConfiguration();
            for (Map.Entry<String, String> e : skins.entrySet()) s.set("skins." + e.getKey(), e.getValue());
            s.save(skinFile);

            YamlConfiguration p = new YamlConfiguration();
            for (Map.Entry<UUID, Set<String>> e : owned.entrySet()) {
                String u = e.getKey().toString();
                p.set("players." + u + ".owned", new ArrayList<>(e.getValue()));
                String eq = equipped.get(e.getKey());
                if (eq != null) p.set("players." + u + ".equipped", eq);
            }
            p.save(playerFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("데이터 저장 실패: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------ 스킨 목록

    /** 폰트 이름이 올바른 형식인지 (소문자/숫자/_ . - / :) */
    public static boolean validFont(String font) {
        if (font == null || !FONT_RE.matcher(font).matches()) return false;
        try {
            fontKey(font);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /** json 파일 이름(확장자 제외) -> 폰트 키. 네임스페이스가 없으면 minecraft */
    public static Key fontKey(String font) {
        if (font.contains(":")) return Key.key(font);
        return Key.key("minecraft", font);
    }

    public boolean has(String font) {
        return skins.containsKey(font);
    }

    public void register(String font, String legacyName) {
        skins.put(font, legacyName);
        save();
    }

    public boolean remove(String font) {
        if (skins.remove(font) == null) return false;
        for (Set<String> s : owned.values()) s.remove(font);
        equipped.values().removeIf(font::equals);
        save();
        return true;
    }

    public List<String> fonts() {
        return new ArrayList<>(skins.keySet());
    }

    public String name(String font) {
        return skins.getOrDefault(font, font);
    }

    // ------------------------------------------------------------ 유저 데이터

    public boolean owns(UUID id, String font) {
        Set<String> s = owned.get(id);
        return s != null && s.contains(font);
    }

    public void own(UUID id, String font) {
        owned.computeIfAbsent(id, k -> new LinkedHashSet<>()).add(font);
        save();
    }

    /** 보유 중이면서 아직 등록돼 있는 스킨 목록 */
    public List<String> ownedFonts(UUID id) {
        List<String> r = new ArrayList<>();
        Set<String> s = owned.get(id);
        if (s == null) return r;
        for (String f : s) if (skins.containsKey(f)) r.add(f);
        return r;
    }

    public String equipped(UUID id) {
        return equipped.get(id);
    }

    /** font == null 이면 장착 해제(기본 스킨) */
    public void equip(UUID id, String font) {
        if (font == null) equipped.remove(id);
        else equipped.put(id, font);
        save();
    }

    /** 이 유저의 데미지에 실제로 쓸 폰트 (없으면 null = 표시 안 함) */
    public String activeFont(UUID id) {
        String eq = equipped.get(id);
        if (eq != null && skins.containsKey(eq) && owns(id, eq)) return eq;
        String def = plugin.getConfig().getString("default-skin", "");
        if (def == null || def.isEmpty()) return null;
        if (!validFont(def)) return null;
        return def;
    }

    // ------------------------------------------------------------ 표시용 컴포넌트 / 아이템

    public Component nameComponent(String font) {
        return CHDamageSkin.c(name(font)).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public Component preview(String font) {
        return Component.text(PREVIEW)
                .font(fontKey(font))
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false);
    }

    public ItemStack createItem(String font, int amount) {
        ItemStack it = new ItemStack(Material.PAPER, Math.max(1, amount));
        ItemMeta m = it.getItemMeta();
        m.displayName(CHDamageSkin.c("&6[데미지 스킨] ").decoration(TextDecoration.ITALIC, false)
                .append(nameComponent(font)));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(CHDamageSkin.c("&7미리보기 ").decoration(TextDecoration.ITALIC, false).append(preview(font)));
        lore.add(Component.empty());
        lore.add(CHDamageSkin.c("&e웅크리고 우클릭하여 등록합니다.").decoration(TextDecoration.ITALIC, false));
        lore.add(CHDamageSkin.c("&7등록 후 &f/데미지스킨&7 에서 적용할 수 있습니다.").decoration(TextDecoration.ITALIC, false));
        m.lore(lore);
        m.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, font);
        it.setItemMeta(m);
        return it;
    }

    /** 스킨 아이템이면 폰트 이름, 아니면 null */
    public String fontOf(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        return it.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
    }
}
