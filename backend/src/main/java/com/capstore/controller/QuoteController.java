package com.capstore.controller;

import com.capstore.dto.ExtraDtos.*;
import com.capstore.service.QuoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quotes")
@RequiredArgsConstructor
public class QuoteController {
    private final QuoteService quotes;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuoteResponse create(@Valid @RequestBody QuoteCreateRequest r) { return quotes.create(r); }

    @GetMapping
    public List<QuoteResponse> mine() { return quotes.mine(); }

    @PutMapping("/{id}/decision")
    public QuoteResponse decide(@PathVariable Long id, @RequestBody QuoteDecisionRequest r) { return quotes.decide(id, r.accept()); }
}
