package pl.koszela.nowoczesnebud.Controller;

import org.springframework.web.bind.annotation.*;
import pl.koszela.nowoczesnebud.Model.Gutter;
import pl.koszela.nowoczesnebud.Model.TilesInput;
import pl.koszela.nowoczesnebud.Service.GuttersService;
import pl.koszela.nowoczesnebud.Service.QuantityService;

import java.util.List;

@RestController
@RequestMapping("/api/gutters")
@CrossOrigin(origins = "https://angular-nowoczesne.herokuapp.com")
//@CrossOrigin(origins = "http://localhost:4200")
public class GuttersController {

    private final GuttersService guttersService;
    private final QuantityService quantityService;

    public GuttersController(GuttersService guttersService, QuantityService quantityService) {
        this.guttersService = guttersService;
        this.quantityService = quantityService;
    }

    @GetMapping("/getAll")
    public List<Gutter> getAllGutters() {
        return guttersService.getAllGutters();
    }

    @PostMapping("/map")
    public List<Gutter> getTilesWithFilledQuantity(@RequestBody List<TilesInput> tilesInput){
        return quantityService.filledQuantityInGutters(tilesInput);
    }

    @GetMapping("/getDiscounts")
    public List<Gutter> getDiscounts() {
        return guttersService.getDiscounts ();
    }

    @PostMapping("/saveDiscounts")
    public List<Gutter> saveDiscounts(@RequestBody Gutter gutter) {
        return guttersService.saveDiscounts (gutter);
    }
}
