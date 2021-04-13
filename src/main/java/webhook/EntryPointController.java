package webhook;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/endpoint")
public class EntryPointController {

    @GetMapping
    public Object API() {
        System.out.println("Hello World");
        return "azazazaz";
    }
}
