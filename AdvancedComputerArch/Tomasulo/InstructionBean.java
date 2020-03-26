

public class InstructionBean {
    String instruction; //full instruction
    //String instructionType;
    String comment;
    int instruction_number;

    String destination_register;
    //all the ints represent the cycle number. When it was issued, when it was executed, etc
    int issue;
    int exec;
    int memaccess;
    int writeCDB;
    int commit;
    int Iteration;

    //set 0 values to few fields which can be null till the end


    public InstructionBean() {
        this.exec=0;
        this.memaccess=0;
        this.writeCDB =0;
        this.commit =0;
        this.comment=null;
        this.Iteration=0;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

//    public void setInstructionType(String instructionType){
//        this.instructionType = instructionType;
//    }
//
//    public String getInstructionType(){
//        return instructionType;
//    }

    public void setIssue(int i){
        this.issue = i;
    }

    public int getIssue(){
        return issue;
    }

    public void setExec(int exec){
        this.exec = exec;
    }

    public int getExec() {
        return exec;
    }

    public int getMemaccess() {
        return memaccess;
    }

    public void setMemaccess(int memaccess) {
        this.memaccess = memaccess;
    }

    public void setWriteCDB(int writeCDB) {
        this.writeCDB = writeCDB;
    }

    public void setCommit(int commit) {
        this.commit = commit;
    }

    public int getWriteCDB() {
        return writeCDB;
    }

    public int getCommit() {
        return commit;
    }

    public String getDestination_register() {
        return destination_register;
    }

    public void setDestination_register(String destination_register) {
        this.destination_register = destination_register;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public InstructionBean getBean(){
        return this;
    }

    public int getInstruction_number() {
        return instruction_number;
    }

    public void setInstruction_number(int instruction_number) {
        this.instruction_number = instruction_number;
    }

    public int getIteration() {
        return Iteration;
    }

    public void setIteration(int iteration) {
        Iteration = iteration;
    }
}
