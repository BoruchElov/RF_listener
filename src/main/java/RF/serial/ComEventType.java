package RF.serial;

/** Нормализованный перечень событий COM-порта. */
public enum ComEventType {
    DATA_AVAILABLE,
    DATA_RECEIVED,
    DATA_WRITTEN,
    CTS,
    DSR,
    BREAK_INTERRUPT,
    PARITY_ERROR,
    RING_INDICATOR,
    CARRIER_DETECT,
    PORT_DISCONNECTED,
    UNKNOWN
}
