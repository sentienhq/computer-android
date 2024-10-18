package fr.neamar.kiss.searcher;

import java.util.List;

import fr.neamar.kiss.pojo.Pojo;

public interface ResultsHandler {
    boolean addResult(Pojo pojo);

    boolean addResults(List<? extends Pojo> pojos);
}
