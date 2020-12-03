package pl.koszela.nowoczesnebud.Service;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import pl.koszela.nowoczesnebud.Model.Gutters;
import pl.koszela.nowoczesnebud.Repository.GuttersRepository;

import java.util.List;
import java.util.Objects;

@Service
public class GuttersService {

    private final GuttersRepository guttersRepository;
    private final ServiceCsv serviceCsv;

    public GuttersService(GuttersRepository guttersRepository, ServiceCsv serviceCsv) {
        this.guttersRepository = Objects.requireNonNull(guttersRepository);
        this.serviceCsv = Objects.requireNonNull(serviceCsv);
    }

    public List<Gutters> getAllGutters() {
        if (CollectionUtils.isEmpty(guttersRepository.findAll())) {
            guttersRepository.saveAll(serviceCsv.readAndSaveGutters("src/main/resources/assets/rynny"));
            return guttersRepository.findAll();
        }
        return guttersRepository.findAll();
    }
}
