package pl.koszela.nowoczesnebud.Model;

/**
 * Metody obliczania końcowego rabatu z 4 składowych rabatów
 */
public enum DiscountCalculationMethod {
    /**
     * Sumaryczny: basic + additional + promotion + skonto
     * Przykład: 25% + 10% + 10% + 3% = 48%
     */
    SUMARYCZNY,
    
    /**
     * Kaskadowo A: podstawowy → dodatkowy → skonto (bez promocyjnego)
     * 100 - 25% = 75 - 10% = 67.5 - 3% = 65.48 → wynik: 34.52%
     */
    KASKADOWO_A,
    
    /**
     * Kaskadowo B: podstawowy → dodatkowy → promocyjny → skonto
     * 100 - 25% = 75 - 10% = 67.5 - 10% = 60.75 - 3% = 58.93 → wynik: 41.07%
     */
    KASKADOWO_B,
    
    /**
     * Kaskadowo C: podstawowy → (dodatkowy + promocyjny) → skonto
     * 100 - 25% = 75 - (10% + 10%) = 60 - 3% = 58.2 → wynik: 41.8%
     */
    KASKADOWO_C,
    
    /**
     * Kaskadowo D: (podstawowy + dodatkowy + promocyjny) → skonto
     * 25% + 10% + 10% = 45%, 100 - 45% = 55 - 3% = 53.35 → wynik: 46.65%
     */
    KASKADOWO_D
}

