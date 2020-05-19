package pl.koszela.nowoczesnebud.Service;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import pl.koszela.nowoczesnebud.Model.Tiles;
import pl.koszela.nowoczesnebud.Repository.TilesRepository;

import java.util.List;
import java.util.Objects;

@Service
public class TilesService {

    private TilesRepository tilesRepository;
    private ServiceCsv serviceCsv;

    public TilesService(TilesRepository tilesRepository, ServiceCsv serviceCsv) {
        this.tilesRepository = Objects.requireNonNull(tilesRepository);
        this.serviceCsv = Objects.requireNonNull(serviceCsv);
    }

    public List<Tiles> getAllTiles() {
        if (CollectionUtils.isEmpty(tilesRepository.findAll())) {
            tilesRepository.saveAll(serviceCsv.saveTiles());
            return tilesRepository.findAll();
        }
        return tilesRepository.findAll();
    }

    void saveListTiles(List<Tiles> tilesList) {
        tilesRepository.saveAll(tilesList);
    }

    void deleteAllTiles() {
        tilesRepository.deleteAll();
    }
}
