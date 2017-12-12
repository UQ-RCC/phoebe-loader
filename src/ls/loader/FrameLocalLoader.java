/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ls.loader;

//import database.FrameRecord;
import database.ImageFrameRecord;
import ij.ImagePlus;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Oliver
 */
public class FrameLocalLoader implements Runnable
{
    //FrameRecord frameRecord;    
    long threadId;
    long numThreads;
    ConcurrentLinkedQueue<Path> pathQueue;    
    int filterThreads;
    
    
    public FrameLocalLoader(ConcurrentLinkedQueue<Path> pathQueue, int numThreads, int threadId, int filterThreads)
    {        
        this.threadId = threadId;
        this.numThreads = numThreads;
        this.pathQueue = pathQueue;
        this.filterThreads = filterThreads;
    }
    
    @Override
    public void run()
    {
        Path p = pathQueue.poll();
        while (p != null)
        {
            try
            {
                ImageFrameRecord imageFrame = new ImageFrameRecord();
                imageFrame.directory = p.getParent().toString();
                imageFrame.original_filename = p.getFileName().toString();
                imageFrame.file_name = UUID.randomUUID().toString();
                System.out.printf("loading %s : %s\n", imageFrame.directory, imageFrame.original_filename);
                ImagePlus imagePluse = Loader.readFrame(LSLoader.sourceRootDirectory, imageFrame);
                Loader.analyseFrameLZ(imagePluse, imageFrame, LSLoader.sourceRootDirectory);
                FrameLocalLoader.printFrame(imageFrame);
                p = pathQueue.poll();
            }
            catch (IOException ex)
            {
                Logger.getLogger(FrameLocalLoader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public static synchronized void printFrame(ImageFrameRecord f)
    {
        System.out.printf("directory: %s\n", f.directory);
        System.out.printf("original filename: %s\n", f.original_filename);
        //System.out.printf("size: %d %d %d\n", f.width, f.depth, f.height);        
    }

}


