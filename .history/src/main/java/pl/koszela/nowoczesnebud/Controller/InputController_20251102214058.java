package pl.koszela.nowoczesnebud.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.koszela.nowoczesnebud.Model.Input;
import pl.koszela.nowoczesnebud.Repository.InputRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Kontroler dla Input (Wprowadź dane)
 */
@RestController
@RequestMapping("/api/inputs")
public class InputController {

    private static final Logger logger = LoggerFactory.getLogger(InputController.class);
    
    private final InputRepository inputRepository;

    public InputController(InputRepository inputRepository) {
        this.inputRepository = inputRepository;
    }

    /**
     * Pobierz wszystkie Inputy
     * GET /api/inputs
     */
    @GetMapping
    public ResponseEntity<List<Input>> getAllInputs() {
        List<Input> inputs = inputRepository.findAll();
        logger.info("📋 Pobrano {} inputów", inputs.size());
        return ResponseEntity.ok(inputs);
    }

    /**
     * Pobierz tylko nazwy Inputów (dla dropdown w mapperName)
     * GET /api/inputs/names
     */
    @GetMapping("/names")
    public ResponseEntity<List<String>> getInputNames() {
        List<Input> inputs = inputRepository.findAll();
        
        // Wyciągnij unikalne nazwy (pomijając null/empty)
        List<String> names = inputs.stream()
                .map(Input::getName)
                .filter(name -> name != null && !name.trim().isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        
        logger.info("📋 Zwracam {} unikalnych nazw Input", names.size());
        return ResponseEntity.ok(names);
    }

    /**
     * Pobierz Input po ID
     * GET /api/inputs/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Input> getInputById(@PathVariable Long id) {
        return inputRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Utwórz nowy Input
     * POST /api/inputs
     */
    @PostMapping
    public ResponseEntity<Input> createInput(@RequestBody Input input) {
        Input saved = inputRepository.save(input);
        logger.info("✅ Utworzono Input: {}", saved.getName());
        return ResponseEntity.ok(saved);
    }

    /**
     * Usuń Input
     * DELETE /api/inputs/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInput(@PathVariable Long id) {
        if (!inputRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        inputRepository.deleteById(id);
        logger.info("🗑️ Usunięto Input ID: {}", id);
        return ResponseEntity.ok().build();
    }
}






