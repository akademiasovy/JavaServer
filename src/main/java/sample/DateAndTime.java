package sample;

import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.xml.ws.Response;
import java.util.ArrayList;
import java.util.List;

@RestController
public class DateAndTime {
    List<User> list =  new ArrayList<User>();

    public DateAndTime() {
        list.add(new User("Roman","Simko","roman","heslo"));
    }

    @RequestMapping("/time")
    public String getTime() {
        return "11:29:15";
    }

    @RequestMapping("/primenumber/{number}")
    public String checkPrimeNumber() {
        return "true / false";
    }

    @RequestMapping("/time/hour")
    public String getHour(){
        return "11";
    }

    @RequestMapping("/hello")
    public String getHello(){
        return "Hello. How are you? ";
    }

    @RequestMapping("/hello/{name}")
    public String getHelloWithName(@PathVariable String name){
        return "Hello "+name+". How are you? ";
    }

    @RequestMapping("/hi")
    public String getHi(@RequestParam(value="fname") String fname, @RequestParam(value="age") String age){
        return "Hello. How are you? Your name is "+fname+" and you are "+age;
    }

    @RequestMapping(method=RequestMethod.POST, value="/login")
    public String login(@RequestBody String credential){
        System.out.println(credential);
        return "{\"Error\":\"Login already exists\"}";
    }

    @RequestMapping(method=RequestMethod.POST, value="/signup")
    public ResponseEntity<String> signup(@RequestBody String data){
        System.out.println(data);
                return ResponseEntity.status(201).body(data);
    }

    @RequestMapping(method=RequestMethod.POST, value="/logout")
    public ResponseEntity<String> logout(@RequestBody String data){
        JSONObject obj = new JSONObject(data);
        JSONObject res = new JSONObject();
        System.out.println(obj.getString("login"));
        System.out.println(data);
        res.put("message","Logouot succesful");
        res.put("login","kral");
        return ResponseEntity.status(200).body(res.toString());
    }

}
