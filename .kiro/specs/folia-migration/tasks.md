# План реализации: Портирование MLSAC на Folia

## Overview

План реализации портирования MLSAC на Folia с сохранением совместимости с обычными Spigot/Paper серверами. Реализация следует архитектуре Scheduler Adapter Pattern, описанной в дизайн документе.

## Tasks

- [x] 1. Обновление конфигурации проекта и зависимостей
  - Обновить build.gradle для поддержки Folia API
  - Добавить зависимость на Folia API (1.20.1+)
  - Сохранить зависимость на Paper API для обратной совместимости
  - Обновить plugin.yml для указания поддержки обоих типов серверов
  - Убедиться что Java версия 17+
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [ ]* 1.1 Написать unit тесты для конфигурации проекта
  - Проверить что build.gradle содержит нужные зависимости
  - Проверить что plugin.yml правильно сконфигурирован
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 2. Создать слой абстракции для планирования задач
  - [x] 2.1 Создать enum ServerType
    - Определить типы серверов: FOLIA, BUKKIT
    - _Requirements: 1.1_

  - [x] 2.2 Создать интерфейс SchedulerAdapter
    - Определить методы для синхронного выполнения
    - Определить методы для асинхронного выполнения
    - Определить методы для выполнения на сущностях
    - Определить методы для выполнения в регионах
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

  - [x] 2.3 Создать интерфейс ScheduledTask
    - Определить методы cancel(), isCancelled(), isRunning()
    - _Requirements: 2.1_

  - [ ]* 2.4 Написать unit тесты для интерфейсов
    - Проверить что интерфейсы определены правильно
    - Проверить что методы имеют правильные сигнатуры
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 3. Реализовать BukkitSchedulerAdapter
  - [x] 3.1 Создать класс BukkitSchedulerAdapter
    - Реализовать методы синхронного выполнения
    - Реализовать методы асинхронного выполнения
    - Реализовать методы для сущностей (используя обычный Scheduler)
    - Реализовать методы для регионов (используя обычный Scheduler)
    - _Requirements: 1.4, 4.1, 4.2, 4.3, 4.4_

  - [x] 3.2 Создать класс BukkitScheduledTask
    - Реализовать методы cancel(), isCancelled(), isRunning()
    - Обернуть BukkitTask
    - _Requirements: 2.1_

  - [ ]* 3.3 Написать unit тесты для BukkitSchedulerAdapter
    - Тестировать синхронное выполнение
    - Тестировать асинхронное выполнение
    - Тестировать отмену задач
    - **Property 3: Sync Task Execution Order**
    - **Validates: Requirements 2.2, 4.1**
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

  - [ ]* 3.4 Написать property тесты для BukkitSchedulerAdapter
    - **Property 4: Async Task Independence**
    - **Validates: Requirements 2.3, 4.3**
    - **Property 7: Repeating Task Periodicity**
    - **Validates: Requirements 4.4**
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [x] 4. Реализовать FoliaSchedulerAdapter
  - [x] 4.1 Создать класс FoliaSchedulerAdapter
    - Реализовать методы синхронного выполнения (используя GlobalRegionScheduler)
    - Реализовать методы асинхронного выполнения (используя AsyncScheduler)
    - Реализовать методы для сущностей (используя EntityScheduler)
    - Реализовать методы для регионов (используя RegionScheduler)
    - _Requirements: 1.3, 4.1, 4.2, 4.3, 4.4_

  - [x] 4.2 Создать класс FoliaScheduledTask
    - Реализовать методы cancel(), isCancelled(), isRunning()
    - Обернуть ScheduledTask из Folia API
    - _Requirements: 2.1_

  - [ ]* 4.3 Написать unit тесты для FoliaSchedulerAdapter
    - Тестировать синхронное выполнение
    - Тестировать асинхронное выполнение
    - Тестировать выполнение на сущностях
    - Тестировать выполнение в регионах
    - Тестировать отмену задач
    - **Property 3: Sync Task Execution Order**
    - **Validates: Requirements 2.2, 4.1**
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

  - [ ]* 4.4 Написать property тесты для FoliaSchedulerAdapter
    - **Property 4: Async Task Independence**
    - **Validates: Requirements 2.3, 4.3**
    - **Property 5: Entity Scheduler Binding**
    - **Validates: Requirements 2.4, 4.2, 6.1, 6.2**
    - **Property 6: Region Scheduler Locality**
    - **Validates: Requirements 2.5, 6.3, 6.4**
    - **Property 7: Repeating Task Periodicity**
    - **Validates: Requirements 4.4**
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [x] 5. Создать SchedulerManager для управления адаптером
  - [x] 5.1 Создать класс SchedulerManager
    - Реализовать метод detectServerType()
    - Реализовать метод initialize(Plugin plugin)
    - Реализовать метод getAdapter()
    - Реализовать метод getServerType()
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [ ]* 5.2 Написать unit тесты для SchedulerManager
    - Тестировать определение типа сервера
    - Тестировать инициализацию адаптера
    - Тестировать получение адаптера
    - **Property 1: Server Type Detection Consistency**
    - **Validates: Requirements 1.1, 1.2, 1.3, 1.4**
    - **Property 2: Scheduler Adapter Initialization**
    - **Validates: Requirements 1.2, 2.1, 3.1, 3.2**
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 6. Checkpoint - Убедиться что все тесты проходят
  - Запустить все unit тесты для слоя абстракции
  - Запустить все property тесты для слоя абстракции
  - Убедиться что нет ошибок компиляции
  - Спросить у пользователя если есть вопросы

- [x] 7. Обновить Main класс плагина
  - [x] 7.1 Инициализировать SchedulerManager при загрузке плагина
    - Вызвать SchedulerManager.initialize(this) в методе onEnable()
    - Обработать исключения при инициализации
    - _Requirements: 1.1, 1.2_

  - [x] 7.2 Обновить обработчики событий для использования SchedulerAdapter
    - Заменить все вызовы Bukkit.getScheduler() на SchedulerManager.getAdapter()
    - Убедиться что все события обрабатываются потокобезопасно
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

  - [ ]* 7.3 Написать unit тесты для Main класса
    - Тестировать инициализацию SchedulerManager
    - Тестировать обработку исключений
    - _Requirements: 1.1, 1.2_

- [x] 8. Обновить систему сбора данных (DataCollector)
  - [x] 8.1 Обновить класс PlayerData для потокобезопасности
    - Добавить ReentrantReadWriteLock для синхронизации
    - Обновить методы для использования lock
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

  - [x] 8.2 Обновить методы сохранения и загрузки данных
    - Использовать SchedulerAdapter для синхронизации
    - Убедиться что операции атомарны
    - _Requirements: 7.2, 7.3_

  - [ ]* 8.3 Написать unit тесты для PlayerData
    - Тестировать потокобезопасность
    - Тестировать сохранение и загрузку
    - **Property 9: Player Data Consistency**
    - **Validates: Requirements 7.1, 7.2, 7.3, 7.4**
    - **Property 10: Violation Count Monotonicity**
    - **Validates: Requirements 9.4, 8.1, 8.2**
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

  - [ ]* 8.4 Написать property тесты для PlayerData
    - **Property 9: Player Data Consistency**
    - **Validates: Requirements 7.1, 7.2, 7.3, 7.4**
    - **Property 10: Violation Count Monotonicity**
    - **Validates: Requirements 9.4, 8.1, 8.2**
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [x] 9. Обновить систему проверок (AICheck)
  - [x] 9.1 Обновить класс AICheck для использования SchedulerAdapter
    - Использовать SchedulerAdapter для доступа к данным игрока
    - Использовать SchedulerAdapter для доступа к данным мира
    - Убедиться что проверки потокобезопасны
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

  - [ ]* 9.2 Написать unit тесты для AICheck
    - Тестировать выполнение проверок
    - Тестировать потокобезопасность
    - **Property 11: Check Execution Thread Safety**
    - **Validates: Requirements 8.1, 8.2, 8.3, 8.4**
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

  - [ ]* 9.3 Написать property тесты для AICheck
    - **Property 11: Check Execution Thread Safety**
    - **Validates: Requirements 8.1, 8.2, 8.3, 8.4**
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

- [-] 10. Обновить систему штрафов (Penalty)
  - [x] 10.1 Обновить класс Penalty для использования SchedulerAdapter
    - Использовать SchedulerAdapter для выполнения штрафов
    - Использовать SchedulerAdapter для выполнения команд
    - Убедиться что штрафы выполняются в правильном потоке
    - _Requirements: 9.1, 9.2, 9.3_

  - [ ]* 10.2 Написать unit тесты для Penalty
    - Тестировать выполнение штрафов
    - Тестировать выполнение команд
    - **Property 12: Penalty Execution Synchronization**
    - **Validates: Requirements 9.1, 9.2, 9.3**
    - _Requirements: 9.1, 9.2, 9.3_

  - [ ]* 10.3 Написать property тесты для Penalty
    - **Property 12: Penalty Execution Synchronization**
    - **Validates: Requirements 9.1, 9.2, 9.3**
    - _Requirements: 9.1, 9.2, 9.3_

- [x] 11. Обновить систему алертов (AlertManager)
  - [x] 11.1 Обновить класс AlertManager для потокобезопасности
    - Использовать SchedulerAdapter для отправки алертов
    - Убедиться что алерты отправляются безопасно
    - _Requirements: 9.3_

  - [ ]* 11.2 Написать unit тесты для AlertManager
    - Тестировать отправку алертов
    - Тестировать потокобезопасность
    - _Requirements: 9.3_   

- [x] 12. Обновить систему конфигурации
  - [x] 12.1 Обновить класс Config для поддержки параметров Folia
    - Добавить параметры для конфигурации Folia
    - Убедиться что параметры игнорируются на других серверах
    - _Requirements: 10.1, 10.2, 10.3_

  - [x] 12.2 Обновить метод reload() для безопасной перезагрузки
    - Использовать SchedulerAdapter для синхронизации
    - Убедиться что новые параметры применяются безопасно
    - _Requirements: 10.2, 10.3_

  - [ ]* 12.3 Написать unit тесты для Config
    - Тестировать загрузку конфигурации
    - Тестировать перезагрузку конфигурации
    - **Property 13: Configuration Reload Safety**
    - **Validates: Requirements 10.2, 10.3**
    - _Requirements: 10.1, 10.2, 10.3_

  - [ ]* 12.4 Написать property тесты для Config
    - **Property 13: Configuration Reload Safety**
    - **Validates: Requirements 10.2, 10.3**
    - _Requirements: 10.1, 10.2, 10.3_

- [x] 13. Checkpoint - Убедиться что все тесты проходят
  - Запустить все unit тесты
  - Запустить все property тесты
  - Убедиться что нет ошибок компиляции
  - Спросить у пользователя если есть вопросы

- [-] 14. Интеграционное тестирование
  - [x] 14.1 Создать integration тесты для Bukkit сервера
    - Тестировать загрузку плагина
    - Тестировать работу всех компонентов вместе
    - **Property 14: Cross-Server Behavioral Equivalence**
    - **Validates: Requirements 11.1, 11.2, 11.3, 11.5**
    - _Requirements: 11.1, 11.2, 11.3, 11.5_

  - [x] 14.2 Создать integration тесты для Folia сервера
    - Тестировать загрузку плагина
    - Тестировать работу всех компонентов вместе
    - **Property 14: Cross-Server Behavioral Equivalence**
    - **Validates: Requirements 11.1, 11.2, 11.3, 11.5**
    - _Requirements: 11.1, 11.2, 11.3, 11.5_

  - [ ]* 14.3 Написать property тесты для интеграции
    - **Property 14: Cross-Server Behavioral Equivalence**
    - **Validates: Requirements 11.1, 11.2, 11.3, 11.5**
    - **Property 15: Task Cancellation Idempotence**
    - **Validates: Requirements 2.1, 2.2, 2.3**
    - _Requirements: 11.1, 11.2, 11.3, 11.5_

- [x] 15. Final Checkpoint - Убедиться что все тесты проходят
  - Запустить все unit тесты
  - Запустить все property тесты
  - Запустить все integration тесты
  - Убедиться что нет ошибок компиляции
  - Спросить у пользователя если есть вопросы

## Notes

- Задачи отмеченные с `*` являются опциональными и могут быть пропущены для более быстрого MVP
- Каждая задача ссылается на конкретные требования для отслеживания
- Checkpoints обеспечивают инкрементальную валидацию
- Property тесты валидируют универсальные свойства корректности
- Unit тесты валидируют конкретные примеры и edge cases

