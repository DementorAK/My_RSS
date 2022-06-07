import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

public class RSSHelper {

    private static Timer timer;

    public static void startLoadData(){

        loadData();

        if (Settings.useAutoUpdate && Settings.timeUpdate > 0) {

            if (timer != null) {
                timer.stop();
                timer.setDelay(Settings.timeUpdate * 60000);
            }

            else
                timer = new Timer(Settings.timeUpdate * 60000, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        loadData();
                    }
                });

            timer.start();

        }
    }

    public static void loadData() {
        Settings.setCurrentDate();
        RSSReader rssReader = new RSSReader();
        RSSReader.Response response = rssReader.checkUrl(Settings.channel.path);
        if (response.succes) {
            try {
                DBHelper.saveMessages(response.messages);
                GUIHelper.loadMessages(DBHelper.getMessages());
            } catch (SQLException e) {
                e.printStackTrace();
                GUIHelper.loadMessages(response.messages);
            }
        } else {
            JOptionPane.showMessageDialog(null, "ERROR: "+response.description);
            // if the Internet is not available, then load from the database
            try {
                GUIHelper.loadMessages(DBHelper.getMessages());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

}

class RSSChannel {
    UUID id;
    String title;
    String path;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RSSChannel that = (RSSChannel) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public RSSChannel(UUID id, String title, String path) {
        this.id = id;
        this.title = title;
        this.path = path;
    }

    @Override
    public String toString() {
        return title;
    }

    public static RSSChannel getDefaultChannel(){
        return new RSSChannel(
                UUID.randomUUID(),
                "BBC News - Business",
                "http://feeds.bbci.co.uk/news/business/rss.xml"
        );
    }

}

class RSSMessage {

    public String guid;
    public String title;
    public String description;
    public String link;
    public Date pubDate;

    public RSSMessage() {
        super();
    }

    public RSSMessage(String guid, String title, String description, String link, Date pubDate) {
        this.guid = guid;
        this.title = title;
        this.description = description;
        this.link = link;
        this.pubDate = pubDate;
    }
}

class RSSReader {

    class Response {
        boolean succes;
        String description;
        List<RSSMessage> messages;

        public Response(boolean succes, String description, List<RSSMessage> messages) {
            this.succes = succes;
            this.description = description;
            this.messages = messages;
        }
    }

    public RSSReader() {
        super();
    }

    public Response checkUrl(String path){
        URL url = null;
        URLConnection connection = null;

        try {
            url = new URL(path);
        } catch (MalformedURLException e) {
            return new Response(false, "Malformed URL", null);
        }

        try {
            connection = url.openConnection();
        } catch (IOException e) {
            return new Response(false, "URL not found", null);
        }

        Document document = null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            document = db.parse(connection.getInputStream());
            document.getDocumentElement().normalize();
        } catch (ParserConfigurationException e) {
            return new Response(false, "internal error...", null);
        } catch (SAXException | IOException e) {
            return new Response(false, "Content not found", null);
        }

        NodeList channels = document.getElementsByTagName("channel");
        if (channels==null || channels.getLength()==0)
            return new Response(false, "Channel not found", null);

        Node channel = channels.item(0);
        NodeList channelChildNodes = channel.getChildNodes();

        String description = "";
        List<RSSMessage> messages = new ArrayList<>();

        for (int i = 0; i < channelChildNodes.getLength(); i++) {
            Node child = channelChildNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE){
                if (child.getNodeName()=="title")
                    description = child.getTextContent();
                else if (child.getNodeName()=="item") {
                    RSSMessage rssMessage = new RSSMessage();
                    NodeList itemChildren = child.getChildNodes();
                    for (int j = 0; j < itemChildren.getLength(); j++) {
                        Node itemChild = itemChildren.item(j);
                        if (itemChild.getNodeName()=="title")
                            rssMessage.title = itemChild.getTextContent();
                        else if (itemChild.getNodeName()=="description")
                            rssMessage.description = itemChild.getTextContent();
                        else if (itemChild.getNodeName()=="link")
                            rssMessage.link = itemChild.getTextContent();
                        else if (itemChild.getNodeName()=="pubDate")
                            rssMessage.pubDate = parseDate(itemChild.getTextContent());
                        else if (itemChild.getNodeName()=="guid")
                                rssMessage.guid = itemChild.getTextContent();
                    }
                    if (rssMessage.pubDate==null){
                        // some sources do not have publication dates
                        rssMessage.pubDate = GregorianCalendar.getInstance().getTime();
                    }
                    messages.add(rssMessage);
                }
            }
        }

        Collections.sort(messages, new Comparator<RSSMessage>() {
            @Override
            public int compare(RSSMessage rssMessage, RSSMessage nextMessage) {
                return rssMessage.pubDate.compareTo(nextMessage.pubDate);
            }
        });

        return new Response(true, description, messages);
    }

    private Date parseDate(String string){

        // default value
        Date result = GregorianCalendar.getInstance().getTime();

        try {
            result = new Date(string);
            return result;
        } catch (Exception e){}

        //Sun, 25 Mar 2018 11:52:46 GMT
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzzz");
        try {
            result = formatter.parse(string);
            return result;
        } catch (Exception e){}

        // 2003-12-13T18:30:02......
        formatter = new SimpleDateFormat("yyyy-mm-ddTHH:mm:ss");
        try {
            result = formatter.parse(string.substring(0,19));
            return result;
        } catch (Exception e){}

        return result;
    }

}

class RssFeedTableModel extends AbstractTableModel {
    private List<RSSMessage> messages = new ArrayList<>();

    public void updateData(List<RSSMessage> messages){
        this.messages = messages;
        fireTableDataChanged();
    }
    public RSSMessage getFeedMessage(int index){
        return messages.get(index);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex){
            case 0: return String.class;
            case 1: return Date.class;
        }
        return Object.class;
    }

    @Override
    public int getRowCount() {
        return messages.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex){
            case 0: return messages.get(rowIndex).title;
            case 1: return messages.get(rowIndex).pubDate;
        }
        return null;
    }
}
