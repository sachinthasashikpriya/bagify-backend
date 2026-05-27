package com.mycompany.app.order.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Cryptography utility class to generate and validate PayHere security hash signatures (MD5).
 * All MD5 hashes calculated must be returned as uppercase hexadecimal strings.
 */
public class PayHereSignatureGenerator {

    /**
     * Generates the initial checkout hash required in the payment payload.
     * Formula: MD5(merchant_id + order_id + amount + currency + MD5(merchant_secret)) in UPPERCASE.
     */
    public static String generateCheckoutHash(
            String merchantId,
            String orderId,
            String amount,
            String currency,
            String merchantSecret) {

        String merchantSecretMd5 = getMd5Uppercase(merchantSecret);
        String combinedString = merchantId + orderId + amount + currency + merchantSecretMd5;
        
        return getMd5Uppercase(combinedString);
    }

    /**
     * Generates the webhook verification hash to validate the notify request from PayHere.
     * Formula: MD5(merchant_id + order_id + payhere_amount + payhere_currency + status_code + MD5(merchant_secret)) in UPPERCASE.
     */
    public static String generateNotificationHash(
            String merchantId,
            String orderId,
            String amount,
            String currency,
            String statusCode,
            String merchantSecret) {

        String merchantSecretMd5 = getMd5Uppercase(merchantSecret);
        String combinedString = merchantId + orderId + amount + currency + statusCode + merchantSecretMd5;

        return getMd5Uppercase(combinedString);
    }

    /**
     * Computes the MD5 checksum of an input string and returns it as an uppercase hex string.
     */
    private static String getMd5Uppercase(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02X", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 digest algorithm is not available in the environment", e);
        }
    }
}
