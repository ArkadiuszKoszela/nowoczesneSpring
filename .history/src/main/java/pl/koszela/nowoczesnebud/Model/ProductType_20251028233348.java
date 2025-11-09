package pl.koszela.nowoczesnebud.Model;

import com.poiji.annotation.ExcelCell;
import com.poiji.annotation.ExcelCellName;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * UPROSZCZONY MODEL
 * PRZED: extends Discount (anty-pattern @MappedSuperclass)
 * PO: Pola rabatów bezpośrednio w klasie
 * KORZYŚĆ: Prostszy model, lepsza czytelność, łatwiejsze utrzymanie
 */
@Entity
public class ProductType {

    @Id
    @GeneratedValue(generator = "generator")
    @GenericGenerator(name = "generator", strategy = "increment")
    private long id;
    
    @ExcelCellName("name")
    private String name;
    
    @ExcelCell(1)
    private double detalPrice = 0.00d;
    
    @ExcelCell(2)
    private double quantityConverter;
    
    @ExcelCellName("mapperName")
    private String mapperName = "";
    
    @ExcelCellName("procent katalog")
    private double marginUnitDetalPrice = 0.00d;
    
    private String unit;
    
    @ExcelCellName("cena zakupu")
    private double purchasePrice = 0.00d;
    
    private double sellingPrice = 0.00d;
    private double quantity = 0.00d;
    
    // POLA PRZENIESIONE Z Discount (usunięcie dziedziczenia)
    @ExcelCellName("basicDiscount")
    private int basicDiscount = 0;
    
    @ExcelCellName("promotionDiscount")
    private int promotionDiscount = 0;
    
    @ExcelCellName("additionalDiscount")
    private int additionalDiscount = 0;
    
    @ExcelCellName("skonto")
    private int skontoDiscount = 0;


    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDetalPrice(double detalPrice) {
        this.detalPrice = detalPrice;
    }

    public void setQuantityConverter(double quantityConverter) {
        this.quantityConverter = quantityConverter;
    }

    public void setMapperName(String mapperName) {
        this.mapperName = mapperName;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getDetalPrice() {
        return detalPrice;
    }

    public double getQuantityConverter() {
        return quantityConverter;
    }

    public String getMapperName() {
        return mapperName;
    }

    public String getUnit() {
        return unit;
    }

    public double getQuantity() {
        return quantity;
    }

    public double getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public double getMarginUnitDetalPrice() {
        return marginUnitDetalPrice;
    }

    public void setMarginUnitDetalPrice(double marginUnitDetalPrice) {
        this.marginUnitDetalPrice = marginUnitDetalPrice;
    }

    // Gettery i settery dla pól rabatowych (przeniesione z Discount)
    public int getBasicDiscount() {
        return basicDiscount;
    }

    public void setBasicDiscount(int basicDiscount) {
        this.basicDiscount = basicDiscount;
    }

    public int getPromotionDiscount() {
        return promotionDiscount;
    }

    public void setPromotionDiscount(int promotionDiscount) {
        this.promotionDiscount = promotionDiscount;
    }

    public int getAdditionalDiscount() {
        return additionalDiscount;
    }

    public void setAdditionalDiscount(int additionalDiscount) {
        this.additionalDiscount = additionalDiscount;
    }

    public int getSkontoDiscount() {
        return skontoDiscount;
    }

    public void setSkontoDiscount(int skontoDiscount) {
        this.skontoDiscount = skontoDiscount;
    }
}
