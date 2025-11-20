// Generated from ..\ant4\IslLexer.g4 by ANTLR 4.9.1
package com.intuit.isl.ant4;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class IslLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.9.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		IFCODE=1, ELSE=2, ENDIFCODE=3, SWITCH=4, ARROW=5, ENDSWITCH=6, FILTER=7, 
		MAP=8, EQUAL_EQUAL=9, NOT_EQUAL=10, LESS_OR_EQUAL=11, GREATER_OR_EQUAL=12, 
		GREATER=13, LESS=14, CONTAINS=15, NOT_CONTAINS=16, STARTS_WITH=17, NOT_STARTS_WITH=18, 
		ENDS_WITH=19, NOT_ENDS_WITH=20, IN=21, NOT_IN=22, IS=23, NOT_IS=24, MATCHES=25, 
		NOT_MATCHES=26, PARALLEL=27, FOR=28, ENDFOR=29, WHILELOOP=30, ENDWHILELOOP=31, 
		FUN=32, MODIFIER_FUN=33, RETURN=34, CACHE=35, IMPORT=36, DECLARETYPE=37, 
		AS=38, FROM=39, OPEN_BACKTICK=40, LOP=41, BANG=42, COLON=43, EQUAL=44, 
		SEMICOLON=45, MODIFIER=46, COMMA=47, DOLLAR=48, BACKSLASH=49, AT=50, SPREAD=51, 
		DOT=52, COALESCE=53, OPAREN=54, CPAREN=55, ARRAYCONDOPEN=56, ARRAYCONDCLOSE=57, 
		SQUAREOPEN=58, SQUARECLOSE=59, CURLYOPEN=60, CURLYCLOSE=61, CURLYOPENOPEN=62, 
		CURLYCLOSECLOSE=63, MATH_TIMES=64, MATH_DIV=65, MATH_PLUS=66, MATH_MINUS=67, 
		BOOL=68, NUM=69, ID=70, QUOTEDSTRING=71, COMMENT=72, COMMENT2=73, WS=74, 
		UNKNOWN=75, ENTER_EXPR_INTERP=76, ENTER_MATH_INTERP=77, ENTER_FUNC_INTERP=78, 
		ID_INTERP=79, TEXT=80, RECOVERTOKENS_INTERP=81, CLOSE_BACKTICK=82;
	public static final int
		INTERPOLATE=1;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE", "INTERPOLATE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"IFCODE", "ELSE", "ENDIFCODE", "SWITCH", "ARROW", "ENDSWITCH", "FILTER", 
			"MAP", "EQUAL_EQUAL", "NOT_EQUAL", "LESS_OR_EQUAL", "GREATER_OR_EQUAL", 
			"GREATER", "LESS", "CONTAINS", "NOT_CONTAINS", "STARTS_WITH", "NOT_STARTS_WITH", 
			"ENDS_WITH", "NOT_ENDS_WITH", "IN", "NOT_IN", "IS", "NOT_IS", "MATCHES", 
			"NOT_MATCHES", "PARALLEL", "FOR", "ENDFOR", "WHILELOOP", "ENDWHILELOOP", 
			"FUN", "MODIFIER_FUN", "RETURN", "CACHE", "IMPORT", "DECLARETYPE", "AS", 
			"FROM", "OPEN_BACKTICK", "LOP", "BANG", "COLON", "EQUAL", "SEMICOLON", 
			"MODIFIER", "COMMA", "DOLLAR", "BACKSLASH", "AT", "SPREAD", "DOT", "COALESCE", 
			"OPAREN", "CPAREN", "ARRAYCONDOPEN", "ARRAYCONDCLOSE", "SQUAREOPEN", 
			"SQUARECLOSE", "CURLYOPEN", "CURLYCLOSE", "CURLYOPENOPEN", "CURLYCLOSECLOSE", 
			"MATH_TIMES", "MATH_DIV", "MATH_PLUS", "MATH_MINUS", "BOOL", "NUM", "ID", 
			"QUOTEDSTRING", "COMMENT", "COMMENT2", "WS", "UNKNOWN", "Idpart", "Digit", 
			"ENTER_EXPR_INTERP", "ENTER_MATH_INTERP", "ENTER_FUNC_INTERP", "ID_INTERP", 
			"TEXT", "RECOVERTOKENS_INTERP", "CLOSE_BACKTICK"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'if'", "'else'", "'endif'", "'switch'", "'->'", "'endswitch'", 
			"'filter'", "'map'", "'=='", "'!='", "'<='", "'>='", "'>'", "'<'", "'contains'", 
			"'!contains'", "'startsWith'", "'!startsWith'", "'endsWith'", "'!endsWith'", 
			"'in'", "'!in'", "'is'", "'!is'", "'matches'", "'!matches'", "'parallel'", 
			"'foreach'", "'endfor'", "'while'", "'endwhile'", "'fun'", "'modifier'", 
			"'return'", "'cache'", "'import'", "'type'", "'as'", "'from'", null, 
			null, "'!'", "':'", "'='", "';'", "'|'", "','", "'$'", "'\\'", "'@'", 
			"'...'", "'.'", "'??'", "'('", "')'", "'[('", "')]'", "'['", "']'", "'{'", 
			"'}'", null, "'}}'", "'*'", "'/'", "'+'", "'-'", null, null, null, null, 
			null, null, null, null, "'${'", null, "'@.'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "IFCODE", "ELSE", "ENDIFCODE", "SWITCH", "ARROW", "ENDSWITCH", 
			"FILTER", "MAP", "EQUAL_EQUAL", "NOT_EQUAL", "LESS_OR_EQUAL", "GREATER_OR_EQUAL", 
			"GREATER", "LESS", "CONTAINS", "NOT_CONTAINS", "STARTS_WITH", "NOT_STARTS_WITH", 
			"ENDS_WITH", "NOT_ENDS_WITH", "IN", "NOT_IN", "IS", "NOT_IS", "MATCHES", 
			"NOT_MATCHES", "PARALLEL", "FOR", "ENDFOR", "WHILELOOP", "ENDWHILELOOP", 
			"FUN", "MODIFIER_FUN", "RETURN", "CACHE", "IMPORT", "DECLARETYPE", "AS", 
			"FROM", "OPEN_BACKTICK", "LOP", "BANG", "COLON", "EQUAL", "SEMICOLON", 
			"MODIFIER", "COMMA", "DOLLAR", "BACKSLASH", "AT", "SPREAD", "DOT", "COALESCE", 
			"OPAREN", "CPAREN", "ARRAYCONDOPEN", "ARRAYCONDCLOSE", "SQUAREOPEN", 
			"SQUARECLOSE", "CURLYOPEN", "CURLYCLOSE", "CURLYOPENOPEN", "CURLYCLOSECLOSE", 
			"MATH_TIMES", "MATH_DIV", "MATH_PLUS", "MATH_MINUS", "BOOL", "NUM", "ID", 
			"QUOTEDSTRING", "COMMENT", "COMMENT2", "WS", "UNKNOWN", "ENTER_EXPR_INTERP", 
			"ENTER_MATH_INTERP", "ENTER_FUNC_INTERP", "ID_INTERP", "TEXT", "RECOVERTOKENS_INTERP", 
			"CLOSE_BACKTICK"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


	    int interpolationNestingExpressions = 0;
	    int interpolationNestingMath = 0;
	    int interpolationNestingFunc = 0;


	public IslLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "IslLexer.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	@Override
	public void action(RuleContext _localctx, int ruleIndex, int actionIndex) {
		switch (ruleIndex) {
		case 54:
			CPAREN_action((RuleContext)_localctx, actionIndex);
			break;
		case 60:
			CURLYCLOSE_action((RuleContext)_localctx, actionIndex);
			break;
		case 62:
			CURLYCLOSECLOSE_action((RuleContext)_localctx, actionIndex);
			break;
		case 77:
			ENTER_EXPR_INTERP_action((RuleContext)_localctx, actionIndex);
			break;
		case 78:
			ENTER_MATH_INTERP_action((RuleContext)_localctx, actionIndex);
			break;
		case 79:
			ENTER_FUNC_INTERP_action((RuleContext)_localctx, actionIndex);
			break;
		}
	}
	private void CPAREN_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 0:

			    if ( interpolationNestingFunc > 0 ){
			        interpolationNestingFunc--;
			        popMode();
			    }

			break;
		}
	}
	private void CURLYCLOSE_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 1:

			    if ( interpolationNestingExpressions > 0 ){
			        interpolationNestingExpressions--;
			        popMode();
			    }

			break;
		}
	}
	private void CURLYCLOSECLOSE_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 2:

			    if ( interpolationNestingMath > 0 ){
			        interpolationNestingMath--;
			        popMode();
			    }

			break;
		}
	}
	private void ENTER_EXPR_INTERP_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 3:

			    interpolationNestingExpressions++;
			    pushMode(DEFAULT_MODE);

			break;
		}
	}
	private void ENTER_MATH_INTERP_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 4:

			    interpolationNestingMath++;
			    pushMode(DEFAULT_MODE);

			break;
		}
	}
	private void ENTER_FUNC_INTERP_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 5:

			    interpolationNestingFunc++;
			    pushMode(DEFAULT_MODE);

			break;
		}
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2T\u0263\b\1\b\1\4"+
		"\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n"+
		"\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t"+
		" \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t"+
		"+\4,\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64"+
		"\t\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\4=\t"+
		"=\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\4C\tC\4D\tD\4E\tE\4F\tF\4G\tG\4H\tH\4"+
		"I\tI\4J\tJ\4K\tK\4L\tL\4M\tM\4N\tN\4O\tO\4P\tP\4Q\tQ\4R\tR\4S\tS\4T\t"+
		"T\4U\tU\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\4\3\4\3\4\3\4\3\4\3\4\3\5\3"+
		"\5\3\5\3\5\3\5\3\5\3\5\3\6\3\6\3\6\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7"+
		"\3\7\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\t\3\t\3\t\3\t\3\n\3\n\3\n\3\13\3\13"+
		"\3\13\3\f\3\f\3\f\3\r\3\r\3\r\3\16\3\16\3\17\3\17\3\20\3\20\3\20\3\20"+
		"\3\20\3\20\3\20\3\20\3\20\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21"+
		"\3\21\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\23\3\23"+
		"\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\24\3\24\3\24\3\24"+
		"\3\24\3\24\3\24\3\24\3\24\3\25\3\25\3\25\3\25\3\25\3\25\3\25\3\25\3\25"+
		"\3\25\3\26\3\26\3\26\3\27\3\27\3\27\3\27\3\30\3\30\3\30\3\31\3\31\3\31"+
		"\3\31\3\32\3\32\3\32\3\32\3\32\3\32\3\32\3\32\3\33\3\33\3\33\3\33\3\33"+
		"\3\33\3\33\3\33\3\33\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\35"+
		"\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\36\3\36\3\36\3\36\3\36\3\36\3\36"+
		"\3\37\3\37\3\37\3\37\3\37\3\37\3 \3 \3 \3 \3 \3 \3 \3 \3 \3!\3!\3!\3!"+
		"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3#\3#\3#\3#\3#\3#\3#\3$\3$\3$\3$"+
		"\3$\3$\3%\3%\3%\3%\3%\3%\3%\3&\3&\3&\3&\3&\3\'\3\'\3\'\3(\3(\3(\3(\3("+
		"\3)\3)\3)\3)\3*\3*\3*\3*\3*\5*\u01a4\n*\3+\3+\3,\3,\3-\3-\3.\3.\3/\3/"+
		"\3\60\3\60\3\61\3\61\3\62\3\62\3\63\3\63\3\64\3\64\3\64\3\64\3\65\3\65"+
		"\3\66\3\66\3\66\3\67\3\67\38\38\38\39\39\39\3:\3:\3:\3;\3;\3<\3<\3=\3"+
		"=\3>\3>\3>\3?\3?\3?\3@\3@\3@\3@\3@\3A\3A\3B\3B\3C\3C\3D\3D\3E\3E\3E\3"+
		"E\3E\3E\3E\3E\3E\3E\3E\3E\3E\5E\u01f2\nE\3F\5F\u01f5\nF\3F\3F\3F\5F\u01fa"+
		"\nF\3G\3G\7G\u01fe\nG\fG\16G\u0201\13G\3H\3H\7H\u0205\nH\fH\16H\u0208"+
		"\13H\3H\3H\3H\7H\u020d\nH\fH\16H\u0210\13H\3H\5H\u0213\nH\3I\3I\7I\u0217"+
		"\nI\fI\16I\u021a\13I\3I\3I\3J\3J\3J\3J\7J\u0222\nJ\fJ\16J\u0225\13J\3"+
		"J\3J\3K\6K\u022a\nK\rK\16K\u022b\3K\3K\3L\3L\3M\3M\3M\3M\3M\5M\u0237\n"+
		"M\3N\6N\u023a\nN\rN\16N\u023b\3O\3O\3O\3O\3O\3P\3P\3P\3P\3P\3Q\3Q\3Q\3"+
		"Q\3Q\3R\3R\3R\7R\u0250\nR\fR\16R\u0253\13R\3S\3S\3S\3S\3S\6S\u025a\nS"+
		"\rS\16S\u025b\3T\3T\3U\3U\3U\3U\4\u0206\u020e\2V\4\3\6\4\b\5\n\6\f\7\16"+
		"\b\20\t\22\n\24\13\26\f\30\r\32\16\34\17\36\20 \21\"\22$\23&\24(\25*\26"+
		",\27.\30\60\31\62\32\64\33\66\348\35:\36<\37> @!B\"D#F$H%J&L\'N(P)R*T"+
		"+V,X-Z.\\/^\60`\61b\62d\63f\64h\65j\66l\67n8p9r:t;v<x=z>|?~@\u0080A\u0082"+
		"B\u0084C\u0086D\u0088E\u008aF\u008cG\u008eH\u0090I\u0092J\u0094K\u0096"+
		"L\u0098M\u009a\2\u009c\2\u009eN\u00a0O\u00a2P\u00a4Q\u00a6R\u00a8S\u00aa"+
		"T\4\2\3\13\5\2C\\aac|\6\2\62;C\\aac|\3\2$$\3\2))\4\2\f\f\17\17\5\2\13"+
		"\f\17\17\"\"\3\2\62;\6\2&&BBbb}}\5\2&&BB}}\2\u0271\2\4\3\2\2\2\2\6\3\2"+
		"\2\2\2\b\3\2\2\2\2\n\3\2\2\2\2\f\3\2\2\2\2\16\3\2\2\2\2\20\3\2\2\2\2\22"+
		"\3\2\2\2\2\24\3\2\2\2\2\26\3\2\2\2\2\30\3\2\2\2\2\32\3\2\2\2\2\34\3\2"+
		"\2\2\2\36\3\2\2\2\2 \3\2\2\2\2\"\3\2\2\2\2$\3\2\2\2\2&\3\2\2\2\2(\3\2"+
		"\2\2\2*\3\2\2\2\2,\3\2\2\2\2.\3\2\2\2\2\60\3\2\2\2\2\62\3\2\2\2\2\64\3"+
		"\2\2\2\2\66\3\2\2\2\28\3\2\2\2\2:\3\2\2\2\2<\3\2\2\2\2>\3\2\2\2\2@\3\2"+
		"\2\2\2B\3\2\2\2\2D\3\2\2\2\2F\3\2\2\2\2H\3\2\2\2\2J\3\2\2\2\2L\3\2\2\2"+
		"\2N\3\2\2\2\2P\3\2\2\2\2R\3\2\2\2\2T\3\2\2\2\2V\3\2\2\2\2X\3\2\2\2\2Z"+
		"\3\2\2\2\2\\\3\2\2\2\2^\3\2\2\2\2`\3\2\2\2\2b\3\2\2\2\2d\3\2\2\2\2f\3"+
		"\2\2\2\2h\3\2\2\2\2j\3\2\2\2\2l\3\2\2\2\2n\3\2\2\2\2p\3\2\2\2\2r\3\2\2"+
		"\2\2t\3\2\2\2\2v\3\2\2\2\2x\3\2\2\2\2z\3\2\2\2\2|\3\2\2\2\2~\3\2\2\2\2"+
		"\u0080\3\2\2\2\2\u0082\3\2\2\2\2\u0084\3\2\2\2\2\u0086\3\2\2\2\2\u0088"+
		"\3\2\2\2\2\u008a\3\2\2\2\2\u008c\3\2\2\2\2\u008e\3\2\2\2\2\u0090\3\2\2"+
		"\2\2\u0092\3\2\2\2\2\u0094\3\2\2\2\2\u0096\3\2\2\2\2\u0098\3\2\2\2\3\u009e"+
		"\3\2\2\2\3\u00a0\3\2\2\2\3\u00a2\3\2\2\2\3\u00a4\3\2\2\2\3\u00a6\3\2\2"+
		"\2\3\u00a8\3\2\2\2\3\u00aa\3\2\2\2\4\u00ac\3\2\2\2\6\u00af\3\2\2\2\b\u00b4"+
		"\3\2\2\2\n\u00ba\3\2\2\2\f\u00c1\3\2\2\2\16\u00c4\3\2\2\2\20\u00ce\3\2"+
		"\2\2\22\u00d5\3\2\2\2\24\u00d9\3\2\2\2\26\u00dc\3\2\2\2\30\u00df\3\2\2"+
		"\2\32\u00e2\3\2\2\2\34\u00e5\3\2\2\2\36\u00e7\3\2\2\2 \u00e9\3\2\2\2\""+
		"\u00f2\3\2\2\2$\u00fc\3\2\2\2&\u0107\3\2\2\2(\u0113\3\2\2\2*\u011c\3\2"+
		"\2\2,\u0126\3\2\2\2.\u0129\3\2\2\2\60\u012d\3\2\2\2\62\u0130\3\2\2\2\64"+
		"\u0134\3\2\2\2\66\u013c\3\2\2\28\u0145\3\2\2\2:\u014e\3\2\2\2<\u0156\3"+
		"\2\2\2>\u015d\3\2\2\2@\u0163\3\2\2\2B\u016c\3\2\2\2D\u0170\3\2\2\2F\u0179"+
		"\3\2\2\2H\u0180\3\2\2\2J\u0186\3\2\2\2L\u018d\3\2\2\2N\u0192\3\2\2\2P"+
		"\u0195\3\2\2\2R\u019a\3\2\2\2T\u01a3\3\2\2\2V\u01a5\3\2\2\2X\u01a7\3\2"+
		"\2\2Z\u01a9\3\2\2\2\\\u01ab\3\2\2\2^\u01ad\3\2\2\2`\u01af\3\2\2\2b\u01b1"+
		"\3\2\2\2d\u01b3\3\2\2\2f\u01b5\3\2\2\2h\u01b7\3\2\2\2j\u01bb\3\2\2\2l"+
		"\u01bd\3\2\2\2n\u01c0\3\2\2\2p\u01c2\3\2\2\2r\u01c5\3\2\2\2t\u01c8\3\2"+
		"\2\2v\u01cb\3\2\2\2x\u01cd\3\2\2\2z\u01cf\3\2\2\2|\u01d1\3\2\2\2~\u01d4"+
		"\3\2\2\2\u0080\u01d7\3\2\2\2\u0082\u01dc\3\2\2\2\u0084\u01de\3\2\2\2\u0086"+
		"\u01e0\3\2\2\2\u0088\u01e2\3\2\2\2\u008a\u01f1\3\2\2\2\u008c\u01f4\3\2"+
		"\2\2\u008e\u01fb\3\2\2\2\u0090\u0212\3\2\2\2\u0092\u0214\3\2\2\2\u0094"+
		"\u021d\3\2\2\2\u0096\u0229\3\2\2\2\u0098\u022f\3\2\2\2\u009a\u0231\3\2"+
		"\2\2\u009c\u0239\3\2\2\2\u009e\u023d\3\2\2\2\u00a0\u0242\3\2\2\2\u00a2"+
		"\u0247\3\2\2\2\u00a4\u024c\3\2\2\2\u00a6\u0259\3\2\2\2\u00a8\u025d\3\2"+
		"\2\2\u00aa\u025f\3\2\2\2\u00ac\u00ad\7k\2\2\u00ad\u00ae\7h\2\2\u00ae\5"+
		"\3\2\2\2\u00af\u00b0\7g\2\2\u00b0\u00b1\7n\2\2\u00b1\u00b2\7u\2\2\u00b2"+
		"\u00b3\7g\2\2\u00b3\7\3\2\2\2\u00b4\u00b5\7g\2\2\u00b5\u00b6\7p\2\2\u00b6"+
		"\u00b7\7f\2\2\u00b7\u00b8\7k\2\2\u00b8\u00b9\7h\2\2\u00b9\t\3\2\2\2\u00ba"+
		"\u00bb\7u\2\2\u00bb\u00bc\7y\2\2\u00bc\u00bd\7k\2\2\u00bd\u00be\7v\2\2"+
		"\u00be\u00bf\7e\2\2\u00bf\u00c0\7j\2\2\u00c0\13\3\2\2\2\u00c1\u00c2\7"+
		"/\2\2\u00c2\u00c3\7@\2\2\u00c3\r\3\2\2\2\u00c4\u00c5\7g\2\2\u00c5\u00c6"+
		"\7p\2\2\u00c6\u00c7\7f\2\2\u00c7\u00c8\7u\2\2\u00c8\u00c9\7y\2\2\u00c9"+
		"\u00ca\7k\2\2\u00ca\u00cb\7v\2\2\u00cb\u00cc\7e\2\2\u00cc\u00cd\7j\2\2"+
		"\u00cd\17\3\2\2\2\u00ce\u00cf\7h\2\2\u00cf\u00d0\7k\2\2\u00d0\u00d1\7"+
		"n\2\2\u00d1\u00d2\7v\2\2\u00d2\u00d3\7g\2\2\u00d3\u00d4\7t\2\2\u00d4\21"+
		"\3\2\2\2\u00d5\u00d6\7o\2\2\u00d6\u00d7\7c\2\2\u00d7\u00d8\7r\2\2\u00d8"+
		"\23\3\2\2\2\u00d9\u00da\7?\2\2\u00da\u00db\7?\2\2\u00db\25\3\2\2\2\u00dc"+
		"\u00dd\7#\2\2\u00dd\u00de\7?\2\2\u00de\27\3\2\2\2\u00df\u00e0\7>\2\2\u00e0"+
		"\u00e1\7?\2\2\u00e1\31\3\2\2\2\u00e2\u00e3\7@\2\2\u00e3\u00e4\7?\2\2\u00e4"+
		"\33\3\2\2\2\u00e5\u00e6\7@\2\2\u00e6\35\3\2\2\2\u00e7\u00e8\7>\2\2\u00e8"+
		"\37\3\2\2\2\u00e9\u00ea\7e\2\2\u00ea\u00eb\7q\2\2\u00eb\u00ec\7p\2\2\u00ec"+
		"\u00ed\7v\2\2\u00ed\u00ee\7c\2\2\u00ee\u00ef\7k\2\2\u00ef\u00f0\7p\2\2"+
		"\u00f0\u00f1\7u\2\2\u00f1!\3\2\2\2\u00f2\u00f3\7#\2\2\u00f3\u00f4\7e\2"+
		"\2\u00f4\u00f5\7q\2\2\u00f5\u00f6\7p\2\2\u00f6\u00f7\7v\2\2\u00f7\u00f8"+
		"\7c\2\2\u00f8\u00f9\7k\2\2\u00f9\u00fa\7p\2\2\u00fa\u00fb\7u\2\2\u00fb"+
		"#\3\2\2\2\u00fc\u00fd\7u\2\2\u00fd\u00fe\7v\2\2\u00fe\u00ff\7c\2\2\u00ff"+
		"\u0100\7t\2\2\u0100\u0101\7v\2\2\u0101\u0102\7u\2\2\u0102\u0103\7Y\2\2"+
		"\u0103\u0104\7k\2\2\u0104\u0105\7v\2\2\u0105\u0106\7j\2\2\u0106%\3\2\2"+
		"\2\u0107\u0108\7#\2\2\u0108\u0109\7u\2\2\u0109\u010a\7v\2\2\u010a\u010b"+
		"\7c\2\2\u010b\u010c\7t\2\2\u010c\u010d\7v\2\2\u010d\u010e\7u\2\2\u010e"+
		"\u010f\7Y\2\2\u010f\u0110\7k\2\2\u0110\u0111\7v\2\2\u0111\u0112\7j\2\2"+
		"\u0112\'\3\2\2\2\u0113\u0114\7g\2\2\u0114\u0115\7p\2\2\u0115\u0116\7f"+
		"\2\2\u0116\u0117\7u\2\2\u0117\u0118\7Y\2\2\u0118\u0119\7k\2\2\u0119\u011a"+
		"\7v\2\2\u011a\u011b\7j\2\2\u011b)\3\2\2\2\u011c\u011d\7#\2\2\u011d\u011e"+
		"\7g\2\2\u011e\u011f\7p\2\2\u011f\u0120\7f\2\2\u0120\u0121\7u\2\2\u0121"+
		"\u0122\7Y\2\2\u0122\u0123\7k\2\2\u0123\u0124\7v\2\2\u0124\u0125\7j\2\2"+
		"\u0125+\3\2\2\2\u0126\u0127\7k\2\2\u0127\u0128\7p\2\2\u0128-\3\2\2\2\u0129"+
		"\u012a\7#\2\2\u012a\u012b\7k\2\2\u012b\u012c\7p\2\2\u012c/\3\2\2\2\u012d"+
		"\u012e\7k\2\2\u012e\u012f\7u\2\2\u012f\61\3\2\2\2\u0130\u0131\7#\2\2\u0131"+
		"\u0132\7k\2\2\u0132\u0133\7u\2\2\u0133\63\3\2\2\2\u0134\u0135\7o\2\2\u0135"+
		"\u0136\7c\2\2\u0136\u0137\7v\2\2\u0137\u0138\7e\2\2\u0138\u0139\7j\2\2"+
		"\u0139\u013a\7g\2\2\u013a\u013b\7u\2\2\u013b\65\3\2\2\2\u013c\u013d\7"+
		"#\2\2\u013d\u013e\7o\2\2\u013e\u013f\7c\2\2\u013f\u0140\7v\2\2\u0140\u0141"+
		"\7e\2\2\u0141\u0142\7j\2\2\u0142\u0143\7g\2\2\u0143\u0144\7u\2\2\u0144"+
		"\67\3\2\2\2\u0145\u0146\7r\2\2\u0146\u0147\7c\2\2\u0147\u0148\7t\2\2\u0148"+
		"\u0149\7c\2\2\u0149\u014a\7n\2\2\u014a\u014b\7n\2\2\u014b\u014c\7g\2\2"+
		"\u014c\u014d\7n\2\2\u014d9\3\2\2\2\u014e\u014f\7h\2\2\u014f\u0150\7q\2"+
		"\2\u0150\u0151\7t\2\2\u0151\u0152\7g\2\2\u0152\u0153\7c\2\2\u0153\u0154"+
		"\7e\2\2\u0154\u0155\7j\2\2\u0155;\3\2\2\2\u0156\u0157\7g\2\2\u0157\u0158"+
		"\7p\2\2\u0158\u0159\7f\2\2\u0159\u015a\7h\2\2\u015a\u015b\7q\2\2\u015b"+
		"\u015c\7t\2\2\u015c=\3\2\2\2\u015d\u015e\7y\2\2\u015e\u015f\7j\2\2\u015f"+
		"\u0160\7k\2\2\u0160\u0161\7n\2\2\u0161\u0162\7g\2\2\u0162?\3\2\2\2\u0163"+
		"\u0164\7g\2\2\u0164\u0165\7p\2\2\u0165\u0166\7f\2\2\u0166\u0167\7y\2\2"+
		"\u0167\u0168\7j\2\2\u0168\u0169\7k\2\2\u0169\u016a\7n\2\2\u016a\u016b"+
		"\7g\2\2\u016bA\3\2\2\2\u016c\u016d\7h\2\2\u016d\u016e\7w\2\2\u016e\u016f"+
		"\7p\2\2\u016fC\3\2\2\2\u0170\u0171\7o\2\2\u0171\u0172\7q\2\2\u0172\u0173"+
		"\7f\2\2\u0173\u0174\7k\2\2\u0174\u0175\7h\2\2\u0175\u0176\7k\2\2\u0176"+
		"\u0177\7g\2\2\u0177\u0178\7t\2\2\u0178E\3\2\2\2\u0179\u017a\7t\2\2\u017a"+
		"\u017b\7g\2\2\u017b\u017c\7v\2\2\u017c\u017d\7w\2\2\u017d\u017e\7t\2\2"+
		"\u017e\u017f\7p\2\2\u017fG\3\2\2\2\u0180\u0181\7e\2\2\u0181\u0182\7c\2"+
		"\2\u0182\u0183\7e\2\2\u0183\u0184\7j\2\2\u0184\u0185\7g\2\2\u0185I\3\2"+
		"\2\2\u0186\u0187\7k\2\2\u0187\u0188\7o\2\2\u0188\u0189\7r\2\2\u0189\u018a"+
		"\7q\2\2\u018a\u018b\7t\2\2\u018b\u018c\7v\2\2\u018cK\3\2\2\2\u018d\u018e"+
		"\7v\2\2\u018e\u018f\7{\2\2\u018f\u0190\7r\2\2\u0190\u0191\7g\2\2\u0191"+
		"M\3\2\2\2\u0192\u0193\7c\2\2\u0193\u0194\7u\2\2\u0194O\3\2\2\2\u0195\u0196"+
		"\7h\2\2\u0196\u0197\7t\2\2\u0197\u0198\7q\2\2\u0198\u0199\7o\2\2\u0199"+
		"Q\3\2\2\2\u019a\u019b\7b\2\2\u019b\u019c\3\2\2\2\u019c\u019d\b)\2\2\u019d"+
		"S\3\2\2\2\u019e\u019f\7c\2\2\u019f\u01a0\7p\2\2\u01a0\u01a4\7f\2\2\u01a1"+
		"\u01a2\7q\2\2\u01a2\u01a4\7t\2\2\u01a3\u019e\3\2\2\2\u01a3\u01a1\3\2\2"+
		"\2\u01a4U\3\2\2\2\u01a5\u01a6\7#\2\2\u01a6W\3\2\2\2\u01a7\u01a8\7<\2\2"+
		"\u01a8Y\3\2\2\2\u01a9\u01aa\7?\2\2\u01aa[\3\2\2\2\u01ab\u01ac\7=\2\2\u01ac"+
		"]\3\2\2\2\u01ad\u01ae\7~\2\2\u01ae_\3\2\2\2\u01af\u01b0\7.\2\2\u01b0a"+
		"\3\2\2\2\u01b1\u01b2\7&\2\2\u01b2c\3\2\2\2\u01b3\u01b4\7^\2\2\u01b4e\3"+
		"\2\2\2\u01b5\u01b6\7B\2\2\u01b6g\3\2\2\2\u01b7\u01b8\7\60\2\2\u01b8\u01b9"+
		"\7\60\2\2\u01b9\u01ba\7\60\2\2\u01bai\3\2\2\2\u01bb\u01bc\7\60\2\2\u01bc"+
		"k\3\2\2\2\u01bd\u01be\7A\2\2\u01be\u01bf\7A\2\2\u01bfm\3\2\2\2\u01c0\u01c1"+
		"\7*\2\2\u01c1o\3\2\2\2\u01c2\u01c3\7+\2\2\u01c3\u01c4\b8\3\2\u01c4q\3"+
		"\2\2\2\u01c5\u01c6\7]\2\2\u01c6\u01c7\7*\2\2\u01c7s\3\2\2\2\u01c8\u01c9"+
		"\7+\2\2\u01c9\u01ca\7_\2\2\u01cau\3\2\2\2\u01cb\u01cc\7]\2\2\u01ccw\3"+
		"\2\2\2\u01cd\u01ce\7_\2\2\u01cey\3\2\2\2\u01cf\u01d0\7}\2\2\u01d0{\3\2"+
		"\2\2\u01d1\u01d2\7\177\2\2\u01d2\u01d3\b>\4\2\u01d3}\3\2\2\2\u01d4\u01d5"+
		"\7}\2\2\u01d5\u01d6\7}\2\2\u01d6\177\3\2\2\2\u01d7\u01d8\7\177\2\2\u01d8"+
		"\u01d9\7\177\2\2\u01d9\u01da\3\2\2\2\u01da\u01db\b@\5\2\u01db\u0081\3"+
		"\2\2\2\u01dc\u01dd\7,\2\2\u01dd\u0083\3\2\2\2\u01de\u01df\7\61\2\2\u01df"+
		"\u0085\3\2\2\2\u01e0\u01e1\7-\2\2\u01e1\u0087\3\2\2\2\u01e2\u01e3\7/\2"+
		"\2\u01e3\u0089\3\2\2\2\u01e4\u01e5\7h\2\2\u01e5\u01e6\7c\2\2\u01e6\u01e7"+
		"\7n\2\2\u01e7\u01e8\7u\2\2\u01e8\u01f2\7g\2\2\u01e9\u01ea\7v\2\2\u01ea"+
		"\u01eb\7t\2\2\u01eb\u01ec\7w\2\2\u01ec\u01f2\7g\2\2\u01ed\u01ee\7p\2\2"+
		"\u01ee\u01ef\7w\2\2\u01ef\u01f0\7n\2\2\u01f0\u01f2\7n\2\2\u01f1\u01e4"+
		"\3\2\2\2\u01f1\u01e9\3\2\2\2\u01f1\u01ed\3\2\2\2\u01f2\u008b\3\2\2\2\u01f3"+
		"\u01f5\7/\2\2\u01f4\u01f3\3\2\2\2\u01f4\u01f5\3\2\2\2\u01f5\u01f6\3\2"+
		"\2\2\u01f6\u01f9\5\u009cN\2\u01f7\u01f8\7\60\2\2\u01f8\u01fa\5\u009cN"+
		"\2\u01f9\u01f7\3\2\2\2\u01f9\u01fa\3\2\2\2\u01fa\u008d\3\2\2\2\u01fb\u01ff"+
		"\t\2\2\2\u01fc\u01fe\t\3\2\2\u01fd\u01fc\3\2\2\2\u01fe\u0201\3\2\2\2\u01ff"+
		"\u01fd\3\2\2\2\u01ff\u0200\3\2\2\2\u0200\u008f\3\2\2\2\u0201\u01ff\3\2"+
		"\2\2\u0202\u0206\7$\2\2\u0203\u0205\n\4\2\2\u0204\u0203\3\2\2\2\u0205"+
		"\u0208\3\2\2\2\u0206\u0207\3\2\2\2\u0206\u0204\3\2\2\2\u0207\u0209\3\2"+
		"\2\2\u0208\u0206\3\2\2\2\u0209\u0213\7$\2\2\u020a\u020e\7)\2\2\u020b\u020d"+
		"\n\5\2\2\u020c\u020b\3\2\2\2\u020d\u0210\3\2\2\2\u020e\u020f\3\2\2\2\u020e"+
		"\u020c\3\2\2\2\u020f\u0211\3\2\2\2\u0210\u020e\3\2\2\2\u0211\u0213\7)"+
		"\2\2\u0212\u0202\3\2\2\2\u0212\u020a\3\2\2\2\u0213\u0091\3\2\2\2\u0214"+
		"\u0218\7%\2\2\u0215\u0217\n\6\2\2\u0216\u0215\3\2\2\2\u0217\u021a\3\2"+
		"\2\2\u0218\u0216\3\2\2\2\u0218\u0219\3\2\2\2\u0219\u021b\3\2\2\2\u021a"+
		"\u0218\3\2\2\2\u021b\u021c\bI\6\2\u021c\u0093\3\2\2\2\u021d\u021e\7\61"+
		"\2\2\u021e\u021f\7\61\2\2\u021f\u0223\3\2\2\2\u0220\u0222\n\6\2\2\u0221"+
		"\u0220\3\2\2\2\u0222\u0225\3\2\2\2\u0223\u0221\3\2\2\2\u0223\u0224\3\2"+
		"\2\2\u0224\u0226\3\2\2\2\u0225\u0223\3\2\2\2\u0226\u0227\bJ\6\2\u0227"+
		"\u0095\3\2\2\2\u0228\u022a\t\7\2\2\u0229\u0228\3\2\2\2\u022a\u022b\3\2"+
		"\2\2\u022b\u0229\3\2\2\2\u022b\u022c\3\2\2\2\u022c\u022d\3\2\2\2\u022d"+
		"\u022e\bK\6\2\u022e\u0097\3\2\2\2\u022f\u0230\13\2\2\2\u0230\u0099\3\2"+
		"\2\2\u0231\u0236\5\u008eG\2\u0232\u0233\7]\2\2\u0233\u0234\5\u009cN\2"+
		"\u0234\u0235\7_\2\2\u0235\u0237\3\2\2\2\u0236\u0232\3\2\2\2\u0236\u0237"+
		"\3\2\2\2\u0237\u009b\3\2\2\2\u0238\u023a\t\b\2\2\u0239\u0238\3\2\2\2\u023a"+
		"\u023b\3\2\2\2\u023b\u0239\3\2\2\2\u023b\u023c\3\2\2\2\u023c\u009d\3\2"+
		"\2\2\u023d\u023e\7&\2\2\u023e\u023f\7}\2\2\u023f\u0240\3\2\2\2\u0240\u0241"+
		"\bO\7\2\u0241\u009f\3\2\2\2\u0242\u0243\7}\2\2\u0243\u0244\7}\2\2\u0244"+
		"\u0245\3\2\2\2\u0245\u0246\bP\b\2\u0246\u00a1\3\2\2\2\u0247\u0248\7B\2"+
		"\2\u0248\u0249\7\60\2\2\u0249\u024a\3\2\2\2\u024a\u024b\bQ\t\2\u024b\u00a3"+
		"\3\2\2\2\u024c\u024d\7&\2\2\u024d\u0251\t\2\2\2\u024e\u0250\t\3\2\2\u024f"+
		"\u024e\3\2\2\2\u0250\u0253\3\2\2\2\u0251\u024f\3\2\2\2\u0251\u0252\3\2"+
		"\2\2\u0252\u00a5\3\2\2\2\u0253\u0251\3\2\2\2\u0254\u0255\7^\2\2\u0255"+
		"\u025a\7&\2\2\u0256\u0257\7&\2\2\u0257\u025a\7\60\2\2\u0258\u025a\n\t"+
		"\2\2\u0259\u0254\3\2\2\2\u0259\u0256\3\2\2\2\u0259\u0258\3\2\2\2\u025a"+
		"\u025b\3\2\2\2\u025b\u0259\3\2\2\2\u025b\u025c\3\2\2\2\u025c\u00a7\3\2"+
		"\2\2\u025d\u025e\t\n\2\2\u025e\u00a9\3\2\2\2\u025f\u0260\7b\2\2\u0260"+
		"\u0261\3\2\2\2\u0261\u0262\bU\n\2\u0262\u00ab\3\2\2\2\24\2\3\u01a3\u01f1"+
		"\u01f4\u01f9\u01ff\u0206\u020e\u0212\u0218\u0223\u022b\u0236\u023b\u0251"+
		"\u0259\u025b\13\7\3\2\38\2\3>\3\3@\4\b\2\2\3O\5\3P\6\3Q\7\6\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}