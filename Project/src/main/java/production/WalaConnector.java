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

public class WalaConnector {
    //singleton
    private static WalaConnector instance;
    public static WalaConnector getInstance(){
        if(instance==null){
            instance=new WalaConnector();
        }
        return instance;
    };
    private WalaConnector(){}

    public void createMethodDotFile(String target,ClassLoader SomeClass){
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

    public  void createClassDotFile(){
        // 通过读取method.dot文件来生成class.dot
        //map的结构是object指向一个装满调用方(subjective)的hashset
        Map<String, HashSet<String>> hashMap = new HashMap<>();
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
                    throw new Exception(Property.errorList[Property.errorType.Format.ordinal()]);
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
}
