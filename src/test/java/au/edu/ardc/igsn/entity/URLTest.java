package au.edu.ardc.igsn.entity;

import au.edu.ardc.igsn.TestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
public class URLTest {

    @Autowired
    TestEntityManager entityManager;

    @Test
    public void a_url_should_have_auto_generated_uuid() {
        URL url = TestHelper.mockUrl();

        // uuid is generated and is the correct format
        assertThat(url.getId()).isNotNull();
        assertThat(url.getId()).isInstanceOf(UUID.class);
        assertThat(url.getId().toString()).matches("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})");
    }

    @Test
    public void a_url_must_have_a_record() {
        URL url = TestHelper.mockUrl();
        assertThat(url.getRecord()).isInstanceOf(Record.class);
    }

    @Test
    public void a_url_must_have_a_date() {
        URL url = TestHelper.mockUrl();
        assertThat(url.getCreatedAt()).isInstanceOf(Date.class);
    }

    @Test
    public void a_url_must_have_a_url() {
        String expected = "http://aurl.com";
        URL  url = TestHelper.mockUrl();
        url.setUrl(expected);
        String actual = url.getUrl();
        assertThat(expected).isEqualTo(actual);
    }

    @Test
    public void a_url_must_have_a_status() {
        URL.Status expected = URL.Status.RESOLVABLE;
        URL  url = TestHelper.mockUrl();
        url.setStatus(expected);
        URL.Status actual= url.getStatus();
        assertThat(expected).isEqualTo(actual);
    }

    @Test
    public void an_identifier_must_set_dates() {
        Date expected = new Date();
        URL url = TestHelper.mockUrl();
        url.setUpdatedAt(expected);
        Date actual_update = url.getUpdatedAt();
        url.setCheckedAt(expected);
        Date actual_checked = url.getCheckedAt();
        assertThat(expected).isEqualTo(actual_update);
        assertThat(url.getUpdatedAt()).isInstanceOf(Date.class);
        assertThat(expected).isEqualTo(actual_checked);
        assertThat(url.getCheckedAt()).isInstanceOf(Date.class);
    }

}
