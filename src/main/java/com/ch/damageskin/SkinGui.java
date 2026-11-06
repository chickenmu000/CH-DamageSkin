package com.ch.damageskin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** /데미지스킨 : 보유한 스킨 목록 (클릭 = 장착). 0번째 항목 = 기본 스킨 */
public class SkinGui implements InventoryHolder {

    public static final int PER_PAGE = 45;
    public static final int PREV = 45;
    public static final int NEXT = 53;

    private final CHDamageSkin plugin;
    private final Player player;
    private int page;
    /** 현재 페이지 슬롯 -> 폰트 ("" = 기본 스킨) */
    private final String[] slots = new String[PER_PAGE];
    private Inventory inv;

    public SkinGui(CHDamageSkin plugin, Player player, int page) {
        this.plugin = plugin;
        this.player = player;
        this.page = page;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public int page() {
        return page;
    }

    public String fontAt(int slot) {
        if (slot < 0 || slot >= PER_PAGE) return null;
        return slots[slot];
    }

    public void open() {
        inv = Bukkit.createInventory(this, 54, Component.text("데미지 스킨"));
        render();
        player.openInventory(inv);
    }

    public void setPage(int p) {
        this.page = p;
        render();
    }

    public int maxPage() {
        return Math.max(0, (list().size() - 1) / PER_PAGE);
    }

    private List<String> list() {
        List<String> all = new ArrayList<>();
        all.add("");
        all.addAll(plugin.skins().ownedFonts(player.getUniqueId()));
        return all;
    }

    public void render() {
        inv.clear();
        for (int i = 0; i < PER_PAGE; i++) slots[i] = null;

        SkinManager sm = plugin.skins();
        String eq = sm.equipped(player.getUniqueId());
        if (eq != null && (!sm.has(eq) || !sm.owns(player.getUniqueId(), eq))) eq = null;

        List<String> all = list();
        if (page > maxPage()) page = maxPage();
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < all.size(); i++) {
            String font = all.get(start + i);
            slots[i] = font;
            inv.setItem(i, icon(font, font.equals(eq == null ? "" : eq)));
        }

        if (page > 0) inv.setItem(PREV, nav("&a◀ 이전 페이지"));
        if (page < maxPage()) inv.setItem(NEXT, nav("&a다음 페이지 ▶"));
    }

    private ItemStack nav(String name) {
        ItemStack it = new ItemStack(Material.ARROW);
        ItemMeta m = it.getItemMeta();
        m.displayName(CHDamageSkin.c(name).decoration(TextDecoration.ITALIC, false));
        it.setItemMeta(m);
        return it;
    }

    private ItemStack icon(String font, boolean on) {
        SkinManager sm = plugin.skins();
        ItemStack it = new ItemStack(Material.PAPER);
        ItemMeta m = it.getItemMeta();
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        if (font.isEmpty()) {
            m.displayName(CHDamageSkin.c("&f기본 데미지 스킨").decoration(TextDecoration.ITALIC, false));
            String def = plugin.getConfig().getString("default-skin", "");
            if (def != null && !def.isEmpty() && SkinManager.validFont(def)) {
                lore.add(CHDamageSkin.c("&7미리보기 ").decoration(TextDecoration.ITALIC, false).append(sm.preview(def)));
            } else {
                lore.add(CHDamageSkin.c("&7스킨을 쓰지 않으면 데미지가 표시되지 않습니다.").decoration(TextDecoration.ITALIC, false));
            }
        } else {
            m.displayName(sm.nameComponent(font));
            lore.add(CHDamageSkin.c("&7미리보기 ").decoration(TextDecoration.ITALIC, false).append(sm.preview(font)));
        }
        lore.add(Component.empty());
        if (on) {
            lore.add(CHDamageSkin.c("&a✔ 적용 중").decoration(TextDecoration.ITALIC, false));
            m.addEnchant(Enchantment.UNBREAKING, 1, true);
            m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            lore.add(CHDamageSkin.c("&e▶ 클릭하여 적용").decoration(TextDecoration.ITALIC, false));
        }
        m.lore(lore);
        it.setItemMeta(m);
        return it;
    }
}
