package com.msa.jewelry.product.internal.imagesearch.client;

import com.msa.jewelry.product.internal.imagesearch.dto.ImageSearchDtos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * image-search-service 호출 어댑터.
 *
 * - 모든 호출은 RestClient 빈을 사용 (ImageSearchAsyncConfig#imageSearchRestClient)
 * - 실패는 RuntimeException으로 던지고, 호출 측에서 fallback/재시도 결정
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageSearchClient {

    private final RestClient imageSearchRestClient;

    // ============================================================
    // 인덱싱
    // ============================================================
    public EmbedResponse embed(EmbedRequest request) {
        return imageSearchRestClient.post()
                .uri("/embed")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(EmbedResponse.class);
    }

    public EmbedDeleteResponse embedDelete(EmbedDeleteRequest request) {
        return imageSearchRestClient.post()
                .uri("/embed/delete")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(EmbedDeleteResponse.class);
    }

    // ============================================================
    // 검색
    // ============================================================
    public SearchInternalResponse searchByImage(
            byte[] imageBytes,
            String fileName,
            String tenantId,
            String classification,
            int topK
    ) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new NamedByteArrayResource(imageBytes, fileName));
        body.add("tenantId", tenantId);
        if (classification != null) {
            body.add("classification", classification);
        }
        body.add("topK", String.valueOf(topK));

        return imageSearchRestClient.post()
                .uri("/search/image")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(SearchInternalResponse.class);
    }

    public SearchInternalResponse searchByText(SearchTextRequest request) {
        return imageSearchRestClient.post()
                .uri("/search/text")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(SearchInternalResponse.class);
    }

    // ============================================================
    // multipart에 원본 파일명 보존하기 위한 ByteArrayResource 확장
    // ============================================================
    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
