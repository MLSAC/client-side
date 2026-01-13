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

package wtf.mlsac.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import wtf.mlsac.Main;
import wtf.mlsac.config.Label;
import wtf.mlsac.util.AimProcessor;

public class DataSession {
    
    private final UUID uuid;
    private final String playerName;
    private final Label label;
    private final String comment;
    private final Queue<TickData> recordedTicks;
    private final Instant startTime;
    private final AimProcessor aimProcessor;
    
    private int ticksSinceAttack;
    private static final int COMBAT_TIMEOUT = 40;
    
    public DataSession(UUID uuid, String playerName, Label label, String comment) {
        this(uuid, playerName, label, comment, new AimProcessor());
    }
    
    public DataSession(UUID uuid, String playerName, Label label, String comment, AimProcessor aimProcessor) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.label = label;
        this.comment = comment;
        this.recordedTicks = new ConcurrentLinkedQueue<>();
        this.startTime = Instant.now();
        this.aimProcessor = aimProcessor;
        this.ticksSinceAttack = COMBAT_TIMEOUT;
    }
    
    public void processTick(float yaw, float pitch) {
        TickData tickData = aimProcessor.process(yaw, pitch);
        
        if (ticksSinceAttack < COMBAT_TIMEOUT) {
            recordedTicks.add(tickData);
        }
        
        ticksSinceAttack++;
    }
    
    public void onAttack() {
        ticksSinceAttack = 0;
    }
    
    public int getTickCount() {
        return recordedTicks.size();
    }
    
    public boolean isInCombat() {
        return ticksSinceAttack < COMBAT_TIMEOUT;
    }
    
    public UUID getUuid() {
        return uuid;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public Label getLabel() {
        return label;
    }
    
    public String getComment() {
        return comment;
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public int getTicksSinceAttack() {
        return ticksSinceAttack;
    }
    
    public String generateFileName() {
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss")
            .format(new Date(startTime.toEpochMilli()));
        String statusForFilename = label.name();
        if (comment != null && !comment.isEmpty()) {
            String sanitized = comment.replace(' ', '#')
                .replaceAll("[/\\\\?%*:|\"<>']", "-");
            statusForFilename = statusForFilename + "_" + sanitized;
        }
        return String.format("%s_%s_%s.csv", statusForFilename, playerName, timestamp);
    }
    
    public String generateCsvContent() {
        if (recordedTicks.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(TickData.getHeader()).append("\n");
        
        String cheatingStatus = "UNLABELED";
        if (label == Label.CHEAT) {
            cheatingStatus = "CHEAT";
        } else if (label == Label.LEGIT) {
            cheatingStatus = "LEGIT";
        }
        
        List<TickData> ticks = new ArrayList<>(recordedTicks);
        for (TickData tick : ticks) {
            sb.append(tick.toCsv(cheatingStatus)).append("\n");
        }
        
        return sb.toString();
    }
    
    public void saveAndClose(Main plugin) throws IOException {
        saveAndClose(plugin, null);
    }
    
    public void saveAndClose(Main plugin, String sessionFolder) throws IOException {
        String csvContent = generateCsvContent();
        if (csvContent.isEmpty()) {
            return;
        }
        
        File dataFolder;
        if (sessionFolder != null && !sessionFolder.isEmpty()) {
            dataFolder = new File(plugin.getDataFolder(), plugin.getConfig().getString("outputDirectory") + sessionFolder);
        } else {
            dataFolder = new File(plugin.getDataFolder(), "data");
        }
        
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        File outputFile = new File(dataFolder, generateFileName());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(csvContent);
        }
        
        plugin.getLogger().info("Saved " + recordedTicks.size() + " ticks to " + outputFile.getName());
    }
}
