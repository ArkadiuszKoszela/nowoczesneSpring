-- Migracja: Konwersja is_main_option z BOOLEAN na VARCHAR (enum GroupOption)
-- Wartości: true → 'MAIN', false → 'OPTIONAL', NULL → 'NONE'
-- Obsługuje H2, MySQL/MariaDB

-- 1. Konwersja w tabeli project_product_groups
-- Krok 1: Najpierw zaktualizuj wartości Boolean na stringi (przed zmianą typu)
UPDATE project_product_groups 
  SET is_main_option = CASE 
    WHEN is_main_option = TRUE OR is_main_option = 1 THEN 'MAIN'
    WHEN is_main_option = FALSE OR is_main_option = 0 THEN 'OPTIONAL'
    WHEN is_main_option IS NULL THEN 'NONE'
    ELSE 'NONE'
  END
  WHERE is_main_option IS NOT NULL AND CAST(is_main_option AS VARCHAR) NOT IN ('MAIN', 'OPTIONAL', 'NONE');

-- Krok 2: Zmień typ kolumny na VARCHAR
ALTER TABLE project_product_groups 
  ALTER COLUMN is_main_option VARCHAR(20);

-- Krok 3: Ustaw wartości NULL na 'NONE'
UPDATE project_product_groups 
  SET is_main_option = 'NONE'
  WHERE is_main_option IS NULL;

-- 2. Konwersja w tabeli project_draft_changes_ws
-- Krok 1: Najpierw zaktualizuj wartości Boolean na stringi (przed zmianą typu)
UPDATE project_draft_changes_ws 
  SET draft_is_main_option = CASE 
    WHEN draft_is_main_option = TRUE OR draft_is_main_option = 1 THEN 'MAIN'
    WHEN draft_is_main_option = FALSE OR draft_is_main_option = 0 THEN 'OPTIONAL'
    WHEN draft_is_main_option IS NULL THEN 'NONE'
    ELSE 'NONE'
  END
  WHERE draft_is_main_option IS NOT NULL AND CAST(draft_is_main_option AS VARCHAR) NOT IN ('MAIN', 'OPTIONAL', 'NONE');

-- Krok 2: Zmień typ kolumny na VARCHAR
ALTER TABLE project_draft_changes_ws 
  ALTER COLUMN draft_is_main_option VARCHAR(20);

-- Krok 3: Ustaw wartości NULL na 'NONE'
UPDATE project_draft_changes_ws 
  SET draft_is_main_option = 'NONE'
  WHERE draft_is_main_option IS NULL;


