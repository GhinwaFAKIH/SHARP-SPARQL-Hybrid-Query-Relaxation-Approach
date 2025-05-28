package Similarity;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class TermSim {
    // Java implementation of recursive Levenshtein distance
        static int compute_Levenshtein_distance(String str1,
                                                String str2)
        {
            if (str1.isEmpty())
            {
                return str2.length();
            }

            if (str2.isEmpty())
            {
                return str1.length();
            }

  // calculate the number of distinct characters to be replaced in str1 by recursively traversing each substring
            int replace = compute_Levenshtein_distance(
                    str1.substring(1), str2.substring(1))
                    + NumOfReplacement(str1.charAt(0),str2.charAt(0));

  // calculate the number of insertions in str1 recursively
            int insert = compute_Levenshtein_distance(
                    str1, str2.substring(1))+ 1;

  // calculate the number of deletions in str1 recursively
            int delete = compute_Levenshtein_distance(
                    str1.substring(1), str2)+ 1;

   // returns minimum of three operations

            return minm_edits(replace, insert, delete);
        }

        static int NumOfReplacement(char c1, char c2)
        {
            // check for distinct characters
            // in str1 and str2

            return c1 == c2 ? 0 : 1;
        }

        static int minm_edits(int... nums)
        {
            // receives the count of different
            // operations performed and returns the
            // minimum value among them.

            return Arrays.stream(nums).min().orElse(
                    Integer.MAX_VALUE);
        }

    public static double calculateNumericSimilarity(double value1, double value2, double min, double max) {
        if (max <= min) {
            throw new IllegalArgumentException("Max must be greater than min");
        }
        return 1.0 - Math.abs(value1 - value2) / (max - min);
    }

    public static double calculateDateSimilarity(LocalDate date1, LocalDate date2, LocalDate min, LocalDate max) {
        if (max.isBefore(min)) {
            throw new IllegalArgumentException("Max date must be equal to or after min date");
        }

        long daysDifference = Math.abs(ChronoUnit.DAYS.between(date1, date2));
        long maxMinDifference = ChronoUnit.DAYS.between(min, max);

        return 1.0 - (double) daysDifference / maxMinDifference;
    }

        public static void main(String args[])
        {
            String s1 = "GIKY";
            String s2 = "GEEKY";
            double value1 = 5.0;
            double value2 = 8.0;
            double min = 0.0;
            double max = 10.0;

            double similarity = calculateNumericSimilarity(value1, value2, min, max);

            System.out.println("Similarity between " + value1 + " and " + value2 + ": " + similarity);

            System.out.println(compute_Levenshtein_distance(s1, s2));
        }

}
