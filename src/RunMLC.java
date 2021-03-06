import imputation.Imputation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import mlc.ClusterEndpoint;
import mlc.ConfusionMatrix;
import mlc.MLCDataInfo;
import mlc.ModelInfo;
import mlc.reporting.MultiValidationReport;
import mlc.reporting.PredictCompounds;
import mlc.reporting.ReportMLC;
import mlc.reporting.ValidationReport;
import mulan.classifier.MultiLabelLearner;
import mulan.classifier.MultiLabelOutput;
import mulan.classifier.lazy.MLkNN;
import mulan.classifier.meta.HOMER;
import mulan.classifier.meta.HierarchyBuilder;
import mulan.classifier.transformation.BinaryRelevance;
import mulan.classifier.transformation.EnsembleOfClassifierChains;
import mulan.classifier.transformation.MultiLabelStacking;
import mulan.data.MultiLabelInstances;
import mulan.evaluation.MissingCapableEvaluator;
import mulan.evaluation.MultipleEvaluation;
import mulan.evaluation.Settings;
import mulan.evaluation.SinglePredictionTracker;
import mulan.evaluation.SinglePredictionTrackerUtil;
import mulan.evaluation.measure.ConfidenceLevel;
import mulan.evaluation.measure.ConfidenceLevelProvider;
import mulan.evaluation.measure.HammingLoss;
import mulan.evaluation.measure.MacroAUC;
import mulan.evaluation.measure.MacroAccuracy;
import mulan.evaluation.measure.MacroFMeasure;
import mulan.evaluation.measure.MacroMCC;
import mulan.evaluation.measure.MacroNegativePredictiveValue;
import mulan.evaluation.measure.MacroPrecision;
import mulan.evaluation.measure.MacroRecall;
import mulan.evaluation.measure.MacroSpecificity;
import mulan.evaluation.measure.MicroAccuracy;
import mulan.evaluation.measure.MicroFMeasure;
import mulan.evaluation.measure.MicroMCC;
import mulan.evaluation.measure.MicroRecall;
import mulan.evaluation.measure.MicroSpecificity;
import mulan.evaluation.measure.SubsetAccuracy;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;

import pct.Categories;
import pct.PredictiveClusteringTrees;
import util.ArrayUtil;
import util.FileUtil;
import util.FileUtil.CSVFile;
import util.ParallelHandler;
import util.StringUtil;
import weka.ArffWriter;
import weka.FilledArffWritable;
import weka.classifiers.Classifier;
import weka.classifiers.functions.SMO;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.SerializationHelper;
import appDomain.ADVisualization;
import appDomain.ApplicabilityDomain;
import appDomain.CentroidBasedApplicabilityDomain;
import appDomain.DefaultMLCApplicabilityDomain;
import appDomain.DistanceBasedApplicabilityDomain;
import appDomain.NeighborDistanceBasedApplicabilityDomain;
import datamining.ResultSet;
import datamining.ResultSetIO;

public class RunMLC extends MLCOptions
{
	MLCDataInfo data;
	ParallelHandler parallel;

	static boolean DEBUG = false;

	public RunMLC()
	{
	}

	private ApplicabilityDomain getAppDomain(String appDomainStr, String appDomainParamsStr)
	{
		HashMap<String, String> params = new HashMap<String, String>();
		if (appDomainParamsStr != null)
		{
			for (String keyValue : appDomainParamsStr.split(";"))
			{
				if (StringUtil.numOccurences(keyValue, "=") != 1)
					throw new IllegalArgumentException();
				int index = keyValue.indexOf("=");
				String key = keyValue.substring(0, index);
				String value = keyValue.substring(index + 1);
				params.put(key, value);
			}
		}
		if (appDomainStr.equals("None"))
			return null;

		DistanceBasedApplicabilityDomain ad;
		if (appDomainStr.equals("Centroid"))
		{
			ad = new CentroidBasedApplicabilityDomain();
		}
		else if (appDomainStr.equals("Neighbor"))
		{
			ad = new NeighborDistanceBasedApplicabilityDomain();
			if (params.containsKey("neighbors"))
			{
				((NeighborDistanceBasedApplicabilityDomain) ad).setNumNeighbors(Integer.parseInt(params
						.get("neighbors")));
				params.remove("neighbors");
			}
		}
		else
			throw new IllegalArgumentException("unknown app-domain: " + appDomainStr
					+ " available: None Centroid Neighbor");
		for (String key : params.keySet())
		{
			if (key.equals("method"))
				ad.setMethod(CentroidBasedApplicabilityDomain.Method.valueOf(params.get(key)));
			else if (key.equals("distance"))
				ad.setDistanceMultiplier(Double.parseDouble(params.get(key)));
			else if (key.equals("confidence"))
				ad.setAdjustConfidence(Boolean.parseBoolean(params.get(key)));
			else if (key.equals("full"))
				ad.setFullDistanceMultiplier(Double.parseDouble(params.get(key)));
			else
				throw new IllegalArgumentException("not a app-domain param: " + key);
		}
		return ad;
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
		else if (mlcAlgorithmStr.equals("PCT"))
		{
			PredictiveClusteringTrees.Heuristic heuristic = PredictiveClusteringTrees.Heuristic.VarianceReduction;
			PredictiveClusteringTrees.PruningMethod pruningMethod = PredictiveClusteringTrees.PruningMethod.C4_5;
			PredictiveClusteringTrees.EnsembleMethod ensembleMethod = PredictiveClusteringTrees.EnsembleMethod.None;
			Integer minimalNumberExamples = null;
			Double fTest = null;

			if (mlcParamHash.size() > 0)
			{
				for (String keys : mlcParamHash.keySet())
				{
					if (keys.equals("heuristic"))
						heuristic = PredictiveClusteringTrees.Heuristic.valueOf(mlcParamHash.get(keys));
					else if (keys.equals("pruning"))
						pruningMethod = PredictiveClusteringTrees.PruningMethod.valOf(mlcParamHash.get(keys));
					else if (keys.equals("ensemble"))
						ensembleMethod = PredictiveClusteringTrees.EnsembleMethod.valueOf(mlcParamHash.get(keys));
					else if (keys.equals("min-num"))
						minimalNumberExamples = Integer.parseInt(mlcParamHash.get(keys));
					else if (keys.equals("ftest"))
						fTest = Double.parseDouble(mlcParamHash.get(keys));
					else
						throw new IllegalArgumentException("illegal param for PCT: '" + keys + "'");
				}
			}
			return new PredictiveClusteringTrees(heuristic, pruningMethod, ensembleMethod, minimalNumberExamples, fTest);
		}
		else
			throw new Error("unknown mlc algorithm: " + mlcAlgorithmStr);
	}

	public void iterate(boolean storeResults, final MLCMethod method) throws Exception
	{
		if (getExperimentName() == null)
			throw new IllegalArgumentException("experiment-name missing");

		if (getNumCores() > 1)
			parallel = new ParallelHandler(getNumCores());

		final ResultSet res;
		final File resFile;
		List<SinglePredictionTracker> trackers;
		if (storeResults)
		{
			res = new ResultSet();
			resFile = new File(Settings.resultFile(getExperimentName(), getDatasetNames()));
			trackers = new ArrayList<SinglePredictionTracker>();
		}
		else
		{
			res = null;
			resFile = null;
			trackers = null;
		}

		final int numRepetitions = (getMaxSeedExclusive() - getMinSeed()) * getClassifiers().length
				* getMlcAlgorithms().length * getImputations().length * getAppDomains().length;

		for (final String datasetNameStr : getDatasetNames())
		{
			final MultiLabelInstances dataset = new MultiLabelInstances(Settings.arffFile(datasetNameStr),
					Settings.xmlFile(datasetNameStr));

			final SinglePredictionTracker tracker;
			if (storeResults)
			{
				tracker = new SinglePredictionTracker(datasetNameStr, getExperimentName(), dataset, numRepetitions);
				trackers.add(tracker);
			}
			else
			{
				if (getDatasetNames().length > 1 || numRepetitions > 1)
					throw new Error("no repetetions for no valdidation");
				tracker = null;
			}

			final MLCDataInfo di = MLCDataInfo.get(dataset);
			di.print();

			for (final String classifierString : getClassifiers())
			{
				final Classifier classifier;
				if (classifierString.equals("None"))
					classifier = null;
				else if (classifierString.equals("SMO"))
					classifier = new SMO();
				else if (classifierString.equals("RandomForest"))
					classifier = new RandomForest();
				else if (classifierString.equals("IBk"))
					classifier = new IBk(1);
				else
					throw new Error("WTF");

				for (int mlcAlgIdx = 0; mlcAlgIdx < getMlcAlgorithms().length; mlcAlgIdx++)
				{
					final String mlcAlgorithmStr = getMlcAlgorithms()[mlcAlgIdx];
					if (getMlcAlgorithmParams().length > 0
							&& getMlcAlgorithmParams().length != getMlcAlgorithms().length)
						throw new IllegalArgumentException("num mlc-algorithms " + getMlcAlgorithms().length
								+ " != num mlc-algorithm-params " + getMlcAlgorithmParams().length);
					final String mlcAlgorithmParamsStr;
					if (getMlcAlgorithmParams().length == 0 || getMlcAlgorithmParams()[mlcAlgIdx].equals("default"))
						mlcAlgorithmParamsStr = null;
					else
						mlcAlgorithmParamsStr = getMlcAlgorithmParams()[mlcAlgIdx];
					final MultiLabelLearner mlcAlgorithm = getMLCAlgorithms(mlcAlgorithmStr, mlcAlgorithmParamsStr,
							classifier);

					for (int adIdx = 0; adIdx < getAppDomains().length; adIdx++)
					{
						final String appDomainStr = getAppDomains()[adIdx];
						if (getAppDomainParams().length > 0 && getAppDomainParams().length != getAppDomains().length)
							throw new IllegalArgumentException("num app-domain " + getAppDomains().length
									+ " != num app-domain-params " + getAppDomainParams().length);
						final String appDomainParamsStr;
						if (getAppDomainParams().length == 0 || getAppDomainParams()[adIdx].equals("default"))
							appDomainParamsStr = null;
						else
							appDomainParamsStr = getAppDomainParams()[adIdx];
						final ApplicabilityDomain appDomain = getAppDomain(appDomainStr, appDomainParamsStr);

						for (int s = getMinSeed(); s < getMaxSeedExclusive(); s++)
						{
							final int seed = s;

							for (final String imputationString : getImputations())
							{
								if (ArrayUtil.indexOf(new String[] { "true", "false", "random" }, imputationString) == -1)
									throw new Error("WTF");

								Runnable r = new Runnable()
								{
									@Override
									public void run()
									{
										String classifierName = classifier == null ? "None" : classifier.getClass()
												.getSimpleName();
										String info = datasetNameStr + " sd:" + seed + " impt:" + imputationString
												+ " weka:" + classifierName + " mlc:"
												+ mlcAlgorithm.getClass().getSimpleName() + " mlcP:"
												+ mlcAlgorithmParamsStr + " ad:" + appDomainStr + " adP:"
												+ appDomainParamsStr;
										System.out.println(info);

										try
										{
											method.runMLC(datasetNameStr, dataset, di, mlcAlgorithmStr, mlcAlgorithm,
													imputationString, classifierString, mlcAlgorithmParamsStr,
													appDomain, appDomainStr, appDomainParamsStr, seed, res, resFile,
													tracker, numRepetitions * getNumFolds() * getDatasetNames().length,
													info);
										}
										catch (Exception e)
										{
											e.printStackTrace();
											System.exit(1);
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
		}

		if (parallel != null)
			parallel.waitForAll();

		PredictiveClusteringTrees.clear();

		if (trackers != null)
			for (SinglePredictionTracker tracker : trackers)
				SinglePredictionTrackerUtil.attachToCsv(tracker);
	}

	interface MLCMethod
	{
		public void runMLC(String datasetNameStr, MultiLabelInstances dataset, MLCDataInfo di, String mlcAlgorithmStr,
				MultiLabelLearner mlcAlgorithm, String imputationString, String classifierString,
				String mlcAlgorithmParamsStr, ApplicabilityDomain appDomain, String appDomainStr,
				String appDomainParamsStr, int seed, ResultSet res, File resFile, SinglePredictionTracker tracker,
				int numExperimentFolds, String info) throws Exception;
	}

	public void validate() throws Exception
	{
		if (getMaxSeedExclusive() - getMinSeed() <= 0)
			throw new IllegalArgumentException("max-cv-seed-exclusive has be set to >=1 (-u)");

		iterate(true, new MLCMethod()
		{
			@Override
			public void runMLC(String datasetNameStr, MultiLabelInstances dataset, MLCDataInfo di,
					String mlcAlgorithmStr, MultiLabelLearner mlcAlgorithm, String imputationString,
					String classifierString, String mlcAlgorithmParamsStr, ApplicabilityDomain appDomain,
					String appDomainStr, String appDomainParamsStr, int seed, ResultSet res, File resFile,
					SinglePredictionTracker tracker, int numExperimentFolds, String info)
			{
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
					eval.setImputationLearner(new EnsembleOfClassifierChains(new RandomForest(), 15, true, false));
				if (imputationString.equals("random"))
					eval.setImputationAtRandom(new Random());
				eval.setSinglePredictionTracker(tracker);
				eval.setSeed(seed);

				if (appDomain != null)
					eval.setApplicabilityDomain(new DefaultMLCApplicabilityDomain(
							(DistanceBasedApplicabilityDomain) appDomain));
				eval.setInfo(info);

				if (DEBUG)
					System.out.println("starting evalution ...");
				MultipleEvaluation ev = eval.crossValidate(mlcAlgorithm, data, getNumFolds());
				if (DEBUG)
					System.out.println("... evalution done");

				//				ev.calculateStatistics();

				List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
				for (int fold = 0; fold < getNumFolds(); fold++)
				{
					Map<String, Object> result = new LinkedHashMap<String, Object>();
					result.put("dataset-name", datasetNameStr);
					result.put("endpoint-file", di.endpointFile);
					result.put("feature-file", di.featureFile);
					result.put("num-endpoints", di.numEndpoints);
					result.put("num-missing-allowed", di.numMissingAllowed);
					result.put("discretization-level", di.discretizationLevel);
					result.put("include-v", di.includeV);
					result.put("runtime", System.currentTimeMillis() - start);

					result.put("imputation", imputationString);
					result.put("classifier", classifierString);
					result.put("mlc-algorithm", mlcAlgorithmStr);
					result.put("mlc-algorithm-params", mlcAlgorithmParamsStr);

					result.put("app-domain", appDomainStr);
					result.put("app-domain-params", appDomainParamsStr);

					result.put("cv-seed", seed);
					result.put("num-folds", getNumFolds());
					result.put("fold", fold);

					result.put("num-compounds", dataset.getNumInstances());
					result.put("num-labels", dataset.getNumLabels());
					for (int i = 0; i < dataset.getNumLabels(); i++)
						result.put("label#" + i, dataset.getDataSet().attribute(dataset.getLabelIndices()[i]).name());

					result.put("cardinality", dataset.getCardinality());

					result.put("num-predictions", ev.getData(fold).getNumInstances());

					int n = dataset.getNumLabels();
					result.put("macro-inside-ad", ev.getPctInsideAD(fold));
					//					for (int i = 0; i < n; i++)
					//						result.put("macro-inside-ad#" + i, ev.getPctInsideAD(fold, i));

					for (ConfidenceLevel c : ConfidenceLevelProvider.LEVELS)
					{
						String s = c.getShortName();

						for (int i = 0; i < n; i++)
							result.put("macro-inside-ad#" + i + s, ev.getPctInsideAD(fold, i, c));

						result.put("hamming-loss" + s, ev.getResult(new HammingLoss(c).getName(), fold));
						result.put("1-hamming-loss" + s, 1 - ev.getResult(new HammingLoss(c).getName(), fold));

						result.put("subset-accuracy" + s, ev.getResult(new SubsetAccuracy(c).getName(), fold));
						//result.put( "accuracy", ev.getMean(new ExampleBasedAccuracy().getName()));
						//result.put( "precision", ev.getMean(new ExampleBasedPrecision().getName()));
						//result.put( "recall", ev.getMean(new ExampleBasedRecall().getName()));

						result.put("macro-accuracy" + s, ev.getResult(new MacroAccuracy(c, n, false).getName(), fold));
						result.put("weighted-macro-accuracy" + s,
								ev.getResult(new MacroAccuracy(c, n, true).getName(), fold));
						for (int i = 0; i < n; i++)
							result.put("macro-accuracy#" + i + s,
									ev.getResult(new MacroAccuracy(c, n, false).getName(), fold, i));
						result.put("micro-accuracy" + s, ev.getResult(new MicroAccuracy(c, n).getName(), fold));

						result.put("micro-f-measure" + s, ev.getResult(new MicroFMeasure(c, n).getName(), fold));
						result.put("macro-f-measure" + s, ev.getResult(new MacroFMeasure(c, n, false).getName(), fold));
						result.put("weighted-macro-f-measure" + s,
								ev.getResult(new MacroFMeasure(c, n, true).getName(), fold));
						for (int i = 0; i < n; i++)
							result.put("macro-f-measure#" + i + s,
									ev.getResult(new MacroFMeasure(c, n, false).getName(), fold, i));

						//							result.put( "micro-auc" + s,
						//									ev.getResult(new MicroAUC(c, n).getName(), fold));
						result.put("macro-auc" + s, ev.getResult(new MacroAUC(c, n, false).getName(), fold));
						result.put("weighted-macro-auc" + s, ev.getResult(new MacroAUC(c, n, true).getName(), fold));
						for (int i = 0; i < n; i++)
							result.put("macro-auc#" + i + s, ev.getResult(new MacroAUC(c, n, false).getName(), fold, i));

						result.put("micro-mcc" + s, ev.getResult(new MicroMCC(c, n).getName(), fold));
						result.put("macro-mcc" + s, ev.getResult(new MacroMCC(c, n, false).getName(), fold));
						result.put("weighted-macro-mcc" + s, ev.getResult(new MacroMCC(c, n, true).getName(), fold));
						for (int i = 0; i < n; i++)
							result.put("macro-mcc#" + i + s, ev.getResult(new MacroMCC(c, n, false).getName(), fold, i));

						result.put("micro-sensitivity" + s, ev.getResult(new MicroRecall(c, n).getName(), fold));
						result.put("macro-sensitivity" + s, ev.getResult(new MacroRecall(c, n).getName(), fold));
						for (int i = 0; i < n; i++)
							result.put("macro-sensitivity#" + i + s,
									ev.getResult(new MacroRecall(c, n).getName(), fold, i));

						result.put("micro-specificity" + s, ev.getResult(new MicroSpecificity(c, n).getName(), fold));
						result.put("macro-specificity" + s, ev.getResult(new MacroSpecificity(c, n).getName(), fold));
						for (int i = 0; i < n; i++)
							result.put("macro-specificity#" + i + s,
									ev.getResult(new MacroSpecificity(c, n).getName(), fold, i));

						result.put("macro-ppv" + s, ev.getResult(new MacroPrecision(c, n).getName(), fold));
						for (int i = 0; i < n; i++)
							result.put("macro-ppv#" + i + s, ev.getResult(new MacroPrecision(c, n).getName(), fold, i));

						result.put("macro-npv" + s,
								ev.getResult(new MacroNegativePredictiveValue(c, n).getName(), fold));
						for (int i = 0; i < n; i++)
							result.put("macro-npv#" + i + s,
									ev.getResult(new MacroNegativePredictiveValue(c, n).getName(), fold, i));

						for (int i = 0; i < n; i++)
							for (ConfusionMatrix.Values v : ConfusionMatrix.Values.values())
								result.put(v.toString() + "#" + i + s,
										ev.getConfMatrixValue(v.toString(), new MacroRecall(c, n).getName(), fold, i));

						//							result.put( "macro-appdomain", ev.getResult(new MacroPercentInsideAD(
						//									 n).getName(), fold));
						//							for (int i = 0; i < n; i++)
						//								result.put( "macro-appdomain#" + i, ev.getResult(
						//										new MacroPercentInsideAD(confLevel, n).getName(), fold, i));
					}
					results.add(result);
				}
				synchronized (res)
				{
					for (Map<String, Object> result : results)
					{
						int resIndex = res.addResult();
						for (String key : result.keySet())
							res.setResultValue(resIndex, key, result.get(key));
					}
					if (numExperimentFolds < res.getNumResults())
						throw new Error("WTF");
					else if (numExperimentFolds == res.getNumResults())
					{
						System.out.println("\nprinting " + res.getNumResults() + " results to " + resFile);
						//System.out.println(res.toNiceString());
						ResultSetIO.printToFile(resFile, res, true);

						if (Settings.LIST_PCT_CATEGORIES)
						{
							System.out.println("printing categories to file /tmp/categories.txt");
							String s = "";
							for (Categories c : PredictiveClusteringTrees.collectedCategories)
								s += c.getCompactString() + "\n";
							FileUtil.writeStringToFile("/tmp/categories.txt", s);
						}
					}
				}
			}
		});
	}

	public void buildModel(final String modelName, final String modelAlias, final boolean exitWithoutBuilding)
			throws Exception
	{
		iterate(false, new MLCMethod()
		{
			@Override
			public void runMLC(String datasetNameStr, MultiLabelInstances dataset, MLCDataInfo di,
					String mlcAlgorithmStr, MultiLabelLearner mlcAlgorithm, String imputationString,
					String classifierString, String mlcAlgorithmParamsStr, ApplicabilityDomain appDomain,
					String appDomainStr, String appDomainParamsStr, int seed, ResultSet res, File resFile,
					SinglePredictionTracker tracker, int numExperimentFolds, String info) throws Exception
			{
				System.out.println("\nbuild model: " + modelName + "\n");

				if (!isPureModelBuilding())
				{

					Settings.createModelDirectory(modelName);

					ReportMLC rep = new ReportMLC(Settings.modelDescriptionReport(modelName),
							"Technical model description", false, "../../");
					rep.report.addParagraph("This is a technical description of model "
							+ rep.report.encodeLink(".", modelAlias) + ".");
					rep.report.newSection("General");
					ResultSet rs = new ResultSet();
					int r = rs.addResult();
					rs.setResultValue(r, "name", modelAlias);
					rs.setResultValue(r, "#endpoints", di.numEndpoints);
					rs.setResultValue(r, "#training dataset compounds", dataset.getNumInstances());
					rs.setResultValue(r, "#descriptors", dataset.getDataSet().numAttributes() - dataset.getNumLabels());

					rs.setResultValue(r, Settings.text("mlc-algorithm"),
							Settings.text("mlc-algorithm." + mlcAlgorithmStr));
					rs.setResultValue(r, Settings.text("mlc-algorithm"),
							Settings.text("mlc-algorithm." + mlcAlgorithmStr));
					if (mlcAlgorithmStr.equals("PCT"))
						classifierString = "n/a";
					else
						rs.setResultValue(r, Settings.text("classifier"),
								Settings.text("classifier." + classifierString));
					rs.setResultValue(r, Settings.text("descriptors"),
							Settings.text("descriptors." + Settings.getFeaturesFromDatabaseName(datasetNameStr)));
					rs.setResultValue(r, Settings.text("applicability-domain"),
							Settings.text("applicability-domain." + appDomainStr));
					rs.setResultValue(r, Settings.text("imputation"), Settings.text("imputation." + imputationString));
					rep.report.addTable(rs, true);

					String categoriesStr = mlcAlgorithmStr.equals("PCT") ? "PCT" : "None";
					String[][] information = { { "mlc-algorithm", mlcAlgorithmStr },
							{ "classifier", classifierString },
							{ "descriptors", Settings.getFeaturesFromDatabaseName(datasetNameStr) },
							{ "applicability-domain", appDomainStr }, { "imputation", imputationString } };
					for (String[] inf : information)
					{
						String concept = inf[0];
						String method = inf[1];
						if (Settings.contains(concept + "." + method))
						{
							rep.report.newSection(Settings.text(concept));
							rep.report.addParagraph(Settings.text(concept + ".description"));
							if (method != null)
							{
								rep.report.addGap();
								rep.report.addParagraph("The method used for this model is:");
								rep.report.newSubsection(Settings.text(concept + "." + method));
								rep.report.addParagraph(Settings.text(concept + "." + method + ".description"));

								if (concept.equals("mlc-algorithm") && mlcAlgorithmStr.equals("PCT"))
								{
									rep.report.newSubsection(Settings.text("categories"));
									rep.report.addParagraph(Settings.text("categories.description"));
								}
							}
						}
					}

					rep.report.newSection(Settings.text("model-confidence"));
					rep.report.addParagraph(Settings.text("model-confidence.description",
							ConfidenceLevelProvider.CONFIDENCE_LEVEL_HIGH.getNiceName(),
							ConfidenceLevelProvider.CONFIDENCE_LEVEL_LOW.getNiceName()));
					rep.close();
				}

				if (!isPureModelBuilding() && appDomain != null)
				{
					DefaultMLCApplicabilityDomain ad = new DefaultMLCApplicabilityDomain(
							(DistanceBasedApplicabilityDomain) appDomain);
					ad.init(dataset);
					System.out.println("writing app domain model to file: " + Settings.modelADFile(modelName));
					SerializationHelper.write(Settings.modelADFile(modelName), ad);
				}

				if (!imputationString.equals("false"))
				{
					dataset = dataset.clone();
					if (imputationString.equals("true"))
					{
						Imputation.apply(new EnsembleOfClassifierChains(new RandomForest(), 15, true, false), null,
								dataset);
						//						Imputation.apply(new EnsembleOfClassifierChains(new RandomForest(), 10, false, false), null,
						//								dataset);
					}
					else if (imputationString.equals("random"))
						Imputation.apply(null, new Random(seed), dataset);
				}

				if (!exitWithoutBuilding)
				{
					mlcAlgorithm.build(dataset);
					System.out.println(mlcAlgorithm.toString());
				}

				if (!isPureModelBuilding())
				{
					if (!exitWithoutBuilding)
					{
						if (mlcAlgorithm instanceof PredictiveClusteringTrees)
							((PredictiveClusteringTrees) mlcAlgorithm).listCategories().writeToFile(modelName);
						System.out.println("writing mlc model to file: " + Settings.modelFile(modelName));
						if (mlcAlgorithm instanceof PredictiveClusteringTrees)
							((PredictiveClusteringTrees) mlcAlgorithm).prepareSerialize();
						SerializationHelper.write(Settings.modelFile(modelName), mlcAlgorithm);
					}
					ModelInfo.writeModelProps(modelName, modelAlias, datasetNameStr,
							Settings.getFeaturesFromDatabaseName(datasetNameStr), getExperimentName());
				}

				if (exitWithoutBuilding)
				{
					System.err.println("HANDLE WITH CARE: quitting after model building; skipping validation");
					System.exit(0);
				}
				//					MultiLabelLearner mlcAlgorithm2 = (MultiLabelLearner) SerializationHelper.read("/tmp/test.model");
			}
		});
	}

	private void fillMissing() throws Exception
	{
		iterate(false, new MLCMethod()
		{
			@Override
			public void runMLC(String datasetNameStr, MultiLabelInstances dataset, MLCDataInfo di,
					String mlcAlgorithmStr, MultiLabelLearner mlcAlgorithm, String imputationString,
					String classifierString, String mlcAlgorithmParamsStr, ApplicabilityDomain appDomain,
					String appDomainStr, String appDomainParamsStr, int seed, ResultSet res, File resFile,
					SinglePredictionTracker tracker, int numExperimentFolds, String info) throws Exception
			{
				if (getFillMissings().length == 0)
					throw new IllegalArgumentException(
							"please specifiy fill-missing (-m class|confidence|class,confidence)");
				for (final String fillMissingString : getFillMissings())
				{
					if (ArrayUtil.indexOf(new String[] { "class", "confidence" }, fillMissingString) == -1)
						throw new IllegalArgumentException(
								"please fill-missing has to be class|confidence|class,confidence but is "
										+ fillMissingString);

					final boolean confidence = fillMissingString.equals("confidence");

					MultiLabelInstances data = dataset;
					DefaultMLCApplicabilityDomain ad = null;
					if (appDomain != null)
					{
						ad = new DefaultMLCApplicabilityDomain((DistanceBasedApplicabilityDomain) appDomain);
						ad.init(data);
					}
					mlcAlgorithm.build(data);

					//					SerializationHelper.write("/tmp/test.model", mlcAlgorithm);
					//					MultiLabelLearner mlcAlgorithm2 = (MultiLabelLearner) SerializationHelper.read("/tmp/test.model");

					CSVFile csv = FileUtil.readCSV(Settings.csvFile(datasetNameStr));

					if (csv.content.size() - 1 != data.getNumInstances())
						throw new IllegalStateException("WTF");

					HashMap<Integer, MultiLabelOutput> compoundPrediction = new HashMap<Integer, MultiLabelOutput>();

					for (int j = 1; j < csv.getHeader().length; j++)
					{
						int toFillCount = 0;
						int insideADCount = 0;

						String column = csv.getHeader()[j];
						if (column.endsWith("_real"))
							continue;
						Attribute attr = null;
						Integer labelIndex = null;

						List<Instance> missings = new ArrayList<Instance>();
						//											boolean showHist = true;

						for (int i = 1; i < csv.content.size(); i++)
						{
							int compoundIndex = i - 1;
							String vals[] = csv.content.get(i);

							if (vals[j] == null)
							{
								if (attr == null)
								{
									attr = data.getDataSet().attribute(column);
									if (attr == null)
										throw new Error("attr not found: " + column);
									labelIndex = data.getLabelsOrder().get(column);
									System.err.print("there are missings for " + column + " labelIndex:" + labelIndex);
									if (labelIndex != null)
										System.err.println(", filling with prediction");
									else
									{
										System.err.println(", this is no endpoint, skip this");
										break;
									}
								}

								Instance inst = data.getDataSet().get(compoundIndex);
								double val = inst.value(attr);
								if (!Double.isNaN(val))
									throw new Error("not missing in arff");

								if (!compoundPrediction.containsKey(compoundIndex))
									compoundPrediction.put(compoundIndex, mlcAlgorithm.makePrediction(inst));
								if (appDomain != null)
									if (ad.isInside(inst, labelIndex))
										insideADCount++;

								missings.add(inst);
								//													else
								//													{
								//														if (showHist)
								//															ADVisualization.showDistHistogramm(attr.name(),
								//																	ad.getApplicabilityDomain(labelIndex),
								//																	ad.getDistance(inst, labelIndex));
								//														showHist = false;
								//													}

								MultiLabelOutput out = compoundPrediction.get(compoundIndex);
								if (confidence)
								{
									vals[j] = String.valueOf(out.getConfidences()[labelIndex]);
								}
								else
								{
									if (out.getBipartition()[labelIndex])
										vals[j] = "1";
									else
										vals[j] = "0";
								}

								toFillCount++;
								//													System.out.println("csv-null value at compound " + compoundIndex
								//															+ " for feature " + column + " (" + j + "), arff: " + val);
							}
						}

						if (appDomain != null)
							if (toFillCount > 0)
							{
								System.err.println("filled " + insideADCount + "/" + toFillCount
										+ " missing values (size ad "
										+ ad.getApplicabilityDomain(labelIndex).getData().numInstances() + ", dist ad "
										+ ad.getApplicabilityDomain(labelIndex).getAverageTrainingDistance() + ")");
								if (DEBUG)
									ADVisualization.showDistHistogramm(attr.name(), ad, labelIndex,
											ArrayUtil.toArray(missings));

							}
					}

					String outfile = Settings.filledCsvFile(datasetNameStr, getExperimentName(), confidence);
					System.out
							.println("writing filled csv " + (confidence ? "Confidence" : "Class") + " to " + outfile);
					FileUtil.writeCSV(outfile, csv, false);
					for (Boolean addID : new Boolean[] { true, false })
					{
						String arffFile = Settings.filledArffFile(datasetNameStr, getExperimentName(), confidence,
								addID);
						System.out.println("writing filled arff " + (confidence ? "Confidence" : "Class") + " to "
								+ arffFile);
						String xmlFile = null;
						if (!addID)
							xmlFile = Settings.filledXmlFile(datasetNameStr, getExperimentName(), confidence);
						ArffWriter.writeToArffFile(new File(arffFile), new FilledArffWritable(csv, addID, xmlFile));
					}
					//									MultipleEvaluation ev = eval.crossValidate(mlcAlgorithm, data, numFolds);
				}
			}
		});
	}

	enum Function
	{
		predict_compounds, predict_appdomain, validate, multi_validation_report, validation_report, model_report,
		dataset_report, predict, fill_missing, cluster, filter_missclassified, compound_table, endpoint_table,
		category_table;
	}

	public static void main(String args[]) throws Exception
	{
		Locale.setDefault(Locale.US);

		if (args != null && args.length == 1 && args[0].equals("debug"))
		{
			//launch
			DEBUG = true;
			String a;

			//String model = "pct-test-333";
			//a = "endpoint_table -o " + model;

			//a = "validate -x 1 -i 0 -u 1 -o pct-ftest-imputation-3 -y FP1 -d dataEAgg_noV_Ca15-20c20_FP1 -a PCT -p \"pruning=None;ftest=0.125;min-num=3\" -t true -c RandomForest -e PCT-Ftest-Impu -q None -w \"default\"";
			a = "validate -x 2 -i 0 -u 1 -f 2 -lc -y FP1 -d dataEAgg_noV_Ca15-20c20_FP1 -a PCT -p \"pruning=None;ftest=0.125;min-num=3\" -t true -c RandomForest -e PCT-Ftest-Impu -q None -w \"default\"";
			//			a = "validate -x 1 -i 0 -u 1 -o br-man2 -y MAN2 -d dataE_noV_Ca15-20c20_MAN2 -a BR -p \"default\" -t false -c RandomForest -e BR-MAN2 -q None -w \"default\"";
			//a = "validate -x 2 -i 0 -u 1 -o br-ad-123 -ox -y PCFP1 -d dataE_noV_Ca15-20c20_PCFP1 -a BR -p \"default\" -t false -c RandomForest -e BR-AD2 -q Centroid -w \"confidence=false;distance=2.0\"";
			//a = "multi_validation_report -e ParamsECC-Y -d dataY_PCFP -z all";
			//a = "predict_compounds -o 03-br-model -v 975f4ba64117f4dc65044299552ee698";
			//a = "predict_compounds -o pct-ftest-imputation -v da560e1f985a1e4f38d482c499c5fda4";
			//a = "validation_report -o br-ad-123 -z all_ad";
			//a = "model_report -o br-ad-123";

			//			Settings.PWD = "/home/martin/workspace/BMBF-MLC/";
			//args = "dataset_report -d dataY_OB".split(" "); //,dataR_noV_EqF_PCFP,dataR_noV_Cl68_PCFP
			//a = "multi_validation_report -e BR-AD -d dataB_noV_Ca15-20c20_PCFP -z all";

			//a = "validation_report -o CPDBAS -z all";
			//a = "validation_report -o RepdoseNeustoff-EqF -z all";

			//a = "compound_table -o Repdose";
			//a = "predict_compounds -o Repdose -v 8746894c3510f705bb330497272a4602";

			//			String cl = "Ca15-20c20";
			//			String data = "dataA";
			//			String model = "RepdoseNeustoff-old-" + cl;
			//			//a = "validation_report -o RepdoseNeustoff-" + cl + " -z all";
			//			
			//			//a = "compound_table -o RepdoseNeustoff-" + cl;

			//a = "predict_compounds -o pct-ftest -v 75a9bfd8e5fbc442cbc9b811364d8d2b";
			//a = "filter_missclassified -d dataY_PCFP -e BR-Y -s activityoutcome-cpdbas-hamster";
			//a = "category_table -o pct-ftest -c1 cb11f87e838a8d5c45bb830f750c556b -c2 8,10,62,210,510,550,598,678,729";
			//a = "category_table -o pct-ftest -c1 b6a140be7cfc68c351c10b285aa11ca6 -c2 0,116,140,175,184,271,278,281,363,364,417,460,495,498,501,567,731,782,827,834,867";

			//args = "validation_report -d dataY_OB -e BRY -z all -o cpdbas".split(" ");
			//a = "compound_table -d dataY_OB -o cpdbas";

			//args = "predict_compounds -o MLC_model_v0.0.1 -v f9451e3ab767e8317afb8a939af279fe".split(" ");
			//args = "predict_compounds -o MLC_model_v0.0.1 -v c29c3a4367a8d417c99addd3e1e2ebfc".split(" ");
			//args = "predict_compounds -o MLC_model_v0.0.1 -v 75a9bfd8e5fbc442cbc9b811364d8d2b".split(" ");
			//			args = "predict_compounds -o small-model -v 18c6234f6a91a297831236742bef3899".split(" ");
			//args = "predict_compounds -o only-repdose-model -v ea656a858ee3a71d9ad91d7377ec5b3b".split(" ");
			//args = "predict_compounds -o only-repdose-model -v 7129b181d3b77e51e8dcc2b999ed0ded".split(" ");
			//args = "predict_compounds -o only-repdose-model -v 9b6fd19cd0245838059df5b99a3d58d1".split(" ");

			//a = "validation_report -o pct -z all";
			//a = "compound_table -o pct-ftest";

			//a = "predict_compounds -o pct -v 41fcba09f2bdcdf315ba4119dc7978dd";
			//			args = "predict_appdomain -o only-repdose-model -v 9b6fd19cd0245838059df5b99a3d58d1 -b 0 -s liver"
			//					.split(" ");

			//args = "dataset_report -d dataR_withV_RvsV_PCFP".split(" ");
			//
			//a = "fill_missing -a BR -c RandomForest -d dataB_noV_Cl68_PC -m class -q None -e test";

			//a = "cluster -1 data/dataR_noV.csv -2 ratio -3 0.6 -4 0.8";
			//a = "multi_validation_report -e CL -d dataB_noV_EqF_PC,dataB_noV_Cl68_PC,dataB_noV_Cl15-20a_PC -z all";
			//a = "multi_validation_report -e ECC -d dataB_noV_Ca15-20c20_PCFP -z all";
			//a = "multi_validation_report -e BR -d dataB_noV_Ca15-20c20_PCFP -z all";

			//a = "multi_validation_report -e ParamsPCT -d dataA_noV_Ca15-20c20_PCFP -z all";
			//a = "cluster -1 data/dataC_withV.csv -2 absolute -3 1.5 -4 2.0 -5 2.0";
			//a = "multi_validation_report -e PC12-Y -d dataY_PC1,dataY_PC2 -z all";
			//			a = "dataset_report -d dataC_noV_Ca15-20c20_dummy";
			//a = "multi_validation_report -e BR -d dataA_noV_Ca15-20c20_PCFP1,dataB_noV_Ca15-20c20_PCFP1,dataC_noV_Ca15-20c20_PCFP1,dataD_noV_Ca15-20c20_PCFP1,dataE_noV_Ca15-20c20_PCFP1 -z all";
			//a = "multi_validation_report -e FeatECC -d dataE_noV_Ca15-20c20_PC,dataE_noV_Ca15-20c20_PCX,dataE_noV_Ca15-20c20_FP1,dataE_noV_Ca15-20c20_PCFP1,dataE_noV_Ca15-20c20_PCXFP1 -z all";

			//a = "fill_missing -m class -x 1 -d dataE_noV_Ca15-20c20_FP1 -i 0 -u 1 -f 10 -a ECC -p \"num-chains=15;confidences=true;replacement=false\" -t false -c RandomForest -e ECC-FP1 -q None -w \"default\"";
			//a = "fill_missing -m class -x 1 -d dataEAgg_noV_Ca15-20c20_MAN2 -i 0 -u 1 -f 10 -a ECC -p \"num-chains=15;confidences=true;replacement=false\" -t false -c RandomForest -e ECC-MAN2 -q None -w \"default\"";
			//a = "dataset_report -d dataE_noV_Ca15-20c20_PC";

			//a = "multi_validation_report -e ImpuPCT -d dataE_noV_Ca15-20c20_PCFP1 -z auc_only";

			//a = "multi_validation_report -e BR-MLCs-2-Impu -d dataE_noV_Ca15-20c20_PCFP1 -z all";

			args = a.split(" ");

			//			ReportMLC.poster_pics();
			//			System.exit(1);
		}

		Function func = null;
		try
		{
			func = Function.valueOf(args[0]);
		}
		catch (Exception e)
		{
			System.err.println("First param should be one of the following functions: "
					+ ArrayUtil.toString(Function.values()));
			System.exit(1);
		}

		args = ArrayUtil.removeAt(String.class, args, 0);
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
		options.addOption("m", "fill-missing", true, "Fill missing values in .csv file");
		options.addOption("s", "endpoint", true, "Endpoint (for missclassfied filetering, maybe 'all')");
		options.addOption("y", "feature-type", true, "Feature-type for building models");
		options.addOption("z", "performance-measure", true, "Performance measure for validation report (accuracy|auc)");
		options.addOption("1", "cluster-infile", true, "csv infile for clustering");
		options.addOption("2", "cluster-method", true, "one of " + ArrayUtil.toString(ClusterEndpoint.Method.values()));
		options.addOption("3", "cluster-low-threshold", true, "min threshold for discretization point");
		options.addOption("4", "cluster-high-threshold", true, "max threshold for discretization point");
		options.addOption("5", "cluster-adjust-chronic", true, "adjust chronic values (optional)");
		options.addOption("o", "model-name", true, "Model name (builds this model when validating)");
		options.addOption("oa", "model-alias", true, "Nice model name (builds this model when validating)");
		options.addOption("ox", false, "Quit after building the model (skip validation)");
		options.addOption("v", "compound-arff-file", true, "compound arff file name for prediction");
		options.addOption("q", "app-domain", true, "AppDomain algortihm");
		options.addOption("w", "app-domain-params", true, "AppDomain algortihm params");
		options.addOption("b", "index", true, "an index");
		options.addOption("c1", "category-key", true, "key for model category");
		options.addOption("c2", "category-inidces", true, "compound-indices for model category");
		options.addOption("lc", false, "list-categories when validating pcts");

		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse(options, args);
		RunMLC mlc = new RunMLC();
		if (cmd.hasOption("x"))
			mlc.setNumCores(Integer.parseInt(cmd.getOptionValue("x")));
		if (cmd.hasOption("d"))
			mlc.setDatasetName(cmd.getOptionValue("d"));
		if (cmd.hasOption("i"))
			mlc.setMinSeed(Integer.parseInt(cmd.getOptionValue("i")));
		if (cmd.hasOption("u"))
			mlc.setMaxSeedExclusive(Integer.parseInt(cmd.getOptionValue("u")));
		if (cmd.hasOption("f"))
			mlc.setNumFolds(Integer.parseInt(cmd.getOptionValue("f")));
		if (cmd.hasOption("a"))
			mlc.setMlcAlgorithm(cmd.getOptionValue("a"));
		if (cmd.hasOption("p"))
			mlc.setMlcAlgorithmParams(cmd.getOptionValue("p"));
		if (cmd.hasOption("c"))
			mlc.setClassifier(cmd.getOptionValue("c"));
		if (cmd.hasOption("e"))
			mlc.setExperimentName(cmd.getOptionValue("e"));
		if (cmd.hasOption("t"))
			mlc.setImputation(cmd.getOptionValue("t"));
		if (cmd.hasOption("m"))
			mlc.setFillMissing(cmd.getOptionValue("m"));
		if (cmd.hasOption("q"))
			mlc.setAppDomain(cmd.getOptionValue("q"));
		if (cmd.hasOption("w"))
			mlc.setAppDomainParams(cmd.getOptionValue("w"));

		switch (func)
		{
			case predict_compounds:
				PredictCompounds.predictCompounds(cmd.getOptionValue("o"), cmd.getOptionValue("v"));
				break;
			case predict_appdomain:
				PredictCompounds.predictAppDomain(cmd.getOptionValue("o"), cmd.getOptionValue("v"),
						Integer.parseInt(cmd.getOptionValue("b")), cmd.getOptionValue("s"));
				break;
			case validate:
				if (cmd.hasOption("o"))
				{
					// disable cv-repetitions for model building
					int min = mlc.getMinSeed();
					int max = mlc.getMaxSeedExclusive();
					mlc.setMinSeed(0);
					mlc.setMaxSeedExclusive(1);
					mlc.buildModel(cmd.getOptionValue("o"),
							cmd.hasOption("oa") ? cmd.getOptionValue("oa") : cmd.getOptionValue("o"),
							cmd.hasOption("ox"));
					mlc.setMinSeed(min);
					mlc.setMaxSeedExclusive(max);
				}
				if (cmd.hasOption("lc"))
					Settings.LIST_PCT_CATEGORIES = true;
				mlc.validate();
				break;
			case validation_report:
				ValidationReport.validationReport(cmd.getOptionValue("z"), cmd.getOptionValue("o"));
				break;
			case multi_validation_report:
				MultiValidationReport.multiValidationReport(mlc.getExperimentName(), mlc.getDatasetNames(),
						cmd.getOptionValue("z"));
				break;
			case dataset_report:
				ReportMLC.datasetReport(mlc.getDatasetNames());
				break;
			case compound_table:
				ReportMLC.compoundTable(cmd.getOptionValue("o"));
				break;
			case endpoint_table:
				if (cmd.hasOption("o"))
					ReportMLC.endpointTableFromModel(cmd.getOptionValue("o"));
				else
				{
					if (mlc.getDatasetNames().length != 1)
						throw new IllegalArgumentException("plz give model or exactly one dataset");
					ReportMLC.endpointTableFromDataset(mlc.getDatasetNames()[0]);
				}
				break;
			case model_report:
				ReportMLC.modelReport();
				break;
			case category_table:
				ReportMLC.categoryTable(cmd.getOptionValue("o"), cmd.getOptionValue("c1"), cmd.getOptionValue("c2"));
				break;
			case cluster:
				ClusterEndpoint.apply(cmd.getOptionValue("1"), ClusterEndpoint.Method.valueOf(cmd.getOptionValue("2")),
						Double.parseDouble(cmd.getOptionValue("3")), Double.parseDouble(cmd.getOptionValue("4")),
						cmd.hasOption("5") ? Double.parseDouble(cmd.getOptionValue("5")) : null);
				break;
			case filter_missclassified:
				FilterMissclassified.doFilter(mlc.getDatasetNames(), mlc.getExperimentName(), cmd.getOptionValue("s"));
				break;
			case fill_missing:
				mlc.fillMissing();
				break;
			default:
				System.err.println("Unhandled functionality: " + func);
				System.exit(1);
		}
		System.exit(0);
	}
}
