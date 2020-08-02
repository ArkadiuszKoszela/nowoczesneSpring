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
//        try (Stream<Path> walk = Files.walk(Paths.get(directory))) {
        Arrays.stream(files).forEach(System.out::println);



            for (File filePath : files) {
                List<Tiles> tilesPathList = Poiji.fromExcel(filePath, Tiles.class);
                tilesPathList.forEach(tile -> tile.setManufacturer(getManufacturer(filePath.getName())));
                tilesList.addAll(tilesPathList);
            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private void readFromCSVAccessories(String directory) {
        try (Stream<Path> walk = Files.walk(Paths.get(directory))) {

            List<String> result = walk.filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());

            for (String filePath : result) {
                List<Accessories> accessoriesPathList = Poiji.fromExcel(new File(filePath), Accessories.class);
                accessoriesPathList.forEach(tile -> tile.setManufacturer(getManufacturer(filePath)));
                accessoriesList.addAll(accessoriesPathList);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getManufacturer(String url) {
        String trim = StringUtils.substringAfterLast(url, "\\");
        return StringUtils.substringBeforeLast(trim, ".");
    }
}
