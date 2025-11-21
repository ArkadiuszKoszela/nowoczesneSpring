package pl.koszela.nowoczesnebud.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import pl.koszela.nowoczesnebud.Model.ProductCategory;

/**
 * DTO dla zapisu opcji grupy produktowej w projekcie
 * Wysyłane z frontendu podczas kliknięcia "Zapisz projekt"
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveProjectProductGroupDTO {
    private ProductCategory category;
    private String manufacturer;
    private String groupName;
    private Boolean isMainOption; // true=Główna, false=Opcjonalna
}

