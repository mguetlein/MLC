package pct;

import java.util.ArrayList;
import java.util.List;

import mulan.classifier.NeighborMultiLabelOutput;
import mulan.data.MultiLabelInstances;
import pct.PredictiveClusteringTrees.EnsembleMethod;
import pct.PredictiveClusteringTrees.Heuristic;
import pct.PredictiveClusteringTrees.PruningMethod;
import util.ArrayUtil;
import weka.core.Instance;
import weka.core.Instances;

public class TestPCT
{
	public static void main(String args[])
	{
		try
		{
			//			Settings.VERBOSE = 0;

			MultiLabelInstances trainingDataset;
			//			MultiLabelInstances testDataset;
			Instances testData;
			{
				//				MultiLabelInstances dataset = new MultiLabelInstances(
				//						"/home/martin/workspace/BMBF-MLC/arff/dataEAgg_noV_Ca15-20c20_FP1.arff",
				//						"/home/martin/workspace/BMBF-MLC/arff/dataEAgg_noV_Ca15-20c20_FP1.xml");
				//				MultiLabelInstances dataset = new MultiLabelInstances(
				//						"/home/martin/workspace/BMBF-MLC/arff/dataEAgg_noV_Ca15-20c20_MAN2.arff",
				//						"/home/martin/workspace/BMBF-MLC/arff/dataEAgg_noV_Ca15-20c20_MAN2.xml");
				MultiLabelInstances dataset = new MultiLabelInstances(
						"/home/martin/workspace/BMBF-MLC/filled/dataEAgg_noV_Ca15-20c20_MAN2_filledClass.arff",
						"/home/martin/workspace/BMBF-MLC/filled/dataEAgg_noV_Ca15-20c20_MAN2_filledClass.xml");

				//				//single cv split
				//				Instances workingSet = new Instances(dataset.getDataSet());
				//				int c = 0;
				//				for (Instance instance : workingSet)
				//					instance.setWeight(c++);
				//				Random r = new Random(1);
				//				workingSet.randomize(r);
				//				Instances train = workingSet.trainCV(10, 1);
				//				for (Instance instance : train)
				//					instance.setWeight(1.0);
				//				Instances test = workingSet.testCV(10, 1);
				//				trainingDataset = new MultiLabelInstances(train, dataset.getLabelsMetaData());
				//				testData = new MultiLabelInstances(test, dataset.getLabelsMetaData()).getDataSet();

				//newly created testData
				//				trainingDataset = dataset;
				//				testData = new ArffReader(new FileReader(
				//						"./predictions/75a9bfd8e5fbc442cbc9b811364d8d2b_dataEAgg_noV_Ca15-20c20_FP1.arff")).getData();

				trainingDataset = dataset;
				testData = null;//dataset.getDataSet();
			}

			//			MultiLabelInstances dataset = new MultiLabelInstances(
			//					"/home/martin/workspace/BMBF-MLC/filled/dataEAgg_noV_Ca15-20c20_FP1_filledClass.arff",
			//					"/home/martin/workspace/BMBF-MLC/filled/dataEAgg_noV_Ca15-20c20_FP1_filledClass.xml");

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
				EnsembleMethod m = EnsembleMethod.None;
				//for (EnsembleMethod m : EnsembleMethod.values())
				{
					PruningMethod p = PruningMethod.None;
					//PruningMethod p = PruningMethod.C4_5;
					//for (PruningMethod p : new PruningMethod[] { PruningMethod.None, PruningMethod.C4_5 })
					{
						//					for (Heuristic h : Heuristic.values())
						Heuristic h = Heuristic.VarianceReduction;
						{
							Integer minNum = 3;
							//Integer minNum = null;
							//							for (Integer minNum : new Integer[] { null, 0, 1, 2, 3 })
							{
								//								Double fTest = null;
								Double fTest = 0.125;
								//for (Double fTest : new Double[] { null, 0.0, 1.0 })
								//								for (Double fTest : new Double[] { 0.0, 0.001, 0.005, 0.01, 0.025, 0.05, 0.075, 0.1,
								//										0.125, 0.15, 0.175, 0.2, 0.25, 0.3, 0.4, 0.5, 1.0 })
								//for (Double fTest : new Double[] { 0.025, 0.05, 0.075, 0.1, 0.125, 0.15, 0.175 })
								{

									//									System.out.println(h + " " + p + " " + m + " " + minNum + " " + fTest);
									long time = System.currentTimeMillis();

									PredictiveClusteringTrees learner = new PredictiveClusteringTrees(h, p, m, minNum,
											fTest);
									//									learner = learner.makeCopy();

									learner.build(trainingDataset);

									learner.listCategories();

									//									learner = learner.makeCopy();
									//									if (learner instanceof PredictiveClusteringTrees)
									//										((PredictiveClusteringTrees) learner).prepareSerialize();
									//									File tmp = File.createTempFile("mlc", "model");
									//									SerializationHelper.write(tmp.getAbsolutePath(), learner);
									//									learner = (MultiLabelLearner) SerializationHelper.read(tmp.getAbsolutePath());
									//									tmp.delete();

									System.out.println("f-Test " + fTest);
									System.out.println(learner);

									System.out.println(PredictiveClusteringTrees.modelProps.toString());

									//System.out.println(fTest+","+learner.);

									//							learner = learner.makeCopy();
									//
									//							learner.build(dataset2);
									//
									//							learner = learner.makeCopy();

									//			for(int k=0;k<dataset.getDataSet().size();k++){
									//				MultiLabelOutput out = learner.makePrediction(dataset.getDataSet().get(k));
									//				System.out.println("predictions");

									//									System.out.println(((PredictiveClusteringTrees) learner).getNeighbors());

									if (testData != null)
									{
										String current = "";
										for (int i = 0; i < Math.min(testData.numInstances(), 5); i++)
										{
											Instance inst = testData.get(i);

											//								for (int l = 0; l < dataset.getNumLabels(); l++)
											//									inst.setValue(dataset.getLabelIndices()[l], j + "");

											((PredictiveClusteringTrees) learner).setComputeNeighbors(true);
											NeighborMultiLabelOutput out = (NeighborMultiLabelOutput) learner
													.makePrediction(inst);

											System.out
													.println(i + " " + ArrayUtil.toString(out.getNeighborInstances()));

											for (int l = 0; l < trainingDataset.getNumLabels(); l++)
											{
												//						String endpointName = dataset.getDataSet().attribute(dataset.getLabelIndices()[l]).name();
												//					System.out.println(endpointName + " " + out.getBipartition()[l] + " " + out.getConfidences()[l]);
												current += out.getConfidences()[l] + " ";
											}
											current += "\n";
										}
										//									System.out.println(current);

										//									if (results.indexOf(current) != -1)
										//										System.err.println("EQUAL RESULT!!! as  run " + results.indexOf(current));
										results.add(current);
									}

									//									System.out.println(((System.currentTimeMillis() - time) / 1000.0) + "s\n\n");
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
