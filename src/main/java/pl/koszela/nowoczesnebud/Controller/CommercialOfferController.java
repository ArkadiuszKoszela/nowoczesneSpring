package pl.koszela.nowoczesnebud.Controller;

import org.springframework.web.bind.annotation.*;
import pl.koszela.nowoczesnebud.Model.CommercialOffer;
import pl.koszela.nowoczesnebud.Service.CommercialOfferService;

import java.util.List;

@RestController
@RequestMapping("/api/commercial")
//@CrossOrigin(origins = "https://angular-nowoczesne.herokuapp.com")
@CrossOrigin(origins = "http://localhost:4200")
public class CommercialOfferController {

    private final CommercialOfferService commercialOfferService;

    public CommercialOfferController(CommercialOfferService commercialOfferService) {
        this.commercialOfferService = commercialOfferService;
    }

    @GetMapping("/getCommercialOffers")
    public List<CommercialOffer> getAllCommercialOffers (){
        return commercialOfferService.getCommercialOffers ();
    }

    @PostMapping("/save")
    public CommercialOffer saveCommercialOffer (@RequestBody CommercialOffer commercialOffer) {
        return commercialOfferService.save(commercialOffer);
    }
}
