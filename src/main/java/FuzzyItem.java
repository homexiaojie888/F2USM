public class FuzzyItem implements Comparable {
    int item;
    int region;

    public FuzzyItem(int item, int region) {
        this.item = item;
        this.region = region;
    }

    @Override
    public boolean equals(Object obj) {
        FuzzyItem ori=(FuzzyItem)obj;
        if (ori.item==item&&ori.region==region){
            return true;
        }else {
            return false;
        }
    }


    @Override
    public int compareTo(Object o) {
        FuzzyItem ori=(FuzzyItem)o;
        int res=Integer.compare(item,ori.item);
        if (res==0){
            res=Integer.compare(region,ori.region);
        }
        return res;
    }

    @Override
    public String toString() {
        return item + "." + region;
    }
}
