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
public class FrameDBLoader implements Runnable
{    
    Database database;
    long threadId;    
    String arrayID;    
    int filterThreads = -1;
    int threadID;
    int numThreads;
    
    public FrameDBLoader(Database database, int numThreads, int threadId, String arrayID, int filterThreads)
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
        ImageFrameRecord imageFrame = database.nextImage();
        while (imageFrame != null)            
        {            
            JsonObject logMessage = new JsonObject();
            logMessage.addProperty("numberThreads", numThreads);
            logMessage.addProperty("threadID", threadID);
            logMessage.addProperty("arrayID", arrayID);
            logMessage.addProperty("filterThreads", filterThreads);
            imageFrame.logMessage = logMessage;
            try
            {
                long logID = database.startLog();
                ImagePlus ip = Loader.readFrame(LSLoader.sourceRootDirectory, imageFrame);
                //Loader.saveFrame(ip, imageFrame, LSLoader.destDirectory);
                Loader.saveFrameLZ(ip, imageFrame, LSLoader.destDirectory);
                database.completeImage(imageFrame);
                database.endLog(logID, "image", imageFrame.id, imageFrame.logMessage.toString());
            }
            catch (IOException ex)
            {                
                Logger.getLogger(FrameDBLoader.class.getName()).log(Level.SEVERE, null, ex);
            }
            imageFrame = database.nextImage();
        }
    }
    
}

