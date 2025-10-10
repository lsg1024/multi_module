package com.msa.order.global.qz;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Service
public class QzSigningService {

//    @Value("${qz.private-key}")
    private String QZ_KEY = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQC6t48jr/RUTOlqfvPCAlMqSS94VZYkp6R0VwzoeVvCu2fzBN7mjlHjYGhtflnvLx82kyb/u4msqVu0ZpYpCu+Y/e7OdH74z3T3S/FCt+gkAKE6TqLpUQO3TW1HCdH4SYlHsUFUErKhWWOkIM6BEtPUqR/Behu/GWv9Z2v40KcvH/6H9Q+yL3TLVv00lFR8apXajtTOGhHcsxgNiPQYoKnOljwNcAASFaLEj4rPsrPXnXBIy7PHxg3O6m9oppxyqof+/WQUJaPb5giy3BPgECvHrGSIAlym0vyOA90Vl7SoXeTyvUqu4yRwknnwNd22s7SMaO+Xe4MTFUzvXGHe75+BAgMBAAECggEAFi68mLbMxj8k8/wFJaV1D+8F7rvsTIqGJTQ6WlwgpK/zF5IakTWTUPohLfD26k2k3az50yQKpTKxrGsLp/CeqoRwnKKGezTaZo7lKNIEiFJy1SSGgkWXtW1DcE8gXOkPoC7U3hIimryt88/Z+Met1vQIy8mgBkIU7FQfJb5UPmrQni1ygDx+hO8iMdakW1n5f6MrZ7+K5KQvSq9XfpDd+1fuqWRYEpUh93P3QgirispnQjlU0PSUTp2BUjRepmQekAvt7Mk2dlU6meoBQ/FRQOSkmp0iXwSt8qsEnSEZDGu7qTZORxef8APbVyhDCm5f6hrcd4U1rNIPQsJys3F6yQKBgQDn/puEWortWFPjswX0kJ7NWtFvrj8IMBIrm87KOOA/xW0Mprfg5WLAHv8/zPqD5bNoFEWPW0/W6KGj+dowdteAn6hQLQmF0hQWUc+WfCqGIULLfcZPdMUVFqE02TxGqo3iLgkwl3o+kMAecmYu1to/W7nZp+302N5H64Lo1F7J6wKBgQDOCZPqgZByn1haHYJVmNzrGdtpobLWgfs2hq76h9HjWrR2cXh8D8p5i2TNitfU0hauSrss9oKJH/YSZXiNCRUV1Xw0j76RIbNnZIObPx5bmTLFc6eqrvZD0EKGTJvu9fYl+/E1d41968vvJydmMBg/DgDfA6+UV41bykEPohGVQwKBgGGJ2JTjBHoZotufDyfuDfZE8r1Dw1iL7XfMQDshgpcNSHYDOlgh2UzxO1v1sHX4A2AE9eH6AC1ZDWzFxYiOzrhTaA6dgN10n+FqcB7TAYX3QgAQV2pcq+fb5dc0ZnVHi/PGazK03T6k2UFz06ZpysTMqezq/87rxzjxuc5uS5QVAoGAfdT5olnYtzg8BGVLSS2flnHnP78CcuFVZDqjONykQd8OoduxAsu+E7cfLCzkndRlB7MaV16B4G9FoHyaQEBQwVHtlkhH0WksoTOQ8Mp+puCMUmzM9IAAZPAXAOBex3UuDIqvCMFoB4RvuzJFbLJozHGn9IvSup0x9uIyE/MeG/MCgYAap0Y/C3HfXgZkbt15AOkRAfEK7l8gnGm9b1MzhHwAobw1BCsU/5y0T61cP/A1L7YvCBqFSWY4A4CtMnSetgpa9axfKTq6eVUuuSV9HYGJc8L/8zbCrQW0tZ8GnrHCTXX45YdVaxaqgaJ8PSG8cGhzVTpQlOSvUyMNM/zHFY14WA==";

    public String sign(String dataToSign) throws Exception {
        log.info("Qz Sign Key = {}", QZ_KEY);

        String privateKeyPEM = QZ_KEY
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyPEM);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        Signature signature = Signature.getInstance("SHA1withRSA");
        signature.initSign(privateKey);
        signature.update(dataToSign.getBytes(StandardCharsets.UTF_8));

        byte[] signedBytes = signature.sign();

        String s = Base64.getEncoder().encodeToString(signedBytes);
        log.info("sign log = {}", s);
        return s;
    }
}
