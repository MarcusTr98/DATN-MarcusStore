package com.fpoly.marcusstore.service.ai;

import com.fpoly.marcusstore.dto.ai.AiAdvisorRequest;
import com.fpoly.marcusstore.dto.ai.AiAdvisorResponse;
import com.fpoly.marcusstore.repository.core.HomeProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AiAdvisorServiceTest {

    private HomeProductRepository repository;
    private AiAdvisorService service;

    @BeforeEach
    void setUp() {
        repository = mock(HomeProductRepository.class);
        service = new AiAdvisorService(repository);
        ReflectionTestUtils.setField(service, "apiKey", "");
        ReflectionTestUtils.setField(service, "model", "gpt-5-mini");
        ReflectionTestUtils.setField(service, "baseUrl", "https://api.openai.com/v1");
    }

    @Test
    void answersStoreAddressWithoutCallingDatabaseOrProvider() {
        AiAdvisorRequest request = new AiAdvisorRequest();
        request.setMessage("Địa chỉ Marcus Store ở đâu?");

        AiAdvisorResponse response = service.advise(request);

        assertEquals("Dạ, Marcus Store ở địa chỉ 118 Cát Bi, Hải An, Hải Phòng ạ.",
                response.getAnswer());
        verifyNoInteractions(repository);
    }

    @Test
    void reportsMissingApiKeyForQuestionsThatRequireAi() {
        AiAdvisorRequest request = new AiAdvisorRequest();
        request.setMessage("Tư vấn điện thoại phù hợp để chơi game");

        assertThrows(IllegalStateException.class, () -> service.advise(request));
        verifyNoInteractions(repository);
    }
}
