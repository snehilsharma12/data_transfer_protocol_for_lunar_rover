/**
 * Sends updates when sheduled
 */

import java.util.TimerTask;

public class scheduled_update extends TimerTask{

    @Override
    public void run() {

        synchronized(Main.my_table){

            try {
                
            Main obj = new Main();
            obj.send_response(Main.multicast_ip, Main.multicast_port);

            } catch (Exception e) {
                e.getStackTrace();
            }

        }
        
    }

}
