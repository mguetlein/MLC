package weka;

import java.util.ArrayList;
import java.util.List;

import mulan.data.MultiLabelInstances;
import util.ArrayUtil;
import weka.core.Instance;
import weka.core.Instances;

public class WekaUtil
{
	public static List<Integer> indices(MultiLabelInstances data, Instance inst)
	{
		return indices(data.getDataSet(), inst, data.getLabelIndices());
	}

	public static List<Integer> indices(Instances data, Instance inst)
	{
		return indices(data, inst, new int[0]);
	}

	public static List<Integer> indices(Instances data, Instance inst, int[] ignoreAttributeIndices)
	{
		List<Integer> res = new ArrayList<Integer>();
		if (data.numAttributes() != inst.numAttributes())
			throw new IllegalArgumentException("incompatible attributes " + data.numAttributes() + " != "
					+ inst.numAttributes());
		for (int i = 0; i < data.numAttributes(); i++)
			if (!data.attribute(i).equals(inst.attribute(i)))
				throw new IllegalArgumentException("incompatible data");

		//		System.out.println("    " + inst);

		for (int i = 0; i < data.numInstances(); i++)
		{
			Instance instance = data.get(i);
			//			System.out.println(StringUtil.concatWhitespace(i + "", 3) + " " + instance);

			boolean equal = true;
			for (int a = 0; a < data.numAttributes(); a++)
			{
				if (data.classIndex() == a || ArrayUtil.indexOf(ignoreAttributeIndices, a) != -1)
					continue;
				//				System.err.println(data.attribute(a));
				if (instance.isMissing(a) == true && inst.isMissing(a) == true)
					continue;
				if (instance.isMissing(a) != inst.isMissing(a) || instance.value(a) != inst.value(a))
				{
					//					System.out.println("no match " + data.attribute(a).name() + " : " + inst.value(a) + " != "
					//							+ instance.value(a));
					equal = false;
					break;
				}
			}
			if (equal)
				res.add(i);
		}
		//		System.out.println(ListUtil.toString(res));
		return res;
	}
}
