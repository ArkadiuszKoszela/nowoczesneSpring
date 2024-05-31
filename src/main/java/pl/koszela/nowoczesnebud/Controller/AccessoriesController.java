package pl.koszela.nowoczesnebud.Controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.koszela.nowoczesnebud.Model.Accessory;
import pl.koszela.nowoczesnebud.Service.AccessoriesService;

import java.util.List;

@RestController
@RequestMapping("/api/accessories")
@CrossOrigin(origins = "https://angular-nowoczesne-af04d5c56981.herokuapp.com")
//@CrossOrigin(origins = "http://localhost:4200")
public class AccessoriesController {

    private final AccessoriesService accessoriesService;

    public AccessoriesController(AccessoriesService accessoriesService) {
        this.accessoriesService = accessoriesService;
    }

    @GetMapping("/getAll")
    public List<Accessory> name() {
        return accessoriesService.getAllAccessories();
    }
}
