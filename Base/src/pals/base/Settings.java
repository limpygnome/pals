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
 * A thread-safe collection which holds key/value settings data, which can be
 * loaded/saved to file.
 */
public class Settings
{
    // Constants ***************************************************************
    /**
     * The root XML element name in settings files.
     */
    private static final String SETTINGS_XML_TOPNODE = "settings";
    /**
     * The XML element name of settings in a settings file.
     */
    private static final String SETTINGS_XML_NODE_NAME = "item";
    /**
     * The name of the attribute for storing the path.
     */
    private static final String SETTINGS_XML_NODE__PATH = "path";
    /**
     * The name of the attribute for storing the data-type.
     */
    private static final String SETTINGS_XML_NODE__DATATYPE = "datatype";
    // Fields ******************************************************************
    private final boolean                       readOnly;       // Indicates if the collection is read-only.
    private final HashMap<String,SettingsNode>  settings;       // Path,node with settings data.
    // Methods - Constructors **************************************************
    /**
     * Creates a new instance of a settings store.
     * @param readOnly Indicates if the collection is read-only; if true, the
     * collection cannot be saved.
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
     * @throws SettingsException 
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
     * @throws SettingsException 
     */
    public static Settings loadXml(String xml, boolean readOnly) throws SettingsException
    {
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
                        dt = SettingsNode.DataType.getType(Integer.parseInt(e.getAttribute(SETTINGS_XML_NODE__DATATYPE)));
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
     */
    public void save(String path) throws SettingsException
    {
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
    public String save() throws SettingsException
    {
        // Lock the collection
        synchronized(settings)
        {
            // Check the collection is not read-only
            if(readOnly)
                throw new SettingsException(SettingsException.Type.CollectionReadOnly, new Exception("The settings collection is read-only and cannot be modified!"));
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
     * @return Indicates if the collection is read-only.
     */
    public boolean isReadOnly()
    {
        return readOnly;
    }
    /**
     * Fetches a settings node.
     * @param path The path of the node.
     * @return Node data or null.
     */
    public SettingsNode getNode(String path)
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
     */
    public <T> T get(String path)
    {
        synchronized(settings)
        {
            return settings.containsKey(path) ? (T)settings.get(path).get() : null;
        }
    }
    /**
     * WARNING: this is not thread-safe!
     * @return EntrySet for iterating the settings.
     */
    public Set<Map.Entry<String,SettingsNode>> getRaw()
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
     */
    public boolean set(String path, SettingsNode.DataType dataType, Object value)
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
     * Note: empty strings can become null when reloaded!
     * @param path The path of the setting.
     * @param value The value of the setting.
     * @return True if set, false if failed.
     */
    public boolean setString(String path, String value)
    {
        return set(path, SettingsNode.DataType.String, value);
    }
    /**
     * @param path The path of the setting.
     * @param value The value of the setting.
     * @return True if set, false if failed.
     */
    public boolean setBool(String path, Boolean value)
    {
        return set(path, SettingsNode.DataType.Boolean, value);
    }
    /**
     * @param path The path of the setting.
     * @param value The value of the setting.
     * @return True if set, false if failed.
     */
    public boolean setInt(String path, Integer value)
    {
        return set(path, SettingsNode.DataType.Integer, value);
    }
    /**
     * @param path The path of the setting.
     * @param value The value of the setting.
     * @return True if set, false if failed.
     */
    public boolean setFloat(String path, Float value)
    {
        return set(path, SettingsNode.DataType.Float, value);
    }
    /**
     * @param path The path of the setting.
     * @param value The value of the setting.
     * @return True if set, false if failed.
     */
    public boolean setDouble(String path, Double value)
    {
        return set(path, SettingsNode.DataType.Double, value);
    }
}
