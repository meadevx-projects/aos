import java.util.Random;




public class AOSKeyDistribution {
	
	static int process_matrix[][];
	
	
	public static void main(String[] args)
	{		
		int no_of_processes = 10;
		
		int key_formula_val = ((no_of_processes*(no_of_processes-1))/2);
		
		process_matrix = new int[no_of_processes][no_of_processes];
		
		System.out.println("Process Matrix for "+no_of_processes+" Processes: ");
		
		for (int i = 0; i < no_of_processes; i++) {
			for (int j = 0; j < no_of_processes; j++) {
				
				if(generate_random()%2==0)
				{
					process_matrix[i][j] = 1;
				}
				else
				{
					process_matrix[i][j] = 0;
				}
				
				System.out.print(process_matrix[i][j] + " | ");
			}
			System.out.print("\n");
		}
		
		// Check condition
		for (int i = 0; i < no_of_processes; i++) {
			for (int j = 0; j < no_of_processes; j++) {
				
				if(i==j)
				{
					process_matrix[i][j] = 0;
				}
				else if(process_matrix[i][j] == 1)
				{
					process_matrix[j][i] = 0;
				}			
				else if(process_matrix[i][j] == 0)
				{
					process_matrix[j][i] = 1;
				}
				
			}
			System.out.print("\n");
		}
		
		System.out.println("Final Process Matrix for "+no_of_processes+" Processes: ");
		for (int i = 0; i < no_of_processes; i++) {
			for (int j = 0; j < no_of_processes; j++) {
				
				System.out.print(process_matrix[i][j] + " | ");

			}
			System.out.print("\n");
		}
		
		System.out.println("Final Process Matrix for "+no_of_processes+" Processes: ");
		
		int keycount=0;
		
		for (int i = 0; i < no_of_processes; i++) {
			boolean print_flag=false;
			System.out.print(i+"	");
			
			for (int j = 0; j < no_of_processes; j++) {
				
				if(process_matrix[i][j]==1)
				{
					keycount++;
					if(print_flag==false)
					{
						System.out.print(j);
						print_flag=true;
					}
					else
						System.out.print("," + j);
					
				}
				
				
			}
			System.out.print("\n");
		}
		
		
		
		System.out.println("No of keys by formula: "+key_formula_val);
		System.out.println("No of keys: "+keycount);
		
	}
	
	public static int generate_random() {
		Random randomGenerator = new Random();
		int randomInt = randomGenerator.nextInt(20);
		return randomInt;
	}


}

