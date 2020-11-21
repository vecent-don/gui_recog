package production;

import java.io.*;
import java.util.*;

public class TestSelector {
    //Singleton
    private static TestSelector instance;
    public static TestSelector getInstance(){
        if(instance==null){
            instance=new TestSelector();
        }
        return instance;
    };
    private TestSelector(){}
    public  void createSelectClass(String path){

        try{
            Map<String, HashSet<String>> changeInfo = Util.readChangeInfo(path);
            String  line, callee, caller, subjective, caller_method;
            String[] tmp;
            HashSet<String> classSet = new HashSet<>();
            for (Map.Entry<String, HashSet<String>> entry : changeInfo.entrySet()) {
                classSet.add(entry.getKey());
            }

            // 读取class-cfa.dot
            BufferedReader classReader;
            //寻找方法如下,每一次都检查所有依赖,核查某一个依赖的callee方,是否在受影响的集合内,如果是,将caller方也加入,凡是加入集合内的
            //class均表明受到了影响,以此法完成遍历,不需要构建新图,而最大迭代次数不会超过图的深度(最长路径的长度),每一次至少会向前"感染"一层
            boolean sth_new;
            do{
                sth_new=false;
                classReader = new BufferedReader(new FileReader( "./class-cfa.dot"));
                while ((line = classReader.readLine()) != null){
                    if (line.equals("digraph class {") || line.equals("}")){
                        continue;
                    }
                    line = line.substring(1,line.length()-1);
                    tmp = line.split(" -> ");

                    callee = tmp[0].substring(1,tmp[0].length()-1);
                    caller = tmp[1].substring(1,tmp[1].length()-1);

                    if (classSet.contains(callee)){
                        if (!classSet.contains(caller)){
                            sth_new=true;
                            classSet.add(caller);
                        }
                    }
                }
                classReader.close();
            }while (sth_new);


            // 读取method-cfa.dot
            BufferedReader methodReader = new BufferedReader(new FileReader( "./method-cfa.dot"));
            // 写selection-class.txt
            FileWriter writer = new FileWriter(new File("./selection-class.txt"));
            BufferedWriter bufferedWriter = new BufferedWriter(writer);

            HashSet<String> resultSet = new HashSet<>();
            while ((line = methodReader.readLine()) != null){
                if (line.equals("digraph method {") || line.equals("}")){
                    continue;
                }
                line = line.substring(1,line.length()-1);
                tmp = line.split(" -> ");
                //subjective caller_method 都是调用方
                caller_method = tmp[1].substring(1,tmp[1].length()-1);
                tmp = caller_method.split("\\.");
                subjective = "L" + tmp[0] + "/" + tmp[1] + "/" + tmp[2];

                if (classSet.contains(subjective)){
                    if (caller_method.contains("Test") && !caller_method.contains("<init>()")&& !caller_method.contains("initialize()")){
                        resultSet.add(subjective+" "+caller_method);
                    }
                }
            }

            Iterator iterator = resultSet.iterator();
            String res;
            while (iterator.hasNext()){
                res = (String) iterator.next();
                //tmp 会保存 class 和name
                tmp = res.split(" ");
                bufferedWriter.write(String.format("%s %s\r\n",tmp[0],tmp[1]));
                bufferedWriter.flush();
            }
            bufferedWriter.close();
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
        System.out.println("select-class.txt has been created");
    }

    public  void createSelectMethod(String path){
        String line, callee_method, caller_method, tmpString;
        String[] tmp;
        //第一层的节点不得不单独记录,
        Map<String, HashSet<String>> changeInfo = Util.readChangeInfo(path);
        List<String> firstLevel = new ArrayList<>();   //
        HashSet<String> methodSet = new HashSet<>();
        //通过changeInfo已经拿到了所有的变更新信息,会收录所有的类和方法
        try{
            for (Map.Entry<String, HashSet<String>> entry : changeInfo.entrySet()) {
                Iterator iterator = entry.getValue().iterator();
                while (iterator.hasNext()){
                    tmpString = (String) iterator.next();
                    firstLevel.add(tmpString);
                    methodSet.add(tmpString);
                }
            }

            // 写select-class-test.txt
            FileWriter writer = new FileWriter(new File("./selection-method.txt"));
            BufferedWriter bufferedWriter = new BufferedWriter(writer);
            HashSet<String> tmpSet ;
            String next;
            BufferedReader reader;
            boolean sth_new;
            //寻找方法如下,每一次都检查所有依赖,核查某一个依赖的callee方,是否在受影响的集合内,如果是,将caller方也加入,凡是加入集合内的
            //method均表明受到了影响,以此法完成遍历,不需要构建新图,而最大迭代次数不会超过图的深度(最长路径的长度),每一次至少会向前"感染"一层
            do {
                sth_new=false;
                tmpSet = new HashSet<>();
                // 读取method-CMD-cfa-test.dot

                reader = new BufferedReader(new FileReader( "./method-cfa.dot"));
                while ((line = reader.readLine()) != null) {
                    if (line.equals("digraph method {") || line.equals("}")){
                        continue;
                    }
                    line = line.substring(1,line.length()-1);
                    tmp = line.split(" -> ");

                    callee_method = tmp[0].substring(1,tmp[0].length()-1);
                    caller_method = tmp[1].substring(1,tmp[1].length()-1);

                    if (methodSet.contains(callee_method)){
                        tmpSet.add(caller_method);
                    }
                }
                reader.close();

                Iterator iterator = tmpSet.iterator();
                while (iterator.hasNext()){
                    next = (String) iterator.next();
                    if (methodSet.contains(next)){
                        continue;
                    }
                    methodSet.add(next);
                    sth_new=true;
                }
            }while(sth_new);

            // 最终输出，并且去掉最开始的第一层的方法,不要也可以,下面会剔除
            for (int i=0; i<firstLevel.size(); i++){
                methodSet.remove(firstLevel.get(i));
            }

            Iterator iterator = methodSet.iterator();
            while (iterator.hasNext()){
                String method = (String) iterator.next();
                tmp = method.split("\\.");
                if (tmp[2].contains("Test") && !method.contains("<init>()")&&!method.contains("initialize()")){
                    String className = "L" + tmp[0] + "/" + tmp[1] + "/" + tmp[2];
                    if (className.contains("Test")||className.contains("test")){
                        bufferedWriter.write(String.format("%s %s\r\n",className, method));
                        bufferedWriter.flush();
                    }
                }
            }
            bufferedWriter.close();
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
        System.out.println("select-method.txt has been created");
    }



}
