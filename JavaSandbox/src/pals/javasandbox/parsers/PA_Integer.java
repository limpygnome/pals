package pals.javasandbox.parsers;

import pals.javasandbox.ParsedArgument;

/**
 * Parses an integer argument.
 */
public class PA_Integer implements Parser
{
    @Override
    public void parse(ParsedArgument pa, String[] value, boolean array)
    {
        pa.setArgClass(array ? int[].class : int.class);
        if(array)
        {
            int[] data = new int[value.length];
            for(int i = 0; i < value.length; i++)
                data[i] = Integer.parseInt(value[i]);
            pa.setArgValue(data);
        }
        else
            pa.setArgValue(Integer.parseInt(value[0]));
    }
}
