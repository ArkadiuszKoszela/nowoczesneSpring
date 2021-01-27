package pl.koszela.nowoczesnebud.Controller;

import org.springframework.web.bind.annotation.*;
import pl.koszela.nowoczesnebud.Model.*;
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

    @GetMapping("/productGroups")
    public List<ProductGroup> getProductGroups(@RequestParam ("id") long id) {
        return guttersService.getProductGroups(id);
    }

    @GetMapping("/productTypes")
    public List<ProductType> getProductTypes(@RequestParam ("id") long id) {
        return guttersService.getProductTypes(id);
    }

    @PostMapping("/map")
    public List<Gutter> getTilesWithFilledQuantity(@RequestBody List<Input> input){
        return quantityService.filledQuantityInGutters(input);
    }

    @PostMapping("/editTypeOfTile")
    public DTO editTypeOfTile (@RequestBody DTO dto) {
        return guttersService.editType(dto);
    }

    @GetMapping("/getDiscounts")
    public List<Gutter> getDiscounts() {
        return guttersService.getDiscounts ();
    }

    @PostMapping("/saveDiscounts")
    public List<Gutter> saveDiscounts(@RequestBody Gutter gutter) {
        return guttersService.saveDiscounts (gutter);
    }

    @PostMapping("/setOption")
    public ProductGroup setOption (@RequestBody ProductGroup updateProductGroup) {
        return guttersService.setOption(updateProductGroup);
    }
}
