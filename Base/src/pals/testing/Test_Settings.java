package pals.testing;

import java.util.Map;
import pals.base.Settings;
import pals.base.SettingsException;
import pals.base.SettingsNode;

public class Test_Settings
{
    public static void main(String[] args)
    {
        try
        {
            String testPath = "test.config";
            
            Settings settings = new Settings(false);
            System.out.println("Setting data...");
            settings.setString("hello/world", "hello world!");
            settings.setInt("test/int", 12345);
            settings.setBool("test/bool", true);
            settings.setDouble("test/double", 12345.6789);
            settings.setFloat("test/float", 123.456789f);
            
            System.out.println("Reading back...");
            System.out.println("- hello/world ~ '" + (String)settings.get("hello/world") +  "'");
            System.out.println("- test/int ~ '" + (int)settings.get("test/int") +  "'");
            System.out.println("- test/bool ~ '" + (boolean)settings.get("test/bool") +  "'");
            System.out.println("- test/double ~ '" + (double)settings.get("test/double") +  "'");
            System.out.println("- test/float ~ '" + (float)settings.get("test/float") +  "'");
            
            System.out.println("Saving to file...");
            settings.save(testPath);
            
            System.out.println("Reloading file...");
            settings = Settings.load(testPath, false);
            for(Map.Entry<String,SettingsNode> setting : settings.getRaw())
            {
                System.out.println(" - '" + setting.getKey() + "' ~ data-type: " + setting.getValue().getType() + " ~ value '" + setting.getValue().get() + "'.");
            }
            System.out.println("Finished.");
        }
        catch(SettingsException ex)
        {
            System.err.println("Test failed: " + ex.getExceptionType().toString() + " ~ " + ex.getMessage() + " ~ '" + ex.getCause().getLocalizedMessage() + "'");
        }
    }
}
