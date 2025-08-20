package RF;

import com.fazecast.jSerialComm.SerialPort;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Arrays;

/**
 * Простой графический интерфейс на JavaFX для настройки соединения
 * и вызова функций класса {@link Inverters}.
 */
public class MainUI extends Application {

    private ComboBox<String> portCombo;
    private ComboBox<Integer> baudCombo;
    private ComboBox<Integer> dataBitsCombo;
    private ComboBox<String> parityCombo;
    private ComboBox<Integer> stopBitsCombo;
    private Button connectButton;

    private ComboBox<String> funcCombo;
    private TextField addrField;
    private TextField parField;
    private TextField valueField;
    private Button executeButton;
    private TextArea outputArea;

    private cMAC mac;
    private ARP arp;
    private Inverters invs;

    @Override
    public void start(Stage stage) {
        initUI(stage);
    }

    private void initUI(Stage stage) {
        portCombo = new ComboBox<>();
        portCombo.getItems().addAll(cMAC.getAvailablePorts());
        if (!portCombo.getItems().isEmpty()) {
            portCombo.getSelectionModel().select(0);
        }

        baudCombo = new ComboBox<>();
        baudCombo.getItems().addAll(9600, 19200, 38400, 57600, 115200);
        baudCombo.getSelectionModel().select(Integer.valueOf(9600));

        dataBitsCombo = new ComboBox<>();
        dataBitsCombo.getItems().addAll(5, 6, 7, 8);
        dataBitsCombo.getSelectionModel().select(Integer.valueOf(8));

        parityCombo = new ComboBox<>();
        parityCombo.getItems().addAll("None", "Odd", "Even");
        parityCombo.getSelectionModel().select("None");

        stopBitsCombo = new ComboBox<>();
        stopBitsCombo.getItems().addAll(1, 2);
        stopBitsCombo.getSelectionModel().select(Integer.valueOf(1));

        connectButton = new Button("Connect");
        connectButton.setOnAction(e -> onConnect());

        GridPane connGrid = new GridPane();
        connGrid.setHgap(5);
        connGrid.setVgap(5);
        connGrid.add(new Label("Port"), 0, 0);
        connGrid.add(new Label("Baud"), 1, 0);
        connGrid.add(new Label("Data bits"), 2, 0);
        connGrid.add(new Label("Parity"), 3, 0);
        connGrid.add(new Label("Stop bits"), 4, 0);
        connGrid.add(portCombo, 0, 1);
        connGrid.add(baudCombo, 1, 1);
        connGrid.add(dataBitsCombo, 2, 1);
        connGrid.add(parityCombo, 3, 1);
        connGrid.add(stopBitsCombo, 4, 1);
        connGrid.add(connectButton, 5, 1);

        funcCombo = new ComboBox<>();
        funcCombo.getItems().addAll("BlinkLedStart", "BlinkLedStop", "SetParameter", "GetParameter");
        funcCombo.getSelectionModel().select(0);
        funcCombo.setOnAction(e -> updateFuncFields());

        addrField = new TextField();
        parField = new TextField();
        valueField = new TextField();
        executeButton = new Button("Выполнить");
        executeButton.setOnAction(e -> onExecute());

        GridPane funcGrid = new GridPane();
        funcGrid.setHgap(5);
        funcGrid.setVgap(5);
        funcGrid.add(new Label("Function"), 0, 0);
        funcGrid.add(new Label("Addr (hex)"), 1, 0);
        funcGrid.add(new Label("Par"), 2, 0);
        funcGrid.add(new Label("Value"), 3, 0);
        funcGrid.add(funcCombo, 0, 1);
        funcGrid.add(addrField, 1, 1);
        funcGrid.add(parField, 2, 1);
        funcGrid.add(valueField, 3, 1);
        funcGrid.add(executeButton, 4, 1);

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPrefRowCount(10);

        VBox root = new VBox(10, connGrid, funcGrid, outputArea);
        root.setPadding(new Insets(10));

        stage.setTitle("RF Listener");
        stage.setScene(new Scene(root));
        stage.show();

        updateFuncFields();
    }

    private void updateFuncFields() {
        String func = funcCombo.getSelectionModel().getSelectedItem();
        boolean needsPar = "SetParameter".equals(func);
        boolean needsValue = "SetParameter".equals(func);

        parField.setDisable(!needsPar);
        valueField.setDisable(!needsValue);
    }

    private void onConnect() {
        if (mac != null) {
            try { mac.close(); } catch (Exception ignored) {}
            mac = null; arp = null; invs = null;
            connectButton.setText("Connect");
            output("Disconnected");
            return;
        }

        String port = portCombo.getSelectionModel().getSelectedItem();
        int baud = baudCombo.getSelectionModel().getSelectedItem();
        int dataBits = dataBitsCombo.getSelectionModel().getSelectedItem();
        int stopBits = stopBitsCombo.getSelectionModel().getSelectedItem() == 1 ? SerialPort.ONE_STOP_BIT : SerialPort.TWO_STOP_BITS;
        int parity;
        switch (parityCombo.getSelectionModel().getSelectedItem()) {
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
        output("Connected to " + port);
    }

    private void onExecute() {
        if (invs == null) {
            output("Not connected");
            return;
        }

        String func = funcCombo.getSelectionModel().getSelectedItem();
        String addrText = addrField.getText().trim();
        if (addrText.isEmpty()) {
            output("Address required");
            return;
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    int addrVal = (int) Long.parseLong(addrText, 16);
                    Address addr = new Address(addrVal);
                    boolean res;
                    switch (func) {
                        case "BlinkLedStart":
                            res = invs.BlinkLedStart(addr, false);
                            output("BlinkLedStart: " + res);
                            break;
                        case "BlinkLedStop":
                            res = invs.BlinkLedStop(addr, false);
                            output("BlinkLedStop: " + res);
                            break;
                        case "SetParameter":
                            int par = Integer.parseInt(parField.getText().trim());
                            float val = Float.parseFloat(valueField.getText().trim());
                            res = invs.SetParameter(addr, par, val);
                            output("SetParameter: " + res);
                            break;
                        case "GetParameter":
                            float[] vals = new float[20];
                            res = invs.GetParameter(addr, vals);
                            if (res) {
                                output("GetParameter: " + Arrays.toString(vals));
                            } else {
                                output("GetParameter: false");
                            }
                            break;
                    }
                } catch (Exception ex) {
                    output("Error: " + ex.getMessage());
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private void output(String text) {
        Platform.runLater(() -> outputArea.appendText(text + "\n"));
    }

    public static void main(String[] args) {
        launch(args);
    }
}

