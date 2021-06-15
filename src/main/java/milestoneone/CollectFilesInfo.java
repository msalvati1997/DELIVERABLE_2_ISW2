package milestoneone;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JsonOrgJsonProvider;

public class CollectFilesInfo {
	
	private static final String PROJNAME1 = "OPENJPA";
	private static final String PROJAME2="BOOKKEEPER";
  
	
	private static final String RELEASE = "Release";
	private static final String PERSONIDENT = "PersonIdent";
	private static final String FILENAME = "Filename";
	private static final String DELETELINES = "DeleteLines=";
	private static final String CHANGESLINE = "ChangesLine";
	private static final String JSON = ".json";
	private static final String ADDEDLINES = "AddedLines=";
	
	public static void collectFileInfo(String projName) throws JSONException, IOException, ParseException {
	      
		  JSONObject commitlog = parseJSONFile("CommitLog"+projName+JSON);	      
	      String query = "$.CommitsLog.[*].['CommitTime','NumberOfFilesTouched','DiffFiles','PersonIdent']";
		  JsonPath jsonPath = JsonPath.compile(query);
		  Configuration configuration = Configuration.builder()
		              .jsonProvider(new JsonOrgJsonProvider())
		              .build();
		  Object jsonResult = JsonPath.using(configuration)
	                  .parse(commitlog)
	                  .read(jsonPath);
		  JSONObject result = new JSONObject();
		  result.put("File", jsonResult);
		  JSONArray arr = result.getJSONArray("File");
		  JSONArray aggregate = new JSONArray();
		  ArrayList <String> filenames = new ArrayList<>();
		  for(int i=0; i<arr.length();i++) {
			  JSONObject json = arr.getJSONObject(i);
			  JSONArray files = null;
			  Integer numberoffiles = json.getInt("NumberOfFilesTouched");
			  if(numberoffiles==0) {
				  continue;
			  }
			try {
				files = json.getJSONArray("DiffFiles");
			} catch (Exception e) {
				java.util.logging.Logger.getLogger("CollectFilesInfo").log(Level.WARNING, "Exception", e);

			}
			  Object committime = json.get("CommitTime");
			  String auth = json.getString(PERSONIDENT);
			  DateAndRelease dr = new DateAndRelease(committime.toString(),"", projName);
			  Integer release=dr.fromDateToIndex();
			  aggregate=getJSONArrayinfo(aggregate, filenames, files, auth, release); 
			  }
		  JSONObject prova = new JSONObject();
		  List<String> nfilenames = removeDuplicates(filenames);
		  prova.put("File", aggregate);
		  try (FileWriter file = new FileWriter("file.json")) {
				file.write(prova.toString(1));
	      }
		  JSONObject njs = new JSONObject();
		  for (int p=0;p<nfilenames.size();p++) {
			  String filename = nfilenames.get(p);
			  String query2 =("$.File[?(@.Filename=='"+filename+"')]");
			  JsonPath jsonPath2 = JsonPath.compile(query2);
			  Configuration configuration2 = Configuration.builder()
			              .jsonProvider(new JsonOrgJsonProvider())
			              .build();
			  Object jsonResult2 = JsonPath.using(configuration2)
		                  .parse(prova)
		                  .read(jsonPath2);
			  njs.put(filename, jsonResult2);
		  }
		  try (FileWriter file = new FileWriter("FileHistory"+projName+JSON)) {
				file.write(njs.toString(1));
	      }
	}


	private static JSONArray getJSONArrayinfo(JSONArray aggregate, ArrayList<String> filenames, JSONArray files, String auth,
			Integer release) {
		for(int p=0;p<files.length();p++) {
			  String classname = files.get(p).toString();
			  String added = classname.split("added=")[1].split(" ")[0];
			  int addedl = Integer.parseInt(added);
			  String delete = classname.split("delete=")[1];
			  int delete1 = Integer.parseInt(delete);
			  int changes = addedl+delete1;
			  classname = classname.split("added=")[0];
			  String[] splitted = classname.split(":");
			  String[] names = splitted[2].split("->");
		      if(names.length==1) {
				  JSONObject js=getjson(auth, release, addedl, delete1, changes, names, 0);
				  filenames.add(names[0].trim());
				  js.put("Opt", classname.split(":")[1].trim()); 
				  aggregate.put(js);
			}
			  if(names.length==2) {
				  if (classname.split(":")[1].endsWith("E")) {
				 
				  JSONObject js=getjson(auth, release, addedl, delete1, changes, names, 0);
				  filenames.add(names[0].trim());
				  js.put("Opt", classname.split(":")[1].trim()); 
				  aggregate.put(js);
				  }
				  if (classname.split(":")[1].endsWith("D")) {
					  JSONObject js=getjson(auth, release, addedl, delete1, changes, names,1);

					  filenames.add(names[1].trim());
					  js.put("Opt", classname.split(":")[1].trim()); 
					  aggregate.put(js);
				  }
			   }
  }
		return aggregate;
	}


	private static JSONObject getjson(String auth, Integer release, int addedl, int delete1, int changes, String[] names,
			 int i) {
		  JSONObject js = new JSONObject(); 
		  js.put(RELEASE, release);
		  js.put(PERSONIDENT,auth);
		  js.put(FILENAME,names[i].trim());
		  js.put(ADDEDLINES,addedl);
		  js.put(DELETELINES,delete1);
		  js.put(CHANGESLINE, changes);
		  return js;
	}
	
	
	public static JSONObject parseJSONFile(String filename) throws JSONException, IOException {
        String content = new String(Files.readAllBytes(Paths.get(filename)));
        return new JSONObject(content);
    }
	  // Function to remove duplicates from an ArrayList
    public static <T> List<T> removeDuplicates(List<T> list) {
        // Create a new ArrayList
        ArrayList<T> newList = new ArrayList<>();
        // Traverse through the first list
        for (T element : list) {
            // If this element is not present in newList
            // then add it
            if (!newList.contains(element)) {
                newList.add(element);
            }
        }
        // return the new list
        return newList;
    }
	public static void main(String[] args) throws IOException, JSONException, ParseException { 
		collectFileInfo(PROJNAME1);
		collectFileInfo(PROJAME2);
	}
}

