-- Dodanie kolumn do tabeli inputs dla override'ów ceny i ilości produktów
-- productId != null oznacza że to override dla produktu (nie Input z formularza)

ALTER TABLE inputs
ADD COLUMN product_id BIGINT NULL,
ADD COLUMN manual_selling_price DOUBLE NULL,
ADD COLUMN manual_quantity DOUBLE NULL;

-- Indeks dla szybszego wyszukiwania override'ów po productId
CREATE INDEX idx_inputs_product_id ON inputs(product_id) WHERE product_id IS NOT NULL;






