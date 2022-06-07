import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class GUIHelper {

    static JFrame mainFrame;
    static JTable rssTable;
    static JLabel titleLabel;

    public static void createMainFrame(){

        mainFrame = new JFrame();
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainFrame.getContentPane().setBackground(Settings.backgroundColor);
        mainFrame.setSize(500,300);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setUndecorated(true);
        mainFrame.setOpacity(0.8f);

        JTextPane description = new JTextPane();
        description.setBackground(Settings.backgroundColor2);
        description.setForeground(Color.WHITE);
        description.setFont(new Font("Arial", Font.PLAIN, 10));
        description.setPreferredSize(new Dimension(400, 50));
        description.setMargin(new Insets(10,10,10,10));
        mainFrame.getContentPane().add(description, BorderLayout.SOUTH);

        rssTable = new JTable(new RssFeedTableModel());
        rssTable.setShowGrid(false);
        rssTable.setIntercellSpacing(new Dimension(0,0));
        rssTable.setRowHeight(30);
        rssTable.setBackground(Settings.backgroundColor);
        rssTable.setTableHeader(null);
        rssTable.getColumnModel().getColumn(0).setPreferredWidth(380);
        rssTable.getColumnModel().getColumn(1).setPreferredWidth(120);

        rssTable.setDefaultRenderer(String.class, new DefaultTableCellRenderer(){
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBackground(row%2==0?Settings.backgroundColor2:Settings.backgroundColor);
                setForeground(Settings.titleColor);
                setFont(new Font("Arial", Font.BOLD, 12));
                setText("  "+value);
                return this;
            }
        });

        rssTable.setDefaultRenderer(Date.class, new DefaultTableCellRenderer(){
            SimpleDateFormat fDay = new SimpleDateFormat("dd MMMM yyyy");
            SimpleDateFormat fHour = new SimpleDateFormat("HH:mm:ss");
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if(value instanceof Date) {
                    SimpleDateFormat f = Settings.isCurrentDay((Date) value)?fHour:fDay;
                    setText(f.format(value));
                }
                setBackground(row%2==0?Settings.backgroundColor2:Settings.backgroundColor);
                setForeground(Settings.dateColor);
                setFont(new Font("Courier", Font.BOLD, 12));
                setHorizontalAlignment(JLabel.CENTER);
                return this;
            }
        });

        rssTable.setFillsViewportHeight(true);

        rssTable.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                int index = rssTable.convertRowIndexToModel(rssTable.getSelectedRow());
                if (index==-1) return;
                RSSMessage rssMessage = ((RssFeedTableModel) rssTable.getModel()).getFeedMessage(index);
                if (mouseEvent.getClickCount()>1) {
                    try {
                        Desktop.getDesktop().browse(new URL(rssMessage.link).toURI());
                    } catch (Exception e) {}
                } else {
                    description.setText(rssMessage.description);
                }
            }
            @Override
            public void mousePressed(MouseEvent mouseEvent) {}
            @Override
            public void mouseReleased(MouseEvent mouseEvent) {}
            @Override
            public void mouseEntered(MouseEvent mouseEvent) {}
            @Override
            public void mouseExited(MouseEvent mouseEvent) {}
        });

        JScrollPane scrollPane = new JScrollPane(rssTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(0,0));
        scrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0,0));
        mainFrame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        titleLabel = new JLabel();
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setBackground(Settings.backgroundColor);
        titleLabel.setForeground(Color.WHITE);
        setTitle();

        JButton buttonClose = new JButton(" X ");
        buttonClose.setPreferredSize(new Dimension(25, 20));
        buttonClose.setBorder(BorderFactory.createEmptyBorder());
        buttonClose.setBackground(Settings.backgroundColor);
        buttonClose.setForeground(Color.RED);
        buttonClose.setFocusPainted(false);
        buttonClose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                System.exit(0);
            }
        });

        JButton buttonSettings = new JButton(" ⚙ ");
        buttonSettings.setPreferredSize(new Dimension(25, 20));
        buttonSettings.setBorder(BorderFactory.createEmptyBorder());
        buttonSettings.setBackground(Settings.backgroundColor);
        buttonSettings.setForeground(Color.YELLOW);
        buttonSettings.setFocusPainted(false);
        buttonSettings.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                GUIHelper.showSettings();
            }
        });

        JButton buttonReload = new JButton(" ▼ ");
        buttonReload.setPreferredSize(new Dimension(25, 20));
        buttonReload.setBorder(BorderFactory.createEmptyBorder());
        buttonReload.setBackground(Settings.backgroundColor);
        buttonReload.setForeground(Color.GREEN);
        buttonReload.setFocusPainted(false);
        buttonReload.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                new Thread(()->{
                    RSSHelper.loadData();
                }).start();
            }
        });

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setBackground(Settings.backgroundColor);
        buttonsPanel.add(buttonReload);
        buttonsPanel.add(buttonSettings);
        buttonsPanel.add(buttonClose);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Settings.backgroundColor);

        topPanel.add(titleLabel, BorderLayout.CENTER);
        topPanel.add(buttonsPanel, BorderLayout.LINE_END);

        mainFrame.getContentPane().add(topPanel, BorderLayout.NORTH);

        MouseAdapter listener = new MouseAdapter() {
            int startX, startY;
            @Override
            public void mousePressed(MouseEvent e) {
                if(e.getButton()==MouseEvent.BUTTON1){
                    startX = e.getX();
                    startY = e.getY();
                }
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                Point currentPoint = e.getLocationOnScreen();
                mainFrame.setLocation(currentPoint.x-startX, currentPoint.y-startY);
            }
        };
        topPanel.addMouseListener(listener);
        topPanel.addMouseMotionListener(listener);

        mainFrame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                mainFrame.setShape(new RoundRectangle2D.Double(
                        0, 0, mainFrame.getWidth(), mainFrame.getHeight(),20,20));
            }
        });

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                mainFrame.setVisible(true);
            }
        });

    }

    public static void showSettings(){

        JDialog dialog = new JDialog();
        dialog.setTitle("Settings");
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLocationRelativeTo(null);
        dialog.setUndecorated(true);

        dialog.setBackground(Settings.backgroundColorSetting);
        dialog.getContentPane().setBackground(Settings.backgroundColorSetting);
        Border border = BorderFactory.createEtchedBorder();
        border = BorderFactory.createTitledBorder(border, "Settings", TitledBorder.CENTER,0,null, Settings.textColorSetting);
        dialog.getRootPane().setBorder(border);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
        mainPanel.setBackground(Settings.backgroundColorSetting);
        dialog.add(mainPanel);


        // *** >> MAIN SETTINGS >> *** Start

        JPanel panelUpdate = new JPanel();
        panelUpdate.setLayout(new BoxLayout(panelUpdate, BoxLayout.X_AXIS));
        panelUpdate.setBackground(Settings.backgroundColorSetting);

        JCheckBox boxAutoUpd = new JCheckBox("Use auto update every:", Settings.useAutoUpdate);
        boxAutoUpd.setBackground(Settings.backgroundColorSetting);
        boxAutoUpd.setForeground(Settings.textColorSetting);
        panelUpdate.add(boxAutoUpd);
        panelUpdate.add(Box.createHorizontalStrut(10));

        JSpinner minuteSpinner = new JSpinner(new SpinnerNumberModel((int)Settings.timeUpdate, 0, 9999, 1));
        panelUpdate.add(minuteSpinner);
        panelUpdate.add(Box.createHorizontalStrut(5));

        JLabel labelMinute = new JLabel("minutes");
        labelMinute.setForeground(Settings.textColorSetting);
        panelUpdate.add(labelMinute);
        panelUpdate.add(Box.createHorizontalGlue());

        mainPanel.add(panelUpdate);
        mainPanel.add(Box.createVerticalStrut(10));

        JPanel panelNumber = new JPanel();
        panelNumber.setLayout(new BoxLayout(panelNumber, BoxLayout.X_AXIS));
        panelNumber.setBackground(Settings.backgroundColorSetting);

        JSpinner numberSpinner = new JSpinner(new SpinnerNumberModel(
                (int)Settings.numberMessagesOnPage, 0, Byte.MAX_VALUE, 1));
        panelNumber.add(numberSpinner);
        panelNumber.add(Box.createHorizontalStrut(10));

        JLabel labelNumber = new JLabel("messages per page (zero - show all history)");
        labelNumber.setForeground(Settings.textColorSetting);
        panelNumber.add(labelNumber);
        panelNumber.add(Box.createHorizontalGlue());

        mainPanel.add(panelNumber);
        mainPanel.add(Box.createVerticalStrut(10));

        JPanel panelChannel = new JPanel();
        panelChannel.setLayout(new BoxLayout(panelChannel, BoxLayout.X_AXIS));
        panelChannel.setBackground(Settings.backgroundColorSetting);

        JLabel labelChanel = new JLabel("Read RSS-channel:");
        labelChanel.setForeground(Settings.textColorSetting);
        panelChannel.add(labelChanel);
        panelChannel.add(Box.createHorizontalStrut(10));

        JComboBox channelBox = new JComboBox();
        loadChannels(channelBox);
        panelChannel.add(channelBox);

        mainPanel.add(panelChannel);
        mainPanel.add(Box.createVerticalStrut(10));

        // *** << MAIN SETTINGS << *** End


        // *** >> PANEL FOR ADDING NEW CHANNEL >> *** Start

        JPanel panelAddNew = new JPanel();
        panelAddNew.setLayout(new BoxLayout(panelAddNew, BoxLayout.Y_AXIS));
        panelAddNew.setBackground(Settings.backgroundColorSetting2);
        Border borderAdd = BorderFactory.createEtchedBorder();
        borderAdd = BorderFactory.createTitledBorder(borderAdd, "Add new channel", TitledBorder.CENTER,0,null, Settings.textColorSetting2);
        panelAddNew.setBorder(borderAdd);

        JPanel panelCheckURL = new JPanel();
        panelCheckURL.setLayout(new BoxLayout(panelCheckURL, BoxLayout.X_AXIS));
        panelCheckURL.setBackground(Settings.backgroundColorSetting2);

        JLabel labelURL = new JLabel("Enter your URL:");
        labelURL.setForeground(Settings.textColorSetting2);

        JTextField fieldForURL = new JTextField();
        fieldForURL.setToolTipText("https://example.com/rss");

        JButton buttonCheck = new JButton("check");
        buttonCheck.setPreferredSize(new Dimension(70, 1));

        JPanel panelAddURL = new JPanel();
        panelAddURL.setLayout(new BoxLayout(panelAddURL, BoxLayout.X_AXIS));
        panelAddURL.setBackground(Settings.backgroundColorSetting2);

        JLabel labelChannelName = new JLabel("Channel name:");
        labelChannelName.setForeground(Settings.textColorSetting2);
        labelChannelName.setEnabled(false);

        JTextField fieldChannelName = new JTextField();
        fieldChannelName.setPreferredSize(new Dimension(200, 1));
        fieldChannelName.setEnabled(false);

        JButton buttonAdd = new JButton("add");
        buttonAdd.setPreferredSize(new Dimension(70, 1));
        buttonAdd.setEnabled(false);

        buttonCheck.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String newURL = fieldForURL.getText();
                if(newURL.isBlank()) return;
                RSSReader rssReader = new RSSReader();
                RSSReader.Response response = rssReader.checkUrl(newURL);
                if (response.succes){
                    fieldChannelName.setText(response.description);
                    labelURL.setEnabled(false);
                    fieldForURL.setEnabled(false);
                    buttonCheck.setEnabled(false);
                    labelChannelName.setEnabled(true);
                    fieldChannelName.setEnabled(true);
                    buttonAdd.setEnabled(true);
                } else
                    JOptionPane.showMessageDialog(null, "ERROR: "+response.description);
            }
        });

        buttonAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String newURL = fieldForURL.getText();
                if(newURL.isBlank()) return;
                String newName = fieldChannelName.getText().trim();
                if(newName.isBlank()) newName = newURL;
                if(newName.length()>30) newName = newName.substring(0,30);
                RSSChannel newChannel = new RSSChannel(UUID.randomUUID(), newName, newURL);
                // save new channel
                try {
                    DBHelper dbHelper = DBHelper.getInstance();
                    dbHelper.saveNewChannel(newChannel);
                    dbHelper.readSettings();
                } catch (SQLException | ClassNotFoundException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "ERROR: "+e.getMessage());
                }
                // reload JComboBox with channels
                loadChannels(channelBox);
                // set default state
                fieldForURL.setText("");
                fieldChannelName.setText("");
                labelURL.setEnabled(true);
                fieldForURL.setEnabled(true);
                buttonCheck.setEnabled(true);
                labelChannelName.setEnabled(false);
                fieldChannelName.setEnabled(false);
                buttonAdd.setEnabled(false);
            }
        });

        panelCheckURL.add(labelURL);
        panelCheckURL.add(Box.createHorizontalStrut(5));
        panelCheckURL.add(fieldForURL);
        panelCheckURL.add(buttonCheck);

        panelAddNew.add(panelCheckURL);
        panelAddNew.add(Box.createVerticalStrut(10));

        panelAddURL.add(labelChannelName);
        panelAddURL.add(Box.createHorizontalStrut(8));
        panelAddURL.add(fieldChannelName);
        panelAddURL.add(buttonAdd);
        panelAddNew.add(panelAddURL);

        mainPanel.add(panelAddNew);
        mainPanel.add(Box.createVerticalStrut(10));

        // *** << PANEL FOR ADDING NEW CHANNEL << *** End


        // *** >> SAVE & CLOSE BUTTONS >> *** Start

        JPanel panelButtons = new JPanel();
        panelButtons.setBackground(Settings.backgroundColorSetting);

        JButton buttonSave = new JButton("Save");
        buttonSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Settings.useAutoUpdate = boxAutoUpd.isSelected();
                Settings.timeUpdate = ((Integer) minuteSpinner.getValue()).shortValue();
                Settings.numberMessagesOnPage = ((Integer) numberSpinner.getValue()).byteValue();
                Settings.channel = (RSSChannel) channelBox.getSelectedItem();
                try {
                    DBHelper.getInstance().saveSettings();
                } catch (SQLException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
                RSSHelper.startLoadData();
                setTitle();
                dialog.setVisible(false);
            }
        });
        panelButtons.add(buttonSave);

        JButton buttonClose = new JButton("Close");
        buttonClose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                dialog.setVisible(false);
            }
        });
        panelButtons.add(buttonClose);

        mainPanel.add(panelButtons);

        // *** << SAVE & CLOSE BUTTONS << *** End


        dialog.pack();
        dialog.setVisible(true);

    }

    private static void loadChannels(JComboBox channelBox){
        channelBox.removeAll();
        for (RSSChannel channel: Settings.availableChannels) {
            channelBox.addItem(channel);
        }
        channelBox.setSelectedItem(Settings.channel);
    }

    public static void setTitle(){
        titleLabel.setText("RSS: "+Settings.channel.title);
    }

    public static void loadMessages(List<RSSMessage> messages){
        ((RssFeedTableModel) rssTable.getModel()).updateData(messages);
    }

}
