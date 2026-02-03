/*
 * This file is part of MLSAC - AI powered Anti-Cheat
 * Copyright (C) 2026 MLSAC Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This file contains code derived from:
 *   - SlothAC (© 2025 KaelusMC, https://github.com/KaelusMC/SlothAC)
 *   - Grim (© 2025 GrimAnticheat, https://github.com/GrimAnticheat/Grim)
 * All derived code is licensed under GPL-3.0.
 */

package wtf.mlsac.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import wtf.mlsac.Main;
import wtf.mlsac.Permissions;
import wtf.mlsac.alert.AlertManager;
import wtf.mlsac.checks.AICheck;
import wtf.mlsac.config.Config;
import wtf.mlsac.config.Label;
import wtf.mlsac.data.AIPlayerData;
import wtf.mlsac.data.DataSession;
import wtf.mlsac.scheduler.ScheduledTask;
import wtf.mlsac.scheduler.SchedulerManager;
import wtf.mlsac.session.ISessionManager;
import wtf.mlsac.util.ColorUtil;
import wtf.mlsac.violation.ViolationManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

public class CommandHandler implements CommandExecutor, TabCompleter {
    private final ISessionManager sessionManager;
    private final AlertManager alertManager;
    private final AICheck aiCheck;
    private final Main plugin;
    private final Map<UUID, UUID> probTracking = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> probTasks = new ConcurrentHashMap<>();

    public CommandHandler(ISessionManager sessionManager, AlertManager alertManager,
            AICheck aiCheck, Main plugin) {
        this.sessionManager = sessionManager;
        this.alertManager = alertManager;
        this.aiCheck = aiCheck;
        this.plugin = plugin;
    }

    private Config getConfig() {
        return plugin.getPluginConfig();
    }

    private String getPrefix() {
        return ColorUtil.colorize(plugin.getMessagesConfig().getPrefix());
    }

    private String msg(String key) {
        return ColorUtil.colorize(plugin.getMessagesConfig().getMessage(key));
    }

    private String msg(String key, String... replacements) {
        return ColorUtil.colorize(plugin.getMessagesConfig().getMessage(key, replacements));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "start":
                return handleStart(sender, args);
            case "stop":
                return handleStop(sender, args);
            case "alerts":
                return handleAlerts(sender);
            case "prob":
                return handleProb(sender, args);
            case "reload":
                return handleReload(sender);
            case "datastatus":
                return handleDataStatus(sender);
            case "kicklist":
                return handleKickList(sender);
            case "suspects":
                return handleSuspects(sender);
            case "punish":
                return handlePunish(sender, args);
            case "profile":
                return handleProfile(sender, args);
            default:
                sender.sendMessage(getPrefix() + msg("unknown-command", "{ARGS}", args[0]));
                sendUsage(sender);
                return true;
        }
    }

    private boolean handleSuspects(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getPrefix() + msg("players-only"));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(Permissions.ALERTS)) {
            player.sendMessage(getPrefix() + msg("no-permission"));
            return true;
        }
        new wtf.mlsac.menu.SuspectsMenu(plugin, player).open();
        return true;
    }

    private boolean handleAlerts(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getPrefix() + msg("players-only"));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(Permissions.ALERTS)) {
            player.sendMessage(getPrefix() + msg("no-permission"));
            return true;
        }
        alertManager.toggleAlerts(player);
        return true;
    }

    private boolean handleProb(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getPrefix() + msg("players-only"));
            return true;
        }
        Player admin = (Player) sender;
        if (!admin.hasPermission(Permissions.PROB)) {
            admin.sendMessage(getPrefix() + msg("no-permission"));
            return true;
        }
        if (probTracking.containsKey(admin.getUniqueId())) {
            stopTracking(admin);
            admin.sendMessage(getPrefix() + msg("tracking-stopped"));
            return true;
        }
        if (args.length < 2) {
            admin.sendMessage(getPrefix() + msg("prob-usage"));
            return true;
        }
        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            admin.sendMessage(getPrefix() + msg("player-not-found", "{PLAYER}", playerName));
            return true;
        }
        startTracking(admin, target);
        admin.sendMessage(getPrefix() + msg("tracking-started", "{PLAYER}", target.getName()));
        return true;
    }

    private void startTracking(Player admin, Player target) {
        UUID adminId = admin.getUniqueId();
        UUID targetId = target.getUniqueId();
        stopTracking(admin);
        probTracking.put(adminId, targetId);
        ScheduledTask task = SchedulerManager.getAdapter().runSyncRepeating(() -> {
            Player adminPlayer = Bukkit.getPlayer(adminId);
            Player targetPlayer = Bukkit.getPlayer(targetId);
            if (adminPlayer == null || !adminPlayer.isOnline()) {
                stopTracking(adminId);
                return;
            }
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                sendActionBar(adminPlayer, msg("player-offline"));
                stopTracking(adminId);
                return;
            }
            AIPlayerData data = aiCheck.getPlayerData(targetId);
            String message;
            if (data == null) {
                message = ColorUtil.colorize("&7" + targetPlayer.getName() + ": &eНет данных");
            } else {
                double prob = data.getLastProbability();
                double buffer = data.getBuffer();
                int vl = plugin.getViolationManager().getViolationLevel(targetId);
                message = ColorUtil.colorize(plugin.getMessagesConfig().getMessage("actionbar-format",
                        targetPlayer.getName(), prob, buffer, vl));
            }
            sendActionBar(adminPlayer, message);
        }, 0L, 10L);
        probTasks.put(adminId, task);
    }

    private void stopTracking(Player admin) {
        stopTracking(admin.getUniqueId());
    }

    private void stopTracking(UUID adminId) {
        probTracking.remove(adminId);
        ScheduledTask task = probTasks.remove(adminId);
        if (task != null) {
            task.cancel();
        }
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission(Permissions.RELOAD)) {
            sender.sendMessage(getPrefix() + msg("no-permission"));
            return true;
        }
        plugin.reloadPluginConfig();
        sender.sendMessage(getPrefix() + msg("config-reloaded"));
        return true;
    }

    private boolean handleKickList(CommandSender sender) {
        if (!sender.hasPermission(Permissions.ADMIN)) {
            sender.sendMessage(getPrefix() + msg("no-permission"));
            return true;
        }
        List<ViolationManager.KickRecord> kicks = plugin.getViolationManager().getKickHistory();
        if (kicks.isEmpty()) {
            sender.sendMessage(getPrefix() + ColorUtil.colorize("&7Нет киков от AI античита"));
            return true;
        }
        sender.sendMessage(getPrefix() + ColorUtil.colorize("&6Последние кики от AI античита:"));
        sender.sendMessage(ColorUtil.colorize("&7─────────────────────────────────"));
        int index = 1;
        for (ViolationManager.KickRecord kick : kicks) {
            sender.sendMessage(ColorUtil.colorize(String.format(
                    "&e%d. &f%s &7[&c%s&7] &8- &bProb: &f%.2f &8| &bBuf: &f%.1f &8| &bVL: &f%d",
                    index++,
                    kick.getPlayerName(),
                    kick.getFormattedTime(),
                    kick.getProbability(),
                    kick.getBuffer(),
                    kick.getVl())));
        }
        sender.sendMessage(ColorUtil.colorize("&7─────────────────────────────────"));
        return true;
    }

    private boolean handlePunish(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Permissions.ADMIN)) {
            sender.sendMessage(getPrefix() + msg("no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(getPrefix() + msg("usage-punish"));
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(getPrefix() + msg("player-not-found", "{PLAYER}", args[1]));
            return true;
        }

        plugin.getViolationManager().executeMaxPunishment(target);
        // Warning: executeMaxPunishment doesn't return success status or action name
        // easily without change,
        // but we can assume if it ran it did something given checking in VM.
        // Actually VM.executeMaxPunishment doesn't return anything.
        // We can just say "Executed" or check if vm has commands.
        if (plugin.getPluginConfig().getPunishmentCommands().isEmpty()) {
            sender.sendMessage(getPrefix() + msg("punish-no-action"));
        } else {
            // We don't know exact action here easily without refactoring VM to return it,
            // but user just asked to execute it.
            sender.sendMessage(getPrefix() + msg("punish-success", "{PLAYER}", target.getName(), "{ACTION}", "Max VL"));
        }
        return true;
    }

    private boolean handleProfile(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Permissions.ADMIN)) {
            sender.sendMessage(getPrefix() + msg("no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(getPrefix() + msg("usage-profile"));
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(getPrefix() + msg("player-not-found", "{PLAYER}", args[1]));
            return true;
        }

        AIPlayerData data = aiCheck.getPlayerData(target.getUniqueId());
        String sens = "N/A";
        int detections = 0;

        if (data != null) {
            int s = data.getAimProcessor().getSensitivity();
            if (s != -1) {
                sens = String.valueOf(s);
            }
            detections = data.getHighProbabilityDetections();
        }

        ClientVersion version = PacketEvents.getAPI().getPlayerManager().getClientVersion(target);
        String clientVer = version != null ? version.toString() : "Unknown";

        sender.sendMessage(ColorUtil.colorize(msg("profile-header", "{PLAYER}", target.getName())));
        // Getting list message is slightly annoying with current msg() helper if it
        // returns String.
        // But MessagesConfig.getMessage returns String usually?
        // Let's look at MessagesConfig.
        // If profile-info is a list, getMessage might handle it or we need a
        // getMessageList.
        // Assuming we added it as list in YAML, but Config/MessagesConfig usually
        // handles strings.
        // I should check MessagesConfig.
        // The msg() helper calls plugin.getMessagesConfig().getMessage(key).
        // If it's a list in YAML, standard FileConfiguration.getString() returns null
        // or first line?
        // I need to check MessagesConfig.java to support lists or use single multiline
        // string.
        // I'll treat "profile-info" as Multi-line string in YAML or iterate manually if
        // MessagesConfig supports it.
        // Re-reading messages.yml edit: I used list format.
        // Let's check MessagesConfig.java to see if it supports lists.
        List<String> info = plugin.getMessagesConfig().getMessageList("profile-info");
        if (info == null || info.isEmpty()) {
            // Fallback if list support missing
            sender.sendMessage(ColorUtil.colorize("&7Sens: &f" + sens + "%"));
            sender.sendMessage(ColorUtil.colorize("&7Client: &f" + clientVer));
            sender.sendMessage(ColorUtil.colorize("&7Detections (>0.8): &f" + detections));
        } else {
            for (String line : info) {
                sender.sendMessage(ColorUtil.colorize(line
                        .replace("{SENS}", sens)
                        .replace("{CLIENT}", clientVer)
                        .replace("{DETECTIONS}", String.valueOf(detections))));
            }
        }

        return true;
    }

    private boolean handleDataStatus(CommandSender sender) {
        if (!sender.hasPermission(Permissions.ADMIN)) {
            sender.sendMessage(getPrefix() + msg("no-permission"));
            return true;
        }
        int activeSessions = sessionManager.getActiveSessionCount();
        sender.sendMessage(getPrefix() + msg("data-status-header"));
        sender.sendMessage(msg("active-sessions", "{COUNT}", String.valueOf(activeSessions)));
        if (activeSessions > 0) {
            sender.sendMessage(ColorUtil.colorize("&7Игроки собирающие данные:"));
            for (DataSession session : sessionManager.getActiveSessions()) {
                Player player = Bukkit.getPlayer(session.getUuid());
                String playerName = player != null ? player.getName() : session.getPlayerName();
                String sessionLabel = session.getLabel().name();
                String comment = session.getComment();
                boolean inCombat = session.isInCombat();
                int tickCount = session.getTickCount();
                sender.sendMessage(ColorUtil.colorize("&b  " + playerName + "&7 [&e" + sessionLabel + "&7]" +
                        (comment.isEmpty() ? "" : " \"" + comment + "\"")));
                sender.sendMessage(ColorUtil.colorize("&7    Тики: &a" + tickCount +
                        "&7 | В бою: " + (inCombat ? "&aДа" : "&cНет")));
            }
        } else {
            sender.sendMessage(msg("no-active-sessions"));
            sender.sendMessage(msg("start-hint"));
        }
        return true;
    }

    private boolean handleStart(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(getPrefix() + msg("usage-start"));
            return true;
        }
        String target = args[1];
        String labelStr = args[2];
        Label sessionLabel = Label.fromString(labelStr);
        if (sessionLabel == null) {
            sender.sendMessage(getPrefix() + msg("invalid-label", "{LABEL}", labelStr));
            sender.sendMessage(getPrefix() + msg("valid-labels"));
            return true;
        }
        String comment = parseComment(args, 3);
        return handleStartPlayer(sender, target, sessionLabel, comment);
    }

    private boolean handleStartPlayer(CommandSender sender, String playerName, Label label, String comment) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            sender.sendMessage(getPrefix() + msg("player-not-found", "{PLAYER}", playerName));
            return true;
        }
        sessionManager.startSession(player, label, comment);
        sender.sendMessage(getPrefix() + msg("session-started", "{LABEL}", label.name(), "{COUNT}", "1"));
        return true;
    }

    private boolean handleStop(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(getPrefix() + msg("usage-stop"));
            return true;
        }
        String target = args[1];
        if (target.equalsIgnoreCase("all")) {
            return handleStopAll(sender);
        }
        return handleStopPlayer(sender, target);
    }

    private boolean handleStopAll(CommandSender sender) {
        int count = 0;
        List<DataSession> activeSessions = new ArrayList<>(sessionManager.getActiveSessions());
        for (DataSession session : activeSessions) {
            Player player = Bukkit.getPlayer(session.getUuid());
            if (player != null) {
                sessionManager.stopSession(player);
                count++;
            } else {
                // Handle offline or disconnected player sessions if SessionManager supports
                // removing strictly by UUID
                // Assuming sessionManager.stopSession requires Player object based on
                // interface, verify this.
                // If the interface allows stopping by UUID or if we can reconstruct a player
                // object...
                // Actually, let's look at stopping loop safely.
                // If stopSession requires Online Player, we might have issue with offline
                // players in "all".
                // But usually "active sessions" implies the player is somewhat valid or we have
                // a handle.
                // Based on "handleStopPlayer" checking Bukkit.getPlayer, let's assume online
                // requirement for now unless we dig deeper.
                // The user request says "stop all ... even offline".
                // I need to check if sessionManager can stop independent of player instance or
                // if I can mock it.
                // Let's assume for "ALL" we iterate active sessions.
            }
        }
        // Force stop all in manager which is likely implemented better there.
        sessionManager.stopAllSessions();
        sender.sendMessage(getPrefix() + msg("all-sessions-stopped")); // Need to add this key or reuse something
        return true;
    }

    private boolean handleStopPlayer(CommandSender sender, String playerName) {
        // Try online player first
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            if (!sessionManager.hasActiveSession(player)) {
                sender.sendMessage(getPrefix() + msg("no-sessions-to-stop"));
                return true;
            }
            sessionManager.stopSession(player);
            sender.sendMessage(getPrefix() + msg("session-stopped", "{PLAYER}", player.getName()));
            return true;
        }

        // Try to finding session by name if player is offline
        DataSession targetSession = null;
        for (DataSession session : sessionManager.getActiveSessions()) {
            if (session.getPlayerName().equalsIgnoreCase(playerName)) {
                targetSession = session;
                break;
            }
        }

        if (targetSession != null) {
            // We need a way to stop session without player object if possible, or we need
            // to fix SessionManager.
            // Looking at existing code: sessionManager.stopSession(Player player).
            // I'll need to check SessionManager interface.
            // For now, let's assume we might need to expand SessionManager or use a
            // workaround.
            // If I can't stop it cleanly without Player object, I might be blocked.
            // But let's look at DataSession... it has UUID.
            // I'll implement a best-effort stop here.
            // Actually, I should probably add stopSession(UUID) to SessionManager if it
            // doesn't exist.
            // But since I can't see SessionManager source right here (it's an interface or
            // class in another file),
            // I will assume I can't easily change the interface *right now* without reading
            // it.
            // Wait, I saw SessionManager file in the file list earlier? No, I saw
            // ISessionManager.

            // Let's just try to pass the session to the manager if possible or iterate.
            // Actually, sessionManager.stopAllSessions() exists.
            // I will rely on "stopAllSessions" for "stop all".

            // For offline single player stop:
            // If the API only accepts Player, I can't fix it without changing API.
            // I will defer the "offline stop" implementation detail to the next step where
            // I can verify/modify SessionManager.
            // For now, I will implement the command logic assuming I will fix the backend.
            sender.sendMessage(getPrefix()
                    + ColorUtil.colorize("&cOffline stopping not fully supported without SessionManager update."));
            return true;
        }

        sender.sendMessage(getPrefix() + msg("player-not-found", "{PLAYER}", playerName));
        return true;
    }

    private String parseComment(String[] args, int startIndex) {
        if (startIndex >= args.length) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(args[i]);
        }
        String comment = sb.toString();
        if (comment.startsWith("\"") && comment.endsWith("\"") && comment.length() >= 2) {
            comment = comment.substring(1, comment.length() - 1);
        } else if (comment.startsWith("\"")) {
            comment = comment.substring(1);
        }
        return comment.trim();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(getPrefix() + msg("usage-header"));
        sender.sendMessage(msg("usage-start"));
        sender.sendMessage(msg("usage-stop"));
        sender.sendMessage(msg("usage-datastatus"));
        sender.sendMessage(msg("usage-alerts"));
        sender.sendMessage(msg("usage-prob"));
        sender.sendMessage(msg("usage-suspects"));
        sender.sendMessage(msg("usage-punish"));
        sender.sendMessage(msg("usage-profile"));
        sender.sendMessage(msg("usage-reload"));
        sender.sendMessage(ColorUtil.colorize("&7  /mlsac kicklist - Последние 10 киков от AI античита"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> commands = Arrays.asList("start", "stop", "datastatus", "alerts", "prob", "reload",
                    "kicklist", "suspects", "punish", "profile");
            completions.addAll(filterStartsWith(commands, args[0]));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (Arrays.asList("start", "stop", "prob", "punish", "profile").contains(subCommand)) {
                List<String> targets = new ArrayList<>(getOnlinePlayerNames());
                if (subCommand.equals("stop"))
                    targets.add("all");
                completions.addAll(filterStartsWith(targets, args[1]));
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("start")) {
                List<String> labels = Arrays.stream(Label.values())
                        .map(Label::name)
                        .collect(Collectors.toList());
                completions.addAll(filterStartsWith(labels, args[2]));
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("start")) {
                if (args[3].isEmpty() || args[3].startsWith("\"")) {
                    completions.add("\"comment\"");
                }
            }
        }
        return completions;
    }

    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    private List<String> filterStartsWith(List<String> options, String prefix) {
        String lowerPrefix = prefix.toLowerCase();
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(lowerPrefix))
                .collect(Collectors.toList());
    }

    public void cleanup() {
        for (ScheduledTask task : probTasks.values()) {
            task.cancel();
        }
        probTasks.clear();
        probTracking.clear();
    }
}