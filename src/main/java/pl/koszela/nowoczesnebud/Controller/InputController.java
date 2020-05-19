package pl.koszela.nowoczesnebud.Controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.koszela.nowoczesnebud.Model.TilesInput;

@RestController
@RequestMapping("/input")
@CrossOrigin(origins = "http://localhost:4200")
public class InputController {

    @GetMapping("/test")
    public TilesInput test() {
        TilesInput tilesInput = new TilesInput.Builder()
                .pPolac(14.5)
                .dKalenic(55.4)
                .dKalenicSko(12.0)
                .dKalenicPro(22.9)
                .dKoszy(11.1)
                .dKrawLew(15.6)
                .dKrawPraw(6.5)
                .oKomina(1.1)
                .dOkapu(5.5)
                .dachWent(2.2)
                .kompKomWentyl(5.0)
                .gPocz(2.0)
                .gKon(254.3)
                .gZaokr(55.4)
                .trojnik(44.3)
                .czwornik(6.6)
                .gPodwjMuf(2.2)
                .dDwuf(7.9)
                .oPolac(2.2)
                .build();
        return tilesInput;
    }
}
