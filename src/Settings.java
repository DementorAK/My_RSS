import java.awt.*;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Settings {

    final public static Short version = 1;

    final public static Color backgroundColor = new Color(0x1a, 0x1a, 0x1a);
    final public static Color backgroundColor2 = new Color(61, 61, 61);

    final public static Color backgroundColorSetting = new Color(37, 81, 114);
    final public static Color backgroundColorSetting2 = new Color(40, 100, 136);
    final public static Color textColorSetting = new Color(255, 255, 255);
    final public static Color textColorSetting2 = new Color(238, 213, 54);

    final public static Color titleColor = new Color(0x3a, 0xa2, 0xd7);
    final public static Color dateColor = new Color(83, 215, 64);

    static private Date dateUpdate;

    public static Byte numberMessagesOnPage;
    public static Boolean useAutoUpdate;
    public static Short timeUpdate;
    public static RSSChannel channel;

    public static List<RSSChannel> availableChannels;

    public static void readSettings(){
        try {
            DBHelper dbHelper = DBHelper.getInstance();
            dbHelper.readSettings();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            loadDefaults();
        }
    }

    public static void loadDefaults(){
        numberMessagesOnPage = 20;
        useAutoUpdate = false;
        timeUpdate = 0;
        channel = RSSChannel.getDefaultChannel();
    }

    public static void setCurrentDate(){
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        dateUpdate = calendar.getTime();
    }

    public static Boolean isCurrentDay(Date day){
        return dateUpdate.equals(day) || dateUpdate.before(day);
    }

}
