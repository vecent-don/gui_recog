package production;

import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraphStats;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.AnalysisScopeReader;

import java.io.*;
import java.util.*;



public class Influx {
    private static ClassLoader SomeClass = Influx.class.getClassLoader();
    private static String[] errorList={
      "please examine your command","please checkout method.dot, format is error","Parameters are illegal"
    };


    enum errorType
    {
        Command,Format,Parameter
    };


    public static void main(String[] args) {
        try {
            String parameter = args[0];
            String target = args[1];
            String changeInfo = args[2];
            //规范路径
            target=Util.normalize(target);
            changeInfo=Util.normalize(changeInfo);
            if (!(parameter.equals("-c") || parameter.equals("-m"))){
                System.out.println(errorList[errorType.Command.ordinal()]);
            }else{
                //分别生成两个dotfile
                createMethodDotFile(target);
                createClassDotFile();
                if (parameter.equals("-c")){
                    createSelectClass(changeInfo);
                }else{
                    createSelectMethod(changeInfo);

                }
            }
        }catch(Exception e){
            System.out.println(errorList[errorType.Parameter.ordinal()]);
            System.out.println(e.getMessage());
        }
    }

    public static void createMethodDotFile(String target){
        try {
            AnalysisScope scope = AnalysisScopeReader.readJavaScope("scope.txt", new File(Property.EXCLUSION_FILE), SomeClass);
            List<String> paths = Util.getAllFile(target, false);
            for (String path : paths) {
                System.out.println(path);
                scope.addClassFileToScope(ClassLoaderReference.Application, new File(path));
            }

            // 1.生成类层次关系对象
            ClassHierarchy cha = ClassHierarchyFactory.makeWithRoot(scope);
            // 2.生成进入点
            Iterable<Entrypoint> eps = new AllApplicationEntrypoints(scope, cha);

            // 3.利用CHA算法构建调用图
            CHACallGraph cg = new CHACallGraph(cha);

            //4.进入图
            cg.init(eps);
            String stats = CallGraphStats.getStats(cg);
            System.out.println(stats);
            System.out.println("CHACallGraph is Completed");

            File file = new File( "./method-cfa.dot");
            file.createNewFile();
            FileWriter writer = new FileWriter(file);
            BufferedWriter bufferedWriter = new BufferedWriter(writer);
            bufferedWriter.write("digraph method {\r\n");
            // 4.遍历cg中所有的节点
            for (CGNode node : cg) {
                // node中包含了很多信息，包括类加载器、方法信息等，这里只筛选出需要的信息
                //tmp是箭头后,是调用者,一个node可能被多个调用,所以不得不把其放在key的位置.而让调用者放在value的位置
                if (node.getMethod() instanceof ShrikeBTMethod) {
                    if ("Application".equals(node.getMethod().getDeclaringClass().getClassLoader().toString())) {
                        Iterator<CGNode> iterator = cg.getPredNodes(node);
                        while (iterator.hasNext()) {
                            CGNode tmp = iterator.next();
                            if (tmp.getMethod() instanceof ShrikeBTMethod) {
                                if ("Application".equals(tmp.getMethod().getDeclaringClass().getClassLoader().toString())) {
                                    if (Util.ifTargetClass(tmp.getMethod().getSignature()) && Util.ifTargetClass(node.getMethod().getSignature())) {
                                        bufferedWriter.write(String.format("\t\"%s\" -> \"%s\";\r\n", node.getMethod().getSignature(), tmp.getMethod().getSignature()));
                                    }
                                }
                            }
                        }
                        bufferedWriter.flush();
                    }
                } else {
                }
            }
            bufferedWriter.write("}\r\n");
            bufferedWriter.flush();
            bufferedWriter.close();
            System.out.println("method-cfa.dot has been created");
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
        return;
    }

    public static void createClassDotFile(){
        // 通过读取method.dot文件来生成class.dot
        //map的结构是object指向一个装满调用方(subjective)的hashset
        Map<String,HashSet<String>> hashMap = new HashMap<>();
        String line, objective, subjective;
        HashSet<String> set ;

        try {
            BufferedReader reader = new BufferedReader(new FileReader( "./method-cfa.dot"));
            while ((line = reader.readLine()) != null) {
                if (line.equals("digraph method {") || line.equals("}")){
                    continue;
                }
                //去除首部\t,尾部;用tmp存一下中间结果而已
                String[] tmp;
                line = line.substring(1,line.length()-1);
                tmp = line.split(" -> ");
                if (tmp.length != 2){
                    throw new Exception(errorList[errorType.Format.ordinal()]);
                }
                //objective是被调用者,这里在箭头前,方便处理
                objective = tmp[0].substring(1,tmp[0].length()-1);
                subjective = tmp[1].substring(1,tmp[1].length()-1);
                tmp = objective.split("\\.");  //  0:net ;1:mooctest; 2:类名
                objective = "L" + tmp[0] + "/" + tmp[1] + "/" + tmp[2];
                tmp = subjective.split("\\.");
                subjective = "L" + tmp[0] + "/" + tmp[1] + "/" + tmp[2];
                //判断一下这个key有没有在内存中
                if (hashMap.get(objective) != null){
                    //!notice: java中所有的类在传递的时候,都维护的是引用,所以这里让set指向了新的空间,不会影响之前的值
                    set = hashMap.get(objective);
                    set.add(subjective);
                    hashMap.put(objective,set);

                }else{
                    set = new HashSet<>();
                    set.add(subjective);
                    hashMap.put(objective,set);
                }
            }
            reader.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        try{

            FileWriter writer = new FileWriter(new File( "./class-cfa.dot"));
            BufferedWriter bufferedWriter = new BufferedWriter(writer);
            bufferedWriter.write("digraph class {\r\n");
            Iterator iterator;
            for (Map.Entry<String, HashSet<String>> entry : hashMap.entrySet()) {
                String calleeClass = entry.getKey();
                iterator = entry.getValue().iterator();
                while (iterator.hasNext()){
                    String callerClass = (String) iterator.next();
                    bufferedWriter.write(String.format("\t\"%s\" -> \"%s\";\r\n",calleeClass, callerClass));
                    bufferedWriter.flush();
                }
            }
            bufferedWriter.write("}\r\n");
            bufferedWriter.flush();
            bufferedWriter.close();
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
        System.out.println("class-cfa.dot has been created");
    }

    public static void createSelectClass(String path){

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

    public static void createSelectMethod(String path){
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

