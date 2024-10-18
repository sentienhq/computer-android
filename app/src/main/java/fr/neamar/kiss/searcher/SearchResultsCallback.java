package fr.neamar.kiss.searcher;

import java.util.ArrayList;
import java.util.List;

import fr.neamar.kiss.pojo.Pojo;

public interface SearchResultsCallback {
    void onSearchResults(List<Pojo> results);
}

