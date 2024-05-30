

public class FUZZUtil {
//    static String[] terms={"L","M","H"};
    static Para para=new Para(1.0,6.0,11.0);
    static Para para2=new Para(1.0,26.0,51.0);

    public static double M_TriMembership(double x, Para para) {

        if (x <= para.a || x > para.b) {
            return 0.0;
        } else if (x > para.a && x <= para.m) {
            return (x - para.a) / (para.m - para.a);
        } else if (x > para.m && x <= para.b) {
            return (para.b - x) / (para.b - para.m);
        }
        return 0.0;
    }
    public static double L_TriMembership(double x, Para para) {

        if (x <= para.a) {
            return 1.0;
        } else if (x > para.a && x <= para.m) {
            return (para.m - x) / (para.m - para.a);
        } else {
            return 0.0;
        }
    }
    public static double R_TriMembership(double x, Para para) {

        if (x <= para.m) {
            return 0.0;
        } else if (x > para.m && x <= para.b) {
            return (x - para.m) / (para.b - para.m);
        } else  {
            return 1.0;
        }
    }
    public static FuzzySet[] Fuzz(Integer item, double quantity,Para para) {
        FuzzySet[] fuzzySets=new FuzzySet[3];
        fuzzySets[0]=new FuzzySet(new FuzzyItem(item,0),L_TriMembership(quantity,para));
        fuzzySets[1]=new FuzzySet(new FuzzyItem(item,1),M_TriMembership(quantity,para));
        fuzzySets[2]=new FuzzySet(new FuzzyItem(item,2),R_TriMembership(quantity,para));
        fuzzySets[0].fuzzy_item= new FuzzyItem(item,0);
        return fuzzySets;
    }
    
    public static void main(String[] args) {

        FuzzySet[] res=Fuzz(2,6.0,FUZZUtil.para);
        System.out.println(res);

    }
}
