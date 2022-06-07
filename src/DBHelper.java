import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DBHelper {

    private final String JDBC_DRIVER = "org.h2.Driver";
    private final String LOGIN       = "admin";
    private final String PASSWORD    = "admin";
    private final String DB_NAME     = "./rss_settings";

    private static Connection connection = null;
    private static DBHelper instance = null;

    private DBHelper() throws ClassNotFoundException, SQLException {
        if (connection==null) {
            Class.forName(JDBC_DRIVER);
            connection = DriverManager.getConnection("jdbc:h2:" + DB_NAME, LOGIN, PASSWORD);
            checkTables();
        }
    }
    public static DBHelper getInstance() throws SQLException, ClassNotFoundException {
        if (instance==null)
            instance = new DBHelper();
        return instance;
    }

    private void checkTables() throws SQLException {

        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet resultSet = metaData.getTables(null, null, "RSS_SETTINGS", new String[] {"TABLE"});

        if (!resultSet.next()){

            // it is first start - need create tables
            Statement statement = connection.createStatement();
            statement.execute("CREATE TABLE rss_Settings (" +
                    "use_auto_update BOOLEAN, time_update SMALLINT, " +
                    "number_messages TINYINT, current_channel UUID, " +
                    "version SMALLINT, PRIMARY KEY (version))");
            statement.execute("CREATE TABLE rss_Channels (" +
                    "id UUID, name VARCHAR(30), path VARCHAR(300), PRIMARY KEY (id))");
            statement.execute("CREATE TABLE rss_Messages (" +
                    "guid VARCHAR(500), title VARCHAR(200), description VARCHAR(500), " +
                    "link VARCHAR(500), pubDate TIMESTAMP, channel UUID, " +
                    "id BIGINT auto_increment, PRIMARY KEY (id))");
            statement.close();

            // load default RSS-chanel
            Settings.loadDefaults();
            PreparedStatement prepStatement = connection.prepareStatement(
                    "INSERT INTO rss_Channels(id, name, path) VALUES (?,?,?)");
            prepStatement.setObject(1, Settings.channel.id);
            prepStatement.setString(2, Settings.channel.title);
            prepStatement.setString(3, Settings.channel.path);
            prepStatement.execute();
            // load default settings
            prepStatement = connection.prepareStatement(
                    "INSERT INTO rss_Settings" +
                            "(version, use_auto_update, time_update, number_messages, current_channel) " +
                            "VALUES (?,?,?,?,?)");
            prepStatement.setShort(1, Settings.version);
            prepStatement.setBoolean(2, Settings.useAutoUpdate);
            prepStatement.setShort(3, Settings.timeUpdate);
            prepStatement.setByte(4, Settings.numberMessagesOnPage);
            prepStatement.setObject(5, Settings.channel.id);
            prepStatement.execute();
            prepStatement.close();

        }

        // in new versions there will be a transfer of settings
    }

    public void readSettings() throws SQLException {

        Statement statement = connection.createStatement();

        ResultSet result = statement.executeQuery(
                "SELECT * FROM rss_Settings AS Sett " +
                        "LEFT JOIN rss_Channels AS Ch " +
                            "ON Sett.current_channel=Ch.id " +
                        "WHERE version="+Settings.version);
        if (result.next()){
            Settings.useAutoUpdate = result.getBoolean("use_auto_update");
            Settings.timeUpdate = result.getShort("time_update");
            Settings.numberMessagesOnPage = result.getByte("number_messages");
            Settings.channel = new RSSChannel(
                    (UUID) result.getObject("id"),
                    result.getString("name"),
                    result.getString("path")
            );
        }

        Settings.availableChannels = new ArrayList<>();
        result = statement.executeQuery(
                "SELECT * FROM rss_Channels ORDER BY name");
        while (result.next()){
            Settings.availableChannels.add(new RSSChannel(
                    (UUID) result.getObject("id"),
                    result.getString("name"),
                    result.getString("path")
            ));
        }

        result.close();

    }

    public void saveSettings() throws SQLException {

        PreparedStatement prepStatement = connection.prepareStatement(
                "UPDATE rss_Settings SET " +
                            "use_auto_update=?, " +
                            "time_update=?, " +
                            "number_messages=?, " +
                            "current_channel=? " +
                        "WHERE version="+Settings.version);

        prepStatement.setBoolean(1, Settings.useAutoUpdate);
        prepStatement.setShort(2, Settings.timeUpdate);
        prepStatement.setByte(3, Settings.numberMessagesOnPage);
        prepStatement.setObject(4, Settings.channel.id);

        prepStatement.execute();

        prepStatement.close();

    }

    public void saveNewChannel(RSSChannel newChannel) throws SQLException {

        PreparedStatement prepStatement = connection.prepareStatement(
                "INSERT INTO rss_Channels(id, name, path) VALUES (?,?,?)");

        prepStatement.setObject(1, newChannel.id);
        prepStatement.setString(2, newChannel.title);
        prepStatement.setString(3, newChannel.path);

        prepStatement.execute();

    }

    public static void saveMessages(List<RSSMessage> messages) throws SQLException {

        if (messages.size()==0) return;

        int batchLimit = 20;

        connection.setAutoCommit(false);
        PreparedStatement prepStatement = connection.prepareStatement(
                "INSERT INTO rss_Messages " +
                        "(guid, channel, title, description, link, pubDate) " +
                        "SELECT guid, channel, title, description, link, pubDate FROM " +
                            "(SELECT CAST(? as VARCHAR) as guid, CAST(? as UUID) as channel, " +
                                "CAST(? as VARCHAR) as title, CAST(? as VARCHAR) as description, " +
                                "CAST(? as VARCHAR) as link, CAST(? as TIMESTAMP) as pubDate) sub " +
                        "WHERE NOT EXISTS (SELECT 1 FROM rss_Messages m " +
                            "WHERE sub.channel = m.channel AND sub.guid = m.guid)");

        int batchCount = 0;
        for (RSSMessage message: messages) {
            batchCount++;
            prepStatement.setString(1, message.guid);
            prepStatement.setObject(2, Settings.channel.id);
            prepStatement.setString(3, message.title);
            prepStatement.setString(4, message.description);
            prepStatement.setString(5, message.link);
            prepStatement.setTimestamp(6, new Timestamp(message.pubDate.getTime()));
            prepStatement.addBatch();
            if (batchCount==batchLimit){
                batchCount=0;
                prepStatement.executeBatch();
                connection.commit();
                prepStatement.clearBatch();
            }
        }
        if (batchCount>0) {
            prepStatement.executeBatch();
            connection.commit();
            prepStatement.clearBatch();
        }

        prepStatement.close();
        connection.setAutoCommit(true);

    }

    public static List<RSSMessage> getMessages() throws SQLException {

        List<RSSMessage> messages = new ArrayList<>();

        String limit = Settings.numberMessagesOnPage==0?"":"TOP " + Settings.numberMessagesOnPage;
        PreparedStatement prepStatement = connection.prepareStatement(
                "SELECT "+limit+" * FROM rss_Messages WHERE channel=? ORDER BY pubDate DESC, id DESC");
        prepStatement.setObject(1, Settings.channel.id);

        ResultSet result = prepStatement.executeQuery();
        while (result.next()){
            messages.add(new RSSMessage(
                    result.getString("guid"),
                    result.getString("title"),
                    result.getString("description"),
                    result.getString("link"),
                    new java.util.Date(result.getTimestamp("pubDate").getTime())
            ));
        }
        result.close();

        return messages;
    }

}
