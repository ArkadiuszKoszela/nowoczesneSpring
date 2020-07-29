package pl.koszela.nowoczesnebud.Controller;

import org.springframework.web.bind.annotation.*;
import pl.koszela.nowoczesnebud.Model.TilesInput;
import pl.koszela.nowoczesnebud.Repository.TilesInputRepository;

import java.util.List;

@RestController
@RequestMapping("/api/input")
@CrossOrigin(origins = "https://angular-nowoczesne.herokuapp.com")
public class InputController {

    private final TilesInputRepository tilesInputRepository;

    public InputController(TilesInputRepository tilesInputRepository) {
        this.tilesInputRepository = tilesInputRepository;
    }

    @GetMapping("/getAll")
    public List<TilesInput> getAll (){
        return tilesInputRepository.findAll();
    }

    @PostMapping("/get")
    public TilesInput postInput (@RequestBody TilesInput tilesInput){
        return tilesInputRepository.save(tilesInput);
    }
}
