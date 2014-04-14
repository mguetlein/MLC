package pct;

import util.ArrayUtil;
import util.StringUtil;

public class Category
{
	int[] instances;
	String name;
	String key;

	public Category(int[] instances)
	{
		this.instances = instances;
		this.name = ArrayUtil.toString(ArrayUtil.toIntegerArray(instances), ",", "", "").replaceAll(" ", "");
		this.key = StringUtil.getMD5(name);
	}

	public int getNumInstances()
	{
		return instances.length;
	}

	public String getKey()
	{
		return key;
	}

	public String getName()
	{
		return name;
	}

	public String toCSV()
	{
		return key + ",\"" + name + "\"";
	}

	public String toString()
	{
		return toCSV();
	}
}
