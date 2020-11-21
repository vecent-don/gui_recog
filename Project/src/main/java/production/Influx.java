package production;


public class Influx {
    private static ClassLoader SomeClass = Influx.class.getClassLoader();
    private static WalaConnector walaConnector=WalaConnector.getInstance();
    private static TestSelector testSelector=TestSelector.getInstance();

    public static void main(String[] args) {
        try {
            String parameter = args[0];
            String target = args[1];
            String changeInfo = args[2];
            //规范路径
            target=Util.normalize(target);
            changeInfo=Util.normalize(changeInfo);
            if (!(parameter.equals("-c") || parameter.equals("-m"))){
                System.out.println(Property.errorList[Property.errorType.Command.ordinal()]);
            }else{
                //分别生成两个dotfile
                walaConnector.createMethodDotFile(target,SomeClass);
                walaConnector.createClassDotFile();
                if (parameter.equals("-c")){
                    testSelector.createSelectClass(changeInfo);
                }else{
                    testSelector.createSelectMethod(changeInfo);

                }
            }
        }catch(Exception e){
            System.out.println(Property.errorList[Property.errorType.Parameter.ordinal()]);
            System.out.println(e.getMessage());
        }
    }


}

