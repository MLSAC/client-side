/*
 * This file is part of MLSAC - AI powered Anti-Cheat
 * Copyright (C) 2026 MLSAC Team
 */

package wtf.mlsac.menu;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import wtf.mlsac.checks.AICheck;
import wtf.mlsac.config.Config;
import wtf.mlsac.data.AIPlayerData;
import wtf.mlsac.server.AnalyticsClient;
import wtf.mlsac.util.ColorUtil;
import wtf.mlsac.Main;
import wtf.mlsac.scheduler.SchedulerManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SuspectsMenu implements Listener {

    private final JavaPlugin plugin;
    private final Player admin;
    private final Inventory inventory;
    private final AICheck aiCheck;
    private final AnalyticsClient analyticsClient;
    private final Config pluginConfig;
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 45;

    public SuspectsMenu(JavaPlugin plugin, Player admin) {
        this.plugin = plugin;
        this.admin = admin;
        Main main = (Main) plugin;
        this.aiCheck = main.getAiCheck();
        this.analyticsClient = main.getAnalyticsClient();
        this.pluginConfig = main.getPluginConfig();
        org.bukkit.configuration.file.FileConfiguration config = main.getMenuConfig().getConfig();
        String title = config.getString("gui.title", "&cMLSAC &8> &7Suspects");
        this.inventory = Bukkit.createInventory(null, 54, ColorUtil.colorize(title));
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open() {
        updateInventory();
        admin.openInventory(inventory);
    }

    private void updateInventory() {
        inventory.clear();

        ItemStack loading = new ItemStack(Material.SUNFLOWER);
        ItemMeta loadingMeta = loading.getItemMeta();
        if (loadingMeta != null) {
            loadingMeta.setDisplayName(ColorUtil.colorize("&eLoading suspects..."));
            loading.setItemMeta(loadingMeta);
        }
        inventory.setItem(22, loading);

        SchedulerManager.getAdapter().runAsync(() -> {
            List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());

            List<SuspectData> suspectDataList = onlinePlayers.stream()
                    .map(p -> {
                        AIPlayerData data = aiCheck.getPlayerData(p.getUniqueId());
                        if (data == null || data.getProbabilityHistory().isEmpty()) {
                            return null;
                        }
                        return new SuspectData(
                                p.getUniqueId(),
                                p.getName(),
                                data.getAverageProbability(),
                                new ArrayList<>(data.getProbabilityHistory()));
                    })
                    .filter(d -> d != null)
                    .sorted((d1, d2) -> Double.compare(d2.avgProbability, d1.avgProbability))
                    .collect(Collectors.toList());

            final int totalPages = (int) Math.ceil((double) suspectDataList.size() / ITEMS_PER_PAGE);
            final int currentPage;
            if (page >= totalPages && totalPages > 0) {
                currentPage = totalPages - 1;
            } else if (page < 0) {
                currentPage = 0;
            } else {
                currentPage = page;
            }
            page = currentPage;

            int start = currentPage * ITEMS_PER_PAGE;
            int end = Math.min(start + ITEMS_PER_PAGE, suspectDataList.size());

            List<SuspectData> pageData = suspectDataList.subList(start, end);
            final int finalTotalPages = totalPages;
            final int finalEnd = end;
            final int finalTotalSuspects = suspectDataList.size();

            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (SuspectData data : pageData) {
                if (analyticsClient != null) {
                    CompletableFuture<Void> future = analyticsClient.checkPlayer(data.name).thenAccept(result -> {
                        if (result.isFound()) {
                            data.analyticsDetections = result.getTotalDetections();
                            data.analyticsFound = true;
                        }
                    });
                    futures.add(future);
                }
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
                SchedulerManager.getAdapter().runSync(() -> {
                    inventory.clear();
                    org.bukkit.configuration.file.FileConfiguration config = ((Main) plugin).getMenuConfig()
                            .getConfig();

                    for (int i = 0; i < pageData.size(); i++) {
                        SuspectData data = pageData.get(i);
                        inventory.setItem(i, createSuspectHeadFromData(data, config));
                    }

                    if (currentPage > 0) {
                        Material prevMat = Material
                                .valueOf(config.getString("gui.items.previous_page.material", "ARROW"));
                        String prevName = config.getString("gui.items.previous_page.name",
                                "&ePrevious Page (&f{PAGE}&e)");
                        inventory.setItem(45,
                                createButtonItem(prevMat, prevName.replace("{PAGE}", String.valueOf(currentPage))));
                    }

                    Material infoMat = Material.valueOf(config.getString("gui.items.page_info.material", "PAPER"));
                    String infoName = config.getString("gui.items.page_info.name", "&bPage &f{CURRENT} &7/ &f{TOTAL}");
                    inventory.setItem(49, createButtonItem(infoMat, infoName
                            .replace("{CURRENT}", String.valueOf(currentPage + 1))
                            .replace("{TOTAL}", String.valueOf(Math.max(1, finalTotalPages)))));

                    if (finalEnd < finalTotalSuspects) {
                        Material nextMat = Material.valueOf(config.getString("gui.items.next_page.material", "ARROW"));
                        String nextName = config.getString("gui.items.next_page.name", "&eNext Page (&f{PAGE}&e)");
                        inventory.setItem(53,
                                createButtonItem(nextMat, nextName.replace("{PAGE}", String.valueOf(currentPage + 2))));
                    }

                    Material fillerMat = Material
                            .valueOf(config.getString("gui.items.filler.material", "GRAY_STAINED_GLASS_PANE"));
                    String fillerName = config.getString("gui.items.filler.name", " ");
                    ItemStack filler = createButtonItem(fillerMat, fillerName);
                    for (int i = 45; i < 54; i++) {
                        if (inventory.getItem(i) == null) {
                            inventory.setItem(i, filler);
                        }
                    }
                });
            });
        });
    }

    private static class SuspectData {
        final UUID uuid;
        final String name;
        final double avgProbability;
        final List<Double> history;
        volatile int analyticsDetections = 0;
        volatile boolean analyticsFound = false;

        SuspectData(UUID uuid, String name, double avgProbability, List<Double> history) {
            this.uuid = uuid;
            this.name = name;
            this.avgProbability = avgProbability;
            this.history = history;
        }
    }

    private ItemStack createSuspectHeadFromData(SuspectData data,
            org.bukkit.configuration.file.FileConfiguration config) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            Player suspect = Bukkit.getPlayer(data.uuid);
            if (suspect != null && suspect.isOnline()) {
                meta.setOwningPlayer(suspect);
            } else {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(data.uuid));
            }

            String nameFormat = config.getString("gui.items.suspect_head.name", "&c{PLAYER}");
            meta.setDisplayName(ColorUtil.colorize(nameFormat.replace("{PLAYER}", data.name)));

            List<String> loreFormat = config.getStringList("gui.items.suspect_head.lore");
            if (loreFormat.isEmpty()) {
                loreFormat = new ArrayList<>();
                loreFormat.add("&8&m------------------------");
                loreFormat.add("&7AVG Probability: {AVG_PROB}");
                loreFormat.add("&7History (Last {HISTORY_SIZE}):");
                loreFormat.add("{HISTORY}");
                loreFormat.add("&8&m------------------------");
                loreFormat.add("&eLeft-Click to Teleport");
                loreFormat.add("&eRight-Click to TP + GM3");
            }

            List<String> lore = new ArrayList<>();
            StringBuilder historyStr = new StringBuilder();
            for (Double val : data.history) {
                historyStr.append(getColorInfo(val)).append(" ");
            }

            for (String line : loreFormat) {
                String detectionsStr;
                if (data.analyticsFound) {
                    String color = pluginConfig.getDetectionColor(data.analyticsDetections);
                    detectionsStr = color + data.analyticsDetections;
                } else {
                    detectionsStr = "&7N/A";
                }
                lore.add(ColorUtil.colorize(line
                        .replace("{AVG_PROB}", getColorInfo(data.avgProbability))
                        .replace("{HISTORY_SIZE}", String.valueOf(data.history.size()))
                        .replace("{HISTORY}", historyStr.toString().trim())
                        .replace("{DETECTIONS}", detectionsStr)));
            }

            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    private ItemStack createButtonItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String getColorInfo(double val) {
        ChatColor color = ChatColor.GREEN;
        if (val >= 0.9)
            color = ChatColor.DARK_RED;
        else if (val >= 0.8)
            color = ChatColor.RED;
        else if (val >= 0.6)
            color = ChatColor.GOLD;

        return color + String.format("%.2f", val) + "&r";
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() != inventory)
            return;
        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR)
            return;

        org.bukkit.configuration.file.FileConfiguration config = ((Main) plugin).getMenuConfig().getConfig();

        if (event.getSlot() == 45) {
            Material prevMat = Material.valueOf(config.getString("gui.items.previous_page.material", "ARROW"));
            if (item.getType() == prevMat && page > 0) {
                page--;
                updateInventory();
            }
            return;
        }

        if (event.getSlot() == 53) {
            Material nextMat = Material.valueOf(config.getString("gui.items.next_page.material", "ARROW"));
            if (item.getType() == nextMat) {
                page++;
                updateInventory();
            }
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta) {
            SkullMeta skullMeta = (SkullMeta) meta;
            Player target = skullMeta.getOwningPlayer() != null ? skullMeta.getOwningPlayer().getPlayer() : null;

            if (target != null && target.isOnline()) {
                if (event.isLeftClick()) {
                    admin.teleport(target);
                    admin.sendMessage(ColorUtil.colorize(((Main) plugin).getMessagesConfig()
                            .getMessage("suspects-teleport", "{PLAYER}", target.getName())));
                } else if (event.isRightClick()) {
                    admin.setGameMode(GameMode.SPECTATOR);
                    admin.teleport(target);
                    admin.sendMessage(ColorUtil.colorize(((Main) plugin).getMessagesConfig()
                            .getMessage("suspects-teleport-spectator", "{PLAYER}", target.getName())));
                }
            } else {
                admin.sendMessage(
                        ColorUtil.colorize(((Main) plugin).getMessagesConfig().getMessage("suspects-player-offline")));
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory() == inventory) {
            HandlerList.unregisterAll(this);
        }
    }
}
