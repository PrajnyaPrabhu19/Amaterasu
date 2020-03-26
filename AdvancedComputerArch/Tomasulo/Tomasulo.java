

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Tomasulo {

    //number of cycles for each operation
    private static int add_cycle = 2;
    private static int mult_cycle = 1;
    private static int div_cycle = 1;
    private static int memory_cycle = 1;
    private static int branch_cycle = 1;

    //Have ALU as Map, Units as keys as only one of each are available and the RS object which acquired the lock
    private static HashMap<String, ReservationStation> ALU_UNITS = new HashMap<>();

    //reservation station units
    private static int ADD_BUFFER = 3;
    private static int MULT_BUFFER = 2;
    private static int LOAD_BUFFER = 5;
    private static int BRANCH_BUFFER = 2;
    private static int STORE_BUFFER = 3;

    private static ArrayList<InstructionBean> instructionQueue = new ArrayList<>();
    //maintain list of current instructions in buffers
    private static ArrayList<ReservationStation> LOAD_RS = new ArrayList<>();
    private static ArrayList<ReservationStation> STORE_RS = new ArrayList<>();
    private static ArrayList<ReservationStation> ADD_RS = new ArrayList<>();
    private static ArrayList<ReservationStation> MULT_RS = new ArrayList<>();
    private static ArrayList<ReservationStation> BRANCH_RS = new ArrayList<>();

    private static HashMap<Integer, String> destinationMap = new HashMap<>();

    public static void main(String[] args) {
        if(args.length<12){
            System.out.println("Please enter all 11 required values in following format and rerun");
            System.out.println("java ProgramName fileName load_buffer store_buffer add_buffer mult_buffer branch_buffer" +
                    " memory_access_cycle add_cycle mult_cycle div_cycle branch_cycle branch_prediction");
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
        String branch_predictor = args[11];
      //  String branch_predictor ="NT";
      //  String instructionFile = "/Users/prajnyaprabhu/IdeaProjects/ACA/src/main/java/arch/instTest.txt";
        boolean branch = false;
        ArrayList<String> newInstructions = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(instructionFile))) {
            String line;
            int i=1;
            while ((line = br.readLine()) != null) {
                //line=line+" "+i;
                if(branch_predictor.equalsIgnoreCase("T")){
                    if(line.charAt(0)=='B'){
                        branch =true;
                    }
                }

                newInstructions.add(line);
                // i++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int i= 1;
        ArrayList<String> allInstructions = new ArrayList<>();
        if(branch){
            //find the inst index where branch is present
            int ind=0;
            for(int j=0;j<newInstructions.size();j++){
                if(newInstructions.get(j).charAt(0)=='B'){
                    ind =j;
                    break;
                }
            }
            int counter =0;
            while (counter<4){
                for(int k =0;k<=ind;k++){
                    String s =newInstructions.get(k);
                    String x = s+" "+i+" "+counter;
                    allInstructions.add(x);
                    i++;
                }
                counter++;
            }
            counter--;
            for(int k=ind+1;k<newInstructions.size();k++){
                String s =newInstructions.get(k);
                String x = s+" "+i+" "+counter;
                allInstructions.add(x);
                i++;
            }
        }
        else{
            int g =1;
            for(String z:newInstructions){
                z = z+" "+g;
                allInstructions.add(z);
                g++;
            }
        }
        // initialize all alu units in map;
        ALU_UNITS.put("MEMORY",null);
        ALU_UNITS.put("ADDER", null);
        ALU_UNITS.put("MULTIPLIER",null);
        ALU_UNITS.put("BRANCH",null);

        int cycle =0;
        int counter =0;
        while(counter<allInstructions.size()){
            cycle++;
            String instruction = allInstructions.get(counter);

            String tag = checkWrtCDB(cycle);
            checkExec(cycle);
            checkMemAccess(cycle);
            if(tag!=null){
                checkRSQValues(tag);
            }
            boolean fetched = tryFetch(cycle, instruction,branch);
            if(fetched){
                counter++;
            }
        }
        while (destinationMap.size()>0){
            cycle++;
            checkExec(cycle);
            String tag = checkWrtCDB(cycle);
            checkMemAccess(cycle);
            if(tag!=null){
                checkRSQValues(tag);
            }
        }

        printTomasulo();
    }

    private static void printTomasulo() {
        System.out.println("**** Non-Speculative Tomasulo Results ****");
        System.out.println("=================================================================================================");
        System.out.format("%20s%20s%15s%15s%15s%15s", "Iteration", "Instruction", "Issue", "Exec", "Mem access", "Wrt CDB");
        System.out.println();
        System.out.println("=================================================================================================");
        for (InstructionBean bean : instructionQueue) {
            System.out.format("%20s%20s%15s%15s%15s%15s", bean.getIteration(), bean.getInstruction().substring(0,bean.getInstruction().length()-4),
                    bean.getIssue(), bean.getExec(), bean.getMemaccess(), bean.getWriteCDB());
            System.out.println();
        }
    }

    private static String checkWrtCDB(int cycle) {
        if (cycle < 3) {
            return null;
        }
        HashMap<ReservationStation, String> ready = new HashMap<>();
        if (ALU_UNITS.get("MEMORY") != null) {
            ReservationStation rs = ALU_UNITS.get("MEMORY");
            InstructionBean bean = rs.getBean();
            if (bean.getMemaccess() != 0 && bean.getMemaccess() <= cycle - memory_cycle) {
                ready.put(rs,"L");
            }
        }
        if (ALU_UNITS.get("ADDER") != null) {
            ReservationStation rs = ALU_UNITS.get("ADDER");
            InstructionBean bean = rs.getBean();
            if (bean.getExec() != 0 && bean.getExec() <= cycle - add_cycle) {
                ready.put(rs,"A");
            }
        }
        if (ALU_UNITS.get("MULTIPLIER") != null) {
            ReservationStation rs = ALU_UNITS.get("MULTIPLIER");
            InstructionBean bean = rs.getBean();
            if (rs.getType().equalsIgnoreCase("MULT")) {
                if (bean.getExec() != 0 && bean.getExec() <= cycle - mult_cycle) {
                    ready.put(rs,"M");
                }
            } else {
                if (bean.getExec() != 0 && bean.getExec() <= cycle - div_cycle) {
                    ready.put(rs,"M");
                }
            }

        }
        if (ALU_UNITS.get("BRANCH") != null) {
            ReservationStation rs = ALU_UNITS.get("BRANCH");
            InstructionBean bean = rs.getBean();
            if (bean.getExec() != 0 && bean.getExec() <= cycle - branch_cycle) {
                removeRS(rs.getInstruction_num(),"B");
                destinationMap.remove(rs.getInstruction_num());
            }
        }

        if (ready.size() == 1) {
            ReservationStation rs = ready.keySet().iterator().next();
            if(!rs.getType().equalsIgnoreCase("SW")){
                rs.getBean().setWriteCDB(cycle);
            }

            removeRS(rs.getInstruction_num(),ready.get(rs));
            destinationMap.remove(rs.getInstruction_num());
            return ""+rs.getInstruction_num();
        } else if (ready.size() > 1) {
            //choose rs such that the inst number is min
            ReservationStation rs = null;
            int min = Integer.MAX_VALUE;
            for(Map.Entry<ReservationStation,String> entry: ready.entrySet()){
                if(entry.getKey().getInstruction_num()<min){
                    rs = entry.getKey();
                    min =rs.getInstruction_num();

                }
            }
            if(!rs.getType().equalsIgnoreCase("SW")){
                rs.getBean().setWriteCDB(cycle);
            }
            removeRS(rs.getInstruction_num(),ready.get(rs));
            destinationMap.remove(rs.getInstruction_num());
            return ""+rs.getInstruction_num();
        }
        return null;
    }

    private static void removeRS(int instruction_num, String b) {
        switch (b){
            case "A":
                for(int i=0;i<ADD_RS.size();i++){
                    ReservationStation r = ADD_RS.get(i);
                    if(r.getInstruction_num()==instruction_num){
                        ADD_RS.remove(i);
                        ALU_UNITS.replace("ADDER",null);
                        return;
                    }
                }
                break;

            case "B":
                for(int i=0;i<BRANCH_RS.size();i++){
                    ReservationStation r = BRANCH_RS.get(i);
                    if(r.getInstruction_num()==instruction_num){
                        BRANCH_RS.remove(i);
                        ALU_UNITS.replace("BRANCH",null);
                        return;
                    }
                }
                break;

            case "L":
                for(int i=0;i<LOAD_RS.size();i++){
                    ReservationStation r = LOAD_RS.get(i);
                    if(r.getInstruction_num()==instruction_num){
                        LOAD_RS.remove(i);
                        ALU_UNITS.replace("MEMORY",null);
                        return;
                    }
                }
                for(int i=0;i<STORE_RS.size();i++){
                    ReservationStation r = STORE_RS.get(i);
                    if(r.getInstruction_num()==instruction_num){
                        STORE_RS.remove(i);
                        ALU_UNITS.replace("MEMORY",null);
                        return;
                    }
                }
                break;

            case "M":
                for(int i=0;i<MULT_RS.size();i++){
                    ReservationStation r = MULT_RS.get(i);
                    if(r.getInstruction_num()==instruction_num){
                        MULT_RS.remove(i);
                        ALU_UNITS.replace("MULTIPLIER",null);
                        return;
                    }
                }
                break;
        }
    }

    private static void checkMemAccess(int cycle) {
        if(cycle<3){
            return;
        }
        if(ALU_UNITS.get("MEMORY")==null){
            for(ReservationStation rs: LOAD_RS){
                int exec = rs.getBean().getExec(); //TBR
                if(rs.getQj()==null && rs.getBean().getExec()<cycle){
                    InstructionBean bean  = rs.getBean();
                    bean.setMemaccess(cycle);
                    ALU_UNITS.put("MEMORY",rs);
                    return;
                }
            }
            //check for store insts
            for(ReservationStation rs: STORE_RS){
                int exec = rs.getBean().getExec(); //TBR
                if(rs.getQj()==null && rs.getBean().getExec()<cycle){
                    InstructionBean bean  = rs.getBean();
                    bean.setMemaccess(cycle);
                    ALU_UNITS.put("MEMORY",rs);
                    return;
                }
            }
        }
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

    private static boolean tryFetch(int cycle, String instruction,boolean branch) {
        String[] array = instruction.split(" ");
        switch (array[0]){
            case "LW":
                if(LOAD_RS.size()<LOAD_BUFFER){
                    InstructionBean bean = new InstructionBean();
                    bean.setInstruction(instruction);
                    bean.setInstruction_number(Integer.parseInt(array[3]));
                    bean.setIssue(cycle);
                    if(branch){
                        bean.setIteration(Integer.parseInt(array[4]));
                    }
                    instructionQueue.add(bean);

                    destinationMap.put(Integer.parseInt(array[3]), array[1]);

                    ReservationStation rs = new ReservationStation();
                    rs.setType("LW");
                    rs.setInstruction_num(Integer.parseInt(array[3]));
                    rs.setBean(bean);
                    String val = checkDataHazard(Integer.parseInt(array[3]), array[2]);
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
                    InstructionBean bean = new InstructionBean();
                    bean.setInstruction(instruction);
                    bean.setInstruction_number(Integer.parseInt(array[3]));
                    bean.setIssue(cycle);
                    if(branch){
                        bean.setIteration(Integer.parseInt(array[4]));
                    }
                    instructionQueue.add(bean);

                    destinationMap.put(Integer.parseInt(array[3]), "store");

                    ReservationStation rs = new ReservationStation();
                    rs.setType("SW");
                    rs.setInstruction_num(Integer.parseInt(array[3]));
                    rs.setBean(bean);
                    String val = checkDataHazard(Integer.parseInt(array[3]),array[1]);
                    if(val==null){
                        rs.setVj(array[2]);
                    }
                    else{
                        rs.setQj(val);
                    }
                    STORE_RS.add(rs);
                    return true;
                }
                return false;

            case "ADD":
            case "SUB":
                if(ADD_RS.size()<ADD_BUFFER){
                    InstructionBean ins = new InstructionBean();
                    ins.setInstruction_number(Integer.parseInt(array[4]));
                    ins.setInstruction(instruction);
                    ins.setIssue(cycle);
                    if(branch){
                        ins.setIteration(Integer.parseInt(array[4]));
                    }
                    instructionQueue.add(ins);

                    destinationMap.put(Integer.parseInt(array[4]), array[1]);
                    ReservationStation rs = new ReservationStation();
                    rs.setType(array[0]);
                    rs.setBean(ins);
                    rs.setInstruction_num(Integer.parseInt(array[4]));
                    String addVal = checkDataHazard(Integer.parseInt(array[4]),array[2]);
                    if(addVal==null){
                        rs.setVj(array[2]);
                    }
                    else{
                        rs.setQj(addVal);
                    }
                    addVal = checkDataHazard(Integer.parseInt(array[4]),array[3]);
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

            case "MULT":
            case "DIV" :
                if(MULT_RS.size()<MULT_BUFFER){
                    InstructionBean ins = new InstructionBean();
                    ins.setInstruction_number(Integer.parseInt(array[4]));
                    ins.setInstruction(instruction);
                    ins.setIssue(cycle);
                    if(branch){
                        ins.setIteration(Integer.parseInt(array[4]));
                    }
                    instructionQueue.add(ins);
                    destinationMap.put(Integer.parseInt(array[4]), array[1]);
                    int instr_no = Integer.parseInt(array[4]);
                    ReservationStation rs = new ReservationStation();
                    rs.setType(array[0]);
                    rs.setBean(ins);
                    rs.setInstruction_num(instr_no);
                    String addVal = checkDataHazard(instr_no,array[2]);
                    if(addVal==null){
                        rs.setVj(array[2]);
                    }
                    else{
                        rs.setQj(addVal);
                    }
                    addVal = checkDataHazard(instr_no,array[3]);
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
                    InstructionBean ins = new InstructionBean();
                    ins.setInstruction_number(Integer.parseInt(array[3]));
                    ins.setInstruction(instruction);
                    ins.setIssue(cycle);
                    if(branch){
                        ins.setIteration(Integer.parseInt(array[4]));
                    }
                    instructionQueue.add(ins);
                    destinationMap.put(Integer.parseInt(array[3]), array[1]);

                    int inst_no = Integer.parseInt(array[3]);
                    ReservationStation rs = new ReservationStation();
                    rs.setType(array[0]);
                    rs.setBean(ins);
                    rs.setInstruction_num(inst_no);
                    String addVal = checkDataHazard(inst_no,array[1]);
                    if(addVal==null){
                        rs.setVj(array[1]);
                    }
                    else{
                        rs.setQj(addVal);
                    }
                    addVal = checkDataHazard(inst_no,array[2]);
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

    private static String checkDataHazard(int inst_num, String register) {
        if(destinationMap.size()>0) {
            for (int i = inst_num - 1; i >= 1; i--) {
                if (destinationMap.get(i)!=null && destinationMap.get(i).equalsIgnoreCase(register)) {
                    return "" + i;
                }
            }
        }
        return null;
    }

    private static void checkRSQValues(String tag) {
        for(ReservationStation rs: LOAD_RS){
            if(!(rs.getQj()==null) && rs.getQj().equalsIgnoreCase(tag)){
                rs.setQj(null);
            }
        }
        for(ReservationStation rs: STORE_RS){
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

}
