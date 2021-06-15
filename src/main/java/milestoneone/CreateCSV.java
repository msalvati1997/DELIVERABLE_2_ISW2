package milestoneone;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import com.jayway.jsonpath.spi.json.JsonOrgJsonProvider;

public class CreateCSV {

	private static final String FILERELEASE = "$.FILE[?(@.release==";
	private static final String JSON2 = ".json";
	private static final String FILES2 = "Files";
	private static final String BUGGYNESS = "Buggyness";
	private static final String PROJECT = "Project";
	private static final String DIFF = "Diff";
	private static final String CLASS = "Class";
	private static final String VERSION = "Version";
	private static final String RESULT = "Result";
	private static final String VERSION_INFO_JSON = "VersionInfo.json";
	private static final String CREATED = "created";
	private static final String PROJNAME1 = "OPENJPA";
	private static final String PROJAME2="BOOKKEEPER";
	private static final int MW_1 = 110;
	private static final int MW_2 = 44;
	
	
	public static JSONObject parseJSONFile(String filename) throws JSONException, IOException {
        String content = new String(Files.readAllBytes(Paths.get(filename)));
        return new JSONObject(content);
    }
	public static void getLinkedCommit(String projname) throws JSONException, IOException {
		      JSONObject jsonObject = parseJSONFile("CommitLog"+projname+JSON2);
		      JsonPath jsonPath = JsonPath.compile("$.CommitsLog[?(@.Linked=='Yes')]");
		      Configuration configuration = Configuration.builder()
		              .jsonProvider(new JsonOrgJsonProvider())
		              .build();
		      Object jsonResult = JsonPath.using(configuration)
	                  .parse(jsonObject)
	                  .read(jsonPath);
		      JSONObject json = new JSONObject();
		      json.put(RESULT, jsonResult);
	
		      try (FileWriter file = new FileWriter("LinkedCommits"+projname+JSON2)) {
		   			file.write(json.toString(1));
		   		}
	}
    public static void mappinginfo(String projname,int movingwindowsize) throws JSONException, IOException, ParseException {
	      JSONObject mappingInfo = new JSONObject();
	      JSONObject linkedcommit = parseJSONFile("LinkedCommits"+projname+JSON2);
	      JSONObject jirabugfixed = parseJSONFile("JiraBugsFixed"+projname+JSON2);
	      JSONArray jira = jirabugfixed.getJSONArray("BugsFixed");
		  JSONArray resultforcsv = new JSONArray();
		  JSONArray rows = new JSONArray();
		  ArrayList <String> tickets = new ArrayList<>();
	      for (int i = 0; i<jira.length();i++) {
	    	  //ispeziono il file con tutti i commit e prendo le info relative ai commit con lo specifico ticket 
	    	    JSONObject ticket = (JSONObject) jira.get(i);
	    	    String query = ("$.Result[?(@.Ticket=='"+ticket.get("Key").toString()+"')]");
	    	    tickets.add(ticket.getString("Key"));
			    JsonPath jsonPath = JsonPath.compile(query);
			    Configuration configuration = Configuration.builder()
			              .jsonProvider(new JsonOrgJsonProvider())
			              .build();
			    Object jsonResult = JsonPath.using(configuration)
		                  .parse(linkedcommit)
		                  .read(jsonPath);
			    JSONObject result = new JSONObject();
			    result.put("CommitsLinked", jsonResult);
			    JSONObject json = new JSONObject();
			    json.put("Key", ticket.get("Key").toString());
			    json.put("AffectedVersions", ticket.get("versions"));
			    json.put(CREATED, ticket.get(CREATED));
			    json.put("resolutiondate", ticket.get("resolutiondate"));
			    json.put("id", ticket.get("id"));
			    json.put("fixVersions", ticket.get("fixVersions"));
			    json.put("CommitsRelated", result.get("CommitsLinked"));
			    //dalle info del commit mi prendo le tre celle che mi interessano
			    String query2 = "$.['"+ticket.get("Key")+"'].CommitsRelated[*].['CommitTime','DiffFiles','CommitName']";
			    mappingInfo.put(ticket.get("Key").toString(), json);
			    JsonPath jsonPath2 = JsonPath.compile(query2);
			    Configuration configuration2 = Configuration.builder()
			              .jsonProvider(new JsonOrgJsonProvider())
			              .build();
			    Object jsonResult2 = JsonPath.using(configuration2)
		                  .parse(mappingInfo)
		                  .read(jsonPath2);
			    JSONObject result2 = new JSONObject();
			    result2.put(RESULT, jsonResult2);
			    JSONArray files = result2.getJSONArray(RESULT);
			    resultforcsv=extracted(projname, resultforcsv, ticket, files);
		      }
	      JSONObject js1 = new JSONObject();
	      js1.put("MappingJiraGit",resultforcsv);
		  JSONArray json2= new JSONArray();
	
		  //CALCOLO PROPORTION   
	      JSONArray def1 = new JSONArray();
		  JSONObject prova = proportion(tickets, js1, json2, def1);
		  String query = "$.Mapping3.*";
		  ReadContext ctx = JsonPath.parse(prova.toString());
		  List <Object> resu = ctx.read(query);  
		  Collections.reverse(resu);
		  JSONObject prova2 = new JSONObject();
		  prova2.put("Mapping", resu);
		  JSONArray ar = prova2.getJSONArray("Mapping");  
	      try (FileWriter file = new
				  FileWriter("mappingInfo2.json")) { file.write(prova2.toString(1)); }
	      ArrayList <Integer> mw = new ArrayList<>(movingwindowsize);  // creo la mia moving window con la specifica size
		  rows = movingwindow(projname, movingwindowsize, rows, prova2, ar, mw);
		  JSONObject historyfiles = parseJSONFile("FileHistory"+projname+JSON2);
		  JSONObject prova3 = new JSONObject();
		  prova3.put("CSV", rows);
		  JSONArray newrows = new JSONArray();	  
	      String querymaxversion= "$.VersionInfo..Index";
	      JSONObject versioninfo = parseJSONFile(projname+VERSION_INFO_JSON);
		  DocumentContext ctx2 = JsonPath.parse(versioninfo.toString());
		  ArrayList <Integer> resu3 = ctx2.read(querymaxversion);
		  int end1 = resu3.size()/2; // PRENDO SOLO LA PRIMA META' DELLE RELEASE
		  for (Iterator<?> key=historyfiles.keys();key.hasNext();) {	
			    String filename = (String) key.next();
	    	    JSONArray file =  (JSONArray) historyfiles.get(filename);
		        String query3 = "$.CSV[?(@.Class=='"+filename+"')]['Version']";
		        ReadContext ctx3 = JsonPath.parse(prova3.toString());
		        ArrayList <Integer> resu2 = ctx3.read(query3);
		        Collections.sort(resu2);
		        int max =0;//Version before FV
		        int min =0;//IV 
				/*
				 *  verificare se la classe ha un commit relativo a MIN (IV) altrimenti
				 * decremento min fintanto che la classe ##non ha un commit relativo se la
				 * classe non ha un commit relativo non � possibile che ci sia IV ## mentre la
				 * FV � gi� controllata a partire dal commit quindi ok
				 */
		        int fv = 0;
		        
		        try {
		        	max =resu2.size();
			        min =resu2.get(0);
			        fv=max+1;
		        } catch (Exception e) {
		        	e.printStackTrace();
				}
		    	int addingrelease = 0;
	        	int deleterelease = 0;
	        	ArrayList<Integer> filetouched = new ArrayList<>();	     
			      JSONObject featuresfile = new JSONObject();
			      JSONArray features = new JSONArray();
		        for(int p=0;p<file.length();p++) {
		        	JSONObject revisionfile = file.getJSONObject(p);
		        	String auth = revisionfile.getString("PersonIdent");
		        	int release=revisionfile.getInt("Release");
		        	int changes = revisionfile.getInt("ChangesLine");
		        	int added = revisionfile.getInt("AddedLines=");
		        	int delete = revisionfile.getInt("DeleteLines=");
		        	JSONObject feature = new JSONObject();
		        	feature.put("release", release);
		        	feature.put("auth", auth);
		        	feature.put("changes", changes);
		        	feature.put("AddedLines", added);
		        	feature.put("DeleteLines", delete);
		        	features.put(feature);
		        	String opt=revisionfile.getString("Opt");
		        	if(opt.equals("MODIFY")) {
		        		filetouched.add(release);
		        	}
		        	addingrelease = checkaddingrelease(addingrelease, release, opt);
		        	deleterelease = checkdeleterelease(deleterelease, release, opt);
		        }
		        featuresfile.put("FILE", features);        
		        if(addingrelease==0) { // non considero il file 
		        	continue; // vado avanti nell'iterazione
		        }
		        int end = getendrelease(end1, deleterelease);
		        ArrayList<Integer> classexistence = getclassexistence(addingrelease, end);
        	    //devo cancellare le release in cui il file non era esistente (prima) 
	        	resu2 = getresu2(resu2, max, min, fv, addingrelease, deleterelease);          
	    
        		if(filename.endsWith(".java")) {
	        	newrows.putAll(getrowsforfilename(projname, newrows, filename, resu2, featuresfile, classexistence)); }
	       }
		   json2csv(newrows,projname);
		 
	      }
	private static JSONArray getrowsforfilename(String projname, JSONArray newrows, String filename,
			ArrayList<Integer> resu2, JSONObject featuresfile, ArrayList<Integer> classexistence) throws IOException {
		for(int u=0;u<classexistence.size();u++) {
			int release = classexistence.get(u);
			if(resu2.contains(release)) {
				  JSONObject row = extractrowinfo1(projname, filename, featuresfile,release);
				  newrows.put(row);
			}
			else {
				  JSONObject row = extractrowinfo2(projname, filename, featuresfile, release);
				  newrows.put(row);
		   }
		}
		return newrows;
	}
	private static ArrayList<Integer> getclassexistence(int addingrelease, int end) {
		ArrayList <Integer> classexistence = new ArrayList <>();

		for (int n=addingrelease;n<=end;n++) { // parto da la release in cui � stato aggiunto il file fino a END
			classexistence.add(n);
		}
		return classexistence;
	}
	private static int getendrelease(int end1, int deleterelease) {
		int end=0;
		if(deleterelease==0) {
			end=end1;
		}
		else {
			end=deleterelease-1;
		}
		if (deleterelease>=end1) {
			end=end1;
		}
		return end;
	}
	private static int checkdeleterelease(int deleterelease, int release, String opt) {
		if(opt.equals("DELETE")) {
			 deleterelease = release;
		}
		return deleterelease;
	}
	private static int checkaddingrelease(int addingrelease, int release, String opt) {
		if(opt.equals("ADD")) {
			 addingrelease = release;
		}
		return addingrelease;
	}
	private static ArrayList<Integer> getresu2(ArrayList<Integer> resu2, int max, int min, int fv, int addingrelease,
			int deleterelease) {
		if(min!=0 && min<addingrelease) {
			for(int l=min;l<addingrelease;l++) {
				while(resu2.remove(Integer.valueOf(l))); // rimuove tutte le occorrenze
				 // rimuovo tutte le release precedente alla release di aggiunta 
			}
		}
		// devo cancellare le release dei file cancellati prima di FV  
		if(max!=0 && deleterelease!=0 && deleterelease<=fv) {
			for(int l=deleterelease;l<fv;l++) {
				while(resu2.remove(Integer.valueOf(l)));
			}
		}
		resu2 = new ArrayList<>(new LinkedHashSet<>(resu2));
		return resu2;
	}
	private static JSONArray extracted(String projname, JSONArray resultforcsv, JSONObject ticket, JSONArray files)
			throws IOException, ParseException {
		for (int z=0;z<files.length();z++) {
			int ivindex = 0;
			JSONObject infocommit = files.getJSONObject(z);
			Object committime = infocommit.get("CommitTime");
			Object difffiles = infocommit.get("DiffFiles");
		    JSONObject jsonforcsv = new JSONObject();
		    jsonforcsv.put(FILES2, difffiles);
		    DateAndRelease dr = new DateAndRelease(committime.toString(),"", projname);
		    jsonforcsv.put("FV", dr.fromDateToIndex());
		    jsonforcsv.put("Key", ticket.get("Key").toString());
		    JSONArray jiraav =ticket.getJSONArray("versions");
		    JSONArray jiraav2= new JSONArray();
			String iv = null;
		    for (int p=0;p<jiraav.length();p++) {
		    	dr.setTestRelease(jiraav.getString(p));
		    	int version = dr.fromReleaseToIndex(projname);
		    	iv=jiraav.getString(0);
		    	if(version!=-1) {
		    	jiraav2.put(version); 
		    	}
		    }
		    jsonforcsv.put("AV", jiraav2);
		    //IV � la prima versione dell'AV disponibile
		    resultforcsv.put(getjsonforcsv(projname, ticket, ivindex, jsonforcsv, dr, iv));
		}
		return resultforcsv;
	}
	private static JSONObject getjsonforcsv(String projname, JSONObject ticket, int ivindex, JSONObject jsonforcsv,
			DateAndRelease dr, String iv) throws IOException, ParseException {
		int ovindex;
		if (iv!=null) {
		    dr.setTestRelease(iv);
			ivindex = dr.fromReleaseToIndex(projname);
			if(projname.equals(PROJAME2) && ivindex==4) {
				ivindex=0; // non � possibile che un bug di Bookkeper sia iniettato nella versione 4 non avendo commit
			    jsonforcsv.put("AV", new JSONArray());
			}
			if(ivindex!=-1) {
		    jsonforcsv.put("IV", ivindex); 
		    }
		    if((ivindex)==-1) { // vuol dire che la versione non � stata trovata 
		    	jsonforcsv.put("IV", 0);
		    }
		}
		if (iv==null) {
		    jsonforcsv.put("IV", 0);
		}
		// OV � la prima versione disponibile in cui il TICKET � stato aperto
		dr.setTestDates(ticket.get(CREATED).toString());
		ovindex = dr.fromDateToIndex();
		jsonforcsv.put("OV", dr.fromDateToIndex());
		if(ivindex>ovindex) {    // verifico l'affidabilit� dei ticket su jira
			jsonforcsv.put("IV", 0); //elimino le info sensibili e mi prendo solo OV e FV
		    jsonforcsv.put("AV", new JSONArray());
		}
		return jsonforcsv;
	}
	private static JSONArray movingwindow(String projname, int movingwindowsize, JSONArray rows, JSONObject prova2,
			JSONArray ar, ArrayList<Integer> mw) throws IOException {
		for (int i = 0;i<ar.length();i++) {
			  JSONObject es = ar.getJSONObject(i);
			  Integer p = es.getInt("P");
			  Integer iv = es.getInt("IV");
			  Integer ov = es.getInt("OV");
			  Integer fv = es.getInt("FV");
			  JSONArray av = es.getJSONArray("AV");
			  double total = 0;
			  if (p==-1) {
				for(int c = 0; c < mw.size(); c++)  
					    total=total+mw.get(c); 
			            double avg = total/  mw.size();
			            avg = approximation(mw, total, avg);
					    p=(int) avg;
			            mw=movingwindowmove(movingwindowsize, mw, p);
			    iv = fv-(fv-ov)*p; 
			    es.put("IV",iv);
			    es.put("P",p);
			    for(int l=iv; l<fv;l++) {
      	    		  av.put(l);
      	        }
			    es.put("AV",av);
			  }
			  else {
				  mw=movingwindowmove(movingwindowsize, mw, p);
		  }   
			  try (FileWriter file = new FileWriter("prova2.json")) {
				  file.write(prova2.toString(1)); }
			  JSONArray files = es.getJSONArray(FILES2);
			  rows=aggregateinfo(projname, rows, av, files); 
	      }
		return rows;
	}
	private static ArrayList<Integer> movingwindowmove(int movingwindowsize, ArrayList<Integer> mw, Integer p) {
		if (mw.size()<movingwindowsize) {
			  mw.add(p);
			  }
		  else {
			  mw.remove(0);
			  mw.add(p);
		  }
		return mw;
	}
	private static double approximation(ArrayList<Integer> mw, double total, double avg) {
		if(total%mw.size()>=5) {
			avg=avg+1; // approssimazione
		}
		return avg;
	}
	private static JSONObject extractrowinfo1(String projname, String filename, JSONObject featuresfile, int release)
			throws IOException {
    	ArrayList<Integer> releaseages = new ArrayList<>();
		ArrayList<Integer> addedloc = new ArrayList<>();
		ArrayList<Integer> sizeloc = new ArrayList<>();
		String query;
		ReadContext ctx;
		List<Object> resu;
		JSONObject versioninfo;
		DocumentContext ctx2;
		JSONObject row = new JSONObject();
		  row.put(PROJECT, projname);
		  row.put(CLASS, filename);
		  row.put(VERSION, release);
		  row.put(BUGGYNESS, true);
		  /// 1 FEATURES - NUMERO AUTORI NELLA DETERMINATA RELEASE
		  query = FILERELEASE+release+")]['auth']";
		  ctx = JsonPath.parse(featuresfile.toString());
		  ArrayList <String> authors = ctx.read(query);
		  LinkedHashSet<String> hashSets = new LinkedHashSet<>(authors);
		  ArrayList<String> authors1 = new ArrayList<>(hashSets);         
		  int nauth= authors1.size();
		  row.put("Nauth", nauth);
		  /// 2 FEATURES - NUMERO DI REVISIONI 
		  int nr = authors.size();
		  row.put("NR", nr);
		  /// 3 FEATURES - Churn
		  query = FILERELEASE+release+")]['changes']";
		  ctx = JsonPath.parse(featuresfile.toString());
		  ArrayList <Integer> changes = ctx.read(query);
		  int sum=0;
		  for(int v=0;v<changes.size();v++) {
			  sum=sum+changes.get(v);
		  }
		  sizeloc.add(sum);
		  row.put("Churn", sum);
		  //4 FEATURES :  Loc ADDED 
		  query = FILERELEASE+release+")]['AddedLines']";
		  ArrayList <Integer> addedlines = ctx.read(query);
		  int sumlocadded=0;
		  for(int v=0;v<addedlines.size();v++) {
			  sumlocadded+=addedlines.get(v);
		  }
		  addedloc.add(sumlocadded);
		  row.put("LOC Added", sumlocadded);
		  //5 FEATURES :  MAX LOC Added
		  int maxlocadded=0;
		  if(!addedlines.isEmpty()) {
			  maxlocadded=Collections.max(addedlines);
		  }
		  row.put("MAX LOC Added",maxlocadded );
		  //6 FEATURES :  AVG LOC Added
		  int avglocadded=sumlocadded;
		
		  if(!addedlines.isEmpty()) {
		    avglocadded=sumlocadded/addedlines.size();
		    if(sumlocadded%addedlines.size()>=5) {
				  avglocadded=avglocadded+1; // approssimazione
		        }
		  }
		  row.put("AVG LOC Added",avglocadded );

		  //7 FEATURES : LOC Touched 
		  query = FILERELEASE+release+")]['DeleteLines']";
		  ctx = JsonPath.parse(featuresfile.toString());
		  ArrayList <Integer> deletelines = ctx.read(query);
		  int loctouched=sumlocadded;
		  for(int v=0;v<deletelines.size();v++) {
			  loctouched-=deletelines.get(v);
		  }
		  row.put("LOC TOUCHED",loctouched );

		  //8 FEATURES : Max Churn 
		  ArrayList<Integer> churncomputation = new ArrayList<>();
		  int maxchurn=0;
		  for(int i=0;i<deletelines.size();i++) {
			  int churni= addedlines.get(i)+deletelines.get(i);
			  churncomputation.add(churni);
		  }
		  if(!churncomputation.isEmpty()) {
			   maxchurn =Collections.max(churncomputation);
		  }
		  row.put("Max Churn",maxchurn);
		  // 9 FEATURES : Average Churn 
		  int avgchurn=sum;  //sum � il churn over revisions - nel caso la revisions sia 1 p pari al churn medio
		  if(!churncomputation.isEmpty()) {
			  avgchurn=sum/churncomputation.size();
			if(sum%churncomputation.size()>=5) {
				avgchurn=avgchurn+1; // approssimazione
		        }
		  }

		  row.put("AVG CHURN",avgchurn );
		  //10 FEATURES: Age - versions age in weeks
		  query= "$.VersionInfo[?(@.Index=="+release+")]['Age']";
		  versioninfo = parseJSONFile(projname+VERSION_INFO_JSON);
		  ctx2 = JsonPath.parse(versioninfo.toString());
		  resu = ctx2.read(query);
		  Object age = resu.get(0);
		  row.put("Age", resu.get(0));
		  releaseages.add((Integer) age);
		  //11 FEATURES Weighted Age  
		  
		  row.put("Weighted Age",getweightedage(releaseages, addedloc));

		  //12 FEATURES Size
		  int sumsize=0;
		  for(int i=0;i<sizeloc.size();i++) {
			  sumsize=sumsize+sizeloc.get(i);
		  }
		  row.put("Size", sumsize);
		return row;
	}
	private static int getweightedage(ArrayList<Integer> releaseages, ArrayList<Integer> addedloc) {
		int sumages=0;
		  int num=0;
		  int den=0;
		  int sumadded=0;
		  int weightedage=0;

		  for(int i=0;i<releaseages.size();i++) {
			  sumages=+releaseages.get(i);	
			  sumadded = +addedloc.get(i);
			  num=num+sumages*sumadded;
		  }
		  for(int i=0;i<addedloc.size();i++) {
			  den=den+addedloc.get(i);
		  }
		  if(num!=0 && den!=0) {
			  weightedage=num/den; 
			  if(num%den>=5) {
				  weightedage=weightedage+1; // approssimazione
		      } 
			  }		
		  return weightedage;
	}
	private static JSONObject extractrowinfo2(String projname, String filename, JSONObject featuresfile,
			 int release)
			throws IOException {
    	ArrayList<Integer> releaseages = new ArrayList<>();
		ArrayList<Integer> addedloc = new ArrayList<>();
		ArrayList<Integer> sizeloc = new ArrayList<>();
		String query;
		ReadContext ctx;
		List<Object> resu;
		JSONObject versioninfo;
		DocumentContext ctx2;
		JSONObject row = new JSONObject();
		  row.put(PROJECT, projname);
		  row.put(CLASS, filename);
		  row.put(VERSION, release);
		  row.put(BUGGYNESS, false);
		  /// 1 FEATURES - NUMERO AUTORI NELLA DETERMINATA RELEASE
		  query = FILERELEASE+release+")]['auth']";
		  ctx = JsonPath.parse(featuresfile.toString());
		  ArrayList <String> authors = ctx.read(query);
		  LinkedHashSet<String> hashSets = new LinkedHashSet<>(authors);
		  ArrayList<String> authors1 = new ArrayList<>(hashSets);         
		  int nauth= authors1.size();
		  row.put("Nauth", nauth);
		  /// 2 FEATURES - NUMERO DI REVISIONI 
		  int nr = authors.size();
		  row.put("NR", nr);
		  /// 3 FEATURES -Churn
		  query = FILERELEASE+release+")]['changes']";
		  ArrayList <Integer> changes = ctx.read(query);
		  int sum=0;
		  for(int v=0;v<changes.size();v++) {
			  sum=sum+changes.get(v);
		  }
		  sizeloc.add(sum);
		  row.put("Churn", sum);
		  //4 FEATURES :  LOC Added 
		  query = FILERELEASE+release+")]['AddedLines']";
		  ctx = JsonPath.parse(featuresfile.toString());
		  ArrayList <Integer> addedlines = ctx.read(query);
		  int sumlocadded=0;
		  for(int v=0;v<addedlines.size();v++) {
			  sumlocadded+=addedlines.get(v);
		  }
		  row.put("LOC Added", sumlocadded);
		  addedloc.add(sumlocadded);
		  //5 FEATURES :  MAX LOC Added
		  int maxlocadded=0;
		  if(!addedlines.isEmpty()) {
			  maxlocadded=Collections.max(addedlines);
		  }
		  row.put("MAX LOC Added",maxlocadded );

		  //6 FEATURES :  AVG LOC Added
		  int avglocadded=sumlocadded;

		  if(!addedlines.isEmpty()) {
		    avglocadded=sumlocadded/addedlines.size();
			if(sumlocadded%addedlines.size()>=5) {
				  avglocadded=avglocadded+1; // approssimazione
		        }
		  }
		  row.put("AVG LOC Added",avglocadded );
		  //7 FEATURES : LOC Touched 
		  query = FILERELEASE+release+")]['DeleteLines']";
		  ctx = JsonPath.parse(featuresfile.toString());
		  ArrayList <Integer> deletelines = ctx.read(query);
		  int loctouched=sumlocadded;
		  for(int v=0;v<deletelines.size();v++) {
			  loctouched-=deletelines.get(v);
		  }
		  row.put("LOC TOUCHED",loctouched );
		
		  //8 FEATURES : Max Churn 
		  ArrayList<Integer> churncomputation = new ArrayList<>();
		  int maxchurn=0;
		  for(int i=0;i<deletelines.size();i++) {
			  int churni= addedlines.get(i)+deletelines.get(i);
			  churncomputation.add(churni);
		  }
		  if(!churncomputation.isEmpty()) {
			   maxchurn =Collections.max(churncomputation);
		  }
		  row.put("Max Churn",maxchurn);
		  // 9 FEATURES : Average Churn 
		  int avgchurn=sum;  //sum � il churn over revisions - nel caso la revisions sia 1 p pari al churn medio
		  if(!churncomputation.isEmpty()) {
			  avgchurn=sum/churncomputation.size();
			if(sum%churncomputation.size()>=5) {
				avgchurn=avgchurn+1; // approssimazione
		        }
		  }

		  row.put("AVG CHURN",avgchurn );
		  //10 FEATURES: Age- versions age in weeks
		  query= "$.VersionInfo[?(@.Index=="+release+")]['Age']";
		  versioninfo = parseJSONFile(projname+VERSION_INFO_JSON);
		  ctx2 = JsonPath.parse(versioninfo.toString());
		  resu = ctx2.read(query);
		  Object age = resu.get(0);
		  row.put("Age", resu.get(0));
		  releaseages.add((Integer) age);
		  //11 FEATURES :  Weighted Age  

		  row.put("Weighted Age",getweightedage(releaseages, addedloc));
		  //12 FEATURES Size
		  int size1=0;
		  for(int i=0;i<sizeloc.size();i++) {
			  size1=size1+sizeloc.get(i);
		  }
		  row.put("Size", size1);
		return row;
	}
	private static JSONObject proportion(ArrayList<String> tickets, JSONObject js1, JSONArray json2, JSONArray def1)
			throws IOException {
		for (int t=0; t<tickets.size();t++) {
    		  String tckt= tickets.get(t);
    	      String query = ("$.MappingJiraGit[?(@.Key=='"+tckt+"')]");
    		  JsonPath jsonPath = JsonPath.compile(query);
    		  Configuration configuration = Configuration.builder()
    		              .jsonProvider(new JsonOrgJsonProvider())
    		              .build();
    		  JSONArray jsonResult = JsonPath.using(configuration)
    	                  .parse(js1)
    	                  .read(jsonPath);
    		  JSONObject result = new JSONObject();
    		  if (jsonResult.length()>0) {
    			  result.put(tckt, jsonResult);
        		  json2.put(result);
        		  String query2= "$."+tckt+"[*].FV";
        		  ReadContext ctx = JsonPath.parse(result.toString());
        	      List <Integer> fvlist = ctx.read(query2);
        	      Integer fv = Collections.max(fvlist); // prendo la data dell'ultimo commit
        	      query2= "$."+tckt+"[0].OV";
        	      Integer ov = ctx.read(query2);
        	      query2= "$."+tckt+"[0].IV";
        	      Integer iv = getiv(query2, ctx, ov);
        	      query2= "$."+tckt+"[0].AV";
        	      List <Integer> av = ctx.read(query2);
        	      av=removeFrom(av,fv);
        	   
        	      if (iv!=0) {
        	      for(int l=iv; l<fv;l++) {        // AVs are in the range [IV, FV)
        	    	  if(!av.contains(l)) {
        	    		  av.add(l);
        	      }
        	      } }
        	      Collections.sort(av);
        	      query2= "$."+tckt+"[*].Files.*";
        	      List <String> files = ctx.read(query2);    	
        	      JSONObject def = new JSONObject();
        	      def.put("OV", ov);
        	      def.put("IV", iv);
        	      def.put("FV", fv);
        	      def.put("AV", av);
        	      def.put(FILES2, files);
        	      def.put("Ticket", tckt);
        	      def1=extracted(def1, tckt, fv, ov, iv, def);
        	      }
		  }
		  JSONObject prova = new JSONObject();
		  prova.put("Mapping3", def1);
		  try (FileWriter file = new FileWriter("prova1.json")) {
			  file.write(prova.toString(1)); }
		return prova;
	}
	private static Integer getiv(String query2, ReadContext ctx, Integer ov) {
		Integer iv = ctx.read(query2);
		  if (ov<iv) {
			  iv=ov;
		  }
		  if (ov==1) {
			  iv=ov; //non pu� essere avvenuta prima 
		  }
		return iv;
	}
	private static JSONArray extracted(JSONArray def1, String tckt, Integer fv, Integer ov, Integer iv, JSONObject def) {
		int p;
		if (iv!=0) {
		  int den = fv-ov; 
		  int num = fv-iv;
		  if (den==0) {
			  p=0;
		      def.put("IV", ov); // se P=0 -> IV=OV
		      JSONArray avlist = new JSONArray();
		      if(ov.equals(fv)) {
		    	  def.put("AV", new JSONArray());
		      }
		      else {
		      for(int l=ov;l<=fv;l++) {
		    	  avlist.put(l);
		      }
		      def.put("AV", avlist); // se P=0 -> AV=IV (unica versione affetta)
		  } }
		  else {
			  p=num/den;  //METODO PROPORTION
		  }
		  def.put("P", p);
		  }
		  else { 
			  def.put("P", -1); 
		   }
		  def.put("Ticket", tckt);
		  return def1.put(def);
	}
	private static JSONArray aggregateinfo(String projname, JSONArray rows, JSONArray av, JSONArray files) {
		for (int p1=0;p1<files.length();p1++) {
			  String classname = files.get(p1).toString();
			  classname = classname.split("added=+")[0];
			  String[] splitted = classname.split(":");
			  if (av.length()>0) {
				  String[] names = splitted[2].split("->");
				  String filename = getfilename(classname, names);
					  // qui che ho il filename posso verificare se IV esiste cos� eventualmente la scalo o aggiungo ecc ecc 
				    	for (int t=0;t<av.length();t++) {
								  JSONObject row = new JSONObject();
								  row.put(PROJECT, projname);
								  row.put(DIFF, classname.split(":")[1].trim());
								  row.put(CLASS, filename);
								  row.put(VERSION, av.get(t));
								  row.put(BUGGYNESS, true);
								  rows.put(row);
				    	}
				  } 
		  }
		return rows;
	}
	private static String getfilename(String classname, String[] names) {
		String filename = null;
		   if(names.length==1) {
				   filename=names[0].trim();
		   }
			  if(names.length==2) {
				  if (classname.split(":")[1].endsWith("E")) {
					   filename=names[0].trim();
				  }
				  if (classname.split(":")[1].endsWith("D")) {
					   filename=names[1].trim();
				  }  
				  }
		return filename;
	} 
    @SuppressWarnings("deprecation")
	public static  void json2csv(JSONArray array,String projname) throws IOException {         
    	File file=new File("fromJSON"+projname+".csv");
        String csv = CDL.toString(array);
        FileUtils.writeStringToFile(file, csv);
    }
    public boolean isWithinRange(Date testDate,Date startDate,Date endDate) {
        return testDate.getTime() >= startDate.getTime() &&
                testDate.getTime() <= endDate.getTime();
    }
    static List<Integer> removeFrom(List<Integer> list, int fv) {
        list.removeIf(n -> (n >=fv));
        return list;
      }

	public static void main(String[] args) throws IOException, JSONException, ParseException { 
        getLinkedCommit(PROJNAME1);
	 	mappinginfo(PROJNAME1,MW_1);
	 	getLinkedCommit(PROJAME2);
	 	mappinginfo(PROJAME2,MW_2);
} }
	 
			
	 