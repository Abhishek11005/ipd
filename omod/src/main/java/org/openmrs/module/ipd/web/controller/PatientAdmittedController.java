/**
 *  Copyright 2011 Health Information Systems Project of India
 *
 *  This file is part of IPD module.
 *
 *  IPD module is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.

 *  IPD module is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with IPD module.  If not, see <http://www.gnu.org/licenses/>.
 *
 **/


package org.openmrs.module.ipd.web.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.Encounter;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.ipd.util.IpdConstants;
import org.openmrs.module.ipd.util.IpdUtils;
import org.openmrs.module.hospitalcore.IpdService;
import org.openmrs.module.hospitalcore.PatientDashboardService;
import org.openmrs.module.hospitalcore.model.DepartmentConcept;
import org.openmrs.module.hospitalcore.model.IpdPatientAdmitted;
import org.openmrs.module.hospitalcore.util.ConceptComparator;
import org.openmrs.module.hospitalcore.util.HospitalCoreConstants;
import org.openmrs.module.hospitalcore.util.PatientDashboardConstants;
import org.openmrs.module.hospitalcore.util.PatientUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p> Class: PatientAdmittedController </p>
 * <p> Package: org.openmrs.module.ipd.web.controller </p> 
 * <p> Author: Nguyen manh chuyen </p>
 * <p> Update by: Nguyen manh chuyen </p>
 * <p> Version: $1.0 </p>
 * <p> Create date: Mar 18, 2011 12:43:28 PM </p>
 * <p> Update date: Mar 18, 2011 12:43:28 PM </p>
 **/
@Controller("IPDPatientAdmittedController")
public class PatientAdmittedController {
	Log log = LogFactory.getLog(getClass());
	@RequestMapping(value = "/module/ipd/admittedPatientIndex.htm" , method=RequestMethod.GET)
	public String firstView(
			@RequestParam(value ="searchPatient",required=false) String searchPatient,//patient name or patient identifier
			@RequestParam(value ="fromDate",required=false) String fromDate,
			@RequestParam(value ="toDate",required=false) String toDate,
			@RequestParam(value ="ipdWardString",required=false) String ipdWardString,  //note ipdWardString = 1,2,3,4.....
			@RequestParam(value ="tab",required=false) Integer tab, //If that tab is active we will set that tab active when page load.
			@RequestParam(value ="doctorString",required=false) String doctorString,// note: doctorString= 1,2,3,4.....
			Model model){
		IpdService  ipdService = (IpdService)Context.getService(IpdService.class);
		List<IpdPatientAdmitted> listPatientAdmitted = ipdService.searchIpdPatientAdmitted(searchPatient, IpdUtils.convertStringToList(doctorString), fromDate, toDate,IpdUtils.convertStringToList(ipdWardString), "");
		
		Map<Integer,String> mapRelationName = new HashMap<Integer, String>();
		for( IpdPatientAdmitted admit : listPatientAdmitted ){
			PersonAttribute relationNameattr = admit.getPatient().getAttribute("Father/Husband Name");
			mapRelationName.put(admit.getId(), relationNameattr.getValue());
		}
		model.addAttribute("mapRelationName",mapRelationName);
		
		model.addAttribute("listPatientAdmitted",listPatientAdmitted);
		
		return "module/ipd/admittedList";
	}
	
	@RequestMapping(value = "/module/ipd/transfer.htm" , method=RequestMethod.POST)
	public String transferPost(
			@RequestParam("admittedId") Integer id,
			@RequestParam("toWard") Integer toWardId,
			@RequestParam("doctor") Integer doctorId,
			@RequestParam(value="bedNumber", required=false) String bed,
			Model model){
		IpdService  ipdService = (IpdService)Context.getService(IpdService.class);
		ipdService.transfer(id, toWardId, doctorId, bed);
		model.addAttribute("urlS", "main.htm?tab=1");
		model.addAttribute("message", "Succesfully");
		return "/module/ipd/thickbox/success";
	}
	
	@RequestMapping(value = "/module/ipd/transfer.htm" , method=RequestMethod.GET)
	public String transferView(
			@RequestParam(value ="id",required=false) Integer admittedId,
			Model model){
	
		IpdService  ipdService = (IpdService)Context.getService(IpdService.class);
		Concept ipdConcept = Context.getConceptService().getConceptByName(Context.getAdministrationService().getGlobalProperty(IpdConstants.PROPERTY_IPDWARD));
		model.addAttribute("listIpd", ipdConcept!= null ?  new ArrayList<ConceptAnswer>(ipdConcept.getAnswers()) : null);
		IpdPatientAdmitted admitted = ipdService.getIpdPatientAdmitted(admittedId);
		model.addAttribute("admitted", admitted);
		
		String doctorRoleProps = Context.getAdministrationService().getGlobalProperty(IpdConstants.PROPERTY_NAME_DOCTOR_ROLE);
		Role doctorRole = Context.getUserService().getRole(doctorRoleProps);
		if( doctorRole != null ){
			List<User> listDoctor = Context.getUserService().getUsersByRole(doctorRole);
			model.addAttribute("listDoctor",listDoctor);
		}
		
		Patient patient = admitted.getPatient();
		
		PersonAddress add = patient.getPersonAddress();
		String address = " "+add.getCountyDistrict() + " "+add.getCityVillage();
		
		model.addAttribute("address", address);
		
		PersonAttribute relationNameattr = patient.getAttribute("Father/Husband Name");
		model.addAttribute("relationName", relationNameattr.getValue());
		
		//Patient category
		model.addAttribute("patCategory", PatientUtil.getPatientCategory(patient));
		
		
		return "module/ipd/transferForm";
	}
	
	@RequestMapping(value = "/module/ipd/discharge.htm" , method=RequestMethod.POST)
	public String dischargePost(
			IpdFinalResultCommand command,
			Model model){
		IpdService  ipdService = (IpdService)Context.getService(IpdService.class);
		
		
		//star
		//
		AdministrationService administrationService = Context.getAdministrationService();
		GlobalProperty gpDiagnosis = administrationService.getGlobalPropertyObject(PatientDashboardConstants.PROPERTY_PROVISIONAL_DIAGNOSIS);
		GlobalProperty procedure = administrationService.getGlobalPropertyObject(PatientDashboardConstants.PROPERTY_POST_FOR_PROCEDURE);
		ConceptService conceptService = Context.getConceptService();
		Concept cDiagnosis = conceptService.getConceptByName(gpDiagnosis.getPropertyValue());
		Concept cProcedure = conceptService.getConceptByName(procedure.getPropertyValue());
		IpdPatientAdmitted admitted = ipdService.getIpdPatientAdmitted(command.getAdmittedId());
		Encounter ipdEncounter = admitted.getPatientAdmissionLog().getIpdEncounter();
		List<Obs> listObsOfIpdEncounter = new ArrayList<Obs>(ipdEncounter.getAllObs());
		Location location = new Location( 1 );
		
		User user = Context.getAuthenticatedUser();
		Date date = new Date();
		//diagnosis
		
		Set<Obs> obses = new HashSet(ipdEncounter.getAllObs());
		
		ipdEncounter.setObs(null);
		
		List<Concept> listConceptDianosisOfIpdEncounter = new ArrayList<Concept>();
		List<Concept> listConceptProcedureOfIpdEncounter = new ArrayList<Concept>();
		if(CollectionUtils.isNotEmpty(listObsOfIpdEncounter)){
			for(Obs obx : obses){
				if(obx.getConcept().getConceptId() == cDiagnosis.getConceptId()){
					listConceptDianosisOfIpdEncounter.add(obx.getValueCoded());
				}
				
				if(obx.getConcept().getConceptId() == cProcedure.getConceptId()){
					listConceptProcedureOfIpdEncounter.add(obx.getValueCoded());
				}
			}
		}
		
		List<Concept> listConceptDiagnosis = new ArrayList<Concept>();
		for( Integer cId : command.getSelectedDiagnosisList()){
			Concept cons = conceptService.getConcept(cId);
			listConceptDiagnosis.add(cons);
			if(!listConceptDianosisOfIpdEncounter.contains(cons)){
				Obs obsDiagnosis = new Obs();
				//obsDiagnosis.setObsGroup(obsGroup);
				obsDiagnosis.setConcept(cDiagnosis);
				obsDiagnosis.setValueCoded(cons);
				obsDiagnosis.setCreator( user);
				obsDiagnosis.setObsDatetime(date);
				obsDiagnosis.setLocation(location);
				obsDiagnosis.setDateCreated(date);
				obsDiagnosis.setPatient(ipdEncounter.getPatient());
				obsDiagnosis.setEncounter(ipdEncounter);
				obsDiagnosis =Context.getObsService().saveObs(obsDiagnosis, "update obs diagnosis if need");
				obses.add(obsDiagnosis);
			}
		}
		List<Concept> listConceptProcedure = new ArrayList<Concept>();
		if(!ArrayUtils.isEmpty(command.getSelectedProcedureList())){
			
			if( cProcedure == null ){
				try {
					throw new Exception("Post for procedure concept null");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			for( Integer pId : command.getSelectedProcedureList()){
				Concept cons = conceptService.getConcept(pId);
				listConceptProcedure.add(cons);
				if(!listConceptProcedureOfIpdEncounter.contains(cons)){
					Obs obsProcedure = new Obs();
					//obsDiagnosis.setObsGroup(obsGroup);
					obsProcedure.setConcept(cProcedure);
					obsProcedure.setValueCoded(conceptService.getConcept(pId));
					obsProcedure.setCreator(user);
					obsProcedure.setObsDatetime(date);
					obsProcedure.setLocation(location);
					obsProcedure.setPatient(ipdEncounter.getPatient());
					obsProcedure.setDateCreated(date);
					obsProcedure.setEncounter(ipdEncounter);
					obsProcedure =Context.getObsService().saveObs(obsProcedure, "update obs diagnosis if need");
					//ipdEncounter.addObs(obsProcedure);
					obses.add(obsProcedure);
				}
			}
		
		}
		
		// Remove obs diagnosis and procedure 
		
		for(  Concept con :  listConceptDianosisOfIpdEncounter ){
			if( !listConceptDiagnosis.contains(con)){
				for(Obs obx : listObsOfIpdEncounter){
					if( obx.getValueCoded().getConceptId().intValue() == con.getConceptId().intValue()){
						Context.getObsService().deleteObs(obx);
						obses.remove(obx);
					}
				}
			}
		}
		
		for(  Concept con :  listConceptProcedureOfIpdEncounter ){
			if( !listConceptProcedure.contains(con)){
				for(Obs obx : listObsOfIpdEncounter){
					if( obx.getValueCoded().getConceptId().intValue() == con.getConceptId().intValue()){
						Context.getObsService().deleteObs(obx);
						obses.remove(obx);
					}
				}
			}
		}
		
		ipdEncounter.setObs(obses);
		
		Context.getEncounterService().saveEncounter(ipdEncounter);
		
		//end
		
		
		
		ipdService.discharge(command.getAdmittedId(), command.getOutCome());
		model.addAttribute("urlS", "main.htm?tab=1");
		model.addAttribute("message", "Succesfully");
		return "/module/ipd/thickbox/success";
	}
	
	@RequestMapping(value = "/module/ipd/discharge.htm" , method=RequestMethod.GET)
	public String dischargeView(
			@RequestParam(value ="id",required=false) Integer admittedId, 
			@ModelAttribute("ipdCommand") IpdFinalResultCommand command,
			Model model){
		
		IpdService  ipdService = (IpdService)Context.getService(IpdService.class);
		IpdPatientAdmitted admitted = ipdService.getIpdPatientAdmitted(admittedId);
		
		Patient patient = admitted.getPatient();
		
		PersonAddress add = patient.getPersonAddress();
		String address = " "+add.getCountyDistrict() + " "+add.getCityVillage();
		model.addAttribute("address", address);
		
		PersonAttribute relationNameattr = patient.getAttribute("Father/Husband Name");
		model.addAttribute("relationName", relationNameattr.getValue());
		
		
		Concept outComeList = Context.getConceptService().getConceptByName(HospitalCoreConstants.CONCEPT_ADMISSION_OUTCOME);
		
		model.addAttribute("listOutCome", outComeList.getAnswers());
		
		
		
		model.addAttribute("admitted", admitted);
		
		//change CHUYEN
		
		//
		 ConceptService conceptService = Context.getConceptService();
		 AdministrationService administrationService = Context.getAdministrationService();
        String gpDiagnosis = administrationService.getGlobalProperty(PatientDashboardConstants.PROPERTY_PROVISIONAL_DIAGNOSIS);
        String gpProcedure = administrationService.getGlobalProperty(PatientDashboardConstants.PROPERTY_POST_FOR_PROCEDURE);
		List<Obs> obsList = new ArrayList<Obs>(admitted.getPatientAdmissionLog().getIpdEncounter().getAllObs());
		Concept conDiagnosis  = conceptService.getConcept(gpDiagnosis);
		
		Concept conProcedure  = conceptService.getConcept(gpProcedure);
		
		List<Concept> selectedDiagnosisList = new ArrayList<Concept>();
		List<Concept> selectedProcedureList = new ArrayList<Concept>();
		if(CollectionUtils.isNotEmpty(obsList)){
			for( Obs obs : obsList){
				if( obs.getConcept().getConceptId().equals(conDiagnosis.getConceptId()) ){
					selectedDiagnosisList.add(obs.getValueCoded());
				}
				if( obs.getConcept().getConceptId().equals(conProcedure.getConceptId()) ){
					selectedProcedureList.add(obs.getValueCoded());
				}
			}
		}

		//
		PatientDashboardService dashboardService = Context.getService(PatientDashboardService.class);
		List<Concept> diagnosis = dashboardService.listByDepartmentByWard(admitted.getAdmittedWard().getId(), DepartmentConcept.TYPES[0]);
		if(CollectionUtils.isNotEmpty(diagnosis) && CollectionUtils.isNotEmpty(selectedDiagnosisList)){
			diagnosis.removeAll(selectedDiagnosisList);
		}
		if(CollectionUtils.isNotEmpty(diagnosis)){
			Collections.sort(diagnosis, new ConceptComparator());
		}
		model.addAttribute("listDiagnosis", diagnosis);
		List<Concept> procedures = dashboardService.listByDepartmentByWard(admitted.getAdmittedWard().getId(), DepartmentConcept.TYPES[1]);
		if(CollectionUtils.isNotEmpty(procedures) && CollectionUtils.isNotEmpty(selectedProcedureList)){
			procedures.removeAll(selectedProcedureList);
		}
		if(CollectionUtils.isNotEmpty(procedures)){
			Collections.sort(procedures, new ConceptComparator());
		}
		model.addAttribute("listProcedures", procedures);
		//
		
/*		
		PatientDashboardService dashboardService = Context.getService(PatientDashboardService.class);
		List<Concept> diagnosis = dashboardService.searchDiagnosis(null);
		if(CollectionUtils.isNotEmpty(diagnosis) && CollectionUtils.isNotEmpty(selectedDiagnosisList)){
			diagnosis.removeAll(selectedDiagnosisList);
		}
		Collections.sort(diagnosis, new ConceptComparator());
		model.addAttribute("listDiagnosis", diagnosis);
		List<Concept> procedures = dashboardService.searchProcedure(null);
		if(CollectionUtils.isNotEmpty(procedures) && CollectionUtils.isNotEmpty(selectedProcedureList)){
			procedures.removeAll(selectedProcedureList);
		}
		Collections.sort(procedures, new ConceptComparator());
		model.addAttribute("listProcedures", procedures);*/
		Collections.sort(selectedDiagnosisList, new ConceptComparator());
		Collections.sort(selectedProcedureList, new ConceptComparator());
		//Patient category
		model.addAttribute("patCategory", PatientUtil.getPatientCategory(patient));
		model.addAttribute("sDiagnosisList", selectedDiagnosisList);
		model.addAttribute("sProcedureList", selectedProcedureList);
		
		
		return "module/ipd/dischargeForm";
	}
	
}
