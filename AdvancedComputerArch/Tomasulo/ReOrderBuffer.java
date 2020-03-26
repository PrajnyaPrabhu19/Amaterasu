

public class ReOrderBuffer {
    String type; //operationtype
    String destination; //rx
    int ready;//0 or 1
    String value; //ROBx only for store
    String name; //ROB1
    int instruction_number;

    public ReOrderBuffer() {
        this.ready=0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public int getReady() {
        return ready;
    }

    public void setReady(int ready) {
        this.ready = ready;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getInstruction_number() {
        return instruction_number;
    }

    public void setInstruction_number(int instruction_number) {
        this.instruction_number = instruction_number;
    }
}
