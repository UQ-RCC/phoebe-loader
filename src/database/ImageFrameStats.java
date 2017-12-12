/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database;

import ij.process.StackStatistics;

/**
 *
 * @author Oliver
 */
public class ImageFrameStats
{
    public String operation;
    public StackStatistics stats;

    public ImageFrameStats(String operation, StackStatistics stats)
    {
        this.operation = operation;
        this.stats = stats;
    }
    
}


