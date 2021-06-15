package weka;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
public class CreateArff {
	
	 private static final String USER_DIR = "user.dir";
	public static void convert(String sourcepath,String destpath) throws IOException
	    {
	        // load CSV
	        CSVLoader loader = new CSVLoader();
	        loader.setSource(new File(sourcepath));
	        Instances dataSet = loader.getDataSet();
	        try (// save ARFF
			 BufferedWriter writer = new BufferedWriter(new FileWriter(destpath))) {
				writer.write(dataSet.toString());
				writer.flush();
			} 
	    }
	    public static void main(String[] args) throws Exception
	    {
	    	String proj1= System.getProperty(USER_DIR)+"\\src\\main\\resources\\BOOKKEEPER_CSV.csv";
	    	String proj2= System.getProperty(USER_DIR)+"\\src\\main\\resources\\OPENJPA_CSV.csv";
	    	String proj1final= System.getProperty(USER_DIR)+"\\src\\main\\resources\\BOOKKEEPER.arff";
	    	String proj2finale= System.getProperty(USER_DIR)+"\\src\\main\\resources\\OPENJPA.arff";
            convert(proj1,proj1final);
            convert(proj2,proj2finale);
	    }
}
