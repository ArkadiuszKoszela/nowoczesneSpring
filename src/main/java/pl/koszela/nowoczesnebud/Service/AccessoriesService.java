package pl.koszela.nowoczesnebud.Service;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import pl.koszela.nowoczesnebud.Model.Accessories;
import pl.koszela.nowoczesnebud.Repository.AccessoriesRepository;

import java.util.List;

@Service
public class AccessoriesService {

    private final AccessoriesRepository accessoriesRepository;
    private final ServiceCsv serviceCsv;

    public AccessoriesService(AccessoriesRepository accessoriesRepository, ServiceCsv serviceCsv) {
        this.accessoriesRepository = accessoriesRepository;
        this.serviceCsv = serviceCsv;
    }

    public List<Accessories> getAllAccessories() {
        if (CollectionUtils.isEmpty(accessoriesRepository.findAll())) {
            accessoriesRepository.saveAll(serviceCsv.saveAccessories());
            return accessoriesRepository.findAll();
        }
        return accessoriesRepository.findAll();
    }
}
