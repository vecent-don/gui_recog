package production;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class Util {
    //获取路径下所有的class文件
    public static List<String> getAllFile(String directoryPath, boolean isAddDirectory) {
        List<String> list = new ArrayList<String>();
        File baseFile = new File(directoryPath);
        if (baseFile.isFile() || !baseFile.exists()) {
            return list;
        }
        File[] files = baseFile.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                if(isAddDirectory){
                    list.add(file.getAbsolutePath());
                }
                list.addAll(getAllFile(file.getAbsolutePath(),isAddDirectory));
            }
            else {
                String name = file.getName();
                String extension = name.substring(name.lastIndexOf("."));
                if(extension.equals(".class")){
                    list.add(file.getAbsolutePath());
                }
            }
        }
        return list;
    }
    //规则化路径
    public static String normalize(String path){
        if(path.length()>0){
            if (path.charAt(path.length()-1)=='\\'){
                path = path.substring(0,path.length()-1);
            }
        }
        else {
            System.out.println("path not invalid");
        }
        return path;
    }
    //读取变更信息
    public static Map<String, HashSet<String>> readChangeInfo(String changeInfo) {
        //the string means the callee,while the set contains its callers
        Map<String, HashSet<String>> hashMap = new HashMap<String, HashSet<String>>();
        HashSet<String> set;
        String line;
        String[] tmp;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(changeInfo));
            while ((line = reader.readLine()) != null) {
                tmp = line.split(" ");
                if (tmp.length != 2) {
                    System.out.println("change_info.txt format is wrong");
                    break;
                }
                //索引0存放类，索引1存放方法
                if (hashMap.get(tmp[0]) != null) {
                    // key存在,直接获取该引用
                    set = hashMap.get(tmp[0]);
                    set.add(tmp[1]);
                    hashMap.put(tmp[0], set);
                } else {
                    //key不存在,所有需要为这个key创建一个新的hashset
                    set = new HashSet<>();
                    set.add(tmp[1]);
                    hashMap.put(tmp[0], set);

                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return hashMap;
    }
    //进一步筛选变更类
    public static boolean ifTargetClass(String prefix){
        try{
            if (prefix.length()>="net.mooctest".length()&&prefix.substring(0,12).equals("net.mooctest")){
                return true;
            }
        }catch(Exception e){
            System.out.println("sth mysterious happened");
        }
        return false;
    }
}
