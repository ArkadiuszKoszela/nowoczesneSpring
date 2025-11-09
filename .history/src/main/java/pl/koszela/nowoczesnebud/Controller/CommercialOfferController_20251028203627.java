package pl.koszela.nowoczesnebud.Controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pl.koszela.nowoczesnebud.CreateOffer.CreateOffer;
import pl.koszela.nowoczesnebud.Model.CommercialOffer;
import pl.koszela.nowoczesnebud.Service.CommercialOfferService;

import java.util.List;

/**
 * Kontroler do zarządzania ofertami komercyjnymi
 * ZASTĘPUJE: część OfferController
 */
@RestController
@RequestMapping("/api/offers")
@CrossOrigin(origins = "http://localhost:4200")
public class CommercialOfferController {

    private final CommercialOfferService offerService;
    private final CreateOffer createOffer;

    public CommercialOfferController(CommercialOfferService offerService,
                                    CreateOffer createOffer) {
        this.offerService = offerService;
        this.createOffer = createOffer;
    }

    /**
     * Pobierz wszystkie oferty
     * GET /api/offers
     */
    @GetMapping
    public ResponseEntity<List<CommercialOffer>> getAllOffers() {
        return ResponseEntity.ok(offerService.getAllOffers());
    }

    /**
     * Pobierz ofertę po ID
     * GET /api/offers/123
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommercialOffer> getOffer(@PathVariable Long id) {
        return offerService.getOfferById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Zapisz nową ofertę
     * POST /api/offers
     */
    @PostMapping
    public ResponseEntity<CommercialOffer> saveOffer(
            @RequestBody CommercialOffer offer,
            @RequestParam(defaultValue = "false") boolean forceSave) {
        
        CommercialOffer savedOffer = offerService.saveOffer(offer, forceSave);
        
        if (savedOffer == null) {
            // Klient już istnieje i forceSave = false
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        return ResponseEntity.ok(savedOffer);
    }

    /**
     * Generuj PDF dla oferty
     * POST /api/offers/generate-pdf
     * ZASTĘPUJE: /api/tiles/generateOffer
     */
    @PostMapping("/generate-pdf")
    public ResponseEntity<Object> generatePdf(@RequestBody CommercialOffer offer) {
        // Używamy istniejącej logiki generowania PDF
        createOffer.createOfferFromCommercialOffer(offer);
        
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

    /**
     * Usuń ofertę
     * DELETE /api/offers/123
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOffer(@PathVariable Long id) {
        offerService.deleteOffer(id);
        return ResponseEntity.noContent().build();
    }
}

