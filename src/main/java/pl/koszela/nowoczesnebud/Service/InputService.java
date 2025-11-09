package pl.koszela.nowoczesnebud.Service;

import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.Input;
import pl.koszela.nowoczesnebud.Repository.InputRepository;

import java.util.List;

/**
 * Serwis do zarządzania Input
 * 
 * UWAGA: Nie używamy tutaj saveInputList() które robiło findAll() - to było źródłem problemu
 * z zerowaniem danych poprzednich projektów. Teraz Input jest zarządzany przez ProjectService.
 */
@Service
public class InputService {

    private final InputRepository inputRepository;

    public InputService(InputRepository inputRepository) {
        this.inputRepository = inputRepository;
    }

    /**
     * Zapisuje listę inputów (używane przez ProjectService)
     */
    public List<Input> saveAll(List<Input> inputList) {
        return inputRepository.saveAll(inputList);
    }
    
    /**
     * USUNIĘTO: saveInputList() - było źródłem problemu z zerowaniem danych
     * 
     * Ta metoda robiła findAll() na WSZYSTKICH inputach ze wszystkich projektów
     * i nadpisywała je, co powodowało zerowanie poprzednich projektów.
     * 
     * Teraz Input jest zarządzany przez ProjectService który operuje tylko na inputach
     * powiązanych z konkretnym projektem.
     */
}
