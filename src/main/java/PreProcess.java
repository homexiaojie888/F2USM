import java.io.*;
import java.util.*;
public class PreProcess {
    int max_qual=Integer.MIN_VALUE;
    int min_qual=Integer.MAX_VALUE;
    class ItemQual{
        int item;
        int qual;

        public ItemQual(int item, int qual) {
            this.item = item;
            this.qual = qual;
        }
    }


    public static void main(String[] args) throws IOException {
        //   BIBLE  BMS  Kosarak10k   FIFA  SIGN  Leviathan
    //Yoochoose
        String input="./datasets/" + "BIBLE" + ".txt";
        String output="./datasets/" + "BIBLE_item_to_profits" + ".txt";
        String newDB="./datasets/" + "BIBLE_quantity" + ".txt";
        PreProcess preProcess=new PreProcess();
        //将不同的item所有可能的utility存起来
        Map<Integer,Set<Integer>> mapItemToUtilitys=preProcess.LoadDBToMap(input);
        //求每个item的最大公约数，将其作为利润，并存储起来
        Map<Integer,Integer> mapItemToGcd=preProcess.PrintToFile(mapItemToUtilitys,output);
        //  根据最大公约数计算每个item的数量，并重新生成新的数据文件
//        HUSP_SP_Algo huspSpAlgo=new HUSP_SP_Algo(output);
//
//        Map<Integer,Integer> mapItemToGcd=huspSpAlgo.loadProfit(output);
        preProcess.resetDB(mapItemToGcd,input,newDB);

        System.out.println();
    }

    Map<Integer,Set<Integer>> LoadDBToMap(String input) throws IOException {
        Map<Integer,Set<Integer>> mapItemToUtilitys=new HashMap<>();
        BufferedReader myInput = null;
        String thisLine;
        try {
            myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(input))));
            while ((thisLine = myInput.readLine()) != null) {
                String str[] = thisLine.trim().split(" -2 ");
                String tokens[]=str[0].split(" ");
                for (int i = 0; i < tokens.length-1; i++) {
                    String currentToken = tokens[i];
                    if (currentToken.charAt(0) != '-') {
                        Integer item = Integer.parseInt(currentToken.trim().substring(0, currentToken.trim().indexOf("[")));
                        Integer utility = Integer.parseInt(currentToken.trim().substring(currentToken.trim().indexOf("[") + 1, currentToken.trim().indexOf("]")));
                        if (mapItemToUtilitys.get(item)==null){
                            Set<Integer> Uset=new HashSet<>();
                            Uset.add(utility);
                            mapItemToUtilitys.put(item,Uset);
                        }else {
                            mapItemToUtilitys.get(item).add(utility);
                        }


                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (myInput != null) {
                myInput.close();
            }
        }
        return mapItemToUtilitys;

    }

    private void resetDB(Map<Integer, Integer> mapItemToGcd, String input, String newDB) throws IOException {
        ArrayList<ItemQual[]> DB = new ArrayList<>();
        ArrayList<Integer> SU_of_DB = new ArrayList<>();
        ArrayList<Double> MFSU_of_DB = new ArrayList<>();
        BufferedReader myInput = null;
        String thisLine;
        try {
            myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(input))));
            while ((thisLine = myInput.readLine()) != null) {
                int SU=0;
                String str[] = thisLine.trim().split(" -2 ");
//                String SU_str = str[1];
//                int SU = Integer.parseInt(SU_str.substring(SU_str.indexOf(":") + 1));

                String tokens[]=str[0].split(" ");
                ItemQual itemQuals[]=new ItemQual[tokens.length];
                double mfsu=0.0;
                for (int i = 0; i < tokens.length; i++) {
                    String currentToken = tokens[i];
                    ItemQual itemQual;
                    if (currentToken.charAt(0) != '-') {
                        double item_mfsu=0.0;
                        Integer item = Integer.parseInt(currentToken.trim().substring(0, currentToken.trim().indexOf("[")));
                        Integer utility = Integer.parseInt(currentToken.trim().substring(currentToken.trim().indexOf("[") + 1, currentToken.trim().indexOf("]")));
                        Integer profit=mapItemToGcd.get(item);
                        int quantity=utility/profit;
//                        Integer quantity=Integer.parseInt(currentToken.trim().substring(currentToken.trim().indexOf("[") + 1, currentToken.trim().indexOf("]")));
//                        Integer utility=profit*quantity;
                        SU+=utility;
                        max_qual=Math.max(quantity,max_qual);
                        min_qual=Math.min(quantity,min_qual);
                        itemQual=new ItemQual(item,quantity);
                        FuzzySet[] fuzzySets=FUZZUtil.Fuzz(item, quantity,FUZZUtil.para);
                        double sum_Memshhip=0.0;
                        for (int j = 0; j < fuzzySets.length; j++) {
                            if (Double.compare(fuzzySets[j].fuzzy_val,0.0)>0){
                                double fuzzy_utility=fuzzySets[j].fuzzy_val*utility;
                                item_mfsu=Math.max(item_mfsu,fuzzy_utility);
                                sum_Memshhip+=fuzzySets[j].fuzzy_val;
                            }
                        }
                        if (Double.compare(Math.abs(sum_Memshhip-1.0),0.0)>0){
                            System.out.println("隶属度之和不为0");
                        }
                        mfsu+=item_mfsu;
//                        System.out.println("item:"+item+" qual:"+ quantity+" mfsu:"+item_mfsu);

                    }else {
                        itemQual=new ItemQual(-1,-1);
                    }
                    itemQuals[i]=itemQual;
                }
                MFSU_of_DB.add(mfsu);
                SU_of_DB.add(SU);
                DB.add(itemQuals);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (myInput != null) {
                myInput.close();
            }
        }
        System.out.println("最大数量："+max_qual);
        System.out.println("最小数量："+min_qual);
        BufferedWriter writer=new BufferedWriter(new FileWriter(newDB));
        for (int i = 0; i < DB.size(); i++) {
            ItemQual[] itemQuals=DB.get(i);
            for (int j = 0; j < itemQuals.length; j++) {
                ItemQual itemQual=itemQuals[j];
                if (itemQual.item>0){
                    writer.write(itemQual.item+"["+itemQual.qual+"] ");
                }else {
                    writer.write("-1 ");
                }

            }
            writer.write("-2 "+"SUtility:"+SU_of_DB.get(i));
            writer.write(" MFSU:"+MFSU_of_DB.get(i));
            writer.newLine();

        }
        writer.flush();
    }

    private Map<Integer,Integer> PrintToFile(Map<Integer, Set<Integer>> mapItemToUtilitys,String output) throws IOException {
        Map<Integer,Integer> mapItemToGcd=new HashMap<>();
        for (Integer item:mapItemToUtilitys.keySet()) {
            Set<Integer> Uset=mapItemToUtilitys.get(item);
            int[] utils=Uset.stream().mapToInt(Integer::intValue).toArray();
            int gcd=Stein.gcdOfMultipleNumbers(utils);
            mapItemToGcd.put(item,gcd);
        }
        BufferedWriter writer=new BufferedWriter(new FileWriter(output));
        for (Integer item:mapItemToGcd.keySet()) {
            int gcd=mapItemToGcd.get(item);
            //如果利润太大，则减少些
            if (gcd>20){
                gcd=gcd;
            }
            writer.write(item+" : "+mapItemToGcd.get(item));
            writer.newLine();
        }
        writer.flush();
        return mapItemToGcd;
    }


}
