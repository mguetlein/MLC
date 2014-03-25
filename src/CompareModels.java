import pct.PredictiveClusteringTrees;

public class CompareModels
{
	public static void main(String args[]) throws Exception
	{
		String impu = "true";
		//for (String impu : new String[] { "true", "false" })
		{
			for (Double fTest : new Double[] { 0.125 })
			{
				//		Double fTest[] = new Double[] { 0.0, 0.001, 0.005, 0.01, 0.05, 0.1, 0.125, 0.15, 0.2, 0.25, 0.3, 1.0 })

				RunMLC mlc = new RunMLC();
				mlc.setNumCores(1);
				mlc.setDatasetName("dataEAgg_noV_Ca15-20c20_FP1");
				mlc.setMinSeed(0);
				mlc.setMaxSeedExclusive(1);
				mlc.setNumFolds(10);
				mlc.setMlcAlgorithm("PCT");
				mlc.setMlcAlgorithmParams("pruning=None;ensemble=None;min-num=3;ftest=" + fTest);
				//			mlc.setClassifier(cmd.getOptionValue("c"));
				mlc.setExperimentName("dummy");
				mlc.setImputation(impu);
				mlc.setFillMissing("false");
				mlc.setAppDomain("None");
				//			mlc.setAppDomainParams(cmd.getOptionValue("w"));

				mlc.setPureModelBuilding(true);

				mlc.buildModel(null);
				PredictiveClusteringTrees.modelProps.setResultValue(
						PredictiveClusteringTrees.modelProps.getNumResults() - 1, "imputation", impu);
			}
		}
		System.err.flush();
		System.out.println(PredictiveClusteringTrees.modelProps.toNiceString());
		System.out.println(PredictiveClusteringTrees.modelProps.toString());
	}
}
