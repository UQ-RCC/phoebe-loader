/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileReader;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;
import ls.loader.PropertyHelper;

/**
 *
 * @author Oliver
 */
public class Database
{

    Connection connection = null;
    PreparedStatement insertFramePS;
    CallableStatement insertImagePath;
    CallableStatement nextImage;
    CallableStatement nextDeletedFile;
    CallableStatement completeImage;
    CallableStatement imageStats;
    CallableStatement startLog;
    CallableStatement endLog;
    CallableStatement log;

    public Database()
    {
        connection = getConnection();
        try
        {
            insertImagePath = connection.prepareCall("{call insert_path(?)}");

            // id, directory, original_filename, filename
            nextImage = connection.prepareCall("{call next_image(?, ?, ?, ?)}");
            nextImage.registerOutParameter(1, Types.BIGINT);
            nextImage.registerOutParameter(2, Types.VARCHAR);
            nextImage.registerOutParameter(3, Types.VARCHAR);
            nextImage.registerOutParameter(4, Types.VARCHAR);
            
            nextDeletedFile = connection.prepareCall("{call next_deleted_file(?, ?)}");
            nextDeletedFile.registerOutParameter(1, Types.VARCHAR);
            
            completeImage = connection.prepareCall("{call complete_image(?, ?, ?, ?)}");

            imageStats = connection.prepareCall("{call insert_image_stats(?, ?, ?, ?)}");

            startLog = connection.prepareCall("{call start_log(?)}");
            startLog.registerOutParameter(1, Types.BIGINT);

            endLog = connection.prepareCall("{call end_log(?, ?, ?, ?)}");

            log = connection.prepareCall("{call log(?, ?, ?)}");

        } catch (SQLException ex)
        {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static Connection getConnection()
    {
        Connection c = null;
        try
        {
            Class.forName("org.postgresql.Driver");
            String databaseURL = "jdbc:postgresql://";
            databaseURL += PropertyHelper.getProperty("database-host") + "/";
            databaseURL += PropertyHelper.getProperty("database-name");

            System.out.printf("database url: %s\n", databaseURL);

            String databaseUser = PropertyHelper.getProperty("user-name");
            String password = PropertyHelper.getProperty("user-password");
            c = DriverManager.getConnection(databaseURL, databaseUser, password);
        } catch (Exception e)
        {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }

        return c;
    }

    synchronized public void insertImagePath(String path)
    {
        try
        {
            insertImagePath.setString(1, path);
            insertImagePath.execute();
        } catch (SQLException ex)
        {
            //Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
            long logID = startLog();
            JsonObject logMessage = new JsonObject();
            logMessage.addProperty("path", path);
            logMessage.addProperty("message", ex.getLocalizedMessage());
            endLog(logID, "build path", 0, logMessage.toString());
        }
    }

    synchronized public ImageFrameRecord nextImage()
    {
        try
        {
            nextImage.execute();
            long frameID = nextImage.getLong(1);
            if (nextImage.wasNull())
            {
                return null;
            }
            ImageFrameRecord frame = new ImageFrameRecord();
            frame.id = frameID;
            frame.directory = nextImage.getString(2);
            frame.original_filename = nextImage.getString(3);
            frame.file_name = nextImage.getString(4);
            return frame;
        } catch (SQLException ex)
        {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    synchronized public String nextDeletedFile(String filename, String status)
    {
        try
        {            
            if (filename != null)
            {
                nextDeletedFile.setString(1, filename);
                nextDeletedFile.setString(2, status);
            }
            else
            {                
                nextDeletedFile.setNull(1, Types.VARCHAR);
                nextDeletedFile.setNull(2, Types.VARCHAR);
            }
            
            nextDeletedFile.execute();
            String nextFilename =  nextDeletedFile.getString(1);
            if (nextDeletedFile.wasNull())
            {
                return null;
            }
            return nextFilename;
            
        }
        catch (SQLException ex)
        {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    synchronized public void completeImage(ImageFrameRecord frame)
    {
        try
        {
            completeImage.setLong(1, frame.id);
            completeImage.setInt(2, frame.width);
            completeImage.setInt(3, frame.height);
            completeImage.setInt(4, frame.depth);
            completeImage.execute();
        } catch (SQLException ex)
        {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    synchronized public void imageStats(ImageFrameRecord frame) throws SQLException
    {
        for (ImageFrameStats ifs : frame.statsList)
        {
            imageStats.setLong(1, frame.id);
            imageStats.setString(2, ifs.operation);
            imageStats.setDouble(3, ifs.stats.min);
            imageStats.setDouble(4, ifs.stats.max);
            imageStats.execute();
        }
    }

    synchronized public long startLog()
    {
        try
        {
            startLog.execute();
            return startLog.getLong(1);
        } catch (SQLException ex)
        {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
            return -1;
        }
    }

    synchronized public void endLog(long logID, String type, long fKey, String message)
    {
        try
        {
            endLog.setLong(1, logID);
            endLog.setString(2, type);
            endLog.setLong(3, fKey);
            endLog.setString(4, message);
            endLog.execute();
        } catch (SQLException ex)
        {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    synchronized public void log(String type, String message, double id)
    {
        try
        {
            log.setString(1, type);
            log.setString(2, message);
            log.setDouble(3, id);
            log.execute();
        } catch (SQLException ex)
        {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static JsonObject getError(Throwable t)
    {
        JsonObject json = new JsonObject();
        t.toString();
        json.addProperty("throwable", t.getClass().getName());
        json.addProperty("message", t.getLocalizedMessage());
        JsonArray jarray = new JsonArray();
        for (StackTraceElement e : t.getStackTrace())
        {
            String location = new String();
            if (e.isNativeMethod())
            {
                location = "native method";
            } else
            {
                location = e.getFileName() + ":" + e.getLineNumber();
            }
            jarray.add(e.getClassName() + "." + e.getMethodName() + "(" + location + ")");
        }
        json.add("stackTrace", jarray);
        return json;
    }

    public static void main(String[] args)
    {
        File f = new File("c:/bllala");
        try
        {
            FileReader fr = new FileReader(f);
        } catch (Exception e)
        {            
            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            String toJson = gson.toJson(Database.getError(e));
            System.out.printf("%s\n\n", toJson);
        }

    }

}
