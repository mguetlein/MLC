import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import mulan.classifier.MultiLabelLearner;
import mulan.classifier.lazy.MLkNN;
import mulan.classifier.meta.HOMER;
import mulan.classifier.meta.HierarchyBuilder;
import mulan.classifier.transformation.BinaryRelevance;
import mulan.classifier.transformation.EnsembleOfClassifierChains;
import mulan.classifier.transformation.MultiLabelStacking;
import mulan.data.MultiLabelInstances;
import mulan.evaluation.MissingCapableEvaluator;
import mulan.evaluation.MultipleEvaluation;
import mulan.evaluation.SinglePredictionTracker;
import mulan.evaluation.SinglePredictionTrackerUtil;
import mulan.evaluation.loss.HammingLoss;
import mulan.evaluation.measure.MacroAUC;
import mulan.evaluation.measure.MacroAccuracy;
import mulan.evaluation.measure.MacroFMeasure;
import mulan.evaluation.measure.MicroAUC;
import mulan.evaluation.measure.MicroAccuracy;
import mulan.evaluation.measure.MicroFMeasure;
import mulan.evaluation.measure.SubsetAccuracy;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;

import util.ArrayUtil;
import util.ParallelHandler;
import util.StringUtil;
import weka.classifiers.Classifier;
import weka.classifiers.functions.SMO;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
import datamining.ResultSet;
import datamining.ResultSetIO;

public class RunMLC
{
	MLCDataInfo data;
	ParallelHandler parallel;

	//	private String endpointFile;
	//	private String featureFile;
	private int numCores;
	private String classifier = "SMO";
	private String mlcAlgorithm = "ECC";
	private int numFolds = 10;
	private int minSeed = 0;
	private int maxSeedExclusive = 3;
	private String mlcAlgorithmParams;
	private String datasetName;
	private String experimentName;
	private String imputation = "false";

	//	private int numEndpoints;
	//	private int numMissingAllowed;

	public RunMLC()
	{
	}

	private MultiLabelLearner getMLCAlgorithms(String mlcAlgorithmStr, String mlcAlgorithmParamsStr,
			Classifier classifier)
	{
		HashMap<String, String> mlcParamHash = new HashMap<String, String>();
		if (mlcAlgorithmParamsStr != null && mlcAlgorithmParamsStr.length() > 0)
		{
			for (String keyValue : mlcAlgorithmParamsStr.split(";"))
			{
				if (StringUtil.numOccurences(keyValue, "=") != 1)
					throw new IllegalArgumentException();
				int index = keyValue.indexOf("=");
				String key = keyValue.substring(0, index);
				String value = keyValue.substring(index + 1);
				mlcParamHash.put(key, value);
			}
		}
		if (mlcAlgorithmStr.equals("BR"))
		{
			if (mlcParamHash.size() > 0)
				throw new IllegalArgumentException();
			return new BinaryRelevance(classifier);
		}
		else if (mlcAlgorithmStr.equals("ECC"))
		{
			int numChains = 10;
			boolean confidences = false;
			boolean replacement = false;

			if (mlcParamHash.size() > 0)
			{
				for (String keys : mlcParamHash.keySet())
				{
					if (keys.equals("num-chains"))
						numChains = Integer.parseInt(mlcParamHash.get(keys));
					else if (keys.equals("confidences"))
						confidences = Boolean.parseBoolean(mlcParamHash.get(keys));
					else if (keys.equals("replacement"))
						replacement = Boolean.parseBoolean(mlcParamHash.get(keys));
					else
						throw new IllegalArgumentException("no param for ecc: '" + keys + "'");
				}
			}
			return new EnsembleOfClassifierChains(classifier, numChains, confidences, replacement);
		}
		else if (mlcAlgorithmStr.equals("MLkNN"))
		{
			int numNeighbors = 10;
			if (mlcParamHash.size() > 0)
			{
				for (String keys : mlcParamHash.keySet())
				{
					if (keys.equals("num-neighbors"))
						numNeighbors = Integer.parseInt(mlcParamHash.get(keys));
					else
						throw new IllegalArgumentException();
				}
			}
			return new MLkNN(numNeighbors, 1.0);
		}
		else if (mlcAlgorithmStr.equals("MLS"))
		{
			return new MultiLabelStacking(classifier, classifier);
		}
		else if (mlcAlgorithmStr.equals("HOMER"))
		{
			int numClusters = 3;
			MultiLabelLearner method = new EnsembleOfClassifierChains(classifier, 10, false, false);
			if (mlcParamHash.size() > 0)
			{
				for (String keys : mlcParamHash.keySet())
				{
					if (keys.equals("num-clusters"))
						numClusters = Integer.parseInt(mlcParamHash.get(keys));
					else if (keys.equals("method"))
						method = getMLCAlgorithms(mlcParamHash.get(keys), null, classifier);
					else
						throw new IllegalArgumentException("illegal param for HOMER: '" + keys + "'");
				}
			}
			return new HOMER(method, numClusters, HierarchyBuilder.Method.BalancedClustering);
		}
		else
			throw new Error("unknown mlc algorithm: " + mlcAlgorithmStr);
	}

	public void eval() throws Exception
	{
		if (numCores > 1)
			parallel = new ParallelHandler(numCores);
		final ResultSet res = new ResultSet();
		final File resFile = new File("tmp/" + experimentName + "_" + datasetName.replace(",", "_") + ".results");
		List<SinglePredictionTracker> trackers = new ArrayList<SinglePredictionTracker>();

		for (final String datasetNameStr : datasetName.split(","))
		{
			final MultiLabelInstances dataset = new MultiLabelInstances("tmp/" + datasetNameStr + ".arff", "tmp/"
					+ datasetNameStr + ".xml");
			int numRepetitions = (maxSeedExclusive - minSeed) * (StringUtil.numOccurences(classifier, ",") + 1)
					* (StringUtil.numOccurences(mlcAlgorithm, ",") + 1)
					* (StringUtil.numOccurences(imputation, ",") + 1);
			final SinglePredictionTracker tracker = new SinglePredictionTracker(datasetNameStr, experimentName,
					dataset, numRepetitions);
			trackers.add(tracker);

			final MLCDataInfo di = MLCDataInfo.get(dataset);
			di.print();

			for (final String classifierString : classifier.split(","))
			{
				final Classifier classifier;
				if (classifierString.equals("SMO"))
					classifier = new SMO();
				else if (classifierString.equals("RandomForest"))
					classifier = new RandomForest();
				else if (classifierString.equals("IBk"))
					classifier = new IBk(1);
				else
					throw new Error("WTF");

				String mlcAlgs[] = mlcAlgorithm.split(",");
				String mlcParams[] = null;
				if (mlcAlgorithmParams != null)
				{
					mlcParams = mlcAlgorithmParams.split(",");
					if (mlcParams.length != mlcAlgs.length)
						throw new IllegalArgumentException("num mlc-algorithms " + mlcAlgs.length
								+ " != num mlc-algorithm-params " + mlcParams.length);
				}
				else
					mlcParams = new String[mlcAlgs.length];

				for (final String imputationString : imputation.split(","))
				{
					if (ArrayUtil.indexOf(new String[] { "true", "false", "random" }, imputationString) == -1)
						throw new Error("WTF");

					for (int mlcAlgorithmIndex = 0; mlcAlgorithmIndex < mlcAlgs.length; mlcAlgorithmIndex++)
					{
						final String mlcAlgorithmStr = mlcAlgs[mlcAlgorithmIndex];
						final String mlcAlgorithmParamsStr;
						if (mlcParams[mlcAlgorithmIndex] != null && mlcParams[mlcAlgorithmIndex].equals("default"))
							mlcAlgorithmParamsStr = null;
						else
							mlcAlgorithmParamsStr = mlcParams[mlcAlgorithmIndex];
						final MultiLabelLearner mlcAlgorithm = getMLCAlgorithms(mlcAlgorithmStr, mlcAlgorithmParamsStr,
								classifier);

						for (int s = minSeed; s < maxSeedExclusive; s++)
						{
							final int seed = s;
							Runnable r = new Runnable()
							{
								@Override
								public void run()
								{
									System.out.println(datasetNameStr + " seed:" + seed + " imputation:"
											+ imputationString + " wekaAlg:" + classifier.getClass().getSimpleName()
											+ " mlcAlg:" + mlcAlgorithm.getClass().getSimpleName());

									long start = System.currentTimeMillis();

									MultiLabelInstances data = dataset;

									//								System.err.println("filling");
									//								EnsembleOfClassifierChainsFiller filler = new EnsembleOfClassifierChainsFiller(
									//										new SMO(), 10);
									//								data = filler.fillMissing(data);
									//								System.err.println("filling - done");

									//mulan.evaluation.Evaluator eval = new mulan.evaluation.Evaluator();
									MissingCapableEvaluator eval = new MissingCapableEvaluator();
									if (imputationString.equals("true"))
										eval.setImputationLearner(mlcAlgorithm);
									if (imputationString.equals("random"))
										eval.setImputationAtRandom(new Random());
									eval.setSinglePredictionTracker(tracker);
									eval.setSeed(seed);
									MultipleEvaluation ev = eval.crossValidate(mlcAlgorithm, data, numFolds);
									//				ev.calculateStatistics();

									synchronized (res)
									{
										for (int fold = 0; fold < numFolds; fold++)
										{
											int resCount = res.addResult();
											res.setResultValue(resCount, "dataset-name", datasetNameStr);
											res.setResultValue(resCount, "endpoint-file", di.endpointFile);
											res.setResultValue(resCount, "feature-file", di.featureFile);
											res.setResultValue(resCount, "num-endpoints", di.numEndpoints);
											res.setResultValue(resCount, "num-missing-allowed", di.numMissingAllowed);
											res.setResultValue(resCount, "discretization-level", di.discretizationLevel);
											res.setResultValue(resCount, "include-v", di.includeV);
											res.setResultValue(resCount, "runtime", System.currentTimeMillis() - start);

											res.setResultValue(resCount, "imputation", imputationString);
											res.setResultValue(resCount, "classifier", classifierString);
											res.setResultValue(resCount, "mlc-algorithm", mlcAlgorithmStr);
											res.setResultValue(resCount, "mlc-algorithm-params", mlcAlgorithmParamsStr);

											res.setResultValue(resCount, "cv-seed", seed);
											res.setResultValue(resCount, "num-folds", numFolds);
											res.setResultValue(resCount, "fold", fold);

											res.setResultValue(resCount, "num-compounds", dataset.getNumInstances());
											res.setResultValue(resCount, "num-labels", dataset.getNumLabels());
											for (int i = 0; i < dataset.getNumLabels(); i++)
												res.setResultValue(resCount, "label#" + i, dataset.getDataSet()
														.attribute(dataset.getLabelIndices()[i]).name());

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

											res.setResultValue(resCount, "macro-accuracy", ev.getResult(
													new MacroAccuracy(dataset.getNumLabels()).getName(), fold));

											res.setResultValue(resCount, "weighted-macro-accuracy", ev.getResult(
													new MacroAccuracy(dataset.getNumLabels(), true).getName(), fold));

											for (int i = 0; i < dataset.getNumLabels(); i++)
												res.setResultValue(resCount, "macro-accuracy#" + i, ev.getResult(
														new MacroAccuracy(dataset.getNumLabels()).getName(), fold, i));

											res.setResultValue(resCount, "micro-accuracy", ev.getResult(
													new MicroAccuracy(dataset.getNumLabels()).getName(), fold));

											res.setResultValue(resCount, "micro-f-measure", ev.getResult(
													new MicroFMeasure(dataset.getNumLabels()).getName(), fold));

											res.setResultValue(resCount, "macro-f-measure", ev.getResult(
													new MacroFMeasure(dataset.getNumLabels()).getName(), fold));

											res.setResultValue(resCount, "weighted-macro-f-measure", ev.getResult(
													new MacroFMeasure(dataset.getNumLabels(), true).getName(), fold));

											for (int i = 0; i < dataset.getNumLabels(); i++)
												res.setResultValue(resCount, "macro-f-measure#" + i, ev.getResult(
														new MacroFMeasure(dataset.getNumLabels()).getName(), fold, i));

											res.setResultValue(resCount, "micro-auc",
													ev.getResult(new MicroAUC(dataset.getNumLabels()).getName(), fold));

											res.setResultValue(resCount, "macro-auc",
													ev.getResult(new MacroAUC(dataset.getNumLabels()).getName(), fold));

											res.setResultValue(resCount, "weighted-macro-auc", ev.getResult(
													new MacroAUC(dataset.getNumLabels(), true).getName(), fold));

											for (int i = 0; i < dataset.getNumLabels(); i++)
												res.setResultValue(resCount, "macro-auc#" + i, ev.getResult(
														new MacroAUC(dataset.getNumLabels()).getName(), fold, i));
										}
										System.out.println("\nprinting " + res.getNumResults() + " results to "
												+ resFile);
										//System.out.println(res.toNiceString());
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
		}

		if (parallel != null)
			parallel.waitForAll();

		for (SinglePredictionTracker tracker : trackers)
		{
			//			tracker.print();
			//			System.out.println(SinglePredictionTrackerUtil.toCsv(tracker));
			SinglePredictionTrackerUtil.attachToCsv(tracker);
		}
	}

	public static void main(String args[]) throws Exception
	{
		//		String a = "";
		//		String p = "";
		//		for (int numClusters : new int[] { 3, 6, 9 })
		//		{
		//			for (String method : new String[] { "BR", "ECC", "MLkNN" })
		//			{
		//				a += "HOMER,";
		//				p += "num-clusters=" + numClusters + ";method=" + method + ",";
		//			}
		//		}
		//		a = a.substring(0, a.length() - 1);
		//		p = p.substring(0, p.length() - 1);
		//		System.out.println("-x 1 -f 10 -i 0 -u 1 -a " + a + " -p " + p + " IBk -r tmp/input2013-03-18_16-37-02");
		//		if (true == true)
		//			System.exit(0);
		//		String a = "";
		//		String p = "";
		//		for (int numChains : new int[] { 5, 10, 15 })
		//		{
		//			for (boolean confidences : new boolean[] { true, false })
		//			{
		//				for (boolean replacement : new boolean[] { true, false })
		//				{
		//					a += "ECC,";
		//					p += "num-chains=" + numChains + ";confidences=" + confidences + ";replacement=" + replacement
		//							+ ",";
		//				}
		//			}
		//		}
		//		a = a.substring(0, a.length() - 1);
		//		p = p.substring(0, p.length() - 1);
		//		System.out.println("-a " + a + " -p " + p);
		//		if (true == true)
		//			System.exit(0);
		//		String a = "";
		//		String p = "";
		//		for (int numNeighbors : new int[] { 5, 10, 15 })
		//		{
		//			a += "MLkNN,";
		//			p += "num-neighbors=" + numNeighbors + ",";
		//		}
		//		a = a.substring(0, a.length() - 1);
		//		p = p.substring(0, p.length() - 1);
		//		System.out.println("-a " + a + " -p " + p);
		//		if (true == true)
		//			System.exit(0);

		if (args != null && args.length > 0 && args[0].equals("report"))
		{
			ReportMLC.main(ArrayUtil.removeAt(String.class, args, 0));
			return;
		}

		if (args != null && args.length == 1 && args[0].equals("debug"))
		{
			args = ("-x 1 -f 3 -i 0 -u 1 -t false -a BR -c IBk -d dataAsmall-F1-V -e test").split(" ");
			//args = ("-x 1 -f 10 -i 0 -u 1 -t false -a BR,MLkNN -p \"default,num-neighbors=2\" -c IBk -d dataA-F1 -e test")
			//		.split(" ");
		}

		if (args == null || args.length < 6)
			throw new Exception("params missing");

		Options options = new Options();
		options.addOption("x", "num-cores", true, "Number of cores");
		options.addOption("d", "dataset-name", true, "Data file, requires .arff, .xml, and .csv file");
		options.addOption("i", "min-cv-seed", true, "Min seed for cv");
		options.addOption("u", "max-cv-seed-exclusive", true, "Max seed for cv, exclusive");
		options.addOption("a", "mlc-algorithm", true, "MLC algortihm");
		options.addOption("p", "mlc-algorithm-params", true, "MLC algortihm params");
		options.addOption("f", "num-folds", true, "Num folds for cv");
		options.addOption("c", "classifier", true, "Classifier, default:SMO");
		options.addOption("e", "experiment-name", true, "Experiment name");
		options.addOption("t", "imputation", true, "Enable imputation");
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, args);

		RunMLC run = new RunMLC();
		if (cmd.hasOption("x"))
			run.numCores = Integer.parseInt(cmd.getOptionValue("x"));
		if (cmd.hasOption("d"))
			run.datasetName = cmd.getOptionValue("d");
		if (cmd.hasOption("i"))
			run.minSeed = Integer.parseInt(cmd.getOptionValue("i"));
		if (cmd.hasOption("u"))
			run.maxSeedExclusive = Integer.parseInt(cmd.getOptionValue("u"));
		if (cmd.hasOption("f"))
			run.numFolds = Integer.parseInt(cmd.getOptionValue("f"));
		if (cmd.hasOption("a"))
			run.mlcAlgorithm = cmd.getOptionValue("a");
		if (cmd.hasOption("p"))
			run.mlcAlgorithmParams = cmd.getOptionValue("p");
		if (cmd.hasOption("c"))
			run.classifier = cmd.getOptionValue("c");
		if (cmd.hasOption("e"))
			run.experimentName = cmd.getOptionValue("e");
		if (cmd.hasOption("t"))
			run.imputation = cmd.getOptionValue("t");

		if (run.experimentName == null)
			throw new IllegalArgumentException("experiment-name missing");

		run.eval();
		System.exit(0);
	}
}
