package pl.koszela.nowoczesnebud.Model;

/**
 * Enum określający źródło zmiany ceny sprzedaży produktu w projekcie
 */
public enum PriceChangeSource {
    /**
     * Cena automatyczna z cennika (domyślna)
     */
    AUTO,
    
    /**
     * Cena obliczona przez przycisk "Marża" (cena zakupu + marża%)
     */
    MARGIN,
    
    /**
     * Cena obliczona przez przycisk "Rabat" (cena katalogowa - rabat%)
     */
    DISCOUNT,
    
    /**
     * Cena zmieniona ręcznie przez użytkownika
     */
    MANUAL
}

