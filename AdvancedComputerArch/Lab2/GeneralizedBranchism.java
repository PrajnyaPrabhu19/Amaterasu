package main.java;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.lang.*;
import java.util.*;

public class GeneralizedBranchism {
    private static Queue<Integer> globalHistory = new LinkedList<>();
    private static String global = "";
    private static HashMap<String, String> localBHT = new HashMap<>();

    public static void main(String[] args) {
        if(args.length==0){
            System.out.println("Please enter the arguments while running this file in format:");
            System.out.println("java Program_Name.java Trace_File_Name m n lsb");
            System.exit(1);
        }
        String filename = args[0];
        int m = 0;
        if(args.length>1){
            m = Integer.parseInt(args[1]);
        }

        int n = 0;
        if(args.length>2) {
            n = Integer.parseInt(args[2]);
        }
        int lsb = 8;
        if(args.length>3){
            lsb = Integer.parseInt(args[3]);
        }

        while(lsb<8 || lsb>12){
            Scanner input = new Scanner(System. in);
            System.out.println("Please enter the LSB value in range of 8 to 12 :");
            lsb = input.nextInt();
        }
        while(m>12){
            Scanner input = new Scanner(System. in);
            System.out.println("Please enter the m value in range of 0 to 12 :");
            m = input.nextInt();
        }
        while(n<1 || n>2){
            Scanner input = new Scanner(System. in);
            System.out.println("Please enter the value of n as 1 or 2 :");
            n = input.nextInt();
        }
        int counter =0;
        int miss = 0;
        String Predictor;
        if(n==1){
            Predictor = "0";
        }
        else{
            Predictor = "00";
        }

        //create global history only if correlating predictor is used
        if(m>0) {
            initializeGlobalHistory(m);
        }

        //start reading the trace from input file
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                counter++;
                String[] arrOfTrace = line.split(" ");
                //fetch the hexadecimal address and get the last 2 chars for 8 bit LSB.
                String binary = getBinaryAddressofLocal(arrOfTrace[0],lsb);
                String decision = arrOfTrace[1];
                miss+= checkAndUpdateGlobal(binary, decision, m, n);
            }
            System.out.println("Predictor("+m+","+n+")");
            System.out.println("Number of lines: "+counter+ "; Missed predictions : "+miss);
            float misprediction = (float) ((miss*100)/(float)counter);
            System.out.println("misprediction rate = "+misprediction+"%");
            System.out.println("Number of rows utilized in BHT= "+localBHT.keySet().size());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //method to check if branch prediction is hit or miss and update global if required
    public static int checkAndUpdateGlobal(String localAddr, String decision, int m, int n){
        int miss = 0;
        String address = global+localAddr;
        //update the global history if m>0
        if(m>0){
            String f ="";
            if(decision.equalsIgnoreCase("T")){
                f ="1";
            }
            else{
                f="0";
            }
            global = global.substring(0,m-1);
            global = f+global;
            //System.out.println("Global:" + global);
        }
        if(localBHT.containsKey(address)) {
            if(n==1){
                String predicted = localBHT.get(address);
                //check for mismatches and update miss value and flip the value in BHT
                if(predicted.equalsIgnoreCase("1") && decision.equalsIgnoreCase("N")){
                    miss=1;
                    localBHT.replace(address,"0");
                }
                else if(predicted.equalsIgnoreCase("0") && decision.equalsIgnoreCase("T")){
                    miss=1;
                    localBHT.replace(address,"1");
                }
                else{
                    //do nothing
                }
            }
            //else n=2, 2 bit predictor
            else {
                String predicted = localBHT.get(address);
                String prediction = "";
                if (predicted.equalsIgnoreCase("00")) {
                    prediction = "N";
                    //check if actual decision is same as predicted, if not then update the BHT table
                    if (!prediction.equalsIgnoreCase(decision)) {
                        miss = 1;
                        localBHT.replace(address, "01");
                    }
                }
                else if (predicted.equalsIgnoreCase("01")) {
                    prediction="N";
                    //check if predicted and decision is same, then need to update the prediction state in BHT
                    if(prediction.equalsIgnoreCase(decision)){
                        localBHT.replace(address,"00");
                    }
                    else{
                        //its a miss then, update the localBHT
                        miss =1;
                        localBHT.replace(address,"11");
                    }
                }
                else if (predicted.equalsIgnoreCase("10")) {
                    prediction ="T";
                    if(prediction.equalsIgnoreCase(decision)){
                        localBHT.replace(address, "11");
                    }
                    else{
                        miss =1;
                        localBHT.replace(address,"00");
                    }
                }
                else {
                    prediction="T";
                    if(!prediction.equalsIgnoreCase(decision)){
                        miss=1;
                        localBHT.replace(address,"10");
                    }
                }
            }
        }
        else{
            //check if its 1 bit predictor and it will be miss only if the decision for the first time was T
            if(n==1){
                if(decision.equalsIgnoreCase("T")){
                    miss =1;
                    localBHT.put(address,"1");
                }
                else{
                    miss=0;
                    //its not a miss as initially BHT will be initialized with 0 value
                    localBHT.put(address,"0");
                }
            }
            else{
                //2 bit predictor has 4 states. but since this code is accessed only first time, need to update localBHT with 2 possible values
                if(decision.equalsIgnoreCase("T")){
                    miss =1;
                    localBHT.put(address,"01");
                }
                else{
                    localBHT.put(address,"00");
                }

            }
        }
        return miss;
    }

    //Method to return n bit LSB for local BHT
    public static String getBinaryAddressofLocal(String hex, int lsb){
        //always get 12 bits and then get substring of binary with required bits of PC
        int num = Integer.parseInt(hex.substring(3),16); //substring(4) as to obtain last 2 chars for 8 bit lsb.
        String binary = Integer.toBinaryString(num);
        int len = binary.length();
        //if len>lsb
        if(len>lsb){
            int diff = len-lsb;
            binary = binary.substring(diff);
        }
        //if len < lsb
        if(len<lsb){
            int l = lsb- len;
            String prefix="";
            for(int i=0;i<l;i++){
                prefix +="0";
            }
            binary = prefix+binary;
        }
        //if equal return
        return binary;
    }

    //the global history will first contain 0 as its state
    public static void initializeGlobalHistory(int m) {
        for (int i = 0; i < m; i++) {
            globalHistory.add(0); //dont use queue
            global += "0";
        }
    }

}