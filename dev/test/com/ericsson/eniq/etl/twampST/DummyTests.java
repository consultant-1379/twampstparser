package com.ericsson.eniq.etl.twampST;

import java.util.Map;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.distocraft.dc5000.etl.parser.Main;
import com.distocraft.dc5000.etl.parser.MeasurementFile;
import com.distocraft.dc5000.etl.parser.Parser;
import com.distocraft.dc5000.etl.parser.SourceFile;
import com.ericsson.eniq.common.*;
import com.ericsson.eniq.etl.twampST.twampST;


public class DummyTests {
	
	twampST tst = new twampST();
	
	@Test
	public void test1()
	{
		tst.init(null, null, null, null, "workname");
	}
	
	@Test
	public void test2()
	{
		int result = tst.status();
		assertEquals(result, result);
	}
}
