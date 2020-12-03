package pl.koszela.nowoczesnebud.Model;

import java.util.HashMap;
import java.util.Map;

public class Mapper {

    public Map<String, String> getMap() {
        Map<String, String> map = new HashMap<>();
        map.put("Powierzchnia połaci", "Dachówka podstawowa");
        map.put("Długość kalenic", "Gąsior podstawowy");
        map.put("Długość kalenic skośnych", "");
        map.put("Długość kalenic prostych", "");
        map.put("Długość koszy", "Kosz");
        map.put("Długość krawędzi lewych", "Dachówka krawędziowa lewa,Dachówka krawędziowa dwufalowa lewa,Dachówka krawędziowa dwufalowa prawa");
        map.put("Długość krawędzi prawych", "Dachówka krawędziowa prawa");
        map.put("Obwód komina", "Obwód komina");
        map.put("Długość okapu", "Siatka okapu,Grzebień okapu,Pas okapowy");
        map.put("Dachówka wentylacyjna", "Dachówka wentylacyjna");
        map.put("Komplet kominka wentylacyjnego", "Komplet kominka wentylacyjnego,Kominewk wentylacyjny,Kominewk wentylacyjny 100 komplet,Kominewk wentylacyjny 125 komplet,Kominewk wentylacyjny 150 komplet");
        map.put("Gąsior początkowy", "Płytka początkowa,Zaślepka końcowa,Gąsior początkowy PP,Gąsior początkowy PD,Gąsior początkowy zaokrąglony,Gąsior podstawowy PP,Klamra gąsiora podstawowego PP,Gąsior początkowy PP");
        map.put("Gąsior końcowy", "Płytka końcowa,Zaślepka początkowa");
        map.put("Gąsior zaokrąglony", "Gąsior zaokrąglony");
        map.put("Trójnik", "Trójnik,Trójnik PD");
        map.put("Czwórnik", "Czwórnik");
        map.put("Gąsior z podwójną mufą", "Gąsior z podwójną mufą");
        map.put("Dachówka dwufalowa", "Dachówka dwufalowa");
        map.put("Okno połaciowe", "Okno połaciowe");
        return map;
    }
}
