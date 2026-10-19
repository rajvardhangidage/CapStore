package com.capstore.controller;

import com.capstore.dto.ReturnDtos.CreateReturnRequest;
import com.capstore.dto.ReturnDtos.ReturnResponse;
import com.capstore.service.ReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/returns", "/api/v1/returns"})
@RequiredArgsConstructor
public class ReturnController {
    private final ReturnService returns;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReturnResponse create(@Valid @RequestBody CreateReturnRequest request) {
        return returns.create(request);
    }

    @GetMapping("/{id}")
    public ReturnResponse get(@PathVariable String id) { return returns.getMine(id); }

    @GetMapping("/orders/{orderId}")
    public List<ReturnResponse> forOrder(@PathVariable String orderId) { return returns.forOrderMine(orderId); }
}
