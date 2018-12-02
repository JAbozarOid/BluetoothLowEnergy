package com.zamanak.bluetoothlowenergy.utilities;

import java.nio.ByteBuffer;
import java.util.Random;

public class DataConverter {
    /**
     * convert bytes to hexadecimal for debugging purposes
     *
     * @param bytes
     * @return Hexadecimal String representation of the byte array
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes.length <=0) return "";
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 3];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = hexArray[v >>> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            hexChars[j * 3 + 2] = 0x20; // space
        }
        return new String(hexChars);
    }

    /**
     * convert bytes to an integer in Little Endian for debugging purposes
     *
     * @param bytes a byte array
     * @return int integer representation of byte array
     */
    public static int bytesToInt(byte[] bytes) {
        if ((bytes.length % 2) == 1) {
            return ByteBuffer.wrap(bytes).getInt();
        } else {
            return 0;
        }
    }

    /**
     * convert bytes to a Float
     * @param bytes
     * @return float
     */
    public static float bytesToFloat(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getFloat();
    }

    /**
     * convert bytes to an integer in Little Endian for debugging purposes
     *
     * @param bytes a byte array
     * @return integer integer representation of byte array
     */
    public static long bytesToLong(byte[] bytes) {
        long val = 0;
        for (int i=0; i < bytes.length; i++) {
            val += ((bytes[i] & 0xff) << (8*i));
        }
        return val;
    }

    /**
     * Generate a random String
     *
     * @param int length of resulting String
     * @return random String
     */
    private static final String ALLOWED_CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyz";
    public static String getRandomString(final int length) {
        final Random random = new Random();
        final StringBuilder sb=new StringBuilder(length);
        for(int i=0;i<length;++i) sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        return sb.toString();
    }
}
