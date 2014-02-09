package pals.javasandbox.parsers;

import pals.javasandbox.ParsedArgument;

/**
 * Parses a string argument.
 */
public class PA_String implements Parser
{
    @Override
    public void parse(ParsedArgument pa, String[] value, boolean array)
    {
        pa.setArgClass(array ? String[].class : String.class);
        if(array)
            pa.setArgValue(value);
        else
            pa.setArgValue(value[0]);
    }
}
