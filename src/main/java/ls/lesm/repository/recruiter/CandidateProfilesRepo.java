package ls.lesm.repository.recruiter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ls.lesm.model.MasterEmployeeDetails;
import ls.lesm.model.recruiter.CandidateProfiles;
import ls.lesm.model.recruiter.JobString;

public interface CandidateProfilesRepo extends JpaRepository<CandidateProfiles, String> {

	List<CandidateProfiles> findByMasterEmployeeDetailsAndJobString(MasterEmployeeDetails employee,
			JobString jobString);

	List<CandidateProfiles> findByJobString(JobString jobString);
	
	@Query("from CandidateProfiles where masterEmployeeDetails=6 and jobString=1")
	List<CandidateProfiles> findByMasterEmployeeDetailsAndJobString2(Integer employee,
			JobString c);
	
}
