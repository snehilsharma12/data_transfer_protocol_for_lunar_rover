/**
 * Client that connects to rover and 
 * asks it to send image
 * 
 * @author: Snehil Sharma
 * 
 */

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;



public class client extends Thread{

static byte[] image;
static int my_port;
static int rover_port;
static InetAddress my_address;
static int rover_id = 1;
static int img_data_size;
static int img_seq_start;
static int img_seq_end;
static int no_of_pkt;
static DatagramSocket my_socket;
static Boolean start_flag = true;
static int window = 5;
static int pkt_counter = 0;
static int check_counter = 0;
static int latest_seq_num = 0;
static int ultimate_counter = 0;
static Timer timer = new Timer();

//easy to access packets
static HashMap<String, DatagramPacket> packets = new HashMap<String, DatagramPacket>();

//keep track of sequences received
static ArrayList<Integer> img_seq_received = new ArrayList<Integer>(); 
static int[] expected_seq;

/**
 * Generates the image
 */
public void generate_image(){

    try {

        //you can change the file extension if needed 
        //for testing a PNG maybe, it should automatically convert though
        FileOutputStream fos = new FileOutputStream("output.jpg");
        fos.write(image);    
            
        System.out.println("GENERATING IMAGE");

        fos.close();

        // kills the NACK timer
        timer.cancel();

        byte[] temp = {0,0};
        make_response(420, 0, 0, 0, temp);

        DatagramPacket p = packets.get(String.valueOf(0));

        //SEND COMMAND END TO ROVER
        my_socket.send(p);

    } catch (Exception e) {
        System.out.println(e);
        e.printStackTrace();
    }

    // kills self
    System.exit(0);

}


/**
 * adds the data  to the image array
 * @param seq_num
 * @param data
 */
public void add_image_data(int seq_num, byte[] data){

    int j= 10;
    int index;

    if(seq_num != 0){

        if(seq_num == img_seq_end){

            //System.out.println("IMAGE LENGTH: " + image.length);

            for(int i= image.length - img_data_size; i < image.length; i++){
                
                if(! (i == image.length) ){

                    image[i] = data[j];
        
                    j++;

                }

            }

        }

        else{

            index = (seq_num - img_seq_start)*img_data_size; 
        
            try {
        
                for(int i=index; i < index + data.length - 10; i++){
        
                    image[i] = data[j];
            
                    j++;
                }
             
            } catch (Exception e) {
                System.out.println(e + ("FOR SEQ NO: " + seq_num));
                e.printStackTrace();
            }
        }
    }
}


//convers two bytes to int
public int get_int(Byte b1, Byte b2){

    int num = ((b1 & 0xff) << 8) | (b2 & 0xff);

    return num;
}


//returns a header byte[] that contains the given header info
public byte[] get_header(int command_id, int resp_id, int seq, int nack){

    byte[] header_arr = new byte[10];

    header_arr[0] = (byte) rover_id;

    header_arr[1] = (byte) ((command_id >> 8) & 0xFF);
    header_arr[2] = (byte) (command_id & 0xFF);

    header_arr[3] = (byte) ((resp_id >> 8) & 0xFF);
    header_arr[4] = (byte) (resp_id & 0xFF);

    header_arr[5] = (byte) ((seq >> 8) & 0xFF);
    header_arr[6] = (byte) (seq & 0xFF);

    header_arr[7] = (byte) ((nack >> 8) & 0xFF);
    header_arr[8] = (byte) (nack & 0xFF);

    header_arr[9] = (byte) window;

    return header_arr;

}

//makes a response packet
public void make_response(int command_id, int resp_id, int seq, int nack, byte[] data){

    byte[] header = get_header(command_id, resp_id, seq, nack);

    byte[] packet = new byte[header.length + data.length];

    System.arraycopy(header, 0, packet, 0, header.length);
    System.arraycopy(data, 0, packet, header.length, data.length);

    DatagramPacket pkt =  new DatagramPacket(packet, packet.length, my_address, rover_port);

    //put the packet into easy-access hashmap
    packets.put(String.valueOf(seq), pkt);

}

/**
 * Initiates the connection with rover
 * gets img specifications
 * initializes variables
 * ~Refer doc for command and resp IDs~
 */
public void setup(){

    try {

        byte[] temp = {0,0};
        make_response(100, 0, 1, 0, temp);
        DatagramPacket connect_request = packets.get(String.valueOf(1));

        //CONNECT REQUEST
        my_socket.send(connect_request);

        byte[] connect_resp = new byte[256]; 

        DatagramPacket connect_pkt = new DatagramPacket(connect_resp, connect_resp.length);
        my_socket.receive(connect_pkt);

        int resp_id = get_int(connect_resp[3], connect_resp[4]);

        //IF CONNECT SUCCESS
        if(resp_id == 100){

            System.out.println("Connect Success!");
            
            make_response(201, 0, 2, 0, temp);
            DatagramPacket send_img_cmd = packets.get(String.valueOf(2));

            //SEND GET-IMAGE COMMAND
            my_socket.send(send_img_cmd);
            System.out.println("Img command sent!");

            byte[] img_specs = new byte[256];
            DatagramPacket img_specs_pkt = new DatagramPacket(img_specs, img_specs.length);
            
            my_socket.receive(img_specs_pkt);
            

            int specs_resp_id = get_int(img_specs[3], img_specs[4]);
            int spec_seq_num = get_int(img_specs[5], img_specs[6]);

            //IF IMAGE SPECS RECIEVED
            if(specs_resp_id == 201){
                System.out.println("Img Specs received!");

                img_data_size = get_int(img_specs[10], img_specs[11]);

                System.out.println("img data size = " + img_data_size);

                no_of_pkt = get_int(img_specs[12], img_specs[13]);
                System.out.println("No of img packets expected = " + no_of_pkt);

                int img_size = img_data_size*no_of_pkt;
                image = new byte[img_size]; 

                img_seq_start = spec_seq_num + 1;
                img_seq_end = img_seq_start + no_of_pkt - 1;

                make_response(201, 211, 3, 0, temp);

                DatagramPacket client_ready = packets.get(String.valueOf(3));

                //SEND CLIENT READY
                my_socket.send(client_ready);

                start_flag = false;

            }

        }

        //make an array of expected img sequence nums
        expected_seq = new int[no_of_pkt];
        int j=0;
        for(int i=img_seq_start; i<=img_seq_end; i++){
            expected_seq[j] = i;
            j++;
        }

        System.out.println("Img data starting seq num: " + img_seq_start);
        System.out.println("Getting Data...");

    
    } 
    catch (Exception e) {
        System.out.println(e);
        e.printStackTrace();
    }
}

/**
 * Checks if all packets have been 
 * received or not 
 * Sends NACKs for packets not received
 */
public void check_pkts(){

    byte[] temp = {0,0};

    //System.out.println("SEQ RECEIVED: " + img_seq_received);

    for(int i=img_seq_start; i <= img_seq_end; i++){

        /**
         * Well this 'if' should work but due to hashing
         * and a lack of immutable data-srtuctures in java
         * this doesn't work
         * 
         * Basically would perfectly send NACKs for unreceived packets
         * but hashing makes it always return false for a given seq num
         * anyway, so I just send nack for every seq num until all 
         * packets are received
         * 
         * (if you want to uncomment, you will need to adjust for-loop parameters)
         */
        //if(!img_seq_received.contains(Integer.valueOf(expected_seq[i]))){

            make_response(201, 0, 69, i, temp);

            DatagramPacket s = packets.get(String.valueOf(69));

            try {

                my_socket.send(s);

                //System.out.println("Sending NACK for: " + i);

            } catch (IOException e) {
                System.out.println(e);
                e.printStackTrace();
            }

        //}
    }

    //if all packets are received, generate image
    if(img_seq_received.size() >= no_of_pkt){

        generate_image();
    }

}



public void run(){

    while(true){

        DatagramPacket p;

        synchronized(my_socket){

            if(start_flag){

                setup();
            }

            byte[] packet = new byte[img_data_size + 10];

            p = new DatagramPacket(packet, packet.length);

            try {

                my_socket.receive(p);

            } catch (Exception e) {
                System.out.println(e);
                e.printStackTrace();
            }

        }

        byte[] pkt = p.getData();

        int seq_num = get_int(pkt[5], pkt[6]);


        if(! img_seq_received.contains(Integer.valueOf(seq_num))){
        
            img_seq_received.add(Integer.valueOf(seq_num));

            System.out.println("Img data pkt received: " + seq_num);

        }

        add_image_data(seq_num, pkt);

    }
}



public static void main(String[] args) {
    
    if(args.length > 0){

        my_port = Integer.parseInt(args[0]);

        rover_port = Integer.parseInt(args[1]);

    }

    try {

        my_address = InetAddress.getByName("localhost");

        my_socket = new DatagramSocket(my_port);

    } catch (Exception e) {
        System.out.println(e);
        e.printStackTrace();
    }

    //timer to send NACKs
    timer.schedule( new task(), 3000, 3000);

    client t1 = new client();
    client t2 = new client();
    client t3 = new client();
    client t4 = new client();

    t1.start();
    t2.start();
    t3.start();
    t4.start();


}
    
}



class task extends TimerTask{

    
    @Override  
    public void run() {  

        client obj = new client();
        obj.check_pkts();

    }        

}
