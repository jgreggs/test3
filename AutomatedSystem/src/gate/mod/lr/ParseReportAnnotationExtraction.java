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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import gate.*;
import gate.creole.*;
import gate.creole.metadata.*;
import gate.creole.ontology.DatatypeProperty;
import gate.creole.ontology.InvalidValueException;
import gate.creole.ontology.Literal;
import gate.creole.ontology.OClass;
import gate.creole.ontology.OConstants;
import gate.creole.ontology.OInstance;
import gate.creole.ontology.OURI;
import gate.creole.ontology.OUtils;
import gate.creole.ontology.Ontology;
import gate.util.*;


/** 
 * This class is the implementation of the resource A_ONTOLOGYJAPEMOD.
 */
@CreoleResource(name = "A_ParseReportAnnotationExtractionMod",
        comment = "Add a descriptive comment about this resource")
public class ParseReportAnnotationExtraction  extends AbstractLanguageAnalyser
  implements ProcessingResource {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

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
	  
	  protected Ontology ontology = null;

	  private boolean debug = false;
	  
	  
		/** 
		 *  Should be called to execute this PR on a document.
		 *  Placed after JAPE Process Resource(s)
		 */
		public void execute() throws ExecutionException {
			
			System.out.println("Process is executed.");
			
			
			AnnotationSet set = inputASName == null || inputASName.trim().length() == 0 ? 
					document.getAnnotations() : document.getAnnotations(inputASName);
								
			AnnotationSet outSet = outputASName == null || outputASName.trim().length() == 0 ? 
					document.getAnnotations() : document.getAnnotations(outputASName);
														
			AnnotationSet reportSectionSet = set.get("Ciri_ReportHeader");
			
			for(Iterator<?> rowAnnIter = reportSectionSet.iterator(); rowAnnIter.hasNext();){
				
				Annotation rowAnn = (Annotation) rowAnnIter.next();
				String content = gate.Utils.stringFor(document, rowAnn);
				
				Out.prln("Content " + content);
				

				

			}
			
		}
		
		public void calculateRank(ArrayList<String> content, Annotation rowAnn){
		
			int indexSum = 0;
			int sum = 0;
			
			//Can move this to segment pr.
			//extract from div
			//extract from annotation
			//scan sentence annotation
			//parse text
			//set up methods for each rating
			//get values
			for(String value : content){
										
				String className = (String) rowAnn.getType() + "_" + rowAnn.getId();
				String ratingValue = (String) value;

				OClass aClass = null;
				if(className != null || !className.startsWith("isEmpty")){
					aClass = ontology.getOClass(ontology.createOURIForName(className));
					
					Out.println(className + "\n");
					
					String combinedName = className + "_" + ratingValue;
					String mentionName = OUtils.toResourceName(combinedName);
					
					DatatypeProperty prop = ontology.getDatatypeProperty(ontology.createOURIForName("mentionText"));
					
					OURI mentionURI = ontology.createOURIForName(mentionName);
					
					if(!ontology.containsOInstance(mentionURI)){
						
						OInstance inst = null;
						if(aClass != null)
							inst = ontology.addOInstance(mentionURI, aClass);
						
						try{
							
							Out.println("Inst: " + inst);
							Out.println("Rating: " + ratingValue);

							if(!ratingValue.equals("true")){
								if(inst != null)
									inst.addDatatypePropertyValue(prop, new Literal(ratingValue, OConstants.ENGLISH));
							}
						}catch(InvalidValueException e){
							e.getMessage();
						}
						
					}

				}
				
				DatatypeProperty prop = ontology.getDatatypeProperty(ontology.createOURIForName(className + "_" + ratingValue));
				
				Out.prln("Features before output: " + features);

			}
		
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
	
	  @CreoleParameter(comment="The ontology LR to be used by this transducer")
	  @Optional
	  @RunTime
	  public void setOntology(Ontology onto) {
	    ontology = onto;
	  }

	  public Ontology getOntology() {
	    return ontology;
	  }
	
} // class ReportAnnotationExtraction
