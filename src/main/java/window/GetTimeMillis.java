package window;

import javax.xml.crypto.Data;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GetTimeMillis {

    public static void main(String[] args) throws ParseException {


        Long currentTimeMillis = System.currentTimeMillis();


//        String str1 = LongToString(1630401344000L, "yyyy-MM-dd HH:mm:ss");
//        System.out.println(str1);
        Long t1 = StringToLong("2021-08-31 17:15:43", "yyyy-MM-dd HH:mm:ss");
        System.out.println(t1);
        Long t2 = StringToLong("2021-08-31 17:15:44", "yyyy-MM-dd HH:mm:ss");
        System.out.println(t2);
        Long t3 = StringToLong("2021-08-31 17:15:47", "yyyy-MM-dd HH:mm:ss");
        System.out.println(t3);
        Long t4 = StringToLong("2021-08-31 17:15:48", "yyyy-MM-dd HH:mm:ss");
        System.out.println(t4);


    }

    public static Long StringToLong(String time, String format) throws ParseException {
        if (time != null) {
            return new SimpleDateFormat(format).parse(time).getTime();
        }
        return null;
    }

    public static String LongToString(Long time, String format) {
        if (time != null) {
            return new SimpleDateFormat(format).format(new Date(time));
        }
        return null;
    }
}
