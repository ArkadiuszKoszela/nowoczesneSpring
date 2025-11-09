-- Dodanie kolumn do tabeli inputs dla opcji grup produktów (Główna/Opcjonalna)

ALTER TABLE inputs
ADD COLUMN group_manufacturer VARCHAR(255) NULL,
ADD COLUMN group_name VARCHAR(255) NULL,
ADD COLUMN is_main_option BOOLEAN NULL;

-- Indeks dla szybszego wyszukiwania opcji grup po manufacturer i groupName
CREATE INDEX idx_inputs_group_option ON inputs(group_manufacturer, group_name) 
WHERE group_manufacturer IS NOT NULL AND group_name IS NOT NULL;


