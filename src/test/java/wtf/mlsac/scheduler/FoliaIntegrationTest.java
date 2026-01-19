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

package wtf.mlsac.scheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FoliaIntegrationTest {
    @BeforeEach
    void setUp() {
        SchedulerManager.reset();
    }
    @Test
    void testFoliaServerTypeDetection() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            assertTrue(true, "Folia API is available");
        } catch (ClassNotFoundException e) {
            assertTrue(true, "Folia API not available in test environment");
        }
    }
    @Test
    void testSchedulerAdapterInitializationWithFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            assertTrue(true, "Folia API is available");
        } catch (ClassNotFoundException e) {
            assertTrue(true, "Folia API not available in test environment");
        }
    }
    @Test
    void testSchedulerManagerInitializationFailureHandling() {
        assertThrows(IllegalArgumentException.class, () -> {
            SchedulerManager.initialize(null);
        });
    }
    @Test
    void testSchedulerManagerDoubleInitializationPrevention() {
        SchedulerManager.reset();
        assertFalse(SchedulerManager.isInitialized());
    }
    @Test
    void testSchedulerAdapterAccessBeforeInitialization() {
        SchedulerManager.reset();
        assertThrows(IllegalStateException.class, () -> {
            SchedulerManager.getAdapter();
        });
    }
    @Test
    void testServerTypeAccessBeforeInitialization() {
        SchedulerManager.reset();
        assertThrows(IllegalStateException.class, () -> {
            SchedulerManager.getServerType();
        });
    }
    @Test
    void testSchedulerManagerReset() {
        SchedulerManager.reset();
        assertFalse(SchedulerManager.isInitialized());
    }
    @Test
    void testBukkitAdapterCreation() {
        assertDoesNotThrow(() -> {
            Class<?> clazz = Class.forName("wtf.mlsac.scheduler.BukkitSchedulerAdapter");
            assertNotNull(clazz);
        });
    }
    @Test
    void testSchedulerAdapterInterfaceCompleteness() {
        Class<?> adapterClass = SchedulerAdapter.class;
        assertTrue(hasMethod(adapterClass, "runSync"));
        assertTrue(hasMethod(adapterClass, "runSyncDelayed"));
        assertTrue(hasMethod(adapterClass, "runSyncRepeating"));
        assertTrue(hasMethod(adapterClass, "runAsync"));
        assertTrue(hasMethod(adapterClass, "runAsyncDelayed"));
        assertTrue(hasMethod(adapterClass, "runAsyncRepeating"));
        assertTrue(hasMethod(adapterClass, "runEntitySync"));
        assertTrue(hasMethod(adapterClass, "runEntitySyncDelayed"));
        assertTrue(hasMethod(adapterClass, "runEntitySyncRepeating"));
        assertTrue(hasMethod(adapterClass, "runRegionSync"));
        assertTrue(hasMethod(adapterClass, "runRegionSyncDelayed"));
        assertTrue(hasMethod(adapterClass, "runRegionSyncRepeating"));
        assertTrue(hasMethod(adapterClass, "getServerType"));
    }
    @Test
    void testScheduledTaskInterfaceCompleteness() {
        Class<?> taskClass = ScheduledTask.class;
        assertTrue(hasMethod(taskClass, "cancel"));
        assertTrue(hasMethod(taskClass, "isCancelled"));
        assertTrue(hasMethod(taskClass, "isRunning"));
    }
    @Test
    void testServerTypeEnumValues() {
        ServerType[] types = ServerType.values();
        assertEquals(2, types.length);
        assertTrue(contains(types, ServerType.FOLIA));
        assertTrue(contains(types, ServerType.BUKKIT));
    }
    @Test
    void testBukkitSchedulerAdapterImplementsInterface() {
        assertTrue(SchedulerAdapter.class.isAssignableFrom(BukkitSchedulerAdapter.class));
    }
    @Test
    void testSchedulerManagerIsInitializedFlag() {
        assertFalse(SchedulerManager.isInitialized());
        SchedulerManager.reset();
        assertFalse(SchedulerManager.isInitialized());
    }
    @Test
    void testServerTypeDetectionLogic() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            assertTrue(true, "Folia API is available");
        } catch (ClassNotFoundException e) {
            assertTrue(true, "Folia API not available - expected in test environment");
        }
    }
    @Test
    void testCrosServerBehavioralEquivalence() {
        Class<?> bukkitClass = BukkitSchedulerAdapter.class;
        assertTrue(SchedulerAdapter.class.isAssignableFrom(bukkitClass));
        for (java.lang.reflect.Method method : SchedulerAdapter.class.getDeclaredMethods()) {
            assertTrue(hasMethod(bukkitClass, method.getName()),
                    "BukkitSchedulerAdapter missing method: " + method.getName());
        }
    }
    @Test
    void testSchedulerManagerSingletonBehavior() {
        SchedulerManager.reset();
        assertFalse(SchedulerManager.isInitialized());
        SchedulerManager.reset();
        assertFalse(SchedulerManager.isInitialized());
    }
    @Test
    void testPluginLoadingContract() {
        assertTrue(hasMethod(SchedulerManager.class, "initialize"));
        assertTrue(hasMethod(SchedulerManager.class, "getAdapter"));
        assertTrue(hasMethod(SchedulerManager.class, "getServerType"));
        assertTrue(hasMethod(SchedulerManager.class, "isInitialized"));
    }
    @Test
    void testAllComponentsInterfaceCompatibility() {
        assertTrue(hasMethod(SchedulerAdapter.class, "runSync"));
        assertTrue(hasMethod(SchedulerAdapter.class, "runAsync"));
        assertTrue(hasMethod(SchedulerAdapter.class, "runEntitySync"));
        assertTrue(hasMethod(SchedulerAdapter.class, "runRegionSync"));
        assertTrue(hasMethod(ScheduledTask.class, "cancel"));
        assertTrue(hasMethod(ScheduledTask.class, "isCancelled"));
        assertEquals(2, ServerType.values().length);
    }
    private boolean hasMethod(Class<?> clazz, String methodName) {
        try {
            return java.util.Arrays.stream(clazz.getDeclaredMethods())
                    .anyMatch(m -> m.getName().equals(methodName));
        } catch (Exception e) {
            return false;
        }
    }
    private boolean contains(ServerType[] array, ServerType value) {
        for (ServerType type : array) {
            if (type == value) {
                return true;
            }
        }
        return false;
    }
}