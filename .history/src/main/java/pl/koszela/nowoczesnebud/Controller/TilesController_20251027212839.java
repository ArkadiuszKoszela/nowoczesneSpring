package pl.koszela.nowoczesnebud.Controller;

import org.apache.poi.util.IOUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.koszela.nowoczesnebud.CreateOffer.CreateOffer;
import pl.koszela.nowoczesnebud.Model.*;
import pl.koszela.nowoczesnebud.Service.*;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/tiles")
//@CrossOrigin(origins = "https://angular-nowoczesne-af04d5c56981.herokuapp.com")
@CrossOrigin(origins = "http://localhost:4200")
public class TilesController {

    private final TilesService tilesService;
    private final QuantityService quantityService;
    private final InputService inputService;
    private final CreateOffer createOffer;
    private final ExcelExporter excelExporter;
    private final ProductGroupService productGroupService;
    private final ProductTypeService productTypeService;

    public TilesController(TilesService tilesService, QuantityService quantityService, InputService inputService,
                           CreateOffer createOffer, ExcelExporter excelExporter,
                           ProductGroupService productGroupService,
                           ProductTypeService productTypeService) {
        this.tilesService = tilesService;
        this.quantityService = quantityService;
        this.inputService = inputService;
        this.createOffer = createOffer;
        this.excelExporter = excelExporter;
        this.productGroupService = productGroupService;
        this.productTypeService = productTypeService;
    }

    @PostMapping("/saveInputs")
    public List<Input> saveInputList(@RequestBody List<Input> inputList) {
        return inputService.saveInputList(inputList);
    }

    @GetMapping("/getAll")
    public List<Tile> getAllTiles() {
        return tilesService.getAllTilesOrCreate();
    }

    @GetMapping("/productGroup")
    public List<ProductGroup> getProductGroups(@RequestParam("id") long id) {
        return productGroupService.getProductGroupsForTile(id);
    }

    @GetMapping("/productGroups")
    public List<ProductGroup> getProductGroups() {
        return tilesService.getProductGroupsForTile();
    }

    @GetMapping("/productTypes")
    public List<ProductType> getProductTypes(@RequestParam("id") long id) {
        return productTypeService.findProductTypesByProductGroupId(id);
    }

    @PostMapping("/productType")
    public ProductType getProductTypes(@RequestParam("value") int value, @RequestBody ProductGroup productGroup) {
        return productTypeService.getProductType(value, productGroup);
    }

    @PostMapping("/map")
    public List<Tile> getTilesWithFilledQuantity(@RequestBody List<Input> input) {
        return tilesService.convertTilesToDTO(quantityService.filledQuantityInTiles(input));
    }

    @PostMapping("/generateOffer")
    public ResponseEntity<Object> generatePdf(@RequestBody Offer offer) {
        createOffer.createOffer(offer);
        String filename = "src/main/resources/templates/CommercialOffer.pdf";
        FileSystemResource resource = new FileSystemResource(filename);
        MediaType mediaType = MediaTypeFactory
                .getMediaType(resource)
                .orElse(MediaType.APPLICATION_PDF);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        ContentDisposition disposition = ContentDisposition
                .builder("inline")
                .filename(resource.getFilename())
                .build();
        headers.setContentDisposition(disposition);
        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }

    @GetMapping("/getDiscounts")
    public List<Tile> getDiscounts() {
        return tilesService.getDiscounts();
    }

    @PostMapping("/saveDiscounts")
    public List<ProductGroup> saveDiscounts(@RequestBody ProductType productType) {
        return productGroupService.saveDiscounts(productType);
    }

    @PostMapping("/editTypeOfTile")
    public DTO editTypeOfTile(@RequestBody DTO dto) {
        return tilesService.editTypeOfTile(dto);
    }

    @PostMapping("/import")
    public List<Tile> importFiles(@RequestParam("file[]") MultipartFile[] file) throws IOException {
        List<MultipartFile> array = Arrays.asList(file);
        return tilesService.getAllTile(array);
    }

    @PostMapping("/importWithNames")
    public List<Tile> importFilesWithNames(
            @RequestParam("file[]") MultipartFile[] files,
            @RequestParam("name[]") String[] names) throws IOException {
        
        if (files.length != names.length) {
            throw new IllegalArgumentException("Liczba plików musi być równa liczbie nazw");
        }
        
        List<MultipartFile> fileList = Arrays.asList(files);
        return tilesService.getAllTileWithNames(fileList, Arrays.asList(names));
    }

    @PostMapping("/setOption")
    public ProductGroup setOption(@RequestBody ProductGroup updateProductGroup) {
        return productGroupService.setOption(updateProductGroup);
    }

    @GetMapping(value = "/export/excel", produces = "application/zip")
    public void exportToExcel(HttpServletResponse response) throws IOException {
        //setting headers
        response.setStatus(HttpServletResponse.SC_OK);
        response.addHeader("Content-Disposition", "attachment; filename=\"cenniki dachówek.zip\"");

        ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());

        // create a list to add files to be zipped
        List<File> files = excelExporter.getAll();

        // package files
        for (File file : files) {
            //new zip entry and copying inputstream with file to zipOutputStream, after all closing streams
            zipOutputStream.putNextEntry(new ZipEntry(file.getName()));
            FileInputStream fileInputStream = new FileInputStream(file);

            IOUtils.copy(fileInputStream, zipOutputStream);

            fileInputStream.close();
            zipOutputStream.closeEntry();
        }
        zipOutputStream.close();
    }

    @PostMapping("/calculateMargin")
    public void calculateMargin(@RequestBody int margin) {
        List<ProductGroup> allProductGroupForTile = new ArrayList<>();
        for (ProductGroup productGroup : getAllTiles().iterator().next().getProductGroupList()) {
            if (productGroup.getOption() != null)
                allProductGroupForTile.add(productGroup);
        }
        List<ProductGroup> productGroupList = productGroupService.calculateMargin(margin, null, allProductGroupForTile);
        productGroupService.saveAll(productGroupList);
    }

    @PostMapping("/calculateDiscount")
    public void calculateDiscount(@RequestBody int discount) {
        List<ProductGroup> allProductGroupForTile = new ArrayList<>();
        for (ProductGroup productGroup : getAllTiles().iterator().next().getProductGroupList()) {
            if (productGroup.getOption() != null)
                allProductGroupForTile.add(productGroup);
        }
        List<ProductGroup> productGroupList = productGroupService.calculateMargin(null, discount, allProductGroupForTile);
        productGroupService.saveAll(productGroupList);
    }

    @GetMapping("/hasOnlyOneMainProductGroup")
    public boolean hasOnlyOneMainProductGroup () {
        return productGroupService.hasOnlyOneMainProductGroup ();
    }
}
