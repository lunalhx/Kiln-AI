package cn.lunalhx.ai.kilnai.domain.learning.graph;

import cn.lunalhx.ai.kilnai.domain.apply.model.ModelContractInvalidException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClarificationClassificationTest {

    @Test
    void parsesTheClosedClassificationContract() {
        assertEquals(ClarificationClassification.PROCEDURAL, ClarificationClassification.parse(
                "{\"schema\":\"clarification_classification/v1\",\"classification\":\"procedural\"}"));
        assertEquals(ClarificationClassification.SUBSTANTIVE, ClarificationClassification.parse(
                "{\"schema\":\"clarification_classification/v1\",\"classification\":\"substantive\"}"));
        assertEquals(ClarificationClassification.UNCERTAIN, ClarificationClassification.parse(
                "{\"schema\":\"clarification_classification/v1\",\"classification\":\"uncertain\"}"));
    }

    @Test
    void rejectsUnknownClassificationAsContractInvalid() {
        ModelContractInvalidException error = assertThrows(ModelContractInvalidException.class,
                () -> ClarificationClassification.parse(
                        "{\"schema\":\"clarification_classification/v1\",\"classification\":\"guess\"}"));
        assertEquals(List.of("invalid_enum"), error.violationCodes());
        assertFalse(error.getMessage().contains("guess"));
    }

    @Test
    void rejectsUnknownFields() {
        ModelContractInvalidException error = assertThrows(ModelContractInvalidException.class,
                () -> ClarificationClassification.parse(
                        "{\"schema\":\"clarification_classification/v1\",\"classification\":\"procedural\",\"answer\":\"x\"}"));
        assertEquals(List.of("unknown_field"), error.violationCodes());
        assertFalse(error.getMessage().contains("answer"));
    }
}
