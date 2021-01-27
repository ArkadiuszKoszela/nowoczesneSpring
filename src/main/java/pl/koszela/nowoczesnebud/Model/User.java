package pl.koszela.nowoczesnebud.Model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    @CreationTimestamp
    private LocalDateTime createDateTime;

    @UpdateTimestamp
    private LocalDateTime updateDateTime;
}
