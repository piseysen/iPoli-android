package io.ipoli.android.quest.parsers;

import android.util.Log;

import org.ocpsoft.prettytime.nlp.PrettyTimeParser;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Venelin Valkov <venelin@curiousily.com>
 * on 2/19/16.
 */
public class DueDateMatcher implements QuestTextMatcher<Date> {

    private static final String DUE_TODAY_TOMORROW_PATTERN = "(?:^|\\s)(today|tomorrow)(?:$|\\s)";
    private static final String DUE_MONTH_PATTERN = "((?:^|\\s)on)?\\s(\\d){1,2}(\\s)?(st|th)?\\s(of\\s)?(next month|this month|January|February|March|April|May|June|July|August|September|October|November|December|Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec){1}(?:$|\\s)";
    private static final String DUE_AFTER_IN_PATTERN = "(?:^|\\s)(after|in)\\s\\w+\\s(day|week|month|year)s?(?:$|\\s)";
    private static final String DUE_FROM_NOW_PATTERN = "(?:^|\\s)\\w+\\s(day|week|month|year)s?\\sfrom\\snow(?:$|\\s)";
    private static final String DUE_THIS_NEXT_PATTERN = "(?:^|\\s)(this|next)\\s(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday|Mon|Tue|Wed|Thur|Fri|Sat|Sun)(?:$|\\s)";
    private static final String DUE_THIS_MONTH_PATTERN = "(?:^|\\s)on\\s?(\\d{1,2})\\s?(st|th)$";

    private static final Pattern[] dueDatePatterns = {
            Pattern.compile(DUE_TODAY_TOMORROW_PATTERN, Pattern.CASE_INSENSITIVE),
            Pattern.compile(DUE_MONTH_PATTERN, Pattern.CASE_INSENSITIVE),
            Pattern.compile(DUE_THIS_NEXT_PATTERN, Pattern.CASE_INSENSITIVE),
            Pattern.compile(DUE_AFTER_IN_PATTERN, Pattern.CASE_INSENSITIVE),
            Pattern.compile(DUE_FROM_NOW_PATTERN, Pattern.CASE_INSENSITIVE)
    };

    private static final Pattern dueThisMonthPattern = Pattern.compile(DUE_THIS_MONTH_PATTERN, Pattern.CASE_INSENSITIVE);
    private final PrettyTimeParser parser;

    public DueDateMatcher(PrettyTimeParser parser) {
        this.parser = parser;
    }

    @Override
    public String match(String text) {

        Matcher tmm = dueThisMonthPattern.matcher(text);
        if (tmm.find()) {
            int day = Integer.parseInt(tmm.group(1));
            Calendar c = Calendar.getInstance();
            int maxDaysInMoth = c.getActualMaximum(Calendar.DAY_OF_MONTH);
            if (day > maxDaysInMoth) {
                return "";
            }
            return tmm.group().trim();
        }

        for (Pattern p : dueDatePatterns) {
            Matcher matcher = p.matcher(text);
            if (matcher.find()) {
                return matcher.group().trim();
            }
        }
        return "";
    }

    @Override
    public Date parse(String text) {
        Matcher tmm = dueThisMonthPattern.matcher(text);
        if (tmm.find()) {
            int day = Integer.parseInt(tmm.group(1));
            Calendar c = Calendar.getInstance();
            int maxDaysInMoth = c.getActualMaximum(Calendar.DAY_OF_MONTH);
            if (day > maxDaysInMoth) {
                return null;
            }
            c.set(Calendar.DAY_OF_MONTH, day);
            return c.getTime();
        }

        for (Pattern p : dueDatePatterns) {
            Matcher matcher = p.matcher(text);
            if (matcher.find()) {
                List<Date> dueResult = parser.parse(matcher.group());
                if (dueResult.size() != 1) {
                    return null;
                }
                return dueResult.get(0);
            }
        }
        return null;
    }

    @Override
    public boolean partiallyMatches(String text) {
        Matcher tmm = dueThisMonthPattern.matcher(text);
        tmm.matches();
        if(tmm.hitEnd()) {
            Log.d("Due Date part true tmm", dueThisMonthPattern.toString());
            return true;
        }

        for (Pattern p : dueDatePatterns) {
            Matcher matcher = p.matcher(text);
            matcher.matches();
            if(matcher.hitEnd()) {
                Log.d("Due Date partially true", p.toString());
                return true;
            }
        }
        return false;
    }

}
