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
package pals.base;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests {@link Settings}.
 * 
 * @version 1.0
 */
public class SettingsTest
{
    private final String EXAMPLE_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
"<settings>\n" +
"<item datatype=\"0\" path=\"hello/world\"><![CDATA[hello world!]]></item>\n" +
"<item datatype=\"2\" path=\"test/int\"><![CDATA[12345]]></item>\n" +
"<item datatype=\"4\" path=\"test/float\"><![CDATA[123.45679]]></item>\n" +
"<item datatype=\"8\" path=\"test/double\"><![CDATA[12345.6789]]></item>\n" +
"<item datatype=\"1\" path=\"test/bool\"><![CDATA[true]]></item>\n" +
"</settings>";
    
    /**
     * Tests the constructor.
     * 
     * @since 1.0
     */
    @Test
    public void testConstructor()
    {
        Settings s;
        
        s= new Settings(true);
        assertTrue(s.isReadOnly());
        
        s = new Settings(false);
        assertFalse(s.isReadOnly());
    }
    /**
     * Tests loading XML.
     * 
     * @since 1.0
     */
    @Test
    public void testLoadXML()
    {
        Settings s;
        
        try
        {
            s = Settings.loadXml(EXAMPLE_XML, true);
            assertTrue(s.isReadOnly());
        }
        catch(SettingsException ex)
        {
            fail("Failed to load XML.");
        }
        
        try
        {
            s = Settings.loadXml(EXAMPLE_XML, false);
            assertFalse(s.isReadOnly());
        }
        catch(SettingsException ex)
        {
        }
        
        try
        {
            s = Settings.loadXml(null, false);
            fail("Loadded null data without exception.");
        }
        catch(SettingsException ex)
        {
            assertTrue(true); // Jenkins purposes
        }
        
        try
        {
            s = Settings.loadXml("", false);
            fail("Loadded null data without exception.");
        }
        catch(SettingsException ex)
        {
            assertTrue(true); // Jenkins purposes
        }
    }
    /**
     * Tests compiling/building settings.
     * 
     * @since 1.0
     */
    @Test
    public void testSaveXML()
    {
        Settings s;
        
        try
        {
            s = Settings.loadXml(EXAMPLE_XML, true);
            assertTrue(s.isReadOnly());
            
            String data = s.save();
            assertNotNull(data);
            assertTrue(data.length() > 0);
        }
        catch(SettingsException ex)
        {
            fail("Failed to load/save XML. - "+ex.getMessage());
        }
    }
    /**
     * Tests the mutators and accessors of setting nodes.
     * 
     * @since 1.0
     */
    @Test
    public void testSetGet()
    {
        Settings s;
        // Load
        try
        {
            s = Settings.loadXml(EXAMPLE_XML, true);
            assertTrue(true);
        }
        catch(SettingsException ex)
        {
            fail("Failed to load settings.");
            return;
        }
        // Test fetching each type
        assertNotNull(s.getStr("hello/world", null));
        assertEquals("hello world!", s.getStr("hello/world", null));
        assertEquals(12345, s.getInt("test/int", 0));
        assertEquals(123.45679f, s.getFloat("test/float", 0.0f), 0.0f);
        assertEquals(12345.6789, s.getDouble("test/double", 0.0), 0.0);
        assertEquals(true, s.getBool("test/bool", false));
        
        // Test replacing node
        assertTrue(s.setString("hello/world", "a"));
        assertEquals("a", s.getStr("hello/world", null));
        
        // Test adding and fetching nodes
        assertTrue(s.setString("unit/str", "a"));
        assertTrue(s.setInt("unit/int", 123));
        assertTrue(s.setFloat("unit/float", 123.4567f));
        assertTrue(s.setDouble("unit/double", 12.3456));
        assertTrue(s.setBool("unit/bool", true));
        
        assertEquals("a", s.getStr("unit/str", null));
        assertEquals(123, s.getInt("unit/int", 0));
        assertEquals(123.4567f, s.getFloat("unit/float", 0.0f), 0.0f);
        assertEquals(12.3456, s.getDouble("unit/double", 0.0), 0.0f);
        assertEquals(true, s.getBool("unit/bool", true));
    }
}
