package pals.javasandbox.parsers;

import pals.javasandbox.ParsedArgument;

/**
 * Parses a short argument.
 */
public class PA_Short implements Parser
{
    @Override
    public void parse(ParsedArgument pa, String[] value, boolean array)
    {
        pa.setArgClass(array ? short[].class : short.class);
        if(array)
        {
            short[] data = new short[value.length];
            for(int i = 0; i < value.length; i++)
                data[i] = Short.parseShort(value[i]);
            pa.setArgValue(data);
        }
        else
            pa.setArgValue(Short.parseShort(value[0]));
    }
}
