package ir;

import java.util.ArrayList;
import java.util.Collections;

public class Ranking {
    public static PostingsList tf_idf(Query query, PostingsList postingsList, Index index, String tf_scheme, String df_scheme){
        return cosineScore(query, postingsList, index, tf_scheme, df_scheme);
    }

    public static PostingsList cosineScore(Query query, PostingsList postingsList, Index index, String tf_scheme, String df_scheme){
        ArrayList<Double> score = new ArrayList<>(Collections.nCopies(postingsList.size(), 0.0));
        ArrayList<Integer> tftds = new ArrayList<>();

        for (Query.QueryTerm t : query.queryterm){
            // Compute tftd
            tftds.clear();
            for (PostingsEntry doc : postingsList.getList()){
                tftds.add(doc.getTf());
            }
            // Compute tftq
            int tftq = (int)query.queryterm.stream().filter(element -> element.equals(t)).count();
            // Compute df
            int df = index.getPostings(t.term).size();

            for (int d = 0; d < postingsList.size(); d++){
                double Wftd = calWeight(tftds.get(d), df, postingsList, index, tf_scheme, df_scheme);
                double Wtq = calWeight(tftq, df, postingsList, index, tf_scheme, df_scheme);
                double weight = Wftd * Wtq;
                score.set(d, score.get(d) + weight);
            }
        }

        for (int d = 0; d < postingsList.size(); d++){
            postingsList.get(d).score = score.get(d) / index.docLengths.get(postingsList.get(d).docID);
        }

        Collections.sort(postingsList.getList());

        return postingsList;
    }

    public static Double calWeight(int tf, int df, PostingsList postingsList, Index index, String tf_scheme, String df_scheme){
        return tf_weightingScheme(tf, postingsList, tf_scheme) * df_weightingScheme(df, index, df_scheme);
    }

    public static double tf_weightingScheme(int tf, PostingsList postingsList, String scheme){
        return switch (scheme) {
            case "n" -> (double) tf;
            case "l" -> 1 + Math.log10(tf);
            default -> -1.0;
        };
    }

    public static double df_weightingScheme(int df, Index index, String scheme){
        return switch (scheme) {
            case "t" -> Math.log10((double) index.docLengths.size() / df);
            default -> -1.0;
        };
    }
}
