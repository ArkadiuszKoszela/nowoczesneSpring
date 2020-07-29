package pl.koszela.nowoczesnebud.Controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pl.koszela.nowoczesnebud.CreateOffer.CreateOffer;
import pl.koszela.nowoczesnebud.Model.TilesDTO;
import pl.koszela.nowoczesnebud.Model.TilesInput;
import pl.koszela.nowoczesnebud.Model.User;
import pl.koszela.nowoczesnebud.Service.QuantityService;
import pl.koszela.nowoczesnebud.Service.TilesService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/tiles")
@CrossOrigin(origins = "https://angular-nowoczesne.herokuapp.com")
public class TilesController {

    private final TilesService tilesService;
    private final QuantityService quantityService;
    private final CreateOffer createOffer;

    public TilesController(TilesService tilesService, QuantityService quantityService, CreateOffer createOffer) {
        this.tilesService = tilesService;
        this.quantityService = quantityService;
        this.createOffer = createOffer;
    }

    @GetMapping("/getAll")
    public List<TilesDTO> getAllTiles() {
        return tilesService.getAllTiles();
    }

    @PostMapping("/map")
    public List<TilesDTO> getTilesWithFilledQuantity(@RequestBody List<TilesInput> tilesInput){
        quantityService.filledQuantityInTiles(tilesInput);
        return tilesService.getAllTiles();
    }

    @GetMapping("/generateOffer")
    public ResponseEntity<Object> generatePdf(@RequestParam(name = "id") long id) throws IOException {
        createOffer.createOffer(id);
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
}
