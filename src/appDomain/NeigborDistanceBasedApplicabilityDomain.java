package appDomain;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Arrays;

import util.DoubleArraySummary;
import util.StringLineAdder;
import util.StringUtil;
import weka.core.DistanceFunction;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader.ArffReader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

public class NeigborDistanceBasedApplicabilityDomain implements DistanceBasedApplicabilityDomain
{
	Instances data;
	protected ReplaceMissingValues missingValuesConverter;
	protected DistanceFunction distanceFunction;
	protected double medianDistance;
	protected double[] trainingDistances;
	int k = 3;
	double factor = 2.0;
	public static boolean DEBUG = false;

	public NeigborDistanceBasedApplicabilityDomain()
	{
		this(5);
	}

	private NeigborDistanceBasedApplicabilityDomain(int k)
	{
		this.k = k;
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
			dist = Arrays.copyOfRange(dist, 0, k);
			trainingDistances[i] = DoubleArraySummary.create(dist).getMedian();
		}
		//		System.out.println(ArrayUtil.toString(distances));
		medianDistance = DoubleArraySummary.create(trainingDistances).getMedian();

		if (DEBUG)
			System.out.println("AD is nearest-neigbor-dist <= " + factor + " * "
					+ StringUtil.formatDouble(medianDistance) + " ( "
					+ StringUtil.formatDouble(factor * medianDistance) + " )");
	}

	@Override
	public double getApplicabilityDomainDistance()
	{
		return medianDistance * factor;
	}

	@Override
	public double getContinousFullApplicabilityDomainDistance()
	{
		return medianDistance;
	}

	@Override
	public double getApplicabilityDomainPropability(Instance i)
	{
		return getApplicabilityDomainPropability(getDistance(i));
	}

	@Override
	public double getApplicabilityDomainPropability(Double x)
	{
		if (x < 0)
			throw new Error();
		if (x < getContinousFullApplicabilityDomainDistance())
			return 1;
		if (x > getApplicabilityDomainDistance())
			return 0;

		//map fullAd-ad to -3, 3
		x -= getContinousFullApplicabilityDomainDistance();
		x /= (getApplicabilityDomainDistance() - getContinousFullApplicabilityDomainDistance());
		x *= 6;
		x -= 3;
		double y = Math.tanh(x);
		// put upside down
		y *= -1;
		// transition from -1 - 1 to 0-1
		y += 1;
		y /= 2.0;
		return y;
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
		dist = Arrays.copyOfRange(dist, 0, k);
		double d = DoubleArraySummary.create(dist).getMedian();
		return d;

	}

	@Override
	public boolean isInside(Instance i)
	{
		double d = getDistance(i);

		boolean ad = d <= medianDistance * factor;
		if (DEBUG)
			System.out.println("Dist is " + StringUtil.formatDouble(d) + " -> AD is " + ad);
		return ad;
	}

	@Override
	public Instances getData()
	{
		return data;
	}

	public double getMedianDistance()
	{
		return medianDistance;
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
		NeigborDistanceBasedApplicabilityDomain.DEBUG = true;
		NeigborDistanceBasedApplicabilityDomain ad = new NeigborDistanceBasedApplicabilityDomain();
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