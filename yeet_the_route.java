/**
 * triggers when router is unreachable 
 * ( no activity for 10 seconds=> timer expired )
 * updates the table to adjust for cost infinity
 */

import java.net.InetAddress;
import java.util.TimerTask;


public class yeet_the_route extends TimerTask {

    InetAddress router_ip;

    yeet_the_route(){}

    yeet_the_route(InetAddress router_ip){
        this.router_ip = router_ip;
    }

    @Override
    public void run() {
        
        synchronized(Main.my_neighbors){

            synchronized(Main.my_table){

                Main obj = new Main();

                //remove this neighbor
                Main.my_nb_timers.remove(router_ip);

                //Change cost to infinity for this router
                route neighbor_route = Main.my_table.getRoute(router_ip);

                //16 is our infinity
                int cost = 16;
                InetAddress mask = neighbor_route.mask;
                int id = neighbor_route.id;

                //delete all routes that had this router as next hop
                for(int i=0; i<Main.my_table.size(); i++){

                    route r = Main.my_table.get(i);

                    if(r.next_hop.equals(router_ip)){

                        Main.my_table.remove(i);
                    }
                }

                //put new route for this router with cost as infinity
                Main.my_table.add(id, router_ip, mask, router_ip, cost);

                obj.print_table();

                //set the trigger flag
                Main.trigger_flag = true;
                
            }

        }
        
    }
   
}
