-- Migracja: Konwersja is_main_option z BOOLEAN na VARCHAR (enum GroupOption)
-- Wartości: true → 'MAIN', false → 'OPTIONAL', NULL → 'NONE'
-- Obsługuje H2 (tryb MySQL), MySQL/MariaDB

-- 1. Konwersja w tabeli project_product_groups
-- Krok 1: Utwórz kolumnę tymczasową z konwertowanymi wartościami
ALTER TABLE project_product_groups 
  ADD COLUMN is_main_option_temp VARCHAR(20);

-- Krok 2: Skopiuj i skonwertuj wartości z Boolean na enum
UPDATE project_product_groups 
  SET is_main_option_temp = CASE 
    WHEN is_main_option = TRUE THEN 'MAIN'
    WHEN is_main_option = FALSE THEN 'OPTIONAL'
    WHEN is_main_option IS NULL THEN 'NONE'
    ELSE 'NONE'
  END;

-- Krok 3: Usuń starą kolumnę
ALTER TABLE project_product_groups 
  DROP COLUMN is_main_option;

-- Krok 4: Dodaj kolumnę z właściwą nazwą i skopiuj wartości
ALTER TABLE project_product_groups 
  ADD COLUMN is_main_option VARCHAR(20);

UPDATE project_product_groups 
  SET is_main_option = is_main_option_temp;

-- Krok 5: Usuń kolumnę tymczasową
ALTER TABLE project_product_groups 
  DROP COLUMN is_main_option_temp;

-- Krok 6: Ustaw wartości NULL na 'NONE' (na wypadek gdyby coś zostało)
UPDATE project_product_groups 
  SET is_main_option = 'NONE'
  WHERE is_main_option IS NULL;

-- 2. Konwersja w tabeli project_draft_changes_ws
-- Krok 1: Utwórz kolumnę tymczasową z konwertowanymi wartościami
ALTER TABLE project_draft_changes_ws 
  ADD COLUMN draft_is_main_option_temp VARCHAR(20);

-- Krok 2: Skopiuj i skonwertuj wartości z Boolean na enum
UPDATE project_draft_changes_ws 
  SET draft_is_main_option_temp = CASE 
    WHEN draft_is_main_option = TRUE THEN 'MAIN'
    WHEN draft_is_main_option = FALSE THEN 'OPTIONAL'
    WHEN draft_is_main_option IS NULL THEN 'NONE'
    ELSE 'NONE'
  END;

-- Krok 3: Usuń starą kolumnę
ALTER TABLE project_draft_changes_ws 
  DROP COLUMN draft_is_main_option;

-- Krok 4: Dodaj kolumnę z właściwą nazwą i skopiuj wartości
ALTER TABLE project_draft_changes_ws 
  ADD COLUMN draft_is_main_option VARCHAR(20);

UPDATE project_draft_changes_ws 
  SET draft_is_main_option = draft_is_main_option_temp;

-- Krok 5: Usuń kolumnę tymczasową
ALTER TABLE project_draft_changes_ws 
  DROP COLUMN draft_is_main_option_temp;

-- Krok 6: Ustaw wartości NULL na 'NONE' (na wypadek gdyby coś zostało)
UPDATE project_draft_changes_ws 
  SET draft_is_main_option = 'NONE'
  WHERE draft_is_main_option IS NULL;
