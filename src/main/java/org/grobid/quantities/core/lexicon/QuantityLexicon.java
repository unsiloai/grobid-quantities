package org.grobid.core.lexicon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.PatternSyntaxException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.exceptions.GrobidResourceException;
import org.grobid.core.lang.Language;
import org.grobid.core.sax.CountryCodeSaxParser;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.utilities.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.grobid.core.lexicon.FastMatcher;
import org.grobid.core.utilities.UnitUtilities;
import org.grobid.core.data.Unit;

/**
 * Class for managing the measurement lexical resources
 *
 * @author Patrice Lopez
 */
public class QuantityLexicon {
	private static final Logger logger = LoggerFactory.getLogger(QuantityLexicon.class);

	private static volatile QuantityLexicon instance;

	public static QuantityLexicon getInstance() {
		if (instance == null) {
			getNewInstance();
		}
		return instance;
	}

	/**
	 * Create a new instance.
	 */
	private static synchronized void getNewInstance() {
		instance = new QuantityLexicon();
	}

	private FastMatcher unitPattern = null;
	private Set<String> unitTokens = null;
	private Map<String,List<String>> prefix = null; // map prefix symbol to prefix strings

	private QuantityLexicon() {
		init();
	}

	private void init() {
		initPrefix();
		File file = null;
		InputStream ist = null;
        InputStreamReader isr = null;
        BufferedReader dis = null;
        try {			
			unitTokens = new HashSet<String>();
			String path = "src/main/resources/units.en.txt";
			file = new File(path);
	        if (!file.exists()) {
	            throw new GrobidResourceException("Cannot add entries to unit dictionary, because file '" 
					+ file.getAbsolutePath() + "' does not exists.");
	        }
	        if (!file.canRead()) {
	            throw new GrobidResourceException("Cannot add entries to unit dictionary, because cannot read file '" 
					+ file.getAbsolutePath() + "'.");
	        }

			unitPattern = new FastMatcher();
            ist = getClass().getResourceAsStream(path);
			if (ist == null) 
				ist = new FileInputStream(file);
            isr = new InputStreamReader(ist, "UTF8");
            dis = new BufferedReader(isr);
			
            String l = null;
            while ((l = dis.readLine()) != null) {
            	if (l.length() == 0) continue;
            	String[] pieces = l.split("\t");
            	Unit unit = new Unit();
            	for(int i=0; i<pieces.length;i++) {
            		String piece = pieces[i].trim();
            		if (piece.length() == 0)
            			continue;
            		if (i == 0) {
            			try {
	            			unitPattern.loadTerm(piece);
	            		}
	            		catch(Exception e) {
	            			logger.error("invalid unit term: " + piece);
	            		}
            			unit.setNotation(piece);
            			unit.addName(piece);
		                StringTokenizer st = new StringTokenizer(piece, TextUtilities.fullPunctuations);
		                while (st.hasMoreTokens()) {
		                 	String word = st.nextToken().trim().toLowerCase();
		                    if ((word.length() > 0) && !unitTokens.contains(word))
		                      	unitTokens.add(word);
		                }
		            }
		            else if (i == 1) {
		            	UnitUtilities.System_Type system = null;
		            	try {
		            		system = UnitUtilities.System_Type.valueOf(piece);
		            	}
		            	catch(Exception e) {
		            		logger.error("invalid unit system name: " + piece);
		            	}
		            	unit.setSystem(system);
		            }
		            else if (i == 2) {
		            	UnitUtilities.Unit_Type type = null;
		            	try {
		            		type = UnitUtilities.Unit_Type.valueOf(piece);
		            	}
		            	catch(Exception e) {
		            		logger.error("invalid unit type name: " + piece);
		            	}
		            	unit.setType(type);
		            }
		            else if (i ==3) {
		            	String[] subpieces = piece.split(",");
		            	for(int j=0; j<subpieces.length; j++) {
			            	unit.addName(subpieces[j].trim());
			            	try {
		            			unitPattern.loadTerm(subpieces[j].trim());
		            		}
		            		catch(Exception e) {
		            			logger.error("invalid unit term: " + subpieces[j].trim());
		            		}
			            	StringTokenizer st = new StringTokenizer(subpieces[j].trim(), TextUtilities.fullPunctuations);
			                while (st.hasMoreTokens()) {
			                 	String word = st.nextToken().trim().toLowerCase();
			                    if ((word.length() > 0) && !unitTokens.contains(word))
			                      	unitTokens.add(word);
			                }
			            }
		            }
	            }
	        } 
System.out.println(unitTokens.toString());
		}	
		catch (PatternSyntaxException e) {
            throw new 
			GrobidResourceException("Error when compiling lexicon matcher for unit vocabulary.", e);
        }
		catch (FileNotFoundException e) {
//	    	e.printStackTrace();
            throw new GrobidException("An exception occured while running Grobid.", e);
        } 
		catch (IOException e) {
//	    	e.printStackTrace();
            throw new GrobidException("An exception occured while running Grobid.", e);
        } 
		finally {
            try {
                if (ist != null)
                    ist.close();
                if (isr != null)
                    isr.close();
                if (dis != null)
                    dis.close();
            } catch (Exception e) {
                throw new GrobidResourceException("Cannot close all streams.", e);
            }
        }
    }
	
    private void initPrefix() {
    	File file = null;
		InputStream ist = null;
        InputStreamReader isr = null;
        BufferedReader dis = null;
        try {			
			unitTokens = new HashSet<String>();
			String path = "src/main/resources/prefix.txt";
			file = new File(path);
	        if (!file.exists()) {
	            throw new GrobidResourceException("Cannot add entries to unit dictionary, because file '" 
					+ file.getAbsolutePath() + "' does not exists.");
	        }
	        if (!file.canRead()) {
	            throw new GrobidResourceException("Cannot add entries to unit dictionary, because cannot read file '" 
					+ file.getAbsolutePath() + "'.");
	        }

			unitPattern = new FastMatcher();
            ist = getClass().getResourceAsStream(path);
			if (ist == null) 
				ist = new FileInputStream(file);
            isr = new InputStreamReader(ist, "UTF8");
            dis = new BufferedReader(isr);
			
            String l = null;
            while ((l = dis.readLine()) != null) {
            	if (l.length() == 0) continue;
            	String pieces[] = l.split("\t");
            	if (pieces.length != 3) 
            		continue;
            	String symbol = pieces[1].trim();
            	String names = pieces[2].trim();
            	String[] theNames = names.split(",");
            	List<String> nameList = new ArrayList<String>();
            	for(int i=0; i<theNames.length; i++) {
            		nameList.add(theNames[i].trim());
            	}
            	if (prefix == null)
            		prefix = new HashMap<String,List<String>>();
            	prefix.put(symbol, nameList);
            }

System.out.println(prefix.toString());
		}	
		catch (PatternSyntaxException e) {
            throw new 
			GrobidResourceException("Error when compiling prefix map for unit vocabulary.", e);
        }
		catch (FileNotFoundException e) {
//	    	e.printStackTrace();
            throw new GrobidException("An exception occured while running Grobid.", e);
        } 
		catch (IOException e) {
//	    	e.printStackTrace();
            throw new GrobidException("An exception occured while running Grobid.", e);
        } 
		finally {
            try {
                if (ist != null)
                    ist.close();
                if (isr != null)
                    isr.close();
                if (dis != null)
                    dis.close();
            } catch (Exception e) {
                throw new GrobidResourceException("Cannot close all streams.", e);
            }
        }
    }

	/**
	 * Expansion of a non-notation unit name into its inflected forms. Note that the 
	 * input unit name is included in the returned list of forms.
	 */
	private List<String> inflectionalMorphologyExpansion(String unitTerm) {
		List<String> results = new ArrayList<String>();
		results.add(unitTerm);

		// ...

		return results;
	}

	/**
	 * Expansion of a notation and non-notation unit name into derivations 
	 * based on standard unit prefix. Note that the 
	 * input unit name is included in the returned list of forms.
	 * To be called after the inflectional expansion.
	 */
	private List<String> derivationalMorphologyExpansion(String unitTerm, boolean isNotation) {
		List<String> results = new ArrayList<String>();
		results.add(unitTerm);

		// if we have a notation, we use notation prefix (e.g. g -> kg)
		// if we have a full form, we use the derivational prefix (e.g. gram -> kilogram)

		return results;
	}

	/**
     * Soft look-up in unit dictionary
     */
    public List<OffsetPosition> inUnitNames(String s) {
        if (unitPattern == null) {
            init();
        }
        List<OffsetPosition> results = unitPattern.matcher(s);
        return results;
    }

    public List<OffsetPosition> inUnitNames(List<String> s) {
        if (unitPattern == null) {
            init();
        }
        List<OffsetPosition> results = unitPattern.matcher(s);
        return results;
    }
    
	public List<OffsetPosition> inUnitNamesPairs(List<Pair<String,String>> s) {
        if (unitPattern == null) {
            init();
        }
        List<OffsetPosition> results = unitPattern.matcherPairs(s);
        return results;
    }
    
	public boolean inUnitDictionary(String s) {
		return unitTokens.contains(s.toLowerCase());
	}


}