

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class SpeculativeTomasulo {
    //number of cycles for each operation
    private static int add_cycle = 1;
    private static int mult_cycle = 1;
    private static int div_cycle = 1;
    private static int memory_cycle = 1;
    private static int branch_cycle = 1;

    //Have ALU as Map, Units as keys as only one of each are available and the RS object which acquired the lock
    private static HashMap<String, ReservationStation> ALU_UNITS = new HashMap<>();

    //reservation station units
    private static int ADD_BUFFER = 3;
    private static int MULT_BUFFER = 2;
    private static int LOAD_BUFFER = 3;
    private static int BRANCH_BUFFER = 1;
    private static int STORE_BUFFER = 3;

    //ROB size
    private static int ROB_SIZE = 10;

    private static ArrayList<InstructionBean> instructionQueue = new ArrayList<>();
    //maintain list of current instructions in buffers
    private static ArrayList<ReservationStation> LOAD_RS = new ArrayList<>();
    private static ArrayList<ReservationStation> STORE_RS = new ArrayList<>();
    private static ArrayList<ReservationStation> ADD_RS = new ArrayList<>();
    private static ArrayList<ReservationStation> MULT_RS = new ArrayList<>();
    private static ArrayList<ReservationStation> BRANCH_RS = new ArrayList<>();

    private static ArrayList<ReOrderBuffer> RobList = new ArrayList<>();

    private static int ROB_TAG = 1;

    public static void main(String[] args) {

        if(args.length<13){
            System.out.println("Please enter all 11 required values in following format and rerun");
            System.out.println("java ProgramName fileName load_buffer store_buffer add_buffer mult_buffer branch_buffer" +
                    " memory_access_cycle add_cycle mult_cycle div_cycle branch_cycle reorder_buffer_size branch_prediction");
            System.exit(1);
        }
            String instructionFile = args[0];
            LOAD_BUFFER = Integer.parseInt(args[1]);
            STORE_BUFFER = Integer.parseInt(args[2]);
            ADD_BUFFER = Integer.parseInt(args[3]);
            MULT_BUFFER = Integer.parseInt(args[4]);
            BRANCH_BUFFER = Integer.parseInt(args[5]);
            memory_cycle = Integer.parseInt(args[6]);
            add_cycle = Integer.parseInt(args[7]);
            mult_cycle = Integer.parseInt(args[8]);
            div_cycle = Integer.parseInt(args[9]);
            branch_cycle = Integer.parseInt(args[10]);
            ROB_SIZE = Integer.parseInt(args[11]);
            String branch_predictor = args[12];
        //String branch_predictor = "T";
        boolean branch = false;
        //String instructionFile = "/Users/prajnyaprabhu/IdeaProjects/ACA/src/main/java/arch/instruction.txt";

        ArrayList<String> allInstructions = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(instructionFile))) {
            String line;
            //int i=1;
            while ((line = br.readLine()) != null) {
                //line=line+" "+i;
                if(branch_predictor.equalsIgnoreCase("T")){
                    if(line.charAt(0)=='B'){
                        branch =true;
                    }
                }

                allInstructions.add(line);
               // i++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int i= 1;
        ArrayList<String> newInstructions = new ArrayList<>();
        if(branch){
            //find the inst index where branch is present
            int ind=0;
            for(int j=0;j<allInstructions.size();j++){
                if(allInstructions.get(j).charAt(0)=='B'){
                    ind =j;
                    break;
                }
            }
            int counter =0;
            while (counter<4){
                for(int k =0;k<=ind;k++){
                    String s =allInstructions.get(k);
                    String x = s+" "+i+" "+counter;
                    newInstructions.add(x);
                    i++;
                }
                counter++;
            }
            counter--;
            for(int k=ind+1;k<allInstructions.size();k++){
                String s =allInstructions.get(k);
                String x = s+" "+i+" "+counter;
                newInstructions.add(x);
                i++;
            }
        }
        else{
            int g =1;
            for(String z:allInstructions){
                z = z+" "+g;
                newInstructions.add(z);
                g++;
            }
        }

        // initialize all alu units in map;
        ALU_UNITS.put("MEMORY",null);
        ALU_UNITS.put("ADDER", null);
        ALU_UNITS.put("MULTIPLIER",null);
        ALU_UNITS.put("BRANCH",null);

        //while all instructions are read, for every cycle fetch instruction, check execution, check mem access, wrt cdb and commit
        int cycle =0;

        int counter =0;
        while(counter<newInstructions.size()){
            cycle++;

            String instruction = newInstructions.get(counter);
            String tag = checkWrtCDB(cycle); //update qj,qk vals in rs //keep tags to update in the end?
            checkExec(cycle);
            checkMemAccess(cycle);
            checkCommit(cycle);
            if(tag!=null){
                checkRSQValues(tag);
            }
            boolean fetched = tryFetch(cycle, instruction, branch);
            if(fetched){
                counter++;
            }
        }
        int count =0;
        while(RobList.size()>0){
            cycle++;

            String tag = checkWrtCDB(cycle); //update qj,qk vals in rs
            checkExec(cycle);
            checkMemAccess(cycle);
            checkCommit(cycle);
            if(tag!=null){
                checkRSQValues(tag);
            }
            count++;
        }
        printSpecTomasulo();
    }

    private static void checkExec(int cycle) {
        if(cycle==1){
            return;
        }
        else{
            //check load rs
            if(LOAD_RS.size()>0){
                //check if all register values are available
                //check for all buffers if by inst wise they are waiting
                for(ReservationStation rs: LOAD_RS){
                    if(rs.getQj()==null && rs.getBean().getIssue()<cycle){
                        //check for appropriate bean
                        InstructionBean bean = rs.getBean();
                        if(bean.getExec()==0){
                            bean.setExec(cycle);
                            break;
                        }

                    }
                }

            }
            //check add rs
            if(ALU_UNITS.get("ADDER")==null && ADD_RS.size()>0){
                for(ReservationStation rs: ADD_RS){
                    if(rs.getQj()==null && rs.getQk()==null){
                        InstructionBean bean = rs.getBean();
                        if(bean.getExec()==0){
                            bean.setExec(cycle);
                            ALU_UNITS.replace("ADDER",rs);
                        }
                        break;
                    }
                }
            }

            if(ALU_UNITS.get("MULTIPLIER")==null && MULT_RS.size()>0){
                for(ReservationStation rs: MULT_RS){
                    if(rs.getQj()==null && rs.getQk()==null){
                        InstructionBean bean = rs.getBean();
                        if(bean.getExec()==0){
                            bean.setExec(cycle);
                            ALU_UNITS.replace("MULTIPLIER",rs);
                        }
                        break;
                    }
                }
            }
            if(ALU_UNITS.get("BRANCH")==null && BRANCH_RS.size()>0){
                for(ReservationStation rs: BRANCH_RS){
                    if(rs.getQj()==null && rs.getQk()==null){
                        InstructionBean bean = rs.getBean();
                        if(bean.getExec()==0){
                            bean.setExec(cycle);
                            ALU_UNITS.replace("BRANCH",rs);
                        }
                        break;
                    }
                }
            }
        }
    }

    private static InstructionBean getInstructionBean(int instruction_num) {
        for(InstructionBean instructionBean: instructionQueue){
            if(instructionBean.getInstruction_number()==instruction_num){
                return instructionBean;
            }
        }
        return null;
    }

    private static boolean tryFetch(int cycle, String instruction, Boolean branch) {
        if(RobList.size()>=ROB_SIZE){
            return false;
        }
        String[] array = instruction.split(" ");
        switch(array[0]){
            case "LW":
                //check if load buffers are free
                if(LOAD_RS.size()<LOAD_BUFFER){
                    //create ROB entry
                    ReOrderBuffer rob = new ReOrderBuffer();
                    rob.setType(array[0]);
                    rob.setDestination(array[1]);
                    String name = "ROB"+ROB_TAG;
                    rob.setName(name);
                    rob.setInstruction_number(Integer.parseInt(array[3]));
                    ROB_TAG++;
                    RobList.add(rob);

                    //create a instruction entry
                    InstructionBean bean = new InstructionBean();
                    bean.setInstruction(instruction);
                    bean.setInstruction_number(Integer.parseInt(array[3]));
                    bean.setIssue(cycle);
                    if(branch){
                        bean.setIteration(Integer.parseInt(array[4]));
                    }
                    instructionQueue.add(bean);

                    //Create a reservation station object
                    ReservationStation rs = new ReservationStation();
                    rs.setTag(name);
                    rs.setType("LW");
                    rs.setInstruction_num(Integer.parseInt(array[3]));
                    rs.setBean(bean);
                    //for every register value check if its a destination of any other rob
                    String val = checkIfDataHazard(array[2],ROB_TAG);
                    if(val==null){
                        rs.setVj(array[2]);
                    }
                    else{
                        rs.setQj(val);
                    }
                    LOAD_RS.add(rs);

                    return true;
                }
                return false;

            case "SW":
                if(STORE_RS.size()<STORE_BUFFER){
                    //rob entry
                    ReOrderBuffer rob = new ReOrderBuffer();
                    rob.setType(array[0]);
                    String name = "ROB"+ROB_TAG;
                    rob.setName(name);
                    rob.setInstruction_number(Integer.parseInt(array[3]));
                    ROB_TAG++;
                    //check if first reg in store instruction is being updated by other rob
                    String storeVal = checkIfDataHazard(array[1],ROB_TAG);
                    if(storeVal!=null){
                        rob.setValue(storeVal);
                    }
                    RobList.add(rob);

                    InstructionBean ins = new InstructionBean();
                    ins.setInstruction_number(Integer.parseInt(array[3]));
                    ins.setInstruction(instruction);
                    ins.setIssue(cycle);
                    if(branch){
                        ins.setIteration(Integer.parseInt(array[4]));
                    }
                    instructionQueue.add(ins);

                    ReservationStation rs = new ReservationStation();
                    rs.setType("SW");
                    rs.setTag(name);
                    rs.setInstruction_num(Integer.parseInt(array[3]));
                    rs.setBean(ins);
                    STORE_RS.add(rs);
                    return true;
                }
                return false;

            case "SUB":
            case "ADD":
                if(ADD_RS.size()<ADD_BUFFER){
                    ReOrderBuffer rob = new ReOrderBuffer();
                    rob.setType(array[0]);
                    String name = "ROB"+ROB_TAG;
                    rob.setName(name);
                    rob.setInstruction_number(Integer.parseInt(array[4]));
                    ROB_TAG++;
                    rob.setDestination(array[1]);
                    RobList.add(rob);

                    InstructionBean ins = new InstructionBean();
                    ins.setInstruction_number(Integer.parseInt(array[4]));
                    ins.setInstruction(instruction);
                    ins.setIssue(cycle);
                    if(branch){
                        ins.setIteration(Integer.parseInt(array[5]));
                    }
                    instructionQueue.add(ins);

                    ReservationStation rs = new ReservationStation();
                    rs.setType(array[0]);
                    rs.setTag(name);
                    rs.setBean(ins);
                    rs.setInstruction_num(Integer.parseInt(array[4]));
                    String addVal = checkIfDataHazard(array[2],ROB_TAG);
                    if(addVal==null){
                        rs.setVj(array[2]);
                    }
                    else{
                        rs.setQj(addVal);
                    }
                    addVal = checkIfDataHazard(array[3],ROB_TAG);
                    if(addVal==null){
                        rs.setVk(array[3]);
                    }
                    else{
                        rs.setQk(addVal);
                    }
                    ADD_RS.add(rs);
                    return true;
                }
                return false;

            case "DIV":
            case "MULT":
                if(MULT_RS.size()<MULT_BUFFER){
                    ReOrderBuffer rob = new ReOrderBuffer();
                    rob.setType(array[0]);
                    String name = "ROB"+ROB_TAG;
                    rob.setName(name);
                    rob.setInstruction_number(Integer.parseInt(array[4]));
                    ROB_TAG++;
                    rob.setDestination(array[1]);
                    RobList.add(rob);

                    InstructionBean ins = new InstructionBean();
                    ins.setInstruction_number(Integer.parseInt(array[4]));
                    ins.setInstruction(instruction);
                    ins.setIssue(cycle);
                    if(branch){
                        ins.setIteration(Integer.parseInt(array[5]));
                    }
                    instructionQueue.add(ins);

                    ReservationStation rs = new ReservationStation();
                    rs.setType(array[0]);
                    rs.setTag(name);
                    rs.setBean(ins);
                    rs.setInstruction_num(Integer.parseInt(array[4]));
                    String addVal = checkIfDataHazard(array[2],ROB_TAG);
                    if(addVal==null){
                        rs.setVj(array[2]);
                    }
                    else{
                        rs.setQj(addVal);
                    }
                    addVal = checkIfDataHazard(array[3],ROB_TAG);
                    if(addVal==null){
                        rs.setVk(array[3]);
                    }
                    else{
                        rs.setQk(addVal);
                    }
                    MULT_RS.add(rs);
                    return true;
                }
                return false;

            case "BNE":
            case "BE":
                if(BRANCH_RS.size()<BRANCH_BUFFER){
                    ReOrderBuffer rob = new ReOrderBuffer();
                    rob.setType(array[0]);
                    String name = "ROB"+ROB_TAG;
                    rob.setName(name);
                    rob.setInstruction_number(Integer.parseInt(array[3]));
                    ROB_TAG++;
                    RobList.add(rob);

                    InstructionBean ins = new InstructionBean();
                    ins.setInstruction_number(Integer.parseInt(array[3]));
                    ins.setInstruction(instruction);
                    ins.setIssue(cycle);
                    if(branch){
                        ins.setIteration(Integer.parseInt(array[4]));
                    }
                    instructionQueue.add(ins);

                    ReservationStation rs = new ReservationStation();
                    rs.setType(array[0]);
                    rs.setBean(ins);
                    rs.setTag(name);
                    rs.setInstruction_num(Integer.parseInt(array[3]));
                    String addVal = checkIfDataHazard(array[1],ROB_TAG);
                    if(addVal==null){
                        rs.setVj(array[1]);
                    }
                    else{
                        rs.setQj(addVal);
                    }
                    addVal = checkIfDataHazard(array[2],ROB_TAG);
                    if(addVal==null){
                        rs.setVk(array[2]);
                    }
                    else{
                        rs.setQk(addVal);
                    }
                    BRANCH_RS.add(rs);

                    return true;
                }
                return false;
        }
        return false;
    }

    private static String checkIfDataHazard(String s, int l) {
        int x = l-1;
        if(x==1){
            return null;
        }
        ReOrderBuffer r;
        int len;
        if(RobList.size()==1){
            len=0;
        }
        else{
            len = RobList.size()-2;
        }
        for( int i =len;i>=0;i--){
             r = RobList.get(i);
             //check if its null and then check with val
             if(r.getDestination()!=null && r.getDestination().equalsIgnoreCase(s) && r.getReady()==0){
                 return r.getName();
             }
        }
        return null;
    }

    private static void checkMemAccess(int cycle) {
        //check only for load
        if(cycle<3){
            return;
        }
       // ReservationStation rs = LOAD_RS.get(0);
        if(ALU_UNITS.get("MEMORY")==null){
            for(ReservationStation rs: LOAD_RS){
                int exec = rs.getBean().getExec(); //TBR
                if(rs.getQj()==null && rs.getBean().getExec()<cycle){
                    InstructionBean bean  = rs.getBean();
                    bean.setMemaccess(cycle);
                    ALU_UNITS.put("MEMORY",rs);
                    //MEM_UNIT=true;
                    return;
                }
            }
        }

    }

    private static String checkWrtCDB(int cycle) {
        if(cycle<3){
            return null;
        }
        //create prospective list of execution ready states
        ArrayList<ReservationStation> ready = new ArrayList<>();
        //check for load buffers
        if(ALU_UNITS.get("MEMORY")!=null){
            ReservationStation rs = ALU_UNITS.get("MEMORY");
            InstructionBean bean = rs.getBean();
            if(bean.getMemaccess()!=0 && bean.getMemaccess()<=cycle-memory_cycle){
                ready.add(rs);
            }
        }
        if(ALU_UNITS.get("ADDER")!=null){
            ReservationStation rs = ALU_UNITS.get("ADDER");
            InstructionBean bean = rs.getBean();
            if(bean.getExec()!=0 && bean.getExec()<=cycle-add_cycle){
                ready.add(rs);
            }
        }
        if(ALU_UNITS.get("MULTIPLIER")!=null){
            ReservationStation rs = ALU_UNITS.get("MULTIPLIER");
            InstructionBean bean = rs.getBean();
            if(rs.getType().equalsIgnoreCase("MULT")){
                if(bean.getExec()!=0 && bean.getExec()<=cycle-mult_cycle){
                    ready.add(rs);
                }
            }
            else{
                if(bean.getExec()!=0 && bean.getExec()<=cycle-div_cycle){
                    ready.add(rs);
                }
            }

        }
        if(ALU_UNITS.get("BRANCH")!=null){
            ReservationStation rs = ALU_UNITS.get("BRANCH");
            InstructionBean bean = rs.getBean();
            if(bean.getExec()!=0 && bean.getExec()<=cycle-branch_cycle){
                //ready.add(rs);
                setROBStateReady(rs.getTag());
                removeRS(rs.getTag(),rs.getType());
            }
        }

        if(ready.size()==1){
            ReservationStation rs = ready.get(0);
            rs.getBean().setWriteCDB(cycle);
            //get rob tag and update it to ready
            setROBStateReady(rs.getTag());
            //remove from rs buffer
            removeRS(rs.getTag(),rs.getType());
            //also check any rs waiting for the tag
            //checkRSQValues(rs.getTag());
            return rs.getTag();
        }
        else if(ready.size()>1){
            //choose rs such that the inst number is min
            ReservationStation rs=null;
            int min=Integer.MAX_VALUE;
            for(ReservationStation r:ready){
                if(r.getInstruction_num()<min){
                    rs = r;
                    min = r.getInstruction_num();
                }
            }
            rs.getBean().setWriteCDB(cycle);
            //get rob tag and update it to ready
            setROBStateReady(rs.getTag());
            //remove from rs buffer
            removeRS(rs.getTag(),rs.getType());
            //also check any rs waiting for the tag
           // checkRSQValues(rs.getTag());
            return rs.getTag();
        }
    return null;
    }

    private static void checkRSQValues(String tag) {
        for(ReservationStation rs: LOAD_RS){
            if(!(rs.getQk()==null) && rs.getQk().equalsIgnoreCase(tag)){
                rs.setQk(null);
            }
            if(!(rs.getQj()==null) && rs.getQj().equalsIgnoreCase(tag)){
                rs.setQj(null);
            }
        }
        for(ReservationStation rs:ADD_RS){
            if(!(rs.getQk()==null) && rs.getQk().equalsIgnoreCase(tag)){
                rs.setQk(null);
            }
            if(!(rs.getQj()==null) && rs.getQj().equalsIgnoreCase(tag)){
                rs.setQj(null);
            }
        }
        for(ReservationStation rs:MULT_RS){
            if(!(rs.getQk()==null) && rs.getQk().equalsIgnoreCase(tag)){
                rs.setQk(null);
            }
            if(!(rs.getQj()==null) && rs.getQj().equalsIgnoreCase(tag)){
                rs.setQj(null);
            }
        }
        for(ReservationStation rs:BRANCH_RS){
            if(!(rs.getQk()==null) && rs.getQk().equalsIgnoreCase(tag)){
                rs.setQk(null);
            }
            if(!(rs.getQj()==null) && rs.getQj().equalsIgnoreCase(tag)){
                rs.setQj(null);
            }
        }
    }

    private static void removeRS(String tag,String type) {
        if(type.equalsIgnoreCase("LW")){
            for(int i =0;i<LOAD_RS.size();i++){
                if(tag==LOAD_RS.get(i).getTag()){
                    LOAD_RS.remove(i);
                }
            }
            ALU_UNITS.put("MEMORY",null);
        }
        else if(type.equalsIgnoreCase("ADD")||type.equalsIgnoreCase("SUB")) {
            for (int i = 0; i < ADD_RS.size(); i++) {
                if (tag == ADD_RS.get(i).getTag()) {
                    ADD_RS.remove(i);
                }
            }
            ALU_UNITS.put("ADDER",null);
        }
        else if(type.equalsIgnoreCase("MULT")||type.equalsIgnoreCase("DIV")) {
            for (int i = 0; i < MULT_RS.size(); i++) {
                if (tag == MULT_RS.get(i).getTag()) {
                    MULT_RS.remove(i);
                }
            }
            ALU_UNITS.put("MULTIPLIER",null);
        }
        else if(type.equalsIgnoreCase("BNE")||type.equalsIgnoreCase("BE")) {
            for (int i = 0; i < BRANCH_RS.size(); i++) {
                if (tag == BRANCH_RS.get(i).getTag()) {
                    BRANCH_RS.remove(i);
                }
            }
            ALU_UNITS.put("BRANCH", null);
        }
    }

    private static void setROBStateReady(String tag) {
        for(ReOrderBuffer rob: RobList){
            if(rob.getName().equalsIgnoreCase(tag)){
                rob.setReady(1);
            }
            //check any store insts waiting for this tag
            if(rob.getType().equalsIgnoreCase("SW")){
                rob.setValue(null);
                rob.setReady(1);
                for(int i=0;i<STORE_RS.size();i++){
                    ReservationStation r = STORE_RS.get(i);
                    if(rob.getName().equalsIgnoreCase(r.tag)){
                        STORE_RS.remove(i);
                        break;
                    }
                }
            }
        }
    }

    private static void checkCommit(int cycle) {
        if(cycle<2){
            return;
        }
        if(RobList.size()>0){
            ReOrderBuffer rob = RobList.get(0);
            if(rob.getReady()==1){
                int inst_num=rob.getInstruction_number();
                InstructionBean bean = getInstructionBean(inst_num);
                if(bean.getWriteCDB()<cycle){
                    bean.setCommit(cycle);
                    RobList.remove(0);
                }
            }
        }
    }

    private static void printSpecTomasulo() {
        System.out.println("****Speculative Tomasulo Results****");
        System.out.println("===========================================================================================================");
        System.out.format("%15s%15s%15s%15s%15s%15s%15s", "Iteration", "Instruction", "Issue", "Exec", "Mem access", "Wrt CDB", "Commit");
        System.out.println();
        System.out.println("============================================================================================================");
        for (InstructionBean bean : instructionQueue) {
            System.out.format("%15s%15s%15s%15s%15s%15s%15s", bean.getIteration(), bean.getInstruction().substring(0,bean.getInstruction().length()-4),
                    bean.getIssue(), bean.getExec(), bean.getMemaccess(), bean.getWriteCDB(), bean.getCommit());
            System.out.println();
  }
    }

}