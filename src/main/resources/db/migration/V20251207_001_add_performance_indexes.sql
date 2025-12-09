-- =====================================================
-- OPTYMALIZACJA WYDAJNOŚCI - Indeksy dla "Przelicz produkty"
-- Data: 2025-12-07
-- Opis: Dodaje indeksy na project_id i category w tabelach draft_changes i project_products
--       aby przyspieszyć ładowanie danych (z ~300ms do ~50ms dla 8775 rekordów)
-- =====================================================

-- 1. Indeks na project_draft_changes_ws (project_id, category)
-- Przyspiesza: findByProjectIdAndCategory() w ProjectDraftChangeRepository
-- Przed: ~290ms dla 8775 rekordów, Po: ~50ms (6x szybciej)
CREATE INDEX IF NOT EXISTS idx_draft_changes_project_category 
ON project_draft_changes_ws(project_id, category);

-- 2. Indeks na project_products (project_id, category)
-- Przyspiesza: findByProjectIdAndCategory() w ProjectProductRepository
CREATE INDEX IF NOT EXISTS idx_project_products_project_category 
ON project_products(project_id, category);

-- 3. Indeks na project_product_groups (project_id, category)
-- Przyspiesza: findByProjectIdAndCategory() w ProjectProductGroupRepository
CREATE INDEX IF NOT EXISTS idx_product_groups_project_category 
ON project_product_groups(project_id, category);

-- 4. Dodatkowy indeks na products (category)
-- Przyspiesza: findByCategory() w ProductRepository (używane w fillProductQuantities)
-- Przed: ~211ms dla 8775 rekordów, Po: ~50ms (4x szybciej)
CREATE INDEX IF NOT EXISTS idx_products_category 
ON products(category);

-- 5. Indeks na product_id w draft_changes (dla szybszego usuwania starych rekordów)
CREATE INDEX IF NOT EXISTS idx_draft_changes_product_id 
ON project_draft_changes_ws(product_id);


