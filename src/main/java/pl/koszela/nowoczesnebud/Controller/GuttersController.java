package pl.koszela.nowoczesnebud.Controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.koszela.nowoczesnebud.Model.*;
import pl.koszela.nowoczesnebud.Service.GuttersService;
import pl.koszela.nowoczesnebud.Service.ProductGroupService;
import pl.koszela.nowoczesnebud.Service.ProductTypeService;
import pl.koszela.nowoczesnebud.Service.QuantityService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/gutters")
@CrossOrigin(origins = "https://angular-nowoczesne-af04d5c56981.herokuapp.com")
//@CrossOrigin(origins = "http://localhost:4200")
public class GuttersController {

    private final GuttersService guttersService;
    private final QuantityService quantityService;
    private final ProductGroupService productGroupService;
    private final ProductTypeService productTypeService;

    public GuttersController(GuttersService guttersService, QuantityService quantityService,
                             ProductGroupService productGroupService,
                             ProductTypeService productTypeService) {
        this.guttersService = guttersService;
        this.quantityService = quantityService;
        this.productGroupService = productGroupService;
        this.productTypeService = productTypeService;
    }

    @GetMapping("/getAll")
    public List<Gutter> getAllGutters() {
        return guttersService.getAllGutters();
    }

    @GetMapping("/productGroups")
    public List<ProductGroup> getProductGroups() {
        return guttersService.getProductGroupsForGutter();
    }

    @GetMapping("/productGroup")
    public List<ProductGroup> getProductGroups(@RequestParam ("id") long id) {
        return productGroupService.getProductGroupsForGutter(id);
    }

    @GetMapping("/productTypes")
    public List<ProductType> getProductTypes(@RequestParam ("id") long id) {
        return productTypeService.findProductTypesByProductGroupId(id);
    }

    @PostMapping("/map")
    public List<Gutter> getTilesWithFilledQuantity(@RequestBody List<Input> input){
        return quantityService.filledQuantityInGutters(input);
    }

    @PostMapping ("/import")
    public List<Gutter> importFiles (@RequestParam("file[]") MultipartFile[] file) throws IOException {
        List<MultipartFile> array = Arrays.asList(file);
        return guttersService.importGutters(array);
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
    public List<ProductGroup> saveDiscounts(@RequestBody ProductType productType) {
        return productGroupService.saveDiscounts (productType);
    }

    @PostMapping("/setOption")
    public ProductGroup setOption (@RequestBody ProductGroup updateProductGroup) {
        return productGroupService.setOption(updateProductGroup);
    }

    @PostMapping("/calculateMargin")
    public void calculateMargin(@RequestBody int margin) {
        List<ProductGroup> allProductGroupForGutter = new ArrayList<>();
        for (ProductGroup productGroup : guttersService.getAllGutters().iterator().next().getProductGroupList()) {
            if (productGroup.getOption() != null)
                allProductGroupForGutter.add(productGroup);
        }
        List<ProductGroup> productGroupList = productGroupService.calculateMargin(margin, null, allProductGroupForGutter);
        productGroupService.saveAll(productGroupList);
    }

    @PostMapping("/calculateDiscount")
    public void calculateDiscount (@RequestBody int discount) {
        List<ProductGroup> allProductGroupForGutter = new ArrayList<>();
        for (ProductGroup productGroup : guttersService.getAllGutters().iterator().next().getProductGroupList()) {
            if (productGroup.getOption() != null)
                allProductGroupForGutter.add(productGroup);
        }
        List<ProductGroup> productGroupList = productGroupService.calculateMargin(null, discount, allProductGroupForGutter);
        productGroupService.saveAll(productGroupList);
    }
}
