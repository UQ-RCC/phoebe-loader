/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ls.loader;

import database.Database;
import java.io.File;
import java.io.IOError;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author Oliver
 */
public class LSLoader {
    
    
    static String sourceDirectory;
    static String sourceRootDirectory;
    static String destDirectory;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        RunOptions ro = new RunOptions();        
        CommandLineParser clp = new DefaultParser();        
        String arrayID = System.getenv("PBS_ARRAYID");        
        int cores = Runtime.getRuntime().availableProcessors();
        int filterThreads = -1;
        
        try
        {
            CommandLine cl = clp.parse(ro.getO(), args);
            
            if (cl.hasOption("h"))
            {
                HelpFormatter hf = new HelpFormatter();
                hf.printHelp("Light Sheet Loader", ro.getO());
                System.exit(0);
            }
            
            // Set the root directory
            sourceRootDirectory = ".";
            sourceDirectory = ".";
            destDirectory = ".";
            
            if (cl.hasOption("d"))
            {
                sourceRootDirectory = cl.getOptionValue("d");
                destDirectory = cl.getOptionValue("d");
            }
            sourceRootDirectory += "/scope";
            destDirectory += "/data";
                                    
            if (cl.hasOption("s"))
            {
                sourceDirectory = sourceRootDirectory + "/" + cl.getOptionValue("s");
            }
            else
            {
                sourceDirectory = sourceRootDirectory;
            }
                                    
            // Set max cpu cores
            if (cl.hasOption("m"))
            {
                String s = cl.getOptionValue("m");
                int maxCores = Integer.parseInt(s);
                if (cores > maxCores)
                {
                    cores = maxCores;
                }
            }
            
            // Set filter thread cout
            if (cl.hasOption("f"))
            {
                String s = cl.getOptionValue("f");
                filterThreads = Integer.parseInt(s);
            }
                        
            // Run test
            if (cl.hasOption("t"))
            {                
                Database database = new Database();
                for (int i = 0; i < cores; i++)
                {                    
                    Thread t = new Thread(new FrameDBLoader(database, cores, i + 1, arrayID, filterThreads));
                    t.start();
                }
            }
            
            if (cl.hasOption("stats"))
            {
                Database database = new Database();
                for (int i = 0; i < cores; i++)
                {                    
                    Thread t = new Thread(new FrameDBStats(database, cores, i + 1, arrayID, filterThreads));
                    t.start();
                }
            }
            
            if (cl.hasOption("delete"))
            {
                System.out.printf("Deleting files...");
                Database database = new Database();
                String nextFile = database.nextDeletedFile(null, null);
                long totalSize = 0;
                long fileCount = 0;
                double toMeg = Math.pow(1024, -3);
                while (nextFile != null)
                {
                    Path filePath = Paths.get(destDirectory, nextFile.substring(0, 2), nextFile.substring(2, 4), nextFile);
                    File f = filePath.toFile();
                    if (f.exists())
                    {
                        try
                        {
                            long s = f.length();                            
                            nextFile = database.nextDeletedFile(nextFile, "deleted");
                            totalSize += s;
                            fileCount++;
                        }
                        catch (IOError e)
                        {
                            nextFile = database.nextDeletedFile(nextFile, "IO Error");                            
                        }
                    }
                    else
                    {
                        nextFile = database.nextDeletedFile(nextFile, "not found");
                    }
                    System.out.printf("checked %s\n", filePath);
                }
                System.out.printf("Deleted %,d files (%,.3f GB)\n", fileCount, totalSize * toMeg);
            
            }
            
            // Process files
            if (cl.hasOption("p"))
            {
                Database database = new Database();
                for (int i = 0; i < cores; i++)
                {                    
                    Thread t = new Thread(new FrameDBLoader(database, cores, i + 1, arrayID, filterThreads));
                    t.start();
                }
            }
            
            // Build file list
            if (cl.hasOption("b"))
            {
                Database database = new Database();
                Path rootPath = Paths.get(sourceRootDirectory);
                Path sourcePath = Paths.get(sourceDirectory);
                List<Path> buildPath = Loader.buildPath(rootPath, sourcePath);
                buildPath.forEach(p ->
                {                    
                    database.insertImagePath(p.toString().replace('\\', '/'));
                });
            }
            
        }
        catch (ParseException ex)
        {              
            System.err.println( "Parsing failed: " + ex.getMessage());
        }
        
    }
    
}

