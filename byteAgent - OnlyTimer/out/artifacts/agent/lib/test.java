import java.io.DataInputStream;

public class test
{
    public static void main(String[] args)
    {
        try {
        DataInputStream in = new DataInputStream(System.in);
        String next = in.readLine();
        while (next != null && next.length() > 0 && !next.contains("end")) {
        final String arg = next;
        testf(arg);
        test2(arg);
        next = in.readLine();
        }
    } catch (Exception e) {
    }
    }
    public static void testf(String arg)
    {
        System.out.println(arg);
        test1(arg);
    }
    public static void test1(String arg)
    {
        System.out.println(arg);
    }
    public static void test2(String arg)
    {
        System.out.println(arg);
    }
}
