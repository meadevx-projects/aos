import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
	int file_num;
	String file_name;
	byte[] file_contents;
	int version_number;
	int ru_value;
	int logical_timestamp;
	int[] vector_timestamp;	
	int request_number;
}

public class AOSProject3 {
	
	public static int local_node_id;
	
	public static int no_of_nodes;
	public static int no_of_files;
	public static int no_of_op;
	public static int mean_delay;
	public static int fraction_read;
	public static int exp_backoff_min;
	public static int exp_backoff_max;
	public static int exp_last_max;
	
	public static HashMap<Integer,NodeLoc> NodeLocInfo  = new HashMap<>();

	public static int my_version_number[]; //VN
	public static int my_ru_value[];
	public static int max_version_number[];
	public static int my_n_value[];
	public static int my_ds_value;
	public static int quorum_timer;
	public static int my_request_number;

	public static boolean write_lock_flag[];
	
	public static boolean request_fulfilled_flag[];
	public static boolean wait_for_read_latest_file[];
	public static boolean wait_for_write_latest_file[];
	public static boolean quorum_timer_off_flag[];
	public static boolean exp_backoff_done_flag[];
	
	public static int lock_server_flag[];
	public static int lock_client_flag[];
	
	//public static ConcurrentHashMap<Integer,Integer> readLockGrantHashmap  = new ConcurrentHashMap<>();
	public static ArrayList<ConcurrentHashMap<Integer,Integer>> readLockGrantHashmapList  = new ArrayList<>();
	
	//public static ConcurrentHashMap<Integer,Integer> readLockReceiveHashmap  = new ConcurrentHashMap<>();
	public static ArrayList<ConcurrentHashMap<Integer,Integer>> readLockReceiveHashmapList  = new ArrayList<>();
	
	//public static ConcurrentHashMap<Integer,Integer> writeLockGrantHashmap  = new ConcurrentHashMap<>();
	public static ArrayList<ConcurrentHashMap<Integer,Integer>> writeLockGrantHashmapList  = new ArrayList<>();
	
	//public static ConcurrentHashMap<Integer,Integer> writeLockReceiveHashmap  = new ConcurrentHashMap<>();
	public static ArrayList<ConcurrentHashMap<Integer,Integer>> writeLockReceiveHashmapList  = new ArrayList<>();
	
	//public static ConcurrentHashMap<Integer,Integer> versionNumberHashmap  = new ConcurrentHashMap<>();
	public static ArrayList<ConcurrentHashMap<Integer,Integer>> versionNumberHashmapList  = new ArrayList<>();	
	
	public static List<Integer> p_set = new ArrayList<Integer>();
	public static List<Integer> q_set = new ArrayList<Integer>();
	
	public static String filesystem_dir;
	public static String log_dir;
	public static String csviolation_dir;
	public static String exception_dir;

	public static int logical_timestamp=0;
	public static int[] vector_timestamp;
		
	public static String resource_file;
	public static int resource_file_num;
	public static String csviolation_file;
	public static String log_file;
	public static String exception_file;
	
	
	public static final int MAX_AVAILABLE = 1;
	//public static final Semaphore semaphore = new Semaphore(MAX_AVAILABLE, true);
	public static Semaphore[] semaphore;
	
	public static Queue<Packet> queue = new LinkedList<Packet>();
	
	public static String transport_protocol;
	
	public static String output_dir = "output";
	public static String output_file;
	public static String all_output_file = "all_output.txt";

	
	public static void main(String[] args) {
		
		try{
			
			String currentWorkingDir = new java.io.File( "." ).getCanonicalPath();
			System.out.println("Current Working Directory:"+currentWorkingDir);
			
			//Get Local Node ID from Command Line Argument
			//local_node_id = 0;
			local_node_id = Integer.parseInt(args[0]);
			
			//output_file = "out.txt";
			output_file = "output_node"+AOSProject3.local_node_id+".txt";
			write_output("Node:"+AOSProject3.local_node_id+" -- Output file:"+output_file,true);
					
			write_output("----------------------------------------------------------------",true);
			write_output("------------- AOSProject3 at Node: " + local_node_id + " -------------------------",true);
			write_output("----------------------------------------------------------------",true);
	
			// Get HostName
			InetAddress iAddress = InetAddress.getLocalHost();
		    String hostName = iAddress.getHostName();
		    //To get  the Canonical host name
		    String canonicalHostName = iAddress.getCanonicalHostName();
		
		    write_output("Node:"+AOSProject3.local_node_id+" -- HostName:" + hostName,true);
		    write_output("Node:"+AOSProject3.local_node_id+" -- Canonical Host Name:" + canonicalHostName,true);
			
		
			// Read Config File
		    write_output("Node:"+AOSProject3.local_node_id+" -- Reading Config file",true);
			read_config();
				
			
			//Read argument on which protocol to run
			transport_protocol = "TCP";
			if(args.length > 1)
			{
				transport_protocol = args[1];		
			}
			transport_protocol = transport_protocol.toUpperCase();		
			write_output("Node:"+AOSProject3.local_node_id+" -- Using Protocol:"+transport_protocol,true);
			
			
			
			//Initialize Algo variables
			my_ds_value = 0; // node_id = 0 is DS
			quorum_timer = 600; //milliseconds
			
			//Dir
			filesystem_dir = currentWorkingDir+"/filesystem/node"+local_node_id+"/";
			log_dir = currentWorkingDir+"/log/";
			csviolation_dir = currentWorkingDir+"/csviolation/";
			exception_dir = currentWorkingDir+"/exception/";
			
			//Initialize 
			semaphore = new Semaphore[no_of_files];
			write_lock_flag = new boolean[no_of_files];
			my_version_number = new int[no_of_files];
			my_ru_value = new int[no_of_files];
			max_version_number =  new int[no_of_files];
			my_n_value = new int[no_of_files];
			
			request_fulfilled_flag = new boolean[no_of_files];
			wait_for_read_latest_file = new boolean[no_of_files];
			wait_for_write_latest_file = new boolean[no_of_files];
			quorum_timer_off_flag = new boolean[no_of_files];
			exp_backoff_done_flag = new boolean[no_of_files];
			
			lock_server_flag = new int[no_of_files];
			lock_client_flag = new int[no_of_files];
			
			for (int i = 0; i < no_of_files; i++) {
				
				semaphore[i] = new Semaphore(MAX_AVAILABLE, true);
				
				readLockGrantHashmapList.add(new ConcurrentHashMap<Integer,Integer>());
				writeLockGrantHashmapList.add(new ConcurrentHashMap<Integer,Integer>());
				
				readLockReceiveHashmapList.add(new ConcurrentHashMap<Integer,Integer>());
				writeLockReceiveHashmapList.add(new ConcurrentHashMap<Integer,Integer>());
				
				versionNumberHashmapList.add(new ConcurrentHashMap<Integer,Integer>());
				
				write_lock_flag[i] = false;
				my_version_number[i] = 0;
				my_ru_value[i] = no_of_nodes;
				max_version_number[i] = 0;
				my_n_value[i] = no_of_nodes;
				
				request_fulfilled_flag[i] = false;
				wait_for_read_latest_file[i] = false;
				wait_for_write_latest_file[i] = false;
				quorum_timer_off_flag[i] = false;
				exp_backoff_done_flag[i] = false;
				
				lock_server_flag[i]=0;
				lock_client_flag[i]=0;
				
			}
			
			
			for (int i = 0; i < no_of_files; i++) 
			{
				// Initialize Hashmaps
				for (int j = 0; j < no_of_nodes; j++)
				{
					
					readLockGrantHashmapList.get(i).put(j, 0);
					writeLockGrantHashmapList.get(i).put(j, 0);
					
					readLockReceiveHashmapList.get(i).put(j, 0);
					writeLockReceiveHashmapList.get(i).put(j, 0);
					
					versionNumberHashmapList.get(i).put(j, 0);
					
				}
			}
			
			//Exception file
			exception_file = "exception"+local_node_id+".txt";
			
			// Start Server
			int server_node_port = NodeLocInfo.get(local_node_id).node_port;
			
			CommServer CommServerObj = new CommServer(server_node_port,transport_protocol);
			CommServerObj.start();
			
			write_output("Node:"+AOSProject3.local_node_id+" -- Started "+transport_protocol+" Server at Port:"+server_node_port,true);
			
			
			// Go to sleep for sometime so that all nodes are active before execution	
			try {
				Thread.sleep(4000);
			} catch (InterruptedException e) {
				AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - Main - Start Server",AOSProject3.exception_dir+AOSProject3.exception_file);
				e.printStackTrace();
			}
			
			
			
			
			
			//No. of Read Operations
			int op_read = (no_of_op * fraction_read)/100;
			
			//No. of Write Operations
			int op_write = no_of_op - op_read;
			
			int op_read_done = 0;
			int op_write_done = 0;
			
			Service srv = new Service();
			
			for (int i = 0; i < AOSProject3.no_of_op; i++) 
			{
	
				
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- Request Number:"+i,true);
				
				
				
				//Pick random file
				if(no_of_files>1)
				{
					resource_file_num = generate_random(no_of_files);
				}
				else
				{
					resource_file_num = 0;
				}
				
				resource_file =  "file"+resource_file_num+".txt";
				csviolation_file = "csviolation"+resource_file_num+".txt";
				log_file = "log"+resource_file_num+".txt";
				
				
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- Resource file:"+resource_file,true);
				
				//AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [aaa] -- Write Operation",true);
				//srv.write_operation(i);
				
							
				if((generate_random(20)%2==0))
				{														
					//Do Read Operation
					if(op_read_done < op_read)
					{
						AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Even] -- Read Operation",true);
						op_read_done = op_read_done + 1;
						srv.read_operation(i);
					}
					else if(op_write_done < op_write)
					{
						AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Even] -- Write Operation",true);
						op_write_done = op_write_done + 1;
						srv.write_operation(i);
					}
				}
				else
				{
					//Do Write Operation				
					if(op_write_done < op_write)
					{
						AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Odd] -- Write Operation",true);
						op_write_done = op_write_done + 1;
						srv.write_operation(i);
					}
					else if(op_read_done < op_read)
					{
						AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Odd] -- Read Operation",true);
						op_read_done = op_read_done + 1;
						srv.read_operation(i);
					}
				}			
				
				
				//Mean delay between two consecutive operations
				try {
					Thread.sleep(generateExponentialDist(AOSProject3.mean_delay));
				} catch (InterruptedException e) {
					AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - Main - Mean delay between CS",AOSProject3.exception_dir+AOSProject3.exception_file);
					e.printStackTrace();
				}
				
			}
			
			
		
				
		}
		catch(Exception e)
		{
			AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - Main ",AOSProject3.exception_dir+AOSProject3.exception_file);
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
			boolean line_no_of_files=false;			
			boolean line_no_of_op=false;
			boolean line_mean_delay=false;
			boolean line_fraction_read=false;
			boolean line_exp_backoff=false;
			
			br = new BufferedReader(new FileReader(configFile));
			
			while ((line = br.readLine()) != null) {
				
				//System.out.println(line);
				
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
				else if(line.contains("Numbers of files in the file system")==true)
				{
					line_location=false;
					
					line_no_of_files=true;
					continue;
				}
				else if(line.contains("Number of operations executed per node")==true)
				{							
					line_no_of_op=true;
					continue;
				}
				else if(line.contains("Mean delay between two consecutive operations (assume exponentially distributed)")==true)
				{							
					line_mean_delay=true;
					continue;
				}
				else if(line.contains("Fraction of read operations")==true)
				{							
					line_fraction_read=true;
					continue;
				}
				else if(line.contains("Parameters of exponential backoff (minimum and maximum waiting times)")==true)
				{							
					line_exp_backoff=true;
					continue;
				}
	 
				if(line.startsWith("#")==false || line.startsWith(" ")==false)
				{				
					if(line_no_of_nodes==true)
					{
						no_of_nodes = Integer.parseInt(line.trim());
						write_output("Read - No. of Nodes:"+no_of_nodes,true);
						line_no_of_nodes=false;
						vector_timestamp = new int[no_of_nodes];
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
					else if(line_no_of_files==true)
					{						
						no_of_files = Integer.parseInt(line.trim());
						write_output("Read - Numbers of files in the file system : "+no_of_files,true);
						line_no_of_files=false;
					}
					else if(line_no_of_op==true)
					{						
						no_of_op = Integer.parseInt(line.trim());
						write_output("Read - Number of operations executed per node : "+no_of_op,true);
						line_no_of_op=false;
					}
					else if(line_mean_delay==true)
					{						
						mean_delay = Integer.parseInt(line.trim());
						write_output("Read - Mean delay between two consecutive operations (assume exponentially distributed): "+mean_delay,true);
						line_mean_delay=false;
					}
					else if(line_fraction_read==true)
					{					
						fraction_read = Integer.parseInt(line.trim());
						write_output("Read - Fraction of read operations: "+fraction_read,true);
						line_fraction_read=false;
					}
					else if(line_exp_backoff==true)
					{					
						exp_backoff_min = Integer.parseInt(line.split("\t")[0].trim());
						exp_backoff_max = Integer.parseInt(line.split("\t")[1].trim());
						exp_last_max = exp_backoff_min;
						write_output("Read - Exponential Backoff Min: "+exp_backoff_min,true);
						write_output("Read - Exponential Backoff Max: "+exp_backoff_max,true);
						line_exp_backoff=false;
					}
						
				}
						
		
				line_no++;
			}
			

			System.out.println("\nNode "+AOSProject3.local_node_id+" Location Info:");
			for(Entry<Integer, NodeLoc> entry : NodeLocInfo.entrySet()){
				System.out.printf("Key : %s and Value: %s %n", entry.getKey(), entry.getValue().node_id+"\t"+entry.getValue().node_name+"\t"+entry.getValue().node_port);
		    }
			
			
						
	 
		} catch (FileNotFoundException e) {
			AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - Main - read_config1",AOSProject3.exception_dir+AOSProject3.exception_file);
			e.printStackTrace();
		} catch (IOException e) {
			AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - Main - read_config2",AOSProject3.exception_dir+AOSProject3.exception_file);
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - Main - read_config3",AOSProject3.exception_dir+AOSProject3.exception_file);
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
		    
		    //pw2 = new PrintWriter(new BufferedWriter(new FileWriter(output_dir + "/" + all_output_file, true)));
		    //pw2.println(stmt);
		    
		    if(show_console==true)
		    {
		    	System.out.println(stmt);
		    }
		    
		}catch (IOException e) {
			AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - Main - write_output",AOSProject3.exception_dir+AOSProject3.exception_file);
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
		/*
		try {
			Thread.sleep(generate_random(20));
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}*/
		
		PrintWriter pw = null;

		try {
		    pw = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)));
		    pw.println(stmt);		    		    
		    
		}catch (IOException e) {
			AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - Main - write_to_file",AOSProject3.exception_dir+AOSProject3.exception_file);
		    System.err.println(e);
		}finally{
		    if(pw != null){
		    	pw.close();
		    }

		}
		
	}
	
	public static int exponential_backoff()
	{
		AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- exponential_backoff() called",true);
		
		int expbackoffval = generate_random(exp_last_max);
		
		//Sleep
		try {
			Thread.sleep(expbackoffval);
		} catch (InterruptedException e) {
			AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - Main Exponential Backoff",AOSProject3.exception_dir+AOSProject3.exception_file);
			e.printStackTrace();
		}
			
		System.out.println("Backoff Val:"+expbackoffval+" | Max Val:"+exp_last_max);

		if(exp_last_max < exp_backoff_max)
		{
			exp_last_max = exp_last_max * 2;
		}
		
		return expbackoffval;
	}
	
	
	public static int generate_random(int val) 
	{
		Random randomGenerator = new Random();
		int randomInt = randomGenerator.nextInt(val);
		//if(randomInt == 0)
		//{
		//	return generate_random(val);
		//}
		return randomInt;
	}
	
	public static byte[] fileToByteArray(String filename,String filepath) 
	{
		
		FileInputStream fileInputStream=null;
		 
        File fileObj = new File(filepath+filename);
 
        byte[] fileByteArray = new byte[(int) fileObj.length()];
 
        try {
        	//convert file into array of bytes
        	fileInputStream = new FileInputStream(fileObj);
        	fileInputStream.read(fileByteArray);
        	fileInputStream.close();
 
        	/*
        	for (int i = 0; i < fileByteArray.length; i++) {
        		System.out.print((char)fileByteArray[i]);
            }
 
        	System.out.println("Done");
        	*/
        	
        }catch(Exception e){
        	AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - Main - fileToByteArray",AOSProject3.exception_dir+AOSProject3.exception_file);
        	e.printStackTrace();
        }
		
		return fileByteArray;
	}
	
	public static String byteArrayToFile(byte[] fileByteArray,String filename,String filepath) 
	{
				        
        try {
        	
        	File fileObj = new File(filepath+filename);
            FileOutputStream fos = new FileOutputStream(fileObj);
			fos.write(fileByteArray);
			fos.flush();
	        fos.close();
	        
		} catch (IOException e) {
			AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - Main - byteArrayToFile",AOSProject3.exception_dir+AOSProject3.exception_file);
			e.printStackTrace();
		}
        
        return filename;
        
	}
	
	public static void semaphore_lock(String client_or_server, String location, int resource_file_num_param)
	{
		if(client_or_server == "server")
		{
			//if(AOSProject3.lock_server_flag == 0 && AOSProject3.lock_server_flag == 0)
			if(AOSProject3.lock_server_flag[resource_file_num_param] == 0)
			{
			
				//Acquire semaphore lock
				try {
					semaphore[resource_file_num_param].acquire();
				} catch (InterruptedException e) {
					AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - Main - semaphore_lock",AOSProject3.exception_dir+AOSProject3.exception_file);
					e.printStackTrace();
				}
				
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- Semaphore["+resource_file_num_param+"] locked by: "+client_or_server+", Location: "+location,true);
				
				AOSProject3.lock_server_flag[resource_file_num_param] = AOSProject3.lock_server_flag[resource_file_num_param] + 1;
				
			}
			else
			{
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- Error : (Lock) Server Lock Val: "+AOSProject3.lock_server_flag[resource_file_num_param]+", Location: "+location,true);
			}
		}
		else if(client_or_server == "client")
		{
			//if(AOSProject3.lock_client_flag == 0 && AOSProject3.lock_server_flag == 0)
			if(AOSProject3.lock_client_flag[resource_file_num_param] == 0)
			{
			
				//Acquire semaphore lock
				try {
					semaphore[resource_file_num_param].acquire();
				} catch (InterruptedException e) {
					AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - Main - semaphore_lock",AOSProject3.exception_dir+AOSProject3.exception_file);
					e.printStackTrace();
				}
				
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- Semaphore["+resource_file_num_param+"] locked by: "+client_or_server+", Location: "+location,true);
				
				AOSProject3.lock_client_flag[resource_file_num_param] = AOSProject3.lock_client_flag[resource_file_num_param] + 1;
				
			}
			else
			{
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- Error : (Lock) Client Lock Val: "+AOSProject3.lock_client_flag[resource_file_num_param]+", Location: "+location,true);
			}
		}
	}
	
	public static void semaphore_unlock(String client_or_server, String location, int resource_file_num_param)
	{
		if(client_or_server == "server")
		{
			//if(AOSProject3.lock_server_flag == 1 && AOSProject3.lock_client_flag == 0)
			if(AOSProject3.lock_server_flag[resource_file_num_param] == 1)
			{
				//Release semaphore lock
				semaphore[resource_file_num_param].release();
				
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- Semaphore["+resource_file_num_param+"] released by: "+client_or_server+", Location: "+location,true);
				AOSProject3.lock_server_flag[resource_file_num_param] = AOSProject3.lock_server_flag[resource_file_num_param] - 1;
			}
			else
			{
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- Error : (Unlock) Server Lock Val: "+AOSProject3.lock_server_flag[resource_file_num_param]+", Location: "+location,true);
			}
		}
		else if(client_or_server == "client")
		{
			//if(AOSProject3.lock_client_flag == 1  && AOSProject3.lock_server_flag == 0)
			if(AOSProject3.lock_client_flag[resource_file_num_param] == 1)
			{
				//Release semaphore lock
				semaphore[resource_file_num_param].release();
				
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- Semaphore["+resource_file_num_param+"] released by: "+client_or_server+", Location: "+location,true);
				AOSProject3.lock_client_flag[resource_file_num_param] = AOSProject3.lock_client_flag[resource_file_num_param] - 1;
			}
			else
			{
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- Error : (Unlock) Client Lock Val: "+AOSProject3.lock_client_flag[resource_file_num_param]+", Location: "+location,true);
			}
		}
		
	}
	
	
	public static String file_read_line(String fileWithPath)
	{
				
		BufferedReader br = null;
		String line = "";
	 
		try {
	 
				
			br = new BufferedReader(new FileReader(fileWithPath));
			
			while ((line = br.readLine()) != null) {
						
				break;
			}
			
 
		} catch (FileNotFoundException e) {
			AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - Main - file_read_line1",AOSProject3.exception_dir+AOSProject3.exception_file);
			e.printStackTrace();
		} catch (IOException e) {
			AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - Main - file_read_line2",AOSProject3.exception_dir+AOSProject3.exception_file);
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - Main - file_read_line3",AOSProject3.exception_dir+AOSProject3.exception_file);
					e.printStackTrace();
				}
			}
		}
		// Read Config File - End
			

		return line;
	}
	
}
