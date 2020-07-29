package pl.koszela.nowoczesnebud.Model;

import java.util.HashMap;
import java.util.Map;

public class Mapper {

    public Map<String, String> getMap() {
        Map<String, String> map = new HashMap<>();
        map.put("pPolac", "Dachówka podstawowa");
        map.put("dKalenic", "Gąsior podstawowy");
        map.put("dKalenicSko", "");
        map.put("dKalenicPro", "");
        map.put("dKoszy", "");
        map.put("dKrawLew", "Dachówka krawędziowa lewa");
        map.put("dKrawPraw", "Dachówka krawędziowa prawa");
        map.put("oKomina", "");
        map.put("dOkapu", "");
        map.put("dachWent", "Dachówka wentylacyjna");
        map.put("kompKomWentyl", "Komplet kominka wentylacyjnego, Kominewk wentylacyjny");
        map.put("gPocz", "Płytka początkowa, Zaślepka końcowa");
        map.put("gKon", "Płytka końcowa, Zaślepka początkowa");
        map.put("gZaokr", "Gąsior zaokrąglony");
        map.put("trojnik", "Trójnik");
        map.put("czwornik", "");
        map.put("gPodwjMuf", "");
        map.put("dDwuf", "Dachówka dwufalowa");
        map.put("oPolac", "");
        return map;
    }
}
