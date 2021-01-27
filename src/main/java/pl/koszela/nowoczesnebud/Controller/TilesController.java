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
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/tiles")
@CrossOrigin(origins = "https://angular-nowoczesne.herokuapp.com")
//@CrossOrigin(origins = "http://localhost:4200")
public class TilesController {

    private final TilesService tilesService;
    private final QuantityService quantityService;
    private final InputService inputService;
    private final CreateOffer createOffer;
    private final ExcelExporter excelExporter;

    public TilesController(TilesService tilesService, QuantityService quantityService, InputService inputService, CreateOffer createOffer, ExcelExporter excelExporter) {
        this.tilesService = tilesService;
        this.quantityService = quantityService;
        this.inputService = inputService;
        this.createOffer = createOffer;
        this.excelExporter = excelExporter;
    }

    @PostMapping("/saveInputs")
    public List<Input> saveInputList (@RequestBody List<Input> inputList) {
        return inputService.saveInputList(inputList);
    }

    @GetMapping("/getAll")
    public List<Tile> getAllTiles() {
        return tilesService.getAllTilesOrCreate();
    }

    @GetMapping("/productGroups")
    public List<ProductGroup> getProductGroups(@RequestParam ("id") long id) {
        return tilesService.getProductGroups(id);
    }

    @GetMapping("/productTypes")
    public List<ProductType> getProductTypes(@RequestParam ("id") long id) {
        return tilesService.getProductTypes(id);
    }

    @PostMapping("/map")
    public List<Tile> getTilesWithFilledQuantity(@RequestBody List<Input> input){
        return tilesService.convertTilesToDTO (quantityService.filledQuantityInTiles(input));
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

    @GetMapping("/clear")
    public List<Tile> clearQuantityInTiles (){
        return tilesService.clearQuantity();
    }

    @GetMapping("getManufacturers")
    public List<String> getManufacturers () {
        return tilesService.getTilesManufacturers ();
    }

    @GetMapping("/getDiscounts")
    public List<Tile> getDiscounts() {
        return tilesService.getDiscounts ();
    }

    @PostMapping("/saveDiscounts")
    public List<Tile> saveDiscounts(@RequestBody Tile tile) {
        return tilesService.saveDiscounts (tile);
    }

    @PostMapping("/editTypeOfTile")
    public DTO editTypeOfTile (@RequestBody DTO dto) {
        return tilesService.editTypeOfTile(dto);
    }

    @GetMapping(value="/export/excel", produces="application/zip")
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

    @PostMapping ("/import")
    public List<Tile> importFiles (@RequestParam("file[]") MultipartFile[] file) throws IOException {
        List<MultipartFile> array = Arrays.asList(file);
        List<Tile> list = tilesService.getAllTile(array);
        return list;
    }

    @PostMapping("/setOption")
    public ProductGroup setOption (@RequestBody ProductGroup updateProductGroup) {
        return tilesService.setOption(updateProductGroup);
    }



}
