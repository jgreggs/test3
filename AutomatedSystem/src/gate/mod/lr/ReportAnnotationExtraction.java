/*
 *  ReportAnnotationExtraction.java
 *
 * Copyright (c) 2000-2012, The University of Sheffield.
 *
 * This file is part of GATE (see http://gate.ac.uk/), and is free
 * software, licenced under the GNU Library General Public License,
 * Version 3, 29 June 2007.
 *
 * A copy of this licence is included in the distribution in the file
 * licence.html, and is also available at http://gate.ac.uk/gate/licence.html.
 *
 *  Josh, 21/3/2016
 *
 * For details on the configuration options, see the user guide:
 * http://gate.ac.uk/cgi-bin/userguide/sec:creole-model:config
 */

package gate.mod.lr;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import gate.*;
import gate.alchemyAPI.EntityExtraction;
import gate.alchemyAPI.TextSentimentExtraction;
import gate.alchemyAPI.TextSpan;
import gate.creole.*;
import gate.creole.metadata.*;
import gate.creole.ontology.DataType;
import gate.creole.ontology.DatatypeProperty;
import gate.creole.ontology.InvalidValueException;
import gate.creole.ontology.Literal;
import gate.creole.ontology.OClass;
import gate.creole.ontology.OConstants;
import gate.creole.ontology.OInstance;
import gate.creole.ontology.OURI;
import gate.creole.ontology.ObjectProperty;
import gate.creole.ontology.Ontology;
import gate.mod.pr.dbconnection.MySQLDBConnection;
import gate.util.*;
import gate.wordnet.Synset;
import gate.wordnet.Word;
import gate.wordnet.WordNet;
import gate.wordnet.WordNetException;
import gate.wordnet.WordSense;


/** 
 * This class is the implementation of the resource A_ONTOLOGYJAPEMOD.
 */
@CreoleResource(name = "A_ReportAnnotationExtractionMod",
        comment = "Add a descriptive comment about this resource")
public class ReportAnnotationExtraction  extends TextSentimentExtraction
  implements ProcessingResource {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

/*	  //private WordNet wordNet;        // WordNet instance
	  private URL configFileURL;      // URL to WordNet configuration file
	    */
	/**
	   * Annotation set that contains the segment annotation and the annotations to
	   * be copied.
	   */
	  private String inputASName;
	  
	  /**
	   * Annotation set that contains the segment annotation and the annotations to
	   * set as output.
	   */
	  private String outputASName;
	  
	  private boolean debug = false;
	  // Exit gracefully if exception caught on init()
	  private boolean gracefulExit;	  
	  
  	  private static final String EXECUTIVE_SUMMARY = "Executive Summary";
  	  private static final String SECTION_1 = "Respect for the Integrity of the Person";
  	  private static final String SECTION_2 = "Respect for Civil Liberties";
  	  private static final String SECTION_3 = "Respect for Political Rights";
  	  private static final String SECTION_5 = "Governmental Attitude Regarding International and "
  				+ "Nongovernmental Investigation of Alleged Violations of Human Rights";
  	  private static final String SECTION_6 = "Discrimination, Societal Abuses, and Trafficking in Persons";
  	  private static final String SECTION_7 = "Worker Rights";
  	  
  	  private static final String KILL_SUBSECTION = "a. Arbitrary or Unlawful Deprivation of Life";
  	  private static final String DISAP_SUBSECTION = "b. Disappearance";
  	  private static final String TORT_SUBSECTION = "c. Torture and Other Cruel, Inhuman, or Degrading Treatment or Punishment";
  	  private static final String POLPRIS_SUBSECTION = "Political Prisoners and Detainees";
  	  private static final String ASSN_SUBSECTION = "b. Freedom of Peaceful Assembly and Association";
  	  private static final String FORMOV_SUBSECTION = "d. Freedom of Movement, Internally Displaced Persons, Protection of Refugees, and Stateless Persons";
  	  private static final String DOMMOV_SUBSECTION = "d. Freedom of Movement, Internally Displaced Persons, Protection of Refugees, and Stateless Persons";
  	  private static final String SPEECH_SUBSECTION = "a. Freedom of Speech and Press";
  	 // private static final String ELECSD_SUBSECTION = "Worker Rights";
  	  private static final String NEWRELFRE_SUBSECTION = "c. Freedom of Religion";
  	  private static final String WORKER_SUBSECTION = "Worker Rights";
  	  private static final String WECON_SUBSECTION = "Women";  	  
  	  private static final String WOPOL_SUBSECTION = "Women"; 
  	  private static final String WOSOC_SUBSECTION = "Women"; 
  	  private static final String INJUD_SUBSECTION = "Denial of Fair Public Trial";
  	  
  	  private static final String DISAP = "DISAP";
  	  private static final String TORT = "TORT";
  	  private static final String POLPRIS = "POLPRIS";
  	  private static final String KILL = "KILL";
  	  private static final String ASSN = "ASSN";
  	  private static final String FORMOV = "FORMOV";
  	  private static final String DOMMOV = "DOMMOV";
  	  private static final String SPEECH = "SPEECH";
  	  private static final String ELECSD = "ELECSD";
  	  private static final String NEWRELFRE = "NEW_RELFRE";
  	  private static final String WORKER = "WORKER";
  	  private static final String WECON = "WECON";
  	  private static final String WOPOL = "WOPOL";
  	  private static final String WESOC = "WESOC";
  	  private static final String INJUD = "INJUD";  
  	   
	  	/** 
		 *  Should be called to execute this PR on a document.
		 *  Placed after JAPE Process Resource(s)
		 */
  	  	@Override
		public void execute() throws ExecutionException {			

			alchemy.SetAPIKey("7f8cbbf5c9b38a7f69221249cbe336786ee0dbc7");
			
			AnnotationSet inputAS = inputASName == null || inputASName.trim().length() == 0 ? 
					document.getAnnotations() : document.getAnnotations(inputASName);
								
			AnnotationSet outputAS = outputASName == null || outputASName.trim().length() == 0 ? 
					document.getAnnotations() : document.getAnnotations(outputASName);
																									
			AnnotationSet sectionHeadingSet = inputAS.get("Ciri_SubSection");
			
			Annotation firstAnn = null;
			Annotation lastAnn = null;
			
			Node start = null;
			Node end = null;
			
			List<Annotation> annList = new ArrayList<>((AnnotationSet) sectionHeadingSet);
			
			Collections.sort(annList, new OffsetComparator());
		
			int found = 0;
			
			for(Iterator<Annotation> iter = annList.iterator(); iter.hasNext();){

				if(start == null){
					firstAnn = (Annotation) iter.next();
				}else{
					firstAnn = lastAnn;
				}
				
				String text = gate.Utils.stringFor(document, firstAnn);
				
				if(text.equalsIgnoreCase(EXECUTIVE_SUMMARY)){
					found++;

					if(found >= 2){

						break;
						
					}
					
				}
				
				start = firstAnn.getStartNode();
								
				if(iter.hasNext()){
				
					lastAnn = (Annotation) iter.next();
				
					end = lastAnn.getStartNode();
				}
				
				features.put("rule", "extract");
				
				outputAS.add(start, end, "Ciri_ReportSection", features);

			}
			
			TreeMap<String, Object> ratingsMap = new TreeMap<String, Object>();
			
			AnnotationSet newReportSectionSet = outputAS.get("Ciri_ReportSection");
			
			int disapRatingValue = 0;
			int tortRatingValue = 0;
			int polprisRatingValue = 0;
			int killRatingValue = 0;
			int assnRatingValue = 0;
			int formovRatingValue = 0;
			int dommovRatingValue = 0;
			int speechRatingValue = 0;
			int elecsdRatingValue = 0;
			int newrelfreRatingValue = 0;
			int workerRatingValue = 0;
			int weconRatingValue = 0;
			int wopolRatingValue = 0;
			int wesocRatingValue = 0;
			int injudRatingValue = 0;
			
			for(Annotation anno : newReportSectionSet){

				if(anno != null){
					
					String annoContent = gate.Utils.stringFor(document, anno);
					
	  				/*As the annotation iterates check the report sections.
	  				  Report sections need to match those in CIRI.
	  				  Create conditionals for each report section to each method
	  				*/
					
					try {
						
						//Put Executive Summary condition here for all ratings
						
						if(annoContent.contains(KILL_SUBSECTION)){
							
	  	  					killRatingValue = calculateOccurrenceRatingValue(anno, inputAS, outputAS);
	  	  					ratingsMap.put(KILL, killRatingValue);
						
						}if(annoContent.contains(DISAP_SUBSECTION)){

  	  						disapRatingValue = calculateOccurrenceRatingValue(anno, inputAS, outputAS);
	  	  					ratingsMap.put(DISAP, disapRatingValue);

						}if(annoContent.contains(TORT_SUBSECTION)){
							
	  	  					tortRatingValue = calculateOccurrenceRatingValue(anno, inputAS, outputAS);
	  	  					ratingsMap.put(TORT, tortRatingValue);

						}if(annoContent.contains(POLPRIS_SUBSECTION)){
						
	  	  					polprisRatingValue = calculateOccurrenceRatingValue(anno, inputAS, outputAS);
	  	  					ratingsMap.put(POLPRIS, polprisRatingValue);
	  	  					
						}if(annoContent.contains(ASSN_SUBSECTION)){
							
						assnRatingValue = calculateASSNRating(anno, outputAS);
						ratingsMap.put(ASSN, assnRatingValue);
						
					}if(annoContent.contains(FORMOV_SUBSECTION)){
						
						formovRatingValue = calculateFORMOVRating(anno, outputAS);
						ratingsMap.put(FORMOV, formovRatingValue);
						
					}if(annoContent.contains(DOMMOV_SUBSECTION)){
						Out.prln("dom");
						dommovRatingValue = calculateDOMMOVRating(anno, outputAS);
						ratingsMap.put(DOMMOV, dommovRatingValue);
						
					}if(annoContent.contains(SPEECH_SUBSECTION)){
						
						speechRatingValue = calculateSPEECHRating(anno, outputAS);
						ratingsMap.put(SPEECH, speechRatingValue);
						
					}if(annoContent.contains(SECTION_3)){
						
						elecsdRatingValue = calculateELECSDRating(anno, outputAS);
						ratingsMap.put(ELECSD, elecsdRatingValue);
						
					}if(annoContent.contains(NEWRELFRE_SUBSECTION)){
						
						newrelfreRatingValue = calculateNEWRELFRERating(anno, outputAS);
						ratingsMap.put(NEWRELFRE, newrelfreRatingValue);
						
					}if(annoContent.contains(INJUD_SUBSECTION)){
						
						injudRatingValue = calculateIndepJudiciaryRating(anno, outputAS);
						ratingsMap.put(INJUD, injudRatingValue);
						
					}//if(annoContent.contains(SECTION_6)){
						
						//if(annoContent.contains(WECON_SUBSECTION)){
							weconRatingValue = calculateWomenRightsRating(anno, outputAS);
							ratingsMap.put(WECON, 1);
						//}
						
						//if(annoContent.contains(WOPOL_SUBSECTION)){
							wopolRatingValue = calculateWomenRightsRating(anno, outputAS);
							ratingsMap.put(WOPOL, 1);
						//}
					
					//}
					if(annoContent.contains(SECTION_7)){ 
						
						workerRatingValue = calculateWORKERRating(anno, outputAS);
						ratingsMap.put(WORKER, workerRatingValue);
						
					}

						
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ResourceInstantiationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
										
				}
					
			}
			
			Out.prln("storeCIRIRatings****ratingsMap: " + ratingsMap);
			
			try {
				ratingsMap.put("ctry", "Afghanistan");
				ratingsMap.put("year", 1983);
				storeCIRIRatings(ratingsMap);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
  	  	
		private void storeCIRIRatings(TreeMap<String, Object> ratingsMap) throws Exception {
			
			MySQLDBConnection dbConnection = new MySQLDBConnection();
			
			dbConnection.createAutomatedTable(ratingsMap, "Afghanistan_1983");
			
		}

		public String getSubSectionExists(String sentString){

		    String separator_1 = "Country Reports on Human Rights";
		    String separator_2 = "Bureau of Democracy, Human Rights and Labor";
		    
			ArrayList<String> headerList = new ArrayList<String>();
			
			headerList.add(KILL_SUBSECTION);
			headerList.add(DISAP_SUBSECTION);
			headerList.add(TORT_SUBSECTION);
			headerList.add(POLPRIS_SUBSECTION);

	    	for(String subSection : headerList){
	    		
	    		if(sentString.contains(subSection)){
	    			int subSectionIndex = sentString.indexOf(subSection);
	    			
	    			String replaceSection = sentString.substring(0, subSectionIndex + subSection.length());
	    			sentString = sentString.replace(replaceSection, "");
	    		}
	    		
	    		if(sentString.contains(separator_1) && sentString.contains(separator_2)){
	    		
	    		    int subString_1 = sentString.indexOf(separator_1);
	    		    int subString_2 = sentString.indexOf(separator_2);
	    		    int subLength_2 = separator_2.length();
	    		      
	    		    String replaceSub = sentString.substring(subString_1, subString_2 + subLength_2);
	    		    
	    		    sentString = sentString.replace(replaceSub, "");
	    		}
	    		
			    String regex = "\\b[A-Z]+[\\s]+[0-9]+\\b";
			    Pattern pattern = Pattern.compile(regex);
			      
			    Matcher matches = pattern.matcher(sentString);
			    
	    		if(matches.find()){
	    			
	    			sentString = matches.replaceAll("");
	    		}
	    			
	    	}
	    	
	    	return sentString.trim();
			   
		}
		
		/**
		 * 
		 * Will use IncludeNums and WordNet to get number of occurrences. 
		 * Will also need to include Sentence annotation before IncludeNums for accuracy per sentence.
		 *
		 * @param annotation
		 * @param outputAS
		 * @return
		 * @throws WordNetException 
		 * @throws MalformedURLException 
		 * @throws ResourceInstantiationException 
		 * @throws ExecutionException 
		 */
		public int calculateOccurrenceRatingValue(Annotation annotation, AnnotationSet inputAS, AnnotationSet outputAS) throws MalformedURLException, ResourceInstantiationException, ExecutionException {
			
			int ratingValue = 0;
			int numberValue = 0;
			boolean numeric = false;
			
			double sentimentScore = 0;
			boolean sentenceGreaterThanOne = false;
			
			String sentenceContent = "";
			String sentString = null;
			List<Integer> numberValueList = new ArrayList<Integer>();
			
			Node startAnno = annotation.getStartNode();
			Node endAnno = annotation.getEndNode();
			
			AnnotationSet sentenceSet = outputAS.get("Sentence", startAnno.getOffset(), endAnno.getOffset());
			
			List<Annotation> sentenceList = new ArrayList<Annotation>(Utils.inDocumentOrder(sentenceSet));
									
			int sentenceSize = sentenceList.size();

			AnnotationSet occuranceSet = outputAS.get("IncludeNums", startAnno.getOffset(), endAnno.getOffset());

			for(Annotation annoOccurance : occuranceSet){
				
				String numberString = gate.Utils.stringFor(document, annoOccurance);
				
				numeric = StringUtils.isNumeric(numberString);
				
				if(numeric){
					
					numberValue = Integer.parseInt(numberString);

					numberValueList.add(numberValue);
		
				}
				
			}	
						
			for(int i = 0; i < sentenceSize; i += getNumberOfSentencesInBatch()){
												
				String sentenceContentSet = gate.Utils.stringFor(document, sentenceSet);

				String reports_No = "no reports";
				sentenceContentSet = getSubSectionExists(sentenceContentSet);

				int first = sentenceContent.indexOf(".");
				
				int sentenceContentLength = sentenceContentSet.length();
				if(sentenceContentLength > 2 && sentenceSize == 1){
					
					if(sentenceContentSet.contains(reports_No)){
						ratingValue = 2;
						break;
					}
					
				}else if(sentenceContentSet.length() > 2 && (first >= 0) && (first - sentenceContentSet.lastIndexOf(".")) == 0){
					
					if(sentenceContentSet.contains(reports_No)){
						ratingValue = 2;
						break;
					}
					
				}
				
				int endIndex = i + getNumberOfSentencesInBatch() - 1;
				
				if(endIndex >= sentenceList.size()) 
					endIndex = sentenceList.size() - 1;

				// we add numberOfSentencesInContext in left and right context if
				// they are available
				int contextStartIndex = i - getNumberOfSentencesInContext();

				if(contextStartIndex < 0) 
					contextStartIndex = 0;
				
		      	int contextEndIndex = endIndex + getNumberOfSentencesInContext();
		      	if(contextEndIndex >= sentenceList.size()) {
		      		contextEndIndex = sentenceList.size() - 1;
		      	}
		      
		      	// obtain the string to be annotated
		      	sentString = Utils.stringFor(document, Utils.start(sentenceList.get(contextStartIndex)), Utils.end(sentenceList.get(contextEndIndex)));
		      
			    sentString = getSubSectionExists(sentString);
			    
			    //Out.prln("SentString: \n" + sentString);
			    
				String result = processString(sentString.toString());
				
				int docSentimentStart = result.indexOf("<docSentiment>");
		
				if(docSentimentStart > -1){
					
					int scoreStartIndex = result.lastIndexOf("<score>");
					int scoreEndIndex = result.indexOf("</score>");
					
					if(scoreStartIndex > -1){
						
						sentimentScore = Double.parseDouble(result.substring(scoreStartIndex + 8, scoreEndIndex));
						//Out.prln(sentimentScore);
					}else{
						ratingValue = 2;
						break;
					}

				}
			    
			    sentenceGreaterThanOne = true;

			}
			
			if(sentenceGreaterThanOne){
			    
			    String reports_No = "no reports";
			    
				String reports_Occasional = "occasion";
				String reports_However = "however";
				String reports_But = "but";
				
				String reports_yes = "were reports";
				
				String reports_some = "were some reports";
				
				String reports_widespread = "widespread";
				String reports_systematic = "systematic";
				String reports_numerous = "numerous";
				String reports_credible = "credible reports";
				
				if(sentString.contains(reports_credible)){
					ratingValue = 1;
					if(sentString.contains("government")){
						ratingValue = 0;
					}
					
				}else if(sentString.contains(reports_widespread) || sentString.contains(reports_systematic)){
					//Out.prln("wide_1");
					ratingValue = 0;
				}else if(sentString.contains(reports_numerous)){
					ratingValue = 1;
				}else if(sentString.contains(reports_yes) || sentString.contains(reports_some)){
					
					if(numeric){
						
						Collections.sort(numberValueList);
						
						int freqValue = 50;
						int lowValue = 0;
												
						for(int value : numberValueList){
							
							if(value < 50 && value > 0){
								ratingValue = 1;
							}else if(value >= 50){
								ratingValue = 0;
							}
						}
						
					}else if(!sentString.contains("government")){
						//Out.prln("wide_2");
						ratingValue = 2;
					}else{
						ratingValue = 1;
					}
						
				}else if(sentString.contains(reports_No) && (sentimentScore < 0.52)){
					//Out.prln("2 value");
					ratingValue = 2;
					
					if(sentString.contains("some") && sentString.contains("reports")){
						//Out.prln("1 value");
						ratingValue = 1;
					}
				}else if(sentString.contains(reports_Occasional) || sentString.contains(reports_But) || sentString.contains(reports_However)){
					ratingValue = 1;
				}else if(!sentString.contains(reports_No) && sentimentScore < 0.52){
					ratingValue = 1;
				}else if(sentimentScore > 0.55){
					//Out.prln("wide_3");
					ratingValue = 1;
				}
			    
			}
			
			return ratingValue;	
			
		}
		
		public int calculateSPEECHRating(Annotation annotation, AnnotationSet outputAS) throws ExecutionException{
		
			int ratingValue = 0;

			double sentimentScore = 0;
			boolean sentenceGreaterThanOne = false;
			
			String sentenceContent = "";
			String sentString = null;
			
			Node startAnno = annotation.getStartNode();
			Node endAnno = annotation.getEndNode();
			
			AnnotationSet sentenceSet = outputAS.get("Sentence", startAnno.getOffset(), endAnno.getOffset());
			
			List<Annotation> sentenceList = new ArrayList<Annotation>(Utils.inDocumentOrder(sentenceSet));
									
			int sentenceSize = sentenceList.size();
						
			for(int i = 0; i < sentenceSize; i += getNumberOfSentencesInBatch()){
														
				int endIndex = i + getNumberOfSentencesInBatch() - 1;
				
				if(endIndex >= sentenceList.size()) 
					endIndex = sentenceList.size() - 1;

				// we add numberOfSentencesInContext in left and right context if
				// they are available
				int contextStartIndex = i - getNumberOfSentencesInContext();

				if(contextStartIndex < 0) 
					contextStartIndex = 0;
				
		      	int contextEndIndex = endIndex + getNumberOfSentencesInContext();
		      	if(contextEndIndex >= sentenceList.size()) {
		      		contextEndIndex = sentenceList.size() - 1;
		      	}
		      
		      	// obtain the string to be annotated
		      	sentString = Utils.stringFor(document, Utils.start(sentenceList.get(contextStartIndex)), Utils.end(sentenceList.get(contextEndIndex)));
		      
			    sentString = getSubSectionExists(sentString);
			    
			    //Out.prln("SentString: \n" + sentString);
			    
				String result = processString(sentString.toString());
				
				int docSentimentStart = result.indexOf("<docSentiment>");
		
				if(docSentimentStart > -1){
					
					int scoreStartIndex = result.lastIndexOf("<score>");
					int scoreEndIndex = result.indexOf("</score>");
					
					if(scoreStartIndex > -1){
						
						sentimentScore = Double.parseDouble(result.substring(scoreStartIndex + 8, scoreEndIndex));
						//Out.prln(sentimentScore);
					}else{
						ratingValue = 2;
						break;
					}

				}
			    
			    sentenceGreaterThanOne = true;

			}
			
			if(sentenceGreaterThanOne){
			    
				String self_censorship = "self-censorship";
				String widespread = "widespread";
				String restricted = "restricted";
				String limited = "may limit";
				String limited_speech = "limit free speech";
				String prohibit = "law prohibits";
				String electoral_prohibit = "electoral law prohibits";
				String generally_respected = "respected";
				String government = "government";
				String no_gov_restrictions = "no government restrictions";
				String public_speech = "public speech";
				String generally_not_respected = "did not respect";
				String control = "control";
				String gov_restrictions = "government restrict";
				
				if(sentString.contains(government)){
					
					if(sentString.contains(no_gov_restrictions) 
							|| sentString.contains(generally_respected)){
						
						ratingValue = 2;
						
						if(sentString.contains(limited) 
								|| sentString.contains(limited_speech) 
								|| (sentString.contains(prohibit) && sentString.contains(public_speech))){
							ratingValue = 1;
						}
						
						
					}else if(sentString.contains(generally_not_respected)){
						ratingValue = 1;
						
						if(sentString.contains(gov_restrictions)){
							ratingValue = 0;
						}
					}
					
				}else if(sentString.contains(widespread) || sentString.contains(self_censorship)){
					ratingValue = 0;
				}
					
			}
				
			return ratingValue;	
			
		}
		
		public int calculateASSNRating(Annotation annotation, AnnotationSet outputAS) throws ExecutionException{
			
			int ratingValue = 0;

			double sentimentScore = 0;
			boolean sentenceGreaterThanOne = false;
			
			String sentenceContent = "";
			String sentString = null;
			
			Node startAnno = annotation.getStartNode();
			Node endAnno = annotation.getEndNode();
			
			AnnotationSet sentenceSet = outputAS.get("Sentence", startAnno.getOffset(), endAnno.getOffset());
			
			List<Annotation> sentenceList = new ArrayList<Annotation>(Utils.inDocumentOrder(sentenceSet));
									
			int sentenceSize = sentenceList.size();
						
			for(int i = 0; i < sentenceSize; i += getNumberOfSentencesInBatch()){
														
				int endIndex = i + getNumberOfSentencesInBatch() - 1;
				
				if(endIndex >= sentenceList.size()) 
					endIndex = sentenceList.size() - 1;

				// we add numberOfSentencesInContext in left and right context if
				// they are available
				int contextStartIndex = i - getNumberOfSentencesInContext();

				if(contextStartIndex < 0) 
					contextStartIndex = 0;
				
		      	int contextEndIndex = endIndex + getNumberOfSentencesInContext();
		      	if(contextEndIndex >= sentenceList.size()) {
		      		contextEndIndex = sentenceList.size() - 1;
		      	}
		      
		      	// obtain the string to be annotated
		      	sentString = Utils.stringFor(document, Utils.start(sentenceList.get(contextStartIndex)), Utils.end(sentenceList.get(contextEndIndex)));
		      
			    sentString = getSubSectionExists(sentString);
			    
			    //Out.prln("SentString: \n" + sentString);
			    
				String result = processString(sentString.toString());
				
				int docSentimentStart = result.indexOf("<docSentiment>");
		
				if(docSentimentStart > -1){
					
					int scoreStartIndex = result.lastIndexOf("<score>");
					int scoreEndIndex = result.indexOf("</score>");
					
					if(scoreStartIndex > -1){
						
						sentimentScore = Double.parseDouble(result.substring(scoreStartIndex + 8, scoreEndIndex));
						//Out.prln(sentimentScore);
					}else{
						ratingValue = 2;
						break;
					}

				}
			    
			    sentenceGreaterThanOne = true;

			}
			
			if(sentenceGreaterThanOne){
			    
				String restricted = "restricted";
				String law_restricts = "law restricts";
				String limited = "limited";
				String generally_respected = "respected";
				String government = "government";
				String no_gov_restrictions = "no government restrictions";
				String generally_not_respected = "did not respect";
				String gov_restrictions = "government restrict";
				String severe_restrict = "severely restrict";
				
				if(sentString.contains(government)){
					
					if(sentString.contains(no_gov_restrictions) 
							|| sentString.contains(generally_respected)){
						
						ratingValue = 2;
						
						if(sentString.contains(law_restricts) || sentString.contains(limited)){
							ratingValue = 1;
						}
						
						
					}else if(sentString.contains(generally_not_respected)){
						ratingValue = 1;
						
						if(sentString.contains(gov_restrictions) || sentString.contains(severe_restrict)){
							ratingValue = 0;
						}
					}
					
				}
				
			}
		
			return ratingValue;	
			
		}
 	  	
		private int calculateELECSDRating(Annotation annotation, AnnotationSet outputAS) throws ExecutionException{

			int ratingValue = 0;

			double sentimentScore = 0;
			boolean sentenceGreaterThanOne = false;
			
			String sentenceContent = "";
			String sentString = null;
			
			Node startAnno = annotation.getStartNode();
			Node endAnno = annotation.getEndNode();
			
			AnnotationSet sentenceSet = outputAS.get("Sentence", startAnno.getOffset(), endAnno.getOffset());
			
			List<Annotation> sentenceList = new ArrayList<Annotation>(Utils.inDocumentOrder(sentenceSet));
									
			int sentenceSize = sentenceList.size();
						
			for(int i = 0; i < sentenceSize; i += getNumberOfSentencesInBatch()){
														
				int endIndex = i + getNumberOfSentencesInBatch() - 1;
				
				if(endIndex >= sentenceList.size()) 
					endIndex = sentenceList.size() - 1;

				// we add numberOfSentencesInContext in left and right context if
				// they are available
				int contextStartIndex = i - getNumberOfSentencesInContext();

				if(contextStartIndex < 0) 
					contextStartIndex = 0;
				
		      	int contextEndIndex = endIndex + getNumberOfSentencesInContext();
		      	if(contextEndIndex >= sentenceList.size()) {
		      		contextEndIndex = sentenceList.size() - 1;
		      	}
		      
		      	// obtain the string to be annotated
		      	sentString = Utils.stringFor(document, Utils.start(sentenceList.get(contextStartIndex)), Utils.end(sentenceList.get(contextEndIndex)));
		      
			    sentString = getSubSectionExists(sentString);
			    
			    //Out.prln("SentString: \n" + sentString);
			    
	/*			String result = processString(sentString.toString());
				
				int docSentimentStart = result.indexOf("<docSentiment>");
		
				if(docSentimentStart > -1){
					
					int scoreStartIndex = result.lastIndexOf("<score>");
					int scoreEndIndex = result.indexOf("</score>");
					
					if(scoreStartIndex > -1){
						
						sentimentScore = Double.parseDouble(result.substring(scoreStartIndex + 8, scoreEndIndex));
						Out.prln(sentimentScore);
					}else{
						ratingValue = 2;
						break;
					}

				}*/
			    
			    sentenceGreaterThanOne = true;

			}
			
			if(sentenceGreaterThanOne){
			    
				String limited = "limit";
				String no_right = "do not have the right";
				String restricted = "restricted";
				String government = "government";
				String no_gov_restrictions = "no government restrictions";
				String gov_restrictions = "government restrict";
				String cannot_choose = "cannot freely";
				String could_not_choose = "could not freely";
				String not_provide = "does not provide";
				
				if(sentString.contains(government)){
					
					if(sentString.contains(no_gov_restrictions)){
						
						ratingValue = 2;
						
						if(sentString.contains(limited) ){
							ratingValue = 1;
						}
						
						if(sentString.contains(no_right)){
							ratingValue = 0;
						}
						
						
					}else if(sentString.contains(gov_restrictions) || sentString.contains(restricted)){
						ratingValue = 0;
						
					} else if(sentString.contains(cannot_choose) || sentString.contains(could_not_choose) || sentString.contains(not_provide)){
						ratingValue = 0;
					}
					
				}
				
			}
		
			return ratingValue;	
		}

		private int calculateNEWRELFRERating(Annotation annotation, AnnotationSet outputAS) throws ExecutionException{

			int ratingValue = 0;

			double sentimentScore = 0;
			boolean sentenceGreaterThanOne = false;
			
			String sentenceContent = "";
			String sentString = null;
			
			Node startAnno = annotation.getStartNode();
			Node endAnno = annotation.getEndNode();
			
			AnnotationSet sentenceSet = outputAS.get("Sentence", startAnno.getOffset(), endAnno.getOffset());
			
			List<Annotation> sentenceList = new ArrayList<Annotation>(Utils.inDocumentOrder(sentenceSet));
									
			int sentenceSize = sentenceList.size();
						
			for(int i = 0; i < sentenceSize; i += getNumberOfSentencesInBatch()){
														
				int endIndex = i + getNumberOfSentencesInBatch() - 1;
				
				if(endIndex >= sentenceList.size()) 
					endIndex = sentenceList.size() - 1;

				// we add numberOfSentencesInContext in left and right context if
				// they are available
				int contextStartIndex = i - getNumberOfSentencesInContext();

				if(contextStartIndex < 0) 
					contextStartIndex = 0;
				
		      	int contextEndIndex = endIndex + getNumberOfSentencesInContext();
		      	if(contextEndIndex >= sentenceList.size()) {
		      		contextEndIndex = sentenceList.size() - 1;
		      	}
		      
		      	// obtain the string to be annotated
		      	sentString = Utils.stringFor(document, Utils.start(sentenceList.get(contextStartIndex)), Utils.end(sentenceList.get(contextEndIndex)));
		      
			    sentString = getSubSectionExists(sentString);
			    
			    //Out.prln("SentString: \n" + sentString);
			    
	/*			String result = processString(sentString.toString());
				
				int docSentimentStart = result.indexOf("<docSentiment>");
		
				if(docSentimentStart > -1){
					
					int scoreStartIndex = result.lastIndexOf("<score>");
					int scoreEndIndex = result.indexOf("</score>");
					
					if(scoreStartIndex > -1){
						
						sentimentScore = Double.parseDouble(result.substring(scoreStartIndex + 8, scoreEndIndex));
						Out.prln(sentimentScore);
					}else{
						ratingValue = 2;
						break;
					}

				}*/
			    
			    sentenceGreaterThanOne = true;

			}
			
			if(sentenceGreaterThanOne){
			    
				String restricted = "restricted";
				String limited = "may limit";
				String limited_speech = "limit free speech";
				String prohibit = "law prohibits";
				String electoral_prohibit = "electoral law prohibits";
				String generally_respected = "respected";
				String government = "government";
				String no_gov_restrictions = "no government restrictions";
				String public_speech = "public speech";
				String generally_not_respected = "did not respect";
				String control = "control";
				String gov_restrictions = "government restrict";
				
				if(sentString.contains(government)){
					
					if(sentString.contains(no_gov_restrictions) 
							|| sentString.contains(generally_respected)){
						
						ratingValue = 2;
						
						if(sentString.contains(limited) 
								|| sentString.contains(limited_speech) 
								|| (sentString.contains(prohibit) && sentString.contains(public_speech))){
							ratingValue = 1;
						}
						
						
					}else if(sentString.contains(generally_not_respected)){
						ratingValue = 1;
						
						if(sentString.contains(gov_restrictions)){
							ratingValue = 0;
						}
					}
					
				}
				
			}
		
			return ratingValue;	
		}

		private int calculateWORKERRating(Annotation annotation, AnnotationSet outputAS) throws ExecutionException{

			int ratingValue = 0;

			double sentimentScore = 0;
			boolean sentenceGreaterThanOne = false;
			
			String sentenceContent = "";
			String sentString = null;
			
			Node startAnno = annotation.getStartNode();
			Node endAnno = annotation.getEndNode();
			
			AnnotationSet sentenceSet = outputAS.get("Sentence", startAnno.getOffset(), endAnno.getOffset());
			
			List<Annotation> sentenceList = new ArrayList<Annotation>(Utils.inDocumentOrder(sentenceSet));
									
			int sentenceSize = sentenceList.size();
						
			for(int i = 0; i < sentenceSize; i += getNumberOfSentencesInBatch()){
														
				int endIndex = i + getNumberOfSentencesInBatch() - 1;
				
				if(endIndex >= sentenceList.size()) 
					endIndex = sentenceList.size() - 1;

				// we add numberOfSentencesInContext in left and right context if
				// they are available
				int contextStartIndex = i - getNumberOfSentencesInContext();

				if(contextStartIndex < 0) 
					contextStartIndex = 0;
				
		      	int contextEndIndex = endIndex + getNumberOfSentencesInContext();
		      	if(contextEndIndex >= sentenceList.size()) {
		      		contextEndIndex = sentenceList.size() - 1;
		      	}
		      
		      	// obtain the string to be annotated
		      	sentString = Utils.stringFor(document, Utils.start(sentenceList.get(contextStartIndex)), Utils.end(sentenceList.get(contextEndIndex)));
		      
			    sentString = getSubSectionExists(sentString);
			    
			    //Out.prln("SentString: \n" + sentString);
			    
	/*			String result = processString(sentString.toString());
				
				int docSentimentStart = result.indexOf("<docSentiment>");
		
				if(docSentimentStart > -1){
					
					int scoreStartIndex = result.lastIndexOf("<score>");
					int scoreEndIndex = result.indexOf("</score>");
					
					if(scoreStartIndex > -1){
						
						sentimentScore = Double.parseDouble(result.substring(scoreStartIndex + 8, scoreEndIndex));
						Out.prln(sentimentScore);
					}else{
						ratingValue = 2;
						break;
					}

				}*/
			    
			    sentenceGreaterThanOne = true;

			}
			
			if(sentenceGreaterThanOne){
			    
				String restricted = "restricted";
				String not_allow = "not allow";
				String generally_respected = "respected";
				String government = "government";
				String no_gov_restrictions = "no government restrictions";
				String generally_not_respected = "did not respect";
				String gov_restrictions = "government restrict";
				String gov_allow = "government allow";
				
				if(sentString.contains(not_allow)){
					ratingValue = 1;
				} else if(sentString.contains(government)){
					
					if(sentString.contains(no_gov_restrictions) 
							|| sentString.contains(generally_respected)){
						
						ratingValue = 2;
						

						
					}else if(sentString.contains(generally_not_respected)){
						ratingValue = 1;
						
						if(sentString.contains(gov_restrictions)){
							ratingValue = 0;
						}
					}
					
				}
				
			}
		
			return ratingValue;	
		}

		private int calculateDOMMOVRating(Annotation annotation, AnnotationSet outputAS) throws ExecutionException{

			int ratingValue = 0;

			double sentimentScore = 0;
			boolean sentenceGreaterThanOne = false;
			
			String sentenceContent = "";
			String sentString = null;
			
			Node startAnno = annotation.getStartNode();
			Node endAnno = annotation.getEndNode();
			
			AnnotationSet sentenceSet = outputAS.get("Sentence", startAnno.getOffset(), endAnno.getOffset());
			
			List<Annotation> sentenceList = new ArrayList<Annotation>(Utils.inDocumentOrder(sentenceSet));
									
			int sentenceSize = sentenceList.size();
						
			for(int i = 0; i < sentenceSize; i += getNumberOfSentencesInBatch()){
														
				int endIndex = i + getNumberOfSentencesInBatch() - 1;
				
				if(endIndex >= sentenceList.size()) 
					endIndex = sentenceList.size() - 1;

				// we add numberOfSentencesInContext in left and right context if
				// they are available
				int contextStartIndex = i - getNumberOfSentencesInContext();

				if(contextStartIndex < 0) 
					contextStartIndex = 0;
				
		      	int contextEndIndex = endIndex + getNumberOfSentencesInContext();
		      	if(contextEndIndex >= sentenceList.size()) {
		      		contextEndIndex = sentenceList.size() - 1;
		      	}
		      
		      	// obtain the string to be annotated
		      	sentString = Utils.stringFor(document, Utils.start(sentenceList.get(contextStartIndex)), Utils.end(sentenceList.get(contextEndIndex)));
		      
			    sentString = getSubSectionExists(sentString);
			    
			    //Out.prln("SentString: \n" + sentString);
			    
	/*			String result = processString(sentString.toString());
				
				int docSentimentStart = result.indexOf("<docSentiment>");
		
				if(docSentimentStart > -1){
					
					int scoreStartIndex = result.lastIndexOf("<score>");
					int scoreEndIndex = result.indexOf("</score>");
					
					if(scoreStartIndex > -1){
						
						sentimentScore = Double.parseDouble(result.substring(scoreStartIndex + 8, scoreEndIndex));
						Out.prln(sentimentScore);
					}else{
						ratingValue = 2;
						break;
					}

				}*/
			    
			    sentenceGreaterThanOne = true;

			}
			
			if(sentenceGreaterThanOne){
			    
				String restricted = "restricted";
				String limited = "limit";
				String generally_respected = "respected";
				String government = "government";
				String no_gov_restrictions = "no government restrictions";
				String generally_not_respected = "did not respect";
				String gov_restrictions = "government restrict";
				String some_restrict = "some restrict";
				
				if(sentString.contains(government)){
					
					if(sentString.contains(no_gov_restrictions) 
							|| sentString.contains(generally_respected)){
						
						ratingValue = 2;
						
						if(sentString.contains(limited) || sentString.contains(some_restrict)){
							ratingValue = 1;
						}
						
						
					}else if(sentString.contains(generally_not_respected)){
						ratingValue = 1;
						
						if(sentString.contains(gov_restrictions)){
							ratingValue = 0;
						}
					}
					
				}else{
					ratingValue = 0;
				}
				
			}
		
			return ratingValue;	
		}

		private int calculateFORMOVRating(Annotation annotation, AnnotationSet outputAS) throws ExecutionException{
			
			int ratingValue = 0;

			double sentimentScore = 0;
			boolean sentenceGreaterThanOne = false;
			
			String sentenceContent = "";
			String sentString = null;
			
			Node startAnno = annotation.getStartNode();
			Node endAnno = annotation.getEndNode();
			
			AnnotationSet sentenceSet = outputAS.get("Sentence", startAnno.getOffset(), endAnno.getOffset());
			
			List<Annotation> sentenceList = new ArrayList<Annotation>(Utils.inDocumentOrder(sentenceSet));
									
			int sentenceSize = sentenceList.size();
						
			for(int i = 0; i < sentenceSize; i += getNumberOfSentencesInBatch()){
														
				int endIndex = i + getNumberOfSentencesInBatch() - 1;
				
				if(endIndex >= sentenceList.size()) 
					endIndex = sentenceList.size() - 1;

				// we add numberOfSentencesInContext in left and right context if
				// they are available
				int contextStartIndex = i - getNumberOfSentencesInContext();

				if(contextStartIndex < 0) 
					contextStartIndex = 0;
				
		      	int contextEndIndex = endIndex + getNumberOfSentencesInContext();
		      	if(contextEndIndex >= sentenceList.size()) {
		      		contextEndIndex = sentenceList.size() - 1;
		      	}
		      
		      	// obtain the string to be annotated
		      	sentString = Utils.stringFor(document, Utils.start(sentenceList.get(contextStartIndex)), Utils.end(sentenceList.get(contextEndIndex)));
		      
			    sentString = getSubSectionExists(sentString);
			    
			    //Out.prln("SentString: \n" + sentString);
			    
	/*			String result = processString(sentString.toString());
				
				int docSentimentStart = result.indexOf("<docSentiment>");
		
				if(docSentimentStart > -1){
					
					int scoreStartIndex = result.lastIndexOf("<score>");
					int scoreEndIndex = result.indexOf("</score>");
					
					if(scoreStartIndex > -1){
						
						sentimentScore = Double.parseDouble(result.substring(scoreStartIndex + 8, scoreEndIndex));
						Out.prln(sentimentScore);
					}else{
						ratingValue = 2;
						break;
					}

				}*/
			    
			    sentenceGreaterThanOne = true;

			}
			
			if(sentenceGreaterThanOne){
			    
				String restricted = "restricted";
				String limited = "limit";
				String generally_respected = "respected";
				String government = "government";
				String no_gov_restrictions = "no government restrictions";
				String generally_not_respected = "did not respect";
				String gov_restrictions = "government restrict";
				String some_restrict = "some restrict";
				
				if(sentString.contains(government)){
					
					if(sentString.contains(no_gov_restrictions) 
							|| sentString.contains(generally_respected)){
						
						ratingValue = 2;
						
						if(sentString.contains(limited) || sentString.contains(some_restrict)){
							ratingValue = 1;
						}
						
						
					}else if(sentString.contains(generally_not_respected)){
						ratingValue = 1;
						
						if(sentString.contains(gov_restrictions)){
							ratingValue = 0;
						}
					}
					
				}else{
					ratingValue = 0;
				}
				
			}
		
			return ratingValue;	
		}  	  	
  	  	
		public int calculateIndepJudiciaryRating(Annotation annotation, AnnotationSet outputAS){
			
			int ratingValue = 0;

			double sentimentScore = 0;
			boolean sentenceGreaterThanOne = false;
			
			String sentenceContent = "";
			String sentString = null;
			
			Node startAnno = annotation.getStartNode();
			Node endAnno = annotation.getEndNode();
			
			AnnotationSet sentenceSet = outputAS.get("Sentence", startAnno.getOffset(), endAnno.getOffset());
			
			List<Annotation> sentenceList = new ArrayList<Annotation>(Utils.inDocumentOrder(sentenceSet));
									
			int sentenceSize = sentenceList.size();
						
			for(int i = 0; i < sentenceSize; i += getNumberOfSentencesInBatch()){
														
				int endIndex = i + getNumberOfSentencesInBatch() - 1;
				
				if(endIndex >= sentenceList.size()) 
					endIndex = sentenceList.size() - 1;

				// we add numberOfSentencesInContext in left and right context if
				// they are available
				int contextStartIndex = i - getNumberOfSentencesInContext();

				if(contextStartIndex < 0) 
					contextStartIndex = 0;
				
		      	int contextEndIndex = endIndex + getNumberOfSentencesInContext();
		      	if(contextEndIndex >= sentenceList.size()) {
		      		contextEndIndex = sentenceList.size() - 1;
		      	}
		      
		      	// obtain the string to be annotated
		      	sentString = Utils.stringFor(document, Utils.start(sentenceList.get(contextStartIndex)), Utils.end(sentenceList.get(contextEndIndex)));
		      
			    sentString = getSubSectionExists(sentString);
			    
			    //Out.prln("SentString: \n" + sentString);
			    
	/*			String result = processString(sentString.toString());
				
				int docSentimentStart = result.indexOf("<docSentiment>");
		
				if(docSentimentStart > -1){
					
					int scoreStartIndex = result.lastIndexOf("<score>");
					int scoreEndIndex = result.indexOf("</score>");
					
					if(scoreStartIndex > -1){
						
						sentimentScore = Double.parseDouble(result.substring(scoreStartIndex + 8, scoreEndIndex));
						Out.prln(sentimentScore);
					}else{
						ratingValue = 2;
						break;
					}

				}*/
			    
			    sentenceGreaterThanOne = true;

			}
			
			if(sentenceGreaterThanOne){
			    
				
				String restricted = "restricted";
				String limited = "may limit";
				String limited_speech = "limit free speech";
				String prohibit = "law prohibits";
				String electoral_prohibit = "electoral law prohibits";
				String generally_respected = "respected";
				String government = "government";
				String no_gov_restrictions = "no government restrictions";
				String public_speech = "public speech";
				String generally_not_respected = "did not respect";
				String control = "control";
				String gov_restrictions = "government restrict";
				
				if(sentString.contains(government)){
					
					if(sentString.contains(no_gov_restrictions) 
							|| sentString.contains(generally_respected)){
						
						ratingValue = 2;
						
						if(sentString.contains(limited) 
								|| sentString.contains(limited_speech) 
								|| (sentString.contains(prohibit) && sentString.contains(public_speech))){
							ratingValue = 1;
						}
						
						
					}else if(sentString.contains(generally_not_respected)){
						ratingValue = 1;
						
						if(sentString.contains(gov_restrictions)){
							ratingValue = 0;
						}
					}
					
				}
				
			}
		
			return ratingValue;	
				
		}
		
		public int calculateWomenRightsRating(Annotation annotation, AnnotationSet outputAS){
		
			int ratingValue = 0;

			double sentimentScore = 0;
			boolean sentenceGreaterThanOne = false;
			
			String sentenceContent = "";
			String sentString = null;
			
			Node startAnno = annotation.getStartNode();
			Node endAnno = annotation.getEndNode();
			
			AnnotationSet sentenceSet = outputAS.get("Sentence", startAnno.getOffset(), endAnno.getOffset());
			
			List<Annotation> sentenceList = new ArrayList<Annotation>(Utils.inDocumentOrder(sentenceSet));
									
			int sentenceSize = sentenceList.size();
						
			for(int i = 0; i < sentenceSize; i += getNumberOfSentencesInBatch()){
														
				int endIndex = i + getNumberOfSentencesInBatch() - 1;
				
				if(endIndex >= sentenceList.size()) 
					endIndex = sentenceList.size() - 1;

				// we add numberOfSentencesInContext in left and right context if
				// they are available
				int contextStartIndex = i - getNumberOfSentencesInContext();

				if(contextStartIndex < 0) 
					contextStartIndex = 0;
				
		      	int contextEndIndex = endIndex + getNumberOfSentencesInContext();
		      	if(contextEndIndex >= sentenceList.size()) {
		      		contextEndIndex = sentenceList.size() - 1;
		      	}
		      
		      	// obtain the string to be annotated
		      	sentString = Utils.stringFor(document, Utils.start(sentenceList.get(contextStartIndex)), Utils.end(sentenceList.get(contextEndIndex)));
		      
			    sentString = getSubSectionExists(sentString);
			    
			    //Out.prln("SentString: \n" + sentString);
			    
	/*			String result = processString(sentString.toString());
				
				int docSentimentStart = result.indexOf("<docSentiment>");
		
				if(docSentimentStart > -1){
					
					int scoreStartIndex = result.lastIndexOf("<score>");
					int scoreEndIndex = result.indexOf("</score>");
					
					if(scoreStartIndex > -1){
						
						sentimentScore = Double.parseDouble(result.substring(scoreStartIndex + 8, scoreEndIndex));
						Out.prln(sentimentScore);
					}else{
						ratingValue = 2;
						break;
					}

				}*/
			    
			    sentenceGreaterThanOne = true;

			}
			
			if(sentenceGreaterThanOne){
			    
				String restricted = "restricted";
				String limited = "may limit";
				String limited_speech = "limit free speech";
				String prohibit = "law prohibits";
				String electoral_prohibit = "electoral law prohibits";
				String generally_respected = "respected";
				String government = "government";
				String no_gov_restrictions = "no government restrictions";
				String public_speech = "public speech";
				String generally_not_respected = "did not respect";
				String control = "control";
				String gov_restrictions = "government restrict";
				
				if(sentString.contains(government)){
					
					if(sentString.contains(no_gov_restrictions) 
							|| sentString.contains(generally_respected)){
						
						ratingValue = 2;
						
						if(sentString.contains(limited) 
								|| sentString.contains(limited_speech) 
								|| (sentString.contains(prohibit) && sentString.contains(public_speech))){
							ratingValue = 1;
						}
						
						
					}else if(sentString.contains(generally_not_respected)){
						ratingValue = 1;
						
						if(sentString.contains(gov_restrictions)){
							ratingValue = 0;
						}
					}
					
				}
				
			}
		
			return ratingValue;	
			
		}	
		
		
	/**
	   * Annotation set to use for obtaining segment annotations and the annotations
	   * to copy into the composite document.
	   * 
	   * @return
	   */
	  public String getInputASName() {
	    return inputASName;
	  }

	  /**
	   * Annotation set to use for obtaining segment annotations and the annotations
	   * to copy into the composite document.
	   * 
	   * @param inputASName the inputASName to set
	   */
	  @CreoleParameter(comment="The name of the input annotation set.")
	  @Optional
	  @RunTime
	  public void setInputASName(String inputAS) {
	    this.inputASName = inputAS;
	  }
	  
		/**
	   * Annotation set to use for obtaining segment annotations and the annotations
	   * to set.
	   * 
	   * @return
	   */
	  public String getOutputASName() {
	    return outputASName;
	  }

	  /**
	   * Annotation set to use for obtaining segment annotations and the annotations
	   * to set.
	   * 
	   * @param outputASName the outputASName to set
	   */
	  @CreoleParameter(comment="The name of the output annotation set.")
	  @Optional
	  @RunTime
	  public void setOutputASName(String outputAS) {
	    this.outputASName = outputAS;
	  }
	
} // class ReportAnnotationExtraction
