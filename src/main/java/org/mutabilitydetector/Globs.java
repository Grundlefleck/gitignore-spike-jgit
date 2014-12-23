package org.mutabilitydetector;

import java.util.regex.Pattern;

public final class Globs {

    public static Pattern createRegexInProgress(String glob) {
        char[] chars;
        StringBuilder sb = new StringBuilder();

        if (!glob.startsWith("/")) {
            if (!glob.startsWith("**")) {
                sb.append("([^/]*?/)*");
            }
            chars = glob.toCharArray();
        } else {
            sb.append("^");
            chars = glob.substring(1).toCharArray();
        }

        boolean escape = false, star = false, doubleStar = false, bracket = false;
        for (char ch : chars) {
            if (bracket && ch != ']') {
                sb.append(ch);
                continue;
            } else if (doubleStar) {
                doubleStar = false;
                if (ch == '/') {
                    sb.append("([^/]*/)*?");
                    continue;
                } else {
                    sb.append("[^/]*?");
                }
            }

            if (ch == '*') {
                if (escape) {
                    sb.append("\\*");
                    escape = false;
                    star = false;
                } else if (star) {
                    char prev = sb.length() > 0 ? sb.charAt(sb.length() - 1) : '\0';
                    if (prev == '\0' || prev == '^' || prev == '/') {
                        doubleStar = true;
                    } else {
                        sb.append("[^/]*?");
                    }
                    star = false;
                } else {
                    star = true;
                }
                continue;
            } else if (star) {
                sb.append("[^/]*?");
                star = false;
            }

            switch (ch) {

                case '\\':
                    if (escape) {
                        sb.append("\\\\");
                        escape = false;
                    } else {
                        escape = true;
                    }
                    break;

                case '?':
                    if (escape) {
                        sb.append("\\?");
                        escape = false;
                    } else {
                        sb.append('.');
                    }
                    break;

                case '[':
                    if (escape) {
                        sb.append('\\');
                        escape = false;
                    } else {
                        bracket = true;
                    }
                    sb.append(ch);
                    break;

                case ']':
                    if (!bracket || escape) {
                        sb.append('\\');
                    }
                    sb.append(ch);
                    bracket = false;
                    escape = false;
                    break;

                case '.':
                case '(':
                case ')':
                case '+':
                case '|':
                case '^':
                case '$':
                case '@':
                case '%':
                    sb.append('\\');
                    sb.append(ch);
                    escape = false;
                    break;

                default:
                    escape = false;
                    sb.append(ch);

            }
        }

        if (star || doubleStar) {
            sb.append("[^/]+");
        } else if (sb.charAt(sb.length() - 1) == '/') {
            sb.append("?");
        }
        sb.append("/?$");

        return Pattern.compile(sb.toString());
    }

    public static String createRegex(String glob) {
        char[] chars;
        StringBuilder sb = new StringBuilder();

        if (!glob.startsWith("/")) {
            if (!glob.startsWith("**")) {
                sb.append("([^/]*?/)*");
            }
            chars = glob.toCharArray();
        } else {
            sb.append("^");
            chars = glob.substring(1).toCharArray();
        }

        boolean escape = false, star = false, doubleStar = false, bracket = false;
        for (char ch : chars) {
            if (bracket && ch != ']') {
                sb.append(ch);
                continue;
            } else if (doubleStar) {
                doubleStar = false;
                if (ch == '/') {
                    sb.append("([^/]*/)*?");
                    continue;
                } else {
                    sb.append("[^/]*?");
                }
            }

            if (ch == '*') {
                if (escape) {
                    sb.append("\\*");
                    escape = false;
                    star = false;
                } else if (star) {
                    char prev = sb.length() > 0 ? sb.charAt(sb.length() - 1) : '\0';
                    if (prev == '\0' || prev == '^' || prev == '/') {
                        doubleStar = true;
                    } else {
                        sb.append("[^/]*?");
                    }
                    star = false;
                } else {
                    star = true;
                }
                continue;
            } else if (star) {
                sb.append("[^/]*?");
                star = false;
            }

            switch (ch) {

                case '\\':
                    if (escape) {
                        sb.append("\\\\");
                        escape = false;
                    } else {
                        escape = true;
                    }
                    break;

                case '?':
                    if (escape) {
                        sb.append("\\?");
                        escape = false;
                    } else {
                        sb.append('.');
                    }
                    break;

                case '[':
                    if (escape) {
                        sb.append('\\');
                        escape = false;
                    } else {
                        bracket = true;
                    }
                    sb.append(ch);
                    break;

                case ']':
                    if (!bracket || escape) {
                        sb.append('\\');
                    }
                    sb.append(ch);
                    bracket = false;
                    escape = false;
                    break;

                case '.':
                case '(':
                case ')':
                case '+':
                case '|':
                case '^':
                case '$':
                case '@':
                case '%':
                    sb.append('\\');
                    sb.append(ch);
                    escape = false;
                    break;

                default:
                    escape = false;
                    sb.append(ch);

            }
        }

        if (star || doubleStar) {
            sb.append("[^/]+");
        } else if (sb.charAt(sb.length() - 1) == '/') {
            sb.append("?");
        }
        sb.append("/?$");

        return sb.toString();
    }
}
