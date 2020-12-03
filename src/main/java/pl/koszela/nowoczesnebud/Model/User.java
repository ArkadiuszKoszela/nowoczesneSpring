package pl.koszela.nowoczesnebud.Model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.Date;

@Data
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    private String surname;
    @OneToOne(targetEntity = Address.class, cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id")
    private Address address;
    private String telephoneNumber;
    private LocalDate dateOfMeeting;
    private String email;
    @Temporal(TemporalType.TIMESTAMP)
    @CreatedDate
    private Date created = new Date();
}
