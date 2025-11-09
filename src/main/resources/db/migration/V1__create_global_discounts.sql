-- Tabela rabatów globalnych
-- Obsługuje rabaty dla całych kategorii produktów (TILE, GUTTER, ACCESSORY)

CREATE TABLE IF NOT EXISTS global_discounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- Kategoria produktu (TILE, GUTTER, ACCESSORY)
    category VARCHAR(50) NOT NULL,
    
    -- Typ rabatu (MAIN - główny, OPTIONAL - opcjonalny)
    type VARCHAR(50) NOT NULL,
    
    -- Procent rabatu (0.0 - 100.0)
    discount_percent DOUBLE NOT NULL,
    
    -- Opis rabatu (opcjonalnie)
    description VARCHAR(500),
    
    -- Data rozpoczęcia obowiązywania rabatu
    valid_from DATE NOT NULL,
    
    -- Data zakończenia obowiązywania rabatu
    valid_to DATE NOT NULL,
    
    -- Czy rabat jest aktywny (soft delete)
    active BOOLEAN DEFAULT TRUE NOT NULL,
    
    -- Indeksy dla optymalizacji zapytań
    INDEX idx_category (category),
    INDEX idx_active (active),
    INDEX idx_dates (valid_from, valid_to),
    INDEX idx_category_type (category, type),
    
    -- Constraints
    CHECK (discount_percent >= 0 AND discount_percent <= 100),
    CHECK (valid_from <= valid_to),
    CHECK (category IN ('TILE', 'GUTTER', 'ACCESSORY')),
    CHECK (type IN ('MAIN', 'OPTIONAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Komentarze do kolumn
ALTER TABLE global_discounts COMMENT = 'Rabaty globalne dla całych kategorii produktów';

-- Przykładowe dane testowe (opcjonalnie - zakomentowane)
/*
INSERT INTO global_discounts (category, type, discount_percent, description, valid_from, valid_to, active) 
VALUES 
  ('TILE', 'MAIN', 15.0, 'Promocja jesienna 2024', '2024-10-01', '2024-12-31', TRUE),
  ('TILE', 'OPTIONAL', 5.0, 'Rabat dla stałych klientów', '2024-01-01', '2024-12-31', TRUE),
  ('GUTTER', 'MAIN', 10.0, 'Rabat na rynny', '2024-10-01', '2024-12-31', TRUE);
*/














