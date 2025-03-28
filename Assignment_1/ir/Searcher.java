/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import javax.management.relation.RelationNotFoundException;
import java.util.*;
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
        PostingsList[] pls = prepareList(query);

        // task 1.2, single word search
        if(query.queryterm.size() == 1 && !queryType.equals(QueryType.RANKED_QUERY)){
//            return this.index.getPostings(query.queryterm.get(0).term);
            return pls[0];
        }

        // task 1.3, intersect search
        else if (query.queryterm.size() > 1 && queryType.equals(QueryType.INTERSECTION_QUERY)){
            PostingsList result = null;

            // traverse the queryterms
            for (int i = 0; i < query.queryterm.size(); i++){
                PostingsList postingsList = pls[i];

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
                PostingsList postingsList = pls[i];

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
//            PostingsList result = rankSearch(query, index);
            PostingsList result = new PostingsList();
            return rank(query, result, pls, index, "n", "t", rankingType, normType);
        }

        return null;
    }

    public PostingsList[] prepareList(Query query){
        PostingsList[] pls = new PostingsList[query.size()];

        for (int i = 0; i < query.size(); i++){
            if (kgIndex == null || !query.queryterm.get(i).term.contains("*")){
                pls[i] = index.getPostings(query.queryterm.get(i).term);
            } else {
                PostingsList pl = new PostingsList();
                ArrayList<String> words = kgIndex.getWildcardWords(query.queryterm.get(i).term);
                for (String word : words) {
                    PostingsList p = index.getPostings(word);
                    for (int j = 0; j < p.size(); ++j) {
                        pl.insert(p.get(j));
                    }
                }

                pl.sortByDocID();
                PostingsList result = new PostingsList();
                int lastDoc = -1;
                for (int j = 0; j < pl.size(); j++) {
                    PostingsEntry pe = pl.get(j);
                    if (pe.docID != lastDoc) {
                        result.add(pe);
                    } else {
                        int offsetsLen = pe.offsets.size();
                        for (int k = 0; k < offsetsLen; k++){
                            result.get(result.size() - 1).addOffset(pe.offsets.get(k));
                        }
                        Collections.sort(result.get(result.size() - 1).offsets);
                    }
                    lastDoc = pe.docID;
                }
                pls[i] = result;
            }
        }
        return pls;
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

    public PostingsList rankSearch(Query query, Index index) {
        Set<PostingsEntry> resultSet = new HashSet<>();

        for (Query.QueryTerm t : query.queryterm) {
            PostingsList postings = index.getPostings(t.term);
            if (postings != null) {
                resultSet.addAll(postings.getList());
            }
        }

        PostingsList postingsList = new PostingsList();
        for (PostingsEntry entry : resultSet) {
            postingsList.add(entry);
        }
        return postingsList;
    }

    public PostingsList rank(Query query, PostingsList postingsList, PostingsList[] pls, Index index, String tf_scheme, String df_scheme,
                             RankingType type, NormalizationType normType ){
        return switch (type){
            case TF_IDF -> Ranking.tf_idf(query, pls, index, tf_scheme, df_scheme, normType, kgIndex);
            case PAGERANK -> Ranking.pageRank(postingsList, index);
            case COMBINATION -> Ranking.combination(query, postingsList, index, tf_scheme, df_scheme, normType);
        };
    }

}