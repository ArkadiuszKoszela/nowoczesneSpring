package pl.koszela.nowoczesnebud.Model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class TilesInput {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private double pPolac;
    private double dKalenic;
    private double dKalenicSko;
    private double dKalenicPro;
    private double dKoszy;
    private double dKrawLew;
    private double dKrawPraw;
    private double oKomina;
    private double dOkapu;
    private double dachWent;
    private double kompKomWentyl;
    private double gPocz;
    private double gKon;
    private double gZaokr;
    private double trojnik;
    private double czwornik;
    private double gPodwjMuf;
    private double dDwuf;
    private double oPolac;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public double getpPolac() {
        return pPolac;
    }

    public void setpPolac(double pPolac) {
        this.pPolac = pPolac;
    }

    public double getdKalenic() {
        return dKalenic;
    }

    public void setdKalenic(double dKalenic) {
        this.dKalenic = dKalenic;
    }

    public double getdKalenicSko() {
        return dKalenicSko;
    }

    public void setdKalenicSko(double dKalenicSko) {
        this.dKalenicSko = dKalenicSko;
    }

    public double getdKalenicPro() {
        return dKalenicPro;
    }

    public void setdKalenicPro(double dKalenicPro) {
        this.dKalenicPro = dKalenicPro;
    }

    public double getdKoszy() {
        return dKoszy;
    }

    public void setdKoszy(double dKoszy) {
        this.dKoszy = dKoszy;
    }

    public double getdKrawLew() {
        return dKrawLew;
    }

    public void setdKrawLew(double dKrawLew) {
        this.dKrawLew = dKrawLew;
    }

    public double getdKrawPraw() {
        return dKrawPraw;
    }

    public void setdKrawPraw(double dKrawPraw) {
        this.dKrawPraw = dKrawPraw;
    }

    public double getoKomina() {
        return oKomina;
    }

    public void setoKomina(double oKomina) {
        this.oKomina = oKomina;
    }

    public double getdOkapu() {
        return dOkapu;
    }

    public void setdOkapu(double dOkapu) {
        this.dOkapu = dOkapu;
    }

    public double getDachWent() {
        return dachWent;
    }

    public void setDachWent(double dachWent) {
        this.dachWent = dachWent;
    }

    public double getKompKomWentyl() {
        return kompKomWentyl;
    }

    public void setKompKomWentyl(double kompKomWentyl) {
        this.kompKomWentyl = kompKomWentyl;
    }

    public double getgPocz() {
        return gPocz;
    }

    public void setgPocz(double gPocz) {
        this.gPocz = gPocz;
    }

    public double getgKon() {
        return gKon;
    }

    public void setgKon(double gKon) {
        this.gKon = gKon;
    }

    public double getgZaokr() {
        return gZaokr;
    }

    public void setgZaokr(double gZaokr) {
        this.gZaokr = gZaokr;
    }

    public double getTrojnik() {
        return trojnik;
    }

    public void setTrojnik(double trojnik) {
        this.trojnik = trojnik;
    }

    public double getCzwornik() {
        return czwornik;
    }

    public void setCzwornik(double czwornik) {
        this.czwornik = czwornik;
    }

    public double getgPodwjMuf() {
        return gPodwjMuf;
    }

    public void setgPodwjMuf(double gPodwjMuf) {
        this.gPodwjMuf = gPodwjMuf;
    }

    public double getdDwuf() {
        return dDwuf;
    }

    public void setdDwuf(double dDwuf) {
        this.dDwuf = dDwuf;
    }

    public double getoPolac() {
        return oPolac;
    }

    public void setoPolac(double oPolac) {
        this.oPolac = oPolac;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private double pPolac;
        private double dKalenic;
        private double dKalenicSko;
        private double dKalenicPro;
        private double dKoszy;
        private double dKrawLew;
        private double dKrawPraw;
        private double oKomina;
        private double dOkapu;
        private double dachWent;
        private double kompKomWentyl;
        private double gPocz;
        private double gKon;
        private double gZaokr;
        private double trojnik;
        private double czwornik;
        private double gPodwjMuf;
        private double dDwuf;
        private double oPolac;


        public Builder pPolac(double pPolac) {
            this.pPolac = pPolac;
            return this;
        }

        public Builder dKalenic(double dKalenic) {
            this.dKalenic = dKalenic;
            return this;
        }

        public Builder dKalenicSko(double dKalenicSko) {
            this.dKalenicSko = dKalenicSko;
            return this;
        }

        public Builder dKalenicPro(double dKalenicPro) {
            this.dKalenicPro = dKalenicPro;
            return this;
        }

        public Builder dKoszy(double dKoszy) {
            this.dKoszy = dKoszy;
            return this;
        }

        public Builder dKrawLew(double dKrawLew) {
            this.dKrawLew = dKrawLew;
            return this;
        }

        public Builder dKrawPraw(double dKrawPraw) {
            this.dKrawPraw = dKrawPraw;
            return this;
        }

        public Builder oKomina(double oKomina) {
            this.oKomina = oKomina;
            return this;
        }

        public Builder dOkapu(double dOkapu) {
            this.dOkapu = dOkapu;
            return this;
        }

        public Builder dachWent(double dachWent) {
            this.dachWent = dachWent;
            return this;
        }

        public Builder kompKomWentyl(double kompKomWentyl) {
            this.kompKomWentyl = kompKomWentyl;
            return this;
        }

        public Builder gPocz(double gPocz) {
            this.gPocz = gPocz;
            return this;
        }

        public Builder gKon(double gKon) {
            this.gKon = gKon;
            return this;
        }

        public Builder gZaokr(double gZaokr) {
            this.gZaokr = gZaokr;
            return this;
        }

        public Builder trojnik(double trojnik) {
            this.trojnik = trojnik;
            return this;
        }

        public Builder czwornik(double czwornik) {
            this.czwornik = czwornik;
            return this;
        }

        public Builder gPodwjMuf(double gPodwjMuf) {
            this.gPodwjMuf = gPodwjMuf;
            return this;
        }

        public Builder dDwuf(double dDwuf) {
            this.dDwuf = dDwuf;
            return this;
        }

        public Builder oPolac(double oPolac) {
            this.oPolac = oPolac;
            return this;
        }

        public TilesInput build() {
            TilesInput tilesInput = new TilesInput();
            tilesInput.pPolac = this.pPolac;
            tilesInput.dKalenic = this.dKalenic;
            tilesInput.dKalenicSko = this.dKalenicSko;
            tilesInput.dKalenicPro = this.dKalenicPro;
            tilesInput.dKoszy = this.dKoszy;
            tilesInput.dKrawLew = this.dKrawLew;
            tilesInput.dKrawPraw = this.dKrawPraw;
            tilesInput.oKomina = this.oKomina;
            tilesInput.dOkapu = this.dOkapu;
            tilesInput.dachWent = this.dachWent;
            tilesInput.kompKomWentyl = this.kompKomWentyl;
            tilesInput.gPocz = this.gPocz;
            tilesInput.gKon = this.gKon;
            tilesInput.gZaokr = this.gZaokr;
            tilesInput.trojnik = this.trojnik;
            tilesInput.czwornik = this.czwornik;
            tilesInput.gPodwjMuf = this.gPodwjMuf;
            tilesInput.dDwuf = this.dDwuf;
            tilesInput.oPolac = this.oPolac;
            return tilesInput;
        }
    }
}
