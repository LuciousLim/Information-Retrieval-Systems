/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.*;
import java.nio.charset.*;
import java.io.*;


/**
 *  A class for representing a query as a list of words, each of which has
 *  an associated weight.
 */
public class Query {

    /**
     *  Help class to represent one query term, with its associated weight. 
     */
    class QueryTerm {
        String term;
        double weight;
        QueryTerm( String t, double w ) {
            term = t;
            weight = w;
        }
    }

    /** 
     *  Representation of the query as a list of terms with associated weights.
     *  In assignments 1 and 2, the weight of each term will always be 1.
     */
    public ArrayList<QueryTerm> queryterm = new ArrayList<QueryTerm>();

    /**  
     *  Relevance feedback constant alpha (= weight of original query terms). 
     *  Should be between 0 and 1.
     *  (only used in assignment 3).
     */
    double alpha = 0.2;

    /**  
     *  Relevance feedback constant beta (= weight of query terms obtained by
     *  feedback from the user). 
     *  (only used in assignment 3).
     */
    double beta = 1 - alpha;
    
    
    /**
     *  Creates a new empty Query 
     */
    public Query() {
    }
    
    
    /**
     *  Creates a new Query from a string of words
     */
    public Query( String queryString  ) {
        StringTokenizer tok = new StringTokenizer( queryString );
        while ( tok.hasMoreTokens() ) {
            queryterm.add( new QueryTerm(tok.nextToken(), 1.0) );
        }    
    }
    
    
    /**
     *  Returns the number of terms
     */
    public int size() {
        return queryterm.size();
    }
    
    
    /**
     *  Returns the Manhattan query length
     */
    public double length() {
        double len = 0;
        for ( QueryTerm t : queryterm ) {
            len += t.weight; 
        }
        return len;
    }
    
    
    /**
     *  Returns a copy of the Query
     */
    public Query copy() {
        Query queryCopy = new Query();
        for ( QueryTerm t : queryterm ) {
            queryCopy.queryterm.add( new QueryTerm(t.term, t.weight) );
        }
        return queryCopy;
    }
    
    
    /**
     *  Expands the Query using Relevance Feedback
     *
     *  @param results The results of the previous query.
     *  @param docIsRelevant A boolean array representing which query results the user deemed relevant.
     *  @param engine The search engine object
     */
    public void relevanceFeedback( PostingsList results, boolean[] docIsRelevant, Engine engine ) {
        int relevantDocCount = 0;
        HashMap<String, Integer> Dr_centroid = new HashMap<>();

        for(int i = 0; i < docIsRelevant.length; i++){
            if (docIsRelevant[i]){
                HashMap<String, Integer> tf = getTfVector(engine.index.docNames.get(results.get(i).docID));
                for (Map.Entry<String, Integer> entry : tf.entrySet()) {
                    Dr_centroid.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
                relevantDocCount++;
            }
        }

        if (relevantDocCount == 0)return;

        // Adjust weights for existing query terms
        for (QueryTerm queryTerm : queryterm) {
            queryTerm.weight *= alpha;
        }

        for (QueryTerm queryTerm : queryterm) {
            if (Dr_centroid.containsKey(queryTerm.term)) {
                queryTerm.weight += beta * Dr_centroid.get(queryTerm.term) / relevantDocCount;
                Dr_centroid.remove(queryTerm.term);
            }
        }

        // Add new terms to query
        for (String term : Dr_centroid.keySet()) {
            queryterm.add(new QueryTerm(term, beta * Dr_centroid.get(term) / relevantDocCount));
        }
    }

    /**
     *  Returns a vector given a document name
     */
    public HashMap<String, Integer> getTfVector(String docName){
        HashMap<String, Integer> tf = new HashMap<>();
        File f = new File(docName);
        String patterns_file = "patterns.txt";
        try {
            Reader reader = new InputStreamReader( new FileInputStream(f), StandardCharsets.UTF_8 );
            Tokenizer tok = new Tokenizer( reader, true, false, true, patterns_file );
            while ( tok.hasMoreTokens() ) {
                String token = tok.nextToken();
                if (tf.get(token) == null) tf.put(token, 0);
                tf.merge(token, 1, Integer::sum);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tf;
    }

    public String toString(){
        String s = "";
        for(int i = 0; i < queryterm.size(); i++){
            s += queryterm.get(i).term + "(" + queryterm.get(i).weight + ") ";
        }
        return s;
    }
}


