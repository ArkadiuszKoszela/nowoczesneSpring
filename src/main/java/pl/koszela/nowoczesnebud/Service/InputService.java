package pl.koszela.nowoczesnebud.Service;

import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.Input;
import pl.koszela.nowoczesnebud.Repository.InputRepository;

import java.util.List;

@Service
public class InputService {

    private final InputRepository inputRepository;

    public InputService(InputRepository inputRepository) {
        this.inputRepository = inputRepository;
    }

    public List<Input> saveAll (List<Input> inputList) {
        return inputRepository.saveAll(inputList);
    }

    public List<Input> saveInputList (List<Input> inputList) {
        List<Input> inputs = inputRepository.findAll();
        if (inputs.size() == 0)
            return inputRepository.saveAll(inputList);
        for (Input inputToUpdate: inputs) {
            for (Input input: inputList) {
                if (inputToUpdate.getMapperName().equalsIgnoreCase(input.getMapperName()))
                    inputToUpdate.setQuantity(input.getQuantity());
            }
        }
        return inputRepository.saveAll(inputs);
    }
}
