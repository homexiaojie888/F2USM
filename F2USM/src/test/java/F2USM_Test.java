import java.io.IOException;

/***
 * input 文件中的项必须是增长序排列的，否则会出问题
 */
public class F2USM_Test {

    public static void main(String[] args) throws IOException {

        double minUtilityRatio = 0.3;
//        BIBLE  BMS  Kosarak10k   FIFA  SIGN  Leviathan
        String input = "src/main/resources/q-sequence.txt";
        String profitFile = "src/main/resources/profit.txt";
        String output = "fusminer.txt";

//        for (int i = 0; i < 6; i++) {
            // 初始化
            F2USM_Algo huspMiner = new F2USM_Algo(input, profitFile,minUtilityRatio, output);
            //挖
            huspMiner.runAlgo();

            huspMiner.printStatistics();
//            minUtilityRatio+=0.001;

//        }


    }

}
