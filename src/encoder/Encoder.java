/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package encoder;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Oliver
 * @param <T>
 */
public abstract class Encoder<T extends Number>
{
    abstract Encoder<T> addValue(T v);
    abstract int getSize();
    abstract int getPhysicalSize();
    abstract int getLEBPhysicalSize();
    abstract double getRange();
    
    boolean compare(T o1, T o2)
    {
        return o1==o2 || o1!=null && o2!=null && o1.equals(o2);
    }
}
