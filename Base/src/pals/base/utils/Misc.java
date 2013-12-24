package pals.base.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
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
    /**
     * Serializes an object.
     * 
     * @param obj The object to be serialized; can be null. This must extend
     * Serializable!
     * @return Byte-array of the serialized data; data is returned for null,
     * which can be converted back to null.
     * @throws IOException Thrown if this operation fails.
     */
    public static byte[] bytesSerialize(Object obj) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.flush();
        return baos.toByteArray();
    }
    /**
     * Deserializes an object from byte-data.
     * 
     * @param data The byte-array of data to deserialize into an object.
     * @return An instance of the object, or possibly null if it was serialized
     * as null.
     * @throws IOException Thrown if this operation fails.
     */
    public static Object bytesDeserialize(byte[] data) throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream deserialBais = new ByteArrayInputStream(data);
        ObjectInputStream deserialOis = new ObjectInputStream(deserialBais);
        return deserialOis.readObject();
    }
}
