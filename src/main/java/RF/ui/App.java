package RF.ui;

import com.fazecast.jSerialComm.SerialPort;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        TextField textField = new TextField();
        textField.setPromptText("Введите текст");

        ComboBox<String> portsBox = new ComboBox<>();
        portsBox.setPromptText("Выберите порт");
        for (SerialPort port : SerialPort.getCommPorts()) {
            portsBox.getItems().add(port.getSystemPortName());
        }

        Button sendButton = new Button("Отправить");
        sendButton.setOnAction(e -> {
            String port = portsBox.getValue();
            String text = textField.getText();
            System.out.printf("Порт: %s, текст: %s%n", port, text);
        });

        VBox root = new VBox(10, portsBox, textField, sendButton);
        root.setStyle("-fx-padding: 10;");

        stage.setTitle("RF Слушатель");
        stage.setScene(new Scene(root));
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
