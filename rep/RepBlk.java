/*
 * Rep template system - Copyright 2015 Carlos Rica <jasampler@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package rep;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RepBlk represents a block in a Rep HTML template, and it
 * is used for generating HTML pages based on that template.
 *
 * <p>A Rep template is a valid HTML page that contains marked sections
 * (called blocks) enclosed between two special HTML comments like this:</p>
 *
 * <code>&lt;!--rep blk=BLOCKNAME--&gt; ... &lt;!--/rep--&gt;</code>
 *
 * <p>Any block in the Rep template can have other blocks inside, but it is not
 * allowed to have two or more blocks with the same BLOCKNAME inside the same
 * block.</p>
 *
 * <p>The first block instantiated represents the entire given template,
 * it has no name and gives access to the others blocks in the hierarchy.
 * After creating this initial block, its method start must be called passing
 * as the first argument the Writer on which the generated text must be written.
 * </p>
 *
 * <p>The skip and start methods in RepBlk allow repeating the content of
 * each block zero or more times without handling directly the Rep template.
 * Calling to start writes the first text of the block that is before its first
 * child block in the template, or all the text if it has no blocks inside.
 * If a block has other blocks inside, then after calling start on it, the
 * following operation must be calling to start or skip on its first child.
 * The operations must follow the same order of the blocks in the template,
 * and every operation called in the wrong order will throw an exception.
 * The method next must be called on every block after each one of its children
 * has been processed, because it writes the code after or between blocks.
 * When a block has been skipped or is not going to be restarted anymore, then
 * on the parent block must be called next to write the text after that block.
 * </p>
 *
 * <p>The Rep template can also contain variables to be replaced with values
 * given to the RepBlock, by adding HTML comments with the format:</p>
 *
 * <code>&lt;!--rep var=VARNAME place=STRINGTOREPLACE --&gt;</code>
 *
 * <p>A Rep tag with the var attribute does not have an end tag.
 * The value STRINGTOREPLACE must be enclosed in "double" or 'single' quotes
 * if contains spaces or quotes, and it must appear at least once in the block.
 * It will only be replaced in the text of the block in which it is declared,
 * not in its children, and it will only be replaced if it appears after the
 * declaration. It is not allowed to declare more than one variable with the
 * same VARNAME in the same block.</p>
 *
 * <p>The value of any variable can be assigned by calling to the setVar method,
 * always before calling to start, and the generated text will include that
 * value instead of the STRINGTOREPLACE in the next repetition of the block.</p>
 *
 * <p>Here is a diagram of allowed operations for a block with two blocks
 * inside:</p><pre>
 *                               .--.
 *      .-----------------------(skip)-----------------------.
 *     /             _____       '--' _____                   \
 *    /  .---.      |     |   .--.   |     |   .--.            \
 * --'--(start)--.--|BLOCK|--(next)--|BLOCK|--(next)-- ... --.--'--
 *       '---'  /   |_____|   '--'   |_____|   '--'           \
 *              \                     .---.                   /
 *               '-------------------(start)-----------------'
 *                                    '...'</pre>
*/
public class RepBlk {

	private static final int //0,1,2... is the child block being processed
		STATE_OUT = -4, //in this state parent must do start or next
		STATE_CLOSED = -3, //in this state parent must do next
		STATE_USED = -2, //in this state can do start or parent.next
		STATE_READY = -1; //in this state can do start or skip

	/* Diagram of internal [STATES]:
	          [OUT]
	parent.start|parent.next
	 __________\|/____________
	|           '             |
	|        [READY]----.     |
	|  .------. |start  |skip |
	| /|\      \|       |     |
	|  |        |       |     |
	|  |       \|/      |     |
	|  |       _'_      |     |
	|  |      |   |     |     |
	|  |      |[0]|     |     |
	|  |      |___|     |     |
	|  |    next|       |     |
	|  |       \|/      |     |
	|  |       _'_      |     |
	|  |      |   |     |     |
	|  |      |[1]|     |     |
	|  |      |___|     |     |
	|  |    next|       |     |
	|  |       \|/     \|/    |
	|  |start   '       '     |
	|  '-----[USED]  [CLOSED] |
	|___________|_______|_____|
	            '---.---'
	     parent.next|
	               \|/
	                '
	              [OUT]
	*/

	private static final int
		TAG_NONE = 0,
		TAG_START = 1,
		TAG_END = 2;

	private static final String
	ERR_EMPTY_BLK_ATTR = "Empty blk attribute in Rep tag", //01
	ERR_EMPTY_VAR_ATTR = "Empty var attribute in Rep tag", //02
	ERR_MISSING_ATTR = "Missing blk or var attribute in Rep tag", //03
	ERR_MULTIPLE_ATTR = "Found blk and var attributes in Rep tag", //04
	ERR_NO_PLACE_ATTR = "Missing or empty place attribute in Rep tag", //05
	ERR_PLACE_NOT_FOUND = "One or more variables not found in block", //06
	ERR_NOT_OPENED_BLK = "Found Rep tag closing a block not opened", //07
	ERR_NOT_CLOSED_BLK = "One or more blocks have not been closed", //08
	ERR_REPEATED_BLK = "Repeated block name", //09
	ERR_REPEATED_VAR = "Repeated variable name", //10
	ERR_REPEATED_ATTR = "Repeated attribute in Rep tag", //11
	ERR_INVALID_ATTR = "Invalid attribute format in Rep tag", //12
	ERR_END_NOT_FOUND = "End of comment not found", //13
	ERR_INVALID_METHOD = "This method cannot be used on this block", //14
	ERR_INVALID_STATE = "Operation not allowed now on this block", //15
	ERR_CHILD_STATE = "Next not allowed because the state of child", //16
	ERR_NOT_FINALIZED = "The page has not been written completely"; //17

	private final String blkName;
	private final Writer[] writerBox;
	private final List<String> blkNames;
	private final Set<String> varNamesSet;
	private int state = STATE_OUT; //STATE or index of current child block
	private ArrayList<RepBlk> blocks = new ArrayList<RepBlk>();
	private HashMap<String, RepBlk> blksMap = new HashMap<String, RepBlk>();
	private HashMap<String, String> varsMap = new HashMap<String, String>();
	private ArrayList<Integer> ranges = new ArrayList<Integer>();
	private ArrayList<String> texts = new ArrayList<String>();
	private ArrayList<String> varNames = new ArrayList<String>();

	/**
	 * Reads the template to create the initial block and closes the Reader.
	 * The initial block contains the rest of the blocks of the hierarchy.
	 */
	public RepBlk(Reader reader) throws IOException {
		this(null, new String[] { readAll(reader) }, new Writer[1]);
	}

	/**
	 * Reads the template from the string to create the initial block.
	 * The initial block contains the rest of the blocks of the hierarchy.
	 */
	public RepBlk(String tpl) {
		this(null, new String[] {tpl}, new Writer[1]);
	}

	/**
	 * Begins the writing of the initial block using the given Writer.
	 * If this block has no children it writes all the text of the block,
	 * otherwise it writes the text before the first block inside this one.
	 * The Writer will not be closed, so the user must do it when needed.
	 * This operation is only allowed on the initial block, and it resets
	 * the state of all blocks if a previous process was not finalized.
	 */
	public void start(Writer writer) throws IOException {
		if (blkName != null)
			throwBadState(ERR_INVALID_METHOD);
		if (state > -1)
			resetState();
		writerBox[0] = writer;
		writeState(0);
	}

	/**
	 * Begins the writing of the block, not applicable to the initial block.
	 * If this block has no children it writes all the text of the block,
	 * otherwise it writes the text before the first block inside this one.
	 */
	public void start() throws IOException {
		if (blkName == null)
			throwBadState(ERR_INVALID_METHOD);
		if (state != STATE_READY && state != STATE_USED)
			throwBadState(ERR_INVALID_STATE);
		writeState(0);
	}

	/**
	 * Discards the writing of the block without starting it.
	 * After this call the block cannot be started until restart the parent.
	 * The initial block should be always written, so it cannot be skipped.
	 */
	public void skip() {
		if (blkName == null)
			throwBadState(ERR_INVALID_METHOD);
		if (state != STATE_READY)
			throwBadState(ERR_INVALID_STATE);
		state = STATE_CLOSED;
	}

	/**
	 * Writes the corresponding text after the last processed child block.
	 * This must be called after every child block is processed, that is,
	 * after being skipped or being repeated the desired number of times.
	 */
	public void next() throws IOException {
		if (state < 0)
			throwBadState(ERR_INVALID_STATE);
		RepBlk blk = blocks.get(state);
		if (blk.state != STATE_USED && blk.state != STATE_CLOSED)
			throwBadState(ERR_CHILD_STATE);
		blk.state = STATE_OUT;
		writeState(state + 1);
	}

	/**
	 * Throws an exception if the initial block has not been completed yet.
	 * This operation is only allowed on the initial block, after use it.
	 * The Writer will not be closed, so the user must do it when needed.
	 */
	public void end() throws IOException {
		if (blkName != null)
			throwBadState(ERR_INVALID_METHOD);
		if (state != STATE_USED)
			throwBadState(ERR_NOT_FINALIZED);
	}

	/**
	 * Returns the block with the given name if it is inside this block,
	 * otherwise returns null.
	 * The blocks are requested by name to make them appear in the program.
	 */
	public RepBlk getBlk(String blkName) {
		return blksMap.get(blkName);
	}

	/**
	 * Sets the value of the variable with the given name, if it is defined
	 * in this block, otherwise nothing is done.
	 * Not allowed if the block has written already a part of its contents.
	 * Returns this block, to chain more calls to set or start the block.
	 */
	public RepBlk setVar(String varName, String value) {
		if (state > -1)
			throwBadState(ERR_INVALID_STATE);
		if (varsMap.containsKey(varName))
			varsMap.put(varName, value);
		return this;
	}

	/**
	 * Returns a list of the names of the blocks inside this block.
	 * Returns always the same unmodifiable list, sorted as in the template.
	 */
	public List<String> getBlkNames() {
		return blkNames;
	}

	/**
	 * Returns a collection with the names of the variables of this block.
	 * Returns always the same unmodifiable set of strings.
	 */
	public Set<String> getVarNames() {
		return varNamesSet;
	}

	/**
	 * Returns the Writer used by all the blocks of the hierarchy.
	 * Used to flush or close the Writer passed to the initial block.
	 */
	public Writer getWriter() {
		return writerBox[0];
	}

	//private constructor to create recursively all blocks:
	private RepBlk(String blkName, String[] tplBox, Writer[] writerBox) {
		this.blkName = blkName;
		this.writerBox = writerBox;
		int[] tagType = new int[1];
		String prevText;
		HashMap<String,String> attribs = new HashMap<String,String>();
		addRange(ranges);
		while ((prevText = findRepTag(tplBox,
				tagType, attribs)) != null) {
			incLastRange(parseVariables(prevText, varsMap,
					texts, varNames), ranges);
			if (tagType[0] == TAG_START) {
				String name;
				if ((name = attribs.get("blk")) != null) {
					if (name.length() == 0)
						throwBadArg(ERR_EMPTY_BLK_ATTR);
					if (attribs.get("var") != null)
						throwBadArg(ERR_MULTIPLE_ATTR);
					if (blksMap.containsKey(name))
						throwBadArg(ERR_REPEATED_BLK +
							": " + name);
					RepBlk blk = new RepBlk(name,
							tplBox, writerBox);
					blocks.add(blk);
					blksMap.put(name, blk);
					addRange(ranges);
				}
				else if ((name = attribs.get("var")) != null) {
					name = attribs.get("var");
					if (name.length() == 0)
						throwBadArg(ERR_EMPTY_VAR_ATTR);
					if (varsMap.containsKey(name))
						throwBadArg(ERR_REPEATED_VAR +
							": " + name);
					String place = attribs.get("place");
					if (place == null || place.length()==0)
						throwBadArg(ERR_NO_PLACE_ATTR);
					varsMap.put(name, place);
				}
				else
					throwBadArg(ERR_MISSING_ATTR);
			}
			else if (tagType[0] == TAG_END) {
				if (blkName == null)
					throwBadArg(ERR_NOT_OPENED_BLK);
				break;
			}
		}
		if (blkName == null) {
			incLastRange(parseVariables(tplBox[0], varsMap,
					texts, varNames), ranges);
			tplBox[0] = null;
		}
		else if (tagType[0] != TAG_END)
			throwBadArg(ERR_NOT_CLOSED_BLK + ": " + blkName);
		HashSet<String> notFound = new
				HashSet<String>(varsMap.keySet());
		notFound.removeAll(varNames);
		if (notFound.size() > 0)
			throwBadArg(ERR_PLACE_NOT_FOUND + ": " +
				joinStrings(sortedList(notFound), ", "));
		ArrayList<String> list = new ArrayList<String>(blocks.size());
		for (RepBlk block : blocks)
			list.add(block.blkName);
		blkNames = Collections.unmodifiableList(list);
		varNamesSet = Collections.unmodifiableSet(varsMap.keySet());
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

	//used to return string lists more predictable and testable on errors:
	private static ArrayList<String> sortedList(Collection<String> coll) {
		ArrayList<String> list = new ArrayList<String>(coll);
		Collections.sort(list);
		return list;
	}

	private static void addRange(ArrayList<Integer> ranges) {
		int size = ranges.size();
		int n = (size != 0 ? ranges.get(size - 1).intValue() : 0);
		ranges.add(Integer.valueOf(n));
	}

	private static void incLastRange(int inc, ArrayList<Integer> ranges) {
		int i = ranges.size() - 1;
		ranges.set(i, Integer.valueOf(ranges.get(i).intValue() + inc));
	}

	//Resets recursivelly the state of a block and its children.
	private void resetState() {
		if (state > -1)
			blocks.get(state).resetState();
		state = STATE_OUT;
	}

	//Writes the selected text of the block with its variables replaced.
	//Then, sets the state of the block and of its next child, if any.
	//When the last text of the initial block is written, it calls flush.
	private void writeState(int pos) throws IOException {
		Writer writer = writerBox[0];
		int limit = ranges.get(pos);
		int init = pos > 0 ? ranges.get(pos - 1) : 0;
		for (int i = init; i < limit; i++) {
			writer.write(texts.get(i));
			String varName = varNames.get(i);
			if (varName != null)
				writer.write(varsMap.get(varName));
		}
		if (pos < blocks.size()) {
			state = pos;
			blocks.get(state).state = STATE_READY;
		}
		else {
			state = STATE_USED;
			if (blkName == null) //initial block
				writerBox[0].flush();
		}
	}

	private static final Pattern startTagPat =
			Pattern.compile("<!--(/?)rep\\b");
	private static final String endComment = "-->";

	//Searches a Rep comment tag in tplBox[0] and parses its attributes,
	//It finds a Rep comment, the text before the tag is returned,
	//tagType[0] is set to TAG_START or TAG_END, and
	//tplBox[0] is set to a string without the tag and its previous text.
	//If a Rep comment is not found, null is returned.
	private static String findRepTag(String[] tplBox, int[] tagType,
			HashMap<String,String> attribs) {
		String tpl;
		tagType[0] = TAG_NONE;
		Matcher m = startTagPat.matcher(tpl = tplBox[0]);
		if (m.find()) {
			int endPos = tpl.indexOf(endComment, m.end());
			if (endPos < 0)
				throwBadArg(ERR_END_NOT_FOUND);
			if (tpl.charAt(m.start(1)) == '/')
				tagType[0] = TAG_END;
			else {
				tagType[0] = TAG_START;
				parseAttributes(trimEnd(tpl.substring(m.end(),
					endPos)), attribs);
			}
			String prevText = tpl.substring(0, m.start());
			tplBox[0] = tpl.substring(endPos + endComment.length());
			return prevText;
		}
		return null;
	}

	private static final Pattern attribsPat = Pattern.compile(
		"^\\s*(\\w[-\\w]*)=(\"[^\"]*\"|'[^']*'|[^\"'\\s]*)");

	private static void parseAttributes(String str,
			HashMap<String,String> attribs) {
		attribs.clear();
		Matcher am = attribsPat.matcher(str);
		while (am.find()) {
			String name = am.group(1);
			if (attribs.containsKey(name))
				throwBadArg(ERR_REPEATED_ATTR + ": " + name);
			String value = am.group(2);
			int len = value.length();
			if (len > 0 && (value.charAt(0) == '"' ||
					value.charAt(0) == '\'')) {
				value = value.substring(1, len - 1);
			}
			attribs.put(name, value);
			str = str.substring(am.end());
			am = attribsPat.matcher(str);
		}
		if (str.length() > 0)
			throwBadArg(ERR_INVALID_ATTR);
	}

	//Searches the values of varsMap in the given text and decomposes it,
	//adding parts of the text to the texts list and the names of the found
	//variables to the varNames list, adding the same number of elements to
	//both lists and returning that number (since both lists must have
	//equal size some variable names are null).
	private static int parseVariables(String text,
			HashMap<String,String> varsMap,
			ArrayList<String> texts, ArrayList<String> varNames) {
		int added = 1;
		while ((text = findNextVar(text, varsMap, texts, varNames))
				!= null)
			added++;
		return added;
	}

	//Searches in the text the first occurrence of a value of varsMap and
	//if it is found returns the rest of the text after the found value,
	//adding the text before the value to the texts list and
	//adding the key of the value to the varNames list. If no value is
	//found, adds all the text to texts, null to varNames and returns null.
	private static String findNextVar(String text,
			HashMap<String,String> varsMap,
			ArrayList<String> texts, ArrayList<String> varNames) {
		int nextPos = text.length();
		String nextVar = null;
		for (String var : varsMap.keySet()) {
			int pos = text.indexOf(varsMap.get(var));
			if (pos > -1 && pos < nextPos) {
				nextPos = pos;
				nextVar = var;
			}
		}
		texts.add(text.substring(0, nextPos));
		varNames.add(nextVar);
		if (nextVar != null)
			return text.substring(nextPos +
				varsMap.get(nextVar).length());
		return null;
	}

	private static final Pattern endSpacesPat = Pattern.compile("\\s+$");

	private static String trimEnd(String str) {
		Matcher m = endSpacesPat.matcher(str);
		if (m.find())
			return str.substring(0, m.start());
		return str;
	}

	//reader to string method that close the reader only when needed:
	private static String readAll(Reader reader) throws IOException {
		StringBuilder result = new StringBuilder();
		boolean needClose = (reader != null);
		try {
			char[] buf = new char[5000];
			int n;
			while ((n = reader.read(buf)) != -1)
				result.append(buf, 0, n);
			needClose = false;
			reader.close();
		}
		finally {
			if (needClose) {
				try { reader.close(); }
				catch (IOException e) { } //ignored
			}
		}
		return result.toString();
	}

	private static void throwBadArg(String msg) {
		throw new IllegalArgumentException(msg);
	}

	private static void throwBadState(String msg) {
		throw new IllegalStateException(msg);
	}

}

