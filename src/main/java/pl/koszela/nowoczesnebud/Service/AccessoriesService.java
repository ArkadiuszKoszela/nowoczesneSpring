package pl.koszela.nowoczesnebud.Service;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import pl.koszela.nowoczesnebud.Model.Accessory;
import pl.koszela.nowoczesnebud.Repository.AccessoriesRepository;

import java.util.List;

@Service
public class AccessoriesService {

    private final AccessoriesRepository accessoriesRepository;
    private final CsvImporterImplTile csvImporterImplTile;

    public AccessoriesService(AccessoriesRepository accessoriesRepository, CsvImporterImplTile csvImporterImplTile) {
        this.accessoriesRepository = accessoriesRepository;
        this.csvImporterImplTile = csvImporterImplTile;
    }

    public List<Accessory> getAllAccessories() {
        if (CollectionUtils.isEmpty(accessoriesRepository.findAll())) {
            accessoriesRepository.saveAll(csvImporterImplTile.readAndSaveAccessories("src/main/resources/assets/accessories"));
            return accessoriesRepository.findAll();
        }
        return accessoriesRepository.findAll();
    }
}
