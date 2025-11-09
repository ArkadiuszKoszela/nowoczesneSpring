package pl.koszela.nowoczesnebud.Model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "offer_items")
public class OfferItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nazwa inputu (np. "Powierzchnia polaci")
    private String name;

    // Mapper do produktów
    private String mapperName;

    // Ilość wprowadzona przez użytkownika
    private Double quantity;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

