package au.edu.ardc.igsn.controller.api.pub;

import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.dto.RecordDTO;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.repository.RecordRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class RecordsPublicControllerIT {

    @Autowired
    public WebTestClient webTestClient;

    private final String baseUrl = "/api/public/records/";

    @Autowired
    RecordRepository repository;

    @Test
    void index_show_shouldReturnAllRecords() {
        this.webTestClient.get().uri(baseUrl)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").exists()
                .jsonPath("$.content[*].id").isArray()

        ;
    }

    @Test
    void index_page0size5_returnsTheFirst5() {
        this.webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(baseUrl)
                        .queryParam("page", "0")
                        .queryParam("size", "5")
                        .build())
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.numberOfElements").isEqualTo(5)
                .jsonPath("$.content[0].id").isNotEmpty();
    }

    @BeforeEach
    void setUp() {
        // 5 public records
        for (int i = 0; i < 5; i++) {
            Record record = TestHelper.mockRecord();
            record.setVisible(true);
            repository.save(record);
        }
        repository.flush();
    }
}