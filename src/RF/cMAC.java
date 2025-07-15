package RF;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.nio.ByteBuffer;
import java.util.concurrent.*;
import java.util.Map;

public class cMAC implements AutoCloseable, Runnable, SerialPortDataListener  {
	private Address myMAC  =  new Address(0x0912ABE1);
	private SerialPort  SP;
	private final Map<Integer, PacketHandler> upperLayerHandlers = new ConcurrentHashMap<>();
	private final BlockingQueue<byte[]> sendQueue = new LinkedBlockingQueue<>();
	private volatile boolean running = true;
	
	public cMAC(String COMport_name) {
		
		SP = SerialPort.getCommPort(COMport_name);			// Получить дескриптор на COM порт
		
		// Настройка базовых параметров
		SP.setBaudRate(9600);
		SP.setNumDataBits(8);
		SP.setNumStopBits(SerialPort.ONE_STOP_BIT);
		SP.setParity(SerialPort.NO_PARITY);
		try{
			if(SP.openPort()) {								// Открываем COM порт
				System.out.println("(MAC) COM порт открыт");
			}
			else {
				System.out.println("(MAC) Не удалось открыть порт");
				return;
			}
			
			Thread.sleep(100);
			SP.flushIOBuffers();							// Отчистить буфер приема от мусора
			
			SP.addDataListener(this); 						// Добавлям слушателя к COM порту
			
	
			SP.setFlowControl(SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED); // Аппаратное управление  управление потоком
			
	        // Запускаем поток внутри конструктора (не рекомендуется, но технически возможно)
	        Thread thread = new Thread(this); // this — ссылка на Runnable
	        thread.start();
				
		} 
		catch (Exception e) {
			SP.closePort();
		}
	}
	
	public Address getMAC() {
		return new Address(myMAC.getValue());
	}
	
    // Внешний метод — добавить пакет на отправку
    public void sendPacket(Address addrDes, byte[] packet) {
    	
    	ByteBuffer Packet = ByteBuffer.allocate(packet.length + 8);
    	Packet.putInt(addrDes.getValue());							// Адресс получателя
    	Packet.putInt(this.getMAC().getValue());					// Адрусс отправителя
    	Packet.put(packet);											// PayLoad
    	
        sendQueue.offer(Packet.array()); 							// Отправить пакет без блокировки
    }
	
    // Остановка потока
    public void stop() {
        running = false;
    }
    
    @Override
	public void run() {
    	System.out.println("(MAC) Поток отправки сообщений MAC запущен");
    	
		while (running) {
            try {
                byte[] packet = sendQueue.take(); // ждет, если очередь пуста
                int written = SP.writeBytes(packet, packet.length);
                if (written != packet.length) {
                    System.err.println("(MAC) Ошибка при отправке пакета");
                    // Можно добавить повторную попытку
                } else {
                    System.out.println("(MAC) Пакет отправлен: " + bytesToHex(packet));
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
	}
    
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes)
            sb.append(String.format("%02X ", b));
        return sb.toString();
    }

    
    @Override
    public void close() {
        if (SP != null && SP.isOpen()) {
        	SP.closePort();
            System.out.println("(MAC) COM-порт закрыт.");
        }
    }
    
    @Override
    public int getListeningEvents() {
        return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
            return;

        byte[] buffer = new byte[SP.bytesAvailable()];
        SP.readBytes(buffer, buffer.length);

        System.out.println("(MAC) Получено асинхронно: " + bytesToHex(buffer));
        
        // Здесь обработка полученных данных
        handlePacket(buffer);
    }
    
    private void handlePacket(byte[] Buff) {
        
    	// Проверка адресе.
    	// Пока планируем что проверку адреса будем осуществлять на UCS Stick
    	
    	
    	// Распознаём структуру пакета, проверяет заголовок и решает, кому дальше передать
    	Integer PacketType = Byte.toUnsignedInt(Buff[8]);
    	Address adrsrc = new Address(Buff, 4);
    	ByteBuffer PacketPayload = ByteBuffer.wrap(Buff, 8, Buff.length - 8).slice();
     	
    	PacketHandler handler = upperLayerHandlers.get(PacketType);
        if (handler != null) {
            handler.handlePacket(adrsrc, PacketPayload);
        } else {
            System.out.println("(MAC) Нет обработчика для типа: " + PacketType);
        }
    }
    
    // Регистрация обработчика для определённого типа пакета
    public void registerHandler(int type, PacketHandler handler) {
        upperLayerHandlers.put(type & 0xFF, handler); // & 0xFF — для безопасности
    }
}
