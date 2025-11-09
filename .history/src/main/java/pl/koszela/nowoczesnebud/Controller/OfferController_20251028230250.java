package pl.koszela.nowoczesnebud.Controller;

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

@RestController
@RequestMapping("/api/offer")
//@CrossOrigin(origins = "https://angular-nowoczesne-af04d5c56981.herokuapp.com")
@CrossOrigin(origins = "http://localhost:4200")
public class OfferController {

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
            System.out.println("🔵 Generowanie PDF dla: " + offer.getUser().getName() + " " + offer.getUser().getSurname());
            
            // Generuj PDF
            createOffer.createOffer(offer);
            
            // Odczytaj plik PDF
            Path pdfPath = Paths.get("src/main/resources/templates/CommercialOffer.pdf");
            byte[] pdfBytes = Files.readAllBytes(pdfPath);
            
            // Zwróć PDF jako response
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", "Oferta_" + offer.getUser().getSurname() + ".pdf");
            
            System.out.println("✅ PDF wygenerowany pomyślnie!");
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
                    
        } catch (IOException e) {
            System.err.println("❌ Błąd podczas generowania PDF: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
