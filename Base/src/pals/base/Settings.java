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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.CharacterData;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import pals.base.utils.Files;

/**
 * A collection which holds key/value settings data, which can be loaded/saved
 * to file.
 * 
 * Thread-safe, with the exception of documented methods.
 * 
 * @since 1.0
 */
public class Settings
{
    // Constants ***************************************************************
    /**
     * The root XML element name in settings files.
     * 
     * @since 1.0
     */
    private static final String SETTINGS_XML_TOPNODE = "settings";
    /**
     * The XML element name of settings in a settings file.
     * 
     * @since 1.0
     */
    private static final String SETTINGS_XML_NODE_NAME = "item";
    /**
     * The name of the attribute for storing the path.
     * 
     * @since 1.0
     */
    private static final String SETTINGS_XML_NODE__PATH = "path";
    /**
     * The name of the attribute for storing the data-type.
     * 
     * @since 1.0
     */
    private static final String SETTINGS_XML_NODE__DATATYPE = "datatype";
    // Fields ******************************************************************
    private final boolean                       readOnly;       // Indicates if the collection is read-only.
    private final HashMap<String,SettingsNode>  settings;       // Path,node with settings data.
    // Methods - Constructors **************************************************
    /**
     * Creates a new instance of a settings store.
     * 
     * @param readOnly Indicates if the collection is read-only; if true, the
     * collection cannot be saved.
     * @since 1.0
     */
    public Settings(boolean readOnly)
    {
        this.readOnly = readOnly;
        this.settings = new HashMap<>();
    }
    // Methods - Static ********************************************************
    /**
     * Loads settings from XML.
     * 
     * @param path The path of the XML file.
     * @param readOnly Indicates if the collection should be read-only.
     * @return An instance of a settings collection, loaded with the settings in
     * the XML.
     * @throws SettingsException Thrown if the settings file cannot be loaded.
     * @since 1.0
     */
    public static Settings load(String path, boolean readOnly) throws SettingsException
    {
        String xml;
        try
        {
            xml = Files.fileRead(path);
        }
        catch(IOException ex)
        {
            throw new SettingsException(SettingsException.Type.FailedToLoad, ex);
        }
        return loadXml(xml, readOnly);
    }
    /**
     * Loads settings from XML.
     * 
     * @param xml The XML, as text, to be parsed.
     * @param readOnly Indicates if the collection should be read-only.
     * @return An instance of a settings collection, loaded with the settings in
     * the XML.
     * @throws SettingsException Thrown if the settings file cannot be loaded.
     * @since 1.0
     */
    public static Settings loadXml(String xml, boolean readOnly) throws SettingsException
    {
        if(xml == null || xml.length() == 0)
            throw new SettingsException(SettingsException.Type.FailedToParse, null);
        try
        {
            // Create new Settings collection
            Settings settings = new Settings(readOnly);
            // Turn XML into a stream for reading
            InputSource is = new InputSource(new StringReader(xml));
            // Parse the XML
            DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder xmlBuilder = xmlFactory.newDocumentBuilder();
            Document xmlDocument = xmlBuilder.parse(is);
            // Normalize XML
            xmlDocument.getDocumentElement().normalize();
            // The top-node is always SETTINGS_XML_TOPNODE; fetch sub-elements and load them
            NodeList nodes = xmlDocument.getDocumentElement().getChildNodes();
            Element e;
            Node n;
            Node cdata;
            SettingsNode.DataType dt;
            String path, data;
            for(int i = 0; i < nodes.getLength(); i++)
            {
                n = nodes.item(i);
                // Check the node is an element
                if(n.getNodeType() == Node.ELEMENT_NODE && (e = (Element)n).getTagName().equals(SETTINGS_XML_NODE_NAME))
                {
                    path = data = null;
                    try
                    {
                        // Parse the data-type
                        dt = SettingsNode.DataType.getType(e.getAttribute(SETTINGS_XML_NODE__DATATYPE));
                        // Parse the path
                        path = e.getAttribute(SETTINGS_XML_NODE__PATH).trim();
                        // Check the path is a valid format
                        if(!SettingsNode.validNodePath(path))
                            throw new SettingsException(SettingsException.Type.FailedToParse_InvalidSetting, new Exception("Path '" + path + "' is an invalid path!"));
                        // Parse the data - allow for CData too!
                        if((cdata = n.getFirstChild()) instanceof CharacterData)
                            data = ((CharacterData)cdata).getData();
                        else
                            data = n.getTextContent();
                        // Check the path does not exist
                        if(settings.settings.containsKey(path))
                            throw new SettingsException(SettingsException.Type.FailedToParse_DuplicateSetting, new Exception("Duplicate setting of '" + path + "' ~ paths must be unique!"));
                        else
                            settings.settings.put(path, new SettingsNode(settings, path, data, dt));
                    }
                    catch(IllegalArgumentException ex)
                    {
                        throw new SettingsException(SettingsException.Type.FailedToLoad, new Exception("Invalid setting with data '" + n.getTextContent() + "' ~ path: '" + (path != null ? path : "[could not parse]") + "'!", ex));
                    }
                }
            }
            return settings;
        }
        catch(IOException | ParserConfigurationException | SAXException ex)
        {
            throw new SettingsException(SettingsException.Type.FailedToParse, ex);
        }
    }
    /**
     * Saves the configuration to file.
     * 
     * Note: if the collection is read-only, an exception will be thrown!
     * @param path The path of where to save the configuration.
     * @throws SettingsException Thrown if an error occurs saving the
     * configuration to file.
     * 
     * @since 1.0
     */
    public synchronized void save(String path) throws SettingsException
    {
        // Check the collection is not read-only
        if(readOnly)
            throw new SettingsException(SettingsException.Type.CollectionReadOnly, new Exception("The settings collection is read-only and cannot be modified!"));
        // Generate the XML
        String xml = save();
        // Attempt to save to the specified path
        try
        {
            Files.fileWrite(path, xml, true);
        }
        catch(IOException ex)
        {
            throw new SettingsException(SettingsException.Type.FailedToSave_File, ex);
        }
    }
    /**
     * Builds the output for saving the settings in XML format.
     * 
     * @return The text output of the saved settings, as XML.
     * @throws SettingsException Thrown if the setting cannot be built.
     * @since 1.0
     */
    public synchronized String save() throws SettingsException
    {
        // Lock the collection
        synchronized(settings)
        {
            // Build the XML
            try
            {
                DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder xmlBuilder = xmlFactory.newDocumentBuilder();
                Document xmlDocument = xmlBuilder.newDocument();
                // Create root node
                Node parent = xmlDocument.appendChild(xmlDocument.createElement(SETTINGS_XML_TOPNODE));
                // Add each setting as a child
                SettingsNode sn;
                Element settingE;
                for(Map.Entry<String,SettingsNode> node : settings.entrySet())
                {
                    sn = node.getValue();
                    settingE = xmlDocument.createElement(SETTINGS_XML_NODE_NAME);
                    settingE.setAttribute(SETTINGS_XML_NODE__PATH, sn.getPath());
                    settingE.setAttribute(SETTINGS_XML_NODE__DATATYPE, String.valueOf(sn.getType().getDataType()));
                    settingE.appendChild(xmlDocument.createCDATASection(sn.getData().toString()));
                    parent.appendChild(settingE);
                }
                // Prepare to transform into text
                TransformerFactory xmlTFactory = TransformerFactory.newInstance();
                Transformer xmlTransformer = xmlTFactory.newTransformer();
                xmlTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
                // Transform the document into a string
                DOMSource source = new DOMSource(xmlDocument);
                StringWriter sw = new StringWriter();
                StreamResult sr = new StreamResult(sw);
                xmlTransformer.transform(source, sr);
                sw.flush(); // Ensure all the data has been written
                return sw.toString();
            }
            catch(DOMException | ParserConfigurationException | TransformerException | TransformerFactoryConfigurationError ex)
            {
                throw new SettingsException(SettingsException.Type.FailedToSave_Build, ex);
            }
        }
    }
    // Methods - Accessors *****************************************************
    /**
     * Indicates if the settings is read-only, and can therefore not be saved.
     * 
     * @return Indicates if the collection is read-only.
     * @since 1.0
     */
    public boolean isReadOnly()
    {
        return readOnly;
    }
    /**
     * Fetches a settings node.
     * 
     * @param path The path of the node.
     * @return Node data or null.
     * @since 1.0
     */
    public synchronized SettingsNode getNode(String path)
    {
        return settings.get(path);
    }
    /**
     * Fetches the data of a node.
     * 
     * WARNING: use this method with caution, it can easily be used dangerously!
     * It does not convert the data-type, it only casts the internal storage of
     * the data-type from an Object.
     * 
     * @param <T> The data-type of the setting; refer to SettingsNode get for
     * more information.
     * @param path The path of the node.
     * @return The data of the node as the specified type; possibly null.
     * @since 1.0
     */
    public synchronized <T> T get(String path)
    {
        return settings.containsKey(path) ? (T)settings.get(path).get() : null;
    }
    /**
     * Refer to get method for documentation; this is similar, but throws a
     * SettingsException if a node is missing.
     * 
     * @param <T> The data-type of the setting; refer to SettingsNode get for
     * more information.
     * @param path The path of the node.
     * @return The data of the node as the specified type; possibly null.
     * @throws SettingsException Thrown if a node at the specified path does not
     * exist.
     * @since 1.0
     */
    public synchronized <T> T get2(String path) throws SettingsException
    {
        synchronized(settings)
        {
            if(!settings.containsKey(path))
                throw new SettingsException(SettingsException.Type.MissingNode, new Exception("Node at '" + path + "' is missing!"));
            return (T)settings.get(path).get();
        }
    }
    /**
     * Fetches the data of a node.
     * 
     * WARNING: this does not convert the data, it simply casts it.
     * Note: refer to get method for full documentation.
     * 
     * @param path The path of the node.
     * @return Data as a string.
     * @since 1.0
     */
    public synchronized String getStr(String path)
    {
        return (String)get(path);
    }
    /**
     * Fetches the data of a node.
     * 
     * WARNING: this does not convert the data, it simply casts it.
     * Note: refer to get method for full documentation.
     * 
     * @param path The path of the node.
     * @param alternative The alternative value if the node is null/does not
     * exist.
     * @return Data as a string.
     * @since 1.0
     */
    public synchronized String getStr(String path, String alternative)
    {
        String obj = get(path);
        return obj == null ? alternative : obj;
    }
    /**
     * Fetches the data of a node.
     * 
     * WARNING: this does not convert the data, it simply casts it.
     * Note: refer to get method for full documentation.
     * 
     * @param path The path of the node.
     * @return Data as a boolean.
     * @since 1.0
     */
    public synchronized boolean getBool(String path)
    {
        return (Boolean)get(path);
    }
    /**
     * Fetches the data of a node.
     * 
     * WARNING: this does not convert the data, it simply casts it.
     * Note: refer to get method for full documentation.
     * 
     * @param path The path of the node.
     * @param alternative The alternative value if the node is null/does not
     * exist.
     * @return Data as a boolean.
     * @since 1.0
     */
    public synchronized boolean getBool(String path, boolean alternative)
    {
        Boolean obj = get(path);
        return obj == null ? alternative : obj;
    }
    /**
     * Fetches the data of a node.
     * 
     * WARNING: this does not convert the data, it simply casts it.
     * Note: refer to get method for full documentation.
     * 
     * @param path The path of the node.
     * @return Data as an integer.
     * @since 1.0
     */
    public synchronized int getInt(String path)
    {
        return (Integer)get(path);
    }
    /**
     * Fetches the data of a node.
     * 
     * WARNING: this does not convert the data, it simply casts it.
     * Note: refer to get method for full documentation.
     * 
     * @param path The path of the node.
     * @param alternative The alternative value if the node is null/does not
     * exist.
     * @return Data as an integer.
     * @since 1.0
     */
    public synchronized int getInt(String path, int alternative)
    {
        Integer obj = get(path);
        return obj == null ? alternative : obj;
    }
    /**
     * Fetches the data of a node.
     * 
     * WARNING: this does not convert the data, it simply casts it.
     * Note: refer to get method for full documentation.
     * 
     * @param path The path of the node.
     * @return Data as a float.
     * @since 1.0
     */
    public synchronized float getFloat(String path)
    {
        return (Float)get(path);
    }
    /**
     * Fetches the data of a node.
     * 
     * WARNING: this does not convert the data, it simply casts it.
     * Note: refer to get method for full documentation.
     * 
     * @param path The path of the node.
     * @param alternative The alternative value if the node is null/does not
     * exist.
     * @return Data as a float.
     * @since 1.0
     */
    public synchronized float getFloat(String path, float alternative)
    {
        Float obj = get(path);
        return obj == null ? alternative : obj;
    }
    /**
     * Fetches the data of a node.
     * 
     * WARNING: this does not convert the data, it simply casts it.
     * Note: refer to get method for full documentation.
     * 
     * @param path The path of the node.
     * @return Data as a double.
     * @since 1.0
     */
    public synchronized double getDouble(String path)
    {
        return (Double)get(path);
    }
    /**
     * Fetches the data of a node.
     * 
     * WARNING: this does not convert the data, it simply casts it.
     * Note: refer to get method for full documentation.
     * 
     * @param path The path of the node.
     * @param alternative The alternative value if the node is null/does not
     * exist.
     * @return Data as a double.
     * @since 1.0
     */
    public synchronized double getDouble(String path, double alternative)
    {
        Double obj = get(path);
        return obj == null ? alternative : obj;
    }
    /**
     * The raw data-structure for the settings.
     * 
     * WARNING: this is not thread-safe!
     * 
     * @return EntrySet for iterating the settings.
     * @since 1.0
     */
    public synchronized Set<Map.Entry<String,SettingsNode>> getRaw()
    {
        return settings.entrySet();
    }
    // Methods - Mutators ******************************************************
    /**
     * Sets the value for a path.
     * 
     * Note: if a node exists at a path, it's updated; otherwise a new node is
     * created.
     * @param path The path of the setting.
     * @param dataType The type of the setting's value.
     * @param value The value of the setting; can be null.
     * @return True if updated/created, false if failed.
     * @since 1.0
     */
    public synchronized boolean set(String path, SettingsNode.DataType dataType, Object value)
    {
        // Lock the collection - thread-safety
        synchronized(settings)
        {
            if(settings.containsKey(path))
                // Update the value
                settings.get(path).setData(value, dataType);
            else if(SettingsNode.validNodePath(path))
                // Create a new node
                settings.put(path, new SettingsNode(this, path, (value != null ?  value.toString() : null), dataType));
            else
                // Invalid path - cannot create node!
                return false;
        }
        return true;
    }
    /**
     * Sets a setting.
     * 
     * @param path The path of the setting.
     * @param value The value of the setting.
     * @return True if set, false if failed.
     * @since 1.0
     */
    public synchronized boolean setString(String path, String value)
    {
        return set(path, SettingsNode.DataType.String, value);
    }
    /**
     * Sets a setting.
     * 
     * @param path The path of the setting.
     * @param value The value of the setting.
     * @return True if set, false if failed.
     * @since 1.0
     */
    public synchronized boolean setBool(String path, Boolean value)
    {
        return set(path, SettingsNode.DataType.Boolean, value);
    }
    /**
     * Sets a setting.
     * 
     * @param path The path of the setting.
     * @param value The value of the setting.
     * @return True if set, false if failed.
     * @since 1.0
     */
    public synchronized boolean setInt(String path, Integer value)
    {
        return set(path, SettingsNode.DataType.Integer, value);
    }
    /**
     * Sets a setting.
     * 
     * @param path The path of the setting.
     * @param value The value of the setting.
     * @return True if set, false if failed.
     * @since 1.0
     */
    public synchronized boolean setFloat(String path, Float value)
    {
        return set(path, SettingsNode.DataType.Float, value);
    }
    /**
     * Sets a setting.
     * 
     * @param path The path of the setting.
     * @param value The value of the setting.
     * @return True if set, false if failed.
     * @since 1.0
     */
    public synchronized boolean setDouble(String path, Double value)
    {
        return set(path, SettingsNode.DataType.Double, value);
    }
}
