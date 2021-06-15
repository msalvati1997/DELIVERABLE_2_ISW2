package weka;

import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.filters.supervised.attribute.AttributeSelection;

public class FeaturesSelection {
	


	private FeaturesSelection() {
		super();
	}

	/**
	 * Selezionare attributi utilizzando Best first per ridurre 
	 * il numero di parametri di instanze del dataset
	
	 * @param data input set of instances
	 * @return resampled set of instances
	 */
	
	public static AttributeSelection selectAttributes()
	{
		final AttributeSelection filter = new AttributeSelection();
		final CfsSubsetEval evaluator = new CfsSubsetEval();
		filter.setEvaluator(evaluator);
		final BestFirst search = new BestFirst();
		filter.setSearch(search);
		
		return filter;

	}
	
}
