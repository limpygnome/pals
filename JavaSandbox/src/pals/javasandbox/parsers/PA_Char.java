package pals.javasandbox.parsers;

import pals.javasandbox.ParsedArgument;

/**
 * Parses a character argument.
 */
public class PA_Char implements Parser
{
    @Override
    public void parse(ParsedArgument pa, String[] value, boolean array)
    {
        pa.setArgClass(array ? char[].class : char.class);
        if(array)
        {
            char[] data = new char[value.length];
            for(int i = 0; i < value.length; i++)
                data[i] = parse(value[i]);
            pa.setArgValue(data);
        }
        else
            pa.setArgValue(parse(value[0]));
    }
    private char parse(String data)
    {
        return data.length() == 0 ? data.charAt(0) : (char)Integer.parseInt(data);
    }
}
