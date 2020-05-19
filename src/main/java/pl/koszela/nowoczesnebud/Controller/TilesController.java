package pl.koszela.nowoczesnebud.Controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.koszela.nowoczesnebud.Model.Tiles;
import pl.koszela.nowoczesnebud.Repository.TilesRepository;
import pl.koszela.nowoczesnebud.Service.ServiceCsv;
import pl.koszela.nowoczesnebud.Service.TilesService;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/tiles")
@CrossOrigin(origins = "http://localhost:4200")
public class TilesController {

    private TilesService tilesService;

    public TilesController(TilesService tilesService) {
        this.tilesService = tilesService;
    }

    @GetMapping("/get")
    public List<Tiles> name() {
        return tilesService.getAllTiles();
    }
}
