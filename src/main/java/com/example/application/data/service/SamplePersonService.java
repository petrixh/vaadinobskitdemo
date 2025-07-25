package com.example.application.data.service;

import com.example.application.data.entity.SamplePerson;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class SamplePersonService {

    private final SamplePersonRepository repository;

    @Autowired
    public SamplePersonService(SamplePersonRepository repository) {
        this.repository = repository;
    }

    public Optional<SamplePerson> get(UUID id) {
        return repository.findById(id);
    }

    public SamplePerson update(SamplePerson entity) {
        return repository.save(entity);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }

    public Page<SamplePerson> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public List<SamplePerson> findAll(){
        return repository.findAll();
    }

    public void addSamplePersons(List<SamplePerson> persons){
        repository.saveAllAndFlush(persons);
    }

    public int count() {
        return (int) repository.count();
    }

    public void longServiceCall(int nrCallsToCount) {
        while(nrCallsToCount > 0){
            nrCallsToCount--;
            count();
        }
    }
}
