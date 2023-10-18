package com.aaronicsubstances.kabomu.exceptions;

public class QuasiHttpException extends KabomuException {

    /**
     * Indicates general error without much detail to offer aside inspecting 
     * error messages and inner exceptions.
     */
    public static final int REASON_CODE_GENERAL = 1;

    /**
     * Indicates a timeout in processing.
     */
    public static final int REASON_CODE_TIMEOUT = 2;

    /**
     * Indicates a problem with encoding/decoding headers.
     */
    public static final int REASON_CODE_PROTOCOL_VIOLATION = 3;

    /**
     * Indicates a problem with exceeding header or body size limits.
     */
    public static final int REASON_CODE_MESSAGE_LENGTH_LIMIT_EXCEEDED = 4;
    
    // the following codes are reserved for future use.
    private static final int reasonCodeReserved5 = 5;
    private static final int reasonCodeReserved6 = 6;
    private static final int reasonCodeReserved7 = 7;
    private static final int reasonCodeReserved8 = 8;
    private static final int reasonCodeReserved9 = 9;
    private static final int reasonCodeReserved0 = 0;

    private final int reasonCode;

    public QuasiHttpException (String message) {
        this(message, REASON_CODE_GENERAL, null);
    }

    public QuasiHttpException (String message, int reasonCode) {
        this(message, reasonCode, null);
    }

    public QuasiHttpException (String message, int reasonCode, Throwable cause) {
        super(message, cause);
        switch (reasonCode)
            {
                case reasonCodeReserved5:
                case reasonCodeReserved6:
                case reasonCodeReserved7:
                case reasonCodeReserved8:
                case reasonCodeReserved9:
                case reasonCodeReserved0:
                    throw new IllegalArgumentException("cannot use reserved reason code: " + reasonCode);
                default:
                    break;
            }
            this.reasonCode = reasonCode;
    }

    public int getReasonCode() {
        return reasonCode;
    }
}
