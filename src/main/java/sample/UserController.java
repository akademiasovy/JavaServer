package sample;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
public class UserController {
    HashMap<String, String> listOfTokens =  new HashMap<>();

    public UserController() {

    }

    public boolean isTokenValid(String token) {
        if(listOfTokens.containsValue(token))
                return true;

        return false;
    }


    @RequestMapping(method=RequestMethod.POST, value="/login")
    public ResponseEntity<String> login(@RequestBody String credential){
        JSONObject obj = new JSONObject(credential);/*don't copy*/
        if(obj.has("login") && obj.has("password")){
           Database db=new Database();
            JSONObject res = new JSONObject();
            if(obj.getString("password").isEmpty() || obj.getString("login").isEmpty()){
                res.put("error","Password and login are mandatory fields");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }
            if(db.existUser(obj.getString("login")) && checkPassword(obj.getString("login"), obj.getString("password")))
            {  // heslo aj login su OK

                User loggedUser=new Database().getUser(obj.getString("login"));
                    if(loggedUser==null){
                    // tento riadok by sa nemal nikdy vykonat, osetrene kvoli jave
                        return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{}");
                    }
                    res.put("fname",loggedUser.getFname());
                    res.put("lname",loggedUser.getLname());
                    res.put("login",loggedUser.getLogin());
                    String token = generateToken();
                    res.put("token",token);
                    listOfTokens.put(loggedUser.getLogin(),token);
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
        String pass = new Database().getPassword(login);

            if(pass!=null){
                if(BCrypt.checkpw(password,pass))
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
            if(new Database().existUser(objj.getString("login"))){
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

            boolean result = new Database().addUser(user);
            if(!result){
                JSONObject res = new JSONObject();
                res.put("error","Database issue");
                return ResponseEntity.status(500).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }
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

    @RequestMapping(method=RequestMethod.POST, value="/logout")
    public ResponseEntity<String> logout(@RequestBody String data, @RequestHeader(name = "Authorization") String token){
        JSONObject obj = new JSONObject(data);

        String login = obj.getString("login");
        User user = new Database().getUser(login);
        if(user!=null && token.equals(listOfTokens.get(login))){
            // login aj token su ok, ideme odhlasit
            listOfTokens.remove(login);
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

    @RequestMapping("/users")
    public ResponseEntity<String> getUsers(@RequestParam(value="token") String token) {

        if(token==null){
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body("{\"error\",\"Bad request\"}");
        }
        if(isTokenValid(token)){
            JSONArray array = new JSONArray();
            List<User> users= new Database().getUsers();
            for(User user : users){
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
                User user = new Database().getUser(login);
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
        Log log = new Log(login,getCurrentDateTime(),type);
        new Database().addLog(log);
    }

    @RequestMapping(method=RequestMethod.PATCH, value="/changepassword")
    public ResponseEntity<String> changePasswd(@RequestBody String data ){
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
            if(!new Database().existUser(login) || !checkPassword(login,oldpassword)){
                JSONObject res = new JSONObject();
                res.put("error","Invalid login or password");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }

            String hashPass = hash(objj.getString("newpassword"));

            User user = new Database().getUser(login);
            user.setPassword(hashPass);
            new Database().changePassword(user);
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
            User user = new Database().getUser(login);

            String token2 = listOfTokens.get(login);
            if(user==null || token2==null || !token2.equals(token)){
                JSONObject res = new JSONObject();
                res.put("error","Invalid login or token");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }
            JSONArray res = new JSONArray();
            List<Log> logs = new Database().getLogs(login);

            for(Log temp:logs){
                JSONObject obj = new JSONObject();
                obj.put("date",temp.getDate());
                obj.put("type",temp.getType());
                obj.put("login",login);

                   res.put(obj);
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

    @RequestMapping(method=RequestMethod.POST, value="/message/new")
    public ResponseEntity<String> postMessage(@RequestBody String data, @RequestHeader(name = "Authorization") String token){
        JSONObject obj = new JSONObject(data);/*don't copy*/
        if(obj.has("from") && obj.has("to") && obj.has("message")){
            String from = obj.getString("from");
            String to = obj.getString("to");
            String message = obj.getString("message");
            Database db=new Database();
            if(!isTokenValid(token)){
                JSONObject res = new JSONObject();
                res.put("error","Invalid token");
                return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }
            /*if(!db.existUser(from) || !db.existUser(to)){
                JSONObject res = new JSONObject();
                res.put("error","User does not exist");
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
            }*/
            Message newMessage = new Message(from,to,getCurrentDateTime(),message);
            db.addMessage(newMessage);
            return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body("{}");

        }
        else {
            JSONObject res = new JSONObject();
            res.put("error","Invalid body request");
            return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(res.toString());
        }
    }
}
