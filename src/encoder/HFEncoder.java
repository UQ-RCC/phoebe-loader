/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package encoder;

import java.util.ArrayList;
import java.util.List;
import maths.MinMax;

/**
 *
 * @author Oliver
 */
public class HFEncoder<T extends Number> extends Encoder<T>
{

    private List<T> values = new ArrayList<>();
    private long runCounter = 0;
    private static int maxRun = 3; // bigest run allowed in the HFencoder.
    private MinMax minMax = new MinMax();

    public HFEncoder(T v)
    {
        values.add(v);
        runCounter = 1;    
    }
    
    public HFEncoder()
    {        
    }
    
    @Override
    Encoder addValue(T v)
    {
        T previousValue = runCounter > 0 ? this.values.get((int) values.size() - 1) : null;
        if (compare(previousValue,v))
        {            
            values.add(v);
            runCounter++;
            minMax.setVal(v.doubleValue());
        }
        else
        {
            values.add(v);
            runCounter = 1;
            minMax.setVal(v.doubleValue());
        }
        
        if (runCounter > maxRun)
        {            
            values.subList(values.size() - maxRun - 1, values.size()).clear();
            return new FlatEncoder<>(v, maxRun + 1);
        }
        else
        {
            return null;
        }
        
    }

    @Override
    int getSize()
    {
        return values.size() * 2;
    }

    @Override
    int getPhysicalSize()
    {
        return getSize() + 2;
    }

    @Override
    int getLEBPhysicalSize()
    {
        int s = values.stream().mapToInt(e -> e.intValue()).map(VLQCode::signedLeb128Size).sum();
        return s + VLQCode.signedLeb128Size(values.size());
    }
    
    
    @Override
    double getRange()
    {
        return minMax.getRange();
    }
        
}
