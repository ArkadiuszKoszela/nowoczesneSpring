-- Dodaj kolumny windows_margin i windows_discount do tabeli project
-- Dla kategorii WINDOW (Okna)

ALTER TABLE project ADD COLUMN IF NOT EXISTS windows_margin DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE project ADD COLUMN IF NOT EXISTS windows_discount DOUBLE PRECISION DEFAULT 0.0;

-- Dodaj komentarze dla dokumentacji
COMMENT ON COLUMN project.windows_margin IS 'Marża dla kategorii OKNA (%)';
COMMENT ON COLUMN project.windows_discount IS 'Rabat dla kategorii OKNA (%)';


