package com.ch.damageskin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.Registry;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ProtocolLib 패킷으로 TextDisplay 를 보여줌 (서버에 엔티티가 생기지 않고, 보는 유저에게만 전송).
 * 유저마다 투명도를 다르게 보낼 수 있음.
 */
public class PacketRenderer implements Renderer {

    /** 실제 엔티티 ID 와 겹치지 않도록 큰 값에서 감소시키며 사용 */
    private static final AtomicInteger IDS = new AtomicInteger(2_000_000_000);

    private final CHDamageSkin plugin;
    private final ProtocolManager pm;
    private final Set<Live> active = new HashSet<>();

    // Display 메타데이터 인덱스 (1.21.4 기준, 1.21.2 미만은 10번 이상이 1씩 앞당겨짐)
    private final int iDelay = 8;
    private final int iDuration = 9;
    private final int iTranslation;
    private final int iScale;
    private final int iBillboard;
    private final int iText;
    private final int iBackground;
    private final int iOpacity;

    public PacketRenderer(CHDamageSkin plugin) {
        this.plugin = plugin;
        this.pm = ProtocolLibrary.getProtocolManager();
        int shift = atLeast(1, 21, 2) ? 0 : 1;
        iTranslation = 11 - shift;
        iScale = 12 - shift;
        iBillboard = 15 - shift;
        iText = 23 - shift;
        iBackground = 25 - shift;
        iOpacity = 26 - shift;
    }

    private static boolean atLeast(int major, int minor, int patch) {
        String v = Bukkit.getMinecraftVersion(); // 예: 1.21.4
        String[] a = v.split("\\.");
        int[] n = new int[3];
        for (int i = 0; i < 3 && i < a.length; i++) {
            try {
                n[i] = Integer.parseInt(a[i].replaceAll("\\D.*", ""));
            } catch (NumberFormatException ignored) {
            }
        }
        if (n[0] != major) return n[0] > major;
        if (n[1] != minor) return n[1] > minor;
        return n[2] >= patch;
    }

    @Override
    public void show(Player attacker, java.util.Collection<Player> others, double otherOpacity,
                     Location loc, long value, Key font, Anim anim) {
        List<Viewer> viewers = new ArrayList<>();
        viewers.add(new Viewer(attacker, (byte) -1));
        byte op = (byte) Math.round(Math.max(0, Math.min(1, otherOpacity)) * 255);
        for (Player p : others) viewers.add(new Viewer(p, op));
        anim.play(plugin, new Live(loc, value, font, anim, viewers));
    }

    @Override
    public void clear() {
        for (Live l : new HashSet<>(active)) l.destroy();
        active.clear();
    }

    private record Viewer(Player player, byte opacity) {
    }

    private final class Live implements Anim.Target {
        private final int id = IDS.decrementAndGet();
        private final UUID uuid = UUID.randomUUID();
        private final Location loc;
        private final long value;
        private final Key font;
        private final Anim anim;
        private final List<Viewer> viewers;

        Live(Location loc, long value, Key font, Anim anim, List<Viewer> viewers) {
            this.loc = loc;
            this.value = value;
            this.font = font;
            this.anim = anim;
            this.viewers = viewers;
        }

        private WrappedDataValue textValue(String hex) {
            Component c = Component.text(String.valueOf(value)).font(font).color(TextColor.fromHexString(hex));
            String json = GsonComponentSerializer.gson().serialize(c);
            return new WrappedDataValue(iText, Registry.getChatComponentSerializer(false),
                    WrappedChatComponent.fromJson(json).getHandle());
        }

        private WrappedDataValue val(int idx, Class<?> type, Object v) {
            return new WrappedDataValue(idx, Registry.get(type), v);
        }

        private void send(Viewer v, PacketContainer pkt) {
            if (!v.player().isOnline()) return;
            try {
                pm.sendServerPacket(v.player(), pkt);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        private PacketContainer metadata(List<WrappedDataValue> values) {
            PacketContainer pkt = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
            pkt.getIntegers().write(0, id);
            pkt.getDataValueCollectionModifier().write(0, values);
            return pkt;
        }

        @Override
        public void spawn() {
            PacketContainer spawn = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);
            spawn.getIntegers().write(0, id);
            spawn.getUUIDs().write(0, uuid);
            spawn.getEntityTypeModifier().write(0, EntityType.TEXT_DISPLAY);
            spawn.getDoubles().write(0, loc.getX()).write(1, loc.getY()).write(2, loc.getZ());

            for (Viewer v : viewers) {
                List<WrappedDataValue> d = new ArrayList<>();
                d.add(val(iDelay, Integer.class, 0));
                d.add(val(iDuration, Integer.class, 0));
                d.add(val(iScale, Vector3f.class, new Vector3f(anim.startScale)));
                d.add(val(iBillboard, Byte.class, (byte) 3)); // CENTER
                d.add(textValue(anim.startColor));
                d.add(val(iBackground, Integer.class, 0));
                d.add(val(iOpacity, Byte.class, v.opacity()));
                send(v, spawn);
                send(v, metadata(d));
            }
            active.add(this);
        }

        @Override
        public void apply(Anim.Frame f) {
            for (Viewer v : viewers) {
                List<WrappedDataValue> d = new ArrayList<>();
                d.add(val(iDelay, Integer.class, 0));
                d.add(val(iDuration, Integer.class, f.interp()));
                d.add(val(iTranslation, Vector3f.class, new Vector3f(0, f.rise(), 0)));
                d.add(val(iScale, Vector3f.class, new Vector3f(f.scale())));
                if (f.color() != null) d.add(textValue(f.color()));
                send(v, metadata(d));
            }
        }

        @Override
        public void destroy() {
            if (!active.remove(this)) return;
            PacketContainer pkt = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
            pkt.getIntLists().write(0, List.of(id));
            for (Viewer v : viewers) {
                try {
                    send(v, pkt);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
