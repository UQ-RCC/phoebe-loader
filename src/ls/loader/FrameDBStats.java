/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ls.loader;

import com.google.gson.JsonObject;



import database.Database;
import database.ImageFrameRecord;
import ij.ImagePlus;
import ij.Prefs;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Oliver
 */
public class FrameDBStats implements Runnable
{    
    Database database;
    long threadId;    
    String arrayID;    
    int filterThreads = -1;
    int threadID;
    int numThreads;
    
    public FrameDBStats(Database database, int numThreads, int threadId, String arrayID, int filterThreads)
    {
        this.database = database;                
        this.filterThreads = filterThreads;
        if (filterThreads > 0)
        {
            Prefs.setThreads(filterThreads);                
        }        
        this.arrayID = arrayID;
        this.numThreads = numThreads;
        this.threadID = threadId;        
        
    }
    
    @Override
    public void run()
    {
        System.out.printf("thread %d starting\n", threadID);
        ImageFrameRecord imageFrame = database.nextImage();
        while (imageFrame != null)            
        {            
            JsonObject logMessage = new JsonObject();
            logMessage.addProperty("numberThreads", numThreads);
            logMessage.addProperty("threadID", threadID);
            logMessage.addProperty("arrayID", arrayID);
            logMessage.addProperty("filterThreads", filterThreads);
            imageFrame.logMessage = logMessage;
            long logID = -1;
            try
            {                
                logID = database.startLog();
                Loader.readFrame(LSLoader.sourceRootDirectory, imageFrame);                
                database.imageStats(imageFrame);
                database.endLog(logID, "image_stats", imageFrame.id, imageFrame.logMessage.toString());
            }
            catch (Exception ex)
            {
                if (logID > -1)
                {
                    imageFrame.logMessage.add("error", Database.getError(ex));
                    database.endLog(logID, "image_stats", imageFrame.id, imageFrame.logMessage.toString());
                }
                else
                {
                    database.log("stats exception", Database.getError(ex).toString(), imageFrame.id);                    
                }                
            }
            imageFrame = database.nextImage();
        }
        System.out.printf("thread %d stopping\n", threadID);
    }
    
}

