package pl.koszela.nowoczesnebud.Service;

import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.Product;

import java.util.ArrayList;
import java.util.List;

/**
 * Serwis walidacji produktów - logika biznesowa
 */
@Service
public class ProductValidationService {

    /**
     * Waliduj produkt przed zapisem
     */
    public ValidationResult validate(Product product) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // === WALIDACJA KRYTYCZNA (błędy) ===

        // 1. Cena sprzedaży nie może być niższa niż cena zakupu (STRATA!)
        if (product.getSellingPrice() != null && product.getPurchasePrice() != null &&
            product.getSellingPrice() > 0 && product.getPurchasePrice() > 0) {
            
            if (product.getSellingPrice() < product.getPurchasePrice()) {
                errors.add(String.format(
                    "Produkt '%s': Cena sprzedaży (%.2f PLN) jest niższa niż cena zakupu (%.2f PLN) - STRATA!",
                    product.getName(),
                    product.getSellingPrice(),
                    product.getPurchasePrice()
                ));
            }
        }

        // 2. Suma rabatów nie może przekraczać 100%
        int totalDiscount = (product.getBasicDiscount() != null ? product.getBasicDiscount() : 0) +
                           (product.getAdditionalDiscount() != null ? product.getAdditionalDiscount() : 0) +
                           (product.getPromotionDiscount() != null ? product.getPromotionDiscount() : 0) +
                           (product.getSkontoDiscount() != null ? product.getSkontoDiscount() : 0);

        if (totalDiscount > 100) {
            errors.add(String.format(
                "Produkt '%s': Suma rabatów wynosi %d%% (przekracza 100%%)",
                product.getName(),
                totalDiscount
            ));
        }

        // 3. Ceny nie mogą być ujemne
        if (product.getRetailPrice() != null && product.getRetailPrice() < 0) {
            errors.add(String.format(
                "Produkt '%s': Cena katalogowa nie może być ujemna",
                product.getName()
            ));
        }

        if (product.getPurchasePrice() != null && product.getPurchasePrice() < 0) {
            errors.add(String.format(
                "Produkt '%s': Cena zakupu nie może być ujemna",
                product.getName()
            ));
        }

        if (product.getSellingPrice() != null && product.getSellingPrice() < 0) {
            errors.add(String.format(
                "Produkt '%s': Cena sprzedaży nie może być ujemna",
                product.getName()
            ));
        }

        // 4. Rabaty muszą być w zakresie 0-100%
        if (product.getBasicDiscount() != null && 
            (product.getBasicDiscount() < 0 || product.getBasicDiscount() > 100)) {
            errors.add(String.format(
                "Produkt '%s': Rabat podstawowy musi być w zakresie 0-100%%",
                product.getName()
            ));
        }

        // === WALIDACJA OSTRZEGAWCZA (warnings) ===

        // 1. Bardzo wysokie rabaty (suma > 50%)
        if (totalDiscount > 50 && totalDiscount <= 100) {
            warnings.add(String.format(
                "Produkt '%s': Suma rabatów wynosi %d%% (bardzo wysokie rabaty)",
                product.getName(),
                totalDiscount
            ));
        }

        // 2. Brak ceny katalogowej
        if (product.getRetailPrice() == null || product.getRetailPrice() == 0) {
            warnings.add(String.format(
                "Produkt '%s': Brak ceny katalogowej",
                product.getName()
            ));
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    /**
     * Waliduj wiele produktów
     */
    public BatchValidationResult validateBatch(List<Product> products) {
        List<ValidationResult> results = new ArrayList<>();
        int validCount = 0;
        int errorCount = 0;
        int warningCount = 0;

        for (Product product : products) {
            ValidationResult result = validate(product);
            results.add(result);

            if (result.isValid()) {
                validCount++;
            } else {
                errorCount++;
            }

            if (!result.getWarnings().isEmpty()) {
                warningCount++;
            }
        }

        return new BatchValidationResult(
            errorCount == 0,
            validCount,
            errorCount,
            warningCount,
            results
        );
    }

    /**
     * Wynik walidacji pojedynczego produktu
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;

        public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors;
            this.warnings = warnings;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }
    }

    /**
     * Wynik walidacji wielu produktów
     */
    public static class BatchValidationResult {
        private final boolean allValid;
        private final int validCount;
        private final int errorCount;
        private final int warningCount;
        private final List<ValidationResult> results;

        public BatchValidationResult(boolean allValid, int validCount, int errorCount, 
                                     int warningCount, List<ValidationResult> results) {
            this.allValid = allValid;
            this.validCount = validCount;
            this.errorCount = errorCount;
            this.warningCount = warningCount;
            this.results = results;
        }

        public boolean isAllValid() {
            return allValid;
        }

        public int getValidCount() {
            return validCount;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public int getWarningCount() {
            return warningCount;
        }

        public List<ValidationResult> getResults() {
            return results;
        }

        public List<String> getAllErrors() {
            List<String> allErrors = new ArrayList<>();
            for (ValidationResult result : results) {
                allErrors.addAll(result.getErrors());
            }
            return allErrors;
        }

        public List<String> getAllWarnings() {
            List<String> allWarnings = new ArrayList<>();
            for (ValidationResult result : results) {
                allWarnings.addAll(result.getWarnings());
            }
            return allWarnings;
        }
    }
}

