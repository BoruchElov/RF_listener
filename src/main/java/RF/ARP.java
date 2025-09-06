package RF;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;

public class ARP implements PacketHandler {
    private cMAC MAC;
    
    private final ConcurrentHashMap<Address, Long> deviceLastSeen = new ConcurrentHashMap<>(); 	// Храним: Address -> время последнего пинга
    private static final long DEVICE_TIMEOUT_MS = 10_000;
    
    public ARP(cMAC mac) {
        this.MAC = mac;
    }

    @Override
    public void handlePacket(Address AddSurs, ByteBuffer Buff) {
    	ByteBuffer bufferTX;
        // Обработка ARP пакета
    	System.out.println("(ARP) Получен пакет");
    	
    	byte TypeLayer = Buff.get();
    	byte TypeMsg = Buff.get();
    	
    	switch(TypeMsg) {
    	case 1:					// Запрос на MAC адрес сервера
    		System.out.println("(ARP) Запрос на MAC адрес от: " + AddSurs.toString());
    		
    		bufferTX = ByteBuffer.allocate(1+1+4);
    		
    		Buff.rewind();
    		bufferTX.put(Buff);
    		bufferTX.putInt(MAC.getMAC().getValue());		// Возвращаемый адрес
    		
    		MAC.sendPacket(AddSurs, bufferTX.array());
    		
    		System.out.println("(ARP) Отправлен ответ устройству: " + AddSurs.toString());
    		break;
    	case 2:
    		System.out.println("(ARP) Запрос на PING от: " + AddSurs.toString());
    		
    		onPing(AddSurs);						// Обновляем список подключенных устройств
    		
    		bufferTX = ByteBuffer.allocate(1+1);
    		Buff.rewind();
    		bufferTX.put(Buff);
    		
    		MAC.sendPacket(AddSurs, bufferTX.array());
    		
    		System.out.println("(ARP) Отправлен ответ устройству: " + AddSurs.toString());    		
    		break;
    	default:
    		System.out.println("(ARP) Пакет не распознан");
    	}
    }
    
    /**
     * Метод вызывается, когда приходит пинг от устройства
     */
    public void onPing(Address Addr) {
        deviceLastSeen.put(Addr, System.currentTimeMillis());
    }
    
    
    /**
     * Возвращает список устройств, которые пинговали нас за последние 10 секунд
     */
    public List<Address> getConnectedDevices() {
        long now = System.currentTimeMillis();
        List<Address> connected = new ArrayList<>();

        for (Map.Entry<Address, Long> entry : deviceLastSeen.entrySet()) {
            if (now - entry.getValue() <= DEVICE_TIMEOUT_MS) {
                connected.add(entry.getKey());
            }
        }

        return connected;
    }
    
    
    /**
     * (необязательно) Удаляет "протухшие" устройства из карты
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        for (Address key : deviceLastSeen.keySet()) {
            if (now - deviceLastSeen.get(key) > DEVICE_TIMEOUT_MS) {
                deviceLastSeen.remove(key);
            }
        }
    }
	

}
