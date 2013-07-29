package mlc;
import java.util.ArrayList;
import java.util.List;

import util.StringUtil;

public class ConfusionMatrix
{
	public static enum Values
	{
		TP, FP, TN, FN;
	}

	public static List<Object[]> buildMatrix(double tp, double tn, double fp, double fn, String t, String f)
	{
		List<Object[]> m = new ArrayList<Object[]>();
		m.add(new Object[] { "Actual \\ Predicted", "Predicted: " + t, "Predicted: " + f, "" });
		m.add(new Object[] { "Actual: " + t, "TP: " + StringUtil.formatDouble(tp),
				"FN: " + StringUtil.formatDouble(fn), (tp + fn) });
		m.add(new Object[] { "Actual: " + f, "FP: " + StringUtil.formatDouble(fp),
				"TN: " + StringUtil.formatDouble(tn), (fp + tn) });
		m.add(new Object[] { "", (tp + fp), (fn + tn), "" });
		return m;
	}

	//	public static List<Object[]> buildMatrix(double tp, double tn, double fp, double fn, String t, String f)
	//	{
	//		List<Object[]> m = new ArrayList<Object[]>();
	//		m.add(new Object[] { "Actual \\ Predicted", "Predicted: " + t, "Predicted: " + f, "" });
	//		m.add(new Object[] { "Actual: " + t, tp, fn, (tp + fn) });
	//		m.add(new Object[] { "Actual: " + f, fp, tn, (fp + tn) });
	//		m.add(new Object[] { "", (tp + fp), (fn + tn), "" });
	//		return m;
	//	}
}
