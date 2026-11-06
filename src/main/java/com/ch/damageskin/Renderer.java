package com.ch.damageskin;

import net.kyori.adventure.key.Key;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;

/** 데미지 숫자를 화면에 띄우는 방식 (패킷 / 엔티티) */
public interface Renderer {

    /**
     * @param others       때린 본인 외에 보는 유저 (비어 있을 수 있음)
     * @param otherOpacity others 에게 보이는 투명도 (0.0 ~ 1.0)
     */
    void show(Player attacker, Collection<Player> others, double otherOpacity,
              Location loc, long value, Key font, Anim anim);

    /** 남아 있는 숫자 정리 */
    void clear();
}
