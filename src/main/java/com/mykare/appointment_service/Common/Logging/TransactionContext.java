package com.mykare.appointment_service.Common.Logging;

import org.slf4j.MDC;

public final class TransactionContext {

    private static final String TRANSACTION_ID_KEY = "transactionId";

    private TransactionContext() {
    }

    public static String getTransactionId() {
        String transactionId = MDC.get(TRANSACTION_ID_KEY);

        return transactionId == null
                ? "N/A"
                : transactionId;
    }
}