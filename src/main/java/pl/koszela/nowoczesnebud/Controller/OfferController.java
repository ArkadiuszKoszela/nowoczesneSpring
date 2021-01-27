package pl.koszela.nowoczesnebud.Controller;

import org.springframework.web.bind.annotation.*;
import pl.koszela.nowoczesnebud.Model.Offer;
import pl.koszela.nowoczesnebud.Service.InputService;
import pl.koszela.nowoczesnebud.Service.OfferService;

import java.util.List;

@RestController
@RequestMapping("/api/offer")
@CrossOrigin(origins = "https://angular-nowoczesne.herokuapp.com")
//@CrossOrigin(origins = "http://localhost:4200")
public class OfferController {

    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    @GetMapping("/getCommercialOffers")
    public List<Offer> getAllCommercialOffers (){
        return offerService.getCommercialOffers ();
    }

    @PostMapping("/save")
    public Offer saveCommercialOffer (@RequestParam boolean forceSave, @RequestBody Offer offer) {
        return offerService.save(offer, forceSave);
    }
}
