package pl.koszela.nowoczesnebud.Model;

import lombok.Data;

import javax.persistence.Embeddable;

/**
 * UPROSZCZONY MODEL
 * PRZED: @Entity z własnym ID i tabelą addresses
 * PO: @Embeddable - pola są częścią tabeli users
 * KORZYŚĆ: 1 zapytanie SQL zamiast 2 (join), brak niepotrzebnej tabeli
 */
@Data
@Embeddable
public class Address {
    private String address;
    private double latitude;
    private double longitude;
    private double zoom;
}
