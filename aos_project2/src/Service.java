import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;



public class Service {
	
	public void cs_enter()
	{
		//Set CS flag
		AOSProject2.cs_request_flag=true;
				
		//Acquire semaphore lock
		try {
			AOSProject2.semaphore.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		for(int i = 0; i < AOSProject2.NodeKeysInfo.size(); i++) 
		{
			
			int client_key_val = AOSProject2.NodeKeysInfo.get(i);
			
			if(client_key_val==0 && (i!=AOSProject2.local_node_id))
			{
				int client_node_id = AOSProject2.NodeLocInfo.get(i).node_id;
				String client_node_name = AOSProject2.NodeLocInfo.get(i).node_name;
				int client_node_port = AOSProject2.NodeLocInfo.get(i).node_port;
				
				// Increment logical timestamp
				AOSProject2.logical_timestamp = AOSProject2.logical_timestamp + 1;
				
				// Increment vector timestamp
				AOSProject2.vector_timestamp[AOSProject2.local_node_id] = AOSProject2.vector_timestamp[AOSProject2.local_node_id] + 1;
				
				Packet packetObj = new Packet();
				packetObj.packet_type = "MSG_CS_REQUEST";
				packetObj.sender_id = AOSProject2.local_node_id;
				packetObj.receiver_node_id = client_node_id;
				packetObj.receiver_node_name = client_node_name;
				packetObj.logical_timestamp = AOSProject2.logical_timestamp;
				packetObj.request_timestamp = AOSProject2.request_timestamp;
				packetObj.vector_timestamp = AOSProject2.vector_timestamp;
				packetObj.request_vector_timestamp = AOSProject2.request_vector_timestamp;
				
				// Start Client
				CommClient CommClientObj = new CommClient(AOSProject2.transport_protocol);
				CommClientObj.go(client_node_id,client_node_name,client_node_port,packetObj);
			}
		
		}
		
		//Release semaphore lock
		AOSProject2.semaphore.release();
		
		while(true)
		{
			// Check for keys
			boolean check_keys_bool = check_for_keys();
			
			if(check_keys_bool==true)
			{

				cs_in_use();
							
				break;
				
			}
		
		}
		
		
		
	}
	
	public boolean check_for_keys()
	{
		
		//Acquire semaphore lock
		try {
			AOSProject2.semaphore.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// Check if I received key from everybody
		boolean received_all_keys = true;
		
		for(int i = 0; i < AOSProject2.NodeKeysInfo.size(); i++) 
		{
			int client_key_val = AOSProject2.NodeKeysInfo.get(i);
			
			if(client_key_val == 0 && (i!=AOSProject2.local_node_id))
			{
				received_all_keys = false;
				break;
			}			

		}
			
		//Release semaphore lock
		AOSProject2.semaphore.release();
		
		return received_all_keys;

	}
	
	public void cs_in_use()
	{
		//Node entered
		
		//Set CS Section flag
		AOSProject2.cs_section_flag=true;
		
		//Create lock file - resource.lck	
		File lockfile = new File(AOSProject2.resource_file+".lck");
		 
	    try {
			if(lockfile.createNewFile()){
				AOSProject2.write_output("Node:"+AOSProject2.local_node_id+" - Lock file created!",true);
			    
			    //Node enters CS
			    
			    //Open resource file
			    PrintWriter pw = null;
				try {
				    pw = new PrintWriter(new BufferedWriter(new FileWriter(AOSProject2.resource_file, true)));
				    
				    pw.println("Node:"+AOSProject2.local_node_id+" entered CS#"+AOSProject2.logical_timestamp+"#"+Arrays.toString(AOSProject2.vector_timestamp));			    
				    AOSProject2.write_output("Node:"+AOSProject2.local_node_id+" entered CS#"+AOSProject2.logical_timestamp+"#"+Arrays.toString(AOSProject2.vector_timestamp),true);
				    				    
				    AOSProject2.write_output("Node:"+AOSProject2.local_node_id+" - KeyHashMap:"+AOSProject2.NodeKeysInfo.toString(),true);
				    
				    //Mean Duration of CS
				    try {
						Thread.sleep(AOSProject2.generateExponentialDist(AOSProject2.mean_duration));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				    
				    pw.println("Node:"+AOSProject2.local_node_id+" left CS   #"+AOSProject2.logical_timestamp+"#"+Arrays.toString(AOSProject2.vector_timestamp));
				    AOSProject2.write_output("Node:"+AOSProject2.local_node_id+" left CS   #"+AOSProject2.logical_timestamp+"#"+Arrays.toString(AOSProject2.vector_timestamp),true);
				    
				}catch (IOException e) {
				    System.err.println(e);
				}finally{
				    if(pw != null){
				    	//Close resource file
				    	pw.close();
				    }
				}
			    
						    
			    //Delete lock file
			    lockfile.delete();
			    AOSProject2.write_output("Node:"+AOSProject2.local_node_id+" - Lock file deleted!",true);
			    
			    //Reset CS Section flag
				AOSProject2.cs_section_flag=false;
			    
				//Node left CS
			    
			}else{
				AOSProject2.write_output("Node:"+AOSProject2.local_node_id+" - Lock file already exists.",true);
				AOSProject2.write_output("Node:"+AOSProject2.local_node_id+" - CRITICAL SECTION VIOLATION !!!",true);
				
				AOSProject2.write_to_file("Node:"+AOSProject2.local_node_id+" LogTS:"+AOSProject2.logical_timestamp+" | VecTS:"+Arrays.toString(AOSProject2.vector_timestamp),AOSProject2.csviolation_file);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		
		//Node left
	    
	    cs_leave();
	
	}
	
	public void cs_leave()
	{
		//Reset CS flag
		AOSProject2.cs_request_flag=false;
		
		//Acquire semaphore lock
		try {
			AOSProject2.semaphore.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		while(!AOSProject2.queue.isEmpty())
		{
			
			Packet pktObj = AOSProject2.queue.poll();
			
			int node_id = pktObj.sender_id;
			
			AOSProject2.write_output("Node:"+AOSProject2.local_node_id+" - Node:"+node_id+" removed from queue",true);
						
			//Send key
			send_key(node_id);
		}
		
		//Mean Delay between CS requests
		try {
			Thread.sleep(AOSProject2.generateExponentialDist(AOSProject2.mean_delay));
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 
		
		//Release semaphore lock
		AOSProject2.semaphore.release();
	}

	public void send_key(int client_node_id)
	{
		// Increment logical timestamp
		AOSProject2.logical_timestamp = AOSProject2.logical_timestamp + 1;
		
		// Increment vector timestamp
		AOSProject2.vector_timestamp[AOSProject2.local_node_id] = AOSProject2.vector_timestamp[AOSProject2.local_node_id] + 1;
		
		
		AOSProject2.write_output("Node:"+AOSProject2.local_node_id+" [send_key - before] - KeyHashMap:"+AOSProject2.NodeKeysInfo.toString(),true);
				
		String client_node_name = AOSProject2.NodeLocInfo.get(client_node_id).node_name;
		int client_node_port = AOSProject2.NodeLocInfo.get(client_node_id).node_port;
			
		Packet packetObj = new Packet();
		packetObj.packet_type = "MSG_SEND_KEY";
		packetObj.sender_id = AOSProject2.local_node_id;
		packetObj.receiver_node_id = client_node_id;
		packetObj.receiver_node_name = client_node_name;
		packetObj.logical_timestamp = AOSProject2.logical_timestamp;
		packetObj.request_timestamp = AOSProject2.request_timestamp;
		packetObj.vector_timestamp = AOSProject2.vector_timestamp;
		packetObj.request_vector_timestamp = AOSProject2.request_vector_timestamp;
			
		// Start Client
		CommClient CommClientObj = new CommClient(AOSProject2.transport_protocol);
		CommClientObj.go(client_node_id,client_node_name,client_node_port,packetObj);
		

		// Reset Key
		AOSProject2.NodeKeysInfo.put(client_node_id, 0);
				

		AOSProject2.write_output("Node:"+AOSProject2.local_node_id+" [send_key - after] - KeyHashMap:"+AOSProject2.NodeKeysInfo.toString(),true);
		
	}
	
	public void request_key(int client_node_id)
	{
		// Increment logical timestamp
		AOSProject2.logical_timestamp = AOSProject2.logical_timestamp + 1;
		
		// Increment vector timestamp
		AOSProject2.vector_timestamp[AOSProject2.local_node_id] = AOSProject2.vector_timestamp[AOSProject2.local_node_id] + 1;
		
		AOSProject2.write_output("Node:"+AOSProject2.local_node_id+" [request_key] - KeyHashMap:"+AOSProject2.NodeKeysInfo.toString(),true);
				
		String client_node_name = AOSProject2.NodeLocInfo.get(client_node_id).node_name;
		int client_node_port = AOSProject2.NodeLocInfo.get(client_node_id).node_port;
		
		Packet packetObj = new Packet();
		packetObj.packet_type = "MSG_REQUEST_KEY";
		packetObj.sender_id = AOSProject2.local_node_id;
		packetObj.receiver_node_id = client_node_id;
		packetObj.receiver_node_name = client_node_name;
		packetObj.logical_timestamp = AOSProject2.logical_timestamp;
		packetObj.request_timestamp = AOSProject2.request_timestamp;
		packetObj.vector_timestamp = AOSProject2.vector_timestamp;
		packetObj.request_vector_timestamp = AOSProject2.request_vector_timestamp;
		
		// Start Client
		CommClient CommClientObj = new CommClient(AOSProject2.transport_protocol);
		CommClientObj.go(client_node_id,client_node_name,client_node_port,packetObj);					
		
	}
}
