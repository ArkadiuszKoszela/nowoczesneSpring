package pl.koszela.nowoczesnebud.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BusinessStatusResponse {
    private Long id;
    private String name;
    private String code;
    private String color;
    private Integer sortOrder;
    private Boolean active;
    private Boolean requiresNextTask;
    private Boolean requiresLossReason;
    private Boolean terminal;
}

