package pl.koszela.nowoczesnebud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import pl.koszela.nowoczesnebud.Model.TilesInput;
import pl.koszela.nowoczesnebud.Repository.TilesInputRepository;

@SpringBootApplication
public class NowoczesneBudApplication {

    public static void main(String[] args) {
        SpringApplication.run(NowoczesneBudApplication.class, args);
    }

}
