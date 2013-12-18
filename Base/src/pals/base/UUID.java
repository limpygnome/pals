package pals.base;

import java.util.Random;

/**
 * A class for representing a Universal Unique Identifier (UUID) data-type.
 * 
 * Using specification RFC4122, available at:
 * http://www.ietf.org/rfc/rfc4122.txt
 */
public class UUID
{
    // Fields - Static *********************************************************
    private static Random rand = null;
    // Fields ******************************************************************
    private final String data, dataHyphens;    // The UUID as a hexadecimal string without hyphens (32 characters), upper-case.
    private byte[] cacheBytes;
    // Methods - Constructors **************************************************
    private UUID(String data)
    {
        this.cacheBytes = null;
        this.data = (data.length() == 32 ? data : data.replace("-", "")).toUpperCase();
        this.dataHyphens = this.data.substring(0, 8) + "-" + this.data.substring(8, 12) + "-" + this.data.substring(12, 16) + "-" + this.data.substring(16, 20) + "-" + this.data.substring(20, 32);
    }
    // Methods - Static ********************************************************
    /**
     * Attempts to parse a UUID from data.
     * @param data UUID as a hexadecimal string with or without hyphens.
     * @return UUID, if valid, or null.
     */
    public static UUID parse(String data)
    {
        return isValid(data) ? new UUID(data) : null;
    }
    public static UUID parse(byte[] data)
    {
        // Check byte array is valid
        if(data == null || data.length != 16)
            return null;
        // Iterate each byte and produce UUID
        char[] uuid = new char[32];
        long c1, c2;
        long bdata;
        int offsetUuid = 0;
        for(int i = 0; i < 16; i++)
        {
            bdata = data[i] & 0xFF; // Converts the signed byte to an unsigned number; feels and looks hacky, but it works correctly.
            uuid[offsetUuid] = getCharValue(bdata >>> 4);       // Shift down four places
            uuid[offsetUuid+1] = getCharValue(bdata & 0x0F);    // Get only the first four bits
            offsetUuid += 2;
        }
        // Parse as a normal string UUID
        return parse(new String(uuid));
    }
    /**
     * Indicates if the data is a valid UUID.
     * @param data A hexadecimal string representation of a UUID; can have
     * hyphens.
     * @return True if valid, false if invalid.
     */
    public static boolean isValid(String data)
    {
        // Single capture group, start (^) to end ($) of string; capture group matches
        // either 8-8-4-4-12 (32 characters + 6 hyphens) characters seoarated by
        // hyphens or 32 hexadecimal characters.
        return data != null && (data.length() == 32 || data.length() == 36) && data.matches("^(([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})|([0-9a-fA-F]{32}))$");
    }
    /**
     * Generates a version 4 UUID, which is generated using pseudo-random
     * numbers (Java's Random class, seeded with System.currentTimeMillis()).
     * @return Version 4 UUID.
     */
    public static UUID generateVersion4()
    {
        // Check the RNG has been setup
        if(rand == null)
            rand = new Random(System.currentTimeMillis()); // Seeded with time
        // Generate V4 random UUID
        char[] data = new char[32];
        for(int i = 0; i < 32; i++)
        {
            switch(i)
            {
                case 12: // bit 12 must be 4
                    data[i] = '4';
                    break;
                case 16: // bit 16 must be either 8, 9, A or B
                    data[i] = (char)getCharValue(8 + rand.nextInt(4));
                    break;
                default: // any hexadecimal number, randomly selected
                    data[i] = (char)getCharValue(rand.nextInt(16));
                    break;
            }
        }
        return new UUID(new String(data));
    }
    // Methods - Accessors *****************************************************
    /**
     * Gets the string hexadecimal representation of the UUID.
     * @return UUID as string, upper-case.
     */
    public String getHex()
    {
        return data;
    }
    /**
     * Gets the string hexadecimal representation of the UUID, with hyphens.
     * @return UUID as string, with hyphens and upper-case.
     */
    public String getHexHyphens()
    {
        return dataHyphens;
    }
    /**
     * Gets the UUID as a series of bytes; note: this is recompiled each time,
     * therefore it should be cached when used multiple times (expensive).
     * @return Byte-array of the UUID, consisting of sixteen bytes.
     */
    public byte[] getBytes()
    {
        if(cacheBytes == null)
        {
            cacheBytes = new byte[16];
            // 1 2 4 8 - we can produce a hex character with just four bits (2^4=16),
            // thus we can fit two characters per byte - efficient storage!
            int index;
            long t;
            for(int i = 0; i < 31; i+=2)
            {
                index = i/2;
                t = (getHexValue(data.charAt(i)) << 4) | getHexValue(data.charAt(i+1));
                cacheBytes[index] = (byte)t;
            }
        }
        return cacheBytes;
    }
    private static long getHexValue(long hexChar)
    {
        int c = (int)hexChar;
        // 0-9 = ascii 48 to 57, A-F = ascii 65 to 70
        // -- 0 starts at 0, so subtract 48
        // -- A starts at 10, so subtract 55
        return c > 57 ? c-55 : c - 48;
    }
    private static char getCharValue(long intChar)
    {
        // -- refer to getHexValue on why we use 55 and 48; should be pretty obvious.
        return (char)(intChar > 9 ? intChar+55 : intChar+48);
    }
    // Methods - Overrides *****************************************************
    @Override
    public boolean equals(Object o)
    {
        return o instanceof UUID && o != null && ((UUID)o).data.equals(data);
    }
    @Override
    public int hashCode()
    {
        // A very simple hash-code solution: simply add all the bytes and allow
        // overflowing to occur
        int hashval = 0;
        for(char c : data.toCharArray())
            hashval += c;
        return hashval;
    }
    
}
