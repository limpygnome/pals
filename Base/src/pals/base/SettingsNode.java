package pals.base;

/**
 * Represents a setting, in a Settings collection.
 */
public class SettingsNode
{
    // Enums *******************************************************************
    /**
     * The data-type of the setting.
     */
    public enum DataType
    {
        String(0),
        Boolean(1),
        Integer(2),
        Float(4),
        Double(8);
        
        private final int dataType;
        private DataType(int dataType)
        {
            this.dataType = dataType;
        }
        /**
         * @return The data-type's value.
         */
        public int getDataType()
        {
            return dataType;
        }
        /**
         * @param type The type to retrieve.
         * @return The enum of the data-type.
         */
        public static DataType getType(int type)
        {
            switch(type)
            {
                default:
                case 0:
                    return String;
                case 1:
                    return Boolean;
                case 2:
                    return Integer;
                case 4:
                    return Float;
                case 5:
                    return Double;
            }
        }
    }
    // Fields ******************************************************************
    private final String    path;           // The path of the node.
    private Object          data;           // The data of the setting; can be null.
    private DataType        type;           // The data-type of the setting.
    private final Settings  parent;         // The collection of where the node resides.
    // Methods - Constructors **************************************************
    protected SettingsNode(Settings parent, String path, String rawData, DataType type)
    {
        this.parent = parent;
        this.path = path;
        this.data = parseDataType(rawData, type);
        this.type = type;
    }
    // Methods - Static ********************************************************
    /**
     * Parses raw-data as the specified data-type; this does not throw errors,
     * however invalid data will result in a null reference.
     * @param rawData The raw-data to be transformed.
     * @param type The data-type of the raw-data.
     * @return The raw-data transformed into the specified type.
     */
    public static Object parseDataType(String rawData, DataType type)
    {
        if(rawData == null || rawData.length() == 0)
            return null;
        try
        {
            switch(type)
            {
                case Boolean:
                    return rawData.equals("1") || rawData.toLowerCase().equals("true");
                case Double:
                    return Double.parseDouble(rawData);
                case Float:
                    return Float.parseFloat(rawData);
                case Integer:
                    return Integer.parseInt(rawData);
                default:
                    return rawData;
            }
        }
        catch(NumberFormatException ex)
        {
            return null;
        }
    }
    /**
     * Indicates if the specified path is valid for a node.
     * 
     * Rules:
     * - Can begin with alpha characters or under-scroll.
     * - May then contain alpha-numeric characters, under-scroll, dot, slashes,
     *   and hyphens.
     * - Does not allow double back-slashes.
     * 
     * Examples:
     * hello/world
     * __hello_/world
     * @param path The path to be tested.
     * @return True if valid, false if invalid.
     */
    public static boolean validNodePath(String path)
    {
        // ^([a-zA-Z\_]+)(?:[a-zA-Z0-9\_\.\-\/]+)?$ ~ unescaped regex format
        return path.matches("^([a-zA-Z\\_]+)(?:[a-zA-Z0-9\\_\\.\\-\\/]+)?$") && !path.matches("//");
    }
    // Methods - Accessors *****************************************************
    /**
     * Fetches the data as a type.
     * 
     * WARNING: this does not convert the data, it simply casts the data as a
     * type, which is held as an Object. Use this method with care!
     * 
     * @param <T> The type of the data.
     * @return The data as the specified type.
     */
    public <T> T get()
    {
        return (T)getData();
    }
    /**
     * @return The path of this node.
     */
    public String getPath()
    {
        return path;
    }
    /**
     * @return Gets the raw data.
     */
    public Object getData()
    {
        return data;
    }
    /**
     * Note: if the collection is read-only, this will have no effect.
     * @param data Sets the raw data.
     * @param type Sets the type of the raw-data.
     */
    public void setData(Object data, DataType type)
    {
        this.data = data;
        this.type = type;
    }
    /**
     * @return Gets the data-type.
     */
    public DataType getType()
    {
        return type;
    }
}
