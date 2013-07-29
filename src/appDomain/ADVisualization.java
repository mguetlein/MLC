package appDomain;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;

import org.jfree.chart.ChartColor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.function.Function2D;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.TextAnchor;

import util.ArrayUtil;
import util.StringLineAdder;
import util.StringUtil;
import util.SwingUtil;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader.ArffReader;

import com.itextpdf.text.Font;

import freechart.HistogramPanel;

public class ADVisualization extends JFrame
{

	public static void showDistHistogramm(String datasetName, DistanceBasedApplicabilityDomain ad, Instance... inst)
	{
		HistogramPanel h = getDistHistogramm(datasetName, ad, inst);
		SwingUtil.showInDialog(h, new Dimension(800, 500));
	}

	public static HistogramPanel getDistHistogramm(String datasetName, DistanceBasedApplicabilityDomain ad,
			Instance... inst)
	{
		Double d[] = null;
		if (inst != null)
		{
			d = new Double[inst.length];
			for (int i = 0; i < d.length; i++)
				d[i] = ad.getDistance(inst[i]);
		}
		return getDistHistogramm(datasetName, ad, ArrayUtil.toDoubleArray(ArrayUtil.removeNullValues(d)));
	}

	public static void showDistHistogramm(String datasetName, DistanceBasedMLCApplicabilityDomain ad, int label,
			Instance... inst)
	{
		HistogramPanel h = getDistHistogramm(datasetName, ad, label, inst);
		SwingUtil.showInDialog(h, new Dimension(800, 500));
	}

	public static HistogramPanel getDistHistogramm(String datasetName, DistanceBasedMLCApplicabilityDomain ad,
			int label, Instance... inst)
	{
		Double d[] = null;
		if (inst != null)
		{
			d = new Double[inst.length];
			for (int i = 0; i < d.length; i++)
				if (label == -1)
					d[i] = ad.getDistanceCompleteDataset(inst[i]);
				else
					d[i] = ad.getDistance(inst[i], label);
		}
		return getDistHistogramm(datasetName,
				label == -1 ? ad.getApplicabilityDomainCompleteDataset() : ad.getApplicabilityDomain(label),
				ArrayUtil.toDoubleArray(ArrayUtil.removeNullValues(d)));
	}

	private static void showDistHistogramm(String datasetName, final DistanceBasedApplicabilityDomain ad,
			Double... testDistance)
	{
		HistogramPanel h = getDistHistogramm(datasetName, ad, testDistance);
		SwingUtil.showInDialog(h, new Dimension(800, 500));
	}

	private static HistogramPanel getDistHistogramm(String datasetName, final DistanceBasedApplicabilityDomain ad,
			Double... testDistance)
	{
		List<String> captions = new ArrayList<String>();
		List<double[]> values = new ArrayList<double[]>();
		List<Double> inside = new ArrayList<Double>();
		List<Double> outside = new ArrayList<Double>();
		boolean drawTestToForeground = testDistance != null && testDistance.length == 1;

		if (drawTestToForeground)
		{
			captions.add("Distance of training data compounds");
			values.add(ad.getTrainingDistances());
		}
		if (testDistance != null)
		{
			for (int i = 0; i < testDistance.length; i++)
			{
				if (testDistance[i] <= ad.getApplicabilityDomainDistance())
					inside.add(testDistance[i]);
				else
					outside.add(testDistance[i]);
			}
			if (inside.size() > 0)
			{
				captions.add((inside.size() == 1 ? "Test instance" : "Test instances") + " inside AD");
				values.add(ArrayUtil.toPrimitiveDoubleArray(inside));
			}
			if (outside.size() > 0)
			{
				captions.add((outside.size() == 1 ? "Test instance" : "Test instances") + " outside AD");
				values.add(ArrayUtil.toPrimitiveDoubleArray(outside));
			}
		}
		if (!drawTestToForeground)
		{
			captions.add("Distance of training data compounds");
			values.add(ad.getTrainingDistances());
		}

		//		List<String> subtitles = ArrayUtil.toList(new String[] { "test" });
		HistogramPanel h = new HistogramPanel("Applicability domain for " + datasetName, null,
				ad.getDistanceDescription(), "num compounds", captions, values, 30);

		int seriesCount = 0;
		if (!drawTestToForeground)
			h.setSeriesColor(seriesCount++, ChartColor.LIGHT_BLUE.brighter());
		if (outside.size() > 0)
			h.setSeriesColor(seriesCount++, ChartColor.LIGHT_RED.brighter());
		if (inside.size() > 0)
			h.setSeriesColor(seriesCount++, ChartColor.LIGHT_GREEN.brighter());
		if (drawTestToForeground)
			h.setSeriesColor(seriesCount++, ChartColor.LIGHT_BLUE.brighter());

		String lines[];
		if (ad.isContinous())
			lines = new String[] { "inside", "full", "test" };//{ "inside", "full", "half", "test" };
		else
			lines = new String[] { "inside", "test" };
		for (String s : lines)//{ "full", "inside" })
		{
			if (!drawTestToForeground)
				continue;

			double d;
			String desc;
			if (s.equals("inside"))
			{
				d = ad.getApplicabilityDomainDistance();
				desc = "↓ Outside ↓";
			}
			else if (s.equals("half"))
			{
				d = (ad.getApplicabilityDomainDistance() - ad.getContinousFullApplicabilityDomainDistance()) * 0.5
						+ ad.getContinousFullApplicabilityDomainDistance();
				desc = "↑ 50% Inside ↑";
			}
			else if (s.equals("full"))
			{
				d = ad.getContinousFullApplicabilityDomainDistance();
				desc = "↑ 100% Inside ↑";
			}
			else if (s.equals("test"))
			{
				d = testDistance[0];
				if (ad.getApplicabilityDomainPropability(testDistance[0]) == 0)
					desc = "Outside (\n"
							+ StringUtil.formatDouble(ad.getApplicabilityDomainPropability(testDistance[0]) * 100, 1)
							+ "%)";
				else
					desc = "Inside (\n"
							+ StringUtil.formatDouble(ad.getApplicabilityDomainPropability(testDistance[0]) * 100, 1)
							+ "%)";
			}
			else
				throw new Error("wtf");

			XYPlot p = ((XYPlot) h.getChart().getPlot());
			if (!s.equals("test"))
			{
				ValueMarker marker = new ValueMarker(d, Color.BLACK, new BasicStroke(1.5F)); // position is the value on the axis
				//			if (s.equals("test"))
				//			{
				//				marker.setStroke(new BasicStroke(2));
				//				if (ad.getApplicabilityDomainPropability(testDistance[0]) == 0)
				//					marker.setPaint(ChartColor.DARK_RED);
				//				else
				//					marker.setPaint(ChartColor.DARK_GREEN);
				//			}
				//			else
				if (p.getDomainAxis().getRange().getUpperBound() < d)
					p.getDomainAxis().setRange(p.getDomainAxis().getRange().getLowerBound(), d * 1.1);
				p.addDomainMarker(marker);
			}

			XYTextAnnotation updateLabel = new XYTextAnnotation(desc, d - p.getDomainAxis().getRange().getLength()
					* 0.005, p.getRangeAxis().getUpperBound() * (!s.equals("test") ? 0.75 : 0.02));
			updateLabel.setFont(updateLabel.getFont().deriveFont(updateLabel.getFont().getSize() + 4.0f)
					.deriveFont(Font.BOLD));
			updateLabel.setRotationAnchor(TextAnchor.BASELINE_CENTER);
			updateLabel.setTextAnchor(TextAnchor.BASELINE_CENTER);
			if (!s.equals("test"))
				updateLabel.setRotationAngle(-3.14 / 2);
			//			if (s.equals("test"))
			//			{
			//				if (ad.getApplicabilityDomainPropability(testDistance[0]) == 0)
			//					updateLabel.setPaint(ChartColor.DARK_RED);
			//				else
			//					updateLabel.setPaint(ChartColor.DARK_GREEN);
			//			}
			//			else
			updateLabel.setPaint(Color.black);
			p.addAnnotation(updateLabel);
		}
		//		XYTextAnnotation updateLabel2 = new XYTextAnnotation("↓ Outside Applicability Domain ↓",
		//				ad.getApplicabilityDomainDistance() //* 1.025
		//				, p.getRangeAxis().getUpperBound() * 0.5);
		//		updateLabel2.setFont(updateLabel2.getFont().deriveFont(updateLabel2.getFont().getSize() + 4.0f)
		//				.deriveFont(Font.BOLD));
		//		updateLabel2.setRotationAnchor(TextAnchor.BASELINE_CENTER);
		//		updateLabel2.setTextAnchor(TextAnchor.BASELINE_CENTER);
		//		updateLabel2.setRotationAngle(-3.14 / 2);
		//		updateLabel2.setPaint(Color.black);
		//		p.addAnnotation(updateLabel2);

		//		marker.setLabel("← Inside AD");
		//		marker.setLabelFont(marker.getLabelFont().deriveFont(marker.getLabelFont().getSize() + 10.0f));
		//		marker.setLabelOffset(new RectangleInsets(10, 0, 0, 0));

		h.setIntegerTickUnits();
		h.addFunction("Applicability Domain Propability", new Function2D()
		{

			@Override
			public double getValue(double x)
			{
				return ad.getApplicabilityDomainPropability(x);
			}
		});

		//		p.getRangeAxis().setAutoRange(false);
		//		p.getRangeAxis().setRange(0, 10);
		return h;
	}

	private static Frame lastFrame = null;

	public static void show(Instances inst, Instances inst2, Instances inst3) throws Exception
	{
		JFrame af = new JFrame("test");

		XYSeries series1 = new XYSeries("data");
		for (int i = 0; i < inst.numInstances(); i++)
			series1.add(inst.get(i).value(0), inst.get(i).value(1));
		CentroidBasedApplicabilityDomain ad = new CentroidBasedApplicabilityDomain();
		NeighborDistanceBasedApplicabilityDomain ad2 = new NeighborDistanceBasedApplicabilityDomain();
		ad.debug = true;
		ad2.debug = true;
		ad.init(inst);
		ad2.init(inst);

		XYSeries series2 = new XYSeries("inside centroid");
		XYSeries series3 = new XYSeries("inside pairwise");
		XYSeries series4 = new XYSeries("outside");

		boolean showNeighborAD = false;
		boolean showCentroidAD = true;

		for (Instance instance : inst2)
		{
			if (showCentroidAD && ad.isInside(instance))
				series2.add(instance.value(0), instance.value(1));
			else if (showNeighborAD && ad2.isInside(instance))
				series3.add(instance.value(0), instance.value(1));
			else
				series4.add(instance.value(0), instance.value(1));
		}
		for (Instance instance : inst3)
		{
			if (showNeighborAD && ad2.isInside(instance))
				series3.add(instance.value(0), instance.value(1));
			else if (showCentroidAD && ad.isInside(instance))
				series2.add(instance.value(0), instance.value(1));
			else
				series4.add(instance.value(0), instance.value(1));
		}

		//		series2.add(1.0, 7.3);
		//		series2.add(2.0, 6.8);
		//		series2.add(3.0, 9.6);
		//		series2.add(4.0, 5.6);
		XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(series1);
		dataset.addSeries(series2);
		dataset.addSeries(series3);
		dataset.addSeries(series4);

		JFreeChart chart = ChartFactory.createXYLineChart("test", "X", "Y", dataset, PlotOrientation.VERTICAL, true,
				false, false);
		XYPlot plot = (XYPlot) chart.getPlot();
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setSeriesLinesVisible(0, false);
		renderer.setSeriesShapesVisible(0, true);
		renderer.setSeriesLinesVisible(1, false);
		renderer.setSeriesShapesVisible(1, true);
		renderer.setSeriesLinesVisible(2, false);
		renderer.setSeriesShapesVisible(2, true);
		renderer.setSeriesLinesVisible(3, false);
		renderer.setSeriesShapesVisible(3, true);

		renderer.setSeriesPaint(0, Color.BLACK);
		renderer.setSeriesPaint(1, ChartColor.LIGHT_CYAN);
		renderer.setSeriesPaint(2, ChartColor.LIGHT_GREEN);
		renderer.setSeriesPaint(3, ChartColor.LIGHT_RED);

		((NumberAxis) plot.getRangeAxis()).setTickUnit(new NumberTickUnit(0.1));
		((NumberAxis) plot.getDomainAxis()).setTickUnit(new NumberTickUnit(0.1));

		plot.setRenderer(renderer);
		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(800, 800));
		af.setContentPane(chartPanel);
		af.pack();
		af.setLocationRelativeTo(null);
		af.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		if (lastFrame != null)
			SwingUtil.waitWhileVisible(lastFrame);
		af.setVisible(true);
		lastFrame = af;
	}

	public static void main(String args[]) throws Exception
	{
		Random r = new Random();
		int n = 20;
		int n_out = 1;
		double x[] = new double[] { r.nextDouble(), r.nextDouble() };
		double minX = Math.min(x[0], x[1]);
		double maxX = Math.max(x[0], x[1]);
		double y[] = new double[] { r.nextDouble(), r.nextDouble() };
		double minY = Math.min(y[0], y[1]);
		double maxY = Math.max(y[0], y[1]);

		StringLineAdder s = new StringLineAdder();
		s.add("@RELATION test");
		s.add("");
		s.add("@ATTRIBUTE x REAL");
		s.add("@ATTRIBUTE y REAL");
		s.add("");
		s.add("@DATA");
		for (int i = 0; i < n; i++)
			s.add((minX + r.nextDouble() * (maxX - minX)) + "," + (minY + r.nextDouble() * (maxY - minY)));
		for (int i = 0; i < n_out; i++)
			s.add(r.nextDouble() + "," + r.nextDouble());
		ArffReader rand = new ArffReader(new BufferedReader(new StringReader(s.toString())));
		Instances data = rand.getData();

		CentroidBasedApplicabilityDomain ad = new CentroidBasedApplicabilityDomain();
		ad.debug = true;
		ad.setContinous(true);
		ad.init(data);
		showDistHistogramm("Testdata", ad, ad.getDistance(data.get(0)));
		if (true == true)
			main(null);

		int n2 = 200;
		StringLineAdder s2 = new StringLineAdder();
		s2.add("@RELATION test");
		s2.add("");
		s2.add("@ATTRIBUTE x REAL");
		s2.add("@ATTRIBUTE y REAL");
		s2.add("");
		s2.add("@DATA");
		for (int i = 0; i < n2; i++)
		{
			s2.add(r.nextDouble() + "," + r.nextDouble());
		}
		ArffReader rand2 = new ArffReader(new BufferedReader(new StringReader(s2.toString())));
		Instances data2 = rand2.getData();

		int n3 = 200;
		StringLineAdder s3 = new StringLineAdder();
		s3.add("@RELATION test");
		s3.add("");
		s3.add("@ATTRIBUTE x REAL");
		s3.add("@ATTRIBUTE y REAL");
		s3.add("");
		s3.add("@DATA");
		for (int i = 0; i < n3; i++)
		{
			s3.add(r.nextDouble() + "," + r.nextDouble());
		}
		ArffReader rand3 = new ArffReader(new BufferedReader(new StringReader(s3.toString())));
		Instances data3 = rand3.getData();

		show(data, data2, data3);

		main(null);

		//		PairwiseDistanceBasedApplicabilityDomain.DEBUG = true;
		//		PairwiseDistanceBasedApplicabilityDomain ad = new PairwiseDistanceBasedApplicabilityDomain();
		//		ad.init(data);
		//
		//		for (int i = 0; i < data.numInstances(); i++)
		//			ad.isInside(data.get(i));
	}
}
