package pl.koszela.nowoczesnebud.Service;

import com.poiji.bind.Poiji;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.Accessories;
import pl.koszela.nowoczesnebud.Model.Tiles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ServiceCsv {

    private final List<Tiles> tilesList;
    private final List<Accessories> accessoriesList;

    public ServiceCsv() {
        tilesList = new ArrayList<>();
        accessoriesList = new ArrayList<>();
    }

    List<Tiles> saveTiles() {
        readFromCSVTiles("src/main/resources/assets/cenniki");
        return tilesList;
    }

    List<Accessories> saveAccessories() {
        readFromCSVAccessories("src/main/resources/assets/accesories");
        return accessoriesList;
    }

    private void readFromCSVTiles(String directory) {
        File[] files = new File(directory).listFiles(File::isFile);
        Arrays.stream(files).forEach(System.out::println);
        for (File file : files) {
            List<Tiles> tilesPathList = Poiji.fromExcel(file, Tiles.class);
            tilesPathList.forEach(obj -> obj.setManufacturer(getManufacturer(file.getName())));
            tilesList.addAll(tilesPathList);
        }
    }

    private void readFromCSVAccessories(String directory) {
        File[] files = new File(directory).listFiles(File::isFile);
        Arrays.stream(files).forEach(System.out::println);
        for (File file : files) {
            List<Accessories> accessoriesPathList = Poiji.fromExcel(file, Accessories.class);
            accessoriesPathList.forEach(obj -> obj.setManufacturer(getManufacturer(file.getName())));
            accessoriesList.addAll(accessoriesPathList);
        }
    }

    private String getManufacturer(String url) {
        return StringUtils.substringBeforeLast(url, ".");
    }
}
