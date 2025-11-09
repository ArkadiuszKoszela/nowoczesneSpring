package pl.koszela.nowoczesnebud.Model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * UPROSZCZONY MODEL
 * PRZED: @OneToOne(cascade=ALL) + @JoinColumn → osobna tabela addresses + JOIN
 * PO: @Embedded → pola Address bezpośrednio w tabeli users
 * KORZYŚĆ: Szybsze zapytania SQL (bez JOIN), prostsza struktura DB
 */
@Data
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    private String surname;
    
    @Embedded
    private Address address;
    
    private String telephoneNumber;
    private LocalDate dateOfMeeting;
    private String email;
    
    @CreationTimestamp
    private LocalDateTime createDateTime;

    @UpdateTimestamp
    private LocalDateTime updateDateTime;
}
