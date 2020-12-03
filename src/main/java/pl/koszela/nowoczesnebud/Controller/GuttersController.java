package pl.koszela.nowoczesnebud.Controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.koszela.nowoczesnebud.Model.Accessories;
import pl.koszela.nowoczesnebud.Model.Gutters;
import pl.koszela.nowoczesnebud.Service.AccessoriesService;
import pl.koszela.nowoczesnebud.Service.GuttersService;

import java.util.List;

@RestController
@RequestMapping("/api/gutters")
@CrossOrigin(origins = "https://angular-nowoczesne.herokuapp.com")
//@CrossOrigin(origins = "http://localhost:4200")
public class GuttersController {

    private final GuttersService guttersService;

    public GuttersController(GuttersService guttersService) {
        this.guttersService = guttersService;
    }

    @GetMapping("/getAll")
    public List<Gutters> getAllGutters() {
        return guttersService.getAllGutters();
    }
}
