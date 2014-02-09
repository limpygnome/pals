package pals.javasandbox.parsers;

import pals.javasandbox.ParsedArgument;

/**
 * Parses a float argument.
 */
public class PA_Float implements Parser
{
    @Override
    public void parse(ParsedArgument pa, String[] value, boolean array)
    {
        pa.setArgClass(array ? float[].class : float.class);
        if(array)
        {
            float[] data = new float[value.length];
            for(int i = 0; i < value.length; i++)
                data[i] = Float.parseFloat(value[i]);
            pa.setArgValue(data);
        }
        else
            pa.setArgValue(Float.parseFloat(value[0]));
    }
}
