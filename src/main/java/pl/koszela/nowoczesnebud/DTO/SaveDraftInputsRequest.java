package pl.koszela.nowoczesnebud.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request do zapisania draft inputs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveDraftInputsRequest {
    private List<DraftInputDTO> inputs;
}


