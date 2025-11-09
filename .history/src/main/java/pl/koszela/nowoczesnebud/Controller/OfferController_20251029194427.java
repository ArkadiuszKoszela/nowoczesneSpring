package pl.koszela.nowoczesnebud.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.koszela.nowoczesnebud.CreateOffer.CreateOffer;
import pl.koszela.nowoczesnebud.Model.Offer;
import pl.koszela.nowoczesnebud.Service.OfferService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Kontroler obsługujący oferty handlowe
 * CORS zarządzany globalnie przez WebConfig
 */
@RestController
@RequestMapping("/api/offer")
public class OfferController {

    private static final Logger logger = LoggerFactory.getLogger(OfferController.class);
    
    private final OfferService offerService;
    private final CreateOffer createOffer;

    public OfferController(OfferService offerService, CreateOffer createOffer) {
        this.offerService = offerService;
        this.createOffer = createOffer;
    }

    @GetMapping("/getCommercialOffers")
    public List<Offer> getAllCommercialOffers (){
        return offerService.getCommercialOffers ();
    }

    @PostMapping("/save")
    public Offer saveCommercialOffer (@RequestParam boolean forceSave, @RequestBody Offer offer) {
        return offerService.save(offer, forceSave);
    }

    /**
     * Generuje PDF oferty na podstawie nowego modelu Product
     */
    @PostMapping("/generate-pdf")
    public ResponseEntity<byte[]> generatePdf(@RequestBody Offer offer) {
        try {
            logger.info("Generowanie PDF dla: {} {}", offer.getUser().getName(), offer.getUser().getSurname());
            
            // Generuj PDF
            createOffer.createOffer(offer);
            
            // Odczytaj plik PDF
            Path pdfPath = Paths.get("src/main/resources/templates/CommercialOffer.pdf");
            byte[] pdfBytes = Files.readAllBytes(pdfPath);
            
            // Zwróć PDF jako response
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", "Oferta_" + offer.getUser().getSurname() + ".pdf");
            
            logger.info("PDF wygenerowany pomyślnie dla {}", offer.getUser().getSurname());
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
                    
        } catch (IOException e) {
            logger.error("Błąd podczas generowania PDF: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
