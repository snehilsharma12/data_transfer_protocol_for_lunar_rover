/**
 * A program that implements RIPv2 routing protocol
 * 
 * @author: Snehil Sharma
 * 
 */


import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.ArrayList;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;


public class Main extends Thread{

    static InetAddress my_ip;
    static Set<InetAddress> my_neighbors = new  HashSet<InetAddress>();
    static int my_port;
    static byte[] multicast_byte_array;
    static InetAddress multicast_ip;
    static int my_id;
    static InetAddress my_mask;
    static int multicast_port;
    static DatagramSocket my_socket;
    static MulticastSocket multicast_socket;
    static rtable my_table;
    static Boolean trigger_flag = false;

    static HashMap<InetAddress, Timer> my_nb_timers = new HashMap<InetAddress, Timer>();

    static Timer send_updates;


    /**
     * Updates the table
     * 
     * @param neighbor_table table received
     * @return Gives back updated table
     * @throws UnknownHostException
     */
    public rtable update_table(rtable neighbor_table) throws UnknownHostException{


        //if neighbor table is empty, do nothing
        if(neighbor_table.isEmpty()){
            return my_table;
        }

        InetAddress nb_entry_ip;
        InetAddress nb_next_hop;
        int nb_cost = 0;

        InetAddress my_entry_ip;
        InetAddress my_next_hop;
        int my_cost = 0;

        //find the address with cost 0
        InetAddress neighbor = get_sender(neighbor_table);
        byte[] err = {0,0,0,0};
        if(neighbor.equals(InetAddress.getByAddress(err))){

            return my_table;
        }

        //Ignore table received from our multicast echo
        if(neighbor.equals(my_ip)){

            return my_table;
        }

        //Add/Update neighbors 
        if(!(neighbor.equals(my_ip))){

            //add neighbor if we have none
            if( my_nb_timers.isEmpty()){

                Timer t = new Timer();

                my_nb_timers.put(neighbor, t);

                t.schedule(new yeet_the_route(neighbor), 10*1000);

                route r = neighbor_table.getRoute(neighbor);

                my_table.add(r.id, r.router, r.mask, r.next_hop, 1);
            }
    
            //update timer is neighbor exist
            else if( my_nb_timers.containsKey(neighbor) ){
    
                my_nb_timers.get(neighbor).cancel();
    
                Timer t = new Timer();
    
                my_nb_timers.put(neighbor, t);
    
                t.schedule(new yeet_the_route(neighbor), 10*1000);
    
            }
            
            //Add/update neighbor and re-start timer
            if( !(my_nb_timers.containsKey(neighbor)) ){
    
    
                Timer t = new Timer();
    
                my_nb_timers.put(neighbor, t);
    
                t.schedule(new yeet_the_route(neighbor), 10*1000);

                route r = neighbor_table.getRoute(neighbor);

                if(my_table.has(neighbor)){

                    int x = my_table.getIndex(neighbor);

                    my_table.remove(x);

                    my_table.add(r.id, r.router, r.mask, r.next_hop, 1);

                }

                else{

                    my_table.add(r.id, r.router, r.mask, r.next_hop, 1);
                }
    
            }


        }

        

        for(int i=0; i<neighbor_table.size(); i++){

            try {
                
                route nb_route = (neighbor_table.get(i));

                nb_entry_ip = nb_route.router;
                nb_cost = nb_route.cost;
                nb_next_hop = nb_route.next_hop;

                //ignore their self route
                if(nb_route.router.equals(neighbor)){

                    continue;
                }

                //if the entry is our router
                if (nb_entry_ip.equals(my_ip)){

                    continue;
                }

                //if the next hop of that entry is this router
                if (nb_next_hop.equals(my_ip)){
                    continue;
                }

                
                //if entry exist in our table
                if (my_table.has(nb_entry_ip) ){

                    //deal with infinity route
                    if(nb_cost == 16){

                        int index = my_table.getIndex(nb_route.router);

                        my_table.remove(index);

                        trigger_flag = true;

                        continue;
                    }

                    //ignore route that we already have, to process later
                    if(my_table.has(nb_next_hop)){
                        continue;
                    }


                    route my_route = my_table.getRoute(nb_entry_ip);
                    my_entry_ip = my_route.router;
                    my_cost = my_route.cost;
                    my_next_hop = my_route.next_hop;


                    //if our next hop is the their router 
                    if(my_next_hop.equals(neighbor) ){

                        if(! my_entry_ip.equals(neighbor)){

                            //see if their route has become better
                            if(my_cost > (nb_cost+1)  ){

                                my_cost = nb_cost + 1;

                                int index = my_table.getIndex(my_entry_ip);

                                my_table.remove(index);
                                InetAddress mask = nb_route.mask;
                                int the_id = nb_route.id;

                                my_table.add(the_id, my_entry_ip, mask, my_next_hop, my_cost);


                            }

                            //if it became worse
                            else if (nb_cost > my_cost - 1){
                                my_cost = nb_cost + 1;

                                int index = my_table.getIndex(my_entry_ip);

                                my_table.remove(index);
                                InetAddress mask = nb_route.mask;
                                int the_id = nb_route.id;

                                my_table.add(the_id, my_entry_ip, mask, my_next_hop, my_cost);
                                
                            }

                        }

                    }

                    //if my cost is more
                    if(my_cost >= (nb_cost+1)  ){
                        
                        my_cost = nb_cost + 1;
                        my_next_hop = neighbor;
                        int index = my_table.getIndex(my_entry_ip);

                        my_table.remove(index);
                        InetAddress mask = nb_route.mask;
                        int the_id = nb_route.id;
    
                        my_table.add(the_id, my_entry_ip, mask, my_next_hop, my_cost);

                    }

                    else if(my_cost < (nb_cost +1)){

                        continue;
                    }
                    

                }

                //if we don't have that entry
                if( !(my_table.has(nb_entry_ip)) ){

                    if(nb_cost == 16){

                        trigger_flag = true;

                        continue;
                    }

                    my_entry_ip = nb_entry_ip;
                    my_next_hop = neighbor;
                    my_cost = nb_route.cost + 1;
                    InetAddress mask = nb_route.mask;
                    int the_id = nb_route.id;

                    my_table.add(the_id, my_entry_ip, mask, my_next_hop, my_cost);
                    
                }
                


            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(e);
            }

        }

        return my_table;
    }


    /**
     * converts table to byte[]
     * @param table routing table
     * @return byte[]
     */
    public byte[] table_to_array(rtable table){

        byte[] arr= new byte[table.size()*20];

        ArrayList<route> v = table.get_ArrayList();

        int j=0;

        for(int i=0; i<v.size(); i++){

            route r = new route();

            r = v.get(i);

            int id = r.id;

            byte[] byte_id =  ByteBuffer.allocate(4).putInt(id).array();

            InetAddress ip = r.router;

            byte[] ip_ar = new byte[4]; 
            ip_ar = ip.getAddress();


            InetAddress mask = r.mask;
            byte[] mask_arr = new byte[4];
            mask_arr = mask.getAddress();

            InetAddress hop = r.next_hop;
            byte[] hop_arr = new byte[4];
            hop_arr = hop.getAddress();

            int cost = r.cost;
            ByteBuffer b = ByteBuffer.allocate(4);
            b.putInt(cost);
            byte[] cost_arr = new byte[4];
            cost_arr = b.array();

            

                arr[j] = byte_id[2];
                arr[j+1] = byte_id[3];
                arr[j+2] = 0;
                arr[j+3] = 0;

                arr[j+4] = ip_ar[0];
                arr[j+5] = ip_ar[1];
                arr[j+6] = ip_ar[2];
                arr[j+7] = ip_ar[3];

                arr[j+8] = mask_arr[0];
                arr[j+9] = mask_arr[1];
                arr[j+10] = mask_arr[2];
                arr[j+11] = mask_arr[3];

                arr[j+12] = hop_arr[0];
                arr[j+13] = hop_arr[1];
                arr[j+14] = hop_arr[2];
                arr[j+15] = hop_arr[3];

                arr[j+16] = cost_arr[0];
                arr[j+17] = cost_arr[1];
                arr[j+18] = cost_arr[2];
                arr[j+19] = cost_arr[3];

                j += 20;

        }


        return arr;

    }


    /**
     * sends a request
     */
    public void send_request(){

        //set operation as request
        byte[] data = {1, 2, 0, 0, 0,0,0,0};

        DatagramPacket request = new DatagramPacket(data, data.length,  multicast_ip, multicast_port);

        try {

            DatagramSocket receiver_socket = new DatagramSocket();

            receiver_socket.send(request);

            receiver_socket.close();

            System.out.println("request sent");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
        }
    }

    /**
     * 
     *Sends a response
     * @param receiver_addr 
     * @param receiver_port
     */
    public void send_response(InetAddress receiver_addr, int receiver_port){


        try {

            byte[] table_data = table_to_array(my_table);

            //set operation as response
            byte[] info_header = {2, 2, 0, 0};



            byte[] packet_data = new byte[table_data.length + 4];

            System.arraycopy(info_header, 0, packet_data, 0, info_header.length);
            System.arraycopy(table_data, 0, packet_data, 4, table_data.length);

            

            DatagramSocket send_socket = new DatagramSocket();

            DatagramPacket response = new DatagramPacket(packet_data, packet_data.length, receiver_addr, receiver_port);

            send_socket.send(response);

            send_socket.close();

            System.out.println("response sent");


        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
        }

    }


    /**
     * Extracts routes and builds a table
     * @param packet
     * @return table
     */
    public rtable get_table(byte[] packet){


        rtable recieved_table = new rtable();

        for(int i=0; i<packet.length; i+=20){

            int x=i;

            byte[] ip_arr = new byte[4];
            byte[] mask_arr = new byte[4];
            byte[] hop_arr = new byte[4];
            byte[] cost_arr = new byte[4];


            Byte id = packet[x+1];

            ip_arr[0] = packet[x+4];
            ip_arr[1] = packet[x+5];
            ip_arr[2] = packet[x+6];
            ip_arr[3] = packet[x+7];

            mask_arr[0] = packet[x+8];
            mask_arr[1] = packet[x+9];
            mask_arr[2] = packet[x+10];
            mask_arr[3] = packet[x+11];

            hop_arr[0] = packet[x+12];
            hop_arr[1] = packet[x+13];
            hop_arr[2] = packet[x+14];
            hop_arr[3] = packet[x+15];

            cost_arr[0] = packet[x+16];
            cost_arr[1] = packet[x+17];
            cost_arr[2] = packet[x+18];
            cost_arr[3] = packet[x+19];


            try {

                InetAddress ip = InetAddress.getByAddress(ip_arr);
                InetAddress mask = InetAddress.getByAddress(mask_arr);
                InetAddress hop = InetAddress.getByAddress(hop_arr);
                int cost = Byte.toUnsignedInt(cost_arr[3]);
                
                recieved_table.add(id, ip, mask, hop, cost);

            } catch (Exception e) {
                
                e.printStackTrace();

            }

            
        }

        return recieved_table;

    }


    /**
     * Trims byte array
     * @param data
     * @param data_length
     * @return
     */
    public byte[] trim_byte_array(byte[] data, int data_length){

        int  i = data.length-1;

        while(data[i]==0){
            i--;
        }

        byte[] new_array = new byte[i+1];

        for(int j=0; j<new_array.length; j++){

            new_array[j] = data[j];
        }

        return new_array;

    }



    //trims out the header in header+table packet
    public byte[] remove_header(byte[] data){

        byte[] new_array = new byte[data.length - 4];

        int j = 0;
        for(int i = 4; j< new_array.length; i++){

            new_array[j] = data[i];

            j++;
        }

        return new_array;
    }

    /**
     * Finds the sender of the table (cost = 0)
     * @param table
     * @return sender ip
     * @throws UnknownHostException
     */
    public InetAddress get_sender(rtable table) throws UnknownHostException{


        int i=0;

        for(i=0; i<table.size(); i++){


            if( (table.get(i)).cost == 0 ){

                return (table.get(i)).router;

            }

            else{

                continue;
            }
        }

        byte[] err = {0,0,0,0};
        InetAddress errad = InetAddress.getByAddress(err);

        return errad;
        
    }

    /**
     * removes unreachable router 
     */
    public void remove_infinity_router(){

        for(int i=0; i<my_table.size(); i++){

            route r = my_table.get(i);

            if(r.cost == (16)){

                my_table.remove(i);

                my_nb_timers.remove(r.router);

            }
        }
    }


    /**
     * prints the table
     */
    public void print_table(){

        System.out.println("IP" + "                     " + "Next Hop" + "                 " + "Cost");

        for(int i=0; i<my_table.size(); i++){

            route r = my_table.get(i);

            String ip = r.router.toString();
            String[] ip_arr = ip.split("/");

            String hop = r.next_hop.toString();
            String[] hop_arr = hop.split("/");


            System.out.println(ip_arr[1] + "          " + hop_arr[1] + "                 " + r.cost);
        }

    }



    public void run(){

        // System.out.println("reached run");

        while(true){

            byte[] buf = new byte[1000];
            DatagramPacket d_packet = new DatagramPacket(buf, buf.length, multicast_ip, multicast_port);

            synchronized(my_ip){

                // System.out.println("reached synch");

                try {

                    //if trigger set then process infinity
                    if(trigger_flag == true){

                        send_response(multicast_ip, multicast_port);

                        trigger_flag = false;


                        remove_infinity_router();

                        continue;

                    }

                    //Wait for multicast
                    multicast_socket.receive(d_packet);

                    System.out.println("RECEIVED FROM MULTICAST");
  

                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println(e);
                }

            }

            try {
                
                //Process packet
                byte[] data = new byte[d_packet.getLength()];

                System.arraycopy(d_packet.getData(), d_packet.getOffset(), data, 0, d_packet.getLength());

                int actual_length = d_packet.getLength();

                if (actual_length == 0){

                    continue;
                }

                //processing packet more
                byte[] table_array = remove_header(data);

                rtable nb_table = new rtable();
                
                //generate received table
                nb_table = get_table(table_array);

                synchronized(my_table){

                    //if operation was request
                    if (data[0] == 1){
                        
                        InetAddress rcvr_adddress = d_packet.getAddress();

                        int rcvr_port = d_packet.getPort();

                        send_response(rcvr_adddress, rcvr_port);
                    }

                    else{

                        update_table(nb_table);

                        print_table();

                    }

                }

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(e);
            }
            
        }
    }


    public static void main(String[] args) {

        if(args.length > 0){
            
            try {

                System.out.println(args[0]);

                multicast_ip = InetAddress.getByName(args[0]);
                
                //not used
                my_port = 4445;

                multicast_port = Integer.parseInt(args[1]);
    
                my_id = Integer.parseInt(args[2]);
    
                my_ip = InetAddress.getLocalHost();

    
                multicast_socket = new MulticastSocket(multicast_port);


                multicast_socket.joinGroup(multicast_ip);

                my_table = new rtable();

                my_mask = InetAddress.getByName("255.255.255." + my_id);

                my_table.add_self(my_id, my_ip, my_mask);
    
                Main t1 = new Main();
                Main t2 = new Main();
                Main t3 = new Main();
                Main t4 = new Main();
    
                t1.start();
                t2.start();
                t3.start();
                t4.start();
    
    
                send_updates = new Timer();
                send_updates.scheduleAtFixedRate(new scheduled_update(), 5*1000, 5*1000);
    
    
    
    
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(e);
            }
    
        }


        else{

            System.out.println("err: Pls give arguments: MulticastIP MulticastPort ID");

        }   
    }
        
}
