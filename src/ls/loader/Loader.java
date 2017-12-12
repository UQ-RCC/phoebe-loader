/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ls.loader;

import database.ImageFrameRecord;
import database.ImageFrameStats;
import encoder.RLcompress;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileInfo;
import ij.io.FileSaver;
import ij.io.Opener;
import ij.io.TiffDecoder;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.RankFilters;
import ij.process.ImageProcessor;
import ij.process.StackStatistics;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import net.jpountz.lz4.LZ4FrameOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;


/**
 *
 * @author Oliver
 */
public class Loader
{
        
    public static ImagePlus readFrame(String root, ImageFrameRecord imageFrame) throws IOException
    {
        
        Path sourcePath = Paths.get(root, imageFrame.directory , imageFrame.original_filename);            
        ImagePlus ip = getImagePlus(sourcePath);
        imageStatsData(ip,imageFrame);
        ImageStack stack = ip.getStack();
        
        imageFrame.width = ip.getWidth();
        imageFrame.height = ip.getHeight();
        imageFrame.depth = stack.size();
        imageFrame.statsList.add(new ImageFrameStats("raw", new StackStatistics(ip)));

        long filterTime = System.nanoTime();

        RankFilters rank = new RankFilters();                        
        for (int i = 1; i <= stack.size(); i++)
        {
            ImageProcessor processor = stack.getProcessor(i);
            rank.rank(processor, 2.0, RankFilters.MEDIAN);
        }
        imageFrame.statsList.add(new ImageFrameStats("median", new StackStatistics(ip)));

        BackgroundSubtracter backgroundSubtracter = new BackgroundSubtracter();
        for (int i = 1; i <= stack.size(); i++)
        {
            ImageProcessor processor = stack.getProcessor(i);
            backgroundSubtracter.rollingBallBackground(processor, 50.0, false, false, false, true, false);
        }
        imageFrame.statsList.add(new ImageFrameStats("background", new StackStatistics(ip)));
                
        //System.out.printf("back sub %f -> %f\n", stackStats.min, stackStats.max);

//        double factor = 1.0 / 64.0;
//        for (int i = 1; i <= stack.size(); i++)
//        {
//            ImageProcessor processor = stack.getProcessor(i);
//            processor.multiply(factor);
//        }
//        
//        stackStats = new StackStatistics(ip);        
//        System.out.printf("to byte %f -> %f\n", stackStats.min, stackStats.max);

//        ImageConverter.setDoScaling(false);
//        ImageConverter ic = new ImageConverter(ip);
//        ic.convertToGray8();
        
//        stackStats = new StackStatistics(ip);        
//        System.out.printf("converted %f -> %f\n", stackStats.min, stackStats.max);
        
        filterTime = System.nanoTime() - filterTime;
        imageFrame.logMessage.addProperty("filterTime", filterTime);

        return ip;

    }
    
    public static void saveFrameLZ(ImagePlus ip, ImageFrameRecord imageFrame, String root) throws IOException
    {
        processFrameLZ(ip, imageFrame, root, true, false);
    }
    
    public static void analyseFrameLZ(ImagePlus ip, ImageFrameRecord imageFrame, String root) throws IOException
    {
        processFrameLZ(ip, imageFrame, root, false, true);
    }
        
    private static void processFrameLZ(ImagePlus ip, ImageFrameRecord imageFrame, String root, boolean saveFrame, boolean analyse) throws IOException
    {   
        int size = ip.getWidth() * ip.getHeight() * ip.getNSlices();
        float[] voxels = new float[size];
        byte[] bytes = new byte[size];
        ByteArrayOutputStream bos = new ByteArrayOutputStream(size * 2);
        DataOutputStream dos = new DataOutputStream(bos);     
        
        ip.getStack().getVoxels(0, 0, 0, ip.getWidth(), ip.getHeight(), ip.getNSlices(), voxels);
        
        short prevValue = 0;
        RLcompress<Short> rldeltaCompress = new RLcompress<>();
        RLcompress<Short> rlCompress = new RLcompress<>();
        
        double sr = Math.pow(2, 15) - 1;
        
        for (int i = 0; i < size; i++)
        {
            Float floaty = Float.valueOf(voxels[i]);
            short shortValue = floaty.shortValue();
                       
            short b = (short) Math.max(0, Math.round(voxels[i] / 64.0));
            bytes[i] = (byte) b;
            double f = Math.round(voxels[i]);
            f = Math.max(0, f);
            f = Math.min(sr, f);
            dos.writeShort((short) f);
            if (analyse)
            {
                if (i > 0)
                {
                    rlCompress.addValue(new Short((short) (f)));                    
                    rldeltaCompress.addValue(new Short((short) (prevValue - (short) (f))));
                }
            }
            prevValue = (short) f;
        }
        double toMB = Math.pow(1024, -2);
        System.out.printf("rle:   %,f, %,f Comp %f\n", rldeltaCompress.getSize() * toMB, rldeltaCompress.getPhysicalSize() * toMB, rldeltaCompress.getCompressionRatio());
        System.out.printf("dlrle:  %,f, %,f Comp %f\n", rldeltaCompress.getSize() * toMB, rldeltaCompress.getLEBPhysicalSize() * toMB, rldeltaCompress.getLebCompressionRatio());
        System.out.printf("lrle:  %,f, %,f Comp %f\n", rlCompress.getSize() * toMB, rlCompress.getLEBPhysicalSize() * toMB, rlCompress.getLebCompressionRatio());
        
        
        if (saveFrame)
        {            
            Path path = getOutputPath(root, imageFrame.file_name + "_byte");
            FileOutputStream fos = new FileOutputStream(path.toFile());
            LZ4FrameOutputStream lz4 = new LZ4FrameOutputStream(fos);
            lz4.write(bytes);
            lz4.close();

            path = getOutputPath(root, imageFrame.file_name + "_short");
            fos = new FileOutputStream(path.toFile());
            lz4 = new LZ4FrameOutputStream(fos);
            lz4.write(bos.toByteArray());
            lz4.close();

            path = getOutputPath(root, imageFrame.file_name + "_raw_byte");
            fos = new FileOutputStream(path.toFile());
            fos.write(bytes);
            fos.close();

            System.out.printf("wrote: %s %d, %d, %d\n", path.toString(), ip.getWidth(), ip.getHeight(), ip.getNSlices());
        }
                
    }
            
    
    public static void saveFrame(ImagePlus ip, ImageFrameRecord imageFrame, String root) throws IOException
    {
            FileSaver fs = new FileSaver(ip);
            Path outputPath = getOutputPath(root, imageFrame.file_name);            
            fs.saveAsRawStack(outputPath.toString());
    }
    
    public static Path getOutputPath(String root, String fileName)
    {
        Path directory = Paths.get(root, fileName.substring(0, 2), fileName.substring(2, 4));
        if (!directory.toFile().exists())
        {
            directory.toFile().mkdirs();            
        }
        return Paths.get(directory.toString(), fileName);
    }
    
    public static List<Path> buildPath(Path rootPath, Path currentPath)
    {
        
        List<Path> pathList = new ArrayList<>();        
        try (Stream<Path> fileList = Files.list(currentPath))
        {
            fileList.forEach(p -> 
            {
                if (Files.isDirectory(p))
                {
                    System.out.printf("processing %s\n", p.toString());
                    pathList.addAll(buildPath(rootPath,p));
                }
                else
                {
                    File f = p.toFile();
                    {
                        try
                        {
                            if (f.getCanonicalPath().endsWith(".bz2") || f.getCanonicalPath().endsWith(".tif"))
                            {
                                if (!f.getCanonicalFile().toString().matches("(?:.*/|^)p[0-9]+_.*"));
                                {
                                    pathList.add(rootPath.relativize(p));
                                }                               
                            }
                        }
                        catch (IOException e)
                        {
                            System.out.printf("%s can't be converted to a file\n", p.toString());
                        }
                    }
                }
            });
        }
        catch (IOException e)
        {
            System.out.printf("%s can't be procesed\n", rootPath.toString());
        }
        return pathList;
    }

    public static ImagePlus getImagePlus(Path p) throws IOException
    {        
        File f = p.toFile();
        Opener o = new Opener();
        ImagePlus ip = null;
        
        InputStream inputStream = null;
        InputStream in = new FileInputStream(p.toFile());
        BufferedInputStream bin = new BufferedInputStream(in);
        try
        {
            if (f.getName().endsWith("bz2"))
            {
                inputStream = new BZip2CompressorInputStream(bin);
            }
            else if (f.getName().endsWith("tif"))
            {
                inputStream = bin;
            }
            else
            {
                Logger.getLogger(FrameDBLoader.class.getName()).log(Level.SEVERE, "undefined file type");
                inputStream = null; //TODO error check this.
            }
            TiffDecoder dec = new TiffDecoder(inputStream, f.getName());
            FileInfo[] fo = dec.getTiffInfo();
            ip = o.openTiffStack(fo);
        }  
        finally
        {
            if (inputStream != null)
            {
                inputStream.close();
            }
        }
        return ip;
    }
    
    public static void imageStatsData(ImagePlus ip, ImageFrameRecord fr)
    {
         fr.width = ip.getWidth();
         fr.height = ip.getHeight();
         fr.depth = ip.getNSlices();         
//         StackStatistics stackStats = new StackStatistics(ip);
         
//         fr.min = stackStats.min;
//         fr.max = stackStats.max;
//         fr.histogram = stackStats.histogram;
//         fr.mean = stackStats.mean;
//         fr.median = stackStats.median;
//         fr.std_dev = stackStats.stdDev;
         
    }

    
}
