/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package encoder;

/**
 *
 * @author Oliver
 */
public class FlatEncoder<T extends Number> extends Encoder<T>
{
    
    private T value;
    private int size;

    public FlatEncoder(T v, int n)
    {
        this.value = v;
        this.size = n;
    }
        
    @Override
    Encoder<T> addValue(T v)
    {
        if (compare(v, value))
        {
            size++;
            return null;
        }
        else
        {
            return new HFEncoder<>(v);
        }
    }

    @Override
    int getSize()
    {
        return size * 2;
    }

    @Override
    int getPhysicalSize()
    {
        return 2 * 2;
    }

    @Override
    int getLEBPhysicalSize()
    {
        return VLQCode.signedLeb128Size(size) + VLQCode.signedLeb128Size(value.intValue());
    }
    
    @Override
    double getRange()
    {
        return 0;
    }
    
}
