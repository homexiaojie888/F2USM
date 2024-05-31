/**
 * Created by Jiexiong Zhang, Wensheng Gan @HITsz, China
 */
public class UPosition {
    private int index;
    private double utility;

//    private double peu_of_pos;

    public UPosition(int index, double utility) {
        this.index = index;
        this.utility = utility;
//        this.peu_of_pos=peu;
    }
    
    public int index() {
        return index;
    }
    
    public double utility() {
        return utility;
    }

//    public double getPeu() {
//        return peu_of_pos;
//    }

    @Override
    public String toString() {
        String ret = "";
        ret += "(index: " + index + "  utility: " + utility  +")";
        return ret;
    }
}
