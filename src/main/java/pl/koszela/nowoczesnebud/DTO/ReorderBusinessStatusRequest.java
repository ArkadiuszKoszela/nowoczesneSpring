package pl.koszela.nowoczesnebud.DTO;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Data
public class ReorderBusinessStatusRequest {

    @NotNull
    private List<Long> orderedStatusIds = new ArrayList<>();
}

