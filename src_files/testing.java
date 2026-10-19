public class testing {
    public static void main(String[] args) {
        String line = "    4 10 -1";
        line = line.strip();
        String[] parts = line.split(" ");
        for (String part : parts) {
            System.out.println("[" + part + "]");
        }
    }
}
