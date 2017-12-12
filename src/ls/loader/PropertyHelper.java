/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ls.loader;

import java.io.*;
import java.util.Properties;

/**
 *
 * @author Oliver Cairncross
 */
public class PropertyHelper
{

    private static Properties property = null;
    private static File propFile = new File("properties.xml");
    
    public static Properties getProperties()
    {

        if (property == null)
        {
            property = new Properties();
            try
            {
                System.out.printf("using property file '%s'\n", propFile.getAbsolutePath());                
                InputStream inputStream = new FileInputStream(propFile);
                property.loadFromXML(inputStream);                
                inputStream.close();
            }
            catch (FileNotFoundException ex)
            {
                String path = propFile.getAbsolutePath();
                try
                {
                    propFile.createNewFile();
                    System.out.printf("created new property file %s\n", path);                    
                }
                catch (IOException ex1)
                {
                    throw new Error(ex1);
                }
            }
            catch (IOException ex)
            {
                throw new Error(ex);
            }
        }
        return property;
    }
    
    public static String getProperty(String property)
    {
        return getProperties().getProperty(property);
    }
    
    public static void setProperty(String property, String value)
    {
        getProperties().setProperty(property, value);        
    }

    public static void saveProperties()
    {
        try
        {
            OutputStream os = new FileOutputStream(propFile);
            property.storeToXML(os, "Defaults");            
        }
        catch (IOException ex)
        {
            throw new Error(ex);
        }
    }
    
    public static void main(String[] args)
    {
        PropertyHelper.setProperty("database-host", "localhost");
        PropertyHelper.setProperty("database-name", "phoebe");
        PropertyHelper.setProperty("user-name", "dataimport");
        PropertyHelper.setProperty("user-password", "import");
        PropertyHelper.saveProperties();
    }
        
}
