-- Tabele snapshotów cenników
-- Snapshoty przechowują stan cennika z określonej daty
-- Wiele projektów może korzystać z tego samego snapshotu

-- Tabela snapshotów cenników
CREATE TABLE IF NOT EXISTS price_list_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- Data snapshotu (data dla której snapshot jest ważny)
    snapshot_date TIMESTAMP NOT NULL,
    
    -- Kategoria produktów (TILE, GUTTER, ACCESSORY)
    category VARCHAR(50) NOT NULL,
    
    -- Data utworzenia snapshotu w bazie
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Indeksy dla optymalizacji zapytań
    INDEX idx_category (category),
    INDEX idx_snapshot_date (snapshot_date),
    INDEX idx_category_date (category, snapshot_date),
    
    -- Constraints
    CHECK (category IN ('TILE', 'GUTTER', 'ACCESSORY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Komentarz do tabeli
ALTER TABLE price_list_snapshots COMMENT = 'Snapshoty cenników dla określonej daty i kategorii';

-- Tabela pozycji snapshotu (produkty w snapshotcie)
CREATE TABLE IF NOT EXISTS price_list_snapshot_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- Relacja do snapshotu
    price_list_snapshot_id BIGINT NOT NULL,
    
    -- Referencja do produktu w cenniku (może być null jeśli produkt usunięty)
    product_id BIGINT,
    
    -- Pełny snapshot danych produktu
    name VARCHAR(500),
    manufacturer VARCHAR(255),
    group_name VARCHAR(255),
    category VARCHAR(50),
    mapper_name VARCHAR(255),
    
    -- Ceny z momentu snapshotu
    retail_price DOUBLE DEFAULT 0.0,
    purchase_price DOUBLE DEFAULT 0.0,
    selling_price DOUBLE DEFAULT 0.0,
    
    -- Rabaty z momentu snapshotu
    basic_discount INT DEFAULT 0,
    promotion_discount INT DEFAULT 0,
    additional_discount INT DEFAULT 0,
    skonto_discount INT DEFAULT 0,
    
    -- Marża z momentu snapshotu
    margin_percent DOUBLE DEFAULT 0.0,
    
    -- Jednostka i konwerter z momentu snapshotu
    unit VARCHAR(50),
    quantity_converter DOUBLE DEFAULT 1.0,
    
    -- Opcja produktu
    is_main_option BOOLEAN,
    
    -- Data utworzenia pozycji w bazie
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Klucz obcy
    FOREIGN KEY (price_list_snapshot_id) REFERENCES price_list_snapshots(id) ON DELETE CASCADE,
    
    -- Indeksy
    INDEX idx_snapshot_id (price_list_snapshot_id),
    INDEX idx_product_id (product_id),
    INDEX idx_category (category),
    
    -- Constraints
    CHECK (category IN ('TILE', 'GUTTER', 'ACCESSORY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Komentarz do tabeli
ALTER TABLE price_list_snapshot_items COMMENT = 'Pozycje produktów w snapshotcie cennika';

-- Dodaj kolumnę snapshot_date do tabeli projects
ALTER TABLE projects ADD COLUMN IF NOT EXISTS snapshot_date TIMESTAMP NULL;

-- Indeks dla snapshot_date w projects
CREATE INDEX IF NOT EXISTS idx_projects_snapshot_date ON projects(snapshot_date);

-- Komentarz do kolumny
ALTER TABLE projects MODIFY COLUMN snapshot_date TIMESTAMP NULL COMMENT 'Data snapshotu cennika użytego w projekcie';

