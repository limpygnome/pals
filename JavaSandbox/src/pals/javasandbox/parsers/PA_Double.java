package pals.javasandbox.parsers;

import pals.javasandbox.ParsedArgument;

/**
 * Parses a double argument.
 */
public class PA_Double implements Parser
{
    @Override
    public void parse(ParsedArgument pa, String[] value, boolean array)
    {
        pa.setArgClass(array ? double[].class : double.class);
        if(array)
        {
            double[] data = new double[value.length];
            for(int i = 0; i < value.length; i++)
                data[i] = Double.parseDouble(value[i]);
            pa.setArgValue(data);
        }
        else
            pa.setArgValue(Double.parseDouble(value[0]));
    }
}
