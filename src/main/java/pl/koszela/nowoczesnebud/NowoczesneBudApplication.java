package pl.koszela.nowoczesnebud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@ComponentScan(basePackages="pl.koszela.nowoczesnebud.*")
@EntityScan("pl.koszela.nowoczesnebud.*")
public class NowoczesneBudApplication {

    public static void main(String[] args) {
        SpringApplication.run(NowoczesneBudApplication.class, args);
    }

}
