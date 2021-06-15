package weka;

import weka.classifiers.CostMatrix;
import weka.classifiers.meta.CostSensitiveClassifier;

public class CostSensitiveClass {
	static int cfp = 1;
	static int cfn = 10*cfp;
	
	private CostSensitiveClass() {
		super();
	}

	
	public static CostMatrix createCostMatrix(double weightFalsePositive, double weightFalseNegative) {
	    CostMatrix costMatrix = new CostMatrix(2);
	    costMatrix.setCell(0, 0, 0.0);
	    costMatrix.setCell(1, 0, weightFalsePositive);
	    costMatrix.setCell(0, 1, weightFalseNegative);
	    costMatrix.setCell(1, 1, 0.0);
	    return costMatrix;
	}
	public static CostSensitiveClassifier calculateSensitiveThresholdClassifier() {
		CostSensitiveClassifier costSensitive = new CostSensitiveClassifier();
		costSensitive.setCostMatrix(createCostMatrix(cfp, cfn));
		costSensitive.setMinimizeExpectedCost(true);
		return costSensitive;	
	}
	public static CostSensitiveClassifier calculateSensitiveLearningClassifier() {
		CostSensitiveClassifier costSensitive = new CostSensitiveClassifier();
		costSensitive.setCostMatrix(createCostMatrix(cfp, cfn));
		costSensitive.setMinimizeExpectedCost(false);
		return costSensitive;
	}
}
