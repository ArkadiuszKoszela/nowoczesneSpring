package pl.koszela.nowoczesnebud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages="pl.koszela.nowoczesnebud.*")
@EntityScan("pl.koszela.nowoczesnebud.*")
@EnableScheduling
public class NowoczesneBudApplication {

    public static void main(String[] args) {
        SpringApplication.run(NowoczesneBudApplication.class, args);
    }

}
