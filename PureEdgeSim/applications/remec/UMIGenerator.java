package applications.remec;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

public class UMIGenerator {
    // Atomic long to keep track of the message identifier (54-bit counter)
    private final AtomicLong messageCounter = new AtomicLong(0);

    // Data Center ID (10 bits) - This should be unique for each DC

    // Constructor to initialize the DC ID
    public UMIGenerator() {
    }

    // Generate a unique message ID (UMI)
    public String generateUMI(int dcId, long userId, long messageId) {
        // Check that dcId is within 10 bits
        if (dcId < 0 || dcId >= (1 << 10)) {
            throw new IllegalArgumentException("DC ID must be between 0 and 1023 (10 bits).");
        }

        // Check that userId is within 64 bits
        if (userId < 0 || userId >= Long.MAX_VALUE) {
            throw new IllegalArgumentException("User ID must be between 0 and 2^64-1 (64 bits). ");
        }

        // Ensure messageId is within 54 bits
        if (messageId < 0 || messageId >= (1L << 54)) {
            throw new IllegalArgumentException("Message ID must be between 0 and 2^54-1 (54 bits).");
        }

        // Combine parts into a single 128-bit UMI
        // Part 1: 10 bits for dcId and the most significant 54 bits of userId
        long part1 = ((long) dcId << 54) | (userId >>> 10);

        // Part 2: 10 least significant bits of userId and 54 bits of messageId
        long part2 = ((userId & 0x3FFL) << 54) | messageId;

        // Convert to byte array
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(part1);
        buffer.putLong(part2);

        return bytesToHex(buffer.array());
    }


    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public UMI decodeUMI(String umiHex) {
        byte[] umiBytes = hexToBytes(umiHex);
        ByteBuffer buffer = ByteBuffer.wrap(umiBytes);

        long part1 = buffer.getLong();
        long part2 = buffer.getLong();

        // Extract parts
        int dcId = (int) (part1 >>> 54);
        long userId = ((part1 & ((1L << 54) - 1)) << 10) | (part2 >>> 54);
        long messageId = part2 & ((1L << 54) - 1);

        System.out.println("DC ID: " + dcId);
        System.out.println("User ID: " + userId);
        System.out.println("Message ID: " + messageId);
        return new UMI(dcId, userId, messageId);
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}