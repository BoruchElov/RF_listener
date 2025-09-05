package RF;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Inverters implements PacketHandler {

	private cMAC MAC;
	private final ConcurrentHashMap<Address, CompletableFuture<ByteBuffer>> pendingResponses = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	Inverters(cMAC mac) {
        this.MAC = mac;
    }
	
	// Команда, задающая новую уставку параметра для системы управления 
	public boolean SetParameter(Address deviceAddr, int Par, float Value) {
		String str = String.format("SET_PARAMETER(%d, %.6f)", Par, Value);
		
		System.out.println("(Inverters) Tx command: " + str);
		
	    // Преобразуем строку в байты (UTF-8, без завершающего \0)
	    byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
	    
	    // Создаём ByteBuffer нужного размера
	    ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
	    buffer.put(bytes); // Копируем байты в буфер
	    buffer.flip();     // Подготовка к чтению, если нужно
	    
	    ByteBuffer RX_massage;
	    byte[] responseBytes;
		try {
			RX_massage = sendRequest(deviceAddr, buffer);
			
		    // Переводим ByteBuffer в строку
		    RX_massage.flip(); // На всякий случай — подготовка к чтению
		    responseBytes = new byte[RX_massage.remaining()];
		    RX_massage.get(responseBytes);
		    
		    String response = new String(responseBytes, StandardCharsets.UTF_8);

		    // Сравниваем с ожидаемым ответом
		    if ("SET_PARAMETER_OK()".equals(response)) {
		        System.out.println("(Inverters) Параметр успешно установлен.");
		        return true;
		    } else {
		        System.err.println("(Inverters) Ошибка: неожиданный ответ от устройства: " + response);
		        return false;
		    }
		    
		} catch (TimeoutException e) {
			System.out.println("(Inverters) Время ожидания истекло!");
			return false;
		} catch (Exception e) {
		    System.out.println("(Inverters) Ошибка при отправке запроса: " + e.getMessage());
		    e.printStackTrace();
		    return false;
		}
	}
	
	// Команда, задающая новую уставку параметра для системы управления 
	public boolean GetParameter(Address deviceAddr, float[] result) {
	    if (result == null || result.length < 20) {
	        throw new IllegalArgumentException("Массив result должен иметь длину минимум 20");
	    }

	    String str = "GET_PARAMETERS()";
	    System.out.println("(Inverters) Tx command: " + str);

	    byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
	    ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
	    buffer.put(bytes);
	    buffer.flip();

	    try {
	        ByteBuffer RX_massage = sendRequest(deviceAddr, buffer);

	        RX_massage.flip();
	        byte[] responseBytes = new byte[RX_massage.remaining()];
	        RX_massage.get(responseBytes);

	        String response = new String(responseBytes, StandardCharsets.UTF_8).trim();
	        System.out.println("(Inverters) Rx response: " + response);

	        // Проверка начала строки
	        final String prefix = "PARAMETERS(";
	        if (!response.startsWith(prefix) || !response.endsWith(")")) {
	            System.err.println("(Inverters) Неверный формат ответа: " + response);
	            return false;
	        }

	        // Извлекаем содержимое между скобками
	        String content = response.substring(prefix.length(), response.length() - 1);
	        String[] parts = content.split(",");

	        if (parts.length != 20) {
	            System.err.println("(Inverters) Ожидалось 20 параметров, получено: " + parts.length);
	            return false;
	        }

	        for (int i = 0; i < 20; i++) {
	            result[i] = Float.parseFloat(parts[i].trim());
	        }

	        return true;

	    } catch (TimeoutException e) {
	        System.out.println("(Inverters) Время ожидания истекло!");
	        return false;
	    } catch (Exception e) {
	        System.err.println("(Inverters) Ошибка: " + e.getMessage());
	        e.printStackTrace();
	        return false;
	    }
	}
	
	// Команда, Индикация инвертора
	public boolean BlinkLedStart(Address deviceAddr, boolean executedOnDevice) {
	    String str = "BLINK_LED_START()";
	    System.out.println("(Inverters) Tx command: " + str);

	    byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
	    ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
	    buffer.put(bytes);
	    buffer.flip();

	    try {
	        ByteBuffer RX_massage = sendRequest(deviceAddr, buffer);

	        RX_massage.flip();
	        byte[] responseBytes = new byte[RX_massage.remaining()];
	        RX_massage.get(responseBytes);

	        String response = new String(responseBytes, StandardCharsets.UTF_8).trim();
	        System.out.println("(Inverters) Rx response: " + response);

	        // Передаем ответ через параметр
	        if ("BLINK_LED_START_OK()".equals(response)) {
	        	executedOnDevice = true;
	            return true;  // вызов прошёл успешно и устройство подтвердило
	        } 
	        else if ("BLINK_LED_START_ERROR()".equals(response)) {
	            executedOnDevice = false;
	            return true;  // вызов прошёл успешно, но устройство ответило ошибкой
	        }
	        else {
	        	return false;
	        }

	    } catch (TimeoutException e) {
	        System.out.println("(Inverters) Время ожидания истекло!");
	        return false;
	    } catch (Exception e) {
	        System.err.println("(Inverters) Ошибка: " + e.getMessage());
	        e.printStackTrace();
	        return false;
	    }
	}
	

	// Команда, Стоп индикации инвертора
	public boolean BlinkLedStop(Address deviceAddr, boolean executedOnDevice) {
	    String str = "BLINK_LED_STOP()";
	    System.out.println("(Inverters) Tx command: " + str);

	    byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
	    ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
	    buffer.put(bytes);
	    buffer.flip();

	    try {
	        ByteBuffer RX_massage = sendRequest(deviceAddr, buffer);

	        RX_massage.flip();
	        byte[] responseBytes = new byte[RX_massage.remaining()];
	        RX_massage.get(responseBytes);

	        String response = new String(responseBytes, StandardCharsets.UTF_8).trim();
	        System.out.println("(Inverters) Rx response: " + response);

	        // Передаем ответ через параметр
	        if ("BLINK_LED_STOP_OK()".equals(response)) {
	        	executedOnDevice = true;
	            return true;  // вызов прошёл успешно и устройство подтвердило
	        } 
	        else if ("BLINK_LED_STOP_ERROR()".equals(response)) {
	            executedOnDevice = false;
	            return true;  // вызов прошёл успешно, но устройство ответило ошибкой
	        }
	        else {
	        	return false;
	        }

	    } catch (TimeoutException e) {
	        System.out.println("(Inverters) Время ожидания истекло!");
	        return false;
	    } catch (Exception e) {
	        System.err.println("(Inverters) Ошибка: " + e.getMessage());
	        e.printStackTrace();
	        return false;
	    }
	}
	
	public boolean CheckSynch(Address deviceAddr, Integer Synch_Status) {
		return false;
	}
	
	public boolean CheckSwitchPos(Address deviceAddr, Integer Switch_Position) {
		return false;
	}
	
	public boolean MasterTimeReq(Address deviceAddr, Integer Time) {
		return false;
	}
	
	public boolean CheckResistance(Address deviceAddr, boolean ResResult) {
		return false;
	}
	
	public boolean CheckResistanceResultOK(Address deviceAddr, Integer Resistance_Status) {
		return false;
	}
	
	public boolean ScenarioPulseOperationBegin(Address deviceAddr, Integer Time, float Curr, float Phase, Integer Timeout, Integer TypeMes1, Integer TypeCont1, Integer Reserv1, Integer TypeMes2, Integer TypeCont2, Integer Reserv2, boolean ScenarioResult) {
		return false;
	}
	
	public boolean ScenarioCurrentStabizationBegin(Address deviceAddr, Integer Time, float Curr, float Phase, Integer Timeout, Integer TypeMes1, Integer TypeCont1, Integer Reserv1, Integer TypeMes2, Integer TypeCont2, Integer Reserv2,  boolean ScenarioResult) {
		return false;
	}
	
	public boolean ForcedStop(Address deviceAddr, boolean ResResult) {
		return false;
	}
	
	
	
	// Синхронная отправка и ожидание ответа
    private ByteBuffer sendRequest(Address deviceAddr, ByteBuffer requestData) throws Exception {
        CompletableFuture<ByteBuffer> future = new CompletableFuture<>();
        pendingResponses.put(deviceAddr, future);

        MAC.sendPacket(deviceAddr, requestData.array()); // Отправка запроса

        // Устанавливаем таймаут 1с
        scheduler.schedule(() -> {
            if (!future.isDone()) {
            	System.out.println("(Inverters) Ответ не получен за 1с от устройства: " + deviceAddr.toString());
            	
                future.completeExceptionally(new TimeoutException("Ответ от устройства не получен за 1с"));
                pendingResponses.remove(deviceAddr);
            }
        }, 1, TimeUnit.SECONDS);

        try {
            return future.get(); // Ждём ответ или исключение
        } catch (Exception e) {
            throw e;
        }
    }
	
	@Override
	public void handlePacket(Address AddSurs, ByteBuffer Buff) {
        CompletableFuture<ByteBuffer> future = pendingResponses.remove(AddSurs);
        if (future != null) {
            future.complete(Buff); 							// Разблокируем ожидающий поток
        } else {
            System.out.println("(Inverters) Ответ от " + AddSurs + " не ожидался или уже истёк");
        }
	}

}
