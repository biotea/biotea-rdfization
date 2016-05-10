package ws.biotea.hello;

import java.io.Writer;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @RequestMapping("/")
    public String index() {
        return "Greetings from Biotea annotation web services, powered by Spring Boot!";
    }
    
    @RequestMapping(value= "/pmcAnnotation", method = RequestMethod.GET)
    public @ResponseBody void getModel(HttpServletRequest request, Writer responseWriter/*, @RequestParam String pmcid*/) {
    	
    }

}