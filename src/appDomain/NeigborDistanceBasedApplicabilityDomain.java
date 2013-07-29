package appDomain;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Arrays;

import util.DoubleArraySummary;
import util.StringLineAdder;
import weka.core.DistanceFunction;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader.ArffReader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

public class NeigborDistanceBasedApplicabilityDomain extends AbstractDistanceBasedApplicabilityDomain
{
	Instances data;
	protected ReplaceMissingValues missingValuesConverter;
	protected DistanceFunction distanceFunction;
	protected double averageDistanceToNeighbors;
	protected double maxDistanceToNeighbors;
	protected double[] trainingDistances;
	int numNeighbors = 5;

	public NeigborDistanceBasedApplicabilityDomain()
	{
		this(5);
	}

	private NeigborDistanceBasedApplicabilityDomain(int numNeighbors)
	{
		this.numNeighbors = numNeighbors;
	}

	public void setNumNeighbors(int numNeighbors)
	{
		this.numNeighbors = numNeighbors;
	}

	@Override
	public String getDistanceDescription()
	{
		return "Distance to training dataset compounds";
	}

	@Override
	public void init(Instances origData) throws Exception
	{
		missingValuesConverter = new ReplaceMissingValues();
		data = new Instances(origData);
		missingValuesConverter.setInputFormat(data);
		data = Filter.useFilter(data, missingValuesConverter);

		distanceFunction = new EuclideanDistance(data);

		trainingDistances = new double[data.numInstances()];
		for (int i = 0; i < data.numInstances(); i++)
		{
			double[] dist = new double[data.numInstances() - 1];
			int count = 0;
			for (int j = 0; j < data.numInstances(); j++)
				if (i != j)
					dist[count++] = distanceFunction.distance(data.get(i), data.get(j));
			Arrays.sort(dist);
			dist = Arrays.copyOfRange(dist, 0, numNeighbors);
			if (method == Method.median)
				trainingDistances[i] = DoubleArraySummary.create(dist).getMedian();
			else
				trainingDistances[i] = DoubleArraySummary.create(dist).getMean();
		}
		//		System.out.println(ArrayUtil.toString(distances));
		DoubleArraySummary vals = DoubleArraySummary.create(trainingDistances);
		if (method == Method.median)
			averageDistanceToNeighbors = vals.getMedian();
		else
			averageDistanceToNeighbors = vals.getMean();
		maxDistanceToNeighbors = vals.getMax();

		//		if (DEBUG)
		//			System.out.println("AD is nearest-neigbor-dist <= " + factor + " * "
		//					+ StringUtil.formatDouble(medianDistance) + " ( "
		//					+ StringUtil.formatDouble(factor * medianDistance) + " )");
	}

	@Override
	public double getDistance(Instance i)
	{
		missingValuesConverter.input(i);
		missingValuesConverter.batchFinished();
		i = missingValuesConverter.output();

		double[] dist = new double[data.numInstances()];
		for (int j = 0; j < data.numInstances(); j++)
			dist[j] = distanceFunction.distance(i, data.get(j));
		Arrays.sort(dist);
		dist = Arrays.copyOfRange(dist, 0, numNeighbors);
		double d;
		if (method == Method.median)
			d = DoubleArraySummary.create(dist).getMedian();
		else
			d = DoubleArraySummary.create(dist).getMean();
		return d;
	}

	@Override
	public Instances getData()
	{
		return data;
	}

	@Override
	public double getAverageTrainingDistance()
	{
		return averageDistanceToNeighbors;
	}

	@Override
	public double getMaxTrainingDistance()
	{
		return maxDistanceToNeighbors;
	}

	@Override
	public double[] getTrainingDistances()
	{
		return trainingDistances;
	}

	public static void main(String args[]) throws Exception
	{
		StringLineAdder s = new StringLineAdder();
		s.add("@RELATION test");
		s.add("");
		s.add("@ATTRIBUTE attr1  REAL");
		s.add("@ATTRIBUTE attr2  REAL");
		s.add("@ATTRIBUTE class {T,F}");
		s.add("");
		s.add("@DATA");
		s.add("5.1,10000.0,T");
		s.add("4.9,9000.0,T");
		s.add("5.1,11000.0,F");
		s.add("4.5,12000.0,T");
		s.add("4.9,?,T");
		s.add("?,?,F");

		ArffReader r = new ArffReader(new BufferedReader(new StringReader(s.toString())));
		Instances data = r.getData();
		data.setClassIndex(data.numAttributes() - 1);
		NeigborDistanceBasedApplicabilityDomain ad = new NeigborDistanceBasedApplicabilityDomain();
		ad.debug = true;
		ad.init(data);

		for (int i = 0; i < data.numInstances(); i++)
			ad.isInside(data.get(i));
	}

	@Override
	public boolean isContinous()
	{
		throw new IllegalStateException("not yet implemented");
	}

	@Override
	public DistanceBasedApplicabilityDomain copy()
	{
		//implement setter first
		throw new IllegalStateException("not yet implemented");
	}
}