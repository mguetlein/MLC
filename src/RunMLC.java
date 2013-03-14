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

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;

import util.ParallelHandler;
import weka.classifiers.Classifier;
import weka.classifiers.functions.SMO;
import weka.classifiers.trees.RandomForest;
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
	private String arffFile;
	private String resultFile = "tmp/results";
	private String wekaClassifier = "SMO";
	private String mlcAlgorithm = "ECC";
	private int numFolds = 10;
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
		final ResultSet res = new ResultSet();
		final File resFile = new File(resultFile);

		for (final String arffFileStr : arffFile.split(","))
		{
			String xmlFile = arffFileStr.replace(".arff", ".xml");
			final MultiLabelInstances dataset = new MultiLabelInstances(arffFileStr, xmlFile);

			final MLCData.DatasetInfo di = new MLCData.DatasetInfo(dataset);
			di.print();

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
								System.out.println(arffFileStr);
								System.out.println(classifier.getClass().getSimpleName());
								System.out.println(mlcAlgorithm.getClass().getSimpleName());
								System.out.println();

								long start = System.currentTimeMillis();

								//mulan.evaluation.Evaluator eval = new mulan.evaluation.Evaluator();
								MissingCapableEvaluator eval = new MissingCapableEvaluator();
								eval.setSeed(seed);
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
										res.setResultValue(resCount, "arff-file", arffFileStr);
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

										res.setResultValue(resCount, "num-predictions", ev.getData(fold)
												.getNumInstances());

										res.setResultValue(resCount, "hamming-loss",
												ev.getResult(new HammingLoss().getName(), fold));
										res.setResultValue(resCount, "1-hamming-loss",
												1 - ev.getResult(new HammingLoss().getName(), fold));

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
		}

		if (parallel != null)
			parallel.waitForAll();
	}

	public static void main(String args[]) throws Exception
	{
		//		args = "-x 1 -f 2 -i 0 -u 1 -a BR -r tmp/input2013-03-13_10-11-46.arff,tmp/input2013-03-13_16-14-29.arff"
		//				.split(" ");

		if (args == null || args.length < 6)
			throw new Exception("params missing");

		Options options = new Options();
		options.addOption("x", "num-cores", true, "Number of cores");
		options.addOption("r", "arff-file", true, "Arff file");
		options.addOption("i", "min-cv-seed", true, "Min seed for cv");
		options.addOption("u", "max-cv-seed-exclusive", true, "Max seed for cv, exclusive");
		options.addOption("a", "mlc-algorithm", true, "MLC algortihm");
		options.addOption("f", "num-folds", true, "Num folds for cv");
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, args);

		RunMLC run = new RunMLC();
		if (cmd.hasOption("x"))
			run.numCores = Integer.parseInt(cmd.getOptionValue("x"));
		if (cmd.hasOption("r"))
			run.arffFile = cmd.getOptionValue("r");
		if (cmd.hasOption("i"))
			run.minSeed = Integer.parseInt(cmd.getOptionValue("i"));
		if (cmd.hasOption("u"))
			run.maxSeedExclusive = Integer.parseInt(cmd.getOptionValue("u"));
		if (cmd.hasOption("f"))
			run.numFolds = Integer.parseInt(cmd.getOptionValue("f"));
		if (cmd.hasOption("a"))
			run.mlcAlgorithm = cmd.getOptionValue("a");

		run.eval();
		System.exit(0);
	}

}
