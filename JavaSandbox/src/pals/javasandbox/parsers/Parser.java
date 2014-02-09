package pals.javasandbox.parsers;

import pals.javasandbox.ParsedArgument;

/**
 * The interface for parsing different types of arguments, passed to the
 * sandbox.
 */
public interface Parser
{
    public void parse(ParsedArgument pa, String[] value, boolean array);
}
