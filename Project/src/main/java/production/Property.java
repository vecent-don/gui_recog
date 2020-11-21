package production;

public class Property {
    public static String EXCLUSION_FILE = "exclusion.txt";
    public static String TestClassPath="\\test-classes\\net\\mooctest";
    public static String ProductionClassPath= "\\test-classes\\net\\mooctest";
    public static String[] errorList={
            "please examine your command","please checkout method.dot, format is error","Parameters are illegal"
    };


    enum errorType
    {
        Command,Format,Parameter
    };
}
