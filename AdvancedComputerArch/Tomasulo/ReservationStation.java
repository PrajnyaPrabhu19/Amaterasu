

public class ReservationStation {
    String type; //op
    String tag;

    String vj;
    String vk;
    String qj;
    String qk;
    String dest;

    int instruction_num;
    InstructionBean bean;

    public ReservationStation() {
        this.vj=null;
        this.vk=null;
        this.qj=null;
        this.qk=null;
    }

    public int getInstruction_num() {
        return instruction_num;
    }

    public void setInstruction_num(int instruction_num) {
        this.instruction_num = instruction_num;
    }

    public InstructionBean getBean() {
        return bean;
    }

    public void setBean(InstructionBean bean) {
        this.bean = bean;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getVj() {
        return vj;
    }

    public void setVj(String vj) {
        this.vj = vj;
    }

    public String getVk() {
        return vk;
    }

    public void setVk(String vk) {
        this.vk = vk;
    }

    public String getQj() {
        return qj;
    }

    public void setQj(String qj) {
        this.qj = qj;
    }

    public String getQk() {
        return qk;
    }

    public void setQk(String qk) {
        this.qk = qk;
    }

    public String getDest() {
        return dest;
    }

    public void setDest(String dest) {
        this.dest = dest;
    }
}
