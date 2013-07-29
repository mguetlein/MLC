import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Random;

import util.StringUtil;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.SMO;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.Instances;

public class SingleEvalTest
{
	public static final boolean V = false;
	public static final Random r = new Random((long) 343789);

	public static void main(String args[]) throws Exception
	{
		File file = new File("tmp/dataC_small.arff");

		Instances data = new Instances(new FileReader(file));
		System.out.println(file);

		data.setClassIndex(data.numAttributes() - 1);

		int one = 0;
		double d[] = new double[data.numInstances()];
		for (int i = 0; i < data.numInstances(); i++)
		{
			if (data.instance(i).stringValue((Attribute) data.classAttribute()).equals("1"))
			{
				d[i] = 1;
				one++;
			}
			else
				d[i] = 0;
		}
		int zero = data.numInstances() - one;
		System.out.println(data.numInstances() + " (" + zero + "/" + one + ")");

		if (data.numInstances() < 25)
		{
			System.out.println("skipping, too few instances");
		}
		else
		{

			//Classifier[] c = new Classifier[] { new RandomForest(), new SMO(), new NaiveBayes() };//, new J48() };
			Classifier[] c = new Classifier[] { new SMO(), new RandomForest(), new IBk() };
			for (Classifier classifier : c)
			{
				for (int i = 0; i < 10; i++)
				{
					System.out.print("cv-seed " + i + " ");

					Evaluation eval = new Evaluation(data);
					eval.crossValidateModel(classifier, data, 10, new Random(i));
					System.out.print(StringUtil.concatWhitespace(classifier.getClass().getSimpleName(), 15) + " ");
					LinkedHashMap<String, Double> res = new LinkedHashMap<String, Double>();
					res.put("Accuracy  ", eval.pctCorrect());
					//					res.put("Sens      ", eval.truePositiveRate(1));
					//					res.put("Spec      ", eval.trueNegativeRate(1));
					res.put("Precision ", eval.precision(1));
					res.put("Recall    ", eval.recall(1));

					for (String k : res.keySet())
						System.out.print(k + " "
								+ StringUtil.concatWhitespace(new DecimalFormat("#.##").format(res.get(k)), 5) + " ");

					//					boolean starred = eval.pctCorrect() > 55.0 && eval.precision(1) > 0.5 && eval.recall(1) > 0.5;
					//					if (starred)
					//						System.out.print("*");
					//					starred = eval.pctCorrect() > 65.0 && eval.precision(1) > 0.5 && eval.recall(1) > 0.5;
					//					if (starred)
					//						System.out.print("*");

					System.out.println();
				}
			}
		}

	}
}
