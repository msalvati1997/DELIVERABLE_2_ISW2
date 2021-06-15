package milestoneone;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GetBugFixed  {
	private static final String JSON = ".json";
	private static final String CREATED = "created";
	private static final String RESOLUTIONDATE = "resolutiondate";
	private static final String PROJAME1="OPENJPA";
	private static final String PROJNAME2="BOOKKEEPER";
	
	private static final Logger LOGGER = Logger.getLogger( GetBugFixed.class.getName() );
	public static JSONArray readJsonArrayFromUrl(String url) throws IOException, JSONException {
		      InputStream is = new URL(url).openStream();
		         try (BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
					String jsonText = readAll(rd);
					 return new JSONArray(jsonText);
				}
		   }
	public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
	      InputStream is = new URL(url).openStream();
	         try (BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
				String jsonText = readAll(rd);
				 return new JSONObject(jsonText);
			}
	}	   
	private static String readAll(Reader rd) throws IOException {
		      StringBuilder sb = new StringBuilder();
		      int cp;
		      while ((cp = rd.read()) != -1) {
		         sb.append((char) cp);
		      }
		      return sb.toString();
		   }
	public static JSONObject parseJSONFile(String filename) throws JSONException, IOException {
	        String content = new String(Files.readAllBytes(Paths.get(filename)));
	        return new JSONObject(content);
	    }
    public static List<Object> checkLinkage(String projname) throws JSONException, IOException {
	  String filename = "CommitLog"+projname+JSON;
	  float nolinked=0;
	  float linked=0;
	  ArrayList <String> tickets = new ArrayList<>();
      JSONObject jsonObject = parseJSONFile(filename);
      JSONArray  arr = jsonObject.getJSONArray("CommitsLog");
      for (int i=0;i<arr.length();i++) {
    	    JSONObject commit = arr.getJSONObject(i);
    	    if (commit.get("Linked").toString().equals("No")) {
    	    	nolinked=nolinked+1;
    	    }
    	    if (commit.get("Linked").toString().equals("Yes")) {
    	    	linked=linked+1;
    	    	tickets.add(commit.get("Ticket").toString());
    	    }
    	}
     int tot= (int) (nolinked+linked);
     float link;
     float linkage;
     if (tot!=0) {
       linkage = (linked / (tot))*100;
       link= Math.round(linkage); // per eccesso
     } 
     else {
          link= 0; 
        } 
    
     ArrayList<Object> result = new ArrayList<>();
     result.add(link);
     result.add(tickets);
     result.add(nolinked);
     result.add(linked);
     result.add(tot);
     return result;
  }
    
  public static List<String> validDate(String projName) throws JSONException, IOException {
	  String filename = "CommitLog"+projName+JSON;
      JSONObject jsonObject = parseJSONFile(filename);
      ArrayList<String> dateslist = new ArrayList<>();
	  for (Iterator<?> key=jsonObject.keys();key.hasNext();) {
  	    JSONArray commit = (JSONArray) jsonObject.get((String) key.next());
  	    String dates = commit.get(1).toString().split(":")[1];
  	    dateslist.add(dates);
	  }
	  return dateslist;
  }
  public static Map<String, Integer> countFrequencies(List<String> list){
      // hashmap to store the frequency of element
      Map<String, Integer> hm = new HashMap<>();
      for (String i : list) {
          Integer j = hm.get(i);
          hm.put(i, (j == null) ? 1 : j + 1);
      }
      return  hm;
  }
  
  public static List<String> getJiraBugFixed(String projName) throws IOException, JSONException {
     ArrayList<String> tickets = new ArrayList<>();
     Integer j = 0;
     Integer i = 0;
	 Integer total = 1;
     JSONObject jsonObject = new JSONObject();
	 JSONArray array = new JSONArray();
   //Get JSON API for closed bugs w/ AV in the project
   do {
      //Only gets a max of 1000 at a time, so must do this multiple times if bugs >1000
      j = i + 1000;
      String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
              + projName + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22resolved%22OR"
              + "%22status%22=%22closed%22)AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,fixVersions,created&startAt="
              + i.toString() + "&maxResults=" + j.toString();
      JSONObject json = readJsonFromUrl(url);
      JSONArray issues = json.getJSONArray("issues");
      total = json.getInt("total");
	  try (FileWriter fileWriter = new FileWriter(projName + "Bug_Fixed.csv")) {
          fileWriter.append("Key,id,Created_date,Resolution_date,FixVersions,AffectsVersions");
          fileWriter.append("\n");
          for (; i < total && i < j; i++) {
             JSONObject js = new JSONObject();
             fileWriter.append(issues.getJSONObject(i%1000).get("key").toString());
             js.put("Key",issues.getJSONObject(i%1000).get("key"));
             fileWriter.append(",");
             fileWriter.append(issues.getJSONObject(i%1000).get("id").toString());
             js.put("id",issues.getJSONObject(i%1000).get("id"));
             fileWriter.append(",");
             JSONObject fields= (JSONObject) issues.getJSONObject(i%1000).get("fields");
             fileWriter.append((String) fields.get(CREATED));
             js.put(CREATED,fields.get(CREATED));
             fileWriter.append(",");
             fileWriter.append((String) fields.get(RESOLUTIONDATE));
             js.put(RESOLUTIONDATE,fields.get(RESOLUTIONDATE));
             fileWriter.append(",");
             ArrayList <String> fixVersionsList = new ArrayList<>();
             JSONArray fixVersions=(JSONArray) fields.get("fixVersions");
             for (int z=0;z<fixVersions.length();z++) {
            	 fixVersionsList.add(fixVersions.getJSONObject(z).get("name").toString());
             }
             fileWriter.append(fixVersionsList.toString().replace(",","/").replace(" ",""));
             js.put("fixVersions",fixVersionsList);
             fileWriter.append(",");
             ArrayList <String> affectsVersions = new ArrayList<>();
             JSONArray versions=(JSONArray) fields.get("versions");
             for (int z=0;z<versions.length();z++) {
            	 affectsVersions.add(versions.getJSONObject(z).get("name").toString());
             }
             fileWriter.append(affectsVersions.toString().replace(",", "/").replace(" ",""));
             js.put("versions",affectsVersions);
             fileWriter.append(",");
             String msg="["+issues.getJSONObject(i%1000).get("key").toString()+"]";
             msg= msg.split("\\[")[1];
             msg= msg.split("\\]")[0];
             tickets.add(msg);
             fileWriter.append("\n");
             array.put(js);
          }
          
       } catch (Exception e) {
          LOGGER.log(Level.SEVERE, "Error in csv writer");
       }
   } while (i < total);
   JSONArray newJsonArray = new JSONArray();
		   for (int c = array.length()-1; c>=0; c--) {
		       newJsonArray.put(array.get(c));
		   }
   jsonObject.put("BugsFixed", newJsonArray);
   try (FileWriter file = new FileWriter("JiraBugsFixed"+projName+JSON)) {
			file.write(jsonObject.toString(1));
		}
   return tickets;
}
  public static List<Object> compareArrayList(List<String> arr1,List<String> arr2 ) {
		float match=0; 
		ArrayList<String> missingtickets = new ArrayList<>();
	    for (int i=0;i<arr2.size();i++) {
			  if (arr1.contains(arr2.get(i))) {
				  match=match+1;
			  }	  
			  else {
				  missingtickets.add(arr2.get(i));
			  }
		  }
	    float per = match / arr2.size();
	    ArrayList<Object> result= new ArrayList<>();
	    result.add(Math.round(per*100));
	    result.add(missingtickets);
		return result;
	  }
  public static void getMeasures(String projname,List<String> ticketsjira) throws JSONException, IOException {
	  ArrayList<Object> result = (ArrayList<Object>) checkLinkage(projname); 
	  ArrayList <String> ticketsgit = (ArrayList<String>) result.get(1);
	  ArrayList<Object> result2 = (ArrayList<Object>) compareArrayList(ticketsgit,ticketsjira);
	  JSONObject jsonObject = new JSONObject(); 
	  jsonObject.put("TicketsGit", ticketsgit); 
	  jsonObject.put("TicketsJira", ticketsjira);
	  jsonObject.put("MissingTicket",result2.get(1));
	  jsonObject.put("#CommitNotLinked", result.get(2));
	  jsonObject.put("#CommitLinked", result.get(3)); 
	  jsonObject.put("#Commit",  result.get(4));
	  jsonObject.put("Linkage #CommitLinked/#Commit", result.get(0).toString()+" %");
	  jsonObject.put("Linkage (Bug Fixed Ticket Jira/Total Ticket Git)",  result2.get(0).toString()+" %"); 	
	  jsonObject.put("MW_SIZE", ticketsjira.size()*0.1);
	  try (FileWriter file = new FileWriter("LinkageResult"+projname+JSON)) { 
		  file.write(jsonObject.toString(1)); 
		  }
		
  }
  public static void main(String[] args) throws IOException, JSONException { 
	      ArrayList<String> ticketsjira1= (ArrayList<String>) getJiraBugFixed(PROJAME1);
	      ArrayList<String> ticketsjira2= (ArrayList<String>) getJiraBugFixed(PROJNAME2);
	      getMeasures(PROJAME1,ticketsjira1);
	      getMeasures(PROJNAME2,ticketsjira2);
} }
  