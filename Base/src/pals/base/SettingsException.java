package pals.base;

/**
 * An exception thrown by the Settings class for any errors.
 */
public class SettingsException extends Exception
{
    // Enums
    public enum Type
    {
        /**
         * Indicates the XML from the specified path for the configuration could
         * not be loaded.
         */
        FailedToLoad,
        /**
         * Indicates the XML could not be parsed.
         */
        FailedToParse,
        /**
         * Indicates a setting could not be loaded because of an invalid setting.
         */
        FailedToParse_InvalidSetting,
        /**
         * Indicates a setting with the same path existed.
         */
        FailedToParse_DuplicateSetting,
        /**
         * Indicates a problem occurred building the XML for saving the settings.
         */
        FailedToSave_Build,
        /**
         * Indicates a problem occurred saving the XML, of settings, to file.
         */
        FailedToSave_File,
        /**
         * Indicates the collection is read-only.
         */
        CollectionReadOnly,
        /**
         * Indicates the node attempted to be retrieved is missing/not in the
         * collection.
         */
        MissingNode
    }
    // Fields
    private final Type type;
    // Methods - Constructors
    public SettingsException(Type type, Throwable rootCause)
    {
        super(rootCause);
        this.type = type;
    }
    // Methods - Accessors
    /**
     * @return The type of exception.
     */
    public Type getExceptionType()
    {
        return type;
    }
}
