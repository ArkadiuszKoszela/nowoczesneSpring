package pl.koszela.nowoczesnebud.Service;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import pl.koszela.nowoczesnebud.Model.Gutter;
import pl.koszela.nowoczesnebud.Repository.GuttersRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class GuttersService {

    private final GuttersRepository guttersRepository;
    private final ServiceCsv serviceCsv;

    public GuttersService(GuttersRepository guttersRepository, ServiceCsv serviceCsv) {
        this.guttersRepository = Objects.requireNonNull(guttersRepository);
        this.serviceCsv = Objects.requireNonNull(serviceCsv);
    }

    public List<Gutter> getAllGutters() {
        if (CollectionUtils.isEmpty(guttersRepository.findAll())) {
            guttersRepository.saveAll(serviceCsv.readAndSaveGutters("src/main/resources/assets/rynny"));
            return guttersRepository.findAll();
        }
        return guttersRepository.findAll();
    }

    public List<Gutter> getDiscounts() {
        return guttersRepository.findDiscounts();
    }

    public List<Gutter> saveDiscounts(Gutter tileToSave) {
        Optional<Gutter> gutters = guttersRepository.findById(tileToSave.getId());
        if (!gutters.isPresent())
            return new ArrayList<>();
        gutters.get().setBasicDiscount(tileToSave.getBasicDiscount());
        gutters.get().setAdditionalDiscount(tileToSave.getAdditionalDiscount());
        gutters.get().setPromotionDiscount(tileToSave.getPromotionDiscount());
        gutters.get().setSkontoDiscount(tileToSave.getSkontoDiscount());
        guttersRepository.save(gutters.get());
        return getDiscounts();
    }
}
