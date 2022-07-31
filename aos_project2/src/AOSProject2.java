import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;


class NodeLoc{
	int node_id;
	String node_name;
	int node_port;
}

class Packet implements Serializable {
	String packet_type;
	int sender_id;
	int receiver_node_id;
	String receiver_node_name;
	int logical_timestamp;
	int request_timestamp;
	int[] vector_timestamp;
	int[] request_vector_timestamp;
}

public class AOSProject2 {
	
	public static int no_of_nodes;
	public static int local_node_id;
	public static int no_of_cs_req;
	public static int mean_delay;
	public static int mean_duration;
	public static int logical_timestamp=0;
	public static int request_timestamp=0;
	public static int[] vector_timestamp;
	public static int[] request_vector_timestamp;
	
	public static boolean cs_request_flag=false;
	public static boolean cs_section_flag=false;

	public static final int MAX_AVAILABLE = 1;
	public static final Semaphore semaphore = new Semaphore(MAX_AVAILABLE, true);
	
	public static Queue<Packet> queue = new LinkedList<Packet>();
	
	public static String resource_file = "resource.txt";
	
	public static HashMap<Integer,NodeLoc> NodeLocInfo  = new HashMap<>();
	public static ConcurrentHashMap<Integer,Integer> NodeKeysInfo  = new ConcurrentHashMap<>();

	public static String transport_protocol;
	
	public static String output_dir = "output";
	public static String output_file;
	public static String all_output_file = "all_output.txt";
	public static String csviolation_file = "csviolation.txt";
	
	public static void main(String[] args) {
		
		try{
			
			
			//Get Local Node ID from Command Line Argument
			//local_node_id = 0;
			local_node_id = Integer.parseInt(args[0]);
			
			//output_file = "out.txt";
			output_file = "output_node"+AOSProject2.local_node_id+".txt";
			write_output("Node:"+AOSProject2.local_node_id+" -- Output file:"+output_file,true);
					
			write_output("----------------------------------------------------------------",true);
			write_output("------------- AOSProject2 at Node: " + local_node_id + " -------------------------",true);
			write_output("----------------------------------------------------------------",true);
	
			// Get HostName
			InetAddress iAddress = InetAddress.getLocalHost();
		    String hostName = iAddress.getHostName();
		    //To get  the Canonical host name
		    String canonicalHostName = iAddress.getCanonicalHostName();
		
		    write_output("Node:"+AOSProject2.local_node_id+" -- HostName:" + hostName,true);
		    write_output("Node:"+AOSProject2.local_node_id+" -- Canonical Host Name:" + canonicalHostName,true);
			
		
			// Read Config File
		    write_output("Node:"+AOSProject2.local_node_id+" -- Reading Config file",true);
			read_config();
				
			
			//Read argument on which protocol to run
			transport_protocol = "TCP";
			if(args.length > 1)
			{
				transport_protocol = args[1];		
			}
			transport_protocol = transport_protocol.toUpperCase();		
			write_output("Node:"+AOSProject2.local_node_id+" -- Using Protocol:"+transport_protocol,true);
			
			
			
			// Start Server
			int server_node_port = NodeLocInfo.get(local_node_id).node_port;
			
			CommServer CommServerObj = new CommServer(server_node_port,transport_protocol);
			CommServerObj.start();
			
			write_output("Node:"+AOSProject2.local_node_id+" -- Started "+transport_protocol+" Server at Port:"+server_node_port,true);
			
			
			// Go to sleep for sometime so that all nodes are active before execution	
			try {
				Thread.sleep(4000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		
			for (int i = 0; i < AOSProject2.no_of_cs_req; i++) {
							
				request_timestamp = logical_timestamp;
				
				request_vector_timestamp =  vector_timestamp;
						
				//CS Enter
				Service srv = new Service();
				srv.cs_enter();
								
			}
			
			
		
				
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	
	
	
	
	
	

	}
	
	public static void read_config()
	{
		
		// Read Config File - Start
		//String configFile = "src/config.txt";
		String configFile = "config.txt";
		BufferedReader br = null;
		String line = "";

	 
		try {
	 
			int line_no=1;
				
			boolean line_no_of_nodes=false;
			boolean line_location=false;
			boolean line_key_dist=false;
			boolean line_this_node=false;
			boolean line_no_of_cs_req=false;
			boolean line_mean_delay=false;
			boolean line_mean_duration=false;
			
			br = new BufferedReader(new FileReader(configFile));
			
			while ((line = br.readLine()) != null) {
				
				if(line.contains("Number of nodes")==true)
				{
					line_no_of_nodes=true;
					continue;					
				}
				else if(line.contains("Identifier	Hostname	Port")==true)
				{
					line_location=true;
					continue;
				}
				else if(line.contains("Distribution of shared keys can be done in an arbitrary manner")==true)
				{
					line_location=false;
					
					line_key_dist=true;
					continue;
				}
				else if(line.contains("Parameters to control the behavior of the application")==true)
				{
					line_key_dist=false;
				}
				else if(line.contains("Number of critical section requests per node")==true)
				{							
					line_no_of_cs_req=true;
					continue;
				}
				else if(line.contains("Mean delay between two consecutive critical section requests")==true)
				{							
					line_mean_delay=true;
					continue;
				}
				else if(line.contains("Mean duration of critical section")==true)
				{							
					line_mean_duration=true;
					continue;
				}
	 
				if(line.startsWith("#")==false || line.startsWith(" ")==false)
				{				
					if(line_no_of_nodes==true)
					{
						no_of_nodes = Integer.parseInt(line.trim());
						write_output("No. of Nodes:"+no_of_nodes,true);
						line_no_of_nodes=false;
						vector_timestamp = new int[no_of_nodes];
						request_vector_timestamp = new int[no_of_nodes];
					}
					else if(line_location==true)
					{
															
						// Set value in Node Location Hashmap
						NodeLoc nodeLocObj = new NodeLoc();
						nodeLocObj.node_id = Integer.parseInt(line.split("\t")[0].trim());
						nodeLocObj.node_name = line.split("\t")[1].trim();
						nodeLocObj.node_port = Integer.parseInt(line.split("\t")[2].trim());
						NodeLocInfo.put(nodeLocObj.node_id, nodeLocObj);
								
					}
					else if(line_key_dist==true)
					{
						int this_node_id = Integer.parseInt(line.split("\t")[0]);
						
						if(this_node_id==local_node_id)
						{
							line_this_node=true;
						}
						
						if(line_this_node==true)
						{
							write_output("Read - Key Distribution : "+line.trim(),true);
							
							int node_id = Integer.parseInt(line.split("\t")[0].trim());
							
							int[] keysIntArray = new int[0];
							
							String node_keys_csv ="";
							if(line.split("\t").length>1)
							{
								node_keys_csv = line.split("\t")[1].trim();
															
								String[] keysStrArray = node_keys_csv.split(",");
								keysIntArray = new int[keysStrArray.length];
								for(int i = 0; i < keysStrArray.length; i++) {
									keysIntArray[i] = Integer.parseInt(keysStrArray[i]);
								}
							
							}
							
			
							// Set value in Key Distribution Hashmap
							for (int i = 0; i < no_of_nodes; i++) {
								
								NodeKeysInfo.put(i, 0);
								
								for (int j = 0; j < keysIntArray.length; j++) {
								
									if(i==keysIntArray[j])
									{
										NodeKeysInfo.put(i, 1);
									}
									
								
								}
							}
							
							line_this_node=false;
						}
					}
					else if(line_no_of_cs_req==true)
					{
						write_output("Read - Number of critical section requests per node : "+line.trim(),true);
						no_of_cs_req = Integer.parseInt(line.trim());
						line_no_of_cs_req=false;
					}
					else if(line_mean_delay==true)
					{
						write_output("Read - Mean delay between two consecutive critical section requests : "+line.trim(),true);
						mean_delay = Integer.parseInt(line.trim());
						line_mean_delay=false;
					}
					else if(line_mean_duration==true)
					{
						write_output("Read - Mean duration of critical section : "+line.trim(),true);
						mean_duration = Integer.parseInt(line.trim());
						line_mean_duration=false;
					}
						
				}
						
		
				line_no++;
			}
			

			System.out.println("\nNode "+AOSProject2.local_node_id+" Location Info:");
			for(Entry<Integer, NodeLoc> entry : NodeLocInfo.entrySet()){
				System.out.printf("Key : %s and Value: %s %n", entry.getKey(), entry.getValue().node_id+"\t"+entry.getValue().node_name+"\t"+entry.getValue().node_port);
		    }
			
			System.out.println("\nNode "+AOSProject2.local_node_id+" Key Distribution Info:");
			for(Entry<Integer, Integer> entry : NodeKeysInfo.entrySet()){
				System.out.printf("Key : %s and Value: %s %n", entry.getKey(), entry.getValue());
		    }
			
			
			
	 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		// Read Config File - End
		
		
	}
	
	 public static byte[] serialize(Object obj) throws IOException {
	     ByteArrayOutputStream out = new ByteArrayOutputStream();
	     ObjectOutputStream os = new ObjectOutputStream(out);
	     os.writeObject(obj);
	     return out.toByteArray();
	 }
	
	public static Object deserialize1(byte[] data) throws IOException, ClassNotFoundException {
	     ByteArrayInputStream in = new ByteArrayInputStream(data);
	     ObjectInputStream is = new ObjectInputStream(in);
	     return is.readObject();
	 }
	
	public static Object deserialize(ByteBuffer byteBuffer) throws IOException, ClassNotFoundException {	     		
		ByteArrayInputStream in = new ByteArrayInputStream(byteBuffer.array());
	    ObjectInputStream is = new ObjectInputStream(in);
	    return is.readObject();
	 }

	
	public static void write_output(String stmt,Boolean show_console)
	{
		
		PrintWriter pw = null;
		PrintWriter pw2 = null;
		try {
		    pw = new PrintWriter(new BufferedWriter(new FileWriter(output_dir + "/" + output_file, true)));
		    pw.println(stmt);
		    
		    pw2 = new PrintWriter(new BufferedWriter(new FileWriter(output_dir + "/" + all_output_file, true)));
		    pw2.println(stmt);
		    
		    if(show_console==true)
		    {
		    	System.out.println(stmt);
		    }
		    
		}catch (IOException e) {
		    System.err.println(e);
		}finally{
		    if(pw != null){
		    	pw.close();
		    }
		    if(pw2 != null){
		    	pw2.close();
		    }
		}
		
	}
	
	public static void purgeDirectory(File dir) {
	    for (File file: dir.listFiles()) {
	        if (file.isDirectory()) purgeDirectory(file);
	        file.delete();
	    }
	}
	
	public static int generateExponentialDist(int mean)
	{
		Random random = new Random();		
		int val = (int) (-mean * Math.log(random.nextDouble()));
		return val;	
	}
	
	public static void write_to_file(String stmt,String filename)
	{
		
		PrintWriter pw = null;

		try {
		    pw = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)));
		    pw.println(stmt);		    		    
		    
		}catch (IOException e) {
		    System.err.println(e);
		}finally{
		    if(pw != null){
		    	pw.close();
		    }

		}
		
	}
	
}
