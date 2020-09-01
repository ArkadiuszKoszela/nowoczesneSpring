package pl.koszela.nowoczesnebud.Service;

import com.poiji.bind.Poiji;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import pl.koszela.nowoczesnebud.Model.Accessories;
import pl.koszela.nowoczesnebud.Model.Tiles;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ServiceCsv {

    List<Tiles> readAndSaveTiles(String directory) {
        File[] files = new File(directory).listFiles(File::isFile);
        List<Tiles> tilesPathList = new ArrayList<>();
        Arrays.stream(files).forEach(file -> {
            List<Tiles> manufacturerList = Poiji.fromExcel(file, Tiles.class);
            manufacturerList.forEach(obj -> obj.setManufacturer(getManufacturer(file.getName())));
            tilesPathList.addAll(manufacturerList);
        });
        return tilesPathList;
    }

    List<Accessories> readAndSaveAccessories(String directory) {
        File[] files = new File(directory).listFiles(File::isFile);
        List<Accessories> accessoriesPathList = new ArrayList<>();
        for (File file : files) {
            accessoriesPathList = Poiji.fromExcel(file, Accessories.class);
            accessoriesPathList.forEach(obj -> obj.setManufacturer(getManufacturer(file.getName())));
        }
        return accessoriesPathList;
    }

    private String getManufacturer(String url) {
        return StringUtils.substringBeforeLast(url, ".");
    }
}
