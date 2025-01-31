/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;
import java.util.List;

/**
 *  Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;
    
    /** Constructor */
    public Searcher( Index index, KGramIndex kgIndex ) {
        this.index = index;
        this.kgIndex = kgIndex;
    }

    /**
     *  Searches the index for postings matching the query.
     *  @return A postings list representing the result of the query.
     */
    public PostingsList search( Query query, QueryType queryType, RankingType rankingType, NormalizationType normType ) {
        if(query.queryterm.size() == 1){
            return this.index.getPostings(query.queryterm.get(0).term);
        }
        else if (query.queryterm.size() > 1 && queryType.equals(QueryType.INTERSECTION_QUERY)){
            return intersection(extractPostingLists(query));
        }

        return null;
    }

    public List<PostingsList> extractPostingLists(Query query){
        List<PostingsList> postingsLists = new ArrayList<>();
        if (query.queryterm.size() > 1) {
            for (int i = 0; i < query.queryterm.size(); i++) {
                postingsLists.add(this.index.getPostings((query.queryterm.get(i).term)));
            }
        }
        return postingsLists;
    }

    public PostingsList intersection(List<PostingsList> postingsLists){
        PostingsList result = new PostingsList();
        PostingsList comparingList = new PostingsList();
        int left = 0, right = 1;

        while (left < postingsLists.size() && right < postingsLists.size()) {
            if (postingsLists.size() >= 2){
                if (left == 0){
                    comparingList.copy(postingsLists.get(left));
                }
                else if (result.size() != 0){
                    comparingList.clearList();
                    comparingList.copy(result);
                    result.clearList();
                }
                else {
                    return null;
                }

                int i = 0, j = 0;
                while (i < comparingList.size() && j < postingsLists.get(right).size()){
                    int doc_i = comparingList.get(i).docID, doc_j = postingsLists.get(right).get(j).docID;
                    if (doc_i == doc_j){
                        result.add(new PostingsEntry(doc_i));
                        i++; j++;
                    }
                    else if (doc_i > doc_j){
                        i++;
                    }
                    else {
                        j++;
                    }
                }

                left++;
                right++;
            }
        }

        return result;
    }


}