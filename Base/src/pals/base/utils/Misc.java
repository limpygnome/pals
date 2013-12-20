package pals.base.utils;

import java.util.Random;
import pals.base.NodeCore;

/**
 * Miscellaneous utility methods.
 */
public class Misc
{
    /**
     * Generates a random alpha-numeric string.
     * 
     * @param core Used to retrieve an RNG.
     * @param length The length of the string to generate.
     * @return Alpha-numeric random-character string.
     */
    public static String randomText(NodeCore core, int length)
    {
        final String alphanum = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890";
        Random rng = core.getRNG();
        char[] buffer = new char[length];
        for(int i = 0; i < length; i++)
            buffer[i] = alphanum.charAt(rng.nextInt(alphanum.length()));
        return new String(buffer);
    }
    /**
     * @param input The input string to test.
     * @return True if input consists of alpha-numric chars, false otherwise.
     */
    public static boolean isAlphaNumeric(String input)
    {
        for(char c : input.toCharArray())
        {
            if(!(c >= 48 && c <= 57) && !(c >= 65 && c <= 90) && !(c >= 97 && c <= 122))
                return false;
        }
        return true;
    }
}
