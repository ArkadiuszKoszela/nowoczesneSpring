package pl.koszela.nowoczesnebud.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * UPROSZCZONY MODEL
 * PRZED: @OneToOne(cascade=ALL) + @JoinColumn → osobna tabela addresses + JOIN
 * PO: @Embedded → pola Address bezpośrednio w tabeli users
 * KORZYŚĆ: Szybsze zapytania SQL (bez JOIN), prostsza struktura DB
 */
@Data
@Entity
@Table(name = "\"user\"") // H2 wymaga cudzysłowów dla słowa kluczowego "user"
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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
    
    /**
     * RELACJA: Jeden klient → Jeden projekt (OneToOne)
     * mappedBy wskazuje, że Project jest właścicielem relacji
     * ⚠️ WAŻNE: @JsonIgnore zapobiega cyklicznej referencji podczas deserializacji JSON
     * (gdy frontend wysyła Project z client, nie powinien zawierać project w client)
     */
    @OneToOne(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private Project project;
}
