package pl.koszela.nowoczesnebud.Controller;

import org.apache.poi.util.IOUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.koszela.nowoczesnebud.CreateOffer.CreateOffer;
import pl.koszela.nowoczesnebud.Model.*;
import pl.koszela.nowoczesnebud.Service.ExcelExporter;
import pl.koszela.nowoczesnebud.Service.QuantityService;
import pl.koszela.nowoczesnebud.Service.ServiceCsv;
import pl.koszela.nowoczesnebud.Service.TilesService;

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
//@CrossOrigin(origins = "https://angular-nowoczesne.herokuapp.com")
@CrossOrigin(origins = "http://localhost:4200")
public class TilesController {

    private final TilesService tilesService;
    private final QuantityService quantityService;
    private final CreateOffer createOffer;
    private final ExcelExporter excelExporter;
    private final ServiceCsv serviceCsv;

    public TilesController(TilesService tilesService, QuantityService quantityService, CreateOffer createOffer, ExcelExporter excelExporter, ServiceCsv serviceCsv) {
        this.tilesService = tilesService;
        this.quantityService = quantityService;
        this.createOffer = createOffer;
        this.excelExporter = excelExporter;
        this.serviceCsv = serviceCsv;
    }

    @GetMapping("/getAll")
    public List<Tile> getAllTiles() {
        return tilesService.getAllTilesOrCreate();
    }

    @PostMapping("/map")
    public List<Tile> getTilesWithFilledQuantity(@RequestBody List<TilesInput> tilesInput){
        return tilesService.convertTilesToDTO (quantityService.filledQuantityInTiles(tilesInput));
    }

    @PostMapping("/generateOffer")
    public ResponseEntity<Object> generatePdf(@RequestBody CommercialOffer commercialOffer) {
        createOffer.createOffer(commercialOffer);
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

//    @GetMapping("/getInputsFromName")
//    public GroupOfTiles getGroupOfTilesWithId(@RequestParam long id) {
//        return tilesService.getInputsFromId(id);
//    }

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
    public List<Tile> editTypeOfTile (@RequestBody TypeOfTile typeOfTile) {
        return tilesService.editTypeOfTile(typeOfTile);
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

}
