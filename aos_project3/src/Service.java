import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;



public class Service {
	

	public void read_operation(int request_number)
	{
		//Request Number
		AOSProject3.my_request_number = request_number;
		
		while(true) // Can't begin if cannot acquire lock over my own replica and to handle Exponential Backoff
		{
			//Lock Semaphore
			AOSProject3.semaphore_lock("client","[Read] Starting Read operation",AOSProject3.resource_file_num);
			
			System.out.println("Node:"+AOSProject3.local_node_id+" -- [Read] -- While true");
			//AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Read] -- While true",true);
			
			if(AOSProject3.write_lock_flag[AOSProject3.resource_file_num] == false)
			{
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Read] -- Operation started",true);
				
				//Set readLockReceiveHashmap for yourself
				AOSProject3.readLockReceiveHashmapList.get(AOSProject3.resource_file_num).put(AOSProject3.local_node_id, 1);	
				
				//Set readLockGrantHashmap for yourself
				AOSProject3.readLockGrantHashmapList.get(AOSProject3.resource_file_num).put(AOSProject3.local_node_id, 1);	
				
				//Reset Quorum Timer flag
				AOSProject3.quorum_timer_off_flag[AOSProject3.resource_file_num] = false;
				
				//Reset request_fulfilled_flag
				AOSProject3.request_fulfilled_flag[AOSProject3.resource_file_num] = false;
			
				//Store Max version number
				AOSProject3.max_version_number[AOSProject3.resource_file_num] = AOSProject3.my_version_number[AOSProject3.resource_file_num];
				
				//Store Max version number from packet
				AOSProject3.versionNumberHashmapList.get(AOSProject3.resource_file_num).put(AOSProject3.local_node_id, AOSProject3.my_version_number[AOSProject3.resource_file_num]);
					
				//Set value of N
				AOSProject3.my_n_value[AOSProject3.resource_file_num] = AOSProject3.my_ru_value[AOSProject3.resource_file_num];
				
				//Send MSG_REQUEST_READ_LOCK to all nodes
				for (int i = 0; i < AOSProject3.no_of_nodes; i++) 
				{
					if(AOSProject3.local_node_id != i) // Don't send to yourself
					{
						int client_node_id = AOSProject3.NodeLocInfo.get(i).node_id;
						String client_node_name = AOSProject3.NodeLocInfo.get(i).node_name;
						int client_node_port = AOSProject3.NodeLocInfo.get(i).node_port;
						
						Packet packetObj = new Packet();
						packetObj.packet_type = "MSG_REQUEST_READ_LOCK";
						packetObj.sender_id = AOSProject3.local_node_id;
						packetObj.receiver_node_id = client_node_id;
						packetObj.receiver_node_name = client_node_name;	
						packetObj.logical_timestamp = AOSProject3.logical_timestamp;
						packetObj.vector_timestamp = AOSProject3.vector_timestamp;
						packetObj.request_number = AOSProject3.my_request_number;
						
						packetObj.ru_value = AOSProject3.my_ru_value[AOSProject3.resource_file_num];
						packetObj.version_number = AOSProject3.my_version_number[AOSProject3.resource_file_num];
						packetObj.file_num = AOSProject3.resource_file_num;
					
						// Start Client
						CommClient CommClientObj = new CommClient(AOSProject3.transport_protocol);
						CommClientObj.go(client_node_id,client_node_name,client_node_port,packetObj);
					}
				}
				
				//Unlock Semaphore
				AOSProject3.semaphore_unlock("client","[Read] Timer On",AOSProject3.resource_file_num);
				
				//Timer
				try {
					Thread.sleep(AOSProject3.quorum_timer);
				} catch (InterruptedException e) {
					AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - Client - Read Quorum Timer",AOSProject3.exception_dir+AOSProject3.exception_file);
					e.printStackTrace();
				}
				//Set Quorum timer flag
				AOSProject3.quorum_timer_off_flag[AOSProject3.resource_file_num] = true;
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Read] -- Timer over",true);
				
				//Lock Semaphore
				AOSProject3.semaphore_lock("client","[Read] Timer Off",AOSProject3.resource_file_num);
				
				//Check Hashmap if lock has been received
				for(int i = 0; i < AOSProject3.readLockReceiveHashmapList.get(AOSProject3.resource_file_num).size(); i++) 
				{
					
					int read_lock_val = AOSProject3.readLockReceiveHashmapList.get(AOSProject3.resource_file_num).get(i);						
					
					//Create P Set
					if(read_lock_val == 1)
					{
						AOSProject3.p_set.add(i);
					}
					
					//Create Q Set
					if( (AOSProject3.p_set.contains(i)==true) && (AOSProject3.versionNumberHashmapList.get(AOSProject3.resource_file_num).get(i)==AOSProject3.max_version_number[AOSProject3.resource_file_num]))
					{
						AOSProject3.q_set.add(i);
					}
					
				}	
				
				//P set size
				int p_set_size = AOSProject3.p_set.size();
				
				//Q set size
				int q_set_size = AOSProject3.q_set.size();
				double q_set_size_double = AOSProject3.q_set.size();
				
				
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Read] -- P-Set:"+AOSProject3.p_set.toString()+" P-Set Size:"+p_set_size,true);
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Read] -- Q-Set:"+AOSProject3.q_set.toString()+" Q-Set Size:"+q_set_size,true);	
				
				
				//N value as double
				double my_n_value_double = (double) AOSProject3.my_n_value[AOSProject3.resource_file_num];
				
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Read] -- QSize:"+q_set_size_double+", N:"+my_n_value_double+", [N/2]:"+(my_n_value_double/2),true);
			
				
				if(q_set_size_double > (my_n_value_double/2) ) // TEST1 - GT
				{
					//Got Quorum - So enter CS
					AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Read] -- Got Quorum - TEST1 - GT",true);
					
					//Perform Quorum Operations
					quorum_read_operations();
			
				}
				else if( (q_set_size_double == (my_n_value_double/2)) && (AOSProject3.q_set.contains(AOSProject3.my_ds_value)==true) ) // TEST2 - EQ
				{
					//Got Quorum - So enter CS
					AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Read] -- Got Quorum - TEST2 - EQ",true);
					
					//Perform Quorum Operations
					quorum_read_operations();																	
				}
				else
				{
					//Didnt get quorum
					AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Read] -- Didnt get quorum",true);
					
					//Flush P and Q set
					AOSProject3.p_set.clear();
					AOSProject3.q_set.clear();
					
					//Reset versionNumberHashmap
					reset_version_number_hashmap();
					
					//Reset readLockReceiveHashmap for yourself
					AOSProject3.readLockReceiveHashmapList.get(AOSProject3.resource_file_num).put(AOSProject3.local_node_id,0);
					
					//Reset readLockGrantHashmap for yourself
					AOSProject3.readLockGrantHashmapList.get(AOSProject3.resource_file_num).put(AOSProject3.local_node_id,0);
										
					//Release Read Lock
					release_read_lock();
					
					//Exponential BackOff done
					AOSProject3.exp_backoff_done_flag[AOSProject3.resource_file_num] = true;
					
					//Unlock Semaphore
					AOSProject3.semaphore_unlock("client","[Read] Exponential Backoff",AOSProject3.resource_file_num);
					
					//So do Exponential Backoff
					AOSProject3.exponential_backoff();
					
					//Try again
					//While(true) will handle it		
				}
											
						
				if(AOSProject3.request_fulfilled_flag[AOSProject3.resource_file_num] == true)
				{
					AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Read] -- Request fulfilled",true);
					
					//Flush P and Q set
					AOSProject3.p_set.clear();
					AOSProject3.q_set.clear();
					
					//No need to Update File, VN Value, RU value in P set
										
					//No need to Send MSG_UPDATED_FILE to P set
					
					
					//Reset flag
					AOSProject3.request_fulfilled_flag[AOSProject3.resource_file_num] = false;
					
					//Reset readLockReceiveHashmap for yourself
					AOSProject3.readLockReceiveHashmapList.get(AOSProject3.resource_file_num).put(AOSProject3.local_node_id,0);
					
					//Reset readLockGrantHashmap for yourself
					AOSProject3.readLockGrantHashmapList.get(AOSProject3.resource_file_num).put(AOSProject3.local_node_id,0);
					
					//Release Read Lock
					release_read_lock();
					
					//Request number
					AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- Request Number:"+request_number+" Fulfilled",true);
					
					//Unlock Semaphore
					AOSProject3.semaphore_unlock("client","[Read] Request fulfilled",AOSProject3.resource_file_num);
					
					//Once CS request has been fulfilled, break the loop
					break;
				}
				
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Read] -- Request NOT yet fulfilled",true);
			}
			
			if(AOSProject3.exp_backoff_done_flag[AOSProject3.resource_file_num] == false)
			{
				//Unlock Semaphore
				AOSProject3.semaphore_unlock("client","[Read] Unable to lock own copy",AOSProject3.resource_file_num);
			}
			else
			{
				AOSProject3.exp_backoff_done_flag[AOSProject3.resource_file_num] = false;
			}
			
			
			
		}
		
		
	}
	
	
	
	public void write_operation(int request_number)
	{
		
		//Request Number
		AOSProject3.my_request_number = request_number;
		
		while(true) // Can't begin if cannot acquire lock over my own replica and to handle Exponential Backoff
		{
			//Lock Semaphore
			AOSProject3.semaphore_lock("client","[Write] Starting Write operation",AOSProject3.resource_file_num);
			
			System.out.println("Node:"+AOSProject3.local_node_id+" -- [Write] -- While true");
			//AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Write] -- While true",true);
					
			if(AOSProject3.write_lock_flag[AOSProject3.resource_file_num] == false && (check_read_grant_hashmap_set(AOSProject3.resource_file_num)==false) )
			{
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Write] -- Operation started",true);
										
				//Lock local copy
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- C1 - WLF should be false, next operation true, write operation starting (Client call) -- WriteLockFlag: "+AOSProject3.write_lock_flag[AOSProject3.resource_file_num],true);
				AOSProject3.write_lock_flag[AOSProject3.resource_file_num]=true;			
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- C1 - WLF should be true, write operation starting  (Client call) -- WriteLockFlag: "+AOSProject3.write_lock_flag[AOSProject3.resource_file_num],true);
				
				//Set writeLockReceiveHashmap for yourself
				AOSProject3.writeLockReceiveHashmapList.get(AOSProject3.resource_file_num).put(AOSProject3.local_node_id, 1);				

				//Set writeLockGrantHashmap for yourself
				AOSProject3.writeLockGrantHashmapList.get(AOSProject3.resource_file_num).put(AOSProject3.local_node_id, 1);	

				//Reset Quorum Timer flag
				AOSProject3.quorum_timer_off_flag[AOSProject3.resource_file_num] = false;
			
				//Reset request_fulfilled_flag
				AOSProject3.request_fulfilled_flag[AOSProject3.resource_file_num] = false;
												
				//Store Max version number
				AOSProject3.max_version_number[AOSProject3.resource_file_num] = AOSProject3.my_version_number[AOSProject3.resource_file_num];
				
				//Store Max version number from packet
				AOSProject3.versionNumberHashmapList.get(AOSProject3.resource_file_num).put(AOSProject3.local_node_id, AOSProject3.my_version_number[AOSProject3.resource_file_num]);
				
				//Set value of N
				AOSProject3.my_n_value[AOSProject3.resource_file_num] = AOSProject3.my_ru_value[AOSProject3.resource_file_num];
				
				
				//Send MSG_REQUEST_WRITE_LOCK to all nodes
				for (int i = 0; i < AOSProject3.no_of_nodes; i++) 
				{
					if(AOSProject3.local_node_id != i) // Don't send to yourself
					{
						int client_node_id = AOSProject3.NodeLocInfo.get(i).node_id;
						String client_node_name = AOSProject3.NodeLocInfo.get(i).node_name;
						int client_node_port = AOSProject3.NodeLocInfo.get(i).node_port;
						
						Packet packetObj = new Packet();
						packetObj.packet_type = "MSG_REQUEST_WRITE_LOCK";
						packetObj.sender_id = AOSProject3.local_node_id;
						packetObj.receiver_node_id = client_node_id;
						packetObj.receiver_node_name = client_node_name;
						packetObj.logical_timestamp = AOSProject3.logical_timestamp;
						packetObj.vector_timestamp = AOSProject3.vector_timestamp;
						packetObj.request_number = AOSProject3.my_request_number;
						
						packetObj.ru_value = AOSProject3.my_ru_value[AOSProject3.resource_file_num];
						packetObj.version_number = AOSProject3.my_version_number[AOSProject3.resource_file_num];
						packetObj.file_num = AOSProject3.resource_file_num;
						
						// Start Client
						CommClient CommClientObj = new CommClient(AOSProject3.transport_protocol);
						CommClientObj.go(client_node_id,client_node_name,client_node_port,packetObj);
					}
				}
				
				//Unlock Semaphore
				AOSProject3.semaphore_unlock("client","[Write] Timer On",AOSProject3.resource_file_num);
				
				//Timer
				try {
					Thread.sleep(AOSProject3.quorum_timer);
				} catch (InterruptedException e) {
					AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - Client - Write Quorum Timer",AOSProject3.exception_dir+AOSProject3.exception_file);
					e.printStackTrace();
				}
				//Set Quorum timer flag
				AOSProject3.quorum_timer_off_flag[AOSProject3.resource_file_num] = true;
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Write] -- Timer over",true);
				
				//Lock Semaphore
				AOSProject3.semaphore_lock("client","[Write] Timer Off",AOSProject3.resource_file_num);
				
				//Check Hashmap if lock has been received
				for(int i = 0; i < AOSProject3.writeLockReceiveHashmapList.get(AOSProject3.resource_file_num).size(); i++) 
				{
					
					int write_lock_val = AOSProject3.writeLockReceiveHashmapList.get(AOSProject3.resource_file_num).get(i);
					
					//Create P Set
					if(write_lock_val == 1)
					{
						AOSProject3.p_set.add(i);
					}					

					//Create Q Set
					if( (AOSProject3.p_set.contains(i)==true) && (AOSProject3.versionNumberHashmapList.get(AOSProject3.resource_file_num).get(i)==AOSProject3.max_version_number[AOSProject3.resource_file_num]))
					{
						AOSProject3.q_set.add(i);
					}					
						
				}
				
				//P set size
				int p_set_size = AOSProject3.p_set.size();
				
				//Q set size
				int q_set_size = AOSProject3.q_set.size();
				double q_set_size_double = AOSProject3.q_set.size();
				
				
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Write] -- P-Set:"+AOSProject3.p_set.toString()+" P-Set Size:"+p_set_size,true);
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Write] -- Q-Set:"+AOSProject3.q_set.toString()+" Q-Set Size:"+q_set_size,true);	
				
				
				//N value as double
				double my_n_value_double = (double) AOSProject3.my_n_value[AOSProject3.resource_file_num];
				
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Write] -- QSize:"+q_set_size_double+", N:"+my_n_value_double+", [N/2]:"+(my_n_value_double/2),true);
			
				if(q_set_size_double > (my_n_value_double/2) ) //TEST1 - GT
				{
					//Got Quorum - So enter CS
					AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Write] -- Got Quorum - TEST1 - GT",true);
					
					//Perform Quorum Operations
					quorum_write_operations();

				}
				else if( (q_set_size_double == (my_n_value_double/2)) && (AOSProject3.q_set.contains(AOSProject3.my_ds_value)==true) ) //TEST2 - EQ
				{
					//Got Quorum - So enter CS
					AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Write] -- Got Quorum - TEST2 - EQ",true);
					
					//Perform Quorum Operations
					quorum_write_operations();				
					
				}
				else
				{
					//Didnt get quorum
					AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Write] -- Didnt get quorum",true);
					
					//Flush P and Q set
					AOSProject3.p_set.clear();
					AOSProject3.q_set.clear();						
					
					//Reset versionNumberHashmap
					reset_version_number_hashmap();
					
					//Reset writeLockReceiveHashmap for yourself
					AOSProject3.writeLockReceiveHashmapList.get(AOSProject3.resource_file_num).put(AOSProject3.local_node_id,0);
					
					//Reset writeLockGrantHashmap for yourself
					AOSProject3.writeLockGrantHashmapList.get(AOSProject3.resource_file_num).put(AOSProject3.local_node_id,0);
					
					//Release Write Lock
					release_write_lock(AOSProject3.resource_file_num);						
				
					//Unlock local copy
					AOSProject3.write_lock_flag[AOSProject3.resource_file_num] = false;
					
					//Exponential BackOff done
					AOSProject3.exp_backoff_done_flag[AOSProject3.resource_file_num] = true;
					
					//Unlock Semaphore
					AOSProject3.semaphore_unlock("client","[Write] Exponential Backoff",AOSProject3.resource_file_num);
																	
					//So do Exponential Backoff
					AOSProject3.exponential_backoff();
					
					//Try again
					//While(true) will handle it
					
				}
								
						
				if(AOSProject3.request_fulfilled_flag[AOSProject3.resource_file_num] == true)
				{
					AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Write] -- Request fulfilled",true);
													
					//Update File, VN Value, RU value in P set
					
					//Update RU Value
					AOSProject3.my_ru_value[AOSProject3.resource_file_num] = AOSProject3.p_set.size();
					
					//Increment Version Number
					AOSProject3.my_version_number[AOSProject3.resource_file_num] = AOSProject3.max_version_number[AOSProject3.resource_file_num] + 1;
					
					//Send MSG_UPDATED_FILE to P set
					for (int i = 0; i < AOSProject3.p_set.size(); i++) 
					{
					
						if(AOSProject3.local_node_id != AOSProject3.p_set.get(i)) // Don't send to yourself
						{
							int client_node_id = AOSProject3.NodeLocInfo.get(AOSProject3.p_set.get(i)).node_id;
							String client_node_name = AOSProject3.NodeLocInfo.get(AOSProject3.p_set.get(i)).node_name;
							int client_node_port = AOSProject3.NodeLocInfo.get(AOSProject3.p_set.get(i)).node_port;
							
							// Increment logical timestamp
							AOSProject3.logical_timestamp = AOSProject3.logical_timestamp + 1;
							
							// Increment vector timestamp
							AOSProject3.vector_timestamp[AOSProject3.local_node_id] = AOSProject3.vector_timestamp[AOSProject3.local_node_id] + 1;
							
							Packet packetObj = new Packet();
							packetObj.packet_type = "MSG_SEND_UPDATED_FILE";
							packetObj.sender_id = AOSProject3.local_node_id;
							packetObj.receiver_node_id = client_node_id;
							packetObj.receiver_node_name = client_node_name;
							packetObj.logical_timestamp = AOSProject3.logical_timestamp;
							packetObj.vector_timestamp = AOSProject3.vector_timestamp;
							packetObj.request_number = AOSProject3.my_request_number;
							
							packetObj.ru_value = AOSProject3.my_ru_value[AOSProject3.resource_file_num];
							packetObj.version_number = AOSProject3.my_version_number[AOSProject3.resource_file_num];
							packetObj.file_num = AOSProject3.resource_file_num;
							
							packetObj.file_name = AOSProject3.resource_file;
							packetObj.file_contents = AOSProject3.fileToByteArray(AOSProject3.resource_file,AOSProject3.filesystem_dir);
	
							// Start Client
							CommClient CommClientObj = new CommClient(AOSProject3.transport_protocol);
							CommClientObj.go(client_node_id,client_node_name,client_node_port,packetObj);
						
						}
					}
					
					//Flush P and Q set
					AOSProject3.p_set.clear();
					AOSProject3.q_set.clear();
					
					//Reset flag
					AOSProject3.request_fulfilled_flag[AOSProject3.resource_file_num] = false;
					
					//Reset writeLockReceiveHashmap for yourself
					AOSProject3.writeLockReceiveHashmapList.get(AOSProject3.resource_file_num).put(AOSProject3.local_node_id,0);	
					
					//Reset writeLockGrantHashmap for yourself
					AOSProject3.writeLockGrantHashmapList.get(AOSProject3.resource_file_num).put(AOSProject3.local_node_id,0);
						
					//Release Write Lock
					release_write_lock(AOSProject3.resource_file_num);	
					
					//Unlock local copy
					AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- C3 - WLF should be true, next operation false, request fulfilled, (Client call) -- WriteLockFlag: "+AOSProject3.write_lock_flag[AOSProject3.resource_file_num],true);
					AOSProject3.write_lock_flag[AOSProject3.resource_file_num]=false;
					AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- C3 - WLF should be false, request fulfilled, (Client call) -- WriteLockFlag: "+AOSProject3.write_lock_flag[AOSProject3.resource_file_num],true);
					
					//Request number
					AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- Request Number:"+request_number+" Fulfilled",true);
					
					//Unlock Semaphore
					AOSProject3.semaphore_unlock("client","[Write] Request fulfilled",AOSProject3.resource_file_num);
					
					//Once CS request has been fulfilled, break the loop
					break;
				}
				
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Write] -- Request NOT yet fulfilled",true);
			}
			
			if(AOSProject3.exp_backoff_done_flag[AOSProject3.resource_file_num] == false)
			{
				//Unlock Semaphore
				AOSProject3.semaphore_unlock("client","[Write] Unable to lock own copy",AOSProject3.resource_file_num);
			}
			else
			{
				AOSProject3.exp_backoff_done_flag[AOSProject3.resource_file_num] = false;
			}
			

		}
		
		
	}
	
	public void release_read_lock()
	{

		AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- release_read_lock() called",true);
		
		//Check Hashmap if lock has been received
		for(int i = 0; i < AOSProject3.readLockReceiveHashmapList.get(AOSProject3.resource_file_num).size(); i++) 
		{
			if(AOSProject3.local_node_id != i) // Don't send to yourself
			{
				
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- inside release_read_lock",true);
				
				int read_lock_val = AOSProject3.readLockReceiveHashmapList.get(AOSProject3.resource_file_num).get(i);
				
				if(read_lock_val == 1)
				{		
					int client_node_id = AOSProject3.NodeLocInfo.get(i).node_id;
					String client_node_name = AOSProject3.NodeLocInfo.get(i).node_name;
					int client_node_port = AOSProject3.NodeLocInfo.get(i).node_port;
					
					//Increment logical timestamp
					AOSProject3.logical_timestamp = AOSProject3.logical_timestamp + 1;
					
					//Increment vector timestamp
					AOSProject3.vector_timestamp[AOSProject3.local_node_id] = AOSProject3.vector_timestamp[AOSProject3.local_node_id] + 1;
					
					Packet packetObj = new Packet();
					packetObj.packet_type = "MSG_RELEASE_READ_LOCK";
					packetObj.sender_id = AOSProject3.local_node_id;
					packetObj.receiver_node_id = client_node_id;
					packetObj.receiver_node_name = client_node_name;		
					packetObj.logical_timestamp = AOSProject3.logical_timestamp;
					packetObj.vector_timestamp = AOSProject3.vector_timestamp;
					packetObj.request_number = AOSProject3.my_request_number;
					
					packetObj.ru_value = AOSProject3.my_ru_value[AOSProject3.resource_file_num];
					packetObj.version_number = AOSProject3.my_version_number[AOSProject3.resource_file_num];
					packetObj.file_num = AOSProject3.resource_file_num;
					
					//Start Client
					CommClient CommClientObj = new CommClient(AOSProject3.transport_protocol);
					CommClientObj.go(client_node_id,client_node_name,client_node_port,packetObj);
					
					//Reset ReadlockHashmap
					AOSProject3.readLockReceiveHashmapList.get(AOSProject3.resource_file_num).put(i,0);	
				}
			
			}			
		}
		
		//Reset readLockReceiveHashmap for yourself
		AOSProject3.readLockReceiveHashmapList.get(AOSProject3.resource_file_num).put(AOSProject3.local_node_id,0);	
		
		//Reset readLockGrantHashmap for yourself
		AOSProject3.readLockGrantHashmapList.get(AOSProject3.resource_file_num).put(AOSProject3.local_node_id,0);
		
	}
	
	public void release_write_lock(int resource_file_num)
	{

		AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- release_write_lock() called",true);
		
		//Check Hashmap if lock has been received
		for(int i = 0; i < AOSProject3.writeLockReceiveHashmapList.get(resource_file_num).size(); i++) 
		{	
			if(AOSProject3.local_node_id != i) // Don't send to yourself
			{
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- inside release_write_lock",true);
				
				int write_lock_val = AOSProject3.writeLockReceiveHashmapList.get(resource_file_num).get(i);
				
				if(write_lock_val == 1)
				{		
					int client_node_id = AOSProject3.NodeLocInfo.get(i).node_id;
					String client_node_name = AOSProject3.NodeLocInfo.get(i).node_name;
					int client_node_port = AOSProject3.NodeLocInfo.get(i).node_port;
					
					//Increment logical timestamp
					AOSProject3.logical_timestamp = AOSProject3.logical_timestamp + 1;
					
					//Increment vector timestamp
					AOSProject3.vector_timestamp[AOSProject3.local_node_id] = AOSProject3.vector_timestamp[AOSProject3.local_node_id] + 1;
					
					Packet packetObj = new Packet();
					packetObj.packet_type = "MSG_RELEASE_WRITE_LOCK";
					packetObj.sender_id = AOSProject3.local_node_id;
					packetObj.receiver_node_id = client_node_id;
					packetObj.receiver_node_name = client_node_name;		
					packetObj.logical_timestamp = AOSProject3.logical_timestamp;
					packetObj.vector_timestamp = AOSProject3.vector_timestamp;
					packetObj.request_number = AOSProject3.my_request_number;
					
					packetObj.ru_value = AOSProject3.my_ru_value[AOSProject3.resource_file_num];
					packetObj.version_number = AOSProject3.my_version_number[AOSProject3.resource_file_num];
					packetObj.file_num = AOSProject3.resource_file_num;
					
					//Start Client
					CommClient CommClientObj = new CommClient(AOSProject3.transport_protocol);
					CommClientObj.go(client_node_id,client_node_name,client_node_port,packetObj);
					
					//Reset WritelockHashmap
					AOSProject3.writeLockReceiveHashmapList.get(AOSProject3.resource_file_num).put(i,0);	
				}
			
			}			
		}
		
		
		//Reset writeLockReceiveHashmap for yourself
		AOSProject3.writeLockReceiveHashmapList.get(AOSProject3.resource_file_num).put(AOSProject3.local_node_id,0);

		//Reset writeLockGrantHashmap for yourself
		AOSProject3.writeLockGrantHashmapList.get(AOSProject3.resource_file_num).put(AOSProject3.local_node_id,0);
			
	}
	
	public void grant_read_lock(int node_id, int file_num, int request_num)
	{
		AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- grant_read_lock("+node_id+") called",true);
		
		//Set readLockGrantHashmap
		AOSProject3.readLockGrantHashmapList.get(file_num).put(node_id,1);	
		
		//Send Grant MSG_GRANTED_READ_LOCK
		int client_node_id = AOSProject3.NodeLocInfo.get(node_id).node_id;
		String client_node_name = AOSProject3.NodeLocInfo.get(node_id).node_name;
		int client_node_port = AOSProject3.NodeLocInfo.get(node_id).node_port;
		
		//Increment logical timestamp
		AOSProject3.logical_timestamp = AOSProject3.logical_timestamp + 1;
		
		//Increment vector timestamp
		AOSProject3.vector_timestamp[AOSProject3.local_node_id] = AOSProject3.vector_timestamp[AOSProject3.local_node_id] + 1;
		
		Packet packetObj = new Packet();
		packetObj.packet_type = "MSG_GRANTED_READ_LOCK";
		packetObj.sender_id = AOSProject3.local_node_id;
		packetObj.receiver_node_id = client_node_id;
		packetObj.receiver_node_name = client_node_name;	
		packetObj.logical_timestamp = AOSProject3.logical_timestamp;
		packetObj.vector_timestamp = AOSProject3.vector_timestamp;
		packetObj.request_number = request_num;
				
		packetObj.ru_value = AOSProject3.my_ru_value[file_num];
		packetObj.version_number = AOSProject3.my_version_number[file_num];
		packetObj.file_num = file_num;
		
		//Start Client
		CommClient CommClientObj = new CommClient(AOSProject3.transport_protocol);
		CommClientObj.go(client_node_id,client_node_name,client_node_port,packetObj);

	}
	
	public void grant_write_lock(int node_id, int file_num, int request_num)
	{
	
		AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- grant_write_lock("+node_id+") called",true);
					
		AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- C2 - WLF should be false, next operation true, send grant write lock (Server call) -- WriteLockFlag: "+AOSProject3.write_lock_flag[file_num],true);
		AOSProject3.write_lock_flag[file_num]=true;
		AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- C2 - WLF should be true, send grant write lock (Server call) -- WriteLockFlag: "+AOSProject3.write_lock_flag[file_num],true);
					
		//Set writeLockGrantHashmap
		AOSProject3.writeLockGrantHashmapList.get(file_num).put(node_id,1);	
	
		//Send Grant MSG_GRANTED_WRITE_LOCK
		int client_node_id = AOSProject3.NodeLocInfo.get(node_id).node_id;
		String client_node_name = AOSProject3.NodeLocInfo.get(node_id).node_name;
		int client_node_port = AOSProject3.NodeLocInfo.get(node_id).node_port;
		
		//Increment logical timestamp
		AOSProject3.logical_timestamp = AOSProject3.logical_timestamp + 1;
		
		//Increment vector timestamp
		AOSProject3.vector_timestamp[AOSProject3.local_node_id] = AOSProject3.vector_timestamp[AOSProject3.local_node_id] + 1;
		
		Packet packetObj = new Packet();
		packetObj.packet_type = "MSG_GRANTED_WRITE_LOCK";
		packetObj.sender_id = AOSProject3.local_node_id;
		packetObj.receiver_node_id = client_node_id;
		packetObj.receiver_node_name = client_node_name;	
		packetObj.logical_timestamp = AOSProject3.logical_timestamp;
		packetObj.vector_timestamp = AOSProject3.vector_timestamp;
		packetObj.request_number = request_num;
				
		packetObj.ru_value = AOSProject3.my_ru_value[file_num];
		packetObj.version_number = AOSProject3.my_version_number[file_num];
		packetObj.file_num = file_num;
		
		//Start Client
		CommClient CommClientObj = new CommClient(AOSProject3.transport_protocol);
		CommClientObj.go(client_node_id,client_node_name,client_node_port,packetObj);	

	}
	
	public void request_latest_file_read(int node_id)
	{
		AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- request_latest_file_read("+node_id+") called",true);
		
		int client_node_id = AOSProject3.NodeLocInfo.get(node_id).node_id;
		String client_node_name = AOSProject3.NodeLocInfo.get(node_id).node_name;
		int client_node_port = AOSProject3.NodeLocInfo.get(node_id).node_port;
		
		//Increment logical timestamp
		AOSProject3.logical_timestamp = AOSProject3.logical_timestamp + 1;
		
		//Increment vector timestamp
		AOSProject3.vector_timestamp[AOSProject3.local_node_id] = AOSProject3.vector_timestamp[AOSProject3.local_node_id] + 1;
		
		Packet packetObj = new Packet();
		packetObj.packet_type = "MSG_REQUEST_LATEST_FILE_READ";
		packetObj.sender_id = AOSProject3.local_node_id;
		packetObj.receiver_node_id = client_node_id;
		packetObj.receiver_node_name = client_node_name;	
		packetObj.logical_timestamp = AOSProject3.logical_timestamp;
		packetObj.vector_timestamp = AOSProject3.vector_timestamp;
		packetObj.request_number = AOSProject3.my_request_number;
				
		packetObj.ru_value = AOSProject3.my_ru_value[AOSProject3.resource_file_num];
		packetObj.version_number = AOSProject3.my_version_number[AOSProject3.resource_file_num];
		packetObj.file_num = AOSProject3.resource_file_num;
		
		//Start Client
		CommClient CommClientObj = new CommClient(AOSProject3.transport_protocol);
		CommClientObj.go(client_node_id,client_node_name,client_node_port,packetObj);
	}
	
	public void request_latest_file_write(int node_id)
	{
		AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- request_latest_file_write("+node_id+") called",true);
		
		int client_node_id = AOSProject3.NodeLocInfo.get(node_id).node_id;
		String client_node_name = AOSProject3.NodeLocInfo.get(node_id).node_name;
		int client_node_port = AOSProject3.NodeLocInfo.get(node_id).node_port;
		
		//Increment logical timestamp
		AOSProject3.logical_timestamp = AOSProject3.logical_timestamp + 1;
		
		//Increment vector timestamp
		AOSProject3.vector_timestamp[AOSProject3.local_node_id] = AOSProject3.vector_timestamp[AOSProject3.local_node_id] + 1;
		
		Packet packetObj = new Packet();
		packetObj.packet_type = "MSG_REQUEST_LATEST_FILE_WRITE";
		packetObj.sender_id = AOSProject3.local_node_id;
		packetObj.receiver_node_id = client_node_id;
		packetObj.receiver_node_name = client_node_name;	
		packetObj.logical_timestamp = AOSProject3.logical_timestamp;
		packetObj.vector_timestamp = AOSProject3.vector_timestamp;
		packetObj.request_number = AOSProject3.my_request_number;
				
		packetObj.ru_value = AOSProject3.my_ru_value[AOSProject3.resource_file_num];
		packetObj.version_number = AOSProject3.my_version_number[AOSProject3.resource_file_num];
		packetObj.file_num = AOSProject3.resource_file_num;
		
		//Start Client
		CommClient CommClientObj = new CommClient(AOSProject3.transport_protocol);
		CommClientObj.go(client_node_id,client_node_name,client_node_port,packetObj);
	}
	
	public void send_read_deny_lock(int node_id,int file_num)
	{
		AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- send_read_deny_lock("+node_id+") called",true);
		
		int client_node_id = AOSProject3.NodeLocInfo.get(node_id).node_id;
		String client_node_name = AOSProject3.NodeLocInfo.get(node_id).node_name;
		int client_node_port = AOSProject3.NodeLocInfo.get(node_id).node_port;
		
		//Increment logical timestamp
		AOSProject3.logical_timestamp = AOSProject3.logical_timestamp + 1;
		
		//Increment vector timestamp
		AOSProject3.vector_timestamp[AOSProject3.local_node_id] = AOSProject3.vector_timestamp[AOSProject3.local_node_id] + 1;
		
		Packet packetObj = new Packet();
		packetObj.packet_type = "MSG_DENY_READ_LOCK";
		packetObj.sender_id = AOSProject3.local_node_id;
		packetObj.receiver_node_id = client_node_id;
		packetObj.receiver_node_name = client_node_name;	
		packetObj.logical_timestamp = AOSProject3.logical_timestamp;
		packetObj.vector_timestamp = AOSProject3.vector_timestamp;
		packetObj.request_number = AOSProject3.my_request_number;
		
		packetObj.ru_value = AOSProject3.my_ru_value[file_num];
		packetObj.version_number = AOSProject3.my_version_number[file_num];
		packetObj.file_num = file_num;
		
		//Start Client
		CommClient CommClientObj = new CommClient(AOSProject3.transport_protocol);
		CommClientObj.go(client_node_id,client_node_name,client_node_port,packetObj);
	}
	
	public void send_write_deny_lock(int node_id,int file_num)
	{
		AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- send_write_deny_lock("+node_id+") called",true);
		
		int client_node_id = AOSProject3.NodeLocInfo.get(node_id).node_id;
		String client_node_name = AOSProject3.NodeLocInfo.get(node_id).node_name;
		int client_node_port = AOSProject3.NodeLocInfo.get(node_id).node_port;
		
		//Increment logical timestamp
		AOSProject3.logical_timestamp = AOSProject3.logical_timestamp + 1;
		
		//Increment vector timestamp
		AOSProject3.vector_timestamp[AOSProject3.local_node_id] = AOSProject3.vector_timestamp[AOSProject3.local_node_id] + 1;
		
		Packet packetObj = new Packet();
		packetObj.packet_type = "MSG_DENY_WRITE_LOCK";
		packetObj.sender_id = AOSProject3.local_node_id;
		packetObj.receiver_node_id = client_node_id;
		packetObj.receiver_node_name = client_node_name;	
		packetObj.logical_timestamp = AOSProject3.logical_timestamp;
		packetObj.vector_timestamp = AOSProject3.vector_timestamp;
		packetObj.request_number = AOSProject3.my_request_number;
			
		packetObj.ru_value = AOSProject3.my_ru_value[file_num];
		packetObj.version_number = AOSProject3.my_version_number[file_num];
		packetObj.file_num = file_num;
		
		//Start Client
		CommClient CommClientObj = new CommClient(AOSProject3.transport_protocol);
		CommClientObj.go(client_node_id,client_node_name,client_node_port,packetObj);
	}
	
	public void send_read_response_latest_file(int node_id,int file_num)
	{
		AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- send_read_response_latest_file("+node_id+") called",true);
		
		int client_node_id = AOSProject3.NodeLocInfo.get(node_id).node_id;
		String client_node_name = AOSProject3.NodeLocInfo.get(node_id).node_name;
		int client_node_port = AOSProject3.NodeLocInfo.get(node_id).node_port;
		
		//Increment logical timestamp
		AOSProject3.logical_timestamp = AOSProject3.logical_timestamp + 1;
				
		//Increment vector timestamp
		AOSProject3.vector_timestamp[AOSProject3.local_node_id] = AOSProject3.vector_timestamp[AOSProject3.local_node_id] + 1;
		
		Packet packetObj = new Packet();
		packetObj.packet_type = "MSG_RESPONSE_LATEST_FILE_READ";
		packetObj.sender_id = AOSProject3.local_node_id;
		packetObj.receiver_node_id = client_node_id;
		packetObj.receiver_node_name = client_node_name;
		packetObj.logical_timestamp = AOSProject3.logical_timestamp;
		packetObj.vector_timestamp = AOSProject3.vector_timestamp;
		packetObj.request_number = AOSProject3.my_request_number;
		
		packetObj.ru_value = AOSProject3.my_ru_value[file_num];
		packetObj.version_number = AOSProject3.my_version_number[file_num];
		packetObj.file_num = file_num;
		
		packetObj.file_name = "file"+file_num+".txt";
		packetObj.file_contents = AOSProject3.fileToByteArray("file"+file_num+".txt",AOSProject3.filesystem_dir);
	
		//Start Client
		CommClient CommClientObj = new CommClient(AOSProject3.transport_protocol);
		CommClientObj.go(client_node_id,client_node_name,client_node_port,packetObj);
	}
	
	public void send_write_response_latest_file(int node_id,int file_num)
	{
		AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- send_write_response_latest_file("+node_id+") called",true);
		
		int client_node_id = AOSProject3.NodeLocInfo.get(node_id).node_id;
		String client_node_name = AOSProject3.NodeLocInfo.get(node_id).node_name;
		int client_node_port = AOSProject3.NodeLocInfo.get(node_id).node_port;
		
		//Increment logical timestamp
		AOSProject3.logical_timestamp = AOSProject3.logical_timestamp + 1;
				
		//Increment vector timestamp
		AOSProject3.vector_timestamp[AOSProject3.local_node_id] = AOSProject3.vector_timestamp[AOSProject3.local_node_id] + 1;		
		
		Packet packetObj = new Packet();
		packetObj.packet_type = "MSG_RESPONSE_LATEST_FILE_WRITE";
		packetObj.sender_id = AOSProject3.local_node_id;
		packetObj.receiver_node_id = client_node_id;
		packetObj.receiver_node_name = client_node_name;
		packetObj.logical_timestamp = AOSProject3.logical_timestamp;
		packetObj.vector_timestamp = AOSProject3.vector_timestamp;
		packetObj.request_number = AOSProject3.my_request_number;
		
		packetObj.ru_value = AOSProject3.my_ru_value[file_num];
		packetObj.version_number = AOSProject3.my_version_number[file_num];
		packetObj.file_num = file_num;
		
		packetObj.file_name = "file"+file_num+".txt";
		packetObj.file_contents = AOSProject3.fileToByteArray("file"+file_num+".txt",AOSProject3.filesystem_dir);
	
		//Start Client
		CommClient CommClientObj = new CommClient(AOSProject3.transport_protocol);
		CommClientObj.go(client_node_id,client_node_name,client_node_port,packetObj);
	}
	
	
	
	public void cs_read_enter()
	{
		AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- cs_read_enter() called",true);
		
		//Node enters CS
		
		//multiple read is allowed, sleep if multiple process arrive here
		
		//Create lock file - log<filenum>.<operation>.lck	
		String readlockfilename = AOSProject3.log_dir+AOSProject3.log_file+".read.lck";
		String writelockfilename = AOSProject3.log_dir+AOSProject3.log_file+".write.lck";
		
		File readlockfile = new File(readlockfilename);
		File writelockfile = new File(writelockfilename);
		 
		try 
		{	
			if(writelockfile.exists()==true)
			{
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" - Write Lock file already exists. (WriteLockFile: "+writelockfilename+")",true);
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" - READ-WRITE VIOLATION !!! (WriteLockFile: "+writelockfilename+")",true);
				
				AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - READ-WRITE VIOLATION !!!",AOSProject3.csviolation_dir+AOSProject3.csviolation_file);
			}
			else
			{
				while(true)
				{					
					if(readlockfile.createNewFile())
					{				
						AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" - Lock file created! (ReadLockFile: "+readlockfilename+")",true);
						
						//Open resource file
					    PrintWriter pw = null;
					    
						try {
						    pw = new PrintWriter(new BufferedWriter(new FileWriter(AOSProject3.log_dir+AOSProject3.log_file, true)));
						    
						    pw.println("Node:"+AOSProject3.local_node_id+" -- started reading #"+AOSProject3.logical_timestamp+"#"+Arrays.toString(AOSProject3.vector_timestamp));						    
						    AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- started reading #"+AOSProject3.logical_timestamp+"#"+Arrays.toString(AOSProject3.vector_timestamp),true);		    				    
						    
						    /*
						    //Actual File - Start Read 
						    if(AOSProject3.q_set.contains(AOSProject3.local_node_id)) // You already have max version file
						    {
						    	String file_contents = AOSProject3.file_read_line(AOSProject3.filesystem_dir+AOSProject3.resource_file);
						    	AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- read from "+AOSProject3.filesystem_dir+AOSProject3.resource_file+", FileContents: "+file_contents+"...",true);
						    }
						    else // Read file from read_latest/ folder
						    {
						    	String file_contents = AOSProject3.file_read_line(AOSProject3.filesystem_dir+"read_latest/"+AOSProject3.resource_file);
						    	AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- read from "+AOSProject3.filesystem_dir+"read_latest/"+AOSProject3.resource_file+", FileContents: "+file_contents+"...",true);
						    }
						    */
						    
						    //Mean Duration of CS
						    try {
								Thread.sleep(AOSProject3.generate_random(20));
							} catch (InterruptedException e) {
								AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - Client - Mean Duration of Read CS",AOSProject3.exception_dir+AOSProject3.exception_file);
								e.printStackTrace();
							}
						    
						    pw.println("Node:"+AOSProject3.local_node_id+" -- finished reading#"+AOSProject3.logical_timestamp+"#"+Arrays.toString(AOSProject3.vector_timestamp));
						    AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- finished reading#"+AOSProject3.logical_timestamp+"#"+Arrays.toString(AOSProject3.vector_timestamp),true);
						    
						}catch (IOException e) {
							AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - Client - CS",AOSProject3.exception_dir+AOSProject3.exception_file);
						    System.err.println(e);
						}finally{
						    if(pw != null){
						    	//Close resource file
						    	pw.close();
						    }
						}
			
						//Delete lock file
					    readlockfile.delete();
					    AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" - Lock file deleted! (ReadLockFile: "+readlockfilename+")",true);
					    
					    break;
					}
					else
					{
						//If readlock already exists
						//Sleep for Random time if multiple processes arrive at CS
						try {
							Thread.sleep(AOSProject3.generate_random(20));
						} catch (InterruptedException e) {
							AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - Client - Read CS Random Sleep",AOSProject3.exception_dir+AOSProject3.exception_file);
							e.printStackTrace();
						}
					}
				    
				}
				
			}
			
		} catch (Exception e1) {
			AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - Client - Read CS",AOSProject3.exception_dir+AOSProject3.exception_file);
			e1.printStackTrace();
		}
		
		//Node left CS
	}
	
	public void cs_write_enter()
	{
		AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- cs_write_enter() called",true);
		
		//Node enters CS
		
		//Create lock file - log<filenum>.<operation>.lck	
		String readlockfilename = AOSProject3.log_dir+AOSProject3.log_file+".read.lck";
		String writelockfilename = AOSProject3.log_dir+AOSProject3.log_file+".write.lck";
		
		File readlockfile = new File(readlockfilename);
		File writelockfile = new File(writelockfilename);
			 
		try 
		{
			if(readlockfile.exists()==true)
			{
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" - Read Lock file already exists. (ReadLockFile: "+readlockfilename+")",true);
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" - READ-WRITE VIOLATION !!! (ReadLockFile: "+readlockfilename+")",true);
				
				AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - READ-WRITE VIOLATION !!!",AOSProject3.csviolation_dir+AOSProject3.csviolation_file);
			}
			else
			{
				//Create writelock file
				if(writelockfile.createNewFile())
				{
					AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" - Lock file created! (WriteLockFile: "+writelockfilename+")",true);
		    
				    //Open resource file
				    PrintWriter pw = null;
				    //PrintWriter pw2 = null;
				    
					try {
					    pw = new PrintWriter(new BufferedWriter(new FileWriter(AOSProject3.log_dir+AOSProject3.log_file, true)));
					    pw.println("Node:"+AOSProject3.local_node_id+" -- started writing #"+AOSProject3.logical_timestamp+"#"+Arrays.toString(AOSProject3.vector_timestamp));			    
					    AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- started writing #"+AOSProject3.logical_timestamp+"#"+Arrays.toString(AOSProject3.vector_timestamp),true);		    				    
					    
					    //Actual File - Start Write
					    //pw2 = new PrintWriter(new BufferedWriter(new FileWriter(AOSProject3.filesystem_dir+AOSProject3.resource_file, true)));
					    //pw2.println("Node:"+AOSProject3.local_node_id+" -- started writing #"+AOSProject3.logical_timestamp+"#"+Arrays.toString(AOSProject3.vector_timestamp));			    
					    //AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- started writing to "+AOSProject3.filesystem_dir+AOSProject3.resource_file+"#"+AOSProject3.logical_timestamp+"#"+Arrays.toString(AOSProject3.vector_timestamp),true);
					    
					    //Mean Duration of CS
					    try {
							Thread.sleep(AOSProject3.generate_random(20));
						} catch (InterruptedException e) {
							AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - Client - Write CS Quorum Timer",AOSProject3.exception_dir+AOSProject3.exception_file);
							e.printStackTrace();
						}
					    
					    pw.println("Node:"+AOSProject3.local_node_id+" -- finished writing#"+AOSProject3.logical_timestamp+"#"+Arrays.toString(AOSProject3.vector_timestamp));
					    AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- finished writing#"+AOSProject3.logical_timestamp+"#"+Arrays.toString(AOSProject3.vector_timestamp),true);
					    
					    //Actual File - End Write
					    //pw2.println("Node:"+AOSProject3.local_node_id+" -- finished writing#"+AOSProject3.logical_timestamp+"#"+Arrays.toString(AOSProject3.vector_timestamp));
					    //AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- finished writing to "+AOSProject3.filesystem_dir+AOSProject3.resource_file+"#"+AOSProject3.logical_timestamp+"#"+Arrays.toString(AOSProject3.vector_timestamp),true);
					    
					}catch (IOException e) {
						AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - Client - Write CS",AOSProject3.exception_dir+AOSProject3.exception_file);
					    System.err.println(e);
					}finally{
					    if(pw != null){
					    	//Close resource file
					    	pw.close();
					    }
					    //if(pw2 != null){
					    //	//Close resource file
					    //	pw2.close();
					    //}
					}
					
					 //Delete lock file
				    writelockfile.delete();
				    AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" - Lock file deleted! (WriteLockFile: "+writelockfilename+")",true);
				}
				else
				{
					AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" - Write Lock file already exists. (WriteLockFile: "+writelockfilename+")",true);
					AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" - WRITE-WRITE VIOLATION !!! (WriteLockFile: "+writelockfilename+")",true);
					
					AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - WRITE-WRITE VIOLATION !!!",AOSProject3.csviolation_dir+AOSProject3.csviolation_file);
				}
			    
			}
		    
		} catch (IOException e1) {
			AOSProject3.write_to_file("Node:"+AOSProject3.local_node_id+" - EXCEPTION !!! - Client - Write CS-2",AOSProject3.exception_dir+AOSProject3.exception_file);
			e1.printStackTrace();
		}
		
		//Node left CS
	}
	
	public void quorum_read_operations()
	{	
		AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Read] -- quorum_read_operations() called",true);
				
		//Check for latest version
		if(AOSProject3.max_version_number[AOSProject3.resource_file_num] != AOSProject3.my_version_number[AOSProject3.resource_file_num])
		{							
			//Request latest file
			int max_version_number_file_node_id = AOSProject3.q_set.get(0);
			request_latest_file_read(max_version_number_file_node_id);
			
			//Unlock Semaphore
			AOSProject3.semaphore_unlock("client","[Read] Before Waiting for latest file",AOSProject3.resource_file_num);
			
			//Wait till you receive the latest file
			while(true)
			{
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Read] -- Still waiting for latest file",true);
				
				if(AOSProject3.wait_for_read_latest_file[AOSProject3.resource_file_num] == true)
				{
					AOSProject3.wait_for_read_latest_file[AOSProject3.resource_file_num] = false;
					break;
				}
			}
				
			//Lock Semaphore
			AOSProject3.semaphore_lock("client","[Read] After Waiting for latest file",AOSProject3.resource_file_num);
		}

			
		//Enter CS
		cs_read_enter();
			
		//Reset versionNumberHashmap
		reset_version_number_hashmap();
		
		//Release Read Lock
		release_read_lock();
		
		//Reset request_fulfilled_flag
		AOSProject3.request_fulfilled_flag[AOSProject3.resource_file_num] = true;
		

	}
	
	public void quorum_write_operations()
	{
		
		AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Write] -- quorum_write_operations() called",true);
		
		AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Write] -- Max version number:"+AOSProject3.max_version_number[AOSProject3.resource_file_num]+" | My version number:"+AOSProject3.my_version_number[AOSProject3.resource_file_num],true);
		
		//Check for latest version
		if(AOSProject3.max_version_number[AOSProject3.resource_file_num] != AOSProject3.my_version_number[AOSProject3.resource_file_num])
		{							
			//Request latest file
			int max_version_number_file_node_id = AOSProject3.q_set.get(0);
			request_latest_file_write(max_version_number_file_node_id);
				
			//Unlock Semaphore
			AOSProject3.semaphore_unlock("client","[Write] Before Waiting for latest file",AOSProject3.resource_file_num);
			
			//Wait till you receive the latest file
			while(true)
			{
				AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- [Write] -- Still waiting for latest file",true);
				
				if(AOSProject3.wait_for_write_latest_file[AOSProject3.resource_file_num] == true)
				{
					AOSProject3.wait_for_write_latest_file[AOSProject3.resource_file_num] = false;
					break;
				}
			}									
			
			//Lock Semaphore
			AOSProject3.semaphore_lock("client","[Write] After Waiting for latest file",AOSProject3.resource_file_num);
			
		}
		
			
		//Enter CS
		cs_write_enter();	
		
		//Reset versionNumberHashmap
		reset_version_number_hashmap();

		//Request fulfilled flag
		AOSProject3.request_fulfilled_flag[AOSProject3.resource_file_num] = true;
		
	}
	
	
	public boolean check_read_grant_hashmap_set(int file_num) // Checks if my local replica is locked by me or by any other process
	{
		AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- check_read_grant_hashmap_set("+file_num+") called",true);
			
		boolean all_read_locks_set = false;
											
		//Check Hashmap if lock has been received
		for(int i = 0; i < AOSProject3.readLockGrantHashmapList.get(file_num).size(); i++) 
		{						
			int read_lock_val = AOSProject3.readLockGrantHashmapList.get(file_num).get(i);
			
			if(read_lock_val == 1)
			{		
				all_read_locks_set = true;
				
				return all_read_locks_set;
			}				
						
		}	
			
		return all_read_locks_set;
	}
	
	
	
	public void reset_version_number_hashmap()
	{
		
		AOSProject3.write_output("Node:"+AOSProject3.local_node_id+" -- reset_version_number_hashmap() called",true);
		
		for(int i = 0; i < AOSProject3.versionNumberHashmapList.get(AOSProject3.resource_file_num).size(); i++) 
		{
			AOSProject3.versionNumberHashmapList.get(AOSProject3.resource_file_num).put(i, 0);
		}		

	}
	
}
