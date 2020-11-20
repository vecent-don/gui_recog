package production;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public class AutoTest {
//    public static void main2(String[] args) {
//        HashSet<String> set=new HashSet<>();
//        ArrayList<HashSet<String>> arrayList=new ArrayList<>();
//        set.add("we");
//        HashSet<String> set2=new HashSet<>();
//        set2.add("are");
//        arrayList.add(set);
//        set.add("young");
//        Iterator iterable=arrayList.get(0).iterator();
//        while (iterable.hasNext()){
//            System.out.println((String) iterable.next());
//
//        }
//        set=set2;
//         iterable=arrayList.get(0).iterator();
//        while (iterable.hasNext()){
//            System.out.println((String) iterable.next());
//
//        }
//
//    }
    static String[] test={
            "\\0-CMD\\","\\1-ALU\\","\\2-DataLog\\","\\3-BinaryHeap\\",
            "\\4-NextDay\\","\\5-MoreTriangle\\"
    };
    static String[] test2={
            "\\0-CMD\\data\\","\\1-ALU\\data\\","\\2-DataLog\\data\\","\\3-BinaryHeap\\data\\",
            "\\4-NextDay\\data\\","\\5-MoreTriangle\\data\\"
    };
    static String[] test3={
            "0\\","1\\","2\\","3\\","4\\","5\\"
    };
    static  String[] prefix={"selection-method.txt","selection-class.txt"};
    public static void main2(String[] args) {

        for(int i=0;i< 5;i+=1) {
            String my = args[0]+test2[i]+prefix[0];
            String ans = args[1]+test[i]+prefix[0];
            HashSet<String> myans = new HashSet<>();
            HashSet<String> trueAns = new HashSet<>();
            try {
                String line;
                BufferedReader bufferedReader = new BufferedReader(new FileReader(my));
                while ((line = bufferedReader.readLine()) != null) {
                    myans.add(line);
                }
                bufferedReader.close();
                bufferedReader = new BufferedReader(new FileReader(ans));
                while ((line = bufferedReader.readLine()) != null) {
                    if (line.equals("")) {
                        int a = 0;
                    }
                    trueAns.add(line);
                }
                bufferedReader.close();

                Iterator iterator = myans.iterator();
                while (iterator.hasNext()) {
                    String tmp = (String) iterator.next();
                    if (trueAns.contains(tmp)) {
                        int a = 1;
                    } else {
                        System.out.println(i+" sth more");
                    }

                }
                iterator = trueAns.iterator();
                while (iterator.hasNext()) {
                    String tmp = (String) iterator.next();
                    if (myans.contains(tmp) || tmp.equals("")) {
                        int a = 1;
                    } else {
                        System.out.println(i+ " sth lost");
                    }

                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

    }
    public static void main(String[] args) {

        for(int i=0;i<= 5;i+=1) {
            String my = args[0]+test3[i]+prefix[0];
            String ans = args[1]+test[i]+prefix[0];
            HashSet<String> myans = new HashSet<>();
            HashSet<String> trueAns = new HashSet<>();
            try {
                String line;
                BufferedReader bufferedReader = new BufferedReader(new FileReader(my));
                while ((line = bufferedReader.readLine()) != null) {
                    myans.add(line);
                }
                bufferedReader.close();
                bufferedReader = new BufferedReader(new FileReader(ans));
                while ((line = bufferedReader.readLine()) != null) {
                    if (line.equals("")) {
                        int a = 0;
                    }
                    trueAns.add(line);
                }
                bufferedReader.close();

                Iterator iterator = myans.iterator();
                while (iterator.hasNext()) {
                    String tmp = (String) iterator.next();
                    if (trueAns.contains(tmp)) {
                        int a = 1;
                    } else {
                        System.out.println(i+" sth more");
                    }

                }
                iterator = trueAns.iterator();
                while (iterator.hasNext()) {
                    String tmp = (String) iterator.next();
                    if (myans.contains(tmp) || tmp.equals("")) {
                        int a = 1;
                    } else {
                        System.out.println(i+ " sth lost");
                    }

                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

    }
}
