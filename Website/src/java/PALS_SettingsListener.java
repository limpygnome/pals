import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import pals.base.Settings;
import pals.base.SettingsException;
import pals.base.Storage;

/**
 * Loads required settings when the context/web-app is started.
 */
@WebListener()
public class PALS_SettingsListener implements ServletContextListener
{
    // Fields ******************************************************************
    private static Settings settings = null;
    // Methods *****************************************************************
    @Override
    public void contextInitialized(ServletContextEvent sce)
    {
        try
        {
            settings = Settings.load(sce.getServletContext().getRealPath("WEB-INF/web.config"), true);
            // Check we have access to the shared directory
            Storage.StorageAccess access = Storage.checkAccess(settings.getStr("storage/path"), true, true, true, false);
            switch(access)
            {
                case CannotRead:
                    System.err.println("PALS Error: insufficient read access to shared storage!");
                    break;
                case CannotWrite:
                    System.err.println("PALS Error: insufficient write access to shared storage!");
                    break;
                case DoesNotExist:
                    System.err.println("PALS Error: shared storage does not exist!");
                    break;
                default:
                    System.out.println("PALS: successfully loaded settings and checked shared storage path.");
                    break;
            }
        }
        catch(SettingsException ex)
        {
            System.err.println("Failed to load settings: '" + ex.getMessage() + "'!");
        }
    }
    @Override
    public void contextDestroyed(ServletContextEvent sce)
    {
        settings = null;
    }
    // Methods - Static - Accessors ********************************************
    /**
     * @return Read-only settings for the web-application to interface with
     * the PALS node process.
     */
    public static Settings getSettings()
    {
        return settings;
    }
}
