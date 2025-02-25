/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import javax.management.relation.RelationNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        // task 1.2, single word search
        if(query.queryterm.size() == 1 && !queryType.equals(QueryType.RANKED_QUERY)){
            return this.index.getPostings(query.queryterm.get(0).term);
        }

        // task 1.3, intersect search
        else if (query.queryterm.size() > 1 && queryType.equals(QueryType.INTERSECTION_QUERY)){
//            return intersection(extractPostingLists(query));
            PostingsList result = null;

            // traverse the queryterms
            for (int i = 0; i < query.queryterm.size(); i++){
                PostingsList postingsList = index.getPostings(query.queryterm.get(i).term);

                // return an empty list if the posting list is empty,
                if (postingsList == null){
                    return new PostingsList();
                }

                // when the first term come, the result is empty,
                // give the first term's posting list to it
                if (result == null){
                    result = postingsList;
                } else {
                    result = intersect(result, postingsList);
                }

            }

            return result;

        }

        // task 1.4, phrase search
        else if (query.queryterm.size() > 1 && queryType.equals(QueryType.PHRASE_QUERY)){
            PostingsList result = null;

            // traverse the queryterms
            for (int i = 0; i < query.queryterm.size(); i++){
                PostingsList postingsList = index.getPostings(query.queryterm.get(i).term);

                // return an empty list if the posting list is empty,
                if (postingsList == null){
                    return new PostingsList();
                }

                // when the first term come, the result is empty,
                // give the first term's posting list to it
                if (result == null){
                    result = postingsList;
                } else {
                    result = phrase(result, postingsList);
                }

            }

            return result;
        }

        else if (queryType.equals(QueryType.RANKED_QUERY)){
            PostingsList result = rankSearch(query, index);
            return rank(query, result, index, "n", "t", rankingType, normType);
        }

        return null;
    }


    public PostingsList intersect(PostingsList pl1, PostingsList pl2){
        PostingsList result = new PostingsList();

        int i = 0, j = 0;
        while (i < pl1.size() && j < pl2.size()){
            int doc_i = pl1.get(i).docID, doc_j = pl2.get(j).docID;
            if (doc_i == doc_j){
                result.add(new PostingsEntry(doc_i));
                i++;
                j++;
            }
            else if (doc_i < doc_j){
                // the docIDs are in ascending order,
                i++;
            }
            else {
                j++;
            }
        }

        return result;
    }

    public PostingsList phrase(PostingsList pl1, PostingsList pl2){
        PostingsList result = new PostingsList();

        int i = 0, j =0;
        while (i < pl1.size() && j < pl2.size()){

            if (pl1.get(i).docID == pl2.get(j).docID){
                for (int m = 0; m < pl1.get(i).offsets.size(); m++){
                    for (int n = 0; n < pl2.get(j).offsets.size(); n++){
                        if (pl1.get(i).offsets.get(m) + 1 == pl2.get(j).offsets.get(n)){
                            result.add(pl2.get(j).docID, pl2.get(j).offsets.get(n));
                            break;
                        }
                    }
                }
                i++;
                j++;
            }
            else  if (pl1.get(i).docID < pl2.get(j).docID){
                i++;
            }
            else {
                j++;
            }
        }

        return result;
    }

    public PostingsList rankSearch(Query query, Index index){
        ArrayList<PostingsEntry> result = null;

        for (Query.QueryTerm t : query.queryterm){
            if (result == null){
                result = index.getPostings(t.term).getList();
            }
            else if (index.getPostings(t.term) != null){
                    result = (ArrayList<PostingsEntry>) Stream.concat(result.stream(), index.getPostings(t.term).getList().stream())
                            .distinct()
                            .collect(Collectors.toList());
            }
        }

        PostingsList postingsList = new PostingsList();
        if (result != null){
            for (PostingsEntry e : result){
                postingsList.add(e);
            }
        }

        return postingsList;
    }
    public PostingsList rank(Query query, PostingsList postingsList, Index index, String tf_scheme, String df_scheme,
                             RankingType type, NormalizationType normType ){
        return switch (type){
            case TF_IDF -> Ranking.tf_idf(query, postingsList, index, tf_scheme, df_scheme, normType);
            case PAGERANK -> Ranking.pageRank(postingsList, index);
            case COMBINATION -> Ranking.combination(query, postingsList, index, tf_scheme, df_scheme, normType);
        };
    }

}