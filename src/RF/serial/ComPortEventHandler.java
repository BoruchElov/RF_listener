package RF.serial;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;

/**
 * Класс-обработчик/маппер событий: переводит int-код из jSerialComm в ComEventType.
 * Можно держать всю логику обработки/логирования здесь, чтобы не раздувать cMAC.
 */
public final class ComPortEventHandler {

    /** Главный метод: принимает int-тип события и возвращает enum. */
    private static ComEventType fromCode(int code) {
        return switch (code) {
            case SerialPort.LISTENING_EVENT_DATA_AVAILABLE   -> ComEventType.DATA_AVAILABLE;
            case SerialPort.LISTENING_EVENT_DATA_RECEIVED    -> ComEventType.DATA_RECEIVED;
            case SerialPort.LISTENING_EVENT_DATA_WRITTEN     -> ComEventType.DATA_WRITTEN;
            case SerialPort.LISTENING_EVENT_CTS              -> ComEventType.CTS;
            case SerialPort.LISTENING_EVENT_DSR              -> ComEventType.DSR;
            case SerialPort.LISTENING_EVENT_BREAK_INTERRUPT  -> ComEventType.BREAK_INTERRUPT;
            case SerialPort.LISTENING_EVENT_PARITY_ERROR     -> ComEventType.PARITY_ERROR;
            case SerialPort.LISTENING_EVENT_RING_INDICATOR   -> ComEventType.RING_INDICATOR;
            case SerialPort.LISTENING_EVENT_CARRIER_DETECT   -> ComEventType.CARRIER_DETECT;
            case SerialPort.LISTENING_EVENT_PORT_DISCONNECTED-> ComEventType.PORT_DISCONNECTED;
            default                                          -> ComEventType.UNKNOWN;
        };
    }

    /** Удобный перегруженный метод: сразу принимает событие jSerialComm. */
    public static ComEventType fromEvent(SerialPortEvent e) {
        return fromCode(e.getEventType());
    }

    public static String messageToString(SerialPortEvent e) {
        return switch (fromEvent(e)) {
            case DATA_AVAILABLE -> "Обращение к COM-порту";
            case DATA_RECEIVED -> "Данные получены извне";
            case DATA_WRITTEN -> "Данные приняты к отправке";
            case CTS -> "Изменился сигнал CTS (Clear To Send)";
            case DSR -> "Изменился сигнал DSR (Data Set Ready)";
            case BREAK_INTERRUPT -> "В линии UART обнаружен сигнал BREAK";
            case PARITY_ERROR -> "Ошибка чётности при приёме байта.";
            case RING_INDICATOR -> "Сигнал «звонок» (Ring Indicator)";
            case CARRIER_DETECT -> "Изменение сигнала CD (Carrier Detect)";
            case PORT_DISCONNECTED -> "Порт отключён";
            case UNKNOWN -> "Неизвестная ошибка";
        };
    }

}

