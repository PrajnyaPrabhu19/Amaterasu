##########
1. Objective
The goal of project 2 is to design and implement various branch predictors,
including simple 1-bit and 2-bit predictors, as well as correlating (m,n) predictors.

2. Branch traces
To facilitate the testing of your branch predictor,
we will utilize two sets of traces collected from a run of gcc, "2bit-toogood.txt" and "gcc-10K.txt"

3. Branchsim
Now that you have a better idea of how simulators are implemented, this project will be a bit more open ended.
You will implement from scratch a C++/Java/Python-based branch simulator (let’s call it Branchsim). Your simulator must meet the following requirements.
a) Command line argument for input trace file name
b) Command line argument for branch predictor type
a. Recall, that a simple 1-bit predictor is (0,1) predictor and simple 2-bit predictor is (0,2) predictor with 0 global history. Therefore, you can take in a branch predictor type in the form of (m,n).
c) Initialize all entries of the branch predictor to 0 or 00.
d) Output the misprediction rate.
###########

GeneralizedBranchism.java file is genrealized version which takes the tracefile name as the input in command line along
with m,n vales for predictor and the lsb from the PC to be considered.
Value ranges for m is 0 to 12 and for lsb is 8 to 12.

How to run and complie:
1. javac GeneralizedBranchism.java
2. java GeneralizedBranchism.java Trace_File_Name m n lsb
Example: java GeneralizedBranchism.java 2bit-toogood.txt 0 1 8