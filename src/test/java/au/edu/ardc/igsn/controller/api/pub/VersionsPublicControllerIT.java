package au.edu.ardc.igsn.controller.api.pub;

import au.edu.ardc.igsn.TestHelper;
import au.edu.ardc.igsn.WebIntegrationTest;
import au.edu.ardc.igsn.entity.Record;
import au.edu.ardc.igsn.entity.Version;
import au.edu.ardc.igsn.repository.RecordRepository;
import au.edu.ardc.igsn.repository.VersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class VersionsPublicControllerIT extends WebIntegrationTest {
    private final String baseUrl = "/api/public/versions/";

    @Autowired
    VersionRepository versionRepository;

    @Autowired
    RecordRepository recordRepository;

    @Test
    void show_mixedVersions_showOnlyPublicVersions() {
        // given a record
        Record record = TestHelper.mockRecord();
        record.setVisible(true);
        recordRepository.saveAndFlush(record);

        // with a current version
        Version version = TestHelper.mockVersion(record);
        version.setCurrent(true);
        versionRepository.saveAndFlush(version);

        // and a superseded version
        Version superseded = TestHelper.mockVersion(record);
        superseded.setCurrent(false);
        versionRepository.saveAndFlush(superseded);

        // given a record
        Record privateRecord = TestHelper.mockRecord();
        privateRecord.setVisible(false);
        recordRepository.saveAndFlush(privateRecord);

        // current version on private record
        Version privateVersion = TestHelper.mockVersion(privateRecord);
        privateVersion.setCurrent(true);
        versionRepository.saveAndFlush(privateVersion);

        // when show all, only have 1 current, public version
        this.webTestClient.get().uri(baseUrl)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.numberOfElements").isEqualTo(1);
    }

    @Test
    void show_filterBySchema_onlySchemaReturns() {
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

        // both returns with no schema query
        this.webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(baseUrl)
                        .queryParam("schema", "igsn-descriptive-v1")
                        .build())
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.numberOfElements").isEqualTo(1)
                .jsonPath("$.content[0].schema").isEqualTo("igsn-descriptive-v1");

        // when filter by ?schema=igsn-descriptive-v1, only 1 returns
        this.webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(baseUrl)
                        .queryParam("schema", "igsn-descriptive-v1")
                        .build())
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.numberOfElements").isEqualTo(1)
                .jsonPath("$.content[0].schema").isEqualTo("igsn-descriptive-v1");

        // when filter by ?schema=igsn-csiro-v3, only 1 returns
        this.webTestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(baseUrl)
                        .queryParam("schema", "igsn-csiro-v3")
                        .build())
                .exchange().expectStatus().isOk()
                .expectBody()
                .jsonPath("$.numberOfElements").isEqualTo(1)
                .jsonPath("$.content[0].schema").isEqualTo("igsn-csiro-v3");
    }

    @BeforeEach
    void setUp() {
        versionRepository.flush();
        versionRepository.deleteAll();
        versionRepository.flush();
    }
}