How to compile/run:

1.	Compile
javac < ProgramName.java>
javac SpeculativeTomasulo.java
javac Tomasulo.java

2.	Run
java <ProgramName> <InstructionFilePath> <load_buffer_size> <store_buffer_size> <add_buffer_size> <mult_buffer_size> <branch_buffer_size> <no_of_memory_access_cycle> <no_of_add_cycle> <no_of_mult_cycle> <no_of_div_cycle> <no_of_branch_cycle> <reorder_buffer_size> <branch_prediction>

java SpeculativeTomasulo instruction.txt 3 3 3 2 2 1 2 3 3 1 10 T
java SpeculativeTomasulo instruction.txt 3 3 3 2 2 1 2 3 3 1 5 NT
java SpeculativeTomasulo instruction2.txt 3 3 3 2 2 1 2 3 3 1 3 T
java SpeculativeTomasulo instruction3.txt 3 3 3 2 2 1 2 3 3 1 10 T
java SpeculativeTomasulo instTest.txt 3 3 3 2 2 1 2 3 3 1 10 T


java Tomasulo instruction.txt 3 3 3 2 2 1 2 3 3 1 T
java Tomasulo instruction.txt 3 3 3 2 2 1 2 3 3 1 NT
java Tomasulo instruction2.txt 3 3 3 2 2 1 2 3 3 1 T
java Tomasulo instruction3.txt 3 3 3 2 2 1 2 3 3 1 T
java Tomasulo instTest.txt 3 3 3 2 2 1 2 3 3 1 T


SpeculativeTomasulo.java is the program which executes speculative tomasulo. 
Tomasulo.java is the program which executes non-speculative tomasulo.
The branch prediction is the static branch prediction which is one of the input parameter which indicates if the Branch instruction is always taken (T) or always not taken (NT). If the branch is taken, then we execute all the instruction uptil the branch instruction in a loop for 4 times. (It is assumed that the loop exits after 4th iteration)
