import rep.RepBlk;
import java.io.Writer;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class RepBlkTest {

	public static void main(String[] args) throws IOException {
		test1ProgressiveOutput(); printOk("test1ProgressiveOutput");
		test2ReuseMainBlock(); printOk("test2ReuseMainBlock");
		test3NestedBlocks(); printOk("test3NestedBlocks");
		test4Variables(); printOk("test4Variables");
		test5Complete(); printOk("test5Complete");
		test6BasicExceptions(); printOk("test6BasicExceptions");
		test7OrderExceptions(); printOk("test7OrderExceptions");
		test8VariablesExceptions(); printOk("test8VariablesExceptions");
	}

	private static void printOk(String testName) {
		System.err.println("OK: " + testName);
	}

	private static void assertTrue(boolean actual) {
		if (! actual)
			throw new IllegalStateException("not true!");
	}

	private static void assertEquals(String expected, String actual) {
		if (! actual.equals(expected))
			throw new IllegalStateException("\nexpected: " +
					expected + "\nactual: " + actual);
	}

	private static void assertEquals(int expected, int actual) {
		if (actual != expected)
			throw new IllegalStateException("\nexpected: " +
					expected + "\nactual: " + actual);
	}

	public static void test1ProgressiveOutput() throws IOException {

		String tpl;
		RepBlk page;
		CharArrayWriter out;
		String expected;

		tpl = "" +
			"<html>\n" +
			"<body>\n" +
			"<!--rep blk=b-->\n" +
			"<p>block</p>\n" +
			"<!--/rep-->\n" +
			"<ul>\n" +
			"<!--rep blk=ls-->\t<li>item</li>\n" +
			"<!--/rep--></ul>\n" +
			"</body>\n" +
			"</html>\n" +
			"";

		page = new RepBlk(tpl);
		out = new CharArrayWriter();

		expected = "" +
			"";
		assertEquals(expected, out.toString());

		page.start(out);

		expected = "" +
			"<html>\n" +
			"<body>\n" +
			"";
		assertEquals(expected, out.toString());

		RepBlk b0 = page.getBlk("b");

		assertEquals(expected, out.toString());

		b0.start();

		expected = "" +
			"<html>\n" +
			"<body>\n" +
			"\n" +
			"<p>block</p>\n" +
			"";
		assertEquals(expected, out.toString());

		b0.start();

		expected = "" +
			"<html>\n" +
			"<body>\n" +
			"\n" +
			"<p>block</p>\n" +
			"\n" +
			"<p>block</p>\n" +
			"";
		assertEquals(expected, out.toString());

		page.next();

		expected = "" +
			"<html>\n" +
			"<body>\n" +
			"\n" +
			"<p>block</p>\n" +
			"\n" +
			"<p>block</p>\n" +
			"\n" +
			"<ul>\n" +
			"";
		assertEquals(expected, out.toString());

		RepBlk b1 = page.getBlk("ls");

		assertEquals(expected, out.toString());

		b1.start();

		expected = "" +
			"<html>\n" +
			"<body>\n" +
			"\n" +
			"<p>block</p>\n" +
			"\n" +
			"<p>block</p>\n" +
			"\n" +
			"<ul>\n" +
			"\t<li>item</li>\n" +
			"";
		assertEquals(expected, out.toString());

		b1.start();

		expected = "" +
			"<html>\n" +
			"<body>\n" +
			"\n" +
			"<p>block</p>\n" +
			"\n" +
			"<p>block</p>\n" +
			"\n" +
			"<ul>\n" +
			"\t<li>item</li>\n" +
			"\t<li>item</li>\n" +
			"";
		assertEquals(expected, out.toString());

		b1.start();

		expected = "" +
			"<html>\n" +
			"<body>\n" +
			"\n" +
			"<p>block</p>\n" +
			"\n" +
			"<p>block</p>\n" +
			"\n" +
			"<ul>\n" +
			"\t<li>item</li>\n" +
			"\t<li>item</li>\n" +
			"\t<li>item</li>\n" +
			"";
		assertEquals(expected, out.toString());

		page.next();

		expected = "" +
			"<html>\n" +
			"<body>\n" +
			"\n" +
			"<p>block</p>\n" +
			"\n" +
			"<p>block</p>\n" +
			"\n" +
			"<ul>\n" +
			"\t<li>item</li>\n" +
			"\t<li>item</li>\n" +
			"\t<li>item</li>\n" +
			"</ul>\n" +
			"</body>\n" +
			"</html>\n" +
			"";
		assertEquals(expected, out.toString());

		page.end();
	}

	public static void test2ReuseMainBlock() throws IOException {

		String tpl;
		RepBlk page;
		CharArrayWriter out;
		String expected;

		tpl = "" +
			"<html>\n" +
			"<body>\n" +
			"<!--rep blk=b-->\n" +
			"<p>block</p>\n" +
			"<!--/rep-->\n" +
			"<ul>\n" +
			"<!--rep blk=ls-->\t<li>item</li>\n" +
			"<!--/rep--></ul>\n" +
			"</body>\n" +
			"</html>\n" +
			"";

		page = new RepBlk(tpl);
		out = new CharArrayWriter();
		page.start(out);
		page.getBlk("b").start();
		page.next();
		page.getBlk("ls").skip();
		page.next();

		expected = "" +
			"<html>\n" +
			"<body>\n" +
			"\n" +
			"<p>block</p>\n" +
			"\n" +
			"<ul>\n" +
			"</ul>\n" +
			"</body>\n" +
			"</html>\n" +
			"";
		assertEquals(expected, out.toString());

		out = new CharArrayWriter();
		page.start(out);
		page.getBlk("b").start();

		expected = "" +
			"<html>\n" +
			"<body>\n" +
			"\n" +
			"<p>block</p>\n" +
			"";
		assertEquals(expected, out.toString());

		//the initial block can be restarted when stopping at any point:
		out = new CharArrayWriter();
		page.start(out);
		page.getBlk("b").skip();
		page.next();
		page.getBlk("ls").skip();
		page.next();

		expected = "" +
			"<html>\n" +
			"<body>\n" +
			"\n" +
			"<ul>\n" +
			"</ul>\n" +
			"</body>\n" +
			"</html>\n" +
			"";
		assertEquals(expected, out.toString());

		out = new CharArrayWriter();
		page.start(out);
		page.getBlk("b").skip();
		page.next();
		page.getBlk("ls").start();
		page.getBlk("ls").start();
		page.getBlk("ls").start();
		page.getBlk("ls").start();
		page.next();

		expected = "" +
			"<html>\n" +
			"<body>\n" +
			"\n" +
			"<ul>\n" +
			"\t<li>item</li>\n" +
			"\t<li>item</li>\n" +
			"\t<li>item</li>\n" +
			"\t<li>item</li>\n" +
			"</ul>\n" +
			"</body>\n" +
			"</html>\n" +
			"";
		assertEquals(expected, out.toString());

		page.end();
	}

	public static void test3NestedBlocks() throws IOException {

		String tpl;
		RepBlk page;
		CharArrayWriter out;
		String expected;

		tpl = "" +
			"<html>\n" +
			"<body>\n" +
			"<!--rep blk=b-->\n" +
			"<ul>\n" +
			"<!--rep blk=ls-->\t<li>item</li>\n" +
			"<!--/rep--></ul>\n" +
			"<p><!--rep blk=lt-->text <!--/rep--></p>\n" +
			"<!--/rep-->\n" +
			"</body>\n" +
			"</html>\n" +
			"";

		page = new RepBlk(tpl);
		out = new CharArrayWriter();
		RepBlk b = page.getBlk("b");
		RepBlk ls = b.getBlk("ls");
		RepBlk lt = b.getBlk("lt");

		assertTrue(page.getBlk("bb") == null); //non-existing block

		assertEquals("b", joinStringList(page.getBlkNames(), ","));
		assertEquals("", joinStringSet(page.getVarNames(), ","));

		assertEquals("ls,lt", joinStringList(b.getBlkNames(), ","));
		assertEquals("", joinStringSet(b.getVarNames(), ","));

		assertEquals("", joinStringList(ls.getBlkNames(), ","));
		assertEquals("", joinStringSet(ls.getVarNames(), ","));

		assertEquals("", joinStringList(lt.getBlkNames(), ","));
		assertEquals("", joinStringSet(lt.getVarNames(), ","));

		page.start(out);

		b.start();
		ls.start();
		ls.start();
		b.next();
		lt.skip();
		b.next();

		b.start();
		ls.start();
		b.next();
		lt.start();
		lt.start();
		lt.start();
		lt.start();
		b.next();

		b.start();
		ls.skip();
		b.next();
		lt.skip();
		b.next();

		page.next();

		expected = "" +
			"<html>\n" +
			"<body>\n" +

			"\n" +
			"<ul>\n" +
			"\t<li>item</li>\n" +
			"\t<li>item</li>\n" +
			"</ul>\n" +
			"<p></p>\n" +

			"\n" +
			"<ul>\n" +
			"\t<li>item</li>\n" +
			"</ul>\n" +
			"<p>text text text text </p>\n" +

			"\n" +
			"<ul>\n" +
			"</ul>\n" +
			"<p></p>\n" +

			"\n" +
			"</body>\n" +
			"</html>\n" +
			"";
		assertEquals(expected, out.toString());

		page.end();
	}

	public static void test4Variables() throws IOException {

		String tpl;
		RepBlk page;
		CharArrayWriter out;
		String expected;

		tpl = "" +
			"<html>\n" +
			"<body>\n" +
			"<!--rep var=a      place='a\"a'-->a\"a\n" +
			"<!--rep var=aa     place='\"\"\"'-->\"\"\"\n" +
			"<!--rep var=b      place='\"b'-->\"b\n" +
			"<!--rep var=bb     place='b\"'-->b\"\n" +
			"<!--rep var=c      place=\"c'c\"-->c'c\n" +
			"<!--rep var=cc     place=\"'''\"-->'''\n" +
			"<!--rep var=d      place=\"'d\"-->'d\n" +
			"<!--rep var=dd     place=\"d'\"-->d'\n" +
			"<!--rep var=\"e\"  place=E-->E\n" +
			"<!--rep var='ee'   place=EE-->EE\n" +
			"<!--rep var='f'    place='F=\"\"'-->F=\"\"\n" +
			"<!--rep var=\"ff\" place=\"FF=''\"-->FF=''\n" +
			"<!--rep var='g'    place=\" GG \"--> GG \n" +
			"<!--rep var=\"gg\" place=\"G G\"-->G G\n" +
			"<!--rep var=ggg    place=\"  \"-->  \n" +
			"<!--rep var='h'    place=' HH '--> HH \n" +
			"<!--rep var=\"hh\" place='H H'-->H H\n" +
			"<!--rep var=hhh    place=' - '--> - \n" +
			"<!--rep var=i      place=I1  -->I1\n" +
			"<!--rep var=j      place='J1'  -->J1\n" +
			"<!--rep var=k      place=\"K1\"  -->K1\n" +
			"<!--rep\n var=l\n place=L1\n -->L1\n" +
			"<!--rep \nvar=ll \nplace=L2 \n-->L2\n" +
			"<!--rep \n var=lll \n place=L3 \n -->L3\n" +
			"</body>\n" +
			"</html>\n" +
			"";

		page = new RepBlk(tpl);
		out = new CharArrayWriter();

		assertEquals("", joinStringList(page.getBlkNames(), ","));
		expected = "a,aa,b,bb,c,cc,d,dd,e,ee,f,ff" +
				",g,gg,ggg,h,hh,hhh,i,j,k,l,ll,lll";
		assertEquals(expected, joinStringSet(page.getVarNames(), ","));

		page.setVar("ZZZ", "???"); //non-existing variable

		int i = 1;
		page.setVar("a","" + (i++));
		page.setVar("aa","" + (i++));
		page.setVar("b","" + (i++));
		page.setVar("bb","" + (i++));
		page.setVar("c","" + (i++));
		page.setVar("cc","" + (i++));
		page.setVar("d","" + (i++));
		page.setVar("dd","" + (i++));
		page.setVar("e","" + (i++));
		page.setVar("ee","" + (i++));
		page.setVar("f","" + (i++));
		page.setVar("ff","" + (i++));
		page.setVar("g","" + (i++));
		page.setVar("gg","" + (i++));
		page.setVar("ggg","" + (i++));
		page.setVar("h","" + (i++));
		page.setVar("hh","" + (i++));
		page.setVar("hhh","" + (i++));
		page.setVar("i","" + (i++));
		page.setVar("j","" + (i++));
		page.setVar("k","" + (i++));
		page.setVar("l","" + (i++));
		page.setVar("ll","" + (i++));
		page.setVar("lll","" + (i++));

		i = 1;
		expected = "" +
			"<html>\n" +
			"<body>\n" +
			(i++)+"\n"+(i++)+"\n"+(i++)+"\n"+(i++)+"\n"+(i++)+"\n" +
			(i++)+"\n"+(i++)+"\n"+(i++)+"\n"+(i++)+"\n"+(i++)+"\n" +
			(i++)+"\n"+(i++)+"\n"+(i++)+"\n"+(i++)+"\n"+(i++)+"\n" +
			(i++)+"\n"+(i++)+"\n"+(i++)+"\n"+(i++)+"\n"+(i++)+"\n" +
			(i++)+"\n"+(i++)+"\n"+(i++)+"\n"+(i++)+"\n" +
			"</body>\n" +
			"</html>\n" +
			"";

		page.start(out);

		assertEquals(expected, out.toString());

		page.end();
	}

	public static void test5Complete() throws IOException {

		String tpl;
		RepBlk page;
		CharArrayWriter out;
		String expected;

		tpl = "" +
			"<!--rep var=t place=\"Test page\"-->\n" +
			"<html>\n" +
			"<head><title>Test page</title></head>\n" +
			"<body>\n" +
			"<h1>Test page</h1>\n" +
			"<!--rep blk=b-->\n" +
			"<!--rep var=n place=1000-->\n" +
			"<b>List of</b>: 1000 elements\n" +
			"<ul>\n" +
			"<!--rep blk=li-->\n" +
			"<!--rep var=i place=item-->" +
				"<!--rep var=c place=red-->\n" +
			"\t<li style=\"color: red\">item</li>\n" +
			"<!--/rep--></ul>\n" +
			"(1000 elements)\n" +
			"<p><!--rep blk=la--><!--rep var=t place=text-->" +
				"<!--rep var=s place=\" \"-->" +
				"text <!--/rep--></p>\n" +
			"<!--/rep-->\n" +
			"</body>\n" +
			"</html>\n" +
			"";

		File file = new File("TestRepBlk-temp.rep.html");
		file.delete(); //ensures that is created
		FileWriter writer = new FileWriter(file);
		writer.write(tpl);
		writer.flush();
		writer.close();

		page = new RepBlk(new FileReader(file));
		page.setVar("t", "My lists");
		RepBlk b = page.getBlk("b");
		RepBlk li = b.getBlk("li");
		RepBlk la = b.getBlk("la").setVar("s", " - ");
		out = new CharArrayWriter();
		page.start(out);

		assertEquals("b", joinStringList(page.getBlkNames(), ","));
		assertEquals("t", joinStringSet(page.getVarNames(), ","));

		assertEquals("li,la", joinStringList(b.getBlkNames(), ","));
		assertEquals("n", joinStringSet(b.getVarNames(), ","));

		assertEquals("", joinStringList(li.getBlkNames(), ","));
		assertEquals("c,i", joinStringSet(li.getVarNames(), ","));

		assertEquals("", joinStringList(la.getBlkNames(), ","));
		assertEquals("s,t", joinStringSet(la.getVarNames(), ","));

		b.setVar("n", "3").start();
		li.setVar("c", "green");
		li.setVar("i", "First").start();
		li.setVar("i", "Second").start();
		li.setVar("i", "Third").start();
		b.next();
		la.setVar("t", "Text");
		la.start();
		la.start();
		la.start();
		la.start();
		la.setVar("s", "").start();
		b.next();

		b.setVar("n", "2").start();
		li.setVar("c", "blue");
		li.setVar("i", "One").start();
		li.setVar("i", "Two").start();
		b.next();
		la.setVar("s", " / ");
		la.setVar("t", "Text A").start();
		la.setVar("t", "Text B").start();
		la.setVar("t", "Text C").start();
		la.setVar("t", "Text D").setVar("s", "").start();
		b.next();

		b.setVar("n", "0").start();
		li.skip();
		b.next();
		la.skip();
		b.next();

		page.next();

		expected = "" +
			"\n" +
			"<html>\n" +
			"<head><title>My lists</title></head>\n" +
			"<body>\n" +
			"<h1>My lists</h1>\n" +

			"\n" +
			"\n" +
			"<b>List of</b>: 3 elements\n" +
			"<ul>\n" +
			"\n" +
			"\n" +
			"\t<li style=\"color: green\">First</li>\n" +
			"\n" +
			"\n" +
			"\t<li style=\"color: green\">Second</li>\n" +
			"\n" +
			"\n" +
			"\t<li style=\"color: green\">Third</li>\n" +
			"</ul>\n" +
			"(3 elements)\n" +
			"<p>Text - Text - Text - Text - Text</p>\n" +

			"\n" +
			"\n" +
			"<b>List of</b>: 2 elements\n" +
			"<ul>\n" +
			"\n" +
			"\n" +
			"\t<li style=\"color: blue\">One</li>\n" +
			"\n" +
			"\n" +
			"\t<li style=\"color: blue\">Two</li>\n" +
			"</ul>\n" +
			"(2 elements)\n" +
			"<p>Text A / Text B / Text C / Text D</p>\n" +

			"\n" +
			"\n" +
			"<b>List of</b>: 0 elements\n" +
			"<ul>\n" +
			"</ul>\n" +
			"(0 elements)\n" +
			"<p></p>\n" +

			"\n" +
			"</body>\n" +
			"</html>\n" +
			"";
		assertEquals(expected, out.toString());

		page.end();

		file.delete();
	}

	public static void test6BasicExceptions() throws IOException {

		String tpl;

		tpl = "<!--rep blk=''--><!--/rep-->\n";
		tryNew(tpl, "Empty blk attribute in Rep tag"); //01

		tpl = "<!--rep var='' place=SEA-->SEA,SEA\n";
		tryNew(tpl, "Empty var attribute in Rep tag"); //02

		tpl = "<!--rep--><!--/rep-->\n";
		tryNew(tpl, "Missing blk or var attribute in Rep tag"); //03

		tpl = "<!--rep test=TEST--><!--/rep-->\n";
		tryNew(tpl, "Missing blk or var attribute in Rep tag");

		tpl = "<!--rep var=q place=Q blk=p--><!--/rep-->Q\n";
		tryNew(tpl, "Found blk and var attributes in Rep tag"); //04

		tpl = "<!--rep var=carrot-->\n";
		tryNew(tpl, "Missing or empty place attribute in Rep tag"); //05

		tpl = "<!--rep var=carrot place=''-->\n";
		tryNew(tpl, "Missing or empty place attribute in Rep tag");

		tpl = "<!--rep var=a place=A--><!--rep var=b place=B--> B B \n";
		tryNew(tpl, "One or more variables not found in block: a"); //06

		tpl = "<!--rep var=c place=C--><!--rep var=d place=D-->\n";
		tryNew(tpl, "One or more variables not found in block: c, d");

		tpl = "<!--rep var=e place=A--><!--rep blk=b-->A<!--/rep-->\n";
		tryNew(tpl, "One or more variables not found in block: e");

		tpl = "1<!--/rep-->2\n";
		tryNew(tpl, "Found Rep tag closing a block not opened"); //07

		tpl = "3<!--rep blk=d-->4<!--/rep-->5<!--/rep-->6\n";
		tryNew(tpl, "Found Rep tag closing a block not opened");

		tpl = "<!--rep blk=e-->\n";
		tryNew(tpl, "One or more blocks have not been closed: e"); //08

		tpl = "<!--rep blk=f--><!--rep blk=g-->\n";
		tryNew(tpl, "One or more blocks have not been closed: g");

		tpl = "A<!--rep blk=h-->B<!--rep blk=i-->C<!--/rep-->D\n";
		tryNew(tpl, "One or more blocks have not been closed: h");

		tpl = "E<!--rep blk=j-->F<!--/rep-->G<!--rep blk=k-->H\n";
		tryNew(tpl, "One or more blocks have not been closed: k");

		tpl = "<!--rep blk=park--><!--/rep-->\n" +
			"<!--rep blk=park--><!--/rep-->\n";
		tryNew(tpl, "Repeated block name: park"); //09

		tpl = "<!--rep var=sun place=S-->S\n" +
			"<!--rep place=T var=sun-->T\n";
		tryNew(tpl, "Repeated variable name: sun"); //10

		tpl = "<!--rep var=potato place=PLACE var=tomato-->tomato\n";
		tryNew(tpl, "Repeated attribute in Rep tag: var"); //11

		tpl = "<!--rep var=zen attr place=PLACE-->PLACE\n";
		tryNew(tpl, "Invalid attribute format in Rep tag"); //12

		tpl = "<!--rep var=v place=V>V\n";
		tryNew(tpl, "End of comment not found"); //13

	}

	public static void test7OrderExceptions() throws IOException {

		String tpl;
		RepBlk page;
		CharArrayWriter out;
		String expected;
		String msgMethod, msgOperation, msgNext, msgEnd;

		tpl = "0\n" +
			"<!--rep blk=1-->1\n" +
				"<!--rep blk=2-->2\n" +
				"<!--/rep-->1.0\n" +
				"<!--rep blk=3-->3\n" +
				"<!--/rep-->1.1\n" +
			"<!--/rep-->0.0\n" +
			"<!--rep blk=4-->4\n" +
			"<!--/rep-->0.1\n" +
			"";

		page = new RepBlk(tpl);
		out = new CharArrayWriter();
		RepBlk[] blocks = {
			page,
			page.getBlk("1"),
			page.getBlk("1").getBlk("2"),
			page.getBlk("1").getBlk("3"),
			page.getBlk("4")
		};

		msgMethod = "This method cannot be used on this block"; //14
		msgOperation = "Operation not allowed now on this block"; //15
		msgNext = "Next not allowed because the state of child"; //16
		msgEnd = "The page has not been written completely"; //17

		tryEnd(blocks[0], msgEnd);
		tryEnd(blocks[1], msgMethod);
		tryEnd(blocks[2], msgMethod);
		tryEnd(blocks[3], msgMethod);
		tryEnd(blocks[4], msgMethod);
		//tryStart(blocks[0], out, "?");
		tryStart(blocks[1], out, msgMethod);
		tryStart(blocks[2], out, msgMethod);
		tryStart(blocks[3], out, msgMethod);
		tryStart(blocks[4], out, msgMethod);
		tryStart(blocks[0], msgMethod);
		tryStart(blocks[1], msgOperation);
		tryStart(blocks[2], msgOperation);
		tryStart(blocks[3], msgOperation);
		tryStart(blocks[4], msgOperation);
		tryNext(blocks[0], msgOperation);
		tryNext(blocks[1], msgOperation);
		tryNext(blocks[2], msgOperation);
		tryNext(blocks[3], msgOperation);
		tryNext(blocks[4], msgOperation);
		trySkip(blocks[0], msgMethod);
		trySkip(blocks[1], msgOperation);
		trySkip(blocks[2], msgOperation);
		trySkip(blocks[3], msgOperation);
		trySkip(blocks[4], msgOperation);
		blocks[0].start(out);

		tryEnd(blocks[0], msgEnd);
		//tryStart(blocks[2], "?");
		tryStart(blocks[2], msgOperation);
		tryStart(blocks[3], msgOperation);
		tryStart(blocks[4], msgOperation);
		tryNext(blocks[0], msgNext); //child not used yet
		tryNext(blocks[1], msgOperation);
		tryNext(blocks[2], msgOperation);
		tryNext(blocks[3], msgOperation);
		tryNext(blocks[4], msgOperation);
		trySkip(blocks[0], msgMethod);
		//trySkip(blocks[1], "?");
		trySkip(blocks[2], msgOperation);
		trySkip(blocks[3], msgOperation);
		trySkip(blocks[4], msgOperation);
		blocks[1].start();

		tryEnd(blocks[0], msgEnd);
		tryStart(blocks[1], msgOperation);
		//tryStart(blocks[2], "?");
		tryStart(blocks[3], msgOperation);
		tryStart(blocks[4], msgOperation);
		tryNext(blocks[0], msgNext); //child in use
		tryNext(blocks[1], msgNext); //child not used yet
		tryNext(blocks[2], msgOperation);
		tryNext(blocks[3], msgOperation);
		tryNext(blocks[4], msgOperation);
		trySkip(blocks[0], msgMethod);
		trySkip(blocks[1], msgOperation);
		//trySkip(blocks[2], "?");
		trySkip(blocks[3], msgOperation);
		trySkip(blocks[4], msgOperation);
		blocks[2].start();

		tryEnd(blocks[0], msgEnd);
		tryStart(blocks[1], msgOperation);
		//tryStart(blocks[2], "?");
		tryStart(blocks[3], msgOperation);
		tryStart(blocks[4], msgOperation);
		tryNext(blocks[0], msgNext); //child in use
		//tryNext(blocks[1], "?");
		tryNext(blocks[2], msgOperation);
		tryNext(blocks[3], msgOperation);
		tryNext(blocks[4], msgOperation);
		trySkip(blocks[0], msgMethod);
		trySkip(blocks[1], msgOperation);
		trySkip(blocks[2], msgOperation);
		trySkip(blocks[3], msgOperation);
		trySkip(blocks[4], msgOperation);
		blocks[2].start(); //restart

		tryEnd(blocks[0], msgEnd);
		tryStart(blocks[1], msgOperation);
		//tryStart(blocks[2], "?");
		tryStart(blocks[3], msgOperation);
		tryStart(blocks[4], msgOperation);
		tryNext(blocks[0], msgNext); //child in use
		//tryNext(blocks[1], "?");
		tryNext(blocks[2], msgOperation);
		tryNext(blocks[3], msgOperation);
		tryNext(blocks[4], msgOperation);
		trySkip(blocks[0], msgMethod);
		trySkip(blocks[1], msgOperation);
		trySkip(blocks[2], msgOperation);
		trySkip(blocks[3], msgOperation);
		trySkip(blocks[4], msgOperation);
		blocks[1].next();

		tryEnd(blocks[0], msgEnd);
		tryStart(blocks[1], msgOperation);
		tryStart(blocks[2], msgOperation);
		//tryStart(blocks[3], "?");
		tryStart(blocks[4], msgOperation);
		tryNext(blocks[0], msgNext); //child in use
		tryNext(blocks[1], msgNext); //child not used yet
		tryNext(blocks[2], msgOperation);
		tryNext(blocks[3], msgOperation);
		tryNext(blocks[4], msgOperation);
		trySkip(blocks[0], msgMethod);
		trySkip(blocks[1], msgOperation);
		trySkip(blocks[2], msgOperation);
		//trySkip(blocks[3], "?");
		trySkip(blocks[4], msgOperation);
		blocks[3].skip(); //skip

		tryEnd(blocks[0], msgEnd);
		tryStart(blocks[1], msgOperation);
		tryStart(blocks[2], msgOperation);
		tryStart(blocks[3], msgOperation);
		tryStart(blocks[4], msgOperation);
		tryNext(blocks[0], msgNext); //child in use
		//tryNext(blocks[1], "?");
		tryNext(blocks[2], msgOperation);
		tryNext(blocks[3], msgOperation);
		tryNext(blocks[4], msgOperation);
		trySkip(blocks[0], msgMethod);
		trySkip(blocks[1], msgOperation);
		trySkip(blocks[2], msgOperation);
		trySkip(blocks[3], msgOperation);
		trySkip(blocks[4], msgOperation);
		blocks[1].next();

		tryEnd(blocks[0], msgEnd);
		//tryStart(blocks[1], "?");
		tryStart(blocks[2], msgOperation);
		tryStart(blocks[3], msgOperation);
		tryStart(blocks[4], msgOperation);
		//tryNext(blocks[0], "?");
		tryNext(blocks[1], msgOperation);
		tryNext(blocks[2], msgOperation);
		tryNext(blocks[3], msgOperation);
		tryNext(blocks[4], msgOperation);
		trySkip(blocks[0], msgMethod);
		trySkip(blocks[1], msgOperation);
		trySkip(blocks[2], msgOperation);
		trySkip(blocks[3], msgOperation);
		trySkip(blocks[4], msgOperation);
		blocks[0].next();

		tryEnd(blocks[0], msgEnd);
		tryStart(blocks[1], msgOperation);
		tryStart(blocks[2], msgOperation);
		tryStart(blocks[3], msgOperation);
		//tryStart(blocks[4], "?");
		tryNext(blocks[0], msgNext); //child not used yet
		tryNext(blocks[1], msgOperation);
		tryNext(blocks[2], msgOperation);
		tryNext(blocks[3], msgOperation);
		tryNext(blocks[4], msgOperation);
		trySkip(blocks[0], msgMethod);
		trySkip(blocks[1], msgOperation);
		trySkip(blocks[2], msgOperation);
		trySkip(blocks[3], msgOperation);
		//trySkip(blocks[4], "?");
		blocks[4].start();

		tryEnd(blocks[0], msgEnd);
		tryStart(blocks[1], msgOperation);
		tryStart(blocks[2], msgOperation);
		tryStart(blocks[3], msgOperation);
		//tryStart(blocks[4], "?");
		//tryNext(blocks[0], "?");
		tryNext(blocks[1], msgOperation);
		tryNext(blocks[2], msgOperation);
		tryNext(blocks[3], msgOperation);
		tryNext(blocks[4], msgOperation);
		trySkip(blocks[0], msgMethod);
		trySkip(blocks[1], msgOperation);
		trySkip(blocks[2], msgOperation);
		trySkip(blocks[3], msgOperation);
		trySkip(blocks[4], msgOperation);
		blocks[0].next();

		//tryEnd(blocks[0], msgEnd);
		tryStart(blocks[1], msgOperation);
		tryStart(blocks[2], msgOperation);
		tryStart(blocks[3], msgOperation);
		tryStart(blocks[4], msgOperation);
		tryNext(blocks[0], msgOperation);
		tryNext(blocks[1], msgOperation);
		tryNext(blocks[2], msgOperation);
		tryNext(blocks[3], msgOperation);
		tryNext(blocks[4], msgOperation);
		trySkip(blocks[0], msgMethod);
		trySkip(blocks[1], msgOperation);
		trySkip(blocks[2], msgOperation);
		trySkip(blocks[3], msgOperation);
		trySkip(blocks[4], msgOperation);

		blocks[0].end();

		expected = "" +
			"0\n" +
			"1\n" +
			"2\n" +
			"2\n" + //restart
			"1.0\n" +
			"1.1\n" +
			//"3\n" + //skip
			"0.0\n" +
			"4\n" +
			"0.1\n" +
			"";
		assertEquals(expected, out.toString());

		//now testing state after restart initial block

		blocks[0].start(out);
		blocks[1].start();
		blocks[0].start(out); //restart

		//tryStart(blocks[1], "?");
		tryStart(blocks[2], msgOperation);
		tryStart(blocks[3], msgOperation);
		tryStart(blocks[4], msgOperation);
		tryNext(blocks[0], msgNext); //child not used yet
		tryNext(blocks[1], msgOperation);
		tryNext(blocks[2], msgOperation);
		tryNext(blocks[3], msgOperation);
		tryNext(blocks[4], msgOperation);
		trySkip(blocks[0], msgMethod);
		//trySkip(blocks[1], "?");
		trySkip(blocks[2], msgOperation);
		trySkip(blocks[3], msgOperation);
		trySkip(blocks[4], msgOperation);

		blocks[0].start(out);
		blocks[1].start();
		blocks[2].start();
		blocks[0].start(out); //restart

		//tryStart(blocks[1], "?");
		tryStart(blocks[2], msgOperation);
		tryStart(blocks[3], msgOperation);
		tryStart(blocks[4], msgOperation);
		tryNext(blocks[0], msgNext); //child not used yet
		tryNext(blocks[1], msgOperation);
		tryNext(blocks[2], msgOperation);
		tryNext(blocks[3], msgOperation);
		tryNext(blocks[4], msgOperation);
		trySkip(blocks[0], msgMethod);
		//trySkip(blocks[1], "?");
		trySkip(blocks[2], msgOperation);
		trySkip(blocks[3], msgOperation);
		trySkip(blocks[4], msgOperation);

		blocks[0].start(out);
		blocks[1].start();
		blocks[2].start();
		blocks[1].next();
		blocks[0].start(out); //restart

		//tryStart(blocks[1], "?");
		tryStart(blocks[2], msgOperation);
		tryStart(blocks[3], msgOperation);
		tryStart(blocks[4], msgOperation);
		tryNext(blocks[0], msgNext); //child not used yet
		tryNext(blocks[1], msgOperation);
		tryNext(blocks[2], msgOperation);
		tryNext(blocks[3], msgOperation);
		tryNext(blocks[4], msgOperation);
		trySkip(blocks[0], msgMethod);
		//trySkip(blocks[1], "?");
		trySkip(blocks[2], msgOperation);
		trySkip(blocks[3], msgOperation);
		trySkip(blocks[4], msgOperation);

		blocks[0].start(out);
		blocks[1].start();
		blocks[2].start();
		blocks[1].next();
		blocks[3].skip();
		blocks[0].start(out); //restart

		//tryStart(blocks[1], "?");
		tryStart(blocks[2], msgOperation);
		tryStart(blocks[3], msgOperation);
		tryStart(blocks[4], msgOperation);
		tryNext(blocks[0], msgNext); //child not used yet
		tryNext(blocks[1], msgOperation);
		tryNext(blocks[2], msgOperation);
		tryNext(blocks[3], msgOperation);
		tryNext(blocks[4], msgOperation);
		trySkip(blocks[0], msgMethod);
		//trySkip(blocks[1], "?");
		trySkip(blocks[2], msgOperation);
		trySkip(blocks[3], msgOperation);
		trySkip(blocks[4], msgOperation);

		blocks[0].start(out);
		blocks[1].start();
		blocks[2].start();
		blocks[1].next();
		blocks[3].skip();
		blocks[1].next();
		blocks[0].start(out); //restart

		//tryStart(blocks[1], "?");
		tryStart(blocks[2], msgOperation);
		tryStart(blocks[3], msgOperation);
		tryStart(blocks[4], msgOperation);
		tryNext(blocks[0], msgNext); //child not used yet
		tryNext(blocks[1], msgOperation);
		tryNext(blocks[2], msgOperation);
		tryNext(blocks[3], msgOperation);
		tryNext(blocks[4], msgOperation);
		trySkip(blocks[0], msgMethod);
		//trySkip(blocks[1], "?");
		trySkip(blocks[2], msgOperation);
		trySkip(blocks[3], msgOperation);
		trySkip(blocks[4], msgOperation);

	}

	public static void test8VariablesExceptions() throws IOException {

		String tpl;
		RepBlk page;
		CharArrayWriter out;
		String expected;
		String abc = "ABCDEFGHI";
		String msg;

		tpl = "" +
			"0 "+abc+"\n" +
			"<!--rep var=a place=A-->0+ "+abc+"\n" +
			"<!--rep blk=1-->1 "+abc+"\n" +
				"<!--rep var=b place=B-->1+ "+abc+"\n" +
				"<!--rep blk=2-->2 "+abc+"\n" +
					"<!--rep var=c place=C-->2+ "+abc+"\n" +
				"<!--/rep-->1.0 "+abc+"\n" +
				"<!--rep var=d place=D-->1.0+ "+abc+"\n" +
				"<!--rep blk=3-->3 "+abc+"\n" +
					"<!--rep var=e place=E-->3+ "+abc+"\n" +
				"<!--/rep-->1.1 "+abc+"\n" +
				"<!--rep var=f place=F-->1.1+ "+abc+"\n" +
			"<!--/rep-->0.0 "+abc+"\n" +
			"<!--rep var=g place=G-->0.0+ "+abc+"\n" +
			"<!--rep blk=4-->4 "+abc+"\n" +
				"<!--rep var=h place=H-->4+ "+abc+"\n" +
			"<!--/rep-->0.1 "+abc+"\n" +
			"<!--rep var=i place=I-->0.1+ "+abc+"\n" +
			"";

		page = new RepBlk(tpl);
		RepBlk[] blocks = {
			page, //vars: a,g,i
			page.getBlk("1"), //vars: b,d,f
			page.getBlk("1").getBlk("2"), //vars: c
			page.getBlk("1").getBlk("3"), //vars: e
			page.getBlk("4") //vars: h
		};

		expected = "" +
			"0 ABCDEFGHI\n" +
			"0+ ABCDEFGHI\n" +
			"1 ABCDEFGHI\n" +
			"1+ ABCDEFGHI\n" +
			"2 ABCDEFGHI\n" +
			"2+ ABCDEFGHI\n" +
			"1.0 ABCDEFGHI\n" +
			"1.0+ ABCDEFGHI\n" +
			"3 ABCDEFGHI\n" +
			"3+ ABCDEFGHI\n" +
			"1.1 ABCDEFGHI\n" +
			"1.1+ ABCDEFGHI\n" +
			"0.0 ABCDEFGHI\n" +
			"0.0+ ABCDEFGHI\n" +
			"4 ABCDEFGHI\n" +
			"4+ ABCDEFGHI\n" +
			"0.1 ABCDEFGHI\n" +
			"0.1+ ABCDEFGHI\n" +
			"";

		out = new CharArrayWriter();
		blocks[0].start(out);
		blocks[1].start();
		blocks[2].start();
		blocks[1].next();
		blocks[3].start();
		blocks[1].next();
		blocks[0].next();
		blocks[4].start();
		blocks[0].next();

		assertEquals(expected, out.toString());

		//blocks[0].setVar("a", "*");
		blocks[0].setVar("b", "*");
		blocks[0].setVar("c", "*");
		blocks[0].setVar("d", "*");
		blocks[0].setVar("e", "*");
		blocks[0].setVar("f", "*");
		//blocks[0].setVar("g", "*");
		blocks[0].setVar("h", "*");
		//blocks[0].setVar("i", "*");
		//
		blocks[1].setVar("a", "*");
		//blocks[1].setVar("b", "*");
		blocks[1].setVar("c", "*");
		//blocks[1].setVar("d", "*");
		blocks[1].setVar("e", "*");
		//blocks[1].setVar("f", "*");
		blocks[1].setVar("g", "*");
		blocks[1].setVar("h", "*");
		blocks[1].setVar("i", "*");
		//
		blocks[2].setVar("a", "*");
		blocks[2].setVar("b", "*");
		//blocks[2].setVar("c", "*");
		blocks[2].setVar("d", "*");
		blocks[2].setVar("e", "*");
		blocks[2].setVar("f", "*");
		blocks[2].setVar("g", "*");
		blocks[2].setVar("h", "*");
		blocks[2].setVar("i", "*");
		//
		blocks[3].setVar("a", "*");
		blocks[3].setVar("b", "*");
		blocks[3].setVar("c", "*");
		blocks[3].setVar("d", "*");
		//blocks[3].setVar("e", "*");
		blocks[3].setVar("f", "*");
		blocks[3].setVar("g", "*");
		blocks[3].setVar("h", "*");
		blocks[3].setVar("i", "*");
		//
		blocks[4].setVar("a", "*");
		blocks[4].setVar("b", "*");
		blocks[4].setVar("c", "*");
		blocks[4].setVar("d", "*");
		blocks[4].setVar("e", "*");
		blocks[4].setVar("f", "*");
		blocks[4].setVar("g", "*");
		//blocks[4].setVar("h", "*");
		blocks[4].setVar("i", "*");

		out = new CharArrayWriter();
		blocks[0].start(out);
		blocks[1].start();
		blocks[2].start();
		blocks[1].next();
		blocks[3].start();
		blocks[1].next();
		blocks[0].next();
		blocks[4].start();
		blocks[0].next();

		assertEquals(expected, out.toString());

		blocks[0].setVar("a", "(A)"); //0+
		blocks[1].setVar("b", "(B)"); //1+
		blocks[2].setVar("c", "(C)"); //2+
		blocks[1].setVar("d", "(D)"); //1.0+
		blocks[3].setVar("e", "(E)"); //3+
		blocks[1].setVar("f", "(F)"); //1.1+
		blocks[0].setVar("g", "(G)"); //0.0+
		blocks[4].setVar("h", "(H)"); //4+
		blocks[0].setVar("i", "(I)"); //0.1+

		out = new CharArrayWriter();
		blocks[0].start(out);
		blocks[1].start();
		blocks[2].start();
		blocks[1].next();
		blocks[3].start();
		blocks[1].next();
		blocks[0].next();
		blocks[4].start();
		blocks[0].next();

		expected = "" +
			"0 ABCDEFGHI\n" +
			"0+ (A)BCDEFGHI\n" + //from here (A) in 0
			"1 ABCDEFGHI\n" +
			"1+ A(B)CDEFGHI\n" + //from here (B) in 1
			"2 ABCDEFGHI\n" +
			"2+ AB(C)DEFGHI\n" + //from here (C) in 2 (last)
			"1.0 A(B)CDEFGHI\n" +
			"1.0+ A(B)C(D)EFGHI\n" + //from here (D) in 1
			"3 ABCDEFGHI\n" +
			"3+ ABCD(E)FGHI\n" + //from here (E) in 3 (last)
			"1.1 A(B)C(D)EFGHI\n" +
			"1.1+ A(B)C(D)E(F)GHI\n" + //from here (F) in 1 (last)
			"0.0 (A)BCDEFGHI\n" +
			"0.0+ (A)BCDEF(G)HI\n" + //from here (G) in 0
			"4 ABCDEFGHI\n" +
			"4+ ABCDEFG(H)I\n" + //from here (H) in 4 (last)
			"0.1 (A)BCDEF(G)HI\n" +
			"0.1+ (A)BCDEF(G)H(I)\n" + //from here (I) in 0 (last)
			"";

		assertEquals(expected, out.toString());

		//invalid setVar when block is in use:

		out = new CharArrayWriter();

		blocks[0].start(out);

		msg = "Operation not allowed now on this block";

		trySetVar(blocks[0], "a", "*", msg);
		trySetVar(blocks[0], "g", "*", msg);
		trySetVar(blocks[0], "i", "*", msg);
		//trySetVar(blocks[1], "b", "*", "?"); //not started
		//trySetVar(blocks[1], "d", "*", "?"); //not started
		//trySetVar(blocks[1], "f", "*", "?"); //not started
		//trySetVar(blocks[2], "c", "*", "?"); //not started
		//trySetVar(blocks[3], "e", "*", "?"); //not started
		//trySetVar(blocks[4], "h", "*", "?"); //not started

		blocks[1].start();

		trySetVar(blocks[0], "a", "*", msg);
		trySetVar(blocks[0], "g", "*", msg);
		trySetVar(blocks[0], "i", "*", msg);
		trySetVar(blocks[1], "b", "*", msg);
		trySetVar(blocks[1], "d", "*", msg);
		trySetVar(blocks[1], "f", "*", msg);
		//trySetVar(blocks[2], "c", "*", "?"); //not started
		//trySetVar(blocks[3], "e", "*", "?"); //not started
		//trySetVar(blocks[4], "h", "*", "?"); //not started

		blocks[2].start();

		trySetVar(blocks[0], "a", "*", msg);
		trySetVar(blocks[0], "g", "*", msg);
		trySetVar(blocks[0], "i", "*", msg);
		trySetVar(blocks[1], "b", "*", msg);
		trySetVar(blocks[1], "d", "*", msg);
		trySetVar(blocks[1], "f", "*", msg);
		//trySetVar(blocks[2], "c", "*", "?"); //finalized
		//trySetVar(blocks[3], "e", "*", "?"); //not started
		//trySetVar(blocks[4], "h", "*", "?"); //not started

		blocks[1].next();

		trySetVar(blocks[0], "a", "*", msg);
		trySetVar(blocks[0], "g", "*", msg);
		trySetVar(blocks[0], "i", "*", msg);
		trySetVar(blocks[1], "b", "*", msg);
		trySetVar(blocks[1], "d", "*", msg);
		trySetVar(blocks[1], "f", "*", msg);
		//trySetVar(blocks[2], "c", "*", "?"); //finalized
		//trySetVar(blocks[3], "e", "*", "?"); //not started
		//trySetVar(blocks[4], "h", "*", "?"); //not started

		blocks[3].start();

		trySetVar(blocks[0], "a", "*", msg);
		trySetVar(blocks[0], "g", "*", msg);
		trySetVar(blocks[0], "i", "*", msg);
		trySetVar(blocks[1], "b", "*", msg);
		trySetVar(blocks[1], "d", "*", msg);
		trySetVar(blocks[1], "f", "*", msg);
		//trySetVar(blocks[2], "c", "*", "?"); //finalized
		//trySetVar(blocks[3], "e", "*", "?"); //finalized
		//trySetVar(blocks[4], "h", "*", "?"); //not started

		blocks[1].next();

		trySetVar(blocks[0], "a", "*", msg);
		trySetVar(blocks[0], "g", "*", msg);
		trySetVar(blocks[0], "i", "*", msg);
		//trySetVar(blocks[1], "b", "*", "?"); //finalized
		//trySetVar(blocks[1], "d", "*", "?"); //finalized
		//trySetVar(blocks[1], "f", "*", "?"); //finalized
		//trySetVar(blocks[2], "c", "*", "?"); //finalized
		//trySetVar(blocks[3], "e", "*", "?"); //finalized
		//trySetVar(blocks[4], "h", "*", "?"); //not started

		blocks[0].next();

		trySetVar(blocks[0], "a", "*", msg);
		trySetVar(blocks[0], "g", "*", msg);
		trySetVar(blocks[0], "i", "*", msg);
		//trySetVar(blocks[1], "b", "*", "?"); //finalized
		//trySetVar(blocks[1], "d", "*", "?"); //finalized
		//trySetVar(blocks[1], "f", "*", "?"); //finalized
		//trySetVar(blocks[2], "c", "*", "?"); //finalized
		//trySetVar(blocks[3], "e", "*", "?"); //finalized
		//trySetVar(blocks[4], "h", "*", "?"); //not started

		blocks[4].start();

		trySetVar(blocks[0], "a", "*", msg);
		trySetVar(blocks[0], "g", "*", msg);
		trySetVar(blocks[0], "i", "*", msg);
		//trySetVar(blocks[1], "b", "*", "?"); //finalized
		//trySetVar(blocks[1], "d", "*", "?"); //finalized
		//trySetVar(blocks[1], "f", "*", "?"); //finalized
		//trySetVar(blocks[2], "c", "*", "?"); //finalized
		//trySetVar(blocks[3], "e", "*", "?"); //finalized
		//trySetVar(blocks[4], "h", "*", "?"); //finalized

		blocks[0].next();

		//trySetVar(blocks[0], "a", "*", "?"); //finalized
		//trySetVar(blocks[0], "g", "*", "?"); //finalized
		//trySetVar(blocks[0], "i", "*", "?"); //finalized
		//trySetVar(blocks[1], "b", "*", "?"); //finalized
		//trySetVar(blocks[1], "d", "*", "?"); //finalized
		//trySetVar(blocks[1], "f", "*", "?"); //finalized
		//trySetVar(blocks[2], "c", "*", "?"); //finalized
		//trySetVar(blocks[3], "e", "*", "?"); //finalized
		//trySetVar(blocks[4], "h", "*", "?"); //finalized

		assertEquals(expected, out.toString());

	}

	private static String joinStringSet(Set<String> set, String sep) {
		ArrayList<String> list = new ArrayList<String>(set);
		Collections.sort(list);
		return joinStrings(list, sep);
	}

	private static String joinStringList(List<String> list, String sep) {
		return joinStrings(list, sep);
	}
	private static String joinStrings(Collection<String> coll, String sep) {
		StringBuilder result = new StringBuilder();
		boolean addSep = false;
		for (String str : coll) {
			if (addSep)
				result.append(sep);
			else
				addSep = true;
			result.append(str);
		}
		return result.toString();
	}

	//these methods throw exception if operation does not throw exception
	//or the exception message is not equals to the expected message

	private static void tryNew(String tpl, String expected) {
		boolean ok;
		RepBlk page = null;
		try { page = new RepBlk(tpl); ok = false; }
		catch (Exception e) { ok = expected.equals(e.getMessage()); }
		if (! ok) throw new IllegalStateException();
	}

	private static void tryGetBlk(RepBlk block, String name,
			String expected) {
		boolean ok;
		RepBlk other = null;
		try { other = block.getBlk(name); ok = false; }
		catch (Exception e) { ok = expected.equals(e.getMessage()); }
		if (! ok) throw new IllegalStateException();
	}

	private static void trySetVar(RepBlk block, String key, String value,
			String expected) {
		boolean ok;
		try { block.setVar(key, value); ok = false; }
		catch (Exception e) { ok = expected.equals(e.getMessage()); }
		if (! ok) throw new IllegalStateException();
	}

	private static void tryStart(RepBlk block, String expected) {
		boolean ok;
		try { block.start(); ok = false; }
		catch (Exception e) { ok = expected.equals(e.getMessage()); }
		if (! ok) throw new IllegalStateException();
	}

	private static void tryStart(RepBlk block, Writer writer,
			String expected) {
		boolean ok;
		try { block.start(writer); ok = false; }
		catch (Exception e) { ok = expected.equals(e.getMessage()); }
		if (! ok) throw new IllegalStateException();
	}

	private static void tryNext(RepBlk block, String expected) {
		boolean ok;
		try { block.next(); ok = false; }
		catch (Exception e) { ok = expected.equals(e.getMessage()); }
		if (! ok) throw new IllegalStateException();
	}

	private static void trySkip(RepBlk block, String expected) {
		boolean ok;
		try { block.skip(); ok = false; }
		catch (Exception e) { ok = expected.equals(e.getMessage()); }
		if (! ok) throw new IllegalStateException();
	}

	private static void tryEnd(RepBlk block, String expected) {
		boolean ok;
		try { block.end(); ok = false; }
		catch (Exception e) { ok = expected.equals(e.getMessage()); }
		if (! ok) throw new IllegalStateException();
	}
}
