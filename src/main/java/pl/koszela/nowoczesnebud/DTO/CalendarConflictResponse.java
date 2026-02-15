package pl.koszela.nowoczesnebud.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CalendarConflictResponse {
    private boolean hasConflict;
    private List<CalendarConflictItemResponse> conflicts;
}

