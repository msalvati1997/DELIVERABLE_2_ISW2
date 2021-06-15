package milestoneone;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.csvreader.CsvReader;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
// classe che da una data specifica ritorna la versione associata 
public class DateAndRelease {
	   
	private static final String INDEX = "Index";
	private static final String VERSION_INFO_JSON = "VersionInfo.json";
	private static final String PROJNAME1="OPENJPA";
	private static final String PROJNAME2="BOOKKEEPER";
	List<String> dates ;
	String testdates ;
	String testrelease;
	
	public DateAndRelease(String testdates,String testrelease,String projname) throws JSONException, IOException {
		 this.dates = getVersionsList(projname);
		 this.testdates= testdates;
		 this.testrelease=testrelease;
	        }
	
    public int fromReleaseToIndex(String projname) throws JSONException, IOException {
    	int index1 = -1;
    	String query= "$.VersionInfo[?(@['Version Name'] =='"+testrelease+"')].Index";
    	JSONObject versioninfo = parseJSONFile(projname+VERSION_INFO_JSON);
        ReadContext ctx = JsonPath.parse(versioninfo.toString());
        List <String> index = ctx.read(query);
        if (!index.isEmpty()) {
            index1=Integer.parseInt(index.get(0));
        }
        if (index1==-1) {
        	// la versione non � stata trovata
        }
		return index1;
    }
    public int fromDateToIndex() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date testDate = sdf.parse(testdates);
        int index=0;
        int i;
        for ( i=0;i<dates.size()-1;i++) {
        	String data1 = dates.get(i);
        	String data2 = dates.get(i+1);
            Date d1 = sdf.parse(data1);
            Date d2 = sdf.parse(data2);
            if (isDateInBetweenIncludingEndPoints(d1, d2, testDate)) {
            	return i+2;
            }
            if(testDate.before(d1) && testDate.before(d2)) {
            	return i+1;
            }
        }
        index=i+1;
        
		return index;
    }
    public static boolean isDateInBetweenIncludingEndPoints( Date a,  Date b,  Date d){
    	return a.compareTo(d) * d.compareTo(b) >= 0;
    }
	public static List<String> getVersionsList(String projname) throws JSONException, IOException {
		   JSONObject versioninfo = parseJSONFile(projname+VERSION_INFO_JSON);
		   ReadContext ctx = JsonPath.parse(versioninfo.toString());
		   return  ctx.read("$.VersionInfo..Date");
	}
	public static JSONObject parseJSONFile(String filename) throws JSONException, IOException {
        String content = new String(Files.readAllBytes(Paths.get(filename)));
        return new JSONObject(content);
    }
	public static void generatingJson(String projname) throws IOException, JSONException, ParseException {
			CsvReader products2 = new CsvReader(projname+"VersionInfo.csv");
			products2.readHeaders();
			JSONObject jsonObject2 = new JSONObject();
		    JSONArray array2 = new JSONArray();
		    ArrayList <String> dates = new ArrayList<>();
 			SimpleDateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
			while (products2.readRecord()) {
			    JSONObject json = new JSONObject();
			    json.put(INDEX,products2.get(INDEX));
			    json.put("Version ID",products2.get("Version ID"));
			    json.put("Version Name",products2.get("Version Name"));
			    json.put("Date",products2.get("Date"));
			    dates.add(products2.get("Date"));
			    array2.put(json);
			}
			products2.close();
			String todayStr = inFormat.format(new Date());
			dates.add(todayStr);
			jsonObject2.put("VersionInfo", array2);
			JSONArray jsa = jsonObject2.getJSONArray("VersionInfo");
			for (int i=0;i<jsa.length();i++) {
				JSONObject jso = jsa.getJSONObject(i);
				int age = weeksBetweenTwoDate(inFormat.parse(dates.get(i+1)),inFormat.parse(dates.get(i)));
				jso.put("Age", age);
			}
			Integer index =0;
			String indexstr = null;
			for(int i=0;i<jsa.length();i++) {
				JSONObject jso = jsa.getJSONObject(i);
				if(jso.getInt("Age")==0) {
				    indexstr= (String) jso.get(INDEX);
				    index= Integer.parseInt(indexstr);
					JSONObject jsn2 = jsa.getJSONObject(i+1);
					jsn2.clear();
					break;
				}
			}
			Integer strindex = Integer.parseInt(indexstr);
			for(int i=0;i<jsa.length();i++) {
				JSONObject jso= new JSONObject();
				try {
					jso = jsa.getJSONObject(i);
				    String index3= (String) jso.get(INDEX);
				    Integer index2= Integer.parseInt(index3);
					if(index2>strindex) {
						String indextr = String.valueOf(strindex+1);
						jso.put(INDEX,indextr);
						index=index+1;
						strindex=strindex+1;
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			
			}
			try (FileWriter file = new FileWriter(projname+VERSION_INFO_JSON)) {
				file.write(jsonObject2.toString(1));
			}
			}
	public static int weeksBetweenTwoDate(Date date1,Date date2) {
		int week=0;
		long diff = date1.getTime() - date2.getTime();
		float dayCount = (float) diff / (24 * 60 * 60 * 1000);
		week = (int) (dayCount / 7) ;
		if(dayCount%7>=5) {
			week=week+1; // approssimazione
		}
		return week;
	}

	public static void main(String[] args) throws IOException, JSONException, ParseException { 
		 generatingJson(PROJNAME1);
		 generatingJson(PROJNAME2);                           
	}
	
	public List<String> getDates() {
		return this.dates;
	}
	public  void setDates(List<String> dates) {
		this.dates = dates;
	}
 
	public  String getTestDates() {
		return this.testdates;
	}

	public  void setTestDates(String testDates) {
		this.testdates = testDates;
	}

	public  String getTestRelease() {
		return this.testrelease;
	}

	public   void setTestRelease(String testRelease) {
		this.testrelease = testRelease;
	}
}
