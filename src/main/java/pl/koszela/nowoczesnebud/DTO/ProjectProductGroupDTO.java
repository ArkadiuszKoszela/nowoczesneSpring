package pl.koszela.nowoczesnebud.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import pl.koszela.nowoczesnebud.Model.GroupOption;
import pl.koszela.nowoczesnebud.Model.ProductCategory;

/**
 * DTO zwracane z backendu - opcje grupy produktowej zapisane w projekcie
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectProductGroupDTO {
    private ProductCategory category;
    private String manufacturer;
    private String groupName;
    private GroupOption isMainOption;
}

