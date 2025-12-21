-- Dodanie kolumny display_order do tabeli products
-- Migracja idempotentna - sprawdza czy kolumna już istnieje przed dodaniem
-- Działa zarówno dla nowej bazy jak i dla bazy gdzie kolumna już istnieje

-- Sprawdź czy kolumna istnieje i dodaj tylko jeśli nie istnieje
SET @dbname = DATABASE();
SET @tablename = 'products';
SET @columnname = 'display_order';

SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname)
  ) > 0,
  'SELECT 1', -- Kolumna już istnieje - nie rób nic
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' INT DEFAULT 0')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Utworzenie indeksu dla wydajności sortowania (tylko jeśli nie istnieje)
SET @indexname = 'idx_product_display_order';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (INDEX_NAME = @indexname)
  ) > 0,
  'SELECT 1', -- Indeks już istnieje - nie rób nic
  CONCAT('CREATE INDEX ', @indexname, ' ON ', @tablename, '(category, manufacturer, group_name, display_order)')
));
PREPARE createIndexIfNotExists FROM @preparedStatement;
EXECUTE createIndexIfNotExists;
DEALLOCATE PREPARE createIndexIfNotExists;
