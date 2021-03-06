/*
    The MIT License (MIT)

    Copyright (c) 2014 Marcus Craske <limpygnome@gmail.com>

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
    ----------------------------------------------------------------------------
    Authors:    Marcus Craske           <limpygnome@gmail.com>
    ----------------------------------------------------------------------------
*/
package pals.base.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Random;
import java.util.TreeSet;
import org.joda.time.DateTime;
import org.joda.time.Period;
import pals.base.NodeCore;
import pals.base.database.Connector;
import pals.base.database.DatabaseException;

/**
 * Miscellaneous utility methods.
 * 
 * @version 1.0
 */
public class Misc
{
    /**
     * Takes an array of items and returns the unique items. Also excludes
     * null or empty items.
     * 
     * @param array Array of items.
     * @return Array of filtered items; can be empty.
     * @since 1.0
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
     * Takes an array of items and returns only items which are not null
     * and not empty.
     * 
     * @param array Array of items.
     * @return Array of filtered items; can be empty.
     * @since 1.0
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
     * @since 1.0
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
     * @since 1.0
     */
    public static <T> void arraySwap(T[] array, int index1, int index2)
    {
        T t = array[index1];
        array[index1] = array[index2];
        array[index2] = t;
    }
    /**
     * Indicates if an array contains a type, by using
     * {@link Object#equals(java.lang.Object)} for comparison.
     * 
     * @param <T> The data-type of the array and item (must be the same).
     * @param array The array to be tested.
     * @param item The item to be found.
     * @return Indicates if the specified item exists in the array.
     * @since 1.0
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
     * Merges two arrays.
     * 
     * @param <T> The type of the array.
     * @param t The class of the type.
     * @param arr1 Array to be merged.
     * @param arr2 Array to be merged.
     * @return The two arrays merged.
     * @since 1.0
     */
    public static <T> T[] arrayMerge(Class t, T[] arr1, T[] arr2)
    {
        T[] buffer = (T[])Array.newInstance(t, arr1.length+arr2.length);
        int i;
        for(i = 0; i < arr1.length; i++)
            buffer[i] = arr1[i];
        for(i = 0; i < arr2.length; i++)
            buffer[i+arr1.length] = arr2[i];
        return buffer;
    }
    /**
     * Generates a random alpha-numeric string.
     * 
     * @param core Used to retrieve an RNG.
     * @param length The length of the string to generate.
     * @return Alpha-numeric random-character string.
     * @since 1.0
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
     * Tests if the input contains only alpha-numeric characters.
     * 
     * @param input The input string to test; can be null.
     * @return True if input consists of alpha-numric chars, false otherwise.
     * @since 1.0
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
     * Tests if the input contains numeric characters.
     * 
     * @param input The input to be tested; can be null.
     * @return Indicates if the input is numeric.
     * @since 1.0
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
     * @since 1.0
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
     * @since 1.0
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
     * @since 1.0
     */
    public static Object bytesDeserialize(NodeCore core, byte[] data) throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream deserialBais = new ByteArrayInputStream(data);
        ObjectInputStream deserialOis = new PluginObjectInputStream(core, deserialBais);
        return deserialOis.readObject();
    }
    /**
     * Creates a more human-readable date-time representation, which returns
     * a string saying e.g. x days ago.
     * 
     * @param dt The date-time to compare to present.
     * @return The human date-time.
     * @since 1.0
     */
    public static String humanDateTime(DateTime dt)
    {
        Period p = new Period(dt, DateTime.now());
        int years = p.getYears();
        int months = p.getMonths();
        int days = p.getDays();
        int hours = p.getHours();
        int mins = p.getMinutes();
        int secs = p.getSeconds();
        if(years == 0)
        {
            if(months > 0)
                return months+" month"+(months!=1?"s":"")+" ago";
            else if(days > 0)
                return days+" day"+(days!=1?"s":"")+" ago";
            else if(hours > 0)
                return hours+" hour"+(hours!=1?"s":"")+" ago";
            else if(mins > 0)
                return mins+" minute"+(mins!=1?"s":"")+" ago";
            else if(secs > 0)
                return secs+" second"+(secs!=1?"s":"")+" ago";
        }
        return dt.toString("dd/MM/YYYY HH:mm:ss");
    }
    /**
     * Counts the occurrences of a character in a string.
     * 
     * @param data The data to be checked.
     * @param charMatch The character to match, inside the data.
     * @return The number of times the charMatch occurs inside data.
     * @since 1.0
     */
    public static int countOccurrences(String data, char charMatch)       
    {
        int counter = 0;
        for(char c : data.toCharArray())
            if(c == charMatch)
                counter++;
        return counter;
    }
    /**
     * Parses an integer and uses the alternate value provided if the data
     * cannot be parsed.
     * 
     * @param data The data to be parsed.
     * @param alternate The alternate value.
     * @return The parsed, or alternate, integer.
     * @since 1.0
     */
    public static int parseInt(String data, int alternate)
    {
        try
        {
            return Integer.parseInt(data);
        }
        catch(NumberFormatException ex)
        {
            return alternate;
        }
    }
    /**
     * Executes SQL from a file.
     * 
     * @param file The file with the SQL, in plain-text.
     * @param conn Database connector.
     * @throws FileNotFoundException Thrown if the file is not found.
     * @throws IOException Thrown if an issue occurs reading the file.
     * @throws DatabaseException Thrown if an issue occurs executing the SQL.
     * @since 1.0
     */
    public static void executeSqlFile(File file, Connector conn) throws FileNotFoundException, IOException, DatabaseException
    {
        // Fetch SQL
        String sql = Files.fileRead(new FileInputStream(file));
        // Execute SQL
        conn.execute(sql);
    }
    /**
     * Generates a value between the two numbers. Supports negative values.
     * 
     * @param rng The random number generator used.
     * @param a Start of range. Can be min or max.
     * @param b End of range. Can be min or max.
     * @return Random value using specified RNG between specified range.
     * @since 1.0
     */
    public static int rngRange(Random rng, int a, int b)
    {
        // Treat a as min, b as max - swap if different
        if(a > b)
        {
            int t = a;
            a = b;
            b = t;
        }
        // Generate value
        return rng.nextInt(b-a+1)+a;
    }
}
