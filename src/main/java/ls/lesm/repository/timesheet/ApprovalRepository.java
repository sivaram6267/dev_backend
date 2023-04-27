package ls.lesm.repository.timesheet;

import org.springframework.data.jpa.repository.JpaRepository;

import ls.lesm.model.timesheet.Approval;

public interface ApprovalRepository extends JpaRepository<Approval, Integer> {

}
