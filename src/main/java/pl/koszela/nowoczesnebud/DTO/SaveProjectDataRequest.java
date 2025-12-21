package pl.koszela.nowoczesnebud.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO dla zapisu wszystkich danych projektu
 * POST /api/projects/{id}/save-data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveProjectDataRequest {
    // Globalne rabaty dla zakładek
    private Double tilesMargin;
    private Double tilesDiscount;
    private Double guttersMargin;
    private Double guttersDiscount;
    private Double accessoriesMargin;
    private Double accessoriesDiscount;
    private Double servicesMargin;
    private Double servicesDiscount;
    
    // Dane produktów (z zakładek Dachówki/Rynny/Akcesoria/Usługi)
    private List<SaveProjectProductDTO> products;
    
    // Opcje grup produktowych (Główna/Opcjonalna)
    private List<SaveProjectProductGroupDTO> productGroups;
}
