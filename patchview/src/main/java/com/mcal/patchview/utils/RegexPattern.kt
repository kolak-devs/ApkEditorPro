package com.mcal.patchview.utils

import java.util.regex.Pattern

/**
 * Created by Snow Volf on 26.08.2017, 15:05
 */

object RegexPattern {
    var COMMON_SYMBOLS: Pattern = Pattern.compile("\\[|\\]|\\[/|\\*|\\{|\\}|true|false")
    var ATTRIBUTE: Pattern = Pattern.compile(
        "MIN_ENGINE_VER|AUTHOR|PACKAGE|MATCH_GOTO|MATCH_ASSIGN|" +
                "MATCH_REPLACE|GOTO|ADD_FILES|ADD_FILES|REMOVE_FILES|MERGE|DUMMY|APPLICATION|" +
                "LAUNCHER_ACTIVITIES|ACTIVITIES|GROUP|EXECUTE_DEX|SIGNATURE_REVISE\\d+"
    )
    var SUB_ATTRIBUTE: Pattern = Pattern.compile(
        "REGEX:|MATCH:|REPLACE:|GOTO:|TARGET:|ASSIGN:|MERGE:|" +
                "SOURCE:|EXTRACT:|NAME:|SCRIPT:|SMALI_NEEDED:|MAIN_CLASS:|ENTRANCE:|PARAM:"
    )
    var STRING: Pattern = Pattern.compile("#.*")//"(?:'[^'\\\\\\n]*(?:\\\\.[^'\\\\\\n]*)*')|(?:\"[^\"\\\\\\n]*(?:\\\\.[^\"\\\\\\n]*)*\")")
    var OPERATOR: Pattern = Pattern.compile("\\([^)]+\\)|\\$", Pattern.CASE_INSENSITIVE)
    var NUMBERS: Pattern =
        Pattern.compile("(?<!-|/|\\$)-?\\b(?:0(X|x)[0-9a-zA-Z]+(?:t|T|s|S|l|L)?|\\d+(?:\\.\\d+)?(?:f|F|t|T|s|S|l|L)?)\\b", Pattern.CASE_INSENSITIVE)
    var REGISTERS = "[pv]\\d+"
    var EIGHT_SPACES = "(\\s){8}"
}