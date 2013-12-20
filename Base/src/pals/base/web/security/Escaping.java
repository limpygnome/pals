package pals.base.web.security;

/**
 * Used for escaping encoding/decoding.
 */
public class Escaping
{
    /**
     * Encodes a HTML string.
     * 
     * @param value The string to be escaped; can be null (will become empty
     * string).
     * @return The escaped string.
     */
    public static String htmlEncode(String value)
    {
        return value == null ? "" : value.replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;");
    }
}
