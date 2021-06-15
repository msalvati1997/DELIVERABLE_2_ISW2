package weka;
import java.util.logging.Level;

import weka.core.AttributeStats;
import weka.core.Instances;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SpreadSubsample;
import weka.filters.supervised.instance.SMOTE;


class Sampling {

	private Sampling() {
		super();
	}

	//undersampling of the majority class
	public static SpreadSubsample undersampling() {
		final SpreadSubsample filter = new SpreadSubsample();
		filter.setDistributionSpread(1.0);	
		return filter;
	}
	
	//oversampling of the minority class
	public static Resample oversampling(Instances data) {
		
		int numberOfBuggy= returnNumOfBuggyClass(data);
		double samplesizemajority = computeSampleSizePercentMajorityClass(data,numberOfBuggy);
		if(samplesizemajority<=0) {
			samplesizemajority = computeSampleSizePercentMajorityClass(data,returnNumOfNotBuggyClass(data));
		}
		final Resample filter = new Resample();
		filter.setBiasToUniformClass(1.0);		
		try {
			filter.setInputFormat(data);
			filter.setNoReplacement(false);
			filter.setSampleSizePercent(samplesizemajority);
		} catch (Exception e) {
			java.util.logging.Logger.getLogger("Sampling").log(Level.WARNING, "Exception", e);
		}
		return filter;
	}
	
	//oversampling di SMOTE
	public static weka.filters.supervised.instance.SMOTE smotefilter(Instances data) {
		final SMOTE filter = new SMOTE();
		int numberOfBuggy= returnNumOfBuggyClass(data);
		double samplesizesmote = computeSampleSizeSmote(data,numberOfBuggy);
		if(samplesizesmote<=0) {
			samplesizesmote = computeSampleSizePercentMajorityClass(data,returnNumOfNotBuggyClass(data));
		}
		try {
			filter.setInputFormat(data);
			filter.setPercentage(samplesizesmote);
		} catch (Exception e) {
			java.util.logging.Logger.getLogger("Sampling").log(Level.WARNING, "Exception", e);

		}
		return filter;
	}
	
	public static int returnNumOfBuggyClass(Instances data) {
		AttributeStats stat = data.attributeStats(13);
		return stat.nominalCounts[1];
	}
	public static int returnNumOfNotBuggyClass(Instances data) {
		AttributeStats stat = data.attributeStats(13);
		return stat.nominalCounts[0];
	}
	
	public static double computeSampleSizePercentMajorityClass(Instances data, int numOfFirstClass) {
		int numOfSecondClass = data.numInstances() - numOfFirstClass;
		return 100 * ((double)(data.numInstances() + 
				Math.abs(numOfFirstClass - numOfSecondClass)) 
				/ data.numInstances());
	}
	public static double computeSampleSizeSmote(Instances data, int numOfFirstClass) {
		int numOfSecondClass = data.numInstances() - numOfFirstClass;
		return Math.round(100 * ((double)(numOfSecondClass-numOfFirstClass)/numOfFirstClass));
	}
	

}
