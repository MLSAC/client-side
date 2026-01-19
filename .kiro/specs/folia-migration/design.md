# Дизайн: Портирование MLSAC на Folia с поддержкой обратной совместимости

## Overview

Этот документ описывает архитектуру портирования MLSAC на Folia с сохранением совместимости с обычными Spigot/Paper серверами. Основная идея - создать абстрактный слой (Scheduler Adapter Pattern) для работы с планировщиком задач, который автоматически выбирает правильную реализацию в зависимости от типа сервера.

**Ключевые принципы:**
- Единый код для обоих типов серверов
- Автоматическое определение типа сервера при загрузке
- Абстрактный слой для планирования задач
- Минимальные изменения в существующем коде
- Полная потокобезопасность

## Architecture

### Общая архитектура

```
┌─────────────────────────────────────────────────────────────┐
│                    MLSAC Plugin                              │
│  (Checks, Listeners, Commands, Data Collection)             │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              Scheduler Adapter Layer                         │
│  (Unified interface for task scheduling)                    │
└────────────────────┬────────────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        ▼                         ▼
┌──────────────────┐    ┌──────────────────┐
│  Folia Adapter   │    │  Bukkit Adapter  │
│  (Folia API)     │    │  (Bukkit API)    │
└──────────────────┘    └──────────────────┘
        │                         │
        ▼                         ▼
┌──────────────────┐    ┌──────────────────┐
│  Folia Server    │    │  Paper/Spigot    │
│  (Threaded)      │    │  (Single-thread) │
└──────────────────┘    └──────────────────┘
```

### Server Type Detection

При загрузке плагина система определяет тип сервера:

```java
// Проверка наличия Folia API
try {
    Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
    serverType = ServerType.FOLIA;
} catch (ClassNotFoundException e) {
    serverType = ServerType.BUKKIT;
}
```

## Components and Interfaces

### 1. ServerType Enum

```java
public enum ServerType {
    FOLIA,      // Folia сервер
    BUKKIT      // Обычный Bukkit/Paper/Spigot сервер
}
```

### 2. SchedulerAdapter Interface

Основной интерфейс для абстракции планирования:

```java
public interface SchedulerAdapter {
    
    // Синхронное выполнение на основном потоке
    ScheduledTask runSync(Runnable task);
    ScheduledTask runSyncDelayed(Runnable task, long delayTicks);
    ScheduledTask runSyncRepeating(Runnable task, long delayTicks, long periodTicks);
    
    // Асинхронное выполнение
    ScheduledTask runAsync(Runnable task);
    ScheduledTask runAsyncDelayed(Runnable task, long delayTicks);
    ScheduledTask runAsyncRepeating(Runnable task, long delayTicks, long periodTicks);
    
    // Выполнение для сущности
    ScheduledTask runEntitySync(Entity entity, Runnable task);
    ScheduledTask runEntitySyncDelayed(Entity entity, Runnable task, long delayTicks);
    ScheduledTask runEntitySyncRepeating(Entity entity, Runnable task, long delayTicks, long periodTicks);
    
    // Выполнение для региона (по локации)
    ScheduledTask runRegionSync(Location location, Runnable task);
    ScheduledTask runRegionSyncDelayed(Location location, Runnable task, long delayTicks);
    ScheduledTask runRegionSyncRepeating(Location location, Runnable task, long delayTicks, long periodTicks);
    
    // Получение типа сервера
    ServerType getServerType();
}
```

### 3. BukkitSchedulerAdapter

Реализация для обычных Bukkit/Paper/Spigot серверов:

```java
public class BukkitSchedulerAdapter implements SchedulerAdapter {
    private final Plugin plugin;
    private final BukkitScheduler scheduler;
    
    public BukkitSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
        this.scheduler = Bukkit.getScheduler();
    }
    
    @Override
    public ScheduledTask runSync(Runnable task) {
        return new BukkitScheduledTask(
            scheduler.scheduleSyncDelayedTask(plugin, task)
        );
    }
    
    // ... остальные методы используют BukkitScheduler
}
```

### 4. FoliaSchedulerAdapter

Реализация для Folia серверов:

```java
public class FoliaSchedulerAdapter implements SchedulerAdapter {
    private final Plugin plugin;
    private final GlobalRegionScheduler globalScheduler;
    private final RegionScheduler regionScheduler;
    private final AsyncScheduler asyncScheduler;
    
    public FoliaSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
        this.globalScheduler = Bukkit.getGlobalRegionScheduler();
        this.regionScheduler = Bukkit.getRegionScheduler();
        this.asyncScheduler = Bukkit.getAsyncScheduler();
    }
    
    @Override
    public ScheduledTask runSync(Runnable task) {
        return globalScheduler.run(plugin, (scheduledTask) -> task.run());
    }
    
    @Override
    public ScheduledTask runEntitySync(Entity entity, Runnable task) {
        return entity.getScheduler().run(plugin, (scheduledTask) -> task.run(), null);
    }
    
    // ... остальные методы используют Folia API
}
```

### 5. SchedulerManager

Глобальный менеджер для управления адаптером:

```java
public class SchedulerManager {
    private static SchedulerAdapter adapter;
    private static ServerType serverType;
    
    public static void initialize(Plugin plugin) {
        serverType = detectServerType();
        
        if (serverType == ServerType.FOLIA) {
            adapter = new FoliaSchedulerAdapter(plugin);
        } else {
            adapter = new BukkitSchedulerAdapter(plugin);
        }
    }
    
    public static SchedulerAdapter getAdapter() {
        if (adapter == null) {
            throw new IllegalStateException("SchedulerManager not initialized");
        }
        return adapter;
    }
    
    public static ServerType getServerType() {
        return serverType;
    }
    
    private static ServerType detectServerType() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            return ServerType.FOLIA;
        } catch (ClassNotFoundException e) {
            return ServerType.BUKKIT;
        }
    }
}
```

## Data Models

### ScheduledTask Interface

Унифицированный интерфейс для представления запланированной задачи:

```java
public interface ScheduledTask {
    void cancel();
    boolean isCancelled();
    boolean isRunning();
}
```

### PlayerData Model

Потокобезопасная модель данных игрока:

```java
public class PlayerData {
    private final UUID playerId;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile long lastCheckTime;
    private volatile int violationCount;
    private volatile List<CheckResult> recentChecks;
    
    // Методы с синхронизацией через lock
    public void addViolation() {
        lock.writeLock().lock();
        try {
            violationCount++;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public int getViolationCount() {
        lock.readLock().lock();
        try {
            return violationCount;
        } finally {
            lock.readLock().unlock();
        }
    }
}
```

## Correctness Properties

A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.

### Property 1: Server Type Detection Consistency

*For any* plugin initialization, the detected server type SHALL remain constant throughout the plugin's lifetime and SHALL correctly identify either Folia or Bukkit/Paper/Spigot.

**Validates: Requirements 1.1, 1.2, 1.3, 1.4**

### Property 2: Scheduler Adapter Initialization

*For any* plugin initialization, the SchedulerAdapter SHALL be successfully initialized and available for use, and SHALL match the detected server type.

**Validates: Requirements 1.2, 2.1, 3.1, 3.2**

### Property 3: Sync Task Execution Order

*For any* sequence of synchronous tasks scheduled in order, the tasks SHALL execute in the same order they were scheduled.

**Validates: Requirements 2.2, 4.1**

### Property 4: Async Task Independence

*For any* asynchronous task, the task SHALL execute independently of the server tick and SHALL not block the main thread.

**Validates: Requirements 2.3, 4.3**

### Property 5: Entity Scheduler Binding

*For any* entity and task scheduled via Entity Scheduler, the task SHALL execute in the thread that owns the entity, regardless of the entity's location changes.

**Validates: Requirements 2.4, 4.2, 6.1, 6.2**

### Property 6: Region Scheduler Locality

*For any* location and task scheduled via Region Scheduler, the task SHALL execute in the thread that owns the region containing that location.

**Validates: Requirements 2.5, 6.3, 6.4**

### Property 7: Repeating Task Periodicity

*For any* repeating task with period P, the task SHALL execute at approximately regular intervals of P ticks, and the number of executions SHALL be consistent across multiple runs.

**Validates: Requirements 4.4**

### Property 8: Event Handler Thread Safety

*For any* concurrent event handling from multiple threads, the event handlers SHALL not cause race conditions or data corruption.

**Validates: Requirements 5.1, 5.2, 5.3, 5.4**

### Property 9: Player Data Consistency

*For any* concurrent access to PlayerData from multiple threads, the data SHALL remain consistent and no data corruption SHALL occur.

**Validates: Requirements 7.1, 7.2, 7.3, 7.4**

### Property 10: Violation Count Monotonicity

*For any* player, the violation count SHALL never decrease and SHALL only increase when violations are added, even under concurrent access.

**Validates: Requirements 9.4, 8.1, 8.2**

### Property 11: Check Execution Thread Safety

*For any* check execution with concurrent access to player and world data, the check SHALL execute safely without race conditions.

**Validates: Requirements 8.1, 8.2, 8.3, 8.4**

### Property 12: Penalty Execution Synchronization

*For any* penalty execution, the penalty SHALL be executed in the correct thread context and SHALL not cause race conditions.

**Validates: Requirements 9.1, 9.2, 9.3**

### Property 13: Configuration Reload Safety

*For any* configuration reload, the new parameters SHALL be applied safely without causing race conditions or data inconsistency.

**Validates: Requirements 10.2, 10.3**

### Property 14: Cross-Server Behavioral Equivalence

*For any* code path, the functional behavior SHALL be identical whether running on Folia or Bukkit/Paper/Spigot, except for performance characteristics.

**Validates: Requirements 11.1, 11.2, 11.3, 11.5**

### Property 15: Task Cancellation Idempotence

*For any* scheduled task, calling cancel() multiple times SHALL have the same effect as calling it once, and subsequent calls SHALL not raise exceptions.

**Validates: Requirements 2.1, 2.2, 2.3**

## Error Handling

### Scheduler Initialization Errors

```java
try {
    SchedulerManager.initialize(plugin);
} catch (Exception e) {
    plugin.getLogger().severe("Failed to initialize scheduler: " + e.getMessage());
    plugin.getServer().getPluginManager().disablePlugin(plugin);
}
```

### Task Execution Errors

```java
SchedulerAdapter adapter = SchedulerManager.getAdapter();
adapter.runSync(() -> {
    try {
        // Task logic
    } catch (Exception e) {
        plugin.getLogger().log(Level.SEVERE, "Task execution failed", e);
    }
});
```

### Entity Scheduler Errors

```java
if (entity.isValid()) {
    adapter.runEntitySync(entity, () -> {
        try {
            // Entity-specific logic
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Entity task failed", e);
        }
    });
}
```

## Testing Strategy

### Unit Tests

- Test ServerType detection with mocked classes
- Test SchedulerAdapter interface implementation for both Bukkit and Folia
- Test PlayerData thread safety with concurrent access
- Test task scheduling and cancellation
- Test error handling and edge cases

### Property-Based Tests

- **Property 1**: Server type detection consistency across multiple initializations
- **Property 2**: Scheduler adapter availability for all task types
- **Property 3**: Sync task execution order for random task sequences
- **Property 4**: Entity scheduler binding with entity movement
- **Property 5**: Async task independence from main thread
- **Property 6**: Region scheduler locality for random locations
- **Property 7**: Task cancellation idempotence with random cancellation patterns
- **Property 8**: Player data thread safety with concurrent random operations
- **Property 9**: Violation count monotonicity with random violation additions
- **Property 10**: Cross-server compatibility for random code paths

### Integration Tests

- Test plugin loading on both Folia and Bukkit servers
- Test event handling with scheduler integration
- Test data persistence with concurrent access
- Test check execution with scheduler integration
- Test penalty execution with scheduler integration

