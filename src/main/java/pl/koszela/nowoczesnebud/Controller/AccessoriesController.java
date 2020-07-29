package pl.koszela.nowoczesnebud.Controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.koszela.nowoczesnebud.Model.Accessories;
import pl.koszela.nowoczesnebud.Service.AccessoriesService;

import java.util.List;

@RestController
@RequestMapping("/accessories")
@CrossOrigin(origins = "http://localhost:4200")
public class AccessoriesController {

    private AccessoriesService accessoriesService;

    public AccessoriesController(AccessoriesService accessoriesService) {
        this.accessoriesService = accessoriesService;
    }

    @GetMapping("/getAll")
    public List<Accessories> name() {
        return accessoriesService.getAllAccessories();
    }
}
