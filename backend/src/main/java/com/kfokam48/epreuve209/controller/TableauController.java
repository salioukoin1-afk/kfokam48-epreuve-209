package com.kfokam48.epreuve209.controller;

import com.kfokam48.epreuve209.dto.LigneTableau;
import com.kfokam48.epreuve209.service.TableauService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tableau")
public class TableauController {

    private final TableauService service;

    public TableauController(TableauService service) {
        this.service = service;
    }

    @GetMapping
    public List<LigneTableau> tableau(@RequestParam Long promotionId) {
        return service.tableau(promotionId);
    }
}
