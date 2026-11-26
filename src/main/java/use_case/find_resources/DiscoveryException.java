package use_case.find_resources;

/** Shared discovery failure used by existing Canvas/MCP adapters. */
public final class DiscoveryException extends Exception {
    public enum Code { INVALID_INPUT, PERMISSION_DENIED, DISCONNECTED, INVALID_RESPONSE, UNAVAILABLE }
    private final Code code;

    public DiscoveryException(Code code) {
        super(code.name());
        this.code = code;
    }

    public Code getCode() { return code; }
}
