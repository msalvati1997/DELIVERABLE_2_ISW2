package weka;

import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.AttributeStats;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.supervised.instance.SpreadSubsample;
import weka.core.converters.ArffSaver;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONObject;


import weka.core.converters.ConverterUtils.DataSource;

public class WekaEvaluator {
	
	private static final String NAIVE_BAYES = "NaiveBayes";
	private static final String RANDOM_FOREST = "RandomForest";
	private static final String ACCURACY = "Accuracy";
	private static final String KAPPA = "Kappa";
	private static final String AUC = "AUC";
	private static final String F_MEASURE = "F-Measure";
	private static final String RECALL = "Recall";
	private static final String PRECISION = "Precision";
	private static final String FP_RATE = "FP Rate";
	private static final String TP_RATE = "TP Rate";
	private static final String FN = "FN";
	private static final String TN = "TN";
	private static final String FP = "FP";
	private static final String TP = "TP";
	private static final String SENSITIVITY = "Sensitivity";
	private static final String FEATURE_SELECTION = "Feature Selection";
	private static final String BALANCING = "Balancing";
	private static final String CLASSIFIER2 = "Classifier";
	private static final String TEST_DEFECTIVE = "Test Defective %";
	private static final String DATASET = "Dataset";
	private static final String TEST_RELEASE = "Test Release";
	private static final String TRAINING_RELEASES = "# Training Releases";
	private static final String TRAINING = "Training %";
	private static final String TRAINING_DEFECTIVE = "Training Defective %";
	private static final String USER_DIR = "user.dir";
	private static final String PROJNAME1 ="OPENJPA";
	private static final String PROJNAME2="BOOKKEEPER";

	@SuppressWarnings("unlikely-arg-type")
	//walk forward iteration 
	//ritorna una mappa che contiene training e test secondo l'algoritmo walk forward
	
	public static Map<Instances, Instances> walkforward(String path, String projName)  {
		Map<Instances, Instances> trainingandtest = new HashMap<>();

		try {
			ArrayList  <Instances> trainlist= new ArrayList<>();
			ArrayList  <Instances> testlist= new ArrayList<>();

			DataSource source = new DataSource(path);
			Instances dataset = source.getDataSet();
			//set class index to the last attribute
			dataset.setClassIndex(dataset.numAttributes()-1);
			// get attributes list 
			ArrayList <Attribute> attributeslist= new ArrayList<>();
			for(int i=0; i<dataset.numAttributes();i++) {
				attributeslist.add(dataset.attribute(i));
			}
			//creates the lists that containts releases for training and test
			double lastversion =dataset.instance(dataset.numInstances()-1).value(0);
			double startversion = 1;
			double numberofiteration = lastversion-1;
			ArrayList <Double> trainingreleases = new  ArrayList<>();
			ArrayList <Double> testingreleases = new  ArrayList<>();
			
			for (int j=0;j<numberofiteration;j++) {
			 testingreleases.clear();
			 trainingreleases.add(startversion);
			 testingreleases.add(startversion+1);
			 startversion+=1;
			 int index=-1;
			 int indextest=0;
			 for (int i = 0; i < dataset.numInstances(); i++) {
				    double curr = dataset.instance(i).value(0);
				    if(trainingreleases.contains(curr)) {
				    	 index=i+1;
				    }
				    if(testingreleases.contains(curr)) {
				    	indextest=i+1;
				    }
			}
			List<Instance> traininstances= dataset.subList(0, index);
			List<Instance> testinstances= dataset.subList(index+1, indextest);
			
			int indx = j+1;
			Instances train = new Instances("Train-"+indx+"-"+projName, attributeslist, traininstances.size());
			Instances test = new Instances("Test"+indx+"-"+projName, attributeslist, testinstances.size());
			//
			for(int i=0;i<traininstances.size();i++) {
				train.add(traininstances.get(i));
			}
			//
			for(int i=0;i<testinstances.size();i++) {
				test.add(testinstances.get(i));
			}
			//
			trainingandtest.put(train, test);
			trainlist.add(train);
			testlist.add(test);
			//
			ArffSaver s= new ArffSaver();
			s.setInstances(train);
			s.setFile(new File(System.getProperty(USER_DIR)+"\\src\\main\\resources\\Training_"+j+projName+".arff"));
			s.writeBatch();
			//
			ArffSaver t= new ArffSaver();
			t.setInstances(test);
			t.setFile(new File(System.getProperty(USER_DIR)+"\\src\\main\\resources\\Testing_"+j+projName+".arff"));
			t.writeBatch();
			//
			}
		} catch (Exception ex) {
			java.util.logging.Logger.getLogger("WekaEvaluator").log(Level.INFO, "Exception", ex);
		}
		return trainingandtest;
	}
	
	public static void evaluatorandmetric(Map<Instances, Instances> trainingandtest,String projName)   {
		
	try {
		JSONArray js = new JSONArray();
		ArrayList <Evaluation> evallist = new ArrayList<>();
		ArrayList <ArrayList <Double>> info = new ArrayList<>();
		ArrayList <ArrayList <Double>> infoselected = new ArrayList<>();
		ArrayList <Evaluation> evallistundersampling= new ArrayList<>();
		ArrayList <ArrayList <Double>> infoundersampling= new ArrayList<>();
		ArrayList <String> classifier = new ArrayList<>();
		ArrayList <Evaluation> evallistselected = new ArrayList<>();
		ArrayList <Evaluation> evallistoversampling= new ArrayList<>();
		ArrayList <ArrayList <Double>> infooversampling= new ArrayList<>();	
		ArrayList <Evaluation> evallistsmote= new ArrayList<>();
		ArrayList <ArrayList <Double>> infosmote= new ArrayList<>();
		ArrayList <Evaluation> evallisttr= new ArrayList<>();
		ArrayList <ArrayList <Double>> infotr= new ArrayList<>();
		ArrayList <Evaluation> evallistlr= new ArrayList<>();
		ArrayList <ArrayList <Double>> infolr= new ArrayList<>();
		
		for (Map.Entry<Instances,Instances> entry : trainingandtest.entrySet()) {
	           
	             Instances train = entry.getKey();
	             Instances test  =  entry.getValue();
	             AttributeStats stattrain = train.attributeStats(13);
	             double trainBuggy= stattrain.nominalCounts[1];
	     		 //set class index to the last attribute
			     test.setClassIndex(test.numAttributes()-1);
			     train.setClassIndex(train.numAttributes()-1);
			     	     
	     		 test.attributeStats(13);
	     		 double testBuggy= stattrain.nominalCounts[1];
	     		 double trainInstances = train.numInstances();
                 double trainPlusTestInstances = (double) train.numInstances()+ (double) test.numInstances();
	     		 double trainSize = divide(trainInstances,trainPlusTestInstances);
	     		 double trainDefectiveSize = divide(trainBuggy,train.numInstances());
	     		 double testDefectiveSize = divide(testBuggy,test.numInstances());
			     ArrayList<Double> trainandtest = new ArrayList<>() ;
			     trainandtest.add(test.instance(0).value(0));
			     trainandtest.add(train.instance(train.numInstances()-1).value(0));
			     trainandtest.add(trainSize);
			     trainandtest.add(trainDefectiveSize);
			     trainandtest.add(testDefectiveSize);
			     //STANDARD - NO BALANCING- NO SELECTION 

	             // NAIVEBAYES
	             Evaluation evalnb = new Evaluation(test);
	             NaiveBayes nbClass = new NaiveBayes();
	             nbClass.buildClassifier(train);
	             evalnb.evaluateModel(nbClass, test);
	             evallist.add(evalnb);
			     info.add(trainandtest);
			     classifier.add(NAIVE_BAYES);

	             //IBK
				 IBk ibkClass = new IBk(); // vedere bene l'input di creazione 
				 ibkClass.buildClassifier(train);
	             Evaluation evalibk = new Evaluation(test);
	             evalibk.evaluateModel(ibkClass, test);
	             evallist.add(evalibk);
			     info.add(trainandtest);
			     classifier.add("IBk");
	     
	             //Random Forest
	             RandomForest randomForestClass = new RandomForest();
	             randomForestClass.buildClassifier(train);
	             Evaluation evalrf = new Evaluation(test);
	             evalrf.evaluateModel(randomForestClass, test);
	             evallist.add(evalrf);
			     info.add(trainandtest);
			     classifier.add(RANDOM_FOREST);
			     
			     //SELECTION
			     
			     AttributeSelection filterselected = FeaturesSelection.selectAttributes();
			     filterselected.setInputFormat(train);
			     Instances trainselected = Filter.useFilter(train, filterselected);
			     Instances testselected = Filter.useFilter(test, filterselected);
			    
			     NaiveBayes selectionNbClass = new NaiveBayes();
			     Evaluation evalnb1 = new Evaluation(testselected);
			     selectionNbClass.buildClassifier(trainselected);
		         evalnb1.evaluateModel(selectionNbClass, testselected);
		         evallistselected.add(evalnb1);
		         infoselected.add(trainandtest);
		  	     classifier.add(NAIVE_BAYES);
	
		  	     IBk selectionIBkClass = new IBk(); // vedere bene l'input di creazione 
		  	     selectionIBkClass.buildClassifier(trainselected);
	             Evaluation evalibk1 = new Evaluation(testselected);
	             evalibk1.evaluateModel(selectionIBkClass, testselected);
	             evallistselected.add(evalibk1);
	             infoselected.add(trainandtest);
			     classifier.add("IBk");
			     
			     RandomForest selectionRFClass = new RandomForest();
			     selectionRFClass.buildClassifier(trainselected);
		         Evaluation evalrf1 = new Evaluation(testselected);
		         evalrf1.evaluateModel(selectionRFClass, testselected);
		         evallistselected.add(evalrf1);
		         infoselected.add(trainandtest);
				 classifier.add(RANDOM_FOREST);
				 
				 //UNDERSAMPLING

				 SpreadSubsample filterus = Sampling.undersampling();
				 filterus.setInputFormat(train);
				 Instances trainundersampling = Filter.useFilter(train, filterus);
				 Instances testundersampling = Filter.useFilter(test, filterus);
				 
				 NaiveBayes undersamplingNbClass = new NaiveBayes();
			     Evaluation evalnbus = new Evaluation(trainundersampling);
			     undersamplingNbClass.buildClassifier(testundersampling);
			     evalnbus.evaluateModel(undersamplingNbClass, testundersampling);
			     evallistundersampling.add(evalnbus);
			     infoundersampling.add(trainandtest);
		  	     classifier.add(NAIVE_BAYES);
	
		  	     IBk undersamplingIBkClass = new IBk(); // vedere bene l'input di creazione 
		  	     undersamplingIBkClass.buildClassifier(trainundersampling);
	             Evaluation evalibkus = new Evaluation(testundersampling);
	             evalibkus.evaluateModel(undersamplingIBkClass, testundersampling);
	             evallistundersampling.add(evalibkus);
	             infoundersampling.add(trainandtest);
			     classifier.add("IBk");
			     
			     RandomForest undersamplingRFClass = new RandomForest();
			     undersamplingRFClass.buildClassifier(trainundersampling);
		         Evaluation evalrfus = new Evaluation(testundersampling);
		         evalrfus.evaluateModel(undersamplingRFClass, testundersampling);
		         evallistundersampling.add(evalrfus);
		         infoundersampling.add(trainandtest);
				 classifier.add(RANDOM_FOREST);
				 
				 //OVERSAMPLING
				 Resample filterostrain = Sampling.oversampling(train);
				 Resample filterostest = Sampling.oversampling(test);

				 filterostrain.setInputFormat(train);
				 filterostest.setInputFormat(test);
				 Instances trainoversampling = Filter.useFilter(train, filterostrain);
				 Instances testoversampling = Filter.useFilter(test, filterostest);
				 
				 NaiveBayes oversamplingNbClass = new NaiveBayes();
			     Evaluation evalnbos = new Evaluation(trainoversampling);
			     oversamplingNbClass.buildClassifier(testoversampling);
			     evalnbos.evaluateModel(oversamplingNbClass, testoversampling);
			     evallistoversampling.add(evalnbos);
			     infooversampling.add(trainandtest);
		  	     classifier.add(NAIVE_BAYES);
	
		  	     IBk oversamplingIBkClass = new IBk(); // vedere bene l'input di creazione 
		  	     oversamplingIBkClass.buildClassifier(trainoversampling);
	             Evaluation evalibkos = new Evaluation(testoversampling);
	             evalibkos.evaluateModel(oversamplingIBkClass, testoversampling);
	             evallistoversampling.add(evalibkos);
	             infooversampling.add(trainandtest);
			     classifier.add("IBk");
			     
			     RandomForest oversamplingRFClass = new RandomForest();
			     oversamplingRFClass.buildClassifier(trainoversampling);
		         Evaluation evalrfos = new Evaluation(testoversampling);
		         evalrfos.evaluateModel(oversamplingRFClass, testoversampling);
		         evallistoversampling.add(evalrfos);
		         infooversampling.add(trainandtest);
				 classifier.add(RANDOM_FOREST);
				 
				 //SMOTE
				 SMOTE filtersmotetrain = Sampling.smotefilter(train);
				 SMOTE filtersmotetest = Sampling.smotefilter(test);

				 filtersmotetrain.setInputFormat(train);
				 filtersmotetest.setInputFormat(test);
				 Instances trainsmote = Filter.useFilter(train, filtersmotetrain);
				 Instances testsmote = Filter.useFilter(test, filtersmotetest);
				 
				 NaiveBayes smoteNbClass = new NaiveBayes();
			     Evaluation evalnbsmote = new Evaluation(trainsmote);
			     smoteNbClass.buildClassifier(testsmote);
			     evalnbsmote.evaluateModel(smoteNbClass, testsmote);
			     evallistsmote.add(evalnbsmote);
			     infosmote.add(trainandtest);
		  	     classifier.add(NAIVE_BAYES);
	
		  	     IBk smoteIBkClass = new IBk(); // vedere bene l'input di creazione 
		  	     smoteIBkClass.buildClassifier(trainsmote);
	             Evaluation evalibksmote = new Evaluation(testsmote);
	             evalibksmote.evaluateModel(smoteIBkClass, testsmote);
	             evallistsmote.add(evalibksmote);
	             infosmote.add(trainandtest);
			     classifier.add("IBk");
			     
			     RandomForest smoteRFClass = new RandomForest();
			     smoteRFClass.buildClassifier(trainsmote);
		         Evaluation evalrfsmote = new Evaluation(testsmote);
		         evalrfsmote.evaluateModel(smoteRFClass, testsmote);
		         evallistsmote.add(evalrfsmote);
		         infosmote.add(trainandtest);
				 classifier.add(RANDOM_FOREST);
				 
				 //THRESHOLD
				 CostSensitiveClassifier cst = CostSensitiveClass.calculateSensitiveThresholdClassifier();	 
				 cst.setClassifier(nbClass);
				 Evaluation evalcst =  new Evaluation(test, cst.getCostMatrix());
				 cst.buildClassifier(train);
				 evalcst.evaluateModel(cst, test);
				 evallisttr.add(evalcst);
		         infotr.add(trainandtest);
		  	     classifier.add(NAIVE_BAYES);
		  	     
				 CostSensitiveClassifier cstibk = CostSensitiveClass.calculateSensitiveThresholdClassifier();
				 cstibk.setClassifier(ibkClass);
				 Evaluation evalcstibk =  new Evaluation(test, cstibk.getCostMatrix());
				 cstibk.buildClassifier(train);
				 evalcstibk.evaluateModel(cstibk, test);
				 evallisttr.add(evalcstibk);
		         infotr.add(trainandtest);
			     classifier.add("IBk");

				 CostSensitiveClassifier cstrf = CostSensitiveClass.calculateSensitiveThresholdClassifier();
				 cstrf.setClassifier(randomForestClass);
				 Evaluation evalcstrf =  new Evaluation(test, cstrf.getCostMatrix());
				 cstrf.buildClassifier(train);
				 evalcstrf.evaluateModel(cstrf, test);
				 evallisttr.add(evalcstrf);
		         infotr.add(trainandtest);
				 classifier.add(RANDOM_FOREST);

				 //SENSITIVE LEARNING
				 CostSensitiveClassifier csl = CostSensitiveClass.calculateSensitiveLearningClassifier();
				 csl.setClassifier(nbClass);
				 Evaluation evalcsl =  new Evaluation(test, csl.getCostMatrix());
				 csl.buildClassifier(train);
				 evalcsl.evaluateModel(csl, test);
				 evallistlr.add(evalcsl);
		         infolr.add(trainandtest);
		  	     classifier.add(NAIVE_BAYES);
		  	     
				 CostSensitiveClassifier cslibk = CostSensitiveClass.calculateSensitiveThresholdClassifier();
				 cslibk.setClassifier(ibkClass);
				 Evaluation evalcslibk =  new Evaluation(test, cslibk.getCostMatrix());
				 cslibk.buildClassifier(train);
				 evalcslibk.evaluateModel(cslibk, test);
				 evallistlr.add(evalcslibk);
		         infolr.add(trainandtest);
			     classifier.add("IBk");

				 CostSensitiveClassifier cslrf = CostSensitiveClass.calculateSensitiveThresholdClassifier();
				 cslrf.setClassifier(randomForestClass);
				 Evaluation evalcslrf =  new Evaluation(test, cslrf.getCostMatrix());
				 cslrf.buildClassifier(train);
				 evalcslrf.evaluateModel(cslrf, test);
				 evallistlr.add(evalcstrf);
		         infolr.add(trainandtest);
				 classifier.add(RANDOM_FOREST);
	    }
	    for(int i=0;i<evallist.size();i++) {  	
	    	ArrayList<Double> infoi = info.get(i);
	    	Evaluation eval = evallist.get(i);    	
	        JSONObject jo2 = new JSONObject();
	        jo2.put(DATASET, projName);
            jo2.put(TEST_RELEASE,  infoi.get(0));
            jo2.put(TRAINING_RELEASES,  infoi.get(1));
            jo2.put(TRAINING, infoi.get(2));
            jo2.put(TRAINING_DEFECTIVE, infoi.get(3));
            jo2.put(TEST_DEFECTIVE, infoi.get(4));
            jo2.put(CLASSIFIER2, classifier.get(i));
            jo2.put(BALANCING, false);
            jo2.put(FEATURE_SELECTION, false);
            jo2.put(SENSITIVITY, false);
            jo2.put(TP, eval.numTruePositives(1));
            jo2.put(FP, eval.numFalsePositives(1));
            jo2.put(TN, eval.numTrueNegatives(1));
            jo2.put(FN, eval.numFalseNegatives(1));
            jo2.put(TP_RATE, eval.truePositiveRate(1));
            jo2.put(FP_RATE, eval.falsePositiveRate(1));
            jo2.put(PRECISION,String.valueOf(eval.precision(1)));
            jo2.put(RECALL, eval.recall(1));
			jo2.put(F_MEASURE, String.valueOf(eval.fMeasure(1)));
			jo2.put(AUC, eval.areaUnderROC(1));
		    jo2.put(KAPPA, eval.kappa());
			jo2.put(ACCURACY, eval.pctCorrect()/100);
			js.put(jo2);
	    }
	    for(int i=0;i<evallistselected.size();i++) {  	
	    	
	    	ArrayList<Double> infoi = infoselected.get(i);
	    	Evaluation eval = evallistselected.get(i);    	
	        JSONObject jo2 = new JSONObject();
	        jo2.put(DATASET, projName);
            jo2.put(TEST_RELEASE,  infoi.get(0));
            jo2.put(TRAINING_RELEASES,  infoi.get(1));
            jo2.put(TRAINING, infoi.get(2));
            jo2.put(TRAINING_DEFECTIVE, infoi.get(3));
            jo2.put(TEST_DEFECTIVE, infoi.get(4));
            jo2.put(CLASSIFIER2, classifier.get(i));
            jo2.put(BALANCING, false);
            jo2.put(FEATURE_SELECTION, "BestFirst");
            jo2.put(SENSITIVITY, false);
            jo2.put(TP, eval.numTruePositives(1));
            jo2.put(FP, eval.numFalsePositives(1));
            jo2.put(TN, eval.numTrueNegatives(1));
            jo2.put(FN, eval.numFalseNegatives(1));
            jo2.put(TP_RATE, eval.truePositiveRate(1));
            jo2.put(FP_RATE, eval.falsePositiveRate(1));
            jo2.put(PRECISION,String.valueOf(eval.precision(1)));
            jo2.put(RECALL, eval.recall(1));
			jo2.put(F_MEASURE, String.valueOf(eval.fMeasure(1)));
			jo2.put(AUC, eval.areaUnderROC(1));
		    jo2.put(KAPPA, eval.kappa());
			jo2.put(ACCURACY, eval.pctCorrect()/100);
			js.put(jo2);
	    }
      for(int i=0;i<evallistundersampling.size();i++) {  	
	    	ArrayList<Double> infoi = infoundersampling.get(i);
	    	Evaluation eval = evallistundersampling.get(i);    	
	        JSONObject jo2 = new JSONObject();
	        jo2.put(DATASET, projName);
            jo2.put(TEST_RELEASE,  infoi.get(0));
            jo2.put(TRAINING_RELEASES,  infoi.get(1));
            jo2.put(TRAINING, infoi.get(2));
            jo2.put(TRAINING_DEFECTIVE, infoi.get(3));
            jo2.put(TEST_DEFECTIVE, infoi.get(4));
            jo2.put(CLASSIFIER2, classifier.get(i));
            jo2.put(BALANCING, "UnderSampling");
            jo2.put(FEATURE_SELECTION, false);
            jo2.put(SENSITIVITY, false);
            jo2.put(TP, eval.numTruePositives(1));
            jo2.put(FP, eval.numFalsePositives(1));
            jo2.put(TN, eval.numTrueNegatives(1));
            jo2.put(FN, eval.numFalseNegatives(1));
            jo2.put(TP_RATE, eval.truePositiveRate(1));
            jo2.put(FP_RATE, eval.falsePositiveRate(1));
            jo2.put(PRECISION,String.valueOf(eval.precision(1)));
            jo2.put(RECALL, eval.recall(1));
			jo2.put(F_MEASURE, String.valueOf(eval.fMeasure(1)));
			jo2.put(AUC, eval.areaUnderROC(1));
		    jo2.put(KAPPA, eval.kappa());
			jo2.put(ACCURACY, eval.pctCorrect()/100);
			js.put(jo2);
	    }
      for(int i=0;i<evallistoversampling.size();i++) {  	
	    	ArrayList<Double> infoi = infooversampling.get(i);
	    	Evaluation eval = evallistoversampling.get(i);    	
	        JSONObject jo2 = new JSONObject();
	        jo2.put(DATASET, projName);
            jo2.put(TEST_RELEASE,  infoi.get(0));
            jo2.put(TRAINING_RELEASES,  infoi.get(1));
            jo2.put(TRAINING, infoi.get(2));
            jo2.put(TRAINING_DEFECTIVE, infoi.get(3));
            jo2.put(TEST_DEFECTIVE, infoi.get(4));
            jo2.put(CLASSIFIER2, classifier.get(i));
            jo2.put(BALANCING, "OverSampling");
            jo2.put(FEATURE_SELECTION, false);
            jo2.put(SENSITIVITY, false);
            jo2.put(TP, eval.numTruePositives(1));
            jo2.put(FP, eval.numFalsePositives(1));
            jo2.put(TN, eval.numTrueNegatives(1));
            jo2.put(FN, eval.numFalseNegatives(1));
            jo2.put(TP_RATE, eval.truePositiveRate(1));
            jo2.put(FP_RATE, eval.falsePositiveRate(1));
            jo2.put(PRECISION,String.valueOf(eval.precision(1)));
            jo2.put(RECALL, eval.recall(1));
			jo2.put(F_MEASURE, String.valueOf(eval.fMeasure(1)));
			jo2.put(AUC, eval.areaUnderROC(1));
		    jo2.put(KAPPA, eval.kappa());
			jo2.put(ACCURACY, eval.pctCorrect()/100);
			js.put(jo2);
	    }      
      for(int i=0;i<evallistsmote.size();i++) {  	
	      ArrayList<Double> infoi = infooversampling.get(i);
	  	  Evaluation eval = evallistoversampling.get(i);    	
	      JSONObject jo2 = new JSONObject();
          jo2.put(DATASET, projName);
          jo2.put(TEST_RELEASE,  infoi.get(0));
          jo2.put(TRAINING_RELEASES,  infoi.get(1));
          jo2.put(TRAINING, infoi.get(2));
          jo2.put(TRAINING_DEFECTIVE, infoi.get(3));
          jo2.put(TEST_DEFECTIVE, infoi.get(4));
          jo2.put(CLASSIFIER2, classifier.get(i));
          jo2.put(BALANCING, "SMOTE");
          jo2.put(FEATURE_SELECTION, false);
          jo2.put(SENSITIVITY, false);
          jo2.put(TP, eval.numTruePositives(1));
          jo2.put(FP, eval.numFalsePositives(1));
          jo2.put(TN, eval.numTrueNegatives(1));
          jo2.put(FN, eval.numFalseNegatives(1));
          jo2.put(TP_RATE, eval.truePositiveRate(1));
          jo2.put(FP_RATE, eval.falsePositiveRate(1));
          jo2.put(PRECISION,String.valueOf(eval.precision(1)));
          jo2.put(RECALL, eval.recall(1));
		  jo2.put(F_MEASURE, String.valueOf(eval.fMeasure(1)));
		  jo2.put(AUC, eval.areaUnderROC(1));
		  jo2.put(KAPPA, eval.kappa());
		  jo2.put(ACCURACY, eval.pctCorrect()/100);
	      js.put(jo2);
	    }      
      for(int i=0;i<evallisttr.size();i++) {  	
	      ArrayList<Double> infoi = infotr.get(i);
	  	  Evaluation eval = evallisttr.get(i);    	
	      JSONObject jo2 = new JSONObject();
          jo2.put(DATASET, projName);
          jo2.put(TEST_RELEASE,  infoi.get(0));
          jo2.put(TRAINING_RELEASES,  infoi.get(1));
          jo2.put(TRAINING, infoi.get(2));
          jo2.put(TRAINING_DEFECTIVE, infoi.get(3));
          jo2.put(TEST_DEFECTIVE, infoi.get(4));
          jo2.put(CLASSIFIER2, classifier.get(i));
          jo2.put(BALANCING, false);
          jo2.put(FEATURE_SELECTION, false);
          jo2.put(SENSITIVITY, "SensitiveThreshold");
          jo2.put(TP, eval.numTruePositives(1));
          jo2.put(FP, eval.numFalsePositives(1));
          jo2.put(TN, eval.numTrueNegatives(1));
          jo2.put(FN, eval.numFalseNegatives(1));
          jo2.put(TP_RATE, eval.truePositiveRate(1));
          jo2.put(FP_RATE, eval.falsePositiveRate(1));
          jo2.put(PRECISION,String.valueOf(eval.precision(1)));
          jo2.put(RECALL, eval.recall(1));
		  jo2.put(F_MEASURE, String.valueOf(eval.fMeasure(1)));
		  jo2.put(AUC, eval.areaUnderROC(1));
		  jo2.put(KAPPA, eval.kappa());
		  jo2.put(ACCURACY, eval.pctCorrect()/100);
	      js.put(jo2);
	    }     
      for(int i=0;i<evallisttr.size();i++) {  	
	      ArrayList<Double> infoi = infotr.get(i);
	  	  Evaluation eval = evallisttr.get(i);    	
	      JSONObject jo2 = new JSONObject();
          jo2.put(DATASET, projName);
          jo2.put(TEST_RELEASE,  infoi.get(0));
          jo2.put(TRAINING_RELEASES,  infoi.get(1));
          jo2.put(TRAINING, infoi.get(2));
          jo2.put(TRAINING_DEFECTIVE, infoi.get(3));
          jo2.put(TEST_DEFECTIVE, infoi.get(4));
          jo2.put(CLASSIFIER2, classifier.get(i));
          jo2.put(BALANCING, false);
          jo2.put(FEATURE_SELECTION, false);
          jo2.put(SENSITIVITY, "SensitiveThreshold");
          jo2.put(TP, eval.numTruePositives(1));
          jo2.put(FP, eval.numFalsePositives(1));
          jo2.put(TN, eval.numTrueNegatives(1));
          jo2.put(FN, eval.numFalseNegatives(1));
          jo2.put(TP_RATE, eval.truePositiveRate(1));
          jo2.put(FP_RATE, eval.falsePositiveRate(1));
          jo2.put(PRECISION,String.valueOf(eval.precision(1)));
          jo2.put(RECALL, eval.recall(1));
		  jo2.put(F_MEASURE, String.valueOf(eval.fMeasure(1)));
		  jo2.put(AUC, eval.areaUnderROC(1));
		  jo2.put(KAPPA, eval.kappa());
		  jo2.put(ACCURACY, eval.pctCorrect()/100);
	      js.put(jo2);
	    }
      for(int i=0;i<evallistlr.size();i++) {  
	      ArrayList<Double> infoi = infolr.get(i);
	  	  Evaluation eval = evallistlr.get(i);    	
	      JSONObject jo2 = new JSONObject();
          jo2.put(DATASET, projName);
          jo2.put(TEST_RELEASE, infoi.get(0));
          jo2.put(TRAINING_RELEASES, infoi.get(1));
          jo2.put(TRAINING, infoi.get(2));
          jo2.put(TRAINING_DEFECTIVE, infoi.get(3));
          jo2.put(TEST_DEFECTIVE, infoi.get(4));
          jo2.put(CLASSIFIER2, classifier.get(i));
          jo2.put(BALANCING, false);
          jo2.put(FEATURE_SELECTION, false);
          jo2.put(SENSITIVITY, "SensitiveLearning");
          jo2.put(TP, eval.numTruePositives(1));
          jo2.put(FP, eval.numFalsePositives(1));
          jo2.put(TN, eval.numTrueNegatives(1));
          jo2.put(FN, eval.numFalseNegatives(1));
          jo2.put(TP_RATE, eval.truePositiveRate(1));
          jo2.put(FP_RATE, eval.falsePositiveRate(1));
          jo2.put(PRECISION,String.valueOf(eval.precision(1)));
          jo2.put(RECALL, eval.recall(1));
		  jo2.put(F_MEASURE, String.valueOf(eval.fMeasure(1)));
		  jo2.put(AUC, eval.areaUnderROC(1));
		  jo2.put(KAPPA, eval.kappa());
		  jo2.put(ACCURACY, eval.pctCorrect()/100);
	      js.put(jo2);
	    }   
		json2csv(js, projName);
	}
	catch(Exception ex) {
		java.util.logging.Logger.getLogger("WekaEvaluator").log(Level.INFO, "Exception", ex);
	}
	
     }
	
	@SuppressWarnings("deprecation")
	public static  void json2csv(JSONArray array,String projname) throws IOException {         
    	File file=new File("Weka_Analyzer"+projname+".csv");
        String csv = CDL.toString(array);
        FileUtils.writeStringToFile(file, csv);
    }
	
	public static double divide(double trainInstances,double trainPlusTestInstances) {
		double d=0f;
		if(trainPlusTestInstances!=0) {
			d = trainInstances/trainPlusTestInstances;
			return d;
		}
		return d;
		
	}
	
	public static void main(String[] args)  {
		String proj2finale= System.getProperty(USER_DIR)+"\\src\\main\\resources\\BOOKKEEPER.arff";
    	evaluatorandmetric(walkforward(proj2finale, PROJNAME2), PROJNAME2);
    	String proj1final= System.getProperty(USER_DIR)+"\\src\\main\\resources\\OPENJPA.arff";
    	evaluatorandmetric(walkforward(proj1final, PROJNAME1), PROJNAME1);

	}
}
