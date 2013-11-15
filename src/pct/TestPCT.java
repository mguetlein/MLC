package pct;

import java.util.ArrayList;
import java.util.List;

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

			//			String last = null;

			List<String> results = new ArrayList<String>();

			for (int j = 0; j < 1; j++)
			{
				EnsembleMethod m = EnsembleMethod.RForest;
				//for (EnsembleMethod m : EnsembleMethod.values())
				{
					PruningMethod p = PruningMethod.None;
					//PruningMethod p = PruningMethod.C4_5;
					//for (PruningMethod p : new PruningMethod[] { PruningMethod.None, PruningMethod.C4_5 })
					{
						//					for (Heuristic h : Heuristic.values())
						Heuristic h = Heuristic.VarianceReduction;
						{
							Integer minNum = null;
							//							for (Integer minNum : new Integer[] { null, 0, 1, 2, 3 })
							{
								//Double fTest = null;
								for (Double fTest : new Double[] { null, 0.0, 0.001, 0.005, 0.01, 0.05, 0.1, 0.125, 1.0 })
								{

									System.out.println(h + " " + p + " " + m + " " + minNum + " " + fTest);
									long time = System.currentTimeMillis();

									MultiLabelLearner learner = new PredictiveClusteringTrees(h, p, m, minNum, fTest);

									//learner = learner.makeCopy();

									learner.build(dataset);

									//									System.out.println(learner);
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

									if (results.indexOf(current) != -1)
										System.err.println("EQUAL RESULT!!! as  run " + results.indexOf(current));
									results.add(current);

									System.out.println(((System.currentTimeMillis() - time) / 1000.0) + "s\n\n");
								}
							}
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
