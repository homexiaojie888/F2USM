public class UItem {
     FuzzyItem fuzzyItem;
    //表示L，M，H
     double utility;


    public UItem(FuzzyItem fuzzyItem, double utility) {
        this.fuzzyItem = fuzzyItem;
        this.utility = utility;
    }

    public double utility() {
        return utility;
    }


    public String fuzzyitemName() {
        return fuzzyItem.toString();
    }

}
