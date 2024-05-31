import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;

public class ULinkList {
    /** contain items and their utilities in original transaction.
     * can be replaced by Transaction class which contains transactionUtility and sid??? */
    UItem[] seq;//fuzzy item & fuzzy utility
    double[] remainingUtility;//remaining-utility index

    BitSet itemSetIndex;//element-index
    String [] header;//item-indices table
    Integer [][] headerIndices;//item-indices table


    public ULinkList(){}

    public int length() {
        return seq.length;
    }

    public String FuzzyItemName(int ind) {
        return seq[ind].fuzzyitemName();
    }
    public FuzzyItem FuzzyItem(int ind) {
        return seq[ind].fuzzyItem;
    }
    public int ItemName(int ind) {
        return seq[ind].fuzzyItem.item;
    }

    public double utility(int ind) {
        return seq[ind].utility();
    }

    public double remainUtility(int ind) {
        return remainingUtility[ind];
    }

    public void setRemainUtility(int ind, double remainUtility) {
         this.remainingUtility[ind] = remainUtility;
    }

    public int headerLength() {
        return header.length;
    }
    //？？？？？？？？？？？？？？？？？
    public int nextItemsetPos(int ind) {
        return itemSetIndex.nextSetBit(ind + 1);
    }
    //？？？？？？？？？？？？？？？？？
    public int whichItemset(int ind) {
        return itemSetIndex.previousSetBit(ind);
    }


    public Integer[] getItemIndices(String fuzzyItem_str){
        int i = Arrays.binarySearch(header, fuzzyItem_str, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                int dotpos =o1.indexOf(".");
                int item1=Integer.parseInt(o1.substring(0,dotpos));
                int region1=Integer.parseInt(o1.substring(dotpos+1));
                dotpos =o2.indexOf(".");
                int item2=Integer.parseInt(o2.substring(0,dotpos));
                int region2=Integer.parseInt(o2.substring(dotpos+1));
                int compare=Integer.compare(item1,item2);
                if (compare==0){
                    compare=Integer.compare(region1,region2);
                }
                return compare;
            }
        });
        if(i < 0)
            return null;
        return headerIndices[i];
    }

    public int lastItem(){
       UItem lastUItem= seq[seq.length-1];
        return lastUItem.fuzzyItem.item;
    }

}
