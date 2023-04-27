package ls.lesm.service.impl;

import java.security.Principal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import ls.lesm.bos.EntryTypeBo;
import ls.lesm.bos.ModeBo;
import ls.lesm.bos.ShiftTypeBo;
import ls.lesm.bos.TimeSheetEntryBo;
import ls.lesm.constants.EntryTypeConstant;
import ls.lesm.exception.SupervisorAlreadyExistException;
import ls.lesm.model.MasterEmployeeDetails;
import ls.lesm.model.recruiter.Status;
import ls.lesm.model.timesheet.Approval;
import ls.lesm.model.timesheet.EntryType;
import ls.lesm.model.timesheet.Mode;
import ls.lesm.model.timesheet.ShiftType;
import ls.lesm.model.timesheet.TimeSheetEntry;
import ls.lesm.payload.request.TimeSheetEntryRequest;
import ls.lesm.repository.InternalProjectRepository;
import ls.lesm.repository.timesheet.ApprovalRepository;
import ls.lesm.repository.timesheet.EntryTypeRepository;
import ls.lesm.repository.timesheet.ModeRepository;
import ls.lesm.repository.timesheet.ShiftTypeRepository;
import ls.lesm.repository.timesheet.TimeSheetEntryRepository;

@Service
@Transactional
public class TimeSheetServiceImpl<T> extends EmployeeDetailsServiceImpl{
	
	@Autowired
	private ApprovalRepository approvalRepository;
	@Autowired
	private ModeRepository modeRepository;
	@Autowired
	private ShiftTypeRepository shiftTypeRepository;
	@Autowired
	private TimeSheetEntryRepository timeSheetEntryRepository;
	@Autowired
	private InternalProjectRepository internalProjectRepository;
	@Autowired
	private EntryTypeRepository entryTypeRepository;
	@Autowired
	private ModelMapper modelMapper;
	
	public ShiftTypeBo entity2BoShiftType(ShiftType shiftType) {

		return modelMapper.map(shiftType, ShiftTypeBo.class);
	}

	public ShiftType bO2EntityShiftType(ShiftTypeBo bo) {

		return modelMapper.map(bo, ShiftType.class);
	}

	public ModeBo entity2BoModeBo(Mode mode) {

		return modelMapper.map(mode, ModeBo.class);
	}

	public Mode bO2EntityMode(ModeBo bo) {

		return modelMapper.map(bo, Mode.class);
	}

	public EntryTypeBo entity2EntryTypeBo(EntryType entryType) {

		return modelMapper.map(entryType, EntryTypeBo.class);
	}

	public EntryType bO2EntityEntryType(EntryTypeBo bo) {

		return modelMapper.map(bo, EntryType.class);
	}
	
	public TimeSheetEntry bo2EntityTimeSheetEntry(TimeSheetEntryBo timeSheetEntryBo) {
		
		return modelMapper.map(timeSheetEntryBo, TimeSheetEntry.class);
	}
	
	public TimeSheetEntryBo entity2BoTimeSheetEntry(TimeSheetEntry timeSheetEntry) {
		
		return modelMapper.map(timeSheetEntry, TimeSheetEntryBo.class);
	}
	
	
	

	public Integer saveShiftType(ShiftTypeBo shiftTypeBo) {

		shiftTypeBo.setCreatedAt(LocalDateTime.now());
	
	 Optional<ShiftType> optShiftType =this.shiftTypeRepository.findByShiftCodeIgnoreCase(shiftTypeBo.getShiftCode());
	 
	 if(optShiftType.isPresent())
		 throw new SupervisorAlreadyExistException("This "+optShiftType.get().getShiftCode()+" is already present");
	 
	 	
		return this.shiftTypeRepository.save(bO2EntityShiftType(shiftTypeBo)).getId();
	}
	
	public List<ShiftTypeBo> getAllShiftType() {
		List<ShiftType> shiftTypes = this.shiftTypeRepository.findAll();
		return shiftTypes.stream().map(this::entity2BoShiftType).collect(Collectors.toList());
	}
	
	
	public Integer saveMode(ModeBo modeBo) {

		modeBo.setCreatedAt(LocalDateTime.now());
		Optional<Mode> optMode = this.modeRepository.findByModeIgnoreCase(modeBo.getMode());

		if (optMode.isPresent())
			throw new SupervisorAlreadyExistException("This mode " + optMode.get().getMode() + " is already present");

		return modeRepository.save(bO2EntityMode(modeBo)).getId();

	}
	
	public List<ModeBo> getAllModes() {
		List<Mode> modes = this.modeRepository.findAll();
		return modes.stream().map(this::entity2BoModeBo).collect(Collectors.toList());
	}

	public Integer saveEntryType(EntryTypeBo bo) {
		bo.setCreatedAt(LocalDateTime.now());

		Optional<EntryType> entryType =entryTypeRepository.findByEntryTypeIgnoreCase(bo.getEntryType());
		if (entryType.isPresent())
			throw new SupervisorAlreadyExistException("This OD Type " + bo.getEntryType() + " already present");

		return entryTypeRepository.save(bO2EntityEntryType(bo)).getId();
	}

	public List<EntryTypeBo> getAllEntryType() {

		List<EntryType> odTypes = entryTypeRepository.findAll();

		return odTypes.stream().map(this::entity2EntryTypeBo).collect(Collectors.toList());
	}
	
	public EntryType getEntryTypeById(Integer id) {
		
		return entryTypeRepository.findById(id).orElseThrow();
	}
	
	public Mode getModeById(Integer id) {
		return modeRepository.findById(id).orElseThrow();
	}
	
	public ShiftType getShiftTypeById(Integer id) {
		
		return shiftTypeRepository.findById(id).orElseThrow();
	}
	
	public List<TimeSheetEntry> getAllEntriesByEmployeeId(MasterEmployeeDetails employee){
		
		return  timeSheetEntryRepository.findByEmployeeId(employee);
	}
	

	
	public void saveOD(List<TimeSheetEntryRequest> req, Principal principal) {
	    MasterEmployeeDetails employee = findEmployeeByLancesoftId(principal.getName());
	    Integer id = employee.getEmpId();

	    List<TimeSheetEntry> timeSheetEntries = getAllEntriesByEmployeeId(employee);

	    List<TimeSheetEntry> filteredTimeSheetEntries = filterRejectedEntries(timeSheetEntries);

	    List<LocalDate> existingDates = getExistingDates(filteredTimeSheetEntries);

	    List<TimeSheetEntryRequest> entries = req.stream().filter(reqLine -> {
	        LocalDate odDate = reqLine.getApplyDate();
	        validateFutureDate(odDate);
	        validateDuplicateDate(odDate, existingDates, filteredTimeSheetEntries);
	        boolean isWeekend = isWeekend(odDate);
	        if (isWeekend) {
	            // not saving week off
	            // reqLine.setEntryTypeId(Integer.valueOf(EntryTypeConstant.WO));
	            return false;
	        } else {
	            setEntryTypeId(reqLine, EntryTypeConstant.OD);
	            existingDates.add(odDate);
	            return true;
	        }
	    }).collect(Collectors.toList());

	    saveEntries(entries, employee, filteredTimeSheetEntries);
	}
	
	public void saveLeave(List<TimeSheetEntryRequest> req, Principal principal) {
		 MasterEmployeeDetails employee = findEmployeeByLancesoftId(principal.getName());
		    Integer id = employee.getEmpId();

		    List<TimeSheetEntry> timeSheetEntries = getAllEntriesByEmployeeId(employee);

		    List<TimeSheetEntry> filteredTimeSheetEntries = filterRejectedEntries(timeSheetEntries);

		    List<LocalDate> existingDates = getExistingDates(filteredTimeSheetEntries);
		    List<TimeSheetEntryRequest> entries = req.stream().filter(reqLine -> {
		        LocalDate odDate = reqLine.getApplyDate();
		        validateFutureDate(odDate);
		        validateDuplicateDate(odDate, existingDates, filteredTimeSheetEntries);
		        boolean isWeekend = isWeekend(odDate);
		        if (isWeekend) {
		            // not saving week off
		            // reqLine.setEntryTypeId(Integer.valueOf(EntryTypeConstant.WO));
		            return false;
		        } else {
		            setEntryTypeId(reqLine, EntryTypeConstant.LEAVE);
		            existingDates.add(odDate);
		            return true;
		        }
		    }).collect(Collectors.toList());

		    saveEntries(entries, employee, filteredTimeSheetEntries);

	}

	public void saveExtraWork(List<TimeSheetEntryRequest> req, Principal principal) {
		 MasterEmployeeDetails employee = findEmployeeByLancesoftId(principal.getName());
		    Integer id = employee.getEmpId();

		    List<TimeSheetEntry> timeSheetEntries = getAllEntriesByEmployeeId(employee);

		    List<TimeSheetEntry> filteredTimeSheetEntries = filterRejectedEntries(timeSheetEntries);

		    List<LocalDate> existingDates = getExistingDates(filteredTimeSheetEntries);
		    List<TimeSheetEntryRequest> entries = req.stream().filter(reqLine -> {
		        LocalDate odDate = reqLine.getApplyDate();
		        validateFutureDate(odDate);
		        validateDuplicateDate(odDate, existingDates, filteredTimeSheetEntries);
		        boolean isWeekend = isWeekend(odDate);
		        if (isWeekend) {
		        	
		        	setEntryTypeId(reqLine, EntryTypeConstant.OT);
		            existingDates.add(odDate);
		            
		            return true;
		        } else {
		            
		            return false;
		        }
		    }).collect(Collectors.toList());

		    saveEntries(entries, employee, filteredTimeSheetEntries);

	}
	private List<TimeSheetEntry> filterRejectedEntries(List<TimeSheetEntry> entries) {
	    return entries.stream()
	            .filter(entry -> !entry.getApprovalId().getApprovalStatus().equals(Status.REJECT))
	            .collect(Collectors.toList());
	}

	private List<LocalDate> getExistingDates(List<TimeSheetEntry> entries) {
	    return entries.stream()
	            .map(TimeSheetEntry::getOdDate)
	            .collect(Collectors.toList());
	}

	private void validateFutureDate(LocalDate date) {
	    if (date.isAfter(LocalDate.now())) {
	        throw new SupervisorAlreadyExistException("Invalid: Future dates not allowed");
	    }
	}

	private void validateDuplicateDate(LocalDate date, List<LocalDate> existingDates, List<TimeSheetEntry> filteredEntries) {
	    if (existingDates.contains(date)) {
	        List<TimeSheetEntry> appliedDateRecords = filteredEntries.stream()
	                .filter(entry -> entry.getOdDate().equals(date)).collect(Collectors.toList());

	        if (appliedDateRecords.size() == 2) {
	            TimeSheetEntry firstEntry = appliedDateRecords.get(0);
	            TimeSheetEntry secondEntry = appliedDateRecords.get(1);

	            throw new SupervisorAlreadyExistException("You have already applied " +
	                    firstEntry.getEntryTypeId().getEntryType() + " on " + firstEntry.getOdDate() +
	                    " and " + secondEntry.getEntryTypeId().getEntryType() + " on " + secondEntry.getOdDate());
	        } else if (!appliedDateRecords.isEmpty()) {
	            TimeSheetEntry firstEntry = appliedDateRecords.get(0);
	            throw new SupervisorAlreadyExistException("You have already applied " +
	                    firstEntry.getEntryTypeId().getEntryType() + " on " + firstEntry.getOdDate());
	        }
	    }
	}

	private boolean isWeekend(LocalDate date) {
	    return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
	}

	private void setEntryTypeId(TimeSheetEntryRequest reqLine, Integer entryTypeId) {
	    reqLine.setEntryTypeId(entryTypeId);
	}

	private void saveEntries(List<TimeSheetEntryRequest> entries, MasterEmployeeDetails employee, List<TimeSheetEntry> filteredEntries) {
	    entries.stream()
	            .map(reqLine -> mapToTimeSheetEntry(reqLine, employee))
	            .forEach(entry -> {
	                timeSheetEntryRepository.save(entry);
	                approvalRepository.save(mapToApproval(employee, entry));
	           
	              
	            });
	            }



	private TimeSheetEntry mapToTimeSheetEntry(TimeSheetEntryRequest reqLine,
	        MasterEmployeeDetails employee) {
	    TimeSheetEntry entry = new TimeSheetEntry();

	    entry.setCreatedAt(LocalDateTime.now());
	    entry.setCreatedLoginId(employee.getEmpId());
	    entry.setEmployeeId(employee);
	    entry.setEntryTypeId(getEntryTypeById(reqLine.getEntryTypeId()));
	    if(reqLine.getEmployeesAtClientsId()!=null)
	    entry.setEmployeesAtClientsId(findClientDetailsById(reqLine.getEmployeesAtClientsId()));
	    entry.setInternal(reqLine.isInternal());
	    entry.setModeId(getModeById(reqLine.getModeId()));
	    entry.setOdDate(reqLine.getApplyDate());
	    if(reqLine.getProjectId()!=null)
	    entry.setProjectId(getProjectById(reqLine.getProjectId()));
	    entry.setReason(reqLine.getReason());
	    entry.setShiftTypeId(getShiftTypeById(reqLine.getShiftTypeId()));
	    
	    return entry;
	}
	
	private Approval mapToApproval(MasterEmployeeDetails employee,TimeSheetEntry entry) {
		Approval approval=new Approval();
		approval.setApprovalStatus(Status.PENDING);
		approval.setCreatedAt(LocalDateTime.now());
		approval.setCreatedLoginId(employee.getEmpId());
		approval.setManagerId(employee.getSupervisor());
		approval.setTimeSheetEntry(entry);
		return approval;
		
	}
	
	
	public List<TimeSheetEntryBo> getTimeSheetHistory(Principal principal){
		MasterEmployeeDetails employee= findEmployeeByLancesoftId(principal.getName());
		List<TimeSheetEntry> entries= timeSheetEntryRepository.findByEmployeeId(employee);
		
		return entries.stream()
				.map(this::entity2BoTimeSheetEntry).collect(Collectors.toList());
	}
	
	
	
	
	

}
