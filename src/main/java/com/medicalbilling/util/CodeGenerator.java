package com.medicalbilling.util;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class CodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private CodeGenerator() {}

    public static String generateBillNumber() {
        return "BILL-" + LocalDate.now().format(DATE_FMT) + "-" + randomDigits(4);
    }

    public static String generatePurchaseInvoice() {
        return "PUR-" + LocalDate.now().format(DATE_FMT) + "-" + randomDigits(4);
    }

    public static String generateReturnNumber() {
        return "RET-" + LocalDate.now().format(DATE_FMT) + "-" + randomDigits(4);
    }

    public static String generateMedicineCode() {
        return "MED-" + randomDigits(6);
    }

    private static String randomDigits(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
