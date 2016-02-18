import java.io.IOException;
import java.util.Stack;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Created by jialeirong on 1/21/2016.
 */
public class TimerStack {

    public static void main(String[] args)
    {
        test1();
    }
    public static void test1()
    {
        // Entry Part
        Logger logger = Logger.getLogger("byteAgent");
        FileHandler fileHandler = null;
        try {
            fileHandler = new FileHandler("2.log");
        } catch (IOException e) {
            e.printStackTrace();
        }
        fileHandler.setLevel(Level.INFO);
        logger.addHandler(fileHandler);
        logger.setUseParentHandlers(false);
        // Set Timer
        Long _agentTimerBegin = System.nanoTime();
        



        // End Part
        String _agentLogLine = "[Takes (Sec nano)]" + Long.toString(System.nanoTime() - _agentTimerBegin);        
        logger.info(_agentLogLine);
       
    }
    
}
