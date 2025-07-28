package com.msa.product.local.product.service;

import com.msa.product.local.product.repository.ProductRepository;
import com.msacommon.global.jwt.JwtUtil;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final JwtUtil jwtUtil;
    private final ProductRepository productRepository;

    public ProductService(JwtUtil jwtUtil, ProductRepository productRepository) {
        this.jwtUtil = jwtUtil;
        this.productRepository = productRepository;
    }

    //생성

    //조회

    //수정

    //삭제
}
