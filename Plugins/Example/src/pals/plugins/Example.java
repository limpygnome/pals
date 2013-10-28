package pals.plugins;

/**
 *
 * @author limpygnome
 */
public class Example extends pals.base.Plugin
{
    @Override
    public String test()
    {
        return "plugin!";
    }
    @Override
    public String test(int a, int b)
    {
        return "sum: " + (a+b);
    }
}
