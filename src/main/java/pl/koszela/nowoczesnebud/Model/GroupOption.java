package pl.koszela.nowoczesnebud.Model;

/**
 * Enum dla opcji grup produktowych
 * Zastępuje Boolean (true/false/null) dla lepszej obsługi w UI
 */
public enum GroupOption {
    /**
     * Główna - tylko jedna grupa w kategorii może być główna
     */
    MAIN,
    
    /**
     * Opcjonalna - może być kilka grup opcjonalnych
     */
    OPTIONAL,
    
    /**
     * Nie wybrano - domyślna wartość (zastępuje null)
     */
    NONE
}





