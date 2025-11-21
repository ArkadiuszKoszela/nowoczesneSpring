package pl.koszela.nowoczesnebud.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dla tymczasowych Input (draft inputs)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DraftInputDTO {
    private String mapperName; // Nazwa pola z formularza (np. "Powierzchnia połaci")
    private String name; // Nazwa wyświetlana
    private Double quantity; // Wartość Input (np. 200 dla "Powierzchnia połaci")
}

