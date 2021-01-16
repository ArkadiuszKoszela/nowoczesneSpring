package pl.koszela.nowoczesnebud.Controller;

import org.springframework.web.bind.annotation.*;
import pl.koszela.nowoczesnebud.Model.UserMobile;
import pl.koszela.nowoczesnebud.Service.UserMobileService;

@RestController
@RequestMapping("/api/mobiles")
@CrossOrigin(origins = "https://angular-nowoczesne.herokuapp.com")
//@CrossOrigin(origins = "http://localhost:4200")
public class MobileController {

    private final UserMobileService userMobileService;

    public MobileController(UserMobileService userMobileService) {
        this.userMobileService = userMobileService;
    }

    @PostMapping("save")
    public UserMobile save (@RequestBody UserMobile userMobile) {
        return userMobileService.save(userMobile);
    }

    @PostMapping("getUser")
    public boolean getUser (@RequestBody UserMobile userMobile) {
        return userMobileService.getUser (userMobile);
    }
}
