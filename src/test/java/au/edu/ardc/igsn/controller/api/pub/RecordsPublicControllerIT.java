package au.edu.ardc.igsn.controller.api.pub;

import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.WebIntegrationTest;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.entity.Version;
import au.edu.ardc.igsn.repository.RecordRepository;
import au.edu.ardc.igsn.repository.VersionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

class RecordsPublicControllerIT extends WebIntegrationTest {

    private final String baseUrl = "/api/public/records/";

    @Autowired
    RecordRepository recordRepository;

    @Autowired
    VersionRepository versionRepository;

    @Test
    void index_show_shouldReturnAllRecords() {
        // 5 public records
        for (int i = 0; i < 5; i++) {
            Record record = TestHelper.mockRecord();
            record.setVisible(true);
            recordRepository.save(record);
        }
        recordRepository.flush();

        this.webTestClient.get().uri(baseUrl)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").exists()
                .jsonPath("$.content[*].id").isArray();
    }

    @Test
    void index_page0size5_returnsTheFirst5() {
        // 5 public records
        for (int i = 0; i < 5; i++) {
            Record record = TestHelper.mockRecord();
            record.setVisible(true);
            recordRepository.save(record);
        }
        recordRepository.flush();

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

    @Test
    void show_notFoundOrPrivate_404() {
        // random record returns 404
        this.webTestClient
                .get()
                .uri(baseUrl + UUID.randomUUID().toString())
                .exchange().expectStatus().isNotFound();

        // given a private record
        Record record = TestHelper.mockRecord();
        record.setVisible(false);
        recordRepository.saveAndFlush(record);

        // private record returns 404
        this.webTestClient
                .get()
                .uri(baseUrl + record.getId().toString())
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void show_publicRecord_returnsDTO() {
        // given a public record
        Record record = TestHelper.mockRecord();
        record.setVisible(true);
        recordRepository.saveAndFlush(record);

        this.webTestClient
                .get()
                .uri(baseUrl + record.getId().toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(record.getId().toString());
    }

    @Test
    void showVersions_publicRecord_returnsListOfVersions() {
        // given a public record
        Record record = TestHelper.mockRecord();
        record.setVisible(true);
        recordRepository.saveAndFlush(record);

        // and 3 versions
        for (int i = 0; i < 3; i++) {
            Version version = TestHelper.mockVersion(record);
            versionRepository.saveAndFlush(version);
        }

        this.webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(baseUrl + record.getId().toString() + "/versions")
                        .queryParam("page", "0")
                        .queryParam("size", "5")
                        .build())
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.numberOfElements").isEqualTo(3)
                .jsonPath("$.content[*].id").isNotEmpty();
    }

    @Test
    void showVersions_filterBySchema_returnTheRightSet() {
        // given a record
        Record record = TestHelper.mockRecord();
        record.setVisible(true);
        recordRepository.saveAndFlush(record);

        // with a version of schema igsn-descriptive-v1
        Version version = TestHelper.mockVersion(record);
        version.setCurrent(true);
        version.setSchema("igsn-descriptive-v1");
        versionRepository.saveAndFlush(version);

        // and another version of schema igsn-csiro-v3
        Version version2 = TestHelper.mockVersion(record);
        version2.setCurrent(true);
        version2.setSchema("igsn-csiro-v3");
        versionRepository.saveAndFlush(version2);

        // when filter by ?schema=igsn-descriptive-v1, only 1 returns
        this.webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(baseUrl + record.getId().toString() + "/versions")
                        .queryParam("schema", "igsn-descriptive-v1")
                        .build())
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.numberOfElements").isEqualTo(1)
                .jsonPath("$.content[0].schema").isEqualTo("igsn-descriptive-v1");
    }
}