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
import wtf.mlsac.data.AIPlayerData;
import wtf.mlsac.util.ColorUtil;
import wtf.mlsac.Main;
import wtf.mlsac.scheduler.SchedulerManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class SuspectsMenu implements Listener {

    private final JavaPlugin plugin;
    private final Player admin;
    private final Inventory inventory;
    private final AICheck aiCheck;
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 45;

    public SuspectsMenu(JavaPlugin plugin, Player admin) {
        this.plugin = plugin;
        this.admin = admin;
        this.aiCheck = ((Main) plugin).getAiCheck();
        org.bukkit.configuration.file.FileConfiguration config = ((Main) plugin).getMenuConfig().getConfig();
        String title = config.getString("gui.title", "&cMLSAC &8> &7Suspects");
        // We force 54 for pagination
        this.inventory = Bukkit.createInventory(null, 54, ColorUtil.colorize(title));
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open() {
        updateInventory();
        admin.openInventory(inventory);
    }

    private void updateInventory() {
        inventory.clear();

        // Add loading indicator
        ItemStack loading = new ItemStack(Material.SUNFLOWER);
        ItemMeta loadingMeta = loading.getItemMeta();
        if (loadingMeta != null) {
            loadingMeta.setDisplayName(ColorUtil.colorize("&eLoading suspects..."));
            loading.setItemMeta(loadingMeta);
        }
        inventory.setItem(22, loading);

        SchedulerManager.getAdapter().runAsync(() -> {
            List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
            List<Player> suspects = onlinePlayers.stream()
                    .filter(p -> {
                        AIPlayerData data = aiCheck.getPlayerData(p.getUniqueId());
                        return data != null && !data.getProbabilityHistory().isEmpty();
                    })
                    .sorted((p1, p2) -> {
                        AIPlayerData d1 = aiCheck.getPlayerData(p1.getUniqueId());
                        AIPlayerData d2 = aiCheck.getPlayerData(p2.getUniqueId());
                        return Double.compare(d2.getAverageProbability(), d1.getAverageProbability());
                    })
                    .collect(Collectors.toList());

            int totalPages = (int) Math.ceil((double) suspects.size() / ITEMS_PER_PAGE);
            if (page >= totalPages && totalPages > 0)
                page = totalPages - 1;
            if (page < 0)
                page = 0;

            int start = page * ITEMS_PER_PAGE;
            int end = Math.min(start + ITEMS_PER_PAGE, suspects.size());

            List<ItemStack> headItems = new ArrayList<>();
            org.bukkit.configuration.file.FileConfiguration config = ((Main) plugin).getMenuConfig().getConfig();

            for (int i = start; i < end; i++) {
                Player suspect = suspects.get(i);
                headItems.add(createSuspectHead(suspect, config));
            }

            SchedulerManager.getAdapter().runSync(() -> {
                inventory.clear();

                // Add heads
                for (int i = 0; i < headItems.size(); i++) {
                    inventory.setItem(i, headItems.get(i));
                }

                // Add navigation buttons from config
                if (page > 0) {
                    Material prevMat = Material.valueOf(config.getString("gui.items.previous_page.material", "ARROW"));
                    String prevName = config.getString("gui.items.previous_page.name", "&ePrevious Page (&f{PAGE}&e)");
                    inventory.setItem(45, createButtonItem(prevMat, prevName.replace("{PAGE}", String.valueOf(page))));
                }

                Material infoMat = Material.valueOf(config.getString("gui.items.page_info.material", "PAPER"));
                String infoName = config.getString("gui.items.page_info.name", "&bPage &f{CURRENT} &7/ &f{TOTAL}");
                inventory.setItem(49, createButtonItem(infoMat, infoName
                        .replace("{CURRENT}", String.valueOf(page + 1))
                        .replace("{TOTAL}", String.valueOf(Math.max(1, totalPages)))));

                if (end < suspects.size()) {
                    Material nextMat = Material.valueOf(config.getString("gui.items.next_page.material", "ARROW"));
                    String nextName = config.getString("gui.items.next_page.name", "&eNext Page (&f{PAGE}&e)");
                    inventory.setItem(53,
                            createButtonItem(nextMat, nextName.replace("{PAGE}", String.valueOf(page + 2))));
                }

                // Fill bottom bar with filler from config
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
    }

    private ItemStack createSuspectHead(Player suspect, org.bukkit.configuration.file.FileConfiguration config) {
        AIPlayerData data = aiCheck.getPlayerData(suspect.getUniqueId());
        double avg = data.getAverageProbability();
        List<Double> history = data.getProbabilityHistory();

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(suspect);

            String nameFormat = config.getString("gui.items.suspect_head.name", "&c{PLAYER}");
            meta.setDisplayName(ColorUtil.colorize(nameFormat.replace("{PLAYER}", suspect.getName())));

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
            for (Double val : history) {
                historyStr.append(getColorInfo(val)).append(" ");
            }

            for (String line : loreFormat) {
                lore.add(ColorUtil.colorize(line
                        .replace("{AVG_PROB}", getColorInfo(avg))
                        .replace("{HISTORY_SIZE}", String.valueOf(history.size()))
                        .replace("{HISTORY}", historyStr.toString().trim())));
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
                // We don't have totalPages here, but updateInventory handles bounds.
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
