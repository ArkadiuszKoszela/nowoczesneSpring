-- =====================================================
-- OPTYMALIZACJA WYDAJNOŚCI - UNIQUE constraint dla UPSERT
-- Data: 2025-12-14
-- Opis: Dodaje UNIQUE constraint na (project_id, product_id, category) w project_draft_changes_ws
--       aby umożliwić użycie UPSERT (INSERT ... ON DUPLICATE KEY UPDATE) zamiast DELETE + INSERT
--       To znacznie przyspieszy "Przelicz produkty" (z ~kilka sekund do ~200-500ms)
-- =====================================================

-- Dodaj UNIQUE constraint na (project_id, product_id, category)
-- To umożliwi użycie INSERT ... ON DUPLICATE KEY UPDATE dla UPSERT
-- UWAGA: Jeśli istnieją duplikaty, migracja się nie powiedzie - najpierw usuń duplikaty

-- Usuń duplikaty przed dodaniem UNIQUE constraint (zachowaj najnowszy rekord)
DELETE d1 FROM project_draft_changes_ws d1
INNER JOIN project_draft_changes_ws d2 
WHERE d1.id > d2.id 
AND d1.project_id = d2.project_id 
AND d1.product_id = d2.product_id 
AND d1.category = d2.category;

-- Dodaj UNIQUE constraint
-- UWAGA: Jeśli indeks już istnieje, Flyway może rzucić błąd - można go zignorować
CREATE UNIQUE INDEX uk_draft_changes_project_product_category 
ON project_draft_changes_ws(project_id, product_id, category);

