package RF;

import java.nio.ByteBuffer;

public interface PacketHandler {
	void handlePacket(Address AddSurs, ByteBuffer Buff);
}

