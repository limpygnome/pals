package pals.javasandbox.parsers;

import pals.javasandbox.ParsedArgument;

/**
 * Parses a long argument.
 */
public class PA_Long implements Parser
{
    @Override
    public void parse(ParsedArgument pa, String[] value, boolean array)
    {
        pa.setArgClass(array ? long[].class : long.class);
        if(array)
        {
            long[] data = new long[value.length];
            for(int i = 0; i < value.length; i++)
                data[i] = Long.parseLong(value[i]);
            pa.setArgValue(data);
        }
        else
            pa.setArgValue(Long.parseLong(value[0]));
    }
}
