package com.github.burntmallow.bloggingplatformapi.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.context.WebApplicationContext;

import com.github.burntmallow.bloggingplatformapi.dto.PostRequest;
import com.github.burntmallow.bloggingplatformapi.dto.PostResponse;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class PostEndToEndTest {

    private RestTestClient restTestClient;

    @BeforeEach
    void setUp(WebApplicationContext context) {
        this.restTestClient = RestTestClient.bindToApplicationContext(context)
                .baseUrl("/api/posts")
                .build();
    }

    @Test
    void shouldProcessFullRequestCycle() {
        PostRequest request = new PostRequest("E2E Title", "Valid Content", "Tech", List.of("news"));

        restTestClient.post()
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PostResponse.class)
                .value(response -> {
                    assertThat(response.id()).isNotNull();
                    assertThat(response.title()).isEqualTo("E2E Title");
                });
    }
}
