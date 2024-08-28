package com.ericsson.eniq.etl.twampST;

import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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


public class twampST extends DefaultHandler implements Parser{
	private Logger log;
	private MeasurementFile measFile = null;
	private Map<String, String> measData= null;
	private Map<String,String> sessionData = null;
	private SourceFile sourceFile;
	private final String direction = "DIRECTION";
	private boolean uplink = true;
	private String sdcName = null;
	private String tagID = null;

	// ***************** Worker stuff ****************************
	private String techPack;
	private String setType;
	private String setName;
	private String workerName = "";

	private int status = 0;

	private Main mainParserObject = null;
	public void init(Main main, String techPack, String setType, String setName, String workerName) {
		this.mainParserObject = main;
		this.techPack = techPack;
		this.setType = setType;
		this.setName = setName;
		this.status = 1;
		this.workerName = workerName;

		String logWorkerName = "";
		if (workerName.length() > 0)
			logWorkerName = "." + workerName;

		log = Logger.getLogger("etl." + techPack + "." + setType + "." + setName + ".parser.twampST" + logWorkerName);

	}

	public int status() {
		return status;
	}

	public void run() {

		try {
			this.status = 2;
			SourceFile sf = null;
			while ((sf = mainParserObject.nextSourceFile()) != null) {

				try {
					mainParserObject.preParse(sf);
					parse(sf, techPack, setType, setName);
					mainParserObject.postParse(sf);
				} catch (Exception e) {
					mainParserObject.errorParse(e, sf);
				} finally {
					mainParserObject.finallyParse(sf);
				}
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Worker parser failed to exception", e);
		} finally {
			this.status = 3;
		}
	}

	 public void parse(SourceFile sf, String techPack, String setType, String setName) throws Exception {
            measData = new HashMap<String, String>();
            sessionData = new HashMap<String, String>();
            this.sourceFile = sf;
            log.fine("Parsing Started");
            String sdcMask = sf.getProperty("vendorIDMask", "(.*)(-)(.*)(-)(.*)(-)(.*)([+,-])(.*)");
            String filename =sf.getName();
            sdcName = parseFileName(filename, sdcMask,1);
            tagID = parseFileName(filename, sdcMask,3);
            //sid = parseFileName(filename, sdcMask,5);
            final XMLReader xmlReader = new org.apache.xerces.parsers.SAXParser();
            xmlReader.setContentHandler(this);
            xmlReader.setErrorHandler(this);
            xmlReader.setEntityResolver(new ENIQEntityResolver(log.getName()));
            xmlReader.parse(new InputSource(sf.getFileInputStream()));
            log.fine("Parsing Completed");
      }



	public void startElement(String uri, String name, String qName, Attributes atts) throws SAXException {
		if (qName.equals("Sess") ){
			for (int i = 0; i < atts.getLength(); i++) {
				sessionData.put(atts.getLocalName(i),atts.getValue(i));
			}
		}
		else if (qName.equals("OneWayConf")){
			if (uplink){
				measData.put(direction,"0");
			}
			else{
				measData.put(direction,"1");
			}
			/*Insert direction attribute for OneWayConf entry in each session
			*First OneWayConf entry indicates uplink direction - 0
			*Second OneWayConf entry indicates downlink direction - 1
			*/
			for (int i = 0; i < atts.getLength(); i++) {
				measData.put(atts.getLocalName(i),atts.getValue(i));
			}
		}
	}

	public void endElement(String uri, String name, String qName){
		try{
			if (qName.equals("OneWayConf"))
			{
					if(measFile == null){
						measFile = Main.createMeasurementFile(sourceFile,tagID, techPack, setType, setName, this.workerName, this.log);
						log.fine("Created new File with worker"+this.workerName);
			}
				if (uplink){
					uplink = false;
				}
				else {
					uplink = true;
				}
				measFile.addData("Filename", sourceFile.getName());
				measFile.addData("SDC_NAME",sdcName);
				measFile.addData("DIRNAME",sourceFile.getDir());
				measFile.addData(measData);
				measFile.addData(sessionData);
				measFile.saveData();
				measData.clear();
			}
			else if (qName.equals("Sess")){
				sessionData.clear();
				uplink = true;
			}/*Direction should always be zero for first entry at next session*/
			else if (qName.equals("Response")){
				measFile.close();
				measFile = null;
			}
		}
		catch(final Exception e){
			log.warning("Exception caught at end element::"+e.getMessage());
		}
		
	}

	public String parseFileName(String filename,String regExp,int index) {

		Pattern pattern = Pattern.compile(regExp);
		Matcher matcher = pattern.matcher(filename);
		if (matcher.matches()) {
			String result = matcher.group(index);
			log.fine(" regExp (" + regExp + ") found from " + filename + "  :" + result);
			return result;
		} else {
			log.warning("String " + filename + " doesn't match defined regExp " + regExp);
		}
		return "";
	}
}
