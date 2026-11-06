package com.ch.damageskin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class SkinListener implements Listener {

    private final CHDamageSkin plugin;

    public SkinListener(CHDamageSkin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------ 스킨 아이템 : 웅크리고 우클릭 = 등록

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getHand() != EquipmentSlot.HAND) return;
        ItemStack item = e.getItem();
        String font = plugin.skins().fontOf(item);
        if (font == null) return;

        e.setCancelled(true);
        Player p = e.getPlayer();
        if (!p.isSneaking()) {
            CHDamageSkin.msg(p, "&e웅크리고(Shift) 우클릭하면 데미지 스킨이 등록됩니다.");
            return;
        }
        SkinManager sm = plugin.skins();
        if (!sm.has(font)) {
            CHDamageSkin.msg(p, "&c삭제된 데미지 스킨입니다.");
            return;
        }
        UUID id = p.getUniqueId();
        if (sm.owns(id, font)) {
            CHDamageSkin.msg(p, "&c이미 보유한 데미지 스킨입니다.");
            return;
        }
        sm.own(id, font);
        int left = item.getAmount() - 1;
        if (left <= 0) {
            p.getInventory().setItemInMainHand(null);
        } else {
            ItemStack rest = item.clone();
            rest.setAmount(left);
            p.getInventory().setItemInMainHand(rest);
        }
        p.sendMessage(CHDamageSkin.c("&d[데미지스킨] &f").append(sm.nameComponent(font))
                .append(CHDamageSkin.c(" &f스킨이 등록되었습니다! &7(/데미지스킨 에서 적용)")));
    }

    // ------------------------------------------------------------ GUI

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof SkinGui gui)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) return;

        int slot = e.getRawSlot();
        if (slot == SkinGui.PREV && gui.page() > 0) {
            gui.setPage(gui.page() - 1);
            return;
        }
        if (slot == SkinGui.NEXT && gui.page() < gui.maxPage()) {
            gui.setPage(gui.page() + 1);
            return;
        }
        String font = gui.fontAt(slot);
        if (font == null) return;

        SkinManager sm = plugin.skins();
        if (font.isEmpty()) {
            sm.equip(p.getUniqueId(), null);
            CHDamageSkin.msg(p, "&f기본 데미지 스킨을 적용했습니다.");
        } else {
            if (!sm.has(font) || !sm.owns(p.getUniqueId(), font)) {
                CHDamageSkin.msg(p, "&c사용할 수 없는 스킨입니다.");
                gui.render();
                return;
            }
            sm.equip(p.getUniqueId(), font);
            p.sendMessage(CHDamageSkin.c("&d[데미지스킨] &f").append(sm.nameComponent(font))
                    .append(CHDamageSkin.c(" &f스킨을 적용했습니다.")));
        }
        gui.render();
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof SkinGui) e.setCancelled(true);
    }
}
