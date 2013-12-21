package pals.base.web;

/**
 * Allows dynamic relative paths to be easily parsed; performs repetitive tasks
 * such as index and null-pointer protection, as well as parsing to data-types.
 */
public class MultipartUrlParser
{
    // Fields ******************************************************************
    private String[] data;  // Split of folders of relative URL.
    // Methods - Constructors **************************************************
    /**
     * Creates a new parser.
     * 
     * @param data The data for the current request.
     */
    public MultipartUrlParser(WebRequestData data)
    {
        String url = data.getRequestData().getRelativeUrl();
        this.data = (url == null ? "" : url).split("/");
    }
    // Methods - Parsing *******************************************************
    /**
     * Parses an element to an integer.
     * 
     * @param index The index of the directory to parse.
     * @param alt The alternate value to return if the index is invalid.
     * @return The parsed value or the alt parameter.
     */
    public int parseInt(int index, int alt)
    {
        String s = getPart(index);
        if(s == null)
            return alt;
        try
        {
            return Integer.parseInt(s);
        }
        catch(NumberFormatException ex)
        {
            return alt;
        }
    }
    // Methods - Accessors *****************************************************
    /**
     * @param index The index of the directory to retrieve.
     * @return The data at the index; either a string or null (converted to null
     * if empty-string).
     */
    public String getPart(int index)
    {
        return data.length >= index+1 ? (data[index].length() > 0 ? data[index] : null) : null;
    }
}
