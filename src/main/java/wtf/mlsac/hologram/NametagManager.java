package wtf.mlsac.hologram;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import wtf.mlsac.Main;

import wtf.mlsac.checks.AICheck;
import wtf.mlsac.data.AIPlayerData;
import wtf.mlsac.scheduler.ScheduledTask;
import wtf.mlsac.scheduler.SchedulerManager;
import wtf.mlsac.util.ColorUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class NametagManager extends PacketListenerAbstract implements Listener {

    private final JavaPlugin plugin;
    private final AICheck aiCheck;
    private final Map<UUID, Integer> armorStandIds = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastSentText = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> viewersMap = new ConcurrentHashMap<>();
    private ScheduledTask task;
    private int cleanupCounter = 0;

    public NametagManager(JavaPlugin plugin, AICheck aiCheck) {
        super(PacketListenerPriority.NORMAL);
        this.plugin = plugin;
        this.aiCheck = aiCheck;
    }

    public void start() {
        org.bukkit.configuration.file.FileConfiguration config = ((Main) plugin).getHologramConfig().getConfig();
        if (!config.getBoolean("nametags.enabled", true))
            return;

        PacketEvents.getAPI().getEventManager().registerListener(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);

        task = SchedulerManager.getAdapter().runSyncRepeating(this::globalTick, 1L, 1L);
    }

    public void stop() {
        PacketEvents.getAPI().getEventManager().unregisterListener(this);
        if (task != null) {
            task.cancel();
            task = null;
        }

        for (UUID targetId : armorStandIds.keySet()) {
            despawnForall(targetId);
        }
        armorStandIds.clear();
        lastSentText.clear();
        viewersMap.clear();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_POSITION &&
                event.getPacketType() != PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION)
            return;

        Player player = event.getPlayer();
        if (player == null)
            return;

        Integer entityId = armorStandIds.get(player.getUniqueId());
        if (entityId == null)
            return;

        WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
        if (!flying.hasPositionChanged())
            return;

        Vector3d pos = flying.getLocation().getPosition();
        org.bukkit.configuration.file.FileConfiguration config = ((Main) plugin).getHologramConfig().getConfig();
        double yOffset = config.getDouble("nametags.height_offset", 2.5);

        WrapperPlayServerEntityTeleport teleport = new WrapperPlayServerEntityTeleport(
                entityId, new Vector3d(pos.getX(), pos.getY() + yOffset, pos.getZ()), 0f, 0f, false);

        Set<UUID> viewers = viewersMap.get(player.getUniqueId());
        if (viewers != null) {
            for (UUID viewerId : viewers) {
                Player viewer = Bukkit.getPlayer(viewerId);
                if (viewer != null && viewer.isOnline()) {
                    PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, teleport);
                }
            }
        }
    }

    private void globalTick() {
        List<Player> allPlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        List<Player> admins = new ArrayList<>();
        for (Player p : allPlayers) {
            if (p.hasPermission(wtf.mlsac.Permissions.ADMIN) || p.hasPermission(wtf.mlsac.Permissions.ALERTS)) {
                admins.add(p);
            }
        }

        if (admins.isEmpty()) {
            return;
        }

        for (Player target : allPlayers) {
            updateNametag(target, admins);
        }

        if (++cleanupCounter > 100) {
            cleanupCounter = 0;
            cleanupOfflineViewers();
        }
    }

    private void cleanupOfflineViewers() {
        armorStandIds.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        for (Set<UUID> viewers : viewersMap.values()) {
            viewers.removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        }
    }

    public void updateNametag(Player target, List<Player> admins) {
        AIPlayerData data = aiCheck.getPlayerData(target.getUniqueId());
        if (data == null)
            return;

        org.bukkit.configuration.file.FileConfiguration config = ((Main) plugin).getHologramConfig().getConfig();
        String format = config.getString("nametags.format", "&7AVG: &f{AVG} &8| {HISTORY}");

        double avgProb = data.getAverageProbability();
        List<Double> history = data.getProbabilityHistory();
        String historyStr = "-";
        if (!history.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Double val : history) {
                sb.append(getColorInfo(val)).append(" ");
            }
            historyStr = sb.toString().trim();
        }

        String displayText = format
                .replace("{AVG}", String.format("%.2f", avgProb))
                .replace("{HISTORY}", historyStr);

        int entityId = armorStandIds.computeIfAbsent(target.getUniqueId(),
                k -> ThreadLocalRandom.current().nextInt(1000000, 2000000));

        double yOffset = config.getDouble("nametags.height_offset", 2.5);
        Location loc = target.getLocation().add(0, yOffset, 0);

        String lastText = lastSentText.get(target.getUniqueId());
        boolean textChanged = !displayText.equals(lastText);

        for (Player viewer : admins) {
            if (viewer.getUniqueId().equals(target.getUniqueId()))
                continue;

            if (!viewer.getWorld().equals(target.getWorld()) ||
                    viewer.getLocation().distanceSquared(target.getLocation()) > 10000) {
                removeViewer(target.getUniqueId(), viewer);
                continue;
            }

            updateFor(target, viewer, entityId, loc, displayText, textChanged);
        }

        if (textChanged) {
            lastSentText.put(target.getUniqueId(), displayText);
        }
    }

    public void updateFor(Player target, Player viewer, int entityId, Location loc, String text, boolean textChanged) {
        Set<UUID> viewers = viewersMap.computeIfAbsent(target.getUniqueId(), k -> ConcurrentHashMap.newKeySet());
        boolean isNew = viewers.add(viewer.getUniqueId());

        if (isNew) {
            WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(
                    entityId, Optional.of(UUID.randomUUID()), EntityTypes.ARMOR_STAND,
                    new Vector3d(loc.getX(), loc.getY(), loc.getZ()), 0f, 0f, 0f, 0, Optional.empty());
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, spawn);
        }

        if (isNew || textChanged) {
            List<com.github.retrooper.packetevents.protocol.entity.data.EntityData<?>> metadata = getVersionedMetadata(
                    viewer, text);
            WrapperPlayServerEntityMetadata metadataPacket = new WrapperPlayServerEntityMetadata(entityId, metadata);
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, metadataPacket);
        }

        if (isNew) {
            WrapperPlayServerEntityTeleport teleport = new WrapperPlayServerEntityTeleport(
                    entityId, new Vector3d(loc.getX(), loc.getY(), loc.getZ()), 0f, 0f, false);
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, teleport);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        for (UUID targetId : viewersMap.keySet()) {
            removeViewer(targetId, event.getPlayer());
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getFrom().getWorld() != event.getTo().getWorld())
            return;
        if (event.getFrom().distanceSquared(event.getTo()) > 2500) {
            for (UUID targetId : viewersMap.keySet()) {
                removeViewer(targetId, event.getPlayer());
            }
        }
    }

    public void handlePlayerQuit(Player player) {
        despawnForall(player.getUniqueId());

        for (UUID targetId : viewersMap.keySet()) {
            removeViewer(targetId, player);
        }
    }

    private void removeViewer(UUID targetId, Player viewer) {
        Set<UUID> viewers = viewersMap.get(targetId);
        if (viewers != null && viewers.remove(viewer.getUniqueId())) {
            Integer entityId = armorStandIds.get(targetId);
            if (entityId != null) {
                WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(new int[] { entityId });
                PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, destroy);
            }
        }
    }

    private List<com.github.retrooper.packetevents.protocol.entity.data.EntityData<?>> getVersionedMetadata(
            Player viewer, String text) {
        List<com.github.retrooper.packetevents.protocol.entity.data.EntityData<?>> metadata = new ArrayList<>();
        int version = PacketEvents.getAPI().getPlayerManager().getClientVersion(viewer).getProtocolVersion();

        metadata.add(new com.github.retrooper.packetevents.protocol.entity.data.EntityData<Byte>(
                0, com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.BYTE, (byte) 0x20));

        String colorized = ColorUtil.colorize(text);

        if (version >= 393) {
            net.kyori.adventure.text.Component component = net.kyori.adventure.text.Component.text(colorized);
            metadata.add(new com.github.retrooper.packetevents.protocol.entity.data.EntityData<>(
                    2, com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.OPTIONAL_COMPONENT,
                    Optional.of(component)));

            metadata.add(new com.github.retrooper.packetevents.protocol.entity.data.EntityData<Boolean>(
                    3, com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.BOOLEAN, true));
        } else {
            metadata.add(new com.github.retrooper.packetevents.protocol.entity.data.EntityData<String>(
                    2, com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.STRING, colorized));

            metadata.add(new com.github.retrooper.packetevents.protocol.entity.data.EntityData<Boolean>(
                    3, com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.BOOLEAN, true));
        }

        int markerIndex = 10;
        if (version >= 766)
            markerIndex = 16;
        else if (version >= 755)
            markerIndex = 15;
        else if (version >= 448)
            markerIndex = 14;
        else if (version >= 385)
            markerIndex = 12;
        else if (version >= 107)
            markerIndex = 11;
        else
            markerIndex = 10;

        metadata.add(new com.github.retrooper.packetevents.protocol.entity.data.EntityData<Byte>(
                markerIndex, com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.BYTE, (byte) 0x10));

        return metadata;
    }

    private void despawnForall(UUID targetId) {
        Integer id = armorStandIds.remove(targetId);
        lastSentText.remove(targetId);
        Set<UUID> viewers = viewersMap.remove(targetId);

        if (id == null || viewers == null)
            return;

        WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(new int[] { id });
        for (UUID viewerId : viewers) {
            Player p = Bukkit.getPlayer(viewerId);
            if (p != null && p.isOnline())
                PacketEvents.getAPI().getPlayerManager().sendPacket(p, destroy);
        }
    }

    private String getColorInfo(double val) {
        if (val < 0.5)
            return "&a" + String.format("%.2f", val);
        if (val < 0.6)
            return "&6" + String.format("%.2f", val);
        if (val < 0.8)
            return "&c" + String.format("%.2f", val);
        if (val < 0.9)
            return "&4" + String.format("%.2f", val);
        return "&4&l" + String.format("%.2f", val);
    }
}
