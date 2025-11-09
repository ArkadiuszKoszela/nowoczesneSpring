-- Dodanie kolumny manual_purchase_price do tabeli inputs dla override'u ceny zakupu produktów

ALTER TABLE inputs
ADD COLUMN manual_purchase_price DOUBLE NULL;


