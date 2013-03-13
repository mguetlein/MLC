import java.io.File;

import mulan.classifier.MultiLabelLearner;
import mulan.classifier.transformation.BinaryRelevance;
import mulan.classifier.transformation.EnsembleOfClassifierChains;
import mulan.data.MultiLabelInstances;
import mulan.evaluation.MultipleEvaluation;
import mulan.evaluation.loss.HammingLoss;
import mulan.evaluation.measure.MacroAccuracy;
import mulan.evaluation.measure.MicroAccuracy;
import mulan.evaluation.measure.SubsetAccuracy;
import util.ParallelHandler;
import weka.classifiers.Classifier;
import weka.classifiers.functions.SMO;
import weka.classifiers.trees.RandomForest;

import com.sun.org.apache.xalan.internal.xsltc.cmdline.getopt.GetOpt;

import datamining.ResultSet;
import datamining.ResultSetIO;
import de.tum.in.mulan.evaluation.MissingCapableEvaluator;

public class RunMLC
{

	MLCData.DatasetInfo data;
	ParallelHandler parallel;

	//	private String endpointFile;
	//	private String featureFile;
	private int numCores;
	private String arffFile = "tmp/input.arff";
	private String resultFile = "tmp/results";
	private String wekaClassifier = "SMO";
	private String mlcAlgorithm = "ECC";
	private int minSeed = 0;
	private int maxSeedExclusive = 3;

	//	private int numEndpoints;
	//	private int numMissingAllowed;

	public RunMLC()
	{
	}

	public void eval() throws Exception
	{
		if (numCores > 1)
			parallel = new ParallelHandler(numCores);

		String xmlFile = arffFile.replace(".arff", ".xml");
		final MultiLabelInstances dataset = new MultiLabelInstances(arffFile, xmlFile);

		final MLCData.DatasetInfo di = new MLCData.DatasetInfo(dataset);
		di.print();

		final ResultSet res = new ResultSet();
		final File resFile = new File(resultFile);

		for (final String classifierString : wekaClassifier.split(","))
		{
			final Classifier classifier;
			if (classifierString.equals("SMO"))
				classifier = new SMO();
			else if (classifierString.equals("RandomForest"))
				classifier = new RandomForest();
			else
				throw new Error("WTF");

			for (final String mlcAlgorithmStr : mlcAlgorithm.split(","))
			{
				final MultiLabelLearner mlcAlgorithm;
				if (mlcAlgorithmStr.equals("BR"))
					mlcAlgorithm = new BinaryRelevance(classifier);
				else if (mlcAlgorithmStr.equals("ECC"))
					mlcAlgorithm = new EnsembleOfClassifierChains(classifier, 10, false, false);
				else
					throw new Error("WTF");

				for (int s = minSeed; s < maxSeedExclusive; s++)
				{
					final int seed = s;
					Runnable r = new Runnable()
					{
						@Override
						public void run()
						{
							System.out.println();
							System.out.println(classifier.getClass().getSimpleName());
							System.out.println(mlcAlgorithm.getClass().getSimpleName());
							System.out.println();

							long start = System.currentTimeMillis();

							//mulan.evaluation.Evaluator eval = new mulan.evaluation.Evaluator();
							MissingCapableEvaluator eval = new MissingCapableEvaluator();
							eval.setSeed(seed);
							int numFolds = 3;
							MultipleEvaluation ev = eval.crossValidate(mlcAlgorithm, dataset, numFolds);
							//				ev.calculateStatistics();

							synchronized (res)
							{
								for (int fold = 0; fold < numFolds; fold++)
								{
									int resCount = res.addResult();
									res.setResultValue(resCount, "endpoint-file", di.endpointFile);
									res.setResultValue(resCount, "feature-file", di.featureFile);
									res.setResultValue(resCount, "num-endpoints", di.numEndpoints);
									res.setResultValue(resCount, "num-missing-allowed", di.numMissingAllowed);
									//									res.setResultValue(resCount, "arff-file", arffFile);
									res.setResultValue(resCount, "runtime", System.currentTimeMillis() - start);

									res.setResultValue(resCount, "classifier", classifierString);
									res.setResultValue(resCount, "mlc-algorithm", mlcAlgorithmStr);

									res.setResultValue(resCount, "cv-seed", seed);
									res.setResultValue(resCount, "num-folds", numFolds);
									res.setResultValue(resCount, "fold", fold);

									res.setResultValue(resCount, "num-compounds", dataset.getNumInstances());
									res.setResultValue(resCount, "num-labels", dataset.getNumLabels());
									for (int i = 0; i < dataset.getNumLabels(); i++)
										res.setResultValue(resCount, "label#" + i,
												dataset.getDataSet().attribute(dataset.getLabelIndices()[i]).name());

									res.setResultValue(resCount, "cardinality", dataset.getCardinality());

									res.setResultValue(resCount, "num-predictions", ev.getData(fold).getNumInstances());

									res.setResultValue(resCount, "hamming-loss",
											ev.getResult(new HammingLoss().getName(), fold));
									res.setResultValue(resCount, "subset-accuracy",
											ev.getResult(new SubsetAccuracy().getName(), fold));
									//res.setResultValue(resCount, "accuracy", ev.getMean(new ExampleBasedAccuracy().getName()));
									//res.setResultValue(resCount, "precision", ev.getMean(new ExampleBasedPrecision().getName()));
									//res.setResultValue(resCount, "recall", ev.getMean(new ExampleBasedRecall().getName()));

									res.setResultValue(resCount, "macro-accuracy",
											ev.getResult(new MacroAccuracy(dataset.getNumLabels()).getName(), fold));

									for (int i = 0; i < dataset.getNumLabels(); i++)
										res.setResultValue(resCount, "macro-accuracy#" + i, ev.getResult(
												new MacroAccuracy(dataset.getNumLabels()).getName(), fold, i));

									res.setResultValue(resCount, "micro-accuracy",
											ev.getResult(new MicroAccuracy(dataset.getNumLabels()).getName(), fold));
								}
								System.out.println("\nprinting " + res.getNumResults() + " to " + resFile);
								System.out.println(res.toNiceString());
								ResultSetIO.printToFile(resFile, res, true);
							}
						}
					};
					if (parallel != null)
						parallel.addJob(r);
					else
						r.run();
				}
			}
		}

		if (parallel != null)
			parallel.waitForAll();
	}

	public static void main(String args[]) throws Exception
	{
		//args = "-x 1 -i 0 -u 3 -a ECC,BR".split(" ");

		if (args == null || args.length < 6)
			throw new Exception("params missing");
		RunMLC run = new RunMLC();
		//GetOpt opt = new GetOpt(args, "n:e:f:x:r:o:c:a:i:u:m:");
		GetOpt opt = new GetOpt(args, "x:r:c:a:i:u:");
		int o = -1;
		while ((o = opt.getNextOption()) != -1)
		{
			//			if (o == 'e')
			//				run.endpointFile = opt.getOptionArg();
			//			else if (o == 'f')
			//				run.featureFile = opt.getOptionArg();
			if (o == 'x')
				run.numCores = Integer.parseInt(opt.getOptionArg());
			//else if (o == 'r')
			//	run.arffFile = opt.getOptionArg();
			//			else if (o == 'o')
			//				run.resultFile = opt.getOptionArg();
			else if (o == 'c')
				run.wekaClassifier = opt.getOptionArg();
			else if (o == 'a')
				run.mlcAlgorithm = opt.getOptionArg();
			else if (o == 'i')
				run.minSeed = Integer.parseInt(opt.getOptionArg());
			else if (o == 'u')
				run.maxSeedExclusive = Integer.parseInt(opt.getOptionArg());
			//			else if (o == 'n')
			//				run.numEndpoints = Integer.parseInt(opt.getOptionArg());
			//			else if (o == 'm')
			//				run.numMissingAllowed = Integer.parseInt(opt.getOptionArg());
		}
		run.eval();
		System.exit(0);
	}

}
