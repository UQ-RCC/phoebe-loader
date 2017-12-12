/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ls.loader;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;

/**
 *
 * @author Oliver
 */
public class RunOptions
{
    
    Options o = new Options();

    public RunOptions()
    {
        OptionGroup og  = new OptionGroup();
        og.setRequired(true);
        og.addOption(new Option("b", "build", false, "build file list"));
        og.addOption(new Option("p", "process", false, "process file list"));        
        og.setRequired(false);
        o.addOption("h", "help", false, "print this message");                
        o.addOptionGroup(og);
        o.addOption("d", "dir", true, "base directory");
        o.addOption("s", "source-dir", true, "source directory (within base)");
        o.addOption("t", "test", false, "perfom test operations");
        o.addOption("m", "max-threads", true, "(optional) set maximum threads");
        o.addOption("f", "filter-threads", true, "(optional) threads per image filter");
        o.addOption("stats", false, "add image stats to database");
        o.addOption("delete", false, "delete files");
    }

    public Options getO()
    {
        return o;
    }
        
}
