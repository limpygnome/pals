import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Responsible for starting and stopping the core of PALS for the web context.
 */
@WebListener()
public class PALS_Listener implements ServletContextListener
{
    @Override
    public void contextInitialized(ServletContextEvent sce)
    {
        System.out.println("hello world...");
    }
    @Override
    public void contextDestroyed(ServletContextEvent sce)
    {
        System.out.println("Good-bye world!");
    }
}
