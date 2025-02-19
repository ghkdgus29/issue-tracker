package com.example.be.issue;

import com.example.be.assignee.Assignee;
import com.example.be.comment.Comment;
import com.example.be.comment.CommentRepository;
import com.example.be.issue.dto.*;
import com.example.be.label.LabelRepository;
import com.example.be.milestone.MilestoneRepository;
import com.example.be.user.UserRepository;
import com.example.be.util.Paging;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class IssueService {

    private final IssueRepository issueRepository;
    private final LabelRepository labelRepository;
    private final UserRepository userRepository;
    private final MilestoneRepository milestoneRepository;
    private final CommentRepository commentRepository;

    public IssueService(IssueRepository issueRepository, LabelRepository labelRepository, UserRepository userRepository, MilestoneRepository milestoneRepository, CommentRepository commentRepository) {
        this.issueRepository = issueRepository;
        this.labelRepository = labelRepository;
        this.userRepository = userRepository;
        this.milestoneRepository = milestoneRepository;
        this.commentRepository = commentRepository;
    }

    public FeIssueResponseDTO makeFeIssueResponse(IssueSearchCondition issueSearchCondition,
                                                  Integer cntPage) {
        Paging paging = new Paging(cntPage, issueRepository.findIssueSize(issueSearchCondition));
        addPaginationCondition(issueSearchCondition, paging);

        List<Issue> issues = findIssues(issueSearchCondition);
        CountDTO countDTO = issueRepository.countEntities();
        AllLabelsAndMilestonesAndUsersDTO allLabelsAndMilestonesAndUsersDTO = gatherAllEntities();

        return new FeIssueResponseDTO(issues, countDTO, allLabelsAndMilestonesAndUsersDTO.getAllLabels(), allLabelsAndMilestonesAndUsersDTO.getAllMilestones(), allLabelsAndMilestonesAndUsersDTO.getAllUsers(), paging);
    }

    public IosIssueResponseDTO makeIosIssueResponse(IssueSearchCondition issueSearchCondition,
                                                    Integer cntPage) {
        Paging paging = new Paging(cntPage, issueRepository.findIssueSize(issueSearchCondition));
        addPaginationCondition(issueSearchCondition, paging);

        List<Issue> issues = findIssues(issueSearchCondition);
        return new IosIssueResponseDTO(issues, paging);
    }

    public AllLabelsAndMilestonesAndUsersDTO gatherAllEntities() {
        return new AllLabelsAndMilestonesAndUsersDTO(labelRepository.findAll(), milestoneRepository.findAll(), userRepository.findAll());
    }

    public int createIssue(IssueCreateFormDTO issueCreateFormDTO) {
        if (issueCreateFormDTO.getAssignees() != null && issueCreateFormDTO.getAssignees().size() != 0) {
            issueCreateFormDTO.setAssignees(userRepository.validateNames(issueCreateFormDTO.getAssignees()));
        }

        if (issueCreateFormDTO.getLabelNames() != null && issueCreateFormDTO.getLabelNames().size() != 0) {
            issueCreateFormDTO.setLabelNames(labelRepository.validateNames(issueCreateFormDTO.getLabelNames()));
        }

        if (issueCreateFormDTO.getMilestoneName() != null) {
            issueCreateFormDTO.setMilestoneName(milestoneRepository.validateName(issueCreateFormDTO.getMilestoneName()));
        }

        return issueRepository.save(issueCreateFormDTO);
    }

    public IssueDetailedDTO findIssueDetailed(Integer issueNumber) {
        Issue issue = issueRepository.findIssueByIssueNumber(issueNumber);
        List<IssueNumberWithLabelDTO> issueNumberWithLabelDTOs = issueRepository.findIssueLabelMapsBy(Set.of(issueNumber));
        List<Assignee> assignees = issueRepository.findAssigneesBy(Set.of(issueNumber));
        List<Comment> comments = commentRepository.findCommentByIssueNumber(issueNumber);

        issueNumberWithLabelDTOs.forEach(issueLabel -> issue.add(issueLabel));
        assignees.forEach(assignee -> issue.add(assignee));

        return new IssueDetailedDTO(issue, comments);
    }

    public boolean updateIssue(IssueUpdateFormDTO issueUpdateFormDTO) {
        if (issueUpdateFormDTO.isState() == null &&
                issueUpdateFormDTO.getTitle() == null &&
                issueUpdateFormDTO.getContents() == null &&
                issueUpdateFormDTO.getMilestoneName() == null) {
            return false;
        }
        return issueRepository.update(issueUpdateFormDTO);
    }

    public boolean updateIssueAssigns(IssueAssignsUpdateFormDTO issueAssignsUpdateFormDTO) {
        return issueRepository.updateAssigns(issueAssignsUpdateFormDTO);
    }

    public boolean updateIssueLabelRelation(IssueLabelRelationUpdateFormDTO issueLabelRelationUpdateFormDTO) {
        return issueRepository.updateIssueLabelRelation(issueLabelRelationUpdateFormDTO);
    }

    private List<Issue> findIssues(IssueSearchCondition issueSearchCondition) {
        List<Issue> issues = issueRepository.findIssuesWithoutLabelsBy(issueSearchCondition);

        if (issues.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, Issue> issueMap = issues.stream()
                .collect(Collectors.toMap(
                        Issue::getNumber,
                        Function.identity()
                ));

        List<IssueNumberWithLabelDTO> issueNumberWithLabelDTOs = issueRepository.findIssueLabelMapsBy(issueMap.keySet());
        issueNumberWithLabelDTOs.forEach(issueLabel -> issueMap.get(issueLabel.getIssueNumber()).add(issueLabel));

        List<Assignee> assignees = issueRepository.findAssigneesBy(issueMap.keySet());
        assignees.forEach(assignee -> issueMap.get(assignee.getIssueNumber()).add(assignee));

        return issueMap.values().stream()
                .sorted((o1, o2) -> o1.getNumber() - o2.getNumber())
                .collect(Collectors.toList());
    }

    private void addPaginationCondition(IssueSearchCondition issueSearchCondition, Paging paging) {
        issueSearchCondition.setStartIndex(paging.getStartIndex());
        issueSearchCondition.setCntPerPage(paging.getCntPerPage());
    }
}
