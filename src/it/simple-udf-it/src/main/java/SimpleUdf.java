public class SimpleUdf {

    public String myStringConcat(String a, String b) {
        try {
            return a + b;
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) {
        System.out.println("Hello World");
    }
}