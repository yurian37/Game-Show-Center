package com.gameshowcenter.backend.controllers;

import com.gameshowcenter.backend.dto.MasterMatchRequest;
import com.gameshowcenter.backend.services.MatchCompilerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/match")
@CrossOrigin(origins = "*") 
public class GameMatchController {

    // Inyección de dependencias: conectamos el servicio al controlador
    private final MatchCompilerService compilerService;

    public GameMatchController(MatchCompilerService compilerService) {
        this.compilerService = compilerService;
    }

    @PostMapping("/compile")
    public ResponseEntity<?> compileMatch(@RequestBody MasterMatchRequest request) {
        // La validación se hace en el DTO (si falla, el GlobalExceptionHandler lo atrapa automáticamente)
        request.validateAll();
        
        // Mandamos el trabajo pesado a la "cocina" (el Servicio)
        String resultMessage = compilerService.compileOfflineTemplate(request);
        
        return ResponseEntity.ok(resultMessage);
    }
}