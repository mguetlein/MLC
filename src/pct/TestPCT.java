package pct;

import mulan.classifier.MultiLabelLearner;
import mulan.classifier.MultiLabelOutput;
import mulan.data.MultiLabelInstances;
import pct.PredictiveClusteringTrees.EnsembleMethod;
import pct.PredictiveClusteringTrees.Heuristic;
import pct.PredictiveClusteringTrees.PruningMethod;
import weka.core.Instance;

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

			//			MultiLabelInstances dataset = new MultiLabelInstances("/home/martin/workspace/PCT/arff/test.arff",
			//					"/home/martin/workspace/PCT/arff/test.xml");

			//			MultiLabelInstances dataset2 = new MultiLabelInstances(
			//					"/home/martin/workspace/BMBF-MLC/arff/dataC_noV_Ca15-20c20_PC.arff",
			//					"/home/martin/workspace/BMBF-MLC/arff/dataC_noV_Ca15-20c20_PC.xml");
			//			int numLabels = dataset.getNumLabels();
			//			int numAttributes = dataset.getFeatureAttributes().size();
			//			System.out.println(numLabels+", "+numAttributes);

			String last = null;

			for (int j = 0; j < 2; j++)
			{
				for (EnsembleMethod m : new EnsembleMethod[] { EnsembleMethod.None })
				//for (EnsembleMethod m : EnsembleMethod.values())
				{
					PruningMethod p = PruningMethod.None;
					//					for (PruningMethod p : PruningMethod.values())
					//					for (PruningMethod p : new PruningMethod[] { PruningMethod.None, PruningMethod.C4_5 })
					{
						//					for (Heuristic h : Heuristic.values())
						Heuristic h = Heuristic.VarianceReduction;
						{

							System.out.println(h + " " + p + " " + m);
							long time = System.currentTimeMillis();

							MultiLabelLearner learner = new PredictiveClusteringTrees(h, p, m);

							//learner = learner.makeCopy();

							learner.build(dataset);

							//							learner = learner.makeCopy();
							//
							//							learner.build(dataset2);
							//
							//							learner = learner.makeCopy();

							//			for(int k=0;k<dataset.getDataSet().size();k++){
							//				MultiLabelOutput out = learner.makePrediction(dataset.getDataSet().get(k));
							//				System.out.println("predictions");

							String current = "";
							for (int i = 0; i < 6; i++)
							{
								Instance inst = dataset.getDataSet().get(i);

								//								for (int l = 0; l < dataset.getNumLabels(); l++)
								//									inst.setValue(dataset.getLabelIndices()[l], j + "");

								MultiLabelOutput out = learner.makePrediction(inst);
								for (int l = 0; l < dataset.getNumLabels(); l++)
								{
									//						String endpointName = dataset.getDataSet().attribute(dataset.getLabelIndices()[l]).name();
									//					System.out.println(endpointName + " " + out.getBipartition()[l] + " " + out.getConfidences()[l]);
									current += out.getConfidences()[l] + " ";
								}
								current += "\n";
							}
							System.out.println(current);

							if (last == null)
								last = current;
							else if (last.equals(current))
								throw new Error("equal!");

							System.out.println(((System.currentTimeMillis() - time) / 1000.0) + "s");
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
