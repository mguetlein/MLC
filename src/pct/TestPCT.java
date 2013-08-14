package pct;

import mulan.classifier.MultiLabelLearner;
import mulan.classifier.MultiLabelOutput;
import mulan.data.MultiLabelInstances;

public class TestPCT
{
	public static void main(String args[])
	{
		try
		{
			//			Settings.VERBOSE = 0;

			MultiLabelInstances dataset = new MultiLabelInstances(
					"/home/martin/workspace/PCT/arff/dataB_noV_Cl68_FP.arff",
					"/home/martin/workspace/PCT/arff/dataB_noV_Cl68_FP.xml");
			//			int numLabels = dataset.getNumLabels();
			//			int numAttributes = dataset.getFeatureAttributes().size();
			//			System.out.println(numLabels+", "+numAttributes);

			MultiLabelLearner learner = new PredictiveClusteringTrees();

			//learner = learner.makeCopy();

			learner.build(dataset);

			learner = learner.makeCopy();

			MultiLabelOutput out = learner.makePrediction(dataset.getDataSet().get(0));

			//			for(int k=0;k<dataset.getDataSet().size();k++){
			//				MultiLabelOutput out = learner.makePrediction(dataset.getDataSet().get(k));
			System.out.println("predictions");
			for (int l = 0; l < dataset.getNumLabels(); l++)
			{
				String endpointName = dataset.getDataSet().attribute(dataset.getLabelIndices()[l]).name();
				System.out.println(endpointName + " " + out.getBipartition()[l] + " " + out.getConfidences()[l]);
			}

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

}
