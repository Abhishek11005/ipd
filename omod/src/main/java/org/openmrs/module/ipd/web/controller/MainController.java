package org.openmrs.module.ipd.web.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptName;
import org.openmrs.GlobalProperty;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.ipd.util.IpdConstants;
import org.openmrs.module.ipd.util.IpdUtils;
import org.openmrs.module.hospitalcore.HospitalCoreService;
import org.openmrs.module.hospitalcore.util.ConceptAnswerComparator;
import org.openmrs.module.hospitalcore.util.HospitalCoreConstants;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller("IpdMainController")
@RequestMapping("/module/ipd/main.htm")
public class MainController {
	
	@RequestMapping(method=RequestMethod.GET)
	public String firstView(
			@RequestParam(value ="searchPatient",required=false) String searchPatient,//patient name or patient identifier
			@RequestParam(value ="fromDate",required=false) String fromDate,
			@RequestParam(value ="toDate",required=false) String toDate,
			@RequestParam(value ="ipdWard",required=false) String[] ipdWard, //ipdWard multiselect
			@RequestParam(value ="tab",required=false) Integer tab, //If that tab is active we will set that tab active when page load.
			@RequestParam(value ="doctor",required=false) String[] doctor,
			Model model){
		
		//HospitalCoreService coreService = Context.getService(HospitalCoreService.class);
		//Concept conOutcome =conse.getConcept(HospitalCoreConstants.CONCEPT_ADMISSION_OUTCOME);
		/*System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX 1");
			conOutcome = coreService.insertConceptUnlessExist(HospitalCoreConstants.CONCEPT_DATATYPE_CODED, HospitalCoreConstants.CONCEPT_CLASS_QUESTION, HospitalCoreConstants.CONCEPT_ADMISSION_OUTCOME);
			String [] xxx=  new String[]{ "Improve", "Cured" , "Discharge on request" ,"LAMA", "Absconding", "Death"};
			coreService.addConceptAnswers(conse , conOutcome,xxx , user);
			System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX 2");*/
		//System.out.println("=========================================================");
		creatConceptQuestionAndAnswer(Context.getConceptService() , Context.getAuthenticatedUser() ,HospitalCoreConstants.CONCEPT_ADMISSION_OUTCOME,  new String[]{ "Improve", "Cured" , "Discharge on request" ,"LAMA", "Absconding", "Death"});
		//	System.out.println("=====================xong====================================");
		Concept ipdConcept = Context.getConceptService().getConceptByName(Context.getAdministrationService().getGlobalProperty(IpdConstants.PROPERTY_IPDWARD));
		List<ConceptAnswer> list = (ipdConcept!= null ?  new ArrayList<ConceptAnswer>(ipdConcept.getAnswers()) : null);
		if(CollectionUtils.isNotEmpty(list)){
			Collections.sort(list, new ConceptAnswerComparator());
		}
		model.addAttribute("listIpd", list);
		String doctorRoleProps = Context.getAdministrationService().getGlobalProperty(IpdConstants.PROPERTY_NAME_DOCTOR_ROLE);
		Role doctorRole = Context.getUserService().getRole(doctorRoleProps);
		if( doctorRole != null ){
			List<User> listDoctor = Context.getUserService().getUsersByRole(doctorRole);
			model.addAttribute("listDoctor",listDoctor);
		}
		
		model.addAttribute("fromDate",fromDate);
		model.addAttribute("toDate",toDate);
		model.addAttribute("tab",tab == null? 0 : tab.intValue());
		model.addAttribute("searchPatient",searchPatient);
		model.addAttribute("ipdWard",ipdWard);
		model.addAttribute("ipdWardString",IpdUtils.convertStringArraytoString(ipdWard));

		model.addAttribute("doctor",doctor);
		model.addAttribute("doctorString",IpdUtils.convertStringArraytoString(doctor));
		return "module/ipd/main";
	}
	private Concept insertConcept(ConceptService conceptService,
			String dataTypeName, String conceptClassName, String concept) {
		try {
			ConceptDatatype datatype = Context.getConceptService()
					.getConceptDatatypeByName(dataTypeName);
			ConceptClass conceptClass = conceptService
					.getConceptClassByName(conceptClassName);
			Concept con = conceptService.getConcept(concept);
			// System.out.println(con);
			if (con == null) {
				con = new Concept();
				ConceptName name = new ConceptName(concept,
						Context.getLocale());
				con.addName(name);
				con.setDatatype(datatype);
				con.setConceptClass(conceptClass);
				return conceptService.saveConcept(con);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	public void creatConceptQuestionAndAnswer(ConceptService conceptService,  User user ,String conceptParent, String...conceptChild) {
		// System.out.println("========= insertExternalHospitalConcepts =========");
		Concept concept = conceptService.getConcept(conceptParent);
		if(concept == null){
			insertConcept(conceptService, "Coded", "Question" , conceptParent);
		}
		if (concept != null) {
			
			for (String hn : conceptChild) {
				insertHospital(conceptService, hn);
			}
			addConceptAnswers(concept, conceptChild, user);
		}
	}
	
	private void addConceptAnswers(Concept concept, String[] answerNames,
			User creator) {
		Set<Integer> currentAnswerIds = new HashSet<Integer>();
		for (ConceptAnswer answer : concept.getAnswers()) {
			currentAnswerIds.add(answer.getAnswerConcept().getConceptId());
		}
		boolean changed = false;
		for (String answerName : answerNames) {
			Concept answer = Context.getConceptService().getConcept(answerName);
			if (!currentAnswerIds.contains(answer.getConceptId())) {
				changed = true;
				ConceptAnswer conceptAnswer = new ConceptAnswer(answer);
				conceptAnswer.setCreator(creator);
				concept.addAnswer(conceptAnswer);
			}
		}
		if (changed) {
			Context.getConceptService().saveConcept(concept);
		}
	}
	private Concept insertHospital(ConceptService conceptService,
			String hospitalName) {
		try {
			ConceptDatatype datatype = Context.getConceptService()
					.getConceptDatatypeByName("N/A");
			ConceptClass conceptClass = conceptService
					.getConceptClassByName("Misc");
			Concept con = conceptService.getConceptByName(hospitalName);
			// System.out.println(con);
			if (con == null) {
				con = new Concept();
				ConceptName name = new ConceptName(hospitalName,
						Context.getLocale());
				con.addName(name);
				con.setDatatype(datatype);
				con.setConceptClass(conceptClass);
				return conceptService.saveConcept(con);
			}
			return con;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	

}