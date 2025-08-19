package RF;

import com.fazecast.jSerialComm.SerialPort;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;

/**
 * Простой графический интерфейс для настройки соединения и вызова функций Inverters.
 */
public class MainUI {
    private JFrame frame;
    private JComboBox<String> portCombo;
    private JComboBox<Integer> baudCombo;
    private JComboBox<Integer> dataBitsCombo;
    private JComboBox<String> parityCombo;
    private JComboBox<String> stopBitsCombo;
    private JButton connectButton;

    private JComboBox<String> funcCombo;
    private JTextField addrField;
    private JTextField parField;
    private JTextField valueField;
    private JButton executeButton;
    private JTextArea outputArea;

    private cMAC mac;
    private ARP arp;
    private Inverters invs;

    public MainUI() {
        initUI();
    }

    private void initUI() {
        frame = new JFrame("RF Listener");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // --- Панель соединения ---
        JPanel connPanel = new JPanel(new GridLayout(2,6));

        portCombo = new JComboBox<>(cMAC.getAvailablePorts());
        baudCombo = new JComboBox<>(new Integer[]{9600,19200,38400,57600,115200});
        dataBitsCombo = new JComboBox<>(new Integer[]{5,6,7,8});
        parityCombo = new JComboBox<>(new String[]{"None","Odd","Even"});
        stopBitsCombo = new JComboBox<>(new String[]{"1","2"});
        connectButton = new JButton("Connect");

        connPanel.add(new JLabel("Port"));
        connPanel.add(new JLabel("Baud"));
        connPanel.add(new JLabel("Data bits"));
        connPanel.add(new JLabel("Parity"));
        connPanel.add(new JLabel("Stop bits"));
        connPanel.add(new JLabel(""));

        connPanel.add(portCombo);
        connPanel.add(baudCombo);
        connPanel.add(dataBitsCombo);
        connPanel.add(parityCombo);
        connPanel.add(stopBitsCombo);
        connPanel.add(connectButton);

        frame.add(connPanel, BorderLayout.NORTH);

        // --- Панель функций ---
        JPanel funcPanel = new JPanel(new GridLayout(2,5));
        funcCombo = new JComboBox<>(new String[]{"BlinkLedStart","BlinkLedStop","SetParameter","GetParameter"});
        addrField = new JTextField();
        parField = new JTextField();
        valueField = new JTextField();
        executeButton = new JButton("Выполнить");

        funcPanel.add(new JLabel("Function"));
        funcPanel.add(new JLabel("Addr (hex)"));
        funcPanel.add(new JLabel("Par"));
        funcPanel.add(new JLabel("Value"));
        funcPanel.add(new JLabel(""));

        funcPanel.add(funcCombo);
        funcPanel.add(addrField);
        funcPanel.add(parField);
        funcPanel.add(valueField);
        funcPanel.add(executeButton);

        frame.add(funcPanel, BorderLayout.CENTER);

        // --- Вывод ---
        outputArea = new JTextArea(10,40);
        outputArea.setEditable(false);
        frame.add(new JScrollPane(outputArea), BorderLayout.SOUTH);

        // Action listeners
        connectButton.addActionListener(this::onConnect);
        executeButton.addActionListener(this::onExecute);

        funcCombo.addActionListener(e -> updateFuncFields());
        updateFuncFields();

        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    private void updateFuncFields() {
        String func = (String) funcCombo.getSelectedItem();
        boolean needsPar = "SetParameter".equals(func);
        boolean needsValue = "SetParameter".equals(func);

        parField.setEnabled(needsPar);
        valueField.setEnabled(needsValue);
    }

    private void onConnect(ActionEvent e) {
        if (mac != null) {
            try {
                mac.close();
            } catch (Exception ex) { /* ignore */ }
            mac = null; arp = null; invs = null;
            connectButton.setText("Connect");
            outputArea.append("Disconnected\n");
            return;
        }

        String port = (String) portCombo.getSelectedItem();
        int baud = (Integer) baudCombo.getSelectedItem();
        int dataBits = (Integer) dataBitsCombo.getSelectedItem();
        int stopBits = "1".equals(stopBitsCombo.getSelectedItem()) ? SerialPort.ONE_STOP_BIT : SerialPort.TWO_STOP_BITS;
        int parity;
        switch ((String) parityCombo.getSelectedItem()) {
            case "Odd": parity = SerialPort.ODD_PARITY; break;
            case "Even": parity = SerialPort.EVEN_PARITY; break;
            default: parity = SerialPort.NO_PARITY; break;
        }

        mac = new cMAC(port, baud, dataBits, stopBits, parity);
        arp = new ARP(mac);
        invs = new Inverters(mac);

        mac.registerHandler(0x01, arp);
        mac.registerHandler(0x02, invs);

        connectButton.setText("Disconnect");
        outputArea.append("Connected to " + port + "\n");
    }

    private void onExecute(ActionEvent e) {
        if (invs == null) {
            outputArea.append("Not connected\n");
            return;
        }

        String func = (String) funcCombo.getSelectedItem();
        String addrText = addrField.getText().trim();
        if (addrText.isEmpty()) {
            outputArea.append("Address required\n");
            return;
        }

        try {
            int addrVal = (int)Long.parseLong(addrText,16);
            Address addr = new Address(addrVal);
            boolean res;
            switch (func) {
                case "BlinkLedStart":
                    res = invs.BlinkLedStart(addr,false);
                    outputArea.append("BlinkLedStart: " + res + "\n");
                    break;
                case "BlinkLedStop":
                    res = invs.BlinkLedStop(addr,false);
                    outputArea.append("BlinkLedStop: " + res + "\n");
                    break;
                case "SetParameter":
                    int par = Integer.parseInt(parField.getText().trim());
                    float val = Float.parseFloat(valueField.getText().trim());
                    res = invs.SetParameter(addr, par, val);
                    outputArea.append("SetParameter: " + res + "\n");
                    break;
                case "GetParameter":
                    float[] vals = new float[20];
                    res = invs.GetParameter(addr, vals);
                    if (res) {
                        outputArea.append("GetParameter: " + Arrays.toString(vals) + "\n");
                    } else {
                        outputArea.append("GetParameter: false\n");
                    }
                    break;
            }
        } catch (Exception ex) {
            outputArea.append("Error: " + ex.getMessage() + "\n");
        }
    }

    public void show() {
        frame.setVisible(true);
    }
}

