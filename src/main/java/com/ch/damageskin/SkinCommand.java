package com.ch.damageskin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SkinCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = Arrays.asList("등록", "삭제", "목록", "받기");

    private final CHDamageSkin plugin;

    public SkinCommand(CHDamageSkin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equals("데미지스킨")) {
            if (!(sender instanceof Player p)) {
                CHDamageSkin.msg(sender, "&c게임 안에서만 사용할 수 있습니다.");
                return true;
            }
            new SkinGui(plugin, p, 0).open();
            return true;
        }
        return admin(sender, args);
    }

    private boolean admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chdamageskin.admin")) {
            CHDamageSkin.msg(sender, "&c권한이 없습니다.");
            return true;
        }
        if (args.length == 0) {
            usage(sender);
            return true;
        }
        SkinManager sm = plugin.skins();
        String sub = args[0];

        if (sub.equals("목록")) {
            if (sm.fonts().isEmpty()) {
                CHDamageSkin.msg(sender, "&7등록된 데미지 스킨이 없습니다.");
                return true;
            }
            CHDamageSkin.msg(sender, "&e등록된 데미지 스킨 (" + sm.fonts().size() + "개)");
            for (String f : sm.fonts()) {
                sender.sendMessage(CHDamageSkin.c("&7- &f" + f + " &8: ").append(sm.nameComponent(f)));
            }
            return true;
        }

        if (sub.equals("등록")) {
            if (args.length < 3) {
                CHDamageSkin.msg(sender, "&c사용법: /데미지스킨관리 등록 <폰트> <인게임에서 사용 될 이름>");
                return true;
            }
            String font = args[1];
            if (!SkinManager.validFont(font)) {
                CHDamageSkin.msg(sender, "&c폰트 이름은 json 파일 이름(.json 제외)이며 소문자/숫자/_ . - 만 쓸 수 있습니다.");
                return true;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                if (i > 2) sb.append(' ');
                sb.append(args[i]);
            }
            String name = sb.toString().replace('_', ' ');
            boolean exists = sm.has(font);
            sm.register(font, name);
            sender.sendMessage(CHDamageSkin.c("&d[데미지스킨] &f").append(sm.nameComponent(font))
                    .append(CHDamageSkin.c(exists ? " &f스킨 이름을 수정했습니다. &7(" + font + ")" : " &f스킨을 등록했습니다. &7(" + font + ")")));
            return true;
        }

        if (sub.equals("삭제")) {
            if (args.length < 2) {
                CHDamageSkin.msg(sender, "&c사용법: /데미지스킨관리 삭제 <폰트>");
                return true;
            }
            if (sm.remove(args[1])) {
                CHDamageSkin.msg(sender, "&e" + args[1] + " &f스킨을 삭제했습니다. &7(유저가 보유·장착한 기록도 함께 삭제)");
            } else {
                CHDamageSkin.msg(sender, "&c등록되지 않은 폰트입니다.");
            }
            return true;
        }

        if (sub.equals("받기")) {
            if (!(sender instanceof Player p)) {
                CHDamageSkin.msg(sender, "&c게임 안에서만 사용할 수 있습니다.");
                return true;
            }
            if (args.length < 2) {
                CHDamageSkin.msg(sender, "&c사용법: /데미지스킨관리 받기 <폰트> [개수]");
                return true;
            }
            String font = args[1];
            if (!sm.has(font)) {
                CHDamageSkin.msg(sender, "&c등록되지 않은 폰트입니다. 먼저 /데미지스킨관리 등록 으로 등록하세요.");
                return true;
            }
            int amount = 1;
            if (args.length >= 3) {
                try {
                    amount = Math.max(1, Math.min(64, Integer.parseInt(args[2])));
                } catch (NumberFormatException ex) {
                    CHDamageSkin.msg(sender, "&c개수는 숫자여야 합니다.");
                    return true;
                }
            }
            ItemStack item = sm.createItem(font, amount);
            Map<Integer, ItemStack> left = p.getInventory().addItem(item);
            for (ItemStack rest : left.values()) p.getWorld().dropItemNaturally(p.getLocation(), rest);
            p.sendMessage(CHDamageSkin.c("&d[데미지스킨] &f").append(sm.nameComponent(font))
                    .append(CHDamageSkin.c(" &f스킨 아이템을 지급받았습니다.")));
            return true;
        }

        usage(sender);
        return true;
    }

    private void usage(CommandSender s) {
        CHDamageSkin.msg(s, "&e/데미지스킨관리 등록 <폰트> <이름>  &7- 폰트 등록 (이름의 _ 는 띄어쓰기, &색코드 가능)");
        CHDamageSkin.msg(s, "&e/데미지스킨관리 받기 <폰트> [개수]  &7- 스킨 아이템 받기");
        CHDamageSkin.msg(s, "&e/데미지스킨관리 삭제 <폰트>  &7- 등록 삭제");
        CHDamageSkin.msg(s, "&e/데미지스킨관리 목록  &7- 등록된 스킨 목록");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> r = new ArrayList<>();
        if (!cmd.getName().equals("데미지스킨관리") || !sender.hasPermission("chdamageskin.admin")) return r;
        if (args.length == 1) {
            for (String s : SUBS) if (s.startsWith(args[0])) r.add(s);
        } else if (args.length == 2 && (args[0].equals("삭제") || args[0].equals("받기"))) {
            for (String f : plugin.skins().fonts()) if (f.startsWith(args[1])) r.add(f);
        }
        return r;
    }
}
