import java.io.File;
import java.util.List;

import util.ArrayUtil;
import datamining.ResultSet;
import datamining.ResultSetIO;

public class PlotMLC
{
	public PlotMLC()
	{

	}

	public void showBoxplot()
	{

	}

	public static void main(String args[]) throws Exception
	{
		ResultSet rs = ResultSetIO.parseFromFile(new File("/tmp/result"));
		System.out.println(rs.toNiceString());

		List<String> equalProps = ArrayUtil.toList(new String[] { "cv-seed" });
		List<String> ommitProps = ArrayUtil.toList(new String[] { "label#0", "label#1", "label#2" });
		List<String> varianceProps = ArrayUtil.toList(new String[] {});

		Double numCompounds = Double.parseDouble(rs.getUniqueValue("num-compounds") + "");
		Double numLabels = Double.parseDouble(rs.getUniqueValue("num-labels") + "");
		int numCVSeeds = rs.getResultValues("cv-seed").size();
		Double numCVFolds = Double.parseDouble(rs.getUniqueValue("num-folds") + "");

		List<String> catProps = ArrayUtil.toList(new String[] { "hamming-loss", "macro-accuracy", "micro-accuracy",
				"subset-accuracy" });
		rs.showBoxPlot("Performance for different MLC-algorithms", "Performance", new String[] { "compounds: "
				+ numCompounds + ", labels: " + numLabels + ", " + numCVSeeds + " x " + numCVFolds + "-fold CV" },
				"mlc-algorithm", catProps);

		catProps = ArrayUtil.toList(new String[] { "macro-accuracy#0", "macro-accuracy#1", "macro-accuracy#2" });
		List<String> catPropsDisp = ArrayUtil.toList(new String[] { rs.getUniqueValue("label#0") + "",
				rs.getUniqueValue("label#1") + "", rs.getUniqueValue("label#2") + "" });
		rs.showBoxPlot("Performance for different MLC-algorithms", "Performance", new String[] { "compounds: "
				+ numCompounds + ", labels: " + numLabels + ", " + numCVSeeds + " x " + numCVFolds + "-fold CV" },
				"mlc-algorithm", catProps, catPropsDisp);

		ResultSet rs2 = rs.join(equalProps, ommitProps, varianceProps);
		System.out.println(rs2.toNiceString());

		//		List<String> catProps = ArrayUtil.toList(new String[] { "hamming-loss", "macro-accuracy" });
		//		rs2.showBarPlot("test", "yAxis", "cv-seed", catProps, null, null);

	}
}
