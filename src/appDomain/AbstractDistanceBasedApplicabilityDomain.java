package appDomain;

import util.StringUtil;
import weka.core.Instance;

public abstract class AbstractDistanceBasedApplicabilityDomain implements DistanceBasedApplicabilityDomain
{
	protected boolean debug = false;

	protected Method method = Method.median;
	protected double distanceMultiplier = 3.0;
	protected boolean continous = true;
	protected double continousFullDistanceMultiplier = 1.0;

	@Override
	public void setMethod(Method method)
	{
		this.method = method;
	}

	@Override
	public void setDistanceMultiplier(double distance)
	{
		this.distanceMultiplier = distance;
	}

	@Override
	public void setContinous(boolean continous)
	{
		this.continous = continous;
	}

	@Override
	public boolean isContinous()
	{
		return continous;
	}

	@Override
	public void setContinousFullDistanceMultiplier(double continousFullDistanceMultiplier)
	{
		this.continousFullDistanceMultiplier = continousFullDistanceMultiplier;
	}

	@Override
	public double getApplicabilityDomainDistance()
	{
		return Math.min(getMaxTrainingDistance(), getAverageTrainingDistance() * distanceMultiplier);
	}

	@Override
	public double getContinousFullApplicabilityDomainDistance()
	{
		return getAverageTrainingDistance() * continousFullDistanceMultiplier;
	}

	@Override
	public boolean isInside(Instance i)
	{
		Double dist = getDistance(i);
		double prop = getApplicabilityDomainPropability(dist);
		boolean ad = prop != 0;
		if (debug)
			System.out.println("Dist is " + StringUtil.formatDouble(dist) + " -> AD is " + ad);
		return ad;
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
		if (x > getApplicabilityDomainDistance())
			return 0;
		if (continous)
		{
			if (x < getContinousFullApplicabilityDomainDistance())
				return 1;
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
		else
		{
			return 1;
		}
	}
}
