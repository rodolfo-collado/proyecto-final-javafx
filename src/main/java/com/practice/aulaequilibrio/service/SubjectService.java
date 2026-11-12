package com.practice.aulaequilibrio.service;

import com.practice.aulaequilibrio.model.Subject;
import com.practice.aulaequilibrio.repository.SubjectRepository;
import com.practice.aulaequilibrio.util.ErrorMessages;
import com.practice.aulaequilibrio.util.ValidationException;
import com.practice.aulaequilibrio.util.Validations;

import java.util.List;
import java.util.Locale;

public class SubjectService {

    private static final int MAX_CODIGO = 20;
    private static final int MAX_NOMBRE = 100;

    private final SubjectRepository repository;

    public SubjectService() {
        this(new SubjectRepository());
    }

    public SubjectService(SubjectRepository repository) {
        this.repository = repository;
    }

    public List<Subject> listAll() {
        return repository.findAll();
    }

    /**
     * Crea una asignatura nueva ({@code id == null}) o actualiza una existente.
     */
    public Subject save(Integer id, String codigo, String nombre, String sesionesRequeridas) {
        String cleanCodigo = Validations.required(codigo, "código", MAX_CODIGO).toUpperCase(Locale.ROOT);
        String cleanNombre = Validations.required(nombre, "nombre de la asignatura", MAX_NOMBRE);
        int sesiones = Validations.positiveInteger(sesionesRequeridas, "número de sesiones requeridas");

        try {
            if (repository.existsByCodigo(cleanCodigo, id)) {
                throw new ValidationException("Ya existe una asignatura con el código \"" + cleanCodigo + "\".");
            }

            Subject subject = id == null
                    ? new Subject()
                    : repository.findById(id).orElseThrow(() ->
                    new ValidationException("La asignatura ya no existe. Actualice la lista."));
            subject.setCodigo(cleanCodigo);
            subject.setNombre(cleanNombre);
            subject.setSesionesRequeridas(sesiones);
            return repository.save(subject);
        } catch (ValidationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw ErrorMessages.onSave("la asignatura", exception);
        }
    }

    public void delete(Integer id) {
        if (id == null) {
            throw new ValidationException("Seleccione una asignatura para eliminar.");
        }
        try {
            if (!repository.deleteById(id)) {
                throw new ValidationException("La asignatura ya no existe. Actualice la lista.");
            }
        } catch (ValidationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw ErrorMessages.onDelete("la asignatura", exception);
        }
    }
}
