package cn.lunalhx.ai.kilnai.trigger.application;

import cn.lunalhx.ai.kilnai.domain.learning.service.LearningProgressResult;
import cn.lunalhx.ai.kilnai.domain.learning.service.LearningProgressService;
import cn.lunalhx.ai.kilnai.domain.learning.service.RecordLearningEvidenceCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transaction boundary for the HTTP learning command use case. */
@Service
public class LearningCommandService {

    private final LearningProgressService learningProgressService;

    public LearningCommandService(LearningProgressService learningProgressService) {
        this.learningProgressService = learningProgressService;
    }

    @Transactional
    public LearningProgressResult recordEvidence(RecordLearningEvidenceCommand command) {
        return learningProgressService.recordEvidence(command);
    }
}
