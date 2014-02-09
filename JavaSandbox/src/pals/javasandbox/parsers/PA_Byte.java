package pals.javasandbox.parsers;

import pals.javasandbox.ParsedArgument;

/**
 * Parses a byte argument.
 */
public class PA_Byte implements Parser
{
    @Override
    public void parse(ParsedArgument pa, String[] value, boolean array)
    {
        pa.setArgClass(array ? byte[].class : byte.class);
        if(array)
        {
            byte[] data = new byte[value.length];
            for(int i = 0; i < value.length; i++)
                data[i] = Byte.parseByte(value[i]);
            pa.setArgValue(data);
        }
        else
            pa.setArgValue(Byte.parseByte(value[0]));
    }
}
