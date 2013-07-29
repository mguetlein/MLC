package appDomain;

import java.util.ArrayList;
import java.util.List;

import mulan.data.MultiLabelInstances;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class DefaultMLCApplicabilityDomain implements DistanceBasedMLCApplicabilityDomain
{
	MultiLabelInstances data;
	DistanceBasedApplicabilityDomain adCompleteDataset;
	List<DistanceBasedApplicabilityDomain> adLabels;

	private DistanceBasedApplicabilityDomain baseAd;

	public DefaultMLCApplicabilityDomain(DistanceBasedApplicabilityDomain ad)
	{
		baseAd = ad;
	}

	@Override
	public void init(MultiLabelInstances data) throws Exception
	{
		this.data = data;
		Instances dataNoLabels = new Instances(data.getDataSet());
		for (int l = data.getNumLabels() - 1; l >= 0; l--)
			dataNoLabels.deleteAttributeAt(data.getLabelIndices()[l]);
		adCompleteDataset = baseAd.copy();
		adCompleteDataset.init(dataNoLabels);

		adLabels = new ArrayList<DistanceBasedApplicabilityDomain>();
		for (int l = 0; l < data.getNumLabels(); l++)
		{
			Instances dataL = new Instances(dataNoLabels);
			for (int i = data.getNumInstances() - 1; i >= 0; i--)
				if (data.getDataSet().get(i).isMissing(data.getLabelIndices()[l]))
					dataL.remove(i);
			DistanceBasedApplicabilityDomain ad = baseAd.copy();
			ad.init(dataL);
			adLabels.add(ad);
		}
	}

	private Instance removeLabels(Instance i)
	{
		Instance inst = new DenseInstance(i);
		for (int l = data.getNumLabels() - 1; l >= 0; l--)
			inst.deleteAttributeAt(data.getLabelIndices()[l]);
		return inst;
	}

	@Override
	public double getDistance(Instance i, int labelIndex)
	{
		return ((CentroidBasedApplicabilityDomain) adLabels.get(labelIndex)).getDistance(removeLabels(i));
	}

	@Override
	public double getDistanceCompleteDataset(Instance i)
	{
		return ((CentroidBasedApplicabilityDomain) adCompleteDataset).getDistance(removeLabels(i));
	}

	@Override
	public boolean isInside(Instance i, int labelIndex)
	{
		return adLabels.get(labelIndex).isInside(removeLabels(i));
	}

	@Override
	public boolean isInsideCompleteDataset(Instance i)
	{
		return adCompleteDataset.isInside(removeLabels(i));
	}

	@Override
	public DistanceBasedApplicabilityDomain getApplicabilityDomainCompleteDataset()
	{
		return adCompleteDataset;
	}

	@Override
	public DistanceBasedApplicabilityDomain getApplicabilityDomain(int labelIndex)
	{
		return adLabels.get(labelIndex);
	}

	@Override
	public double[] getApplicabilityDomainPropability(Instance inst)
	{
		double d[] = new double[adLabels.size()];
		inst = removeLabels(inst);
		for (int l = 0; l < d.length; l++)
			d[l] = adLabels.get(l).getApplicabilityDomainPropability(inst);
		return d;
	}

	@Override
	public double getApplicabilityDomainPropability(Instance i, int labelIndex)
	{
		return adLabels.get(labelIndex).getApplicabilityDomainPropability(removeLabels(i));
	}

	@Override
	public double getApplicabilityDomainPropabilityCompleteDataset(Instance i)
	{
		return adCompleteDataset.getApplicabilityDomainPropability(removeLabels(i));
	}

	@Override
	public boolean isContinous()
	{
		return adCompleteDataset.isContinous();
	}

}