package sample;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

@RestController
public class UserController {
    List<User> list =  new ArrayList<User>();
    List<String> log =  new ArrayList<String>();

    public UserController() {
        list.add(new User("Roman","Simko","roman","heslo"));
    }

    public boolean isTokenValid(String token) {
        for(User useri : list)
            if(useri.getToken()!=null && useri.getToken().equals(token))
                return true;

        return false;
    }


    @RequestMapping(method=RequestMethod.POST, value="/login")
    public ResponseEntity<String> login(@RequestBody String credential){
        JSONObject obj = new JSONObject(credential);/*don't copy*/
        if(obj.has("login") && obj.has("password")){
            JSONObject res = new JSONObject();
            if(obj.getString("password").isEmpty() || obj.getString("login").isEmpty()){
                res.put("error","Password and login are mandatory fields");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }
            if(existLogin(obj.getString("login")) && checkPassword(obj.getString("login"), obj.getString("password")))
            {  // heslo aj login su OK
                User loggedUser=getUser(obj.getString("login"));
                    if(loggedUser==null){
                    // tento riadok by sa nemal nikdy vykonat, osetrene kvoli jave
                        return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{}");
                    }
                    res.put("fname",loggedUser.getFname());
                    res.put("lname",loggedUser.getLname());
                    res.put("login",loggedUser.getLogin());
                    String token = generateToken();
                    res.put("token",token);
                    loggedUser.setToken(token);
                   writeLog("login",loggedUser.getLogin());
                    return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }
            else{
                res.put("error","Invalid login or password");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }

        }else {
            JSONObject res = new JSONObject();
            res.put("error","Invalid body request");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
    }

    private boolean checkPassword(String login, String password) {
        String pass;
        User user = getUser(login);
            if(user!=null){
                if(BCrypt.checkpw(password,user.getPassword()))
                    return true;
            }
        return false;
    }

    @RequestMapping(method=RequestMethod.POST, value="/signup")
    public ResponseEntity<String> signup(@RequestBody String data){
        //System.out.println(data);
        JSONObject objj = new JSONObject(data);

        if(objj.has("fname") && objj.has("lname")&& objj.has("login")&& objj.has("password"))
        { // vstup je ok, mame vsetky kluce
            if(existLogin(objj.getString("login"))){
                JSONObject res = new JSONObject();
                res.put("error","User already exists");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }
            String password = objj.getString("password");
            if(password.isEmpty()){
                JSONObject res = new JSONObject();
                res.put("error","Password is a mandatory field");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }
            String hashPass = hash(objj.getString("password"));

            User user = new User(objj.getString("fname"), objj.getString("lname"), objj.getString("login"), hashPass);
            list.add(user);
            JSONObject res = new JSONObject();
            res.put("fname",objj.getString("fname"));
            res.put("lname",objj.getString("lname"));
            res.put("login",objj.getString("login"));
            return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
        else{
            JSONObject res = new JSONObject();
            res.put("error","Invalid body request");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
    }

    public String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    private boolean existLogin(String login) {
        for(User user : list){
            if(user.getLogin().equalsIgnoreCase(login))
                    return true;
        }
        return false;
    }

    @RequestMapping(method=RequestMethod.POST, value="/logout")
    public ResponseEntity<String> logout(@RequestBody String data, @RequestHeader(name = "Authorization") String token){
        JSONObject obj = new JSONObject(data);

        String login = obj.getString("login");
        User user = getUser(login);
        if(user!=null && isTokenValid(token)){
            // login aj token su ok, ideme odhlasit
            user.setToken(null);
            writeLog("logout",user.getLogin());
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body("{}");
        }
        JSONObject res = new JSONObject();
        res.put("error","Incorrect login or token");
        return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
    }

    private String generateToken(){
        int size=25;
        Random rnd = new Random();
        String generatedString="";
        for(int i = 0;i<size;i++) {
            int type=rnd.nextInt(4);

            switch (type) {
                case 0:
                    generatedString += (char) ((rnd.nextInt(26)) + 65);
                    break;
                case 1:
                    generatedString += (char) ((rnd.nextInt(10)) + 48);
                    break;
                default:
                    generatedString += (char) ((rnd.nextInt(26)) + 97);
            }
        }
        return generatedString;
    }

    @RequestMapping("/time")
    public ResponseEntity<String> getTime(@RequestParam(value="token") String token) {

        if(token==null){
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{\"error\",\"Bad request\"}");
        }
        if(isTokenValid(token)){
            JSONObject ris = new JSONObject();
            SimpleDateFormat sdfDate = new SimpleDateFormat("HH:mm:ss");
            Date now = new Date();
            String strTime = sdfDate.format(now);
            ris.put("time",strTime);
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(ris.toString());
        }
        else return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"Invalid token\"}");
    }

    private User getUser(String login){
        for(User user:list){
            if(user.getLogin().equals(login))
                return user;
        }
        return null;
    }


    @RequestMapping("/users")
    public ResponseEntity<String> getUsers(@RequestParam(value="token") String token) {

        if(token==null){
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{\"error\",\"Bad request\"}");
        }
        if(isTokenValid(token)){
            JSONArray array = new JSONArray();
            for(User user : list){
                JSONObject obj = new JSONObject();
                obj.put("fname",user.getFname());
                obj.put("lname",user.getLname());
                obj.put("login",user.getLogin());
                array.put(obj);
            }

            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(array.toString());
        }
        else return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"Invalid token\"}");
    }


    @RequestMapping("/users/{login}")
    public ResponseEntity<String> getOneUser(@RequestParam(value="token") String token, @PathVariable String login) {

        if(token==null){
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{\"error\",\"Bad request\"}");
        }

        if(isTokenValid(token)){
                JSONObject obj = new JSONObject();
                User user = getUser(login);
                if(user==null){
                    return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"Invalid user name\"}");
                }

                obj.put("fname",user.getFname());
                obj.put("lname",user.getLname());
                obj.put("login",user.getLogin());

            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(obj.toString());
        }
        else return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"Invalid token\")");
    }

    private void writeLog(String type, String login) {
        JSONObject obj = new JSONObject();
        obj.put("type",type);
        obj.put("login",login);
        obj.put("datetime",getCurrentDateTime());
        log.add(obj.toString());
    }

    @RequestMapping(method=RequestMethod.POST, value="/changepassword")
    public ResponseEntity<String> changePasswd(@RequestBody String data){
        //System.out.println(data);
        JSONObject objj = new JSONObject(data);

        if(objj.has("oldpassword") && objj.has("newpassword")&& objj.has("login"))
        { // vstup je ok, mame vsetky kluce
            String login = objj.getString("login");
            String oldpassword = objj.getString("oldpassword");
            String newpassword = objj.getString("newpassword");
            if(oldpassword.isEmpty() || newpassword.isEmpty()){
                JSONObject res = new JSONObject();
                res.put("error","Passwords are mandatory fields");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }
            if(!existLogin(login) || !checkPassword(login,oldpassword)){
                JSONObject res = new JSONObject();
                res.put("error","Invalid login or password");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }

            String hashPass = hash(objj.getString("newpassword"));

            User user = getUser(login);
            user.setPassword(hashPass);
            writeLog("password change",user.getLogin());
            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body("{}");
        }
        else{
            JSONObject res = new JSONObject();
            res.put("error","Invalid body request");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
    }

    @RequestMapping(method=RequestMethod.GET, path="/log")
    public ResponseEntity<String> getLogInfo(@RequestBody String data, @RequestHeader(name ="Authorization") String token) {

        JSONObject objj = new JSONObject(data);

        if(objj.has("login")){
            String login = objj.getString("login");
            User user = getUser(login);

            if(user==null || !user.getToken().equals(token)){
                JSONObject res = new JSONObject();
                res.put("error","Invalid login or token");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }
            JSONArray res = new JSONArray();
            for(String record:log){
                JSONObject temp = new JSONObject(record);
                if(temp.has("login") && temp.getString("login").equals(login)){
                    res.put(temp);
                }
            }

            return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(res.toString());

        }
        else {
            JSONObject res = new JSONObject();
            res.put("error","Invalid body request, login is missing");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());

        }


    }

    private String getCurrentDateTime(){
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        Date date = new Date();
        return(dateFormat.format(date));
    }

}
