/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package encoder;

import ij.plugin.Histogram;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.math3.stat.Frequency;

/**
 *
 * @author Oliver
 */
public class RLcompress<T extends Number>
{
    private List<Encoder<T>> encoders = new ArrayList<>();
    private Encoder<T> currentEncoder;
    private long size;
    
    private long less = 0;
    private long more = 0;
    
    public RLcompress()
    {
        currentEncoder = new HFEncoder<T>();
        encoders.add(currentEncoder);
    }
    
    public void addValue(T v)
    {        
        Encoder<T> newEncoder = currentEncoder.addValue(v);
        if (newEncoder != null)
        {            
            currentEncoder = newEncoder;
            encoders.add(newEncoder);
        }
        size++;        
    }
    
    public int getEncoderCount()
    {
        return encoders.size();
    }
    
    public static void main(String[] args)
    {
        RLcompress<Short> c = new RLcompress<>();        
        for (int y = 0; y < 10; y++)
        {   
            for (int i = 0; i < 10; i++)
            {
                c.addValue(new Short((short) (0 + y)));
            }
        }
    }
    
    public long getSize()
    {
        return size * 2;
    }
    
    public long getPhysicalSize()
    {
        return encoders.stream().mapToLong(Encoder::getPhysicalSize).sum();
    }
    
    public long getLEBPhysicalSize()
    {
        return encoders.stream().mapToLong(Encoder::getLEBPhysicalSize).sum();
    }
    
    public double getLebCompressionRatio()
    {
        return getLEBPhysicalSize() / (double) getSize();
    }
    
    public double getCompressionRatio()
    {
        return getPhysicalSize() / (double) getSize();
    }
    
    public static <V> Stream<V> iteratorToStream(final Iterator<V> iterator, final boolean parallell)
    {
        Iterable<V> iterable = () -> iterator;
        return StreamSupport.stream(iterable.spliterator(), parallell);
    }
    
}






