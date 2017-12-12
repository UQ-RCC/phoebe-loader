
package database;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Oliver
 */

public class ImageFrameRecord
{
    public long id;
    public String directory = "";
    public String original_filename = "";
    public int width;
    public int height;
    public int depth;
//    public String pixel_type = "";    
    public String file_name = "";

    public List<ImageFrameStats> statsList = new ArrayList<>();
    public JsonObject logMessage = new JsonObject();    
        
}
