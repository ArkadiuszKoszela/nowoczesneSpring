-- Migracja: Uzupełnij quantity_converter dla istniejących snapshotów
-- Jeśli snapshot nie ma quantity_converter lub ma 1.0, pobierz z aktualnego produktu

-- Uzupełnij quantity_converter dla istniejących snapshotów na podstawie aktualnych produktów
UPDATE price_list_snapshot_items psi
INNER JOIN products p ON psi.product_id = p.id
SET psi.quantity_converter = p.quantity_converter
WHERE (psi.quantity_converter IS NULL OR psi.quantity_converter = 1.0)
  AND p.quantity_converter IS NOT NULL 
  AND p.quantity_converter != 1.0;

-- Uzupełnij unit dla istniejących snapshotów na podstawie aktualnych produktów
UPDATE price_list_snapshot_items psi
INNER JOIN products p ON psi.product_id = p.id
SET psi.unit = p.unit
WHERE psi.unit IS NULL
  AND p.unit IS NOT NULL;








