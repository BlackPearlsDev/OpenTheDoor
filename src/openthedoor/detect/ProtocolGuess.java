package openthedoor.detect;

public enum ProtocolGuess {
    EMPTY,
    TLS_HANDSHAKE,
    HTTP,
    WEBSOCKET_HANDSHAKE,
    JSON,
    XMLSOCKET,
    XML,
    MQTT,
    AMF,
    GZIP,
    ZLIB,
    LENGTH_PREFIXED,
    NULL_TERMINATED_TEXT,
    TEXT,
    BINARY
}
