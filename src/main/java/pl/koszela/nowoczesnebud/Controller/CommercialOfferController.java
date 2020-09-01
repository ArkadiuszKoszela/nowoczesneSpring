package pl.koszela.nowoczesnebud.Controller;

import org.springframework.web.bind.annotation.*;
import pl.koszela.nowoczesnebud.Model.CommercialOffer;
import pl.koszela.nowoczesnebud.Model.User;
import pl.koszela.nowoczesnebud.Service.CommercialOfferService;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "https://angular-nowoczesne.herokuapp.com")
//@CrossOrigin(origins = "http://localhost:4200")
public class CommercialOfferController {

    private CommercialOfferService commercialOfferService;

    public CommercialOfferController(CommercialOfferService commercialOfferService) {
        this.commercialOfferService = commercialOfferService;
    }

    @GetMapping("/getAll")
    public List<User> getAll (){
        return commercialOfferService.getAll();
    }

    @PostMapping("/save")
    public CommercialOffer getTilesWithMap(@RequestBody CommercialOffer commercialOffer) {
        return commercialOfferService.saveUser(commercialOffer.getTileToOffer(), commercialOffer.getUser());
    }

}
