package RF;

import java.nio.ByteBuffer;
import java.util.Objects;

public class Address {
    private int value; // 4 байта

    public Address(int value) {
        this.value = value;
    }

    public Address(byte[] bytes, int offset) {
        this.value = ByteBuffer.wrap(bytes, offset, 4).getInt();
    }

    public void writeTo(byte[] dest, int offset) {
        ByteBuffer.wrap(dest, offset, 4).putInt(value);
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    // Для удобства: вывод как IP-адрес
    @Override
    public String toString() {
        return String.format("%d.%d.%d.%d",
                (value >> 24) & 0xFF,
                (value >> 16) & 0xFF,
                (value >> 8) & 0xFF,
                value & 0xFF);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Address)) return false;
        Address address = (Address) o;
        return value == address.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}

