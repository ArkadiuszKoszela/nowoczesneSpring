package pl.koszela.nowoczesnebud.Model;

import lombok.Data;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(targetEntity = User.class, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(targetEntity = Input.class, cascade = CascadeType.MERGE)
    @JoinColumn(name = "offer_id")
    private List<Input> inputList = new ArrayList<>();
}
