package pals.base.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.TreeSet;
import pals.base.NodeCore;

/**
 * Miscellaneous utility methods.
 */
public class Misc
{
    /**
     * @param array Array of items.
     * @return Array of unique items; excludes null and empty elements too.
     * The items are also ordered.
     */
    public static String[] arrayStringUnique(String[] array)
    {
        TreeSet<String> buffer = new TreeSet<>();
        for(String s : array)
        {
            if(s != null && s.length() > 0)
                buffer.add(s);
        }
        return buffer.toArray(new String[buffer.size()]);
    }
    /**
     * @param array Array of items.
     * @return Array of items with no empty or null items.
     */
    public static String[] arrayStringNonEmpty(String[] array)
    {
        ArrayList<String> buffer = new ArrayList<>();
        for(String s : array)
            if(s != null && s.length() > 0)
                buffer.add(s);
        return buffer.toArray(new String[buffer.size()]);
    }
    /**
     * Scrambles an array, using the Fisher-Yates shuffle algorithm.
     * 
     * @param <T> The data-type of the array.
     * @param rng An instance of a random number generator.
     * @param array The array.
     */
    public static <T> void arrayShuffle(Random rng, T[] array)
    {
        for(int i = array.length - 1; i >= 1; i--)
            arraySwap(array, i, rng.nextInt(array.length));
    }
    /**
     * Swaps two elements in an array; no index protection.
     * 
     * @param <T> The data-type of the array.
     * @param array The array.
     * @param index1 The index of element one.
     * @param index2 The index of element two.
     */
    public static <T> void arraySwap(T[] array, int index1, int index2)
    {
        T t = array[index1];
        array[index1] = array[index2];
        array[index2] = t;
    }
    /**
     * @param <T> The data-type of the array and item (must be the same).
     * @param array The array to be tested.
     * @param item The item to be found.
     * @return Indicates if the specified item exists in the array.
     */
    public static <T> boolean arrayContains(T[] array, T item)
    {
        for(T t : array)
        {
            if(t.equals(item))
                return true;
        }
        return false;
    }
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
     * @param input The input string to test; can be null.
     * @return True if input consists of alpha-numric chars, false otherwise.
     */
    public static boolean isAlphaNumeric(String input)
    {
        if(input == null)
            return false;
        for(char c : input.toCharArray())
        {
            if(!(c >= 48 && c <= 57) && !(c >= 65 && c <= 90) && !(c >= 97 && c <= 122))
                return false;
        }
        return true;
    }
    /**
     * @param input The input to be tested; can be null.
     * @return Indicates if the input is numeric.
     */
    public static boolean isNumeric(String input)
    {
        if(input == null)
            return false;
        for(char c : input.toCharArray())
            if(c < 48 || c > 57)
                return false;
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
     * @throws ClassNotFoundException Thrown if a class is not found.
     */
    public static Object bytesDeserialize(byte[] data) throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream deserialBais = new ByteArrayInputStream(data);
        ObjectInputStream deserialOis = new ObjectInputStream(deserialBais);
        return deserialOis.readObject();
    }
    /**
     * Deserializes an object from byte-data.
     * 
     * Resolves classes, whilst deserializing, by going to each plugin for a
     * class. This is potentially quite expensive.
     * 
     * @param core The current instance of the core.
     * @param data The byte-array of data to deserialize into an object.
     * @return An instance of the object, or possibly null if it was serialized
     * as null.
     * @throws IOException Thrown if this operation fails.
     * @throws ClassNotFoundException Thrown if a class is not found.
     */
    public static Object bytesDeserialize(NodeCore core, byte[] data) throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream deserialBais = new ByteArrayInputStream(data);
        ObjectInputStream deserialOis = new PluginObjectInputStream(core, deserialBais);
        return deserialOis.readObject();
    }
}
