package pl.koszela.nowoczesnebud.Controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pl.koszela.nowoczesnebud.CreateOffer.CreateOffer;
import pl.koszela.nowoczesnebud.Model.CommercialOffer;
import pl.koszela.nowoczesnebud.Model.Tiles;
import pl.koszela.nowoczesnebud.Model.TilesDTO;
import pl.koszela.nowoczesnebud.Model.TilesInput;
import pl.koszela.nowoczesnebud.Service.QuantityService;
import pl.koszela.nowoczesnebud.Service.TilesService;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/tiles")
@CrossOrigin(origins = "https://angular-nowoczesne.herokuapp.com")
//@CrossOrigin(origins = "http://localhost:4200")
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
        return tilesService.getAllTilesOrCreate();
    }

    @PostMapping("/map")
    public List<TilesDTO> getTilesWithFilledQuantity(@RequestBody List<TilesInput> tilesInput){
        quantityService.filledQuantityInTiles(tilesInput);
        return tilesService.getAllTilesOrCreate();
    }

    @PostMapping("/generateOffer")
    public ResponseEntity<Object> generatePdf(@RequestBody CommercialOffer commercialOffer) throws IOException {
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
    public List<Tiles> clearQuantityInTiles (){
        return tilesService.clearQuantity();
    }
}
