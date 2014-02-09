package pals.javasandbox.parsers;

import pals.javasandbox.ParsedArgument;

/**
 * Parses a boolean arugment.
 */
public class PA_Boolean implements Parser
{
    @Override
    public void parse(ParsedArgument pa, String[] value, boolean array)
    {
        pa.setArgClass(array ? boolean[].class : boolean.class);
        if(array)
        {
            boolean[] data = new boolean[value.length];
            for(int i = 0; i < value.length; i++)
                data[i] = Boolean.parseBoolean(value[i]);
            pa.setArgValue(data);
        }
        else
            pa.setArgValue(Boolean.parseBoolean(value[0]));
    }
}
