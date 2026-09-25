package com.kfokam48.epreuve209.controller;

import com.kfokam48.epreuve209.dto.DeposerExerciceRequest;
import com.kfokam48.epreuve209.dto.ExerciceResponse;
import com.kfokam48.epreuve209.service.ExerciceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exercices")
public class ExerciceController {

    private final ExerciceService service;

    public ExerciceController(ExerciceService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ExerciceResponse> deposer(@Valid @RequestBody DeposerExerciceRequest requete) {
        ExerciceResponse exercice = service.deposer(requete);
        return ResponseEntity.status(HttpStatus.CREATED).body(exercice);
    }
}
