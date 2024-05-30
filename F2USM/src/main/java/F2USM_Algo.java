import com.google.common.base.Joiner;

import java.io.*;
import java.util.*;


public class F2USM_Algo {
    protected double threshold;
    protected String pathname;
    protected String profitFile;
    protected double minUtility;
    protected long huspNum;
    protected long candidateNum;
    protected boolean isDebug;
//    protected Map<String,Double> mapPattern2Util;
    protected Map<Integer,Integer> mapItem2Profit;
    protected boolean isWriteToFile;
    protected String output;
    protected long currentTime;
    protected String dataset;
    Boolean firstPEU;
    protected boolean[][] isRemove;
    boolean DBUpdated;
    protected BufferedWriter writer;
    ULinkList[] uLinkListDB;
    List<String> prefix;
//    FuzzyItemComparator fuzzyItemComparator;
    protected class LastId {
        public double swu;
        public ULinkList uLinkList;

        public LastId(double swu, ULinkList uLinkList) {
            this.swu = swu;
            this.uLinkList = uLinkList;
        }
    }

    public F2USM_Algo(String profitFile) {
        this.profitFile = profitFile;
    }


//    class FuzzyItemComparator implements Comparator<String> {
//        // 实现 compare 方法来定义比较规则
//        public int compare(String o1, String o2) {
//            int dotpos =o1.indexOf(".");
//            int item1=Integer.parseInt(o1.substring(0,dotpos));
//            int region1=Integer.parseInt(o1.substring(dotpos+1));
//            dotpos =o2.indexOf(".");
//            int item2=Integer.parseInt(o2.substring(0,dotpos));
//            int region2=Integer.parseInt(o2.substring(dotpos+1));
//            int compare=Integer.compare(item1,item2);
//            if (compare==0){
//                compare=Integer.compare(region1,region2);
//            }
//            return compare;
//        }
//    }
    //初始化
    public F2USM_Algo(String pathname, String profitFile, double threshold, String output) throws IOException {
        this.pathname = pathname;
        this.profitFile=profitFile;
        this.threshold = threshold;
        this.output = output;
        writer=new BufferedWriter(new FileWriter(output));
        this.dataset=pathname;
        huspNum = 0;
        candidateNum = 0;

        isDebug = false;
        isWriteToFile = true;

    }
    public void runAlgo() throws IOException {

        currentTime = System.currentTimeMillis();

        // reset maximum memory
        MemoryLogger.getInstance().reset();

        loadProfit(profitFile);
        loadDB(pathname);

        MemoryLogger.getInstance().checkMemory();

        firstUSpan();

        MemoryLogger.getInstance().checkMemory();

    }

    /***
     * 加载item的profit到mapItem2Profit
     * @param profitFile
     * @throws IOException
     * @return
     */
     Map<Integer, Integer> loadProfit(String profitFile) throws IOException {
        mapItem2Profit=new HashMap<>();
        BufferedReader myInput = null;
        String thisLine;
        try {
            myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(profitFile))));
            while ((thisLine = myInput.readLine()) != null) {

                String str[] = thisLine.trim().split(" : ");
                Integer item=Integer.parseInt(str[0]);
                Integer profit=Integer.parseInt(str[1]);
                mapItem2Profit.put(item,profit);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (myInput != null) {
                myInput.close();
            }
        }
         return mapItem2Profit;
     }

    /***
     * 存数据到rawDB
     * 计算数据库的效用U(D)
     * 计算单个序列的MFSU到mapItemToMFSU
     * 构建DB的seq-array（uLinkListDB）（去掉不满足条件的模糊单项序列）
     * @param fileName
     * @throws IOException
     */
    void loadDB(String fileName) throws IOException {

        /***
         * 存数据到rawDB
         * 计算数据库的效用U(D)
         * 计算单个序列的MFSU到mapItemToMFSU
         */
        Map<String, Double> mapFuzzyItemToMFSU = new HashMap<>();
        List<List<UItem>> rawDB = new ArrayList<>();
        List<List<UItem>> fDB = new ArrayList<>();
        int totalUtil = 0;
        BufferedReader myInput = null;
        String thisLine;
//        int maxs=0;
//        int mins=Integer.MAX_VALUE;
        try {
            myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(fileName))));
            while ((thisLine = myInput.readLine()) != null) {
                HashSet<String> checkedFuzzyItems = new HashSet<>();
                String str[] = thisLine.trim().split(" -2 ");
                String tokens[]=str[0].split(" ");

                String[] SU_MFU_str = str[1].split(" ");

                String SU_str=SU_MFU_str[0];
                int SU = Integer.parseInt(SU_str.substring(SU_str.indexOf(":") + 1));
                totalUtil += SU;

                String MFU_str=SU_MFU_str[1];
                double MFU = Double.parseDouble(MFU_str.substring(MFU_str.indexOf(":") + 1));

                List<UItem> uItems=new ArrayList<>();
                List<UItem> fItems=new ArrayList<>();
                int leng=0;
                for (int i = 0; i < tokens.length-1; i++) {
                    String currentToken = tokens[i];
                    if (currentToken.charAt(0) != '-') {
                        Integer item = Integer.parseInt(currentToken.trim().substring(0, currentToken.trim().indexOf("[")));
                        Integer qual = Integer.parseInt(currentToken.trim().substring(currentToken.trim().indexOf("[") + 1, currentToken.trim().indexOf("]")));
                        int utility = qual*mapItem2Profit.get(item);
                       //针对某2个数据集采用FUZZUtil.para2
                        FuzzySet[] fuzzySets=FUZZUtil.Fuzz(item, utility,FUZZUtil.para);
                        for (int j = 0; j < fuzzySets.length; j++) {
                            if (Double.compare(fuzzySets[j].fuzzy_val,0.0)>0){
                                double fuzzy_utility=fuzzySets[j].fuzzy_val*utility;
                                uItems.add(new UItem(fuzzySets[j].fuzzy_item,fuzzy_utility));
                                fItems.add(new UItem(fuzzySets[j].fuzzy_item,fuzzySets[j].fuzzy_val));
                                String fuzzyitem_str=fuzzySets[j].fuzzy_item.toString();
                                if (!checkedFuzzyItems.contains(fuzzyitem_str)) {
                                    checkedFuzzyItems.add(fuzzyitem_str);
                                    Double MFSU = mapFuzzyItemToMFSU.get(fuzzyitem_str);
                                    if (MFSU==null){
                                        mapFuzzyItemToMFSU.put(fuzzyitem_str, MFU);
                                    }else {
                                        mapFuzzyItemToMFSU.put(fuzzyitem_str, MFSU + MFU);
                                    }
                                }
                            }
                        }
                        leng++;
                    } else {
                        uItems.add(new UItem(null, -1)) ;
                    }
                }
//                maxs=Math.max(maxs,leng);
//                mins=Math.min(mins,leng);
                rawDB.add(uItems);
                fDB.add(fItems);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (myInput != null) {
                myInput.close();
            }
        }
//        System.out.println("mins="+mins);
//        System.out.println("maxs="+maxs);
        minUtility = totalUtil * threshold;
        MemoryLogger.getInstance().checkMemory();
//        System.out.println("minUtility = "+minUtility);
        /***
         * 构建DB的seq-array（uLinkListDB）（去掉不满足条件的模糊单项序列）
         */
        List<ULinkList> uLinkListDBs = new ArrayList<>();
        int maxItemName = 0;
        int maxSequenceLength = 0;

        Iterator<List<UItem>> listIterator = rawDB.iterator();
        while (listIterator.hasNext()) {
            List<UItem> uItems = listIterator.next();
            List<UItem> newItems = new ArrayList<>();
            //索引计数
            int seqIndex = 0;
            Map<String, ArrayList<Integer>> tempHeader = new TreeMap<>(new Comparator<String>() {
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
            BitSet tempItemSetIndices = new BitSet(uItems.size());
            for (UItem uItem : uItems) {

                FuzzyItem fuzzyItem = uItem.fuzzyItem;
                if (fuzzyItem != null) {
                    //IIP去掉low MFSU的单项
                    String fuzzyitem_str=fuzzyItem.toString();
                    if (Double.compare(mapFuzzyItemToMFSU.get(fuzzyitem_str),minUtility)>=0){
                        newItems.add(uItem);
                        //统计
                        if (fuzzyItem.item > maxItemName) {
                            maxItemName = fuzzyItem.item;
                        }
                        if (tempHeader.containsKey(fuzzyitem_str)) {
                            tempHeader.get(fuzzyitem_str).add(seqIndex);
                        }else {
                            ArrayList<Integer> list = new ArrayList<>();
                            list.add(seqIndex);
                            tempHeader.put(fuzzyitem_str, list);
                        }
                        seqIndex++;
                    }else {
//                        System.out.println(fuzzyitem_str+" has been low rsu prune");
                    }

                }else{
                    if (seqIndex != 0){
                        tempItemSetIndices.set(seqIndex);
                    }
                }
            }

            int size = newItems.size();
            if (size<=0){
                continue;
            }
            //为最后一个位置复位
            int lastIndex = tempItemSetIndices.previousSetBit(tempItemSetIndices.length() - 1);
            if (lastIndex>=size){
                tempItemSetIndices.clear(lastIndex);
            }

            if (size > maxSequenceLength) {
                maxSequenceLength = size;
            }
            ULinkList uLinkList = new ULinkList();
            uLinkList.seq = newItems.toArray(new UItem[size]);


            uLinkList.itemSetIndex = tempItemSetIndices;
            uLinkList.header = tempHeader.keySet().toArray(new String[0]);
            uLinkList.headerIndices = new Integer[tempHeader.size()][];
            for (int i = 0; i < uLinkList.header.length; i++) {
                String FItem_str = uLinkList.header[i];
                ArrayList<Integer> indices = tempHeader.get(FItem_str);
                uLinkList.headerIndices[i] = indices.toArray(new Integer[indices.size()]);
            }

            //设置remainingutility(待核查)
            uLinkList.remainingUtility = new double[size];
            double remainingUtility = 0.0;
            double max_item_mfsu = 0.0;
            int last_item=-1;
            int last_index=-1;
            for (int i = uLinkList.length() - 1; i >= 0; --i) {
                int current_item=uLinkList.ItemName(i);
                if (current_item==last_item&&uLinkList.itemSetIndex.previousSetBit(last_index)==uLinkList.itemSetIndex.previousSetBit(i)){
                    remainingUtility-=max_item_mfsu;
                    uLinkList.setRemainUtility(i, remainingUtility);
                    max_item_mfsu=Math.max(max_item_mfsu,uLinkList.utility(i));
                    remainingUtility+=max_item_mfsu;
                }else {
                    uLinkList.setRemainUtility(i, remainingUtility);
                    max_item_mfsu=Math.max(0.0,uLinkList.utility(i));
                    remainingUtility+=max_item_mfsu;
                }
                last_item=current_item;
                last_index=i;
            }

            uLinkListDBs.add(uLinkList);
        }
        prefix = new ArrayList<>(maxSequenceLength);
        isRemove = new boolean[3][maxItemName + 1];
        uLinkListDB =  uLinkListDBs.toArray(new ULinkList[uLinkListDBs.size()]);
        MemoryLogger.getInstance().checkMemory();
    }


    /**
     * Write to file
     */
//    protected void writeToFile() throws IOException {
////        Collections.sort(patterns);
//        writer=new BufferedWriter(new FileWriter(output));
//        for (String seq:mapPattern2Util.keySet()) {
//            writer.write(seq+" : "+mapPattern2Util.get(seq));
//            writer.newLine();
//        }
//        writer.flush();
//    }

    /**
     * First USpan
     */
     void firstUSpan() throws IOException {

         //计算 1-sequence 的TRSU
         Map<String, Double> mapItem2TRSU = new TreeMap<>(new Comparator<String>() {
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
         for (ULinkList uLinkList : uLinkListDB) {
             for (String fuzzyitem_str : uLinkList.header) {
                 Integer itemIndex_1 = uLinkList.getItemIndices(fuzzyitem_str)[0];
                 double trsu = uLinkList.utility(itemIndex_1) + uLinkList.remainUtility(itemIndex_1);
                 double TRSU = mapItem2TRSU.getOrDefault(fuzzyitem_str, 0.0);
                 mapItem2TRSU.put(fuzzyitem_str, trsu + TRSU);
             }
         }

         for (Map.Entry<String, Double> entry : mapItem2TRSU.entrySet()) {

             if (Double.compare(entry.getValue(),minUtility) >= 0) {

                 candidateNum += 1;
                 String addItem = entry.getKey();
//                 System.out.println("current candidate:" +addItem);
                 /***
                  * 计算 1-sequence 的utility
                  * 计算 1-sequence 的PEU
                  * 计算 1-sequence 的seqPro（seq-array + Extension-list）
                  */
                 double sumUtility = 0.0;
                 double PEU = 0.0;
                 ArrayList<ProjectULinkList> newProjectULinkListDB = new ArrayList<>();

                 for (ULinkList uLinkList : uLinkListDB) {
                     Integer[] itemIndices = uLinkList.getItemIndices(addItem);
                     double utility = 0.0;
                     double peu = 0.0;
                     ArrayList<UPosition> newUPositions = new ArrayList<>();
                     if (itemIndices == null)
                     {
                         continue;
                     }
                     for (int index : itemIndices) {
                         double curUtility = uLinkList.utility(index);
                         utility = Math.max(utility, curUtility);
                         double peu_pos=getUpperBound(uLinkList, index, curUtility);
                         peu = Math.max(peu,peu_pos);
                         newUPositions.add(new UPosition(index, curUtility));
                     }

                     if (newUPositions.size() > 0) {
                         newProjectULinkListDB.add(new ProjectULinkList(uLinkList, newUPositions, utility, peu));
                         sumUtility += utility;
                         PEU += peu;
                     }

                 }

                 if (Double.compare(sumUtility,minUtility)>=0){
                     huspNum += 1;
                     writer.write(entry.getKey()+" : "+sumUtility);
                     writer.newLine();
                 }
                 // PEU >= minUtility
                 if (Double.compare(PEU,minUtility)>=0){
                     prefix.add(addItem);
                     runHUSPspan(newProjectULinkListDB);
                     prefix.remove(prefix.size() - 1);
                 }
             }
         }
         writer.flush();
         writer.close();
         MemoryLogger.getInstance().checkMemory();
    }


    /**
     * Run USpan algorithm
     *
     * @param projectULinkListDB
     */
    protected void runHUSPspan(ArrayList<ProjectULinkList> projectULinkListDB) throws IOException {
        //计算 （n+1）-sequence 的RSU

        Map<String, LastId> mapItemToRSU = new TreeMap<>(new Comparator<String>() {
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
        for (ProjectULinkList projectULinkList : projectULinkListDB) {
            ULinkList uLinkList = projectULinkList.getULinkList();
            ArrayList<UPosition> uPositions = projectULinkList.getUPositions();
            //获取前缀n-sequence的PEU
//            double rsu2 = getPEUofTran(projectULinkList);
            double rsu=projectULinkList.getPEU();
//            if (rsu2!=rsu){
//                System.out.println("new rsu: "+rsu2+"; rsu: "+rsu);
//            }
            UPosition uPosition = uPositions.get(0);
            //从第一个扩展位置开始一直到最后都是可扩展的item
            for (int i = uPosition.index() + 1; i < uLinkList.length(); ++i) {
                String fuzzyitem_str = uLinkList.FuzzyItemName(i);
                FuzzyItem fuzzyItem=uLinkList.FuzzyItem(i);
                if (!isRemove[fuzzyItem.region][fuzzyItem.item]) {//？？？？？
                    LastId lastId = mapItemToRSU.get(fuzzyitem_str);
                    if (lastId == null) {
                        mapItemToRSU.put(fuzzyitem_str, new LastId(rsu, uLinkList));
                    } else {
                        if (lastId.uLinkList != uLinkList) {
                            lastId.swu += rsu;
                            lastId.uLinkList = uLinkList;
                        }
                    }
                }
            }
        }
        // remove the item has low RSU
        //IIP
        for (Map.Entry<String, LastId> entry : mapItemToRSU.entrySet()) {
            String fuzzyitem_str = entry.getKey();
            double SWU = entry.getValue().swu;
            if (Double.compare(SWU,minUtility) < 0) {
                int dotpos =fuzzyitem_str.indexOf(".");
                int item=Integer.parseInt(fuzzyitem_str.substring(0,dotpos));
                int region=Integer.parseInt(fuzzyitem_str.substring(dotpos+1));
                isRemove[region][item] = true;//可见性会不会有问题？？？
                DBUpdated = true;
            }
        }
        //重新计算remaining utility
        //有待查验
        removeItem(projectULinkListDB);

        //计算 （n+1）-sequence 的TRSU
        Map<String, Double> mapIitem2TRSU = getmapIitem2TRSU(projectULinkListDB);
        iConcatenation(projectULinkListDB, mapIitem2TRSU);

        // check the memory usage
        MemoryLogger.getInstance().checkMemory();

        //计算 （n+1）-sequence 的TRSU
        Map<String, Double> mapSitem2TRSU = getMapSitem2TRSU(projectULinkListDB);
        sConcatenation(projectULinkListDB, mapSitem2TRSU);
        MemoryLogger.getInstance().checkMemory();

        for (Map.Entry<String, LastId> entry : mapItemToRSU.entrySet()) {
            String fuzzyitem_str = entry.getKey();
            double swu = entry.getValue().swu;
            int dotpos =fuzzyitem_str.indexOf(".");
            int item=Integer.parseInt(fuzzyitem_str.substring(0,dotpos));
            int region=Integer.parseInt(fuzzyitem_str.substring(dotpos+1));
            if (Double.compare(swu,minUtility) < 0) {
                isRemove[region][item] = false;//恢复原来的设置
                DBUpdated = true;
            }
        }
        //恢复原来的remaining utility
        removeItem(projectULinkListDB);
    }
    /**
     * items appear after prefix in the same itemset in difference sequences;
     * SWU = the sum these sequence utilities for each item as their upper bounds under prefix
     * should not add sequence utility of same sequence more than once
     *
     * @param projectedDB: database
     * @return upper-bound
     */
    protected Map<String, Double> getmapIitem2TRSU(List<ProjectULinkList> projectedDB) {
        Map<String, Double> mapIitem2NUB3 = new TreeMap<>(new Comparator<String>() {
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
//        Map<String, Double> mapIitem2NUB1 = new TreeMap<>(new Comparator<String>() {
//            @Override
//            public int compare(String o1, String o2) {
//                int dotpos =o1.indexOf(".");
//                int item1=Integer.parseInt(o1.substring(0,dotpos));
//                int region1=Integer.parseInt(o1.substring(dotpos+1));
//                dotpos =o2.indexOf(".");
//                int item2=Integer.parseInt(o2.substring(0,dotpos));
//                int region2=Integer.parseInt(o2.substring(dotpos+1));
//                int compare=Integer.compare(item1,item2);
//                if (compare==0){
//                    compare=Integer.compare(region1,region2);
//                }
//                return compare;
//            }
//        });
        Map<String, Double> hasCheckedMapIitem2TRSU=new HashMap<>();
//        Map<String, Double> hasCheckedMapIitem2MidU=new HashMap<>();
        for (ProjectULinkList projectULinkList : projectedDB) {
            ULinkList uLinkList = projectULinkList.getULinkList();
            ArrayList<UPosition> uPositions = projectULinkList.getUPositions();
            //获取前缀n-sequence的PEU
            double rsu = getPEUofTran(projectULinkList);//新的PEU（因为删除了某些low RSU的项）
            for (UPosition uPosition : uPositions) {
                double cur_peu = uPosition.utility() + uLinkList.remainUtility(uPosition.index());
                //下一个项集的开始位置
                int nextItemsetPos = uLinkList.nextItemsetPos(uPosition.index());
                if (nextItemsetPos == -1){
                    nextItemsetPos = uLinkList.length();
                }
                //因为统计RSU用的是每个项最大的模糊值，所以减middleU时，也是减最大的
                //用于记录中间的效用
                double middleU=0.0;
                //每个项的最大模糊效用
                double msfu_item=0.0;
                //上个项
                int last_item = uLinkList.FuzzyItem(uPosition.index()).item;
                //前缀的最后一个项
                int prefix_item = uLinkList.FuzzyItem(uPosition.index()).item;
                for (int index = uPosition.index() + 1; index < nextItemsetPos; ++index) {
                    FuzzyItem fuzzyitem = uLinkList.FuzzyItem(index);
                    int cur_item=fuzzyitem.item;
                    int cur_region= fuzzyitem.region;
                    if (!isRemove[cur_region][cur_item]&&prefix_item!=cur_item) {
                        //如果当前模糊项与上个模糊项属于同一项，则需要减去之前的单个项的最大模糊效用
                        if (cur_item==last_item){
                            middleU-=msfu_item;
                        }
                        //-------------NUB3--------------------
                        Double trsu = hasCheckedMapIitem2TRSU.get(fuzzyitem.toString());
                        if (trsu==null){
                            hasCheckedMapIitem2TRSU.put(fuzzyitem.toString(),cur_peu - middleU);

                        }else {
                            hasCheckedMapIitem2TRSU.put(fuzzyitem.toString(),Math.max(trsu,cur_peu - middleU));
                        }
                        //-------------NUB3--------------------
                        //-------------NUB1--------------------
//                        Double midu=hasCheckedMapIitem2MidU.get(fuzzyitem.toString());
//                        if (midu==null){
//                            hasCheckedMapIitem2MidU.put(fuzzyitem.toString(),middleU);
//                        }else {
//                            hasCheckedMapIitem2MidU.put(fuzzyitem.toString(),Math.min(midu,middleU));
//                        }
                        //-------------NUB1--------------------
                        //用于计算当前模糊项所属项的最大模糊效用
                        double fuzzyitemUtil=uLinkList.utility(index);
                        if (cur_item!=last_item){
                            msfu_item=Math.max(0.0,fuzzyitemUtil);
                            last_item=cur_item;
                        }else {
                            msfu_item=Math.max(msfu_item,fuzzyitemUtil);
                        }
                        middleU+=msfu_item;
                    }
                }
            }
            //-------------NUB3--------------------
            for (String fuzzyitem_str:hasCheckedMapIitem2TRSU.keySet()) {
                double old = mapIitem2NUB3.getOrDefault(fuzzyitem_str,0.0);
                mapIitem2NUB3.put(fuzzyitem_str,old+hasCheckedMapIitem2TRSU.get(fuzzyitem_str));
            }
            hasCheckedMapIitem2TRSU.clear();
            //-------------NUB3--------------------
            //-------------NUB1--------------------
//            for (String fuzzyitem_str:hasCheckedMapIitem2MidU.keySet()) {
//                double old = mapIitem2NUB1.getOrDefault(fuzzyitem_str,0.0);
//                mapIitem2NUB1.put(fuzzyitem_str,old+rsu-hasCheckedMapIitem2MidU.get(fuzzyitem_str));
//            }
//            hasCheckedMapIitem2MidU.clear();
            //-------------NUB1--------------------
            //清空map，记录下一个序列
        }
//        if (!(mapIitem2NUB1.isEmpty()&&mapIitem2NUB3.isEmpty())){
//            System.out.print("");
//        }

//        return mapIitem2NUB1;
        return mapIitem2NUB3;
    }


    /**
     * items appear from the next itemset after prefix in difference sequences;
     * SWU = sum these sequence utilities for each item as their upper bounds under prefix
     * should not add sequence utility of same sequence more than once
     *
     * @param projectedDB: database
     * @return upper-bound
     */
    protected Map<String, Double> getMapSitem2TRSU(List<ProjectULinkList> projectedDB) {
        //---------------NUB3---------------
        Map<String, Double> mapSitem2NUB3 = new TreeMap<>(new Comparator<String>() {
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
        //---------------NUB3---------------
        //---------------NUB1---------------
//        Map<String, Double> mapSitem2NUB1 = new TreeMap<>(new Comparator<String>() {
//            @Override
//            public int compare(String o1, String o2) {
//                int dotpos =o1.indexOf(".");
//                int item1=Integer.parseInt(o1.substring(0,dotpos));
//                int region1=Integer.parseInt(o1.substring(dotpos+1));
//                dotpos =o2.indexOf(".");
//                int item2=Integer.parseInt(o2.substring(0,dotpos));
//                int region2=Integer.parseInt(o2.substring(dotpos+1));
//                int compare=Integer.compare(item1,item2);
//                if (compare==0){
//                    compare=Integer.compare(region1,region2);
//                }
//                return compare;
//            }
//        });
        //---------------NUB1---------------
//        Map<String, Double> hasCheckedMapSitem2MiddU = new HashMap<>();
        Map<String, Double> hasCheckedMapSitem2TRSU = new HashMap<>();

        for (ProjectULinkList projectULinkList : projectedDB) {
            ULinkList uLinkList = projectULinkList.getULinkList();
            double localSwu = getPEUofTran(projectULinkList);
            ArrayList<UPosition> uPositions = projectULinkList.getUPositions();
            //下一个项集的开始位置
            int addItemPos = uLinkList.nextItemsetPos(uPositions.get(0).index());
            //index！=-1是说自身所在就是最后一个项集
            for (int index = addItemPos; index < uLinkList.length() && index != -1; ++index) {
                FuzzyItem fuzzyitem = uLinkList.FuzzyItem(index);
                if (!isRemove[fuzzyitem.region][fuzzyitem.item]) {
                    //---------------NUB3---------------
                    Double trsu=hasCheckedMapSitem2TRSU.get(fuzzyitem.toString());
                    if (trsu==null){
                        double NUB1=get_New_NUB3(projectULinkList, index);
                        hasCheckedMapSitem2TRSU.put(fuzzyitem.toString(), NUB1);
//                        hasCheckedMapSitem2TRSU.put(fuzzyitem.toString(), NUB2);
//                        double middleU = get_middleU(projectULinkList, index);
//                        hasCheckedMapSitem2TRSU.put(fuzzyitem.toString(), localSwu - middleU);
                    }
                    else {
                        double NUB1=get_New_NUB3(projectULinkList, index);
                        hasCheckedMapSitem2TRSU.put(fuzzyitem.toString(), Math.max(trsu,NUB1));
//                        hasCheckedMapSitem2TRSU.put(fuzzyitem.toString(), Math.max(trsu,NUB2));
//                        double middleU = get_middleU(projectULinkList, index);
//                        hasCheckedMapSitem2TRSU.put(fuzzyitem.toString(), localSwu - middleU);

                    }
                    //---------------NUB3---------------
                    //---------------NUB1---------------
//                    Double min_middleU=hasCheckedMapSitem2MiddU.get(fuzzyitem.toString());
//                    if (min_middleU==null){
//                        double middU=get_New_NUB1_MiddU(projectULinkList, index);
//                        hasCheckedMapSitem2MiddU.put(fuzzyitem.toString(), middU);
////
//                    }
//                    else {
//                        double middU=get_New_NUB1_MiddU(projectULinkList, index);
//                        hasCheckedMapSitem2MiddU.put(fuzzyitem.toString(), Math.min(middU,min_middleU));
//                    }
                    //---------------NUB1---------------
                }
            }
            //---------------NUB3---------------
            for (String fuzzyitem_str:hasCheckedMapSitem2TRSU.keySet()) {
                Double old_trsu = mapSitem2NUB3.getOrDefault(fuzzyitem_str,0.0);
                mapSitem2NUB3.put(fuzzyitem_str,old_trsu+hasCheckedMapSitem2TRSU.get(fuzzyitem_str));
//                if (Double.compare(Math.abs(hasCheckedMapSitem2TRSU.get(fuzzyitem_str)-(localSwu-hasCheckedMapSitem2MiddU.get(fuzzyitem_str))),0.0001)>0){
//                    System.out.print("");
//                }
            }
            hasCheckedMapSitem2TRSU.clear();
            //---------------NUB3---------------
            //---------------NUB1---------------
//            for (String fuzzyitem_str:hasCheckedMapSitem2MiddU.keySet()) {
//                Double old_trsu = mapSitem2NUB1.getOrDefault(fuzzyitem_str,0.0);
//                mapSitem2NUB1.put(fuzzyitem_str,old_trsu+localSwu-hasCheckedMapSitem2MiddU.get(fuzzyitem_str));
//            }
//            hasCheckedMapSitem2MiddU.clear();
            //---------------NUB1---------------

        }
//        if (!(mapSitem2NUB1.isEmpty()&&mapSitem2NUB3.isEmpty())){
//            for (String key:mapSitem2NUB1.keySet()){
//                if (Double.compare(Math.abs(mapSitem2NUB1.get(key)-mapSitem2NUB3.get(key)),0.001)>0){
//                    System.out.print("");
//                }
//            }
//        }
//        return mapSitem2NUB1;
        return mapSitem2NUB3;
    }
//    private double get_NUB1(ProjectULinkList projectULinkList, int index) {
//        ArrayList<UPosition> uPositions = projectULinkList.getUPositions();
//        ULinkList uLinkList = projectULinkList.getULinkList();
//        double cur_peu;
//        double NUB1 = 0.0;
//        for (UPosition uPosition : uPositions) {
//            if (uPosition.index() < index) {
//                    if (uLinkList.itemSetIndex.previousSetBit(index)==uLinkList.itemSetIndex.previousSetBit(position.index())&&uLinkList.ItemName(index)==uLinkList.ItemName(position.index())){
//                        break;
//                    }
//                cur_peu = uPosition.utility() + uLinkList.remainUtility(uPosition.index());
//                //??????
//                if (uLinkList.ItemName(index)!=uLinkList.ItemName(index-1)||uLinkList.itemSetIndex.previousSetBit(index-1)!=uLinkList.itemSetIndex.previousSetBit(index)){
//                    NUB1 = Math.max(NUB1,cur_peu-(uLinkList.remainUtility(uPosition.index()) - uLinkList.remainUtility(index - 1)));
//                }else {
//                    NUB1 = Math.max(NUB1,cur_peu-(uLinkList.remainUtility(uPosition.index()) - uLinkList.remainUtility(index - 2)));
//                }
//
//            }else {
//                break;
//            }
//        }
//        return NUB1 >= 0 ? NUB1 : 0;
//    }
    private double get_New_NUB3(ProjectULinkList projectULinkList, int index) {
        ArrayList<UPosition> uPositions = projectULinkList.getUPositions();
        ULinkList uLinkList = projectULinkList.getULinkList();
        double max_peu_before_index=Double.MIN_VALUE;
        double newNUB1 = 0.0;
        double middleU=0.0;
        int pIndex = 0;
        for (UPosition position : uPositions) {
            if (position.index() < index) {
                if (uLinkList.itemSetIndex.previousSetBit(index)==uLinkList.itemSetIndex.previousSetBit(position.index())&&uLinkList.ItemName(index)==uLinkList.ItemName(position.index())){
                    break;
                }
                double cur_peu = position.utility() + uLinkList.remainUtility(position.index());
                pIndex = position.index();
                max_peu_before_index=Math.max(max_peu_before_index,cur_peu);
            } else {
                break;
            }
        }

        if (uLinkList.ItemName(index)!=uLinkList.ItemName(index-1)||uLinkList.itemSetIndex.previousSetBit(index-1)!=uLinkList.itemSetIndex.previousSetBit(index)){
            middleU = uLinkList.remainUtility(pIndex) - uLinkList.remainUtility(index - 1);
        }else{
            middleU = uLinkList.remainUtility(pIndex) - uLinkList.remainUtility(index - 2);
        }
        newNUB1=max_peu_before_index-middleU;
        return newNUB1;
    }
    private double get_NUB3(ProjectULinkList projectULinkList, int index, double localSWU) {
        ArrayList<UPosition> uPositions = projectULinkList.getUPositions();
        ULinkList uLinkList = projectULinkList.getULinkList();
        double min_middleU = Double.MAX_VALUE;
        double max_peu = 0.0;
        for (UPosition uPosition : uPositions) {
            if (uPosition.index() < index) {
                if (uLinkList.itemSetIndex.previousSetBit(index)==uLinkList.itemSetIndex.previousSetBit(uPosition.index())&&uLinkList.ItemName(index)==uLinkList.ItemName(uPosition.index())){
                    break;
                }
                max_peu = Math.max(max_peu,uPosition.utility() + uLinkList.remainUtility(uPosition.index()));
//                min_middleU=Math.min(min_middleU,uLinkList.remainUtility(uPosition.index()) - uLinkList.remainUtility(index)+uLinkList.utility(index));
                if (uLinkList.ItemName(index)!=uLinkList.ItemName(index-1)||uLinkList.itemSetIndex.previousSetBit(index-1)!=uLinkList.itemSetIndex.previousSetBit(index)){
                    min_middleU = Math.min(min_middleU,(uLinkList.remainUtility(uPosition.index()) - uLinkList.remainUtility(index - 1)));

                }else {
                    min_middleU = Math.min(min_middleU,(uLinkList.remainUtility(uPosition.index()) - uLinkList.remainUtility(index - 2)));
                }
                 } else {
                break;
            }
        }

        double NUB2=max_peu-min_middleU;
        return NUB2 < localSWU ? NUB2 : localSWU;
    }
    private double get_NUB1_MiddU(ProjectULinkList projectULinkList, int index) {
        ArrayList<UPosition> uPositions = projectULinkList.getUPositions();
        ULinkList uLinkList = projectULinkList.getULinkList();
        double min_middleU = Double.MAX_VALUE;
        for (UPosition uPosition : uPositions) {
            if (uPosition.index() < index) {
                if (uLinkList.itemSetIndex.previousSetBit(index)==uLinkList.itemSetIndex.previousSetBit(uPosition.index())&&uLinkList.ItemName(index)==uLinkList.ItemName(uPosition.index())){
                    break;
                }
                if (uLinkList.ItemName(index)!=uLinkList.ItemName(index-1)||uLinkList.itemSetIndex.previousSetBit(index-1)!=uLinkList.itemSetIndex.previousSetBit(index)){
                    min_middleU = Math.min(min_middleU,(uLinkList.remainUtility(uPosition.index()) - uLinkList.remainUtility(index - 1)));

                }else {
                    min_middleU = Math.min(min_middleU,(uLinkList.remainUtility(uPosition.index()) - uLinkList.remainUtility(index - 2)));
                }
            } else {
                break;
            }
        }

        return min_middleU;
    }
    private double get_New_NUB1_MiddU(ProjectULinkList projectULinkList, int index) {
        ArrayList<UPosition> uPositions = projectULinkList.getUPositions();
        ULinkList uLinkList = projectULinkList.getULinkList();
        double middleU=0.0;
        int pIndex = 0;
        for (UPosition position : uPositions) {
            if (position.index() < index) {
                if (uLinkList.itemSetIndex.previousSetBit(index)==uLinkList.itemSetIndex.previousSetBit(position.index())&&uLinkList.ItemName(index)==uLinkList.ItemName(position.index())){
                    break;
                }
                pIndex = position.index();
            } else {
                break;
            }
        }

        if (uLinkList.ItemName(index)!=uLinkList.ItemName(index-1)||uLinkList.itemSetIndex.previousSetBit(index-1)!=uLinkList.itemSetIndex.previousSetBit(index)){
            middleU = uLinkList.remainUtility(pIndex) - uLinkList.remainUtility(index - 1);
        }else{
            middleU = uLinkList.remainUtility(pIndex) - uLinkList.remainUtility(index - 2);
        }
        return middleU;
    }
    /**
     * @param projectedDB:              database
     * @param mapIitem2TRSU: upper-bound of addItem
     */
    protected void iConcatenation(ArrayList<ProjectULinkList> projectedDB, Map<String, Double> mapIitem2TRSU) throws IOException {
        for (Map.Entry<String, Double> entry : mapIitem2TRSU.entrySet()) {

            if (Double.compare(entry.getValue(),minUtility) >= 0) {
                /***
                 * 计算 (n+1)-sequence 的utility
                 * 计算 (n+1)-sequence 的PEU
                 * 计算 (n+1)-sequence 的seqPro（seq-array + Extension-list）
                 */
                candidateNum += 1;
                String addItem = entry.getKey();
//                System.out.println("current candidaite:"+Joiner.on(" ").join(prefix)+" "+entry.getKey());
                double sumUtility = 0;
                double PEU = 0;
                ArrayList<ProjectULinkList> newProjectULinkListDB = new ArrayList<>();

                for (ProjectULinkList projectULinkList : projectedDB) {
                    ULinkList uLinkList = projectULinkList.getULinkList();
                    ArrayList<UPosition> uPositions = projectULinkList.getUPositions();
                    Integer[] itemIndices = uLinkList.getItemIndices(addItem);

                    double utility = 0;
                    double peu = 0;
                    ArrayList<UPosition> newUPositions = new ArrayList<>();
                    int addItemInd;
                    if (itemIndices == null){
                        continue;
                    }
                    for (int i = 0, j = 0; i < uPositions.size() && j < itemIndices.length; ) {
                        addItemInd = itemIndices[j];
                        UPosition uPosition = uPositions.get(i);
                        //扩展位置属于哪个项集
                        int uPositionItemsetIndex = uLinkList.whichItemset(uPosition.index());
                        //扩展的项属于哪个项集
                        int addItemItemsetIndex = uLinkList.whichItemset(addItemInd);

                        if (uPositionItemsetIndex == addItemItemsetIndex) {
                            double curUtility = uLinkList.utility(addItemInd) + uPosition.utility();
                            utility = Math.max(utility, curUtility);
                            double peu_pos=getUpperBound(uLinkList, addItemInd, curUtility);
                            peu = Math.max(peu, peu_pos);
                            newUPositions.add(new UPosition(addItemInd, curUtility));
                            i++;
                            j++;
                        } else if (uPositionItemsetIndex > addItemItemsetIndex) {
                            j++;
                        } else if (uPositionItemsetIndex < addItemItemsetIndex) {
                            i++;
                        }
                    }

                    if (newUPositions.size() > 0) {
                        newProjectULinkListDB.add(new ProjectULinkList(uLinkList, newUPositions, utility,peu));
                        sumUtility += utility;
                        PEU += peu;
                    }

                }
                if (Double.compare(sumUtility,minUtility) >= 0) {
                    huspNum++;
                    writer.write(Joiner.on(" ").join(prefix)+" "+entry.getKey()+" : "+sumUtility);
                    writer.newLine();
                }
                if (Double.compare(PEU,minUtility) >= 0) {
                    prefix.add(addItem);
                    runHUSPspan(newProjectULinkListDB);
                    prefix.remove(prefix.size() - 1);
                }
            }
        }
    }

    /**
     * S-concatenation
     * <p>
     * each addItem (candidate item) has multiple index in the sequence
     * each index can be s-concatenation with multiple UPositions before this index
     * but these UPositions s-concatenation with the same index are regarded as one sequence
     * so for each index, choose the UPosition with maximal utility
     * <p>
     * candidate sequences are evaluated by (prefix utility + remaining utility) (PU)
     *
     * @param projectedDB:              database
     * @param mapSitem2TRSU: upper-bound of addItem
     */
    protected void sConcatenation(ArrayList<ProjectULinkList> projectedDB, Map<String, Double> mapSitem2TRSU) throws IOException {
        for (Map.Entry<String, Double> entry : mapSitem2TRSU.entrySet()) {
            if (Double.compare(entry.getValue(),minUtility) >= 0) {
                /***
                 * 计算 (n+1)-sequence 的utility
                 * 计算 (n+1)-sequence 的PEU
                 * 计算 (n+1)-sequence 的seqPro（seq-array + Extension-list）
                 */
                candidateNum += 1;
                String addItem = entry.getKey();
//                System.out.println("current candidaite:"+Joiner.on(" ").join(prefix)+" "+entry.getKey());
                double sumUtility = 0.0;
                double PEU = 0.0;
                ArrayList<ProjectULinkList> newProjectULinkListDB = new ArrayList<ProjectULinkList>();

                for (ProjectULinkList projectULinkList : projectedDB) {

                    ULinkList uLinkList = projectULinkList.getULinkList();
                    ArrayList<UPosition> uPositions = projectULinkList.getUPositions();
                    Integer[] itemIndices = uLinkList.getItemIndices(addItem);

                    if (itemIndices == null) {
                        continue;
                    } // addItem should be in the transaction

                    double utility = 0;
                    double peu = 0;
                    ArrayList<UPosition> newUPositions = new ArrayList<UPosition>();

                    /*
                     * each addItem has multiple index (will become new UPosition) in the
                     * sequence, each index (will become new UPosition) can be s-concatenation
                     * with multiple UPositions (contain position of last item in prefix)
                     * before this index, but multiple UPositions s-concatenation with the same
                     * index are regarded as one new UPosition, so for each index, choose the
                     * maximal utility of UPositions before this index as prefix utility for
                     * this index.
                     */
                    double maxPositionUtility = 0;  // choose the maximal utility of UPositions
                    int uPositionNextItemsetPos = -1;

                    int addItemInd;
                    //i表示前缀扩展位置的索引，j表示扩展项所在位置的索引
                    for (int i = 0, j = 0; j < itemIndices.length; j++) {
                        //获取第j个扩展位置
                        addItemInd = itemIndices[j];
                        //从第i个前缀扩展位置开始
                        for (; i < uPositions.size(); i++) {
                            //获得当前前缀扩展位置的下一个项集的起始位置
                            uPositionNextItemsetPos = uLinkList.nextItemsetPos(uPositions.get(i).index());

                            // 1. next itemset should be in transaction
                            // 2. addItem should be after or equal to the next itemset of UPosition
                            //后缀位置确定，找前缀的最大效用
                            //需要满足下一个项集的起始位置<=扩展项所在位置
                            if (uPositionNextItemsetPos != -1 && uPositionNextItemsetPos <= addItemInd) {
                                maxPositionUtility=Math.max(uPositions.get(i).utility(),maxPositionUtility);
                            } else {
                                break;
                            }
                        }

                        // maxPositionUtility is initialized outside the loop,
                        // will be the same or larger than before
                        if (maxPositionUtility != 0) {
                            double curUtility = uLinkList.utility(addItemInd) + maxPositionUtility;
                            utility = Math.max(utility, curUtility);
                            double peu_pos=getUpperBound(uLinkList, addItemInd, curUtility);
                            peu = Math.max(peu,peu_pos);
                            newUPositions.add(new UPosition(addItemInd, curUtility));

                        }
                    }

                    // if exist new positions, update the sumUtility and upper-bound
                    if (newUPositions.size() > 0) {
                        newProjectULinkListDB.add(new ProjectULinkList(uLinkList, newUPositions, utility,peu));
                        sumUtility += utility;
                        PEU += peu;
                    }
                }
                if (Double.compare(sumUtility,minUtility) >= 0) {
                    huspNum++;
//                    patterns.add(Joiner.on(" ").join(prefix)+" -1 "+entry.getKey());
                    writer.write(Joiner.on(" ").join(prefix)+" -1 "+entry.getKey()+" : "+sumUtility);
                    writer.newLine();
                }
                if (Double.compare(PEU,minUtility) >= 0) {
                    prefix.add("-1");
                    prefix.add(addItem);
                    runHUSPspan(newProjectULinkListDB);
                    prefix.remove(prefix.size() - 1);
                    prefix.remove(prefix.size() - 1);
                }
            }
        }
    }

    /**
     *
     * Example for check of S-Concatenation
     * <[(3:25)], [(1:32) (2:18) (4:10) (5:8)], [(2:12) (3:40) (5:1)]> 146
     * Pattern: 3 -1 2
     * UPositions: (3:25), (3:40)
     * For
     * addItemInd = firstPosOfItemByName = (2:18)
     *   UPosition = (3:25)
     *   uPositionNextItemsetPos = [(1:32) (2:18) (4:10) (5:8)]
     *   maxPositionUtility = 25
     *   UPosition = (3:40)
     *   uPositionNextItemsetPos = -1 -> break
     * newUPosition = 25 + 18
     * addItemInd = (2:12)
     *   UPosition = (3:40)
     *   uPositionNextItemsetPos = -1 -> break
     * newUPosition = 25 + 12
     * End
     */

    /**
     * PEU
     *
     * @param uLinkList
     * @param index
     * @param curUtility
     * @return
     */
    protected double getUpperBound(ULinkList uLinkList, int index, double curUtility) {
        return curUtility + uLinkList.remainUtility(index);
    }

    /**
     * PEU
     *
     * @param projectULinkList
     * @return
     */
    protected double getPEUofTran(ProjectULinkList projectULinkList) {
        ULinkList uLinkList = projectULinkList.getULinkList();
        ArrayList<UPosition> uPositions = projectULinkList.getUPositions();
        double peu = 0;
        for (UPosition uPosition : uPositions) {
            peu = Math.max(peu,uPosition.utility() + uLinkList.remainUtility(uPosition.index()));
        }
        return peu;
    }

    /**
     * Funtion of removeItem, using the position of remaining utility
     * used for mapItemSwu(swu = position.utility + position.remaining utility)
     * 只改remaining utility？？？？？
     * @param
     */
    protected void removeItem(ArrayList<ProjectULinkList> projectULinkListDB) {
        if(!DBUpdated)
        {
            return;
        }
//        int ind=1;
        for (ProjectULinkList projectULinkList : projectULinkListDB) {
//            System.out.print(ind+":");

            ULinkList uLinkList = projectULinkList.getULinkList();
            ArrayList<UPosition> uPositions = projectULinkList.getUPositions();
            int positionIndex = uPositions.get(0).index();
            double remainingUtility = 0.0;
            double max_item_mfsu = 0.0;
            int last_item=-1;
            int last_index=-1;
            for (int i = uLinkList.length() - 1; i >= positionIndex; --i) {
                FuzzyItem fuzzyItem=uLinkList.FuzzyItem(i);
                if (!isRemove[fuzzyItem.region][fuzzyItem.item]){
//                    System.out.print(fuzzyItem.toString()+" ");
                    int current_item=uLinkList.ItemName(i);
                    //?????
                    if (current_item==last_item&&uLinkList.itemSetIndex.previousSetBit(last_index)==uLinkList.itemSetIndex.previousSetBit(i)){
                        remainingUtility-=max_item_mfsu;
                        uLinkList.setRemainUtility(i, remainingUtility);
                        max_item_mfsu=Math.max(max_item_mfsu,uLinkList.utility(i));
                        remainingUtility+=max_item_mfsu;
                    }else {
                        uLinkList.setRemainUtility(i, remainingUtility);
                        max_item_mfsu=Math.max(0.0,uLinkList.utility(i));
                        remainingUtility+=max_item_mfsu;
                    }
                    last_item=current_item;
                    last_index=i;
                }else {
                    uLinkList.setRemainUtility(i, remainingUtility);
                }
            }
//            ind++;
//            System.out.print("");
        }
        DBUpdated = false;
    }

    /**
     * Print statistics about the algorithm execution
     */
    public void printStatistics()  {
        System.out.println("=============  HUS_SPull ALGORITHM - STATS ============");
        System.out.println("dataset: "+dataset);
        System.out.println("minUtilRatio: " + String.format("%.5f", threshold));
        System.out.println("minUtil: " + minUtility);
        System.out.println("time: " + (System.currentTimeMillis() - currentTime)/1000.0 + " s");
        System.out.println("Max memory: " + MemoryLogger.getInstance().getMaxMemory() + "  MB");
        System.out.println("HUSPs: " + huspNum);
        System.out.println("Candidates: " + candidateNum);
    }


}
