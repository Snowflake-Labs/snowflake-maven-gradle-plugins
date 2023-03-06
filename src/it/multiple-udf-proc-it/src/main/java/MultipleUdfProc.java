import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.google.gson.Gson;
import com.snowflake.snowpark_java.*;

public class MultipleUdfProc {

    public String myStringConcatWithLog(String a, String b) {
        Log log = LogFactory.getLog(MultipleUdfProc.class);
        try {
            log.info("HERE IS MY LOG");
            String result = a + b;
            log.info(result);
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public String myStringConcat(Session session, String a, String b) {
        try {
            return a + b;
        } catch (Exception e) {
            return null;
        }
    }

    public String myNewJsonSki(int length) {
        Ski ski = new Ski();
        ski.brand = "snowflake";
        ski.length = length;

        Gson gson = new Gson();
        String json = gson.toJson(ski);
        try {
            // Returns a JSON string describing the ski object
            return json;
        } catch (Exception e) {
            return null;
        }
    }

    class Ski {
        public String brand = null;
        public int length = 0;
    }
    public static void main(String[] args) {
        System.out.println("Hello World");
    }
}