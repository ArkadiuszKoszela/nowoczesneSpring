package pl.koszela.nowoczesnebud.DTO;

import lombok.Data;
import pl.koszela.nowoczesnebud.Model.ProductCategory;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * Request do zapisu atrybutów dla grupy produktowej
 */
@Data
public class GroupAttributesRequest {

    @NotNull(message = "Kategoria jest wymagana")
    private ProductCategory category;

    @NotBlank(message = "Producent jest wymagany")
    private String manufacturer;

    @NotBlank(message = "Nazwa grupy jest wymagana")
    private String groupName;

    /**
     * Atrybuty w formacie: {"kolor":["czerwony","brązowy"],"kształt":["płaska"]}
     * Może być null (wtedy usuwamy atrybuty dla grupy)
     */
    private Map<String, java.util.List<String>> attributes;
}

