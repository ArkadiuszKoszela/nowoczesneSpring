package pl.koszela.nowoczesnebud.Service;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import pl.koszela.nowoczesnebud.Model.Tiles;
import pl.koszela.nowoczesnebud.Model.TilesDTO;
import pl.koszela.nowoczesnebud.Repository.TilesRepository;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class TilesService {

    private final TilesRepository tilesRepository;
    private final ServiceCsv serviceCsv;

    public TilesService(TilesRepository tilesRepository, ServiceCsv serviceCsv) {
        this.tilesRepository = Objects.requireNonNull(tilesRepository);
        this.serviceCsv = Objects.requireNonNull(serviceCsv);
    }

    public List<TilesDTO> getAllTiles() {
        if (CollectionUtils.isEmpty(tilesRepository.findAll())) {
            System.out.println("START");
            long timeStart = System.currentTimeMillis();
            tilesRepository.saveAll(serviceCsv.saveTiles());
            long timeStop = System.currentTimeMillis();
            long duration = timeStop - timeStart;
            System.out.println(TimeUnit.MILLISECONDS.toSeconds(duration) + " sec");
            return convertToDTO(tilesRepository.findAll());
        }
        return convertToDTO(tilesRepository.findAll());
    }

    private List<TilesDTO> convertToDTO (List<Tiles> getAll){
        ModelMapper modelMapper = new ModelMapper();
        return getAll.stream().map(tiles -> modelMapper.map(tiles, TilesDTO.class)).collect(Collectors.toList());
    }
}
