package pct;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jeans.util.StringUtils;
import jeans.util.cmdline.CMDLineArgs;
import mulan.classifier.MultiLabelLearner;
import mulan.classifier.NeighborMultiLabelOutput;
import mulan.classifier.transformation.TransformationBasedMultiLabelLearner;
import mulan.data.MultiLabelInstances;
import util.ArrayUtil;
import util.DoubleArraySummary;
import util.FileUtil;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import clus.Clus;
import clus.algo.ClusInductionAlgorithmType;
import clus.algo.tdidt.ClusDecisionTree;
import clus.algo.tdidt.ClusNode;
import clus.algo.tdidt.tune.CDTTuneFTest;
import clus.data.rows.RowData;
import clus.ext.ensembles.ClusEnsembleClassifier;
import clus.main.Settings;
import clus.model.ClusModel;
import clus.statistic.StatisticPrintInfo;
import clus.util.ClusException;

public class PredictiveClusteringTrees extends TransformationBasedMultiLabelLearner implements Serializable
{
	//	static
	//	{
	//		Settings.VERBOSE = 0;
	//	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 7481560926148148956L;
	/**
	 * 
	 */
	private String settingFile;
	private ArrayList<int[]> predsArray;
	private ArrayList<double[]> predsCount;
	private ArrayList<Integer> nbExamples;

	private MultiLabelInstances dataset;

	//	private ClusRun cr;
	private Clus clus;

	public final static String ARFF_ATTRIBUTE = "@ATTRIBUTE";
	/** Constant set for numeric attributes. */
	public static final int NUMERIC = 0;

	/** Constant set for nominal attributes. */
	public static final int NOMINAL = 1;

	/** Constant set for attributes with string values. */
	public static final int STRING = 2;

	/** Constant set for attributes with date values. */
	public static final int DATE = 3;

	/** Constant set for relation-valued attributes. */
	public static final int RELATIONAL = 4;

	/** A keyword used to denote a numeric attribute */
	public final static String ARFF_ATTRIBUTE_INTEGER = "integer";

	/** A keyword used to denote a numeric attribute */
	public final static String ARFF_ATTRIBUTE_REAL = "real";

	/** A keyword used to denote a numeric attribute */
	public final static String ARFF_ATTRIBUTE_NUMERIC = "NUMERIC";

	/** The keyword used to denote a string attribute */
	public final static String ARFF_ATTRIBUTE_STRING = "string";

	/** The keyword used to denote a date attribute */
	public final static String ARFF_ATTRIBUTE_DATE = "date";

	/** The keyword used to denote a relation-valued attribute */
	public final static String ARFF_ATTRIBUTE_RELATIONAL = "relational";

	/** The keyword used to denote the end of the declaration of a subrelation */
	public final static String ARFF_END_SUBRELATION = "@end";

	public static int count = 1;

	private Heuristic heuristic;
	private PruningMethod pruningMethod;
	private EnsembleMethod ensembleMethod;
	private Integer minimalNumberExamples;
	private Double fTest;

	public enum EnsembleMethod
	{
		None, RForest, Bagging
	}

	public enum Heuristic
	{
		Default, ReducedError, Gain, GainRatio, VarianceReduction, MEstimate, Morishita, DispersionAdt, DispersionMlt,
		RDispersionAdt, RDispersionMlt, VarianceReductionMissing
	}

	public enum PruningMethod
	{
		Default, None, C4_5, M5, M5Multi, ReducedErrorVSB, Garofalakis, GarofalakisVSB, CartVSB, CartMaxSizeg;

		@Override
		public String toString()
		{
			if (this == C4_5)
				return "C4.5";
			else
				return super.toString();
		}

		public static PruningMethod valOf(String s)
		{
			if (s.equals("C4.5"))
				return C4_5;
			else
				return valueOf(s);
		}
	}

	public PredictiveClusteringTrees()
	{
		this(Heuristic.Default, PruningMethod.Default, EnsembleMethod.None, null, null);
	}

	public PredictiveClusteringTrees(Heuristic heuristic, PruningMethod pruningMethod, EnsembleMethod ensembleMethod,
			Integer minimalNumberExamples, Double fTest)
	{
		this.heuristic = heuristic;
		this.pruningMethod = pruningMethod;
		this.ensembleMethod = ensembleMethod;
		this.minimalNumberExamples = minimalNumberExamples;
		this.fTest = fTest;
	}

	public String toString(Attribute attribute)
	{

		StringBuffer text = new StringBuffer();

		text.append(ARFF_ATTRIBUTE).append(" ").append(quote(attribute.name())).append(" ");
		switch (attribute.type())
		{
			case NOMINAL:
				text.append('{');
				Enumeration enu = attribute.enumerateValues();
				while (enu.hasMoreElements())
				{
					text.append(Utils.quote((String) enu.nextElement()));
					if (enu.hasMoreElements())
						text.append(',');
				}
				text.append('}');
				break;
			case NUMERIC:
				text.append(ARFF_ATTRIBUTE_NUMERIC);
				break;
			case STRING:
				text.append(ARFF_ATTRIBUTE_STRING);
				break;
			case DATE:
				text.append(ARFF_ATTRIBUTE_DATE).append(" ").append(Utils.quote(attribute.getDateFormat()));
				break;
			case RELATIONAL:
				text.append(ARFF_ATTRIBUTE_RELATIONAL).append("\n");
				Enumeration enm = attribute.relation().enumerateAttributes();
				while (enm.hasMoreElements())
				{
					text.append(enm.nextElement()).append("\n");
				}
				text.append(ARFF_END_SUBRELATION).append(" ").append(Utils.quote(attribute.name()));
				break;
			default:
				text.append("UNKNOWN");
				break;
		}
		return text.toString();
	}

	/**
	 * Quotes a string if it contains special characters.
	 * 
	 * The following rules are applied:
	 *
	 * A character is backquoted version of it is one 
	 * of <tt>" ' % \ \n \r \t</tt>.
	 *
	 * A string is enclosed within single quotes if a character has been
	 * backquoted using the previous rule above or contains 
	 * <tt>{ }</tt> or is exactly equal to the strings 
	 * <tt>, ? space or ""</tt> (empty string).
	 *
	 * A quoted question mark distinguishes it from the missing value which
	 * is represented as an unquoted question mark in arff files.
	 *
	 * @param string 	the string to be quoted
	 * @return 		the string (possibly quoted)
	 * @see		#unquote(String)
	 */
	public static/*@pure@*/String quote(String string)
	{
		boolean quote = true;

		// backquote the following characters 
		if ((string.indexOf('\n') != -1) || (string.indexOf('\r') != -1) || (string.indexOf('\'') != -1)
				|| (string.indexOf('"') != -1) || (string.indexOf('\\') != -1) || (string.indexOf('\t') != -1)
				|| (string.indexOf('%') != -1))
		{
			//		  string = Utils.backQuoteChars(string);
			quote = true;
		}
		// Enclose the string in 's if the string contains a recently added
		// backquote or contains one of the following characters.
		if ((quote == true) || (string.indexOf('{') != -1) || (string.indexOf('}') != -1)
				|| (string.indexOf(',') != -1) || (string.equals("?")) || (string.indexOf(' ') != -1)
				|| (string.equals("")))
		{
			string = ("\"".concat(string)).concat("\"");
		}

		return string;
	}

	public void writeArffHeader(PrintWriter wrt, Instances instances) throws IOException, ClusException
	{
		//		wrt.println("@RELATION '"+StringUtils.removeSingleQuote(instances.relationName())+"'");
		wrt.println("%\n@RELATION " + StringUtils.removeSingleQuote(instances.relationName()));
		wrt.println();
		for (int i = 0; i < instances.numAttributes(); i++)
		{
			wrt.print(toString(instances.attribute(i)));
			wrt.println();
		}
	}

	public void writeArff(String fname, Instances instances) throws IOException, ClusException
	{
		PrintWriter wrt = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fname)));
		writeArffHeader(wrt, instances);
		wrt.println();
		wrt.println("@DATA");
		for (int j = 0; j < instances.numInstances(); j++)
		{
			wrt.print(instances.instance(j).toString() + "\n");
		}
		wrt.flush();
		wrt.close();

	}

	//	public void setSettingFile(String settingFile)
	//	{
	//		this.settingFile = settingFile;
	//	}

	public static int numIterations = 10;

	protected void createSettingFile(String settingsFile)
	{
		int numAttributes = dataset.getFeatureAttributes().size();
		int numLabels = dataset.getNumLabels();

		try
		{
			//settingsFile = File.createTempFile("dataset-settings", "s").getAbsolutePath();

			BufferedWriter writer = new BufferedWriter(new FileWriter(settingsFile));
			/*
			 * [Attributes]
				Target = 243-282
				Clustering = 243-282
				Descriptive = 1-242
				
				[Tree]
				Heuristic = VarianceReduction
				
				[Data] 
				File = dataB_noV_Cl68_FP.arff 
				TestSet = dataB_noV_Cl68_FP_test.arff 
			 */
			writer.write("[General]\n");
			writer.write("Verbose = 0\n");
			writer.write("\n");
			writer.write("[Attributes]\n");
			writer.write("Descriptive = 1-" + numAttributes + "\n");
			writer.write("Target = " + (dataset.getFeatureAttributes().size() + 1) + "-" + (numAttributes + numLabels)
					+ "\n");
			writer.write("Clustering = " + (dataset.getFeatureAttributes().size() + 1) + "-"
					+ (numAttributes + numLabels) + "\n");
			writer.write("\n");
			writer.write("[Tree]\n");
			writer.write("Heuristic = " + heuristic + "\n");
			writer.write("PruningMethod = " + pruningMethod + "\n");
			if (fTest != null)
				writer.write("FTest = " + fTest + "\n");
			writer.write("\n");
			if (ensembleMethod != EnsembleMethod.None)
			{
				writer.write("[Ensemble]\n");
				writer.write("Iterations = " + numIterations + "\n");
				//				System.err.println(numIterations);
				//				numIterations += 10;
				writer.write("EnsembleMethod = " + ensembleMethod + "\n");
				writer.write("\n");
			}
			writer.write("[Data]\n");
			writer.write("File = " + trainArffPath + "\n");
			writer.write("TestSet = " + testArffPath + "\n");
			if (minimalNumberExamples != null)
			{
				writer.write("\n");
				writer.write("[Model]\n");
				writer.write("MinimalNumberExamples = " + minimalNumberExamples + "\n");
			}
			writer.flush();
			writer.close();

			//			System.out.println("created settings file: " + settingsFile);

		}
		catch (Exception e)
		{
			throw new Error(e);
		}
	}

	public void prepareSerialize()
	{
		clus = null;
	}

	@Override
	public MultiLabelLearner makeCopy() throws Exception
	{
		Clus tmp = clus;
		clus = null;
		PredictiveClusteringTrees pct = (PredictiveClusteringTrees) super.makeCopy();
		clus = tmp;
		if (clus != null)
			pct.clus = initClus();
		return pct;
	}

	protected Clus initClus()
	{
		try
		{
			Clus clus = new Clus();

			//			String trainArffPath = "./dataset.arff";
			trainArffPath = File.createTempFile("dataset", "arff").getAbsolutePath();
			tmpArffFiles.add(trainArffPath);
			//			System.out.println("trainArffPath: "+trainArffPath);
			writeArff(trainArffPath, dataset.getDataSet());

			testArffPath = File.createTempFile("dataset_test", "arff").getAbsolutePath();
			tmpArffFiles.add(testArffPath);

			// necessary to write pseudo test file since test file needs to exist when calling Clus
			//			String testArffPath = "./dataset_test.arff";
			FileUtil.copy(trainArffPath, testArffPath);
			//			writeArff(testArffPath, dataset.getDataSet());

			String appName = "pct-model_t" + System.currentTimeMillis() + "_h" + clus.hashCode();
			String settingsFile = appName + ".s";
			if (new File(settingsFile).exists())
				throw new Error();
			createSettingFile(settingsFile);

			//			System.err.println("settings:\n" + FileUtil.readStringFromFile(settingsFile));

			String[] args = new String[1];
			//			System.out.println(settingsFile);
			args[0] = settingsFile;

			Settings sett = clus.getSettings();

			CMDLineArgs cargs = new CMDLineArgs(clus);
			cargs.process(args);

			sett.setDate(new Date());
			sett.setAppName(args[0]);

			clus.initSettings(cargs);
			ClusInductionAlgorithmType clss = null;

			if (ensembleMethod == EnsembleMethod.RForest)
			{
				sett.setEnsembleMode(true);
				clss = new ClusEnsembleClassifier(clus);
			}
			else
			{
				clss = new ClusDecisionTree(clus);
			}
			if (sett.getFTestArray().isVector())
				clss = new CDTTuneFTest(clss, sett.getFTestArray().getDoubleVector());

			if (ensembleMethod == EnsembleMethod.Bagging)
			{
				//clus.isxval = true;
				Settings.IS_XVAL = true;
				clus.initialize(cargs, clss);
				clus.baggingRun(clss);
			}
			else
			{
				clus.initialize(cargs, clss);
				clus.singleRun(clss);
			}

			this.predsArray = clus.getPredsArrs();
			this.predsCount = clus.getPredsCounts();
			this.nbExamples = clus.getNumExamples();

			if (!new File(settingsFile).delete())
				System.err.println("could not delete " + settingsFile);
			if (!new File(appName + ".out").delete())
				System.err.println("could not delete " + appName + ".out");
			if (!new File(appName + ".model").delete())
				System.err.println("could not delete " + appName + ".model");

			return clus;
		}
		catch (Exception e)
		{
			throw new Error(e);
		}
	}

	String trainArffPath;
	String testArffPath;

	protected void buildInternal(MultiLabelInstances instances)
	{
		this.dataset = instances;

		try
		{

			//			ClusOutput.printHeader();

			this.clus = initClus();

			//			if (Debug.debug == 1)
			//				ClusStat.show();
			//			DebugFile.close();
		}
		catch (Exception e)
		{
			throw new Error(e);
		}
	}

	public String toString()
	{
		String s = "";
		int count = 0;
		for (String m : new String[] { "default", "original", "pruned" })
		{
			try
			{
				ClusModel root = clus.getClusRun().getModel(count++);
				RowData pex = (RowData) clus.getClusRun().getTrainingSet();
				StatisticPrintInfo info = clus.getSettings().getStatisticPrintInfo();
				List<Integer> numCompoundsInLeafs = numLeafs(root, info, pex);
				if (numCompoundsInLeafs.size() > 0)
				{
					DoubleArraySummary summary = DoubleArraySummary.create(ArrayUtil.toDoubleArray(ArrayUtil
							.toArray(numCompoundsInLeafs)));
					s += m + ": numLeafs:" + summary.getNum() + ", range:" + (int) summary.getMin() + "-"
							+ (int) summary.getMax() + " median:" + summary.toString() + "\n";
					//				System.out.println(ListUtil.toString(numCompoundsInLeafs));
				}
			}
			catch (NullPointerException e)
			{
				s += m + ": -\n";
			}
		}
		return s;
	}

	public static int YES = 0;
	public static int NO = 1;
	public static int UNK = 2;

	public List<Integer> numLeafs(ClusModel root, StatisticPrintInfo info, RowData examples)
	{
		return numLeafs(new ArrayList<Integer>(), root, info, examples);
	}

	public List<Integer> numLeafs(List<Integer> leafs, ClusModel root, StatisticPrintInfo info, RowData examples)
	{
		ClusNode rootNode = (ClusNode) root;
		int arity = rootNode.getNbChildren();

		if (arity > 0)
		{

			int delta = rootNode.hasUnknownBranch() ? 1 : 0;
			if (arity - delta == 2)
			{

				RowData examples0 = null;
				RowData examples1 = null;
				if (examples != null)
				{
					if ((rootNode.getAlternatives() != null) || (rootNode.getOppositeAlternatives() != null))
					{
						// in the case of alternative tests, the classification is done based on how many of the total tests predict left or right branch
						examples0 = examples.applyAllAlternativeTests(rootNode.getTest(), rootNode.getAlternatives(),
								rootNode.getOppositeAlternatives(), 0);
						examples1 = examples.applyAllAlternativeTests(rootNode.getTest(), rootNode.getAlternatives(),
								rootNode.getOppositeAlternatives(), 1);
					}
					else
					{
						examples0 = examples.apply(rootNode.getTest(), 0);
						examples1 = examples.apply(rootNode.getTest(), 1);
					}
				}

				numLeafs(leafs, (ClusNode) rootNode.getChild(YES), info, examples0);
				if (rootNode.hasUnknownBranch())
				{
					numLeafs(leafs, (ClusNode) rootNode.getChild(NO), info, examples1);
					numLeafs(leafs, (ClusNode) rootNode.getChild(UNK), info, examples0);
				}
				else
					numLeafs(leafs, (ClusNode) rootNode.getChild(NO), info, examples1);
			}
			else
			{
				for (int i = 0; i < arity; i++)
				{
					ClusNode child = (ClusNode) rootNode.getChild(i);
					RowData examplesi = null;
					if (examples != null)
					{
						examples.apply(rootNode.getTest(), i);
					}
					if (i != arity - 1)
					{
						numLeafs(leafs, child, info, examplesi);
					}
					else
					{
						numLeafs(leafs, child, info, examplesi);
					}
				}
			}
		}
		else
		{//on the leaves
			//			System.out.println(examples.getNbRows());
			leafs.add(examples.getNbRows());
		}
		return leafs;
	}

	public void printModel(ClusModel root, StatisticPrintInfo info, RowData examples)
	{

		ClusNode rootNode = (ClusNode) root;
		int arity = rootNode.getNbChildren();

		if (arity > 0)
		{

			int delta = rootNode.hasUnknownBranch() ? 1 : 0;
			if (arity - delta == 2)
			{

				RowData examples0 = null;
				RowData examples1 = null;
				if (examples != null)
				{
					if ((rootNode.getAlternatives() != null) || (rootNode.getOppositeAlternatives() != null))
					{
						// in the case of alternative tests, the classification is done based on how many of the total tests predict left or right branch
						examples0 = examples.applyAllAlternativeTests(rootNode.getTest(), rootNode.getAlternatives(),
								rootNode.getOppositeAlternatives(), 0);
						examples1 = examples.applyAllAlternativeTests(rootNode.getTest(), rootNode.getAlternatives(),
								rootNode.getOppositeAlternatives(), 1);
					}
					else
					{
						examples0 = examples.apply(rootNode.getTest(), 0);
						examples1 = examples.apply(rootNode.getTest(), 1);
					}
				}

				printModel((ClusNode) rootNode.getChild(YES), info, examples0);

				if (rootNode.hasUnknownBranch())
				{

					printModel((ClusNode) rootNode.getChild(NO), info, examples1);

					printModel((ClusNode) rootNode.getChild(UNK), info, examples0);

				}
				else
				{
					printModel((ClusNode) rootNode.getChild(NO), info, examples1);
				}
			}
			else
			{
				for (int i = 0; i < arity; i++)
				{
					ClusNode child = (ClusNode) rootNode.getChild(i);
					RowData examplesi = null;
					if (examples != null)
					{
						examples.apply(rootNode.getTest(), i);
					}
					if (i != arity - 1)
					{
						printModel(child, info, examplesi);
					}
					else
					{
						printModel(child, info, examplesi);
					}
				}
			}
		}
		else
		{//on the leaves
			System.out.println(examples.getNbRows());
		}
	}

	public static Set<String> tmpArffFiles = new HashSet<String>();

	public static void clear()
	{
		for (String f : tmpArffFiles)
			if (!new File(f).delete())
				System.err.println("could not delete " + f);
	}

	protected NeighborMultiLabelOutput makePredictionInternal(Instance instance)
	{
		if (clus == null)
			clus = initClus();

		NeighborMultiLabelOutput output = null;
		//		String testArffPath = null;

		try
		{
			Instances testInstances = new Instances(dataset.getDataSet(), -1);
			testInstances.add(instance);
			testInstances = new Instances(testInstances);
			for (int l = 0; l < dataset.getNumLabels(); l++)
				//hack: set all test values to "0" because missing ("?") could cause errors when no missing in dataset
				testInstances.get(0).setValue(labelIndices[l], "0");
			//			testArffPath = "./dataset_test.arff";

			writeArff(testArffPath, testInstances);

			clus.testModel2(testArffPath);

			int[] predAr = clus.getPredArr();
			boolean[] predArBool = new boolean[predAr.length];
			for (int i = 0; i < predAr.length; i++)
			{
				if (predAr[i] == 1)
					predArBool[i] = true;
				else
					predArBool[i] = false;
			}
			double[] predCount = clus.getPredCount();

			int neigbors[] = new int[] { 0, 1, 2 };
			output = new NeighborMultiLabelOutput(predArBool, predCount, neigbors);

			//			if (Debug.debug == 1)
			//				ClusStat.show();
			//			DebugFile.close();
		}
		catch (ClusException e)
		{
			System.err.println();
			System.err.println("Error: " + e);
		}
		catch (IllegalArgumentException e)
		{
			System.err.println();
			System.err.println("Error: " + e.getMessage());
		}
		catch (FileNotFoundException e)
		{
			System.err.println();
			System.err.println("File not found: " + e);
		}
		catch (IOException e)
		{
			System.err.println();
			System.err.println("IO Error: " + e);
		}

		return output;
	}

}
