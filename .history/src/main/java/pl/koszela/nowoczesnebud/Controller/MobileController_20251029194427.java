package pl.koszela.nowoczesnebud.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import pl.koszela.nowoczesnebud.Model.UserMobile;
import pl.koszela.nowoczesnebud.Service.UserMobileService;

/**
 * Kontroler obsługujący użytkowników mobilnych
 * CORS zarządzany globalnie przez WebConfig
 */
@RestController
@RequestMapping("/api/mobiles")
public class MobileController {

    private static final Logger logger = LoggerFactory.getLogger(MobileController.class);
    
    private final UserMobileService userMobileService;

    public MobileController(UserMobileService userMobileService) {
        this.userMobileService = userMobileService;
    }

    @PostMapping("save")
    public UserMobile save(@RequestBody UserMobile userMobile) {
        logger.info("Zapisywanie użytkownika mobilnego: {}", userMobile.getUsername());
        return userMobileService.save(userMobile);
    }

    @PostMapping("getUser")
    public boolean getUser(@RequestBody UserMobile userMobile) {
        logger.info("Weryfikacja użytkownika mobilnego: {}", userMobile.getUsername());
        boolean result = userMobileService.getUser(userMobile);
        logger.debug("Wynik weryfikacji dla {}: {}", userMobile.getUsername(), result);
        return result;
    }
}
