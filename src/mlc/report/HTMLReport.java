package mlc.report;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import mulan.evaluation.Settings;

import org.rendersnake.HtmlAttributes;
import org.rendersnake.HtmlAttributesFactory;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import util.StringUtil;
import util.TimeFormatUtil;
import datamining.ResultSet;

public class HTMLReport extends AbstractReport
{
	HtmlCanvas html;
	String outfile;

	//	String image_dir;

	public HTMLReport(String outfile, String portalTitle, String portalHeader, String pageTitle)
	{
		this(outfile, portalTitle, portalHeader, pageTitle, true);
	}

	public HTMLReport(String outfile, String portalTitle, String portalHeader, String pageTitle, boolean wide)
	{
		//		super(outfile, title);
		this.outfile = outfile;
		//		image_dir = outfile.replaceAll(".html", "") + "_images/";

		try
		{
			html = new HtmlCanvas().html();
			html.head();
			String loc = "";
			for (int i = 0; i < StringUtil.numOccurences(outfile, "/"); i++)
				loc += "../";
			html.macros().stylesheet(loc + Settings.cssFile());
			html.title().content(pageTitle + " - " + portalTitle);
			//			html.write("<script type=\"text/javascript\">" + "function toggle(control){"
			//					+ "var elem = document.getElementById(control);" + "if(elem.style.display == \"none\"){"
			//					+ "	elem.style.display = \"block\";" + "}else{" + "	elem.style.display = \"none\";" + "}"
			//					+ "}</script>", HtmlCanvas.NO_ESCAPE);

			html._head();
			html.body();
			html.div(HtmlAttributesFactory.id("header")).write(portalHeader, HtmlCanvas.NO_ESCAPE);
			html._div();
			html.div(HtmlAttributesFactory.id(wide ? "wide-content" : "content"));
			newSection(pageTitle);
		}
		catch (Exception e)
		{
			throw new Error(e);
		}
	}

	public void close(String footer)
	{
		try
		{
			html._div();
			html._body();
			html.footer().write("Created at " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
			html.write(footer, HtmlCanvas.NO_ESCAPE);
			html._footer();
			html._html();
			//System.out.println(html.toHtml());

			BufferedWriter buffy = new BufferedWriter(new FileWriter(outfile));
			buffy.write(html.toHtml());
			buffy.close();
			System.out.println("wrote report to " + outfile);
		}
		catch (Exception e)
		{
			throw new Error(e);
		}
	}

	class JSmolPlugin implements Renderable
	{
		String url;

		public JSmolPlugin(String url)
		{
			this.url = url;
		}

		@Override
		public void renderOn(HtmlCanvas html) throws IOException
		{
			html.write("<script type=\"text/javascript\" src=\"lib/JSmol.lite.js\"></script>", HtmlCanvas.NO_ESCAPE);
			html.write("<script type=\"text/javascript\">\nvar Info;\n" + "\n" + ";(function() {\n" + "\n" + "\n"
					+ "Info = {\n" + " border: 1,\n width: 300,\n" + "	height: 300,\n" + "	debug: false,\n"
					+ "	color: \"white\",\n" + "	addSelectionOptions: false,\n" + "	serverURL: \"none\",\n"
					+ "	use: \"HTML5\",\n" + "	readyFunction: null,\n" + "	src: \"" + url + "\",\n"
					+ "    bondWidth: 3,\n" + "    zoomScaling: 1.5,\n" + "    pinchScaling: 2.0,\n"
					+ "    mouseDragFactor: 0.5,\n" + "    touchDragFactor: 0.15,\n" + "    multipleBondSpacing: 4,\n"
					+ "    spinRateX: 0.2,\n" + "    spinRateY: 0.5,\n" + "    spinFPS: 20,\n" + "    spin:false,\n"
					+ "    debug: false\n" + "}\n" + "\n" + "})();\n</script>", HtmlCanvas.NO_ESCAPE);
			html.write(
					"<a href=\"javascript:jmol.spin(true)\">spin ON</a> <a href=\"javascript:jmol.spin(false)\">OFF</a>",
					HtmlCanvas.NO_ESCAPE);
			html.script().content("Jmol.getTMApplet(\"jmol\", Info)", HtmlCanvas.NO_ESCAPE);
		}
	}

	public JSmolPlugin getJSmolPlugin(String url)
	{
		return new JSmolPlugin(url);
	}

	//	public void addToggleImage(String toggleLinkTitle, String path)
	//	{
	//		try
	//		{
	//			body.render(new Toggler(toggleLinkTitle, new Image(path)));
	//		}
	//		catch (IOException e)
	//		{
	//			e.printStackTrace();
	//		}
	//	}
	//
	//	public static int toggleCount = 0;
	//
	//	public static class Toggler implements Renderable
	//	{
	//		String toggleLinkTitle;
	//		Renderable renderable;
	//
	//		//		public Toggler(String toggleLinkTitle)
	//		//		{
	//		//			this.toggleLinkTitle = toggleLinkTitle;
	//		//		}
	//
	//		public Toggler(String toggleLinkTitle, Renderable renderable)
	//		{
	//			this.toggleLinkTitle = toggleLinkTitle;
	//			this.renderable = renderable;
	//		}
	//
	//		public void renderOn(HtmlCanvas html) throws IOException
	//		{
	//			toggleCount++;
	//			html.a(HtmlAttributesFactory.href("javascript:toggle(\"toggle" + toggleCount + "\")")).content(
	//					toggleLinkTitle);
	//
	//			//html.div(HtmlAttributesFactory.style("display: none").id("toggle" + toggleCount)).write(content)._div();
	//			html.div(HtmlAttributesFactory.style("display: none").id("toggle" + toggleCount));
	//			html.render(renderable);
	//			html._div();
	//
	//		}
	//	}

	private static String LINK_WRAP = ".lnk!.";
	private static String LINK_SEP = "-dsc#-";

	public Image getImage(String url)
	{
		return new Image(url);
	}

	public Image getImage(String url, int width, int height)
	{
		return new Image(url, width, height);
	}

	public String encodeLink(String url, String text)
	{
		return LINK_WRAP + url + LINK_SEP + text + LINK_WRAP;
	}

	//	public String decodeLinks(String text)
	//	{
	//		System.err.println(text);
	//		Pattern p = Pattern.compile("!##(.*)#!#(.*)##!");
	//		Matcher m = p.matcher(text);
	//		String s = m.replaceAll("<a href=\"$1\">$2</a>");
	//		System.err.println(s);
	//		return s.toString();
	//	}

	public static class TextWithLinks implements Renderable
	{
		String text;

		//		HtmlAttributes attr;

		public TextWithLinks(String text)
		{
			this.text = text;
		}

		//		public TextWithLinks(String text, HtmlAttributes attr)
		//		{
		//			this.text = text;
		//			this.attr = attr;
		//		}

		@Override
		public void renderOn(HtmlCanvas html) throws IOException
		{
			String s[] = text.split(LINK_WRAP);
			for (String st : s)
			{
				if (st.contains(LINK_SEP))
				{
					String str[] = st.split(LINK_SEP);

					//					HtmlAttributes attribs = attr;
					//					if (attribs == null)
					//						attribs = new HtmlAttributes();
					//					attribs.href(str[0]);
					//					html.a(attribs);

					html.a(HtmlAttributesFactory.href(str[0]));
					if (str.length > 1 && str[1].length() > 1)
						html.write(str[1]);
					html._a();
				}
				else
				{
					html.write(st);
				}
			}
		}
	}

	public static void main(String[] args)
	{
		HTMLReport rep = new HTMLReport("/tmp/delme.html", "Title", "<h1>Title</h1>", "Title of this page");
		rep.newSection("Section");
		rep.addParagraph("Bla a lot of test\nmore text");

		rep.addParagraph("Bla a lot of test\nmore text");

		rep.addImage("/home/martin/workspace/JavaLib/data/ok.png");

		ResultSet set = new ResultSet();
		int idx = set.addResult();
		set.setResultValue(idx, "bla", 123);
		set.setResultValue(idx, "blub", "true " + rep.encodeLink("google.de", "a-link"));
		set.setResultValue(idx, "blob", "img");
		idx = set.addResult();
		set.setResultValue(idx, "bla", rep.getJSmolPlugin("data/test.sdf"));
		//		set.setResultValue(idx, "bla", new Toggler("hidden image", new Image(
		//				"/home/martin/workspace/JavaLib/data/ok.png")));
		set.setResultValue(idx, "blub", 456);
		rep.addTable(set);

		rep.addParagraph("here comes # the link '" + rep.encodeLink("http://google.de", "gogle") + "' <- link");

		set = new ResultSet();
		idx = set.addResult();
		set.setResultValue(idx, "bla", 123);
		set.setResultValue(idx, "blub", "true " + rep.encodeLink("google.de", "a-link"));
		set.setResultValue(idx, "blob", "img");
		idx = set.addResult();
		set.setResultValue(idx, "bla", rep.getJSmolPlugin("data/test.sdf"));
		//		set.setResultValue(idx, "bla", new Toggler("hidden image", new Image(
		//				"/home/martin/workspace/JavaLib/data/ok.png")));
		set.setResultValue(idx, "blub", 456);
		rep.addTable(set);

		rep.close("");
	}

	public static class Image implements Renderable
	{
		HtmlAttributes attributes = new HtmlAttributes();
		String img;

		public Image(String img)
		{
			this.img = img;
			this.attributes.src(img).href(img);
		}

		public Image(String img, int width, int height)
		{
			this.img = img;
			this.attributes.src(img).href(img).width(width).height(height);
		}

		public void renderOn(HtmlCanvas html) throws IOException
		{
			html.a(HtmlAttributesFactory.href(img)).img(attributes)._a();
		}

		public Image alt(String alt)
		{
			this.attributes.alt(alt);
			return this;
		}
	}

	private static HtmlAttributes getAnker(String name)
	{
		return HtmlAttributesFactory.id(name.toLowerCase().replaceAll(" ", "-"));
	}

	public void newSection(String title)
	{
		try
		{
			html.h2(getAnker(title)).content(title);
		}
		catch (IOException e)
		{
			throw new Error(e);
		}
	}

	public void addParagraph(String text)
	{
		try
		{
			html.div().render(new TextWithLinks(text))._div();
		}
		catch (IOException e)
		{
			throw new Error(e);
		}
	}

	public void addImage(String file)
	{
		try
		{
			//					String f = image_dir + file.getName();
			//					FileUtil.createParentFolders(f);
			//					FileUtil.copy(file, new File(f));
			//					file.delete();
			html.div().render(new Image(file))._div();
		}
		catch (IOException e)
		{
			throw new Error(e);
		}
	}

	public void addSmallImages(String[] smallImages)
	{
		try
		{
			HtmlCanvas table = html.table();
			int i = 0;
			for (String file : smallImages)
			{
				//					String f = image_dir + file.getName();
				//					FileUtil.createParentFolders(f);
				//					FileUtil.copy(file, new File(f));
				//					file.delete();
				if (i % 2 == 0)
					table.tr().td().render(new Image(file))._td();
				else
					table.td().render(new Image(file))._td()._tr();
				i++;
			}
			table._table();
		}
		catch (IOException e)
		{
			throw new Error(e);
		}

	}

	//	public void addSection(String title, String content, ResultSet[] tables, String[] images, String[] smallImages)
	//	{
	//		newSection(title);
	//		if (content != null && content.length() > 0)
	//			addParagraph(content);
	//
	//		if (tables != null && tables.length > 0)
	//			for (ResultSet rs : tables)
	//				addTable(rs);
	//
	//		if (images != null && images.length > 0)
	//			for (String file : images)
	//				addImage(file);
	//
	//		if (smallImages != null && smallImages.length > 0)
	//			addSmallImages(smallImages);
	//	}

	private static class TableHeader extends TableData
	{
		public TableHeader(String prop, Object val, boolean format)
		{
			super(prop, val, format);
		}

		protected void init(HtmlCanvas html) throws IOException
		{
			html.th(attr);
		}

		protected void end(HtmlCanvas html) throws IOException
		{
			html._th();
		}
	}

	private static class TableData implements Renderable
	{
		Object val;
		String prop;
		HtmlAttributes attr;

		public TableData(String prop, Object val, boolean format)
		{
			this.prop = prop;
			this.val = val;

			if (format)
			{
				if (val != null && val.toString().matches(".*inactive.*"))
				{
					//					if (val.toString().matches(".*low.*"))
					//						attr = HtmlAttributesFactory.class_("inactive_low");
					//					else if (val.toString().matches(".*medium.*"))
					//						attr = HtmlAttributesFactory.class_("inactive_medium");
					//					else
					attr = HtmlAttributesFactory.class_("inactive");
				}
				else if (val != null && val.toString().matches(".*active.*"))
				{
					//					if (val.toString().matches(".*low.*"))
					//						attr = HtmlAttributesFactory.class_("active_low");
					//					else if (val.toString().matches(".*medium.*"))
					//						attr = HtmlAttributesFactory.class_("active_medium");
					//					else
					attr = HtmlAttributesFactory.class_("active");
				}
				else if (val != null && val.toString().matches(".*missing.*"))
					attr = HtmlAttributesFactory.class_("missing");
				else if (val != null && val.toString().matches(".*outside.*"))
					attr = HtmlAttributesFactory.class_("outside");
			}
		}

		protected void init(HtmlCanvas html) throws IOException
		{
			html.td(attr);
		}

		protected void end(HtmlCanvas html) throws IOException
		{
			html._td();
		}

		@Override
		public void renderOn(HtmlCanvas html) throws IOException
		{
			init(html);
			if (val != null)
			{
				if (prop.contains("runtime") && !val.toString().contains("runtime"))
					html.write(TimeFormatUtil.format(((Double) val).longValue()));
				else if (val instanceof Renderable)
					html.render((Renderable) val);
				else if (val instanceof Double)
					html.write(StringUtil.formatDouble((Double) val, 3));
				else if (val instanceof String)
					html.render(new TextWithLinks((String) val));
				else
					html.write(val + "");
			}
			end(html);
		}
	}

	//	private static void setCell(HtmlCanvas table, String p, Object val) throws IOException
	//	{
	//		table.td();
	//
	//		if (val == null)
	//			table.write("");
	//		else
	//		{
	//			HtmlAttributes attr = null;
	//			if (val.toString().matches(".*inactive.*"))
	//			{
	//				if (val.toString().matches(".*low.*"))
	//					table.attributes().class_("inactive_low");
	//				else if (val.toString().matches(".*medium.*"))
	//					attr = HtmlAttributesFactory.class_("inactive_medium");
	//				else
	//					attr = HtmlAttributesFactory.class_("inactive");
	//			}
	//			else if (val.toString().matches(".*active.*"))
	//			{
	//				if (val.toString().matches(".*low.*"))
	//					attr = HtmlAttributesFactory.class_("active_low");
	//				else if (val.toString().matches(".*medium.*"))
	//					attr = HtmlAttributesFactory.class_("active_medium");
	//				else
	//					table.attributes().class_("active");
	//			}
	//			if (val.toString().matches(".*missing.*"))
	//				attr = HtmlAttributesFactory.class_("missing");
	//
	//		}
	//
	//		table._td();
	//	}

	public void addTable(ResultSet rs)
	{
		addTable(rs, null);
	}

	public void addTable(ResultSet rs, Boolean transpose)
	{
		addTable(rs, transpose, null);
	}

	public void addTable(ResultSet rs, Boolean transpose, String title)
	{
		addTable(rs, transpose, title, true);
	}

	public void addTable(ResultSet rs, Boolean transpose, String title, boolean format)
	{

		//		.tr().th().content("City").th().content("Country")._tr().tr().td().content("Amsterdam").td()
		//				.content("The Netherlands")._tr()._table();

		if (title != null)
		{
			try
			{
				html.h4().content(title);
			}
			catch (IOException e)
			{
				throw new Error(e);
			}
		}

		try
		{
			HtmlCanvas table = html.table();
			if ((transpose == null && rs.getProperties().size() > 8 && rs.getProperties().size() > rs.getNumResults() + 1)
					|| (transpose != null && transpose))
			{
				//transpose, header is first column
				for (String p : rs.getProperties())
				{
					String niceP = rs.getNiceProperty(p);
					table.tr();
					table.render(new TableHeader(niceP, niceP, format));
					for (int i = 0; i < rs.getNumResults(); i++)
					{
						Object val = rs.getResultValue(i, p);
						table.render(new TableData(niceP, val, format));
						//setCell(table, p, val);
					}
					table._tr();
				}
			}
			else
			{
				table.tr();
				for (String p : rs.getProperties())
				{
					String niceP = rs.getNiceProperty(p);
					table.render(new TableHeader(niceP, niceP, format));
				}
				table._tr();
				for (int i = 0; i < rs.getNumResults(); i++)
				{
					table.tr(getAnker(rs.getResultValue(i, rs.getProperties().get(0)) + ""));
					for (String p : rs.getProperties())
					{
						String niceP = rs.getNiceProperty(p);
						Object val = rs.getResultValue(i, p);
						table.render(new TableData(niceP, val, format));
						//setCell(table, p, val);
					}
					table._tr();
				}
			}
			table._table();
		}
		catch (Exception e)
		{
			throw new Error(e);
		}
	}

	//	public void newParagraph()
	//	{
	//		try
	//		{
	//			body.div();
	//		}
	//		catch (IOException e)
	//		{
	//			e.printStackTrace();
	//		}
	//	}
	//
	//	public void addText(String string)
	//	{
	//		try
	//		{
	//			body.write(string);
	//		}
	//		catch (IOException e)
	//		{
	//			e.printStackTrace();
	//		}
	//	}
	//
	//	public void addLink(String url, String text)
	//	{
	//		try
	//		{
	//			body.a(HtmlAttributesFactory.href(url)).content(text);
	//		}
	//		catch (IOException e)
	//		{
	//			e.printStackTrace();
	//		}
	//	}
	//
	//	public void closeParagraph()
	//	{
	//		try
	//		{
	//			body._div();
	//		}
	//		catch (IOException e)
	//		{
	//			e.printStackTrace();
	//		}
	//	}

	public void addGap()
	{
		try
		{
			html.br();
		}
		catch (IOException e)
		{
			throw new Error(e);
		}
	}

	public void newSubsection(String string)
	{
		try
		{
			html.h3(getAnker(string)).content(string);
		}
		catch (IOException e)
		{
			throw new Error(e);
		}
	}

}
