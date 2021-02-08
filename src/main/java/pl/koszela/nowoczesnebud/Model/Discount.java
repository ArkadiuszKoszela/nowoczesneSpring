package pl.koszela.nowoczesnebud.Model;

import com.poiji.annotation.ExcelCellName;

import javax.persistence.MappedSuperclass;

@MappedSuperclass
public class Discount {

    @ExcelCellName("basicDiscount")
    private int basicDiscount = 0;
    @ExcelCellName("promotionDiscount")
    private int promotionDiscount;
    @ExcelCellName("additionalDiscount")
    private int additionalDiscount;
    @ExcelCellName("skonto")
    private int skontoDiscount;

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
