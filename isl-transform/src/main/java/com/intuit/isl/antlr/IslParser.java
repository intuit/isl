// Generated from IslParser.g4 by ANTLR 4.9.1
package com.intuit.isl.antlr;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class IslParser extends Parser {
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
		RULE_spec = 0, RULE_importDeclaration = 1, RULE_annotationParameter = 2, 
		RULE_annotationArguments = 3, RULE_annotation = 4, RULE_functionDeclaration = 5, 
		RULE_functionArguments = 6, RULE_variableWithType = 7, RULE_returnCall = 8, 
		RULE_functionStatements = 9, RULE_functionStatement = 10, RULE_statements = 11, 
		RULE_statement = 12, RULE_relop = 13, RULE_ifStatement = 14, RULE_elseClause = 15, 
		RULE_inlineIf = 16, RULE_inlineElse = 17, RULE_switchCaseStatement = 18, 
		RULE_switchCaseCondition = 19, RULE_switchCaseElseCondition = 20, RULE_regexrelop = 21, 
		RULE_condition = 22, RULE_simpleCondition = 23, RULE_conditionExpression = 24, 
		RULE_assignmentValue = 25, RULE_assignmentValueItem = 26, RULE_variableConditionPart = 27, 
		RULE_variableIndexPart = 28, RULE_variableTextPropertyPart = 29, RULE_assignSelector = 30, 
		RULE_assignProperty = 31, RULE_assignVariableProperty = 32, RULE_variableSelectorStart = 33, 
		RULE_variableSelectorPart = 34, RULE_variableSelector = 35, RULE_assignTextProperty = 36, 
		RULE_spreadSelector = 37, RULE_declareObjectStatement = 38, RULE_declareObject = 39, 
		RULE_variableOrObject = 40, RULE_forEach = 41, RULE_whileLoop = 42, RULE_functionCall = 43, 
		RULE_rhsval = 44, RULE_rhsid = 45, RULE_keyword = 46, RULE_shortIdentifier = 47, 
		RULE_multiIdent = 48, RULE_variableDeclaration = 49, RULE_typeDefinition = 50, 
		RULE_objectTypeDefinition = 51, RULE_declareObjectTypeProperty = 52, RULE_arrayTypeDefinition = 53, 
		RULE_enumTypeDefinition = 54, RULE_typeDeclaration = 55, RULE_typeNameDeclaration = 56, 
		RULE_filterModifier = 57, RULE_conditionModifier = 58, RULE_mapModifier = 59, 
		RULE_genericConditionModifier = 60, RULE_modifier = 61, RULE_variableWithModifier = 62, 
		RULE_argumentValue = 63, RULE_argumentItem = 64, RULE_arguments = 65, 
		RULE_rightSideValue = 66, RULE_arrayArgument = 67, RULE_array = 68, RULE_expressionInterpolate = 69, 
		RULE_mathInterpolate = 70, RULE_funcCallInterpolate = 71, RULE_simpleInterpolateVariable = 72, 
		RULE_interpolateText = 73, RULE_interpolate = 74, RULE_literal = 75, RULE_regexString = 76, 
		RULE_math = 77, RULE_mathExpresion = 78, RULE_mathValue = 79;
	private static String[] makeRuleNames() {
		return new String[] {
			"spec", "importDeclaration", "annotationParameter", "annotationArguments", 
			"annotation", "functionDeclaration", "functionArguments", "variableWithType", 
			"returnCall", "functionStatements", "functionStatement", "statements", 
			"statement", "relop", "ifStatement", "elseClause", "inlineIf", "inlineElse", 
			"switchCaseStatement", "switchCaseCondition", "switchCaseElseCondition", 
			"regexrelop", "condition", "simpleCondition", "conditionExpression", 
			"assignmentValue", "assignmentValueItem", "variableConditionPart", "variableIndexPart", 
			"variableTextPropertyPart", "assignSelector", "assignProperty", "assignVariableProperty", 
			"variableSelectorStart", "variableSelectorPart", "variableSelector", 
			"assignTextProperty", "spreadSelector", "declareObjectStatement", "declareObject", 
			"variableOrObject", "forEach", "whileLoop", "functionCall", "rhsval", 
			"rhsid", "keyword", "shortIdentifier", "multiIdent", "variableDeclaration", 
			"typeDefinition", "objectTypeDefinition", "declareObjectTypeProperty", 
			"arrayTypeDefinition", "enumTypeDefinition", "typeDeclaration", "typeNameDeclaration", 
			"filterModifier", "conditionModifier", "mapModifier", "genericConditionModifier", 
			"modifier", "variableWithModifier", "argumentValue", "argumentItem", 
			"arguments", "rightSideValue", "arrayArgument", "array", "expressionInterpolate", 
			"mathInterpolate", "funcCallInterpolate", "simpleInterpolateVariable", 
			"interpolateText", "interpolate", "literal", "regexString", "math", "mathExpresion", 
			"mathValue"
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

	@Override
	public String getGrammarFileName() { return "IslParser.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public IslParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class SpecContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(IslParser.EOF, 0); }
		public StatementsContext statements() {
			return getRuleContext(StatementsContext.class,0);
		}
		public List<ImportDeclarationContext> importDeclaration() {
			return getRuleContexts(ImportDeclarationContext.class);
		}
		public ImportDeclarationContext importDeclaration(int i) {
			return getRuleContext(ImportDeclarationContext.class,i);
		}
		public List<TypeDeclarationContext> typeDeclaration() {
			return getRuleContexts(TypeDeclarationContext.class);
		}
		public TypeDeclarationContext typeDeclaration(int i) {
			return getRuleContext(TypeDeclarationContext.class,i);
		}
		public List<FunctionDeclarationContext> functionDeclaration() {
			return getRuleContexts(FunctionDeclarationContext.class);
		}
		public FunctionDeclarationContext functionDeclaration(int i) {
			return getRuleContext(FunctionDeclarationContext.class,i);
		}
		public SpecContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_spec; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterSpec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitSpec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitSpec(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SpecContext spec() throws RecognitionException {
		SpecContext _localctx = new SpecContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_spec);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(179);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				{
				{
				setState(163);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==IMPORT) {
					{
					{
					setState(160);
					importDeclaration();
					}
					}
					setState(165);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(169);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==DECLARETYPE) {
					{
					{
					setState(166);
					typeDeclaration();
					}
					}
					setState(171);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(175);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << FUN) | (1L << MODIFIER_FUN) | (1L << CACHE) | (1L << AT))) != 0)) {
					{
					{
					setState(172);
					functionDeclaration();
					}
					}
					setState(177);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				}
				break;
			case 2:
				{
				setState(178);
				statements();
				}
				break;
			}
			setState(181);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ImportDeclarationContext extends ParserRuleContext {
		public TerminalNode IMPORT() { return getToken(IslParser.IMPORT, 0); }
		public TerminalNode ID() { return getToken(IslParser.ID, 0); }
		public TerminalNode FROM() { return getToken(IslParser.FROM, 0); }
		public TerminalNode QUOTEDSTRING() { return getToken(IslParser.QUOTEDSTRING, 0); }
		public TerminalNode SEMICOLON() { return getToken(IslParser.SEMICOLON, 0); }
		public ImportDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_importDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterImportDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitImportDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitImportDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ImportDeclarationContext importDeclaration() throws RecognitionException {
		ImportDeclarationContext _localctx = new ImportDeclarationContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_importDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(183);
			match(IMPORT);
			setState(184);
			match(ID);
			setState(185);
			match(FROM);
			setState(186);
			match(QUOTEDSTRING);
			setState(187);
			match(SEMICOLON);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AnnotationParameterContext extends ParserRuleContext {
		public DeclareObjectContext declareObject() {
			return getRuleContext(DeclareObjectContext.class,0);
		}
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public ArrayContext array() {
			return getRuleContext(ArrayContext.class,0);
		}
		public AnnotationParameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationParameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterAnnotationParameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitAnnotationParameter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitAnnotationParameter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationParameterContext annotationParameter() throws RecognitionException {
		AnnotationParameterContext _localctx = new AnnotationParameterContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_annotationParameter);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(192);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case CURLYOPEN:
				{
				setState(189);
				declareObject();
				}
				break;
			case BOOL:
			case NUM:
			case QUOTEDSTRING:
				{
				setState(190);
				literal();
				}
				break;
			case SQUAREOPEN:
				{
				setState(191);
				array();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AnnotationArgumentsContext extends ParserRuleContext {
		public TerminalNode OPAREN() { return getToken(IslParser.OPAREN, 0); }
		public TerminalNode CPAREN() { return getToken(IslParser.CPAREN, 0); }
		public List<AnnotationParameterContext> annotationParameter() {
			return getRuleContexts(AnnotationParameterContext.class);
		}
		public AnnotationParameterContext annotationParameter(int i) {
			return getRuleContext(AnnotationParameterContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(IslParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(IslParser.COMMA, i);
		}
		public AnnotationArgumentsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationArguments; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterAnnotationArguments(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitAnnotationArguments(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitAnnotationArguments(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationArgumentsContext annotationArguments() throws RecognitionException {
		AnnotationArgumentsContext _localctx = new AnnotationArgumentsContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_annotationArguments);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(194);
			match(OPAREN);
			setState(203);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (SQUAREOPEN - 58)) | (1L << (CURLYOPEN - 58)) | (1L << (BOOL - 58)) | (1L << (NUM - 58)) | (1L << (QUOTEDSTRING - 58)))) != 0)) {
				{
				setState(195);
				annotationParameter();
				setState(200);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(196);
					match(COMMA);
					setState(197);
					annotationParameter();
					}
					}
					setState(202);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(205);
			match(CPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AnnotationContext extends ParserRuleContext {
		public TerminalNode AT() { return getToken(IslParser.AT, 0); }
		public TerminalNode ID() { return getToken(IslParser.ID, 0); }
		public AnnotationArgumentsContext annotationArguments() {
			return getRuleContext(AnnotationArgumentsContext.class,0);
		}
		public AnnotationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterAnnotation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitAnnotation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitAnnotation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationContext annotation() throws RecognitionException {
		AnnotationContext _localctx = new AnnotationContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_annotation);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(207);
			match(AT);
			setState(208);
			match(ID);
			setState(210);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OPAREN) {
				{
				setState(209);
				annotationArguments();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FunctionDeclarationContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(IslParser.ID, 0); }
		public FunctionArgumentsContext functionArguments() {
			return getRuleContext(FunctionArgumentsContext.class,0);
		}
		public TerminalNode CURLYOPEN() { return getToken(IslParser.CURLYOPEN, 0); }
		public FunctionStatementsContext functionStatements() {
			return getRuleContext(FunctionStatementsContext.class,0);
		}
		public TerminalNode CURLYCLOSE() { return getToken(IslParser.CURLYCLOSE, 0); }
		public TerminalNode MODIFIER_FUN() { return getToken(IslParser.MODIFIER_FUN, 0); }
		public TerminalNode COLON() { return getToken(IslParser.COLON, 0); }
		public TypeDefinitionContext typeDefinition() {
			return getRuleContext(TypeDefinitionContext.class,0);
		}
		public TerminalNode FUN() { return getToken(IslParser.FUN, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public TerminalNode CACHE() { return getToken(IslParser.CACHE, 0); }
		public FunctionDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterFunctionDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitFunctionDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitFunctionDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionDeclarationContext functionDeclaration() throws RecognitionException {
		FunctionDeclarationContext _localctx = new FunctionDeclarationContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_functionDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(225);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case FUN:
			case CACHE:
			case AT:
				{
				{
				setState(221);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
				case 1:
					{
					setState(215);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==AT) {
						{
						{
						setState(212);
						annotation();
						}
						}
						setState(217);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
					break;
				case 2:
					{
					setState(219);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==CACHE) {
						{
						setState(218);
						match(CACHE);
						}
					}

					}
					break;
				}
				setState(223);
				match(FUN);
				}
				}
				break;
			case MODIFIER_FUN:
				{
				setState(224);
				match(MODIFIER_FUN);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(227);
			match(ID);
			setState(228);
			functionArguments();
			setState(231);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COLON) {
				{
				setState(229);
				match(COLON);
				setState(230);
				typeDefinition();
				}
			}

			setState(233);
			match(CURLYOPEN);
			setState(234);
			functionStatements();
			setState(235);
			match(CURLYCLOSE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FunctionArgumentsContext extends ParserRuleContext {
		public TerminalNode OPAREN() { return getToken(IslParser.OPAREN, 0); }
		public TerminalNode CPAREN() { return getToken(IslParser.CPAREN, 0); }
		public List<VariableWithTypeContext> variableWithType() {
			return getRuleContexts(VariableWithTypeContext.class);
		}
		public VariableWithTypeContext variableWithType(int i) {
			return getRuleContext(VariableWithTypeContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(IslParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(IslParser.COMMA, i);
		}
		public FunctionArgumentsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionArguments; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterFunctionArguments(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitFunctionArguments(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitFunctionArguments(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionArgumentsContext functionArguments() throws RecognitionException {
		FunctionArgumentsContext _localctx = new FunctionArgumentsContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_functionArguments);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(237);
			match(OPAREN);
			setState(246);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DOLLAR) {
				{
				setState(238);
				variableWithType();
				setState(243);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(239);
					match(COMMA);
					setState(240);
					variableWithType();
					}
					}
					setState(245);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(248);
			match(CPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VariableWithTypeContext extends ParserRuleContext {
		public VariableDeclarationContext variableDeclaration() {
			return getRuleContext(VariableDeclarationContext.class,0);
		}
		public TerminalNode COLON() { return getToken(IslParser.COLON, 0); }
		public TypeDefinitionContext typeDefinition() {
			return getRuleContext(TypeDefinitionContext.class,0);
		}
		public VariableWithTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableWithType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterVariableWithType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitVariableWithType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitVariableWithType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableWithTypeContext variableWithType() throws RecognitionException {
		VariableWithTypeContext _localctx = new VariableWithTypeContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_variableWithType);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(250);
			variableDeclaration();
			setState(253);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COLON) {
				{
				setState(251);
				match(COLON);
				setState(252);
				typeDefinition();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ReturnCallContext extends ParserRuleContext {
		public TerminalNode RETURN() { return getToken(IslParser.RETURN, 0); }
		public AssignmentValueContext assignmentValue() {
			return getRuleContext(AssignmentValueContext.class,0);
		}
		public ReturnCallContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_returnCall; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterReturnCall(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitReturnCall(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitReturnCall(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ReturnCallContext returnCall() throws RecognitionException {
		ReturnCallContext _localctx = new ReturnCallContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_returnCall);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(255);
			match(RETURN);
			setState(256);
			assignmentValue(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FunctionStatementsContext extends ParserRuleContext {
		public List<FunctionStatementContext> functionStatement() {
			return getRuleContexts(FunctionStatementContext.class);
		}
		public FunctionStatementContext functionStatement(int i) {
			return getRuleContext(FunctionStatementContext.class,i);
		}
		public FunctionStatementsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionStatements; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterFunctionStatements(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitFunctionStatements(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitFunctionStatements(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionStatementsContext functionStatements() throws RecognitionException {
		FunctionStatementsContext _localctx = new FunctionStatementsContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_functionStatements);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(259); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(258);
				functionStatement();
				}
				}
				setState(261); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << IFCODE) | (1L << SWITCH) | (1L << FILTER) | (1L << MAP) | (1L << IN) | (1L << MATCHES) | (1L << PARALLEL) | (1L << FOR) | (1L << WHILELOOP) | (1L << RETURN) | (1L << IMPORT) | (1L << DECLARETYPE) | (1L << AS) | (1L << FROM) | (1L << OPEN_BACKTICK) | (1L << LOP) | (1L << DOLLAR) | (1L << AT))) != 0) || _la==ID );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FunctionStatementContext extends ParserRuleContext {
		public AssignVariablePropertyContext assignVariableProperty() {
			return getRuleContext(AssignVariablePropertyContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(IslParser.SEMICOLON, 0); }
		public AssignPropertyContext assignProperty() {
			return getRuleContext(AssignPropertyContext.class,0);
		}
		public IfStatementContext ifStatement() {
			return getRuleContext(IfStatementContext.class,0);
		}
		public SwitchCaseStatementContext switchCaseStatement() {
			return getRuleContext(SwitchCaseStatementContext.class,0);
		}
		public ForEachContext forEach() {
			return getRuleContext(ForEachContext.class,0);
		}
		public WhileLoopContext whileLoop() {
			return getRuleContext(WhileLoopContext.class,0);
		}
		public FunctionCallContext functionCall() {
			return getRuleContext(FunctionCallContext.class,0);
		}
		public ReturnCallContext returnCall() {
			return getRuleContext(ReturnCallContext.class,0);
		}
		public FunctionStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterFunctionStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitFunctionStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitFunctionStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionStatementContext functionStatement() throws RecognitionException {
		FunctionStatementContext _localctx = new FunctionStatementContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_functionStatement);
		int _la;
		try {
			setState(283);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,21,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(263);
				assignVariableProperty();
				setState(265);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==SEMICOLON) {
					{
					setState(264);
					match(SEMICOLON);
					}
				}

				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(267);
				assignProperty();
				setState(269);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==SEMICOLON) {
					{
					setState(268);
					match(SEMICOLON);
					}
				}

				}
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(271);
				ifStatement();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(272);
				switchCaseStatement();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(273);
				forEach();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(274);
				whileLoop();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				{
				setState(275);
				functionCall();
				setState(277);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==SEMICOLON) {
					{
					setState(276);
					match(SEMICOLON);
					}
				}

				}
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				{
				setState(279);
				returnCall();
				setState(281);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==SEMICOLON) {
					{
					setState(280);
					match(SEMICOLON);
					}
				}

				}
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class StatementsContext extends ParserRuleContext {
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public StatementsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_statements; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterStatements(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitStatements(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitStatements(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StatementsContext statements() throws RecognitionException {
		StatementsContext _localctx = new StatementsContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_statements);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(286); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(285);
					statement();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(288); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class StatementContext extends ParserRuleContext {
		public AssignVariablePropertyContext assignVariableProperty() {
			return getRuleContext(AssignVariablePropertyContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(IslParser.SEMICOLON, 0); }
		public AssignPropertyContext assignProperty() {
			return getRuleContext(AssignPropertyContext.class,0);
		}
		public IfStatementContext ifStatement() {
			return getRuleContext(IfStatementContext.class,0);
		}
		public SwitchCaseStatementContext switchCaseStatement() {
			return getRuleContext(SwitchCaseStatementContext.class,0);
		}
		public ForEachContext forEach() {
			return getRuleContext(ForEachContext.class,0);
		}
		public WhileLoopContext whileLoop() {
			return getRuleContext(WhileLoopContext.class,0);
		}
		public FunctionCallContext functionCall() {
			return getRuleContext(FunctionCallContext.class,0);
		}
		public ReturnCallContext returnCall() {
			return getRuleContext(ReturnCallContext.class,0);
		}
		public StatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StatementContext statement() throws RecognitionException {
		StatementContext _localctx = new StatementContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_statement);
		try {
			setState(310);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,27,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(290);
				assignVariableProperty();
				setState(292);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,23,_ctx) ) {
				case 1:
					{
					setState(291);
					match(SEMICOLON);
					}
					break;
				}
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(294);
				assignProperty();
				setState(296);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,24,_ctx) ) {
				case 1:
					{
					setState(295);
					match(SEMICOLON);
					}
					break;
				}
				}
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(298);
				ifStatement();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(299);
				switchCaseStatement();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(300);
				forEach();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(301);
				whileLoop();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				{
				setState(302);
				functionCall();
				setState(304);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,25,_ctx) ) {
				case 1:
					{
					setState(303);
					match(SEMICOLON);
					}
					break;
				}
				}
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				{
				setState(306);
				returnCall();
				setState(308);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,26,_ctx) ) {
				case 1:
					{
					setState(307);
					match(SEMICOLON);
					}
					break;
				}
				}
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RelopContext extends ParserRuleContext {
		public TerminalNode EQUAL_EQUAL() { return getToken(IslParser.EQUAL_EQUAL, 0); }
		public TerminalNode NOT_EQUAL() { return getToken(IslParser.NOT_EQUAL, 0); }
		public TerminalNode LESS_OR_EQUAL() { return getToken(IslParser.LESS_OR_EQUAL, 0); }
		public TerminalNode GREATER_OR_EQUAL() { return getToken(IslParser.GREATER_OR_EQUAL, 0); }
		public TerminalNode GREATER() { return getToken(IslParser.GREATER, 0); }
		public TerminalNode LESS() { return getToken(IslParser.LESS, 0); }
		public TerminalNode CONTAINS() { return getToken(IslParser.CONTAINS, 0); }
		public TerminalNode NOT_CONTAINS() { return getToken(IslParser.NOT_CONTAINS, 0); }
		public TerminalNode STARTS_WITH() { return getToken(IslParser.STARTS_WITH, 0); }
		public TerminalNode NOT_STARTS_WITH() { return getToken(IslParser.NOT_STARTS_WITH, 0); }
		public TerminalNode ENDS_WITH() { return getToken(IslParser.ENDS_WITH, 0); }
		public TerminalNode NOT_ENDS_WITH() { return getToken(IslParser.NOT_ENDS_WITH, 0); }
		public TerminalNode IN() { return getToken(IslParser.IN, 0); }
		public TerminalNode NOT_IN() { return getToken(IslParser.NOT_IN, 0); }
		public TerminalNode IS() { return getToken(IslParser.IS, 0); }
		public TerminalNode NOT_IS() { return getToken(IslParser.NOT_IS, 0); }
		public RelopContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_relop; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterRelop(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitRelop(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitRelop(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RelopContext relop() throws RecognitionException {
		RelopContext _localctx = new RelopContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_relop);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(312);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << EQUAL_EQUAL) | (1L << NOT_EQUAL) | (1L << LESS_OR_EQUAL) | (1L << GREATER_OR_EQUAL) | (1L << GREATER) | (1L << LESS) | (1L << CONTAINS) | (1L << NOT_CONTAINS) | (1L << STARTS_WITH) | (1L << NOT_STARTS_WITH) | (1L << ENDS_WITH) | (1L << NOT_ENDS_WITH) | (1L << IN) | (1L << NOT_IN) | (1L << IS) | (1L << NOT_IS))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IfStatementContext extends ParserRuleContext {
		public StatementsContext trueStatements;
		public TerminalNode IFCODE() { return getToken(IslParser.IFCODE, 0); }
		public ConditionContext condition() {
			return getRuleContext(ConditionContext.class,0);
		}
		public TerminalNode ENDIFCODE() { return getToken(IslParser.ENDIFCODE, 0); }
		public StatementsContext statements() {
			return getRuleContext(StatementsContext.class,0);
		}
		public ElseClauseContext elseClause() {
			return getRuleContext(ElseClauseContext.class,0);
		}
		public IfStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ifStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterIfStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitIfStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitIfStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IfStatementContext ifStatement() throws RecognitionException {
		IfStatementContext _localctx = new IfStatementContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_ifStatement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(314);
			match(IFCODE);
			setState(315);
			condition();
			setState(316);
			((IfStatementContext)_localctx).trueStatements = statements();
			setState(318);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ELSE) {
				{
				setState(317);
				elseClause();
				}
			}

			setState(320);
			match(ENDIFCODE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ElseClauseContext extends ParserRuleContext {
		public StatementsContext falseStatements;
		public TerminalNode ELSE() { return getToken(IslParser.ELSE, 0); }
		public StatementsContext statements() {
			return getRuleContext(StatementsContext.class,0);
		}
		public ElseClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_elseClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterElseClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitElseClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitElseClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ElseClauseContext elseClause() throws RecognitionException {
		ElseClauseContext _localctx = new ElseClauseContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_elseClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(322);
			match(ELSE);
			setState(323);
			((ElseClauseContext)_localctx).falseStatements = statements();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InlineIfContext extends ParserRuleContext {
		public TerminalNode IFCODE() { return getToken(IslParser.IFCODE, 0); }
		public ConditionContext condition() {
			return getRuleContext(ConditionContext.class,0);
		}
		public RhsvalContext rhsval() {
			return getRuleContext(RhsvalContext.class,0);
		}
		public DeclareObjectContext declareObject() {
			return getRuleContext(DeclareObjectContext.class,0);
		}
		public InlineElseContext inlineElse() {
			return getRuleContext(InlineElseContext.class,0);
		}
		public TerminalNode ENDIFCODE() { return getToken(IslParser.ENDIFCODE, 0); }
		public InlineIfContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inlineIf; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterInlineIf(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitInlineIf(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitInlineIf(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InlineIfContext inlineIf() throws RecognitionException {
		InlineIfContext _localctx = new InlineIfContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_inlineIf);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(325);
			match(IFCODE);
			setState(326);
			condition();
			setState(329);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case OPEN_BACKTICK:
			case SEMICOLON:
			case DOLLAR:
			case AT:
			case SQUAREOPEN:
			case BOOL:
			case NUM:
			case QUOTEDSTRING:
				{
				setState(327);
				rhsval();
				}
				break;
			case CURLYOPEN:
				{
				setState(328);
				declareObject();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(332);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,30,_ctx) ) {
			case 1:
				{
				setState(331);
				inlineElse();
				}
				break;
			}
			setState(335);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,31,_ctx) ) {
			case 1:
				{
				setState(334);
				match(ENDIFCODE);
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InlineElseContext extends ParserRuleContext {
		public TerminalNode ELSE() { return getToken(IslParser.ELSE, 0); }
		public RhsvalContext rhsval() {
			return getRuleContext(RhsvalContext.class,0);
		}
		public DeclareObjectContext declareObject() {
			return getRuleContext(DeclareObjectContext.class,0);
		}
		public InlineElseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inlineElse; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterInlineElse(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitInlineElse(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitInlineElse(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InlineElseContext inlineElse() throws RecognitionException {
		InlineElseContext _localctx = new InlineElseContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_inlineElse);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(337);
			match(ELSE);
			setState(340);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case OPEN_BACKTICK:
			case SEMICOLON:
			case DOLLAR:
			case AT:
			case SQUAREOPEN:
			case BOOL:
			case NUM:
			case QUOTEDSTRING:
				{
				setState(338);
				rhsval();
				}
				break;
			case CURLYOPEN:
				{
				setState(339);
				declareObject();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SwitchCaseStatementContext extends ParserRuleContext {
		public TerminalNode SWITCH() { return getToken(IslParser.SWITCH, 0); }
		public TerminalNode OPAREN() { return getToken(IslParser.OPAREN, 0); }
		public RhsidContext rhsid() {
			return getRuleContext(RhsidContext.class,0);
		}
		public TerminalNode CPAREN() { return getToken(IslParser.CPAREN, 0); }
		public TerminalNode ENDSWITCH() { return getToken(IslParser.ENDSWITCH, 0); }
		public List<SwitchCaseConditionContext> switchCaseCondition() {
			return getRuleContexts(SwitchCaseConditionContext.class);
		}
		public SwitchCaseConditionContext switchCaseCondition(int i) {
			return getRuleContext(SwitchCaseConditionContext.class,i);
		}
		public SwitchCaseElseConditionContext switchCaseElseCondition() {
			return getRuleContext(SwitchCaseElseConditionContext.class,0);
		}
		public SwitchCaseStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_switchCaseStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterSwitchCaseStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitSwitchCaseStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitSwitchCaseStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SwitchCaseStatementContext switchCaseStatement() throws RecognitionException {
		SwitchCaseStatementContext _localctx = new SwitchCaseStatementContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_switchCaseStatement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(342);
			match(SWITCH);
			setState(343);
			match(OPAREN);
			setState(344);
			rhsid();
			setState(345);
			match(CPAREN);
			setState(349);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 9)) & ~0x3f) == 0 && ((1L << (_la - 9)) & ((1L << (EQUAL_EQUAL - 9)) | (1L << (NOT_EQUAL - 9)) | (1L << (LESS_OR_EQUAL - 9)) | (1L << (GREATER_OR_EQUAL - 9)) | (1L << (GREATER - 9)) | (1L << (LESS - 9)) | (1L << (CONTAINS - 9)) | (1L << (NOT_CONTAINS - 9)) | (1L << (STARTS_WITH - 9)) | (1L << (NOT_STARTS_WITH - 9)) | (1L << (ENDS_WITH - 9)) | (1L << (NOT_ENDS_WITH - 9)) | (1L << (IN - 9)) | (1L << (NOT_IN - 9)) | (1L << (IS - 9)) | (1L << (NOT_IS - 9)) | (1L << (DOLLAR - 9)) | (1L << (SQUAREOPEN - 9)) | (1L << (MATH_DIV - 9)) | (1L << (BOOL - 9)) | (1L << (NUM - 9)) | (1L << (QUOTEDSTRING - 9)))) != 0)) {
				{
				{
				setState(346);
				switchCaseCondition();
				}
				}
				setState(351);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(353);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ELSE) {
				{
				setState(352);
				switchCaseElseCondition();
				}
			}

			setState(355);
			match(ENDSWITCH);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SwitchCaseConditionContext extends ParserRuleContext {
		public RhsidContext resultVariable;
		public TerminalNode ARROW() { return getToken(IslParser.ARROW, 0); }
		public TerminalNode SEMICOLON() { return getToken(IslParser.SEMICOLON, 0); }
		public RegexStringContext regexString() {
			return getRuleContext(RegexStringContext.class,0);
		}
		public FunctionCallContext functionCall() {
			return getRuleContext(FunctionCallContext.class,0);
		}
		public StatementsContext statements() {
			return getRuleContext(StatementsContext.class,0);
		}
		public DeclareObjectContext declareObject() {
			return getRuleContext(DeclareObjectContext.class,0);
		}
		public RhsidContext rhsid() {
			return getRuleContext(RhsidContext.class,0);
		}
		public VariableSelectorContext variableSelector() {
			return getRuleContext(VariableSelectorContext.class,0);
		}
		public ArrayContext array() {
			return getRuleContext(ArrayContext.class,0);
		}
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public RelopContext relop() {
			return getRuleContext(RelopContext.class,0);
		}
		public SwitchCaseConditionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_switchCaseCondition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterSwitchCaseCondition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitSwitchCaseCondition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitSwitchCaseCondition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SwitchCaseConditionContext switchCaseCondition() throws RecognitionException {
		SwitchCaseConditionContext _localctx = new SwitchCaseConditionContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_switchCaseCondition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(366);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case EQUAL_EQUAL:
			case NOT_EQUAL:
			case LESS_OR_EQUAL:
			case GREATER_OR_EQUAL:
			case GREATER:
			case LESS:
			case CONTAINS:
			case NOT_CONTAINS:
			case STARTS_WITH:
			case NOT_STARTS_WITH:
			case ENDS_WITH:
			case NOT_ENDS_WITH:
			case IN:
			case NOT_IN:
			case IS:
			case NOT_IS:
			case DOLLAR:
			case SQUAREOPEN:
			case BOOL:
			case NUM:
			case QUOTEDSTRING:
				{
				{
				setState(358);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << EQUAL_EQUAL) | (1L << NOT_EQUAL) | (1L << LESS_OR_EQUAL) | (1L << GREATER_OR_EQUAL) | (1L << GREATER) | (1L << LESS) | (1L << CONTAINS) | (1L << NOT_CONTAINS) | (1L << STARTS_WITH) | (1L << NOT_STARTS_WITH) | (1L << ENDS_WITH) | (1L << NOT_ENDS_WITH) | (1L << IN) | (1L << NOT_IN) | (1L << IS) | (1L << NOT_IS))) != 0)) {
					{
					setState(357);
					relop();
					}
				}

				setState(363);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case DOLLAR:
					{
					setState(360);
					variableSelector();
					}
					break;
				case SQUAREOPEN:
					{
					setState(361);
					array();
					}
					break;
				case BOOL:
				case NUM:
				case QUOTEDSTRING:
					{
					setState(362);
					literal();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				}
				break;
			case MATH_DIV:
				{
				setState(365);
				regexString();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(368);
			match(ARROW);
			setState(373);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,38,_ctx) ) {
			case 1:
				{
				setState(369);
				functionCall();
				}
				break;
			case 2:
				{
				setState(370);
				statements();
				}
				break;
			case 3:
				{
				setState(371);
				((SwitchCaseConditionContext)_localctx).resultVariable = rhsid();
				}
				break;
			case 4:
				{
				setState(372);
				declareObject();
				}
				break;
			}
			setState(375);
			match(SEMICOLON);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SwitchCaseElseConditionContext extends ParserRuleContext {
		public TerminalNode ELSE() { return getToken(IslParser.ELSE, 0); }
		public TerminalNode ARROW() { return getToken(IslParser.ARROW, 0); }
		public TerminalNode SEMICOLON() { return getToken(IslParser.SEMICOLON, 0); }
		public FunctionCallContext functionCall() {
			return getRuleContext(FunctionCallContext.class,0);
		}
		public StatementsContext statements() {
			return getRuleContext(StatementsContext.class,0);
		}
		public RhsidContext rhsid() {
			return getRuleContext(RhsidContext.class,0);
		}
		public DeclareObjectContext declareObject() {
			return getRuleContext(DeclareObjectContext.class,0);
		}
		public SwitchCaseElseConditionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_switchCaseElseCondition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterSwitchCaseElseCondition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitSwitchCaseElseCondition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitSwitchCaseElseCondition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SwitchCaseElseConditionContext switchCaseElseCondition() throws RecognitionException {
		SwitchCaseElseConditionContext _localctx = new SwitchCaseElseConditionContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_switchCaseElseCondition);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(377);
			match(ELSE);
			setState(378);
			match(ARROW);
			setState(383);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,39,_ctx) ) {
			case 1:
				{
				setState(379);
				functionCall();
				}
				break;
			case 2:
				{
				setState(380);
				statements();
				}
				break;
			case 3:
				{
				setState(381);
				rhsid();
				}
				break;
			case 4:
				{
				setState(382);
				declareObject();
				}
				break;
			}
			setState(385);
			match(SEMICOLON);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RegexrelopContext extends ParserRuleContext {
		public TerminalNode MATCHES() { return getToken(IslParser.MATCHES, 0); }
		public TerminalNode NOT_MATCHES() { return getToken(IslParser.NOT_MATCHES, 0); }
		public RegexrelopContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_regexrelop; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterRegexrelop(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitRegexrelop(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitRegexrelop(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RegexrelopContext regexrelop() throws RecognitionException {
		RegexrelopContext _localctx = new RegexrelopContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_regexrelop);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(387);
			_la = _input.LA(1);
			if ( !(_la==MATCHES || _la==NOT_MATCHES) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ConditionContext extends ParserRuleContext {
		public TerminalNode OPAREN() { return getToken(IslParser.OPAREN, 0); }
		public ConditionExpressionContext conditionExpression() {
			return getRuleContext(ConditionExpressionContext.class,0);
		}
		public TerminalNode CPAREN() { return getToken(IslParser.CPAREN, 0); }
		public List<TerminalNode> WS() { return getTokens(IslParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(IslParser.WS, i);
		}
		public ConditionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_condition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterCondition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitCondition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitCondition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConditionContext condition() throws RecognitionException {
		ConditionContext _localctx = new ConditionContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_condition);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(392);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==WS) {
				{
				{
				setState(389);
				match(WS);
				}
				}
				setState(394);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(395);
			match(OPAREN);
			setState(399);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==WS) {
				{
				{
				setState(396);
				match(WS);
				}
				}
				setState(401);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(402);
			conditionExpression(0);
			setState(406);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==WS) {
				{
				{
				setState(403);
				match(WS);
				}
				}
				setState(408);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(409);
			match(CPAREN);
			setState(413);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,43,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(410);
					match(WS);
					}
					} 
				}
				setState(415);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,43,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SimpleConditionContext extends ParserRuleContext {
		public RhsidContext singleLeft;
		public RhsidContext leftCondition;
		public RhsidContext rightCondition;
		public List<RhsidContext> rhsid() {
			return getRuleContexts(RhsidContext.class);
		}
		public RhsidContext rhsid(int i) {
			return getRuleContext(RhsidContext.class,i);
		}
		public TerminalNode BANG() { return getToken(IslParser.BANG, 0); }
		public RelopContext relop() {
			return getRuleContext(RelopContext.class,0);
		}
		public RegexrelopContext regexrelop() {
			return getRuleContext(RegexrelopContext.class,0);
		}
		public RegexStringContext regexString() {
			return getRuleContext(RegexStringContext.class,0);
		}
		public SimpleConditionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_simpleCondition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterSimpleCondition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitSimpleCondition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitSimpleCondition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SimpleConditionContext simpleCondition() throws RecognitionException {
		SimpleConditionContext _localctx = new SimpleConditionContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_simpleCondition);
		int _la;
		try {
			setState(428);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,45,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(417);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==BANG) {
					{
					setState(416);
					match(BANG);
					}
				}

				setState(419);
				((SimpleConditionContext)_localctx).singleLeft = rhsid();
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(420);
				((SimpleConditionContext)_localctx).leftCondition = rhsid();
				setState(421);
				relop();
				setState(422);
				((SimpleConditionContext)_localctx).rightCondition = rhsid();
				}
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				{
				setState(424);
				((SimpleConditionContext)_localctx).leftCondition = rhsid();
				setState(425);
				regexrelop();
				setState(426);
				regexString();
				}
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ConditionExpressionContext extends ParserRuleContext {
		public ConditionExpressionContext leftExpression;
		public ConditionExpressionContext rigthExpression;
		public TerminalNode OPAREN() { return getToken(IslParser.OPAREN, 0); }
		public List<ConditionExpressionContext> conditionExpression() {
			return getRuleContexts(ConditionExpressionContext.class);
		}
		public ConditionExpressionContext conditionExpression(int i) {
			return getRuleContext(ConditionExpressionContext.class,i);
		}
		public TerminalNode CPAREN() { return getToken(IslParser.CPAREN, 0); }
		public SimpleConditionContext simpleCondition() {
			return getRuleContext(SimpleConditionContext.class,0);
		}
		public TerminalNode LOP() { return getToken(IslParser.LOP, 0); }
		public ConditionExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_conditionExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterConditionExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitConditionExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitConditionExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConditionExpressionContext conditionExpression() throws RecognitionException {
		return conditionExpression(0);
	}

	private ConditionExpressionContext conditionExpression(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ConditionExpressionContext _localctx = new ConditionExpressionContext(_ctx, _parentState);
		ConditionExpressionContext _prevctx = _localctx;
		int _startState = 48;
		enterRecursionRule(_localctx, 48, RULE_conditionExpression, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(436);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case OPAREN:
				{
				setState(431);
				match(OPAREN);
				setState(432);
				conditionExpression(0);
				setState(433);
				match(CPAREN);
				}
				break;
			case OPEN_BACKTICK:
			case BANG:
			case DOLLAR:
			case AT:
			case SQUAREOPEN:
			case BOOL:
			case NUM:
			case QUOTEDSTRING:
				{
				setState(435);
				simpleCondition();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(443);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,47,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new ConditionExpressionContext(_parentctx, _parentState);
					_localctx.leftExpression = _prevctx;
					_localctx.leftExpression = _prevctx;
					pushNewRecursionContext(_localctx, _startState, RULE_conditionExpression);
					setState(438);
					if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
					setState(439);
					match(LOP);
					setState(440);
					((ConditionExpressionContext)_localctx).rigthExpression = conditionExpression(4);
					}
					} 
				}
				setState(445);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,47,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class AssignmentValueContext extends ParserRuleContext {
		public AssignmentValueItemContext assignmentValueItem() {
			return getRuleContext(AssignmentValueItemContext.class,0);
		}
		public List<AssignmentValueContext> assignmentValue() {
			return getRuleContexts(AssignmentValueContext.class);
		}
		public AssignmentValueContext assignmentValue(int i) {
			return getRuleContext(AssignmentValueContext.class,i);
		}
		public TerminalNode COALESCE() { return getToken(IslParser.COALESCE, 0); }
		public AssignmentValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assignmentValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterAssignmentValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitAssignmentValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitAssignmentValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AssignmentValueContext assignmentValue() throws RecognitionException {
		return assignmentValue(0);
	}

	private AssignmentValueContext assignmentValue(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		AssignmentValueContext _localctx = new AssignmentValueContext(_ctx, _parentState);
		AssignmentValueContext _prevctx = _localctx;
		int _startState = 50;
		enterRecursionRule(_localctx, 50, RULE_assignmentValue, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(447);
			assignmentValueItem();
			}
			_ctx.stop = _input.LT(-1);
			setState(454);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,48,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new AssignmentValueContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_assignmentValue);
					setState(449);
					if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
					setState(450);
					match(COALESCE);
					setState(451);
					assignmentValue(3);
					}
					} 
				}
				setState(456);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,48,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class AssignmentValueItemContext extends ParserRuleContext {
		public MathContext math() {
			return getRuleContext(MathContext.class,0);
		}
		public DeclareObjectContext declareObject() {
			return getRuleContext(DeclareObjectContext.class,0);
		}
		public InlineIfContext inlineIf() {
			return getRuleContext(InlineIfContext.class,0);
		}
		public ForEachContext forEach() {
			return getRuleContext(ForEachContext.class,0);
		}
		public WhileLoopContext whileLoop() {
			return getRuleContext(WhileLoopContext.class,0);
		}
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public ArrayContext array() {
			return getRuleContext(ArrayContext.class,0);
		}
		public InterpolateContext interpolate() {
			return getRuleContext(InterpolateContext.class,0);
		}
		public FunctionCallContext functionCall() {
			return getRuleContext(FunctionCallContext.class,0);
		}
		public SwitchCaseStatementContext switchCaseStatement() {
			return getRuleContext(SwitchCaseStatementContext.class,0);
		}
		public VariableSelectorContext variableSelector() {
			return getRuleContext(VariableSelectorContext.class,0);
		}
		public List<ModifierContext> modifier() {
			return getRuleContexts(ModifierContext.class);
		}
		public ModifierContext modifier(int i) {
			return getRuleContext(ModifierContext.class,i);
		}
		public AssignmentValueItemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assignmentValueItem; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterAssignmentValueItem(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitAssignmentValueItem(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitAssignmentValueItem(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AssignmentValueItemContext assignmentValueItem() throws RecognitionException {
		AssignmentValueItemContext _localctx = new AssignmentValueItemContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_assignmentValueItem);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(468);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case CURLYOPENOPEN:
				{
				setState(457);
				math();
				}
				break;
			case CURLYOPEN:
				{
				setState(458);
				declareObject();
				}
				break;
			case IFCODE:
				{
				setState(459);
				inlineIf();
				}
				break;
			case PARALLEL:
			case FOR:
				{
				setState(460);
				forEach();
				}
				break;
			case WHILELOOP:
				{
				setState(461);
				whileLoop();
				}
				break;
			case BOOL:
			case NUM:
			case QUOTEDSTRING:
				{
				setState(462);
				literal();
				}
				break;
			case SQUAREOPEN:
				{
				setState(463);
				array();
				}
				break;
			case OPEN_BACKTICK:
				{
				setState(464);
				interpolate();
				}
				break;
			case AT:
				{
				setState(465);
				functionCall();
				}
				break;
			case SWITCH:
				{
				setState(466);
				switchCaseStatement();
				}
				break;
			case DOLLAR:
				{
				setState(467);
				variableSelector();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(473);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,50,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(470);
					modifier();
					}
					} 
				}
				setState(475);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,50,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VariableConditionPartContext extends ParserRuleContext {
		public TerminalNode ARRAYCONDOPEN() { return getToken(IslParser.ARRAYCONDOPEN, 0); }
		public ConditionExpressionContext conditionExpression() {
			return getRuleContext(ConditionExpressionContext.class,0);
		}
		public TerminalNode ARRAYCONDCLOSE() { return getToken(IslParser.ARRAYCONDCLOSE, 0); }
		public List<TerminalNode> WS() { return getTokens(IslParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(IslParser.WS, i);
		}
		public VariableConditionPartContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableConditionPart; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterVariableConditionPart(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitVariableConditionPart(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitVariableConditionPart(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableConditionPartContext variableConditionPart() throws RecognitionException {
		VariableConditionPartContext _localctx = new VariableConditionPartContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_variableConditionPart);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(476);
			match(ARRAYCONDOPEN);
			setState(480);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==WS) {
				{
				{
				setState(477);
				match(WS);
				}
				}
				setState(482);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(483);
			conditionExpression(0);
			setState(487);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==WS) {
				{
				{
				setState(484);
				match(WS);
				}
				}
				setState(489);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(490);
			match(ARRAYCONDCLOSE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VariableIndexPartContext extends ParserRuleContext {
		public TerminalNode SQUAREOPEN() { return getToken(IslParser.SQUAREOPEN, 0); }
		public TerminalNode NUM() { return getToken(IslParser.NUM, 0); }
		public TerminalNode SQUARECLOSE() { return getToken(IslParser.SQUARECLOSE, 0); }
		public VariableIndexPartContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableIndexPart; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterVariableIndexPart(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitVariableIndexPart(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitVariableIndexPart(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableIndexPartContext variableIndexPart() throws RecognitionException {
		VariableIndexPartContext _localctx = new VariableIndexPartContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_variableIndexPart);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(492);
			match(SQUAREOPEN);
			setState(493);
			match(NUM);
			setState(494);
			match(SQUARECLOSE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VariableTextPropertyPartContext extends ParserRuleContext {
		public TerminalNode SQUAREOPEN() { return getToken(IslParser.SQUAREOPEN, 0); }
		public TerminalNode QUOTEDSTRING() { return getToken(IslParser.QUOTEDSTRING, 0); }
		public TerminalNode SQUARECLOSE() { return getToken(IslParser.SQUARECLOSE, 0); }
		public List<TerminalNode> WS() { return getTokens(IslParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(IslParser.WS, i);
		}
		public VariableTextPropertyPartContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableTextPropertyPart; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterVariableTextPropertyPart(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitVariableTextPropertyPart(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitVariableTextPropertyPart(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableTextPropertyPartContext variableTextPropertyPart() throws RecognitionException {
		VariableTextPropertyPartContext _localctx = new VariableTextPropertyPartContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_variableTextPropertyPart);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(496);
			match(SQUAREOPEN);
			setState(500);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==WS) {
				{
				{
				setState(497);
				match(WS);
				}
				}
				setState(502);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(503);
			match(QUOTEDSTRING);
			setState(507);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==WS) {
				{
				{
				setState(504);
				match(WS);
				}
				}
				setState(509);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(510);
			match(SQUARECLOSE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AssignSelectorContext extends ParserRuleContext {
		public List<ShortIdentifierContext> shortIdentifier() {
			return getRuleContexts(ShortIdentifierContext.class);
		}
		public ShortIdentifierContext shortIdentifier(int i) {
			return getRuleContext(ShortIdentifierContext.class,i);
		}
		public List<VariableIndexPartContext> variableIndexPart() {
			return getRuleContexts(VariableIndexPartContext.class);
		}
		public VariableIndexPartContext variableIndexPart(int i) {
			return getRuleContext(VariableIndexPartContext.class,i);
		}
		public List<TerminalNode> DOT() { return getTokens(IslParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(IslParser.DOT, i);
		}
		public List<VariableTextPropertyPartContext> variableTextPropertyPart() {
			return getRuleContexts(VariableTextPropertyPartContext.class);
		}
		public VariableTextPropertyPartContext variableTextPropertyPart(int i) {
			return getRuleContext(VariableTextPropertyPartContext.class,i);
		}
		public AssignSelectorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assignSelector; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterAssignSelector(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitAssignSelector(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitAssignSelector(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AssignSelectorContext assignSelector() throws RecognitionException {
		AssignSelectorContext _localctx = new AssignSelectorContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_assignSelector);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(512);
			shortIdentifier();
			setState(514);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SQUAREOPEN) {
				{
				setState(513);
				variableIndexPart();
				}
			}

			setState(526);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOT) {
				{
				{
				setState(516);
				match(DOT);
				setState(519);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case SQUAREOPEN:
					{
					setState(517);
					variableTextPropertyPart();
					}
					break;
				case FILTER:
				case MAP:
				case IN:
				case MATCHES:
				case RETURN:
				case IMPORT:
				case DECLARETYPE:
				case AS:
				case FROM:
				case LOP:
				case ID:
					{
					setState(518);
					shortIdentifier();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(522);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==SQUAREOPEN) {
					{
					setState(521);
					variableIndexPart();
					}
				}

				}
				}
				setState(528);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AssignPropertyContext extends ParserRuleContext {
		public TypeNameDeclarationContext objectType;
		public AssignmentValueContext assignmentValue() {
			return getRuleContext(AssignmentValueContext.class,0);
		}
		public AssignSelectorContext assignSelector() {
			return getRuleContext(AssignSelectorContext.class,0);
		}
		public InterpolateContext interpolate() {
			return getRuleContext(InterpolateContext.class,0);
		}
		public TerminalNode EQUAL() { return getToken(IslParser.EQUAL, 0); }
		public TerminalNode COLON() { return getToken(IslParser.COLON, 0); }
		public TypeNameDeclarationContext typeNameDeclaration() {
			return getRuleContext(TypeNameDeclarationContext.class,0);
		}
		public AssignPropertyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assignProperty; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterAssignProperty(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitAssignProperty(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitAssignProperty(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AssignPropertyContext assignProperty() throws RecognitionException {
		AssignPropertyContext _localctx = new AssignPropertyContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_assignProperty);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(531);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case FILTER:
			case MAP:
			case IN:
			case MATCHES:
			case RETURN:
			case IMPORT:
			case DECLARETYPE:
			case AS:
			case FROM:
			case LOP:
			case ID:
				{
				setState(529);
				assignSelector();
				}
				break;
			case OPEN_BACKTICK:
				{
				setState(530);
				interpolate();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(540);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case COLON:
				{
				{
				setState(533);
				match(COLON);
				setState(537);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (((((_la - 7)) & ~0x3f) == 0 && ((1L << (_la - 7)) & ((1L << (FILTER - 7)) | (1L << (MAP - 7)) | (1L << (IN - 7)) | (1L << (MATCHES - 7)) | (1L << (RETURN - 7)) | (1L << (IMPORT - 7)) | (1L << (DECLARETYPE - 7)) | (1L << (AS - 7)) | (1L << (FROM - 7)) | (1L << (LOP - 7)) | (1L << (ID - 7)))) != 0)) {
					{
					setState(534);
					((AssignPropertyContext)_localctx).objectType = typeNameDeclaration();
					setState(535);
					match(EQUAL);
					}
				}

				}
				}
				break;
			case EQUAL:
				{
				setState(539);
				match(EQUAL);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(542);
			assignmentValue(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AssignVariablePropertyContext extends ParserRuleContext {
		public TypeNameDeclarationContext objectType;
		public TerminalNode DOLLAR() { return getToken(IslParser.DOLLAR, 0); }
		public AssignSelectorContext assignSelector() {
			return getRuleContext(AssignSelectorContext.class,0);
		}
		public AssignmentValueContext assignmentValue() {
			return getRuleContext(AssignmentValueContext.class,0);
		}
		public TerminalNode EQUAL() { return getToken(IslParser.EQUAL, 0); }
		public TerminalNode COLON() { return getToken(IslParser.COLON, 0); }
		public TypeNameDeclarationContext typeNameDeclaration() {
			return getRuleContext(TypeNameDeclarationContext.class,0);
		}
		public AssignVariablePropertyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assignVariableProperty; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterAssignVariableProperty(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitAssignVariableProperty(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitAssignVariableProperty(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AssignVariablePropertyContext assignVariableProperty() throws RecognitionException {
		AssignVariablePropertyContext _localctx = new AssignVariablePropertyContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_assignVariableProperty);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(544);
			match(DOLLAR);
			setState(545);
			assignSelector();
			setState(553);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case COLON:
				{
				{
				setState(546);
				match(COLON);
				setState(550);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (((((_la - 7)) & ~0x3f) == 0 && ((1L << (_la - 7)) & ((1L << (FILTER - 7)) | (1L << (MAP - 7)) | (1L << (IN - 7)) | (1L << (MATCHES - 7)) | (1L << (RETURN - 7)) | (1L << (IMPORT - 7)) | (1L << (DECLARETYPE - 7)) | (1L << (AS - 7)) | (1L << (FROM - 7)) | (1L << (LOP - 7)) | (1L << (ID - 7)))) != 0)) {
					{
					setState(547);
					((AssignVariablePropertyContext)_localctx).objectType = typeNameDeclaration();
					setState(548);
					match(EQUAL);
					}
				}

				}
				}
				break;
			case EQUAL:
				{
				setState(552);
				match(EQUAL);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(555);
			assignmentValue(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VariableSelectorStartContext extends ParserRuleContext {
		public ShortIdentifierContext shortIdentifier() {
			return getRuleContext(ShortIdentifierContext.class,0);
		}
		public VariableIndexPartContext variableIndexPart() {
			return getRuleContext(VariableIndexPartContext.class,0);
		}
		public VariableConditionPartContext variableConditionPart() {
			return getRuleContext(VariableConditionPartContext.class,0);
		}
		public VariableSelectorStartContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableSelectorStart; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterVariableSelectorStart(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitVariableSelectorStart(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitVariableSelectorStart(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableSelectorStartContext variableSelectorStart() throws RecognitionException {
		VariableSelectorStartContext _localctx = new VariableSelectorStartContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_variableSelectorStart);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(558);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,64,_ctx) ) {
			case 1:
				{
				setState(557);
				shortIdentifier();
				}
				break;
			}
			setState(562);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,65,_ctx) ) {
			case 1:
				{
				setState(560);
				variableIndexPart();
				}
				break;
			case 2:
				{
				setState(561);
				variableConditionPart();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VariableSelectorPartContext extends ParserRuleContext {
		public VariableTextPropertyPartContext variableTextPropertyPart() {
			return getRuleContext(VariableTextPropertyPartContext.class,0);
		}
		public ShortIdentifierContext shortIdentifier() {
			return getRuleContext(ShortIdentifierContext.class,0);
		}
		public VariableIndexPartContext variableIndexPart() {
			return getRuleContext(VariableIndexPartContext.class,0);
		}
		public VariableConditionPartContext variableConditionPart() {
			return getRuleContext(VariableConditionPartContext.class,0);
		}
		public VariableSelectorPartContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableSelectorPart; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterVariableSelectorPart(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitVariableSelectorPart(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitVariableSelectorPart(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableSelectorPartContext variableSelectorPart() throws RecognitionException {
		VariableSelectorPartContext _localctx = new VariableSelectorPartContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_variableSelectorPart);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(566);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SQUAREOPEN:
				{
				setState(564);
				variableTextPropertyPart();
				}
				break;
			case FILTER:
			case MAP:
			case IN:
			case MATCHES:
			case RETURN:
			case IMPORT:
			case DECLARETYPE:
			case AS:
			case FROM:
			case LOP:
			case ID:
				{
				setState(565);
				shortIdentifier();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(570);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,67,_ctx) ) {
			case 1:
				{
				setState(568);
				variableIndexPart();
				}
				break;
			case 2:
				{
				setState(569);
				variableConditionPart();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VariableSelectorContext extends ParserRuleContext {
		public TerminalNode DOLLAR() { return getToken(IslParser.DOLLAR, 0); }
		public VariableSelectorStartContext variableSelectorStart() {
			return getRuleContext(VariableSelectorStartContext.class,0);
		}
		public List<TerminalNode> DOT() { return getTokens(IslParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(IslParser.DOT, i);
		}
		public List<VariableSelectorPartContext> variableSelectorPart() {
			return getRuleContexts(VariableSelectorPartContext.class);
		}
		public VariableSelectorPartContext variableSelectorPart(int i) {
			return getRuleContext(VariableSelectorPartContext.class,i);
		}
		public VariableSelectorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableSelector; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterVariableSelector(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitVariableSelector(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitVariableSelector(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableSelectorContext variableSelector() throws RecognitionException {
		VariableSelectorContext _localctx = new VariableSelectorContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_variableSelector);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(572);
			match(DOLLAR);
			setState(573);
			variableSelectorStart();
			setState(578);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,68,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(574);
					match(DOT);
					setState(575);
					variableSelectorPart();
					}
					} 
				}
				setState(580);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,68,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AssignTextPropertyContext extends ParserRuleContext {
		public TypeNameDeclarationContext objectType;
		public TerminalNode QUOTEDSTRING() { return getToken(IslParser.QUOTEDSTRING, 0); }
		public TerminalNode COLON() { return getToken(IslParser.COLON, 0); }
		public AssignmentValueContext assignmentValue() {
			return getRuleContext(AssignmentValueContext.class,0);
		}
		public TerminalNode EQUAL() { return getToken(IslParser.EQUAL, 0); }
		public TypeNameDeclarationContext typeNameDeclaration() {
			return getRuleContext(TypeNameDeclarationContext.class,0);
		}
		public AssignTextPropertyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assignTextProperty; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterAssignTextProperty(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitAssignTextProperty(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitAssignTextProperty(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AssignTextPropertyContext assignTextProperty() throws RecognitionException {
		AssignTextPropertyContext _localctx = new AssignTextPropertyContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_assignTextProperty);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(581);
			match(QUOTEDSTRING);
			setState(582);
			match(COLON);
			setState(586);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 7)) & ~0x3f) == 0 && ((1L << (_la - 7)) & ((1L << (FILTER - 7)) | (1L << (MAP - 7)) | (1L << (IN - 7)) | (1L << (MATCHES - 7)) | (1L << (RETURN - 7)) | (1L << (IMPORT - 7)) | (1L << (DECLARETYPE - 7)) | (1L << (AS - 7)) | (1L << (FROM - 7)) | (1L << (LOP - 7)) | (1L << (ID - 7)))) != 0)) {
				{
				setState(583);
				((AssignTextPropertyContext)_localctx).objectType = typeNameDeclaration();
				setState(584);
				match(EQUAL);
				}
			}

			setState(588);
			assignmentValue(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SpreadSelectorContext extends ParserRuleContext {
		public TerminalNode SPREAD() { return getToken(IslParser.SPREAD, 0); }
		public VariableSelectorContext variableSelector() {
			return getRuleContext(VariableSelectorContext.class,0);
		}
		public FunctionCallContext functionCall() {
			return getRuleContext(FunctionCallContext.class,0);
		}
		public SpreadSelectorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_spreadSelector; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterSpreadSelector(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitSpreadSelector(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitSpreadSelector(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SpreadSelectorContext spreadSelector() throws RecognitionException {
		SpreadSelectorContext _localctx = new SpreadSelectorContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_spreadSelector);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(590);
			match(SPREAD);
			setState(593);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DOLLAR:
				{
				setState(591);
				variableSelector();
				}
				break;
			case AT:
				{
				setState(592);
				functionCall();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DeclareObjectStatementContext extends ParserRuleContext {
		public SpreadSelectorContext spreadSelector() {
			return getRuleContext(SpreadSelectorContext.class,0);
		}
		public AssignTextPropertyContext assignTextProperty() {
			return getRuleContext(AssignTextPropertyContext.class,0);
		}
		public AssignPropertyContext assignProperty() {
			return getRuleContext(AssignPropertyContext.class,0);
		}
		public AssignVariablePropertyContext assignVariableProperty() {
			return getRuleContext(AssignVariablePropertyContext.class,0);
		}
		public DeclareObjectStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_declareObjectStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterDeclareObjectStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitDeclareObjectStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitDeclareObjectStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DeclareObjectStatementContext declareObjectStatement() throws RecognitionException {
		DeclareObjectStatementContext _localctx = new DeclareObjectStatementContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_declareObjectStatement);
		try {
			setState(599);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SPREAD:
				enterOuterAlt(_localctx, 1);
				{
				setState(595);
				spreadSelector();
				}
				break;
			case QUOTEDSTRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(596);
				assignTextProperty();
				}
				break;
			case FILTER:
			case MAP:
			case IN:
			case MATCHES:
			case RETURN:
			case IMPORT:
			case DECLARETYPE:
			case AS:
			case FROM:
			case OPEN_BACKTICK:
			case LOP:
			case ID:
				enterOuterAlt(_localctx, 3);
				{
				setState(597);
				assignProperty();
				}
				break;
			case DOLLAR:
				enterOuterAlt(_localctx, 4);
				{
				setState(598);
				assignVariableProperty();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DeclareObjectContext extends ParserRuleContext {
		public TerminalNode CURLYOPEN() { return getToken(IslParser.CURLYOPEN, 0); }
		public TerminalNode CURLYCLOSE() { return getToken(IslParser.CURLYCLOSE, 0); }
		public List<DeclareObjectStatementContext> declareObjectStatement() {
			return getRuleContexts(DeclareObjectStatementContext.class);
		}
		public DeclareObjectStatementContext declareObjectStatement(int i) {
			return getRuleContext(DeclareObjectStatementContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(IslParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(IslParser.COMMA, i);
		}
		public List<TerminalNode> SEMICOLON() { return getTokens(IslParser.SEMICOLON); }
		public TerminalNode SEMICOLON(int i) {
			return getToken(IslParser.SEMICOLON, i);
		}
		public DeclareObjectContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_declareObject; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterDeclareObject(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitDeclareObject(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitDeclareObject(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DeclareObjectContext declareObject() throws RecognitionException {
		DeclareObjectContext _localctx = new DeclareObjectContext(_ctx, getState());
		enterRule(_localctx, 78, RULE_declareObject);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(601);
			match(CURLYOPEN);
			setState(613);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << FILTER) | (1L << MAP) | (1L << IN) | (1L << MATCHES) | (1L << RETURN) | (1L << IMPORT) | (1L << DECLARETYPE) | (1L << AS) | (1L << FROM) | (1L << OPEN_BACKTICK) | (1L << LOP) | (1L << DOLLAR) | (1L << SPREAD))) != 0) || _la==ID || _la==QUOTEDSTRING) {
				{
				setState(602);
				declareObjectStatement();
				setState(607);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,72,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(603);
						_la = _input.LA(1);
						if ( !(_la==SEMICOLON || _la==COMMA) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(604);
						declareObjectStatement();
						}
						} 
					}
					setState(609);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,72,_ctx);
				}
				setState(611);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(610);
					match(COMMA);
					}
				}

				}
			}

			setState(615);
			match(CURLYCLOSE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VariableOrObjectContext extends ParserRuleContext {
		public VariableSelectorContext variableSelector() {
			return getRuleContext(VariableSelectorContext.class,0);
		}
		public DeclareObjectContext declareObject() {
			return getRuleContext(DeclareObjectContext.class,0);
		}
		public VariableOrObjectContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableOrObject; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterVariableOrObject(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitVariableOrObject(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitVariableOrObject(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableOrObjectContext variableOrObject() throws RecognitionException {
		VariableOrObjectContext _localctx = new VariableOrObjectContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_variableOrObject);
		try {
			setState(619);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DOLLAR:
				enterOuterAlt(_localctx, 1);
				{
				setState(617);
				variableSelector();
				}
				break;
			case CURLYOPEN:
				enterOuterAlt(_localctx, 2);
				{
				setState(618);
				declareObject();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ForEachContext extends ParserRuleContext {
		public VariableOrObjectContext options;
		public VariableDeclarationContext iterator;
		public TerminalNode FOR() { return getToken(IslParser.FOR, 0); }
		public TerminalNode IN() { return getToken(IslParser.IN, 0); }
		public RhsidContext rhsid() {
			return getRuleContext(RhsidContext.class,0);
		}
		public TerminalNode ENDFOR() { return getToken(IslParser.ENDFOR, 0); }
		public VariableDeclarationContext variableDeclaration() {
			return getRuleContext(VariableDeclarationContext.class,0);
		}
		public TerminalNode PARALLEL() { return getToken(IslParser.PARALLEL, 0); }
		public StatementsContext statements() {
			return getRuleContext(StatementsContext.class,0);
		}
		public DeclareObjectContext declareObject() {
			return getRuleContext(DeclareObjectContext.class,0);
		}
		public VariableSelectorContext variableSelector() {
			return getRuleContext(VariableSelectorContext.class,0);
		}
		public VariableOrObjectContext variableOrObject() {
			return getRuleContext(VariableOrObjectContext.class,0);
		}
		public ForEachContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_forEach; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterForEach(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitForEach(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitForEach(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ForEachContext forEach() throws RecognitionException {
		ForEachContext _localctx = new ForEachContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_forEach);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(625);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==PARALLEL) {
				{
				setState(621);
				match(PARALLEL);
				setState(623);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==DOLLAR || _la==CURLYOPEN) {
					{
					setState(622);
					((ForEachContext)_localctx).options = variableOrObject();
					}
				}

				}
			}

			setState(627);
			match(FOR);
			setState(628);
			((ForEachContext)_localctx).iterator = variableDeclaration();
			setState(629);
			match(IN);
			setState(630);
			rhsid();
			setState(632);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,78,_ctx) ) {
			case 1:
				{
				setState(631);
				statements();
				}
				break;
			}
			setState(636);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case CURLYOPEN:
				{
				setState(634);
				declareObject();
				}
				break;
			case DOLLAR:
				{
				setState(635);
				variableSelector();
				}
				break;
			case ENDFOR:
				break;
			default:
				break;
			}
			setState(638);
			match(ENDFOR);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class WhileLoopContext extends ParserRuleContext {
		public DeclareObjectContext options;
		public DeclareObjectContext bodyDeclareObject;
		public TerminalNode WHILELOOP() { return getToken(IslParser.WHILELOOP, 0); }
		public TerminalNode OPAREN() { return getToken(IslParser.OPAREN, 0); }
		public ConditionExpressionContext conditionExpression() {
			return getRuleContext(ConditionExpressionContext.class,0);
		}
		public TerminalNode CPAREN() { return getToken(IslParser.CPAREN, 0); }
		public TerminalNode ENDWHILELOOP() { return getToken(IslParser.ENDWHILELOOP, 0); }
		public StatementsContext statements() {
			return getRuleContext(StatementsContext.class,0);
		}
		public VariableDeclarationContext variableDeclaration() {
			return getRuleContext(VariableDeclarationContext.class,0);
		}
		public TerminalNode COMMA() { return getToken(IslParser.COMMA, 0); }
		public List<DeclareObjectContext> declareObject() {
			return getRuleContexts(DeclareObjectContext.class);
		}
		public DeclareObjectContext declareObject(int i) {
			return getRuleContext(DeclareObjectContext.class,i);
		}
		public WhileLoopContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_whileLoop; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterWhileLoop(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitWhileLoop(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitWhileLoop(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WhileLoopContext whileLoop() throws RecognitionException {
		WhileLoopContext _localctx = new WhileLoopContext(_ctx, getState());
		enterRule(_localctx, 84, RULE_whileLoop);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(640);
			match(WHILELOOP);
			setState(641);
			match(OPAREN);
			setState(642);
			conditionExpression(0);
			setState(645);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(643);
				match(COMMA);
				setState(644);
				((WhileLoopContext)_localctx).options = declareObject();
				}
			}

			setState(647);
			match(CPAREN);
			setState(651);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,81,_ctx) ) {
			case 1:
				{
				setState(648);
				statements();
				}
				break;
			case 2:
				{
				setState(649);
				((WhileLoopContext)_localctx).bodyDeclareObject = declareObject();
				}
				break;
			case 3:
				{
				setState(650);
				variableDeclaration();
				}
				break;
			}
			setState(653);
			match(ENDWHILELOOP);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FunctionCallContext extends ParserRuleContext {
		public Token service;
		public MultiIdentContext name;
		public TerminalNode AT() { return getToken(IslParser.AT, 0); }
		public List<TerminalNode> DOT() { return getTokens(IslParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(IslParser.DOT, i);
		}
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
		}
		public TerminalNode ID() { return getToken(IslParser.ID, 0); }
		public TerminalNode CURLYOPEN() { return getToken(IslParser.CURLYOPEN, 0); }
		public FunctionStatementsContext functionStatements() {
			return getRuleContext(FunctionStatementsContext.class,0);
		}
		public TerminalNode CURLYCLOSE() { return getToken(IslParser.CURLYCLOSE, 0); }
		public MultiIdentContext multiIdent() {
			return getRuleContext(MultiIdentContext.class,0);
		}
		public FunctionCallContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionCall; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterFunctionCall(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitFunctionCall(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitFunctionCall(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionCallContext functionCall() throws RecognitionException {
		FunctionCallContext _localctx = new FunctionCallContext(_ctx, getState());
		enterRule(_localctx, 86, RULE_functionCall);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(655);
			match(AT);
			setState(656);
			match(DOT);
			setState(657);
			((FunctionCallContext)_localctx).service = match(ID);
			setState(660);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DOT) {
				{
				setState(658);
				match(DOT);
				setState(659);
				((FunctionCallContext)_localctx).name = multiIdent();
				}
			}

			setState(662);
			arguments();
			setState(667);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,83,_ctx) ) {
			case 1:
				{
				setState(663);
				match(CURLYOPEN);
				setState(664);
				functionStatements();
				setState(665);
				match(CURLYCLOSE);
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RhsvalContext extends ParserRuleContext {
		public RhsidContext rhsid() {
			return getRuleContext(RhsidContext.class,0);
		}
		public TerminalNode SEMICOLON() { return getToken(IslParser.SEMICOLON, 0); }
		public RhsvalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rhsval; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterRhsval(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitRhsval(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitRhsval(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RhsvalContext rhsval() throws RecognitionException {
		RhsvalContext _localctx = new RhsvalContext(_ctx, getState());
		enterRule(_localctx, 88, RULE_rhsval);
		try {
			setState(674);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,84,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(669);
				rhsid();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(670);
				rhsid();
				setState(671);
				match(SEMICOLON);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(673);
				match(SEMICOLON);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RhsidContext extends ParserRuleContext {
		public RightSideValueContext rightSideValue() {
			return getRuleContext(RightSideValueContext.class,0);
		}
		public List<ModifierContext> modifier() {
			return getRuleContexts(ModifierContext.class);
		}
		public ModifierContext modifier(int i) {
			return getRuleContext(ModifierContext.class,i);
		}
		public RhsidContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rhsid; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterRhsid(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitRhsid(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitRhsid(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RhsidContext rhsid() throws RecognitionException {
		RhsidContext _localctx = new RhsidContext(_ctx, getState());
		enterRule(_localctx, 90, RULE_rhsid);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(676);
			rightSideValue();
			setState(680);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,85,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(677);
					modifier();
					}
					} 
				}
				setState(682);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,85,_ctx);
			}
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class KeywordContext extends ParserRuleContext {
		public TerminalNode IN() { return getToken(IslParser.IN, 0); }
		public TerminalNode IMPORT() { return getToken(IslParser.IMPORT, 0); }
		public TerminalNode DECLARETYPE() { return getToken(IslParser.DECLARETYPE, 0); }
		public TerminalNode AS() { return getToken(IslParser.AS, 0); }
		public TerminalNode FROM() { return getToken(IslParser.FROM, 0); }
		public TerminalNode LOP() { return getToken(IslParser.LOP, 0); }
		public TerminalNode FILTER() { return getToken(IslParser.FILTER, 0); }
		public TerminalNode RETURN() { return getToken(IslParser.RETURN, 0); }
		public TerminalNode MAP() { return getToken(IslParser.MAP, 0); }
		public TerminalNode MATCHES() { return getToken(IslParser.MATCHES, 0); }
		public KeywordContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_keyword; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterKeyword(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitKeyword(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitKeyword(this);
			else return visitor.visitChildren(this);
		}
	}

	public final KeywordContext keyword() throws RecognitionException {
		KeywordContext _localctx = new KeywordContext(_ctx, getState());
		enterRule(_localctx, 92, RULE_keyword);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(683);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << FILTER) | (1L << MAP) | (1L << IN) | (1L << MATCHES) | (1L << RETURN) | (1L << IMPORT) | (1L << DECLARETYPE) | (1L << AS) | (1L << FROM) | (1L << LOP))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ShortIdentifierContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(IslParser.ID, 0); }
		public KeywordContext keyword() {
			return getRuleContext(KeywordContext.class,0);
		}
		public ShortIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_shortIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterShortIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitShortIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitShortIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ShortIdentifierContext shortIdentifier() throws RecognitionException {
		ShortIdentifierContext _localctx = new ShortIdentifierContext(_ctx, getState());
		enterRule(_localctx, 94, RULE_shortIdentifier);
		try {
			setState(687);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(685);
				match(ID);
				}
				break;
			case FILTER:
			case MAP:
			case IN:
			case MATCHES:
			case RETURN:
			case IMPORT:
			case DECLARETYPE:
			case AS:
			case FROM:
			case LOP:
				enterOuterAlt(_localctx, 2);
				{
				setState(686);
				keyword();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MultiIdentContext extends ParserRuleContext {
		public List<ShortIdentifierContext> shortIdentifier() {
			return getRuleContexts(ShortIdentifierContext.class);
		}
		public ShortIdentifierContext shortIdentifier(int i) {
			return getRuleContext(ShortIdentifierContext.class,i);
		}
		public TerminalNode DOT() { return getToken(IslParser.DOT, 0); }
		public MultiIdentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_multiIdent; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterMultiIdent(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitMultiIdent(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitMultiIdent(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MultiIdentContext multiIdent() throws RecognitionException {
		MultiIdentContext _localctx = new MultiIdentContext(_ctx, getState());
		enterRule(_localctx, 96, RULE_multiIdent);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(689);
			shortIdentifier();
			setState(692);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,87,_ctx) ) {
			case 1:
				{
				setState(690);
				match(DOT);
				setState(691);
				shortIdentifier();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VariableDeclarationContext extends ParserRuleContext {
		public TerminalNode DOLLAR() { return getToken(IslParser.DOLLAR, 0); }
		public ShortIdentifierContext shortIdentifier() {
			return getRuleContext(ShortIdentifierContext.class,0);
		}
		public VariableDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterVariableDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitVariableDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitVariableDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableDeclarationContext variableDeclaration() throws RecognitionException {
		VariableDeclarationContext _localctx = new VariableDeclarationContext(_ctx, getState());
		enterRule(_localctx, 98, RULE_variableDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(694);
			match(DOLLAR);
			setState(695);
			shortIdentifier();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeDefinitionContext extends ParserRuleContext {
		public TypeNameDeclarationContext typeNameDeclaration() {
			return getRuleContext(TypeNameDeclarationContext.class,0);
		}
		public ObjectTypeDefinitionContext objectTypeDefinition() {
			return getRuleContext(ObjectTypeDefinitionContext.class,0);
		}
		public ArrayTypeDefinitionContext arrayTypeDefinition() {
			return getRuleContext(ArrayTypeDefinitionContext.class,0);
		}
		public EnumTypeDefinitionContext enumTypeDefinition() {
			return getRuleContext(EnumTypeDefinitionContext.class,0);
		}
		public TypeDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterTypeDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitTypeDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitTypeDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeDefinitionContext typeDefinition() throws RecognitionException {
		TypeDefinitionContext _localctx = new TypeDefinitionContext(_ctx, getState());
		enterRule(_localctx, 100, RULE_typeDefinition);
		try {
			setState(701);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,88,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(697);
				typeNameDeclaration();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(698);
				objectTypeDefinition();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(699);
				arrayTypeDefinition();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(700);
				enumTypeDefinition();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ObjectTypeDefinitionContext extends ParserRuleContext {
		public TerminalNode CURLYOPEN() { return getToken(IslParser.CURLYOPEN, 0); }
		public TerminalNode CURLYCLOSE() { return getToken(IslParser.CURLYCLOSE, 0); }
		public List<DeclareObjectTypePropertyContext> declareObjectTypeProperty() {
			return getRuleContexts(DeclareObjectTypePropertyContext.class);
		}
		public DeclareObjectTypePropertyContext declareObjectTypeProperty(int i) {
			return getRuleContext(DeclareObjectTypePropertyContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(IslParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(IslParser.COMMA, i);
		}
		public ObjectTypeDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_objectTypeDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterObjectTypeDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitObjectTypeDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitObjectTypeDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ObjectTypeDefinitionContext objectTypeDefinition() throws RecognitionException {
		ObjectTypeDefinitionContext _localctx = new ObjectTypeDefinitionContext(_ctx, getState());
		enterRule(_localctx, 102, RULE_objectTypeDefinition);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(703);
			match(CURLYOPEN);
			setState(715);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 7)) & ~0x3f) == 0 && ((1L << (_la - 7)) & ((1L << (FILTER - 7)) | (1L << (MAP - 7)) | (1L << (IN - 7)) | (1L << (MATCHES - 7)) | (1L << (RETURN - 7)) | (1L << (IMPORT - 7)) | (1L << (DECLARETYPE - 7)) | (1L << (AS - 7)) | (1L << (FROM - 7)) | (1L << (LOP - 7)) | (1L << (ID - 7)))) != 0)) {
				{
				setState(704);
				declareObjectTypeProperty();
				setState(709);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,89,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(705);
						match(COMMA);
						setState(706);
						declareObjectTypeProperty();
						}
						} 
					}
					setState(711);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,89,_ctx);
				}
				setState(713);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(712);
					match(COMMA);
					}
				}

				}
			}

			setState(717);
			match(CURLYCLOSE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DeclareObjectTypePropertyContext extends ParserRuleContext {
		public ShortIdentifierContext shortIdentifier() {
			return getRuleContext(ShortIdentifierContext.class,0);
		}
		public TerminalNode COLON() { return getToken(IslParser.COLON, 0); }
		public TypeDefinitionContext typeDefinition() {
			return getRuleContext(TypeDefinitionContext.class,0);
		}
		public DeclareObjectTypePropertyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_declareObjectTypeProperty; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterDeclareObjectTypeProperty(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitDeclareObjectTypeProperty(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitDeclareObjectTypeProperty(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DeclareObjectTypePropertyContext declareObjectTypeProperty() throws RecognitionException {
		DeclareObjectTypePropertyContext _localctx = new DeclareObjectTypePropertyContext(_ctx, getState());
		enterRule(_localctx, 104, RULE_declareObjectTypeProperty);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(719);
			shortIdentifier();
			setState(720);
			match(COLON);
			setState(721);
			typeDefinition();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArrayTypeDefinitionContext extends ParserRuleContext {
		public TerminalNode SQUAREOPEN() { return getToken(IslParser.SQUAREOPEN, 0); }
		public TerminalNode SQUARECLOSE() { return getToken(IslParser.SQUARECLOSE, 0); }
		public TerminalNode ID() { return getToken(IslParser.ID, 0); }
		public ObjectTypeDefinitionContext objectTypeDefinition() {
			return getRuleContext(ObjectTypeDefinitionContext.class,0);
		}
		public ArrayTypeDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arrayTypeDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterArrayTypeDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitArrayTypeDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitArrayTypeDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArrayTypeDefinitionContext arrayTypeDefinition() throws RecognitionException {
		ArrayTypeDefinitionContext _localctx = new ArrayTypeDefinitionContext(_ctx, getState());
		enterRule(_localctx, 106, RULE_arrayTypeDefinition);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(725);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ID:
				{
				setState(723);
				match(ID);
				}
				break;
			case CURLYOPEN:
				{
				setState(724);
				objectTypeDefinition();
				}
				break;
			case SQUAREOPEN:
				break;
			default:
				break;
			}
			setState(727);
			match(SQUAREOPEN);
			setState(728);
			match(SQUARECLOSE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class EnumTypeDefinitionContext extends ParserRuleContext {
		public TerminalNode SQUAREOPEN() { return getToken(IslParser.SQUAREOPEN, 0); }
		public List<LiteralContext> literal() {
			return getRuleContexts(LiteralContext.class);
		}
		public LiteralContext literal(int i) {
			return getRuleContext(LiteralContext.class,i);
		}
		public TerminalNode SQUARECLOSE() { return getToken(IslParser.SQUARECLOSE, 0); }
		public List<TerminalNode> COMMA() { return getTokens(IslParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(IslParser.COMMA, i);
		}
		public EnumTypeDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumTypeDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterEnumTypeDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitEnumTypeDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitEnumTypeDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnumTypeDefinitionContext enumTypeDefinition() throws RecognitionException {
		EnumTypeDefinitionContext _localctx = new EnumTypeDefinitionContext(_ctx, getState());
		enterRule(_localctx, 108, RULE_enumTypeDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(730);
			match(SQUAREOPEN);
			setState(731);
			literal();
			setState(736);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(732);
				match(COMMA);
				setState(733);
				literal();
				}
				}
				setState(738);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(739);
			match(SQUARECLOSE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeDeclarationContext extends ParserRuleContext {
		public TerminalNode DECLARETYPE() { return getToken(IslParser.DECLARETYPE, 0); }
		public TerminalNode ID() { return getToken(IslParser.ID, 0); }
		public TerminalNode SEMICOLON() { return getToken(IslParser.SEMICOLON, 0); }
		public TerminalNode AS() { return getToken(IslParser.AS, 0); }
		public TypeDefinitionContext typeDefinition() {
			return getRuleContext(TypeDefinitionContext.class,0);
		}
		public TerminalNode FROM() { return getToken(IslParser.FROM, 0); }
		public TerminalNode QUOTEDSTRING() { return getToken(IslParser.QUOTEDSTRING, 0); }
		public TypeDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterTypeDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitTypeDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitTypeDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeDeclarationContext typeDeclaration() throws RecognitionException {
		TypeDeclarationContext _localctx = new TypeDeclarationContext(_ctx, getState());
		enterRule(_localctx, 110, RULE_typeDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(741);
			match(DECLARETYPE);
			setState(742);
			match(ID);
			setState(747);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case AS:
				{
				{
				setState(743);
				match(AS);
				setState(744);
				typeDefinition();
				}
				}
				break;
			case FROM:
				{
				{
				setState(745);
				match(FROM);
				setState(746);
				match(QUOTEDSTRING);
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(749);
			match(SEMICOLON);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeNameDeclarationContext extends ParserRuleContext {
		public List<ShortIdentifierContext> shortIdentifier() {
			return getRuleContexts(ShortIdentifierContext.class);
		}
		public ShortIdentifierContext shortIdentifier(int i) {
			return getRuleContext(ShortIdentifierContext.class,i);
		}
		public List<TerminalNode> DOT() { return getTokens(IslParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(IslParser.DOT, i);
		}
		public TerminalNode SQUAREOPEN() { return getToken(IslParser.SQUAREOPEN, 0); }
		public TerminalNode SQUARECLOSE() { return getToken(IslParser.SQUARECLOSE, 0); }
		public TypeNameDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeNameDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterTypeNameDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitTypeNameDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitTypeNameDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeNameDeclarationContext typeNameDeclaration() throws RecognitionException {
		TypeNameDeclarationContext _localctx = new TypeNameDeclarationContext(_ctx, getState());
		enterRule(_localctx, 112, RULE_typeNameDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(751);
			shortIdentifier();
			setState(756);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOT) {
				{
				{
				setState(752);
				match(DOT);
				setState(753);
				shortIdentifier();
				}
				}
				setState(758);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(761);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SQUAREOPEN) {
				{
				setState(759);
				match(SQUAREOPEN);
				setState(760);
				match(SQUARECLOSE);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FilterModifierContext extends ParserRuleContext {
		public TerminalNode FILTER() { return getToken(IslParser.FILTER, 0); }
		public ConditionContext condition() {
			return getRuleContext(ConditionContext.class,0);
		}
		public FilterModifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_filterModifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterFilterModifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitFilterModifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitFilterModifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FilterModifierContext filterModifier() throws RecognitionException {
		FilterModifierContext _localctx = new FilterModifierContext(_ctx, getState());
		enterRule(_localctx, 114, RULE_filterModifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(763);
			match(FILTER);
			setState(764);
			condition();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ConditionModifierContext extends ParserRuleContext {
		public TerminalNode IFCODE() { return getToken(IslParser.IFCODE, 0); }
		public ConditionContext condition() {
			return getRuleContext(ConditionContext.class,0);
		}
		public MultiIdentContext multiIdent() {
			return getRuleContext(MultiIdentContext.class,0);
		}
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
		}
		public ConditionModifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_conditionModifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterConditionModifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitConditionModifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitConditionModifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConditionModifierContext conditionModifier() throws RecognitionException {
		ConditionModifierContext _localctx = new ConditionModifierContext(_ctx, getState());
		enterRule(_localctx, 116, RULE_conditionModifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(766);
			match(IFCODE);
			setState(767);
			condition();
			setState(768);
			multiIdent();
			setState(770);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,97,_ctx) ) {
			case 1:
				{
				setState(769);
				arguments();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MapModifierContext extends ParserRuleContext {
		public TerminalNode MAP() { return getToken(IslParser.MAP, 0); }
		public TerminalNode OPAREN() { return getToken(IslParser.OPAREN, 0); }
		public ArgumentValueContext argumentValue() {
			return getRuleContext(ArgumentValueContext.class,0);
		}
		public TerminalNode CPAREN() { return getToken(IslParser.CPAREN, 0); }
		public MapModifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_mapModifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterMapModifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitMapModifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitMapModifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MapModifierContext mapModifier() throws RecognitionException {
		MapModifierContext _localctx = new MapModifierContext(_ctx, getState());
		enterRule(_localctx, 118, RULE_mapModifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(772);
			match(MAP);
			setState(773);
			match(OPAREN);
			setState(774);
			argumentValue(0);
			setState(775);
			match(CPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GenericConditionModifierContext extends ParserRuleContext {
		public MultiIdentContext multiIdent() {
			return getRuleContext(MultiIdentContext.class,0);
		}
		public TerminalNode OPAREN() { return getToken(IslParser.OPAREN, 0); }
		public TerminalNode CPAREN() { return getToken(IslParser.CPAREN, 0); }
		public ConditionExpressionContext conditionExpression() {
			return getRuleContext(ConditionExpressionContext.class,0);
		}
		public List<TerminalNode> COMMA() { return getTokens(IslParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(IslParser.COMMA, i);
		}
		public List<ArgumentValueContext> argumentValue() {
			return getRuleContexts(ArgumentValueContext.class);
		}
		public ArgumentValueContext argumentValue(int i) {
			return getRuleContext(ArgumentValueContext.class,i);
		}
		public GenericConditionModifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_genericConditionModifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterGenericConditionModifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitGenericConditionModifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitGenericConditionModifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GenericConditionModifierContext genericConditionModifier() throws RecognitionException {
		GenericConditionModifierContext _localctx = new GenericConditionModifierContext(_ctx, getState());
		enterRule(_localctx, 120, RULE_genericConditionModifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(777);
			multiIdent();
			setState(778);
			match(OPAREN);
			{
			setState(779);
			conditionExpression(0);
			setState(784);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(780);
				match(COMMA);
				setState(781);
				argumentValue(0);
				}
				}
				setState(786);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
			setState(787);
			match(CPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ModifierContext extends ParserRuleContext {
		public TerminalNode MODIFIER() { return getToken(IslParser.MODIFIER, 0); }
		public ConditionModifierContext conditionModifier() {
			return getRuleContext(ConditionModifierContext.class,0);
		}
		public FilterModifierContext filterModifier() {
			return getRuleContext(FilterModifierContext.class,0);
		}
		public MapModifierContext mapModifier() {
			return getRuleContext(MapModifierContext.class,0);
		}
		public GenericConditionModifierContext genericConditionModifier() {
			return getRuleContext(GenericConditionModifierContext.class,0);
		}
		public MultiIdentContext multiIdent() {
			return getRuleContext(MultiIdentContext.class,0);
		}
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
		}
		public ModifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_modifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterModifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitModifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitModifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ModifierContext modifier() throws RecognitionException {
		ModifierContext _localctx = new ModifierContext(_ctx, getState());
		enterRule(_localctx, 122, RULE_modifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(789);
			match(MODIFIER);
			setState(798);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,100,_ctx) ) {
			case 1:
				{
				setState(790);
				conditionModifier();
				}
				break;
			case 2:
				{
				setState(791);
				filterModifier();
				}
				break;
			case 3:
				{
				setState(792);
				mapModifier();
				}
				break;
			case 4:
				{
				setState(793);
				genericConditionModifier();
				}
				break;
			case 5:
				{
				{
				setState(794);
				multiIdent();
				setState(796);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,99,_ctx) ) {
				case 1:
					{
					setState(795);
					arguments();
					}
					break;
				}
				}
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VariableWithModifierContext extends ParserRuleContext {
		public VariableSelectorContext variableSelector() {
			return getRuleContext(VariableSelectorContext.class,0);
		}
		public List<ModifierContext> modifier() {
			return getRuleContexts(ModifierContext.class);
		}
		public ModifierContext modifier(int i) {
			return getRuleContext(ModifierContext.class,i);
		}
		public VariableWithModifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableWithModifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterVariableWithModifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitVariableWithModifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitVariableWithModifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableWithModifierContext variableWithModifier() throws RecognitionException {
		VariableWithModifierContext _localctx = new VariableWithModifierContext(_ctx, getState());
		enterRule(_localctx, 124, RULE_variableWithModifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(800);
			variableSelector();
			setState(804);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==MODIFIER) {
				{
				{
				setState(801);
				modifier();
				}
				}
				setState(806);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArgumentValueContext extends ParserRuleContext {
		public ArgumentItemContext argumentItem() {
			return getRuleContext(ArgumentItemContext.class,0);
		}
		public List<ArgumentValueContext> argumentValue() {
			return getRuleContexts(ArgumentValueContext.class);
		}
		public ArgumentValueContext argumentValue(int i) {
			return getRuleContext(ArgumentValueContext.class,i);
		}
		public TerminalNode COALESCE() { return getToken(IslParser.COALESCE, 0); }
		public ArgumentValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_argumentValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterArgumentValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitArgumentValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitArgumentValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgumentValueContext argumentValue() throws RecognitionException {
		return argumentValue(0);
	}

	private ArgumentValueContext argumentValue(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ArgumentValueContext _localctx = new ArgumentValueContext(_ctx, _parentState);
		ArgumentValueContext _prevctx = _localctx;
		int _startState = 126;
		enterRecursionRule(_localctx, 126, RULE_argumentValue, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(808);
			argumentItem();
			}
			_ctx.stop = _input.LT(-1);
			setState(815);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,102,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new ArgumentValueContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_argumentValue);
					setState(810);
					if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
					setState(811);
					match(COALESCE);
					setState(812);
					argumentValue(3);
					}
					} 
				}
				setState(817);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,102,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class ArgumentItemContext extends ParserRuleContext {
		public MathContext math() {
			return getRuleContext(MathContext.class,0);
		}
		public DeclareObjectContext declareObject() {
			return getRuleContext(DeclareObjectContext.class,0);
		}
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public ArrayContext array() {
			return getRuleContext(ArrayContext.class,0);
		}
		public InterpolateContext interpolate() {
			return getRuleContext(InterpolateContext.class,0);
		}
		public FunctionCallContext functionCall() {
			return getRuleContext(FunctionCallContext.class,0);
		}
		public VariableSelectorContext variableSelector() {
			return getRuleContext(VariableSelectorContext.class,0);
		}
		public List<ModifierContext> modifier() {
			return getRuleContexts(ModifierContext.class);
		}
		public ModifierContext modifier(int i) {
			return getRuleContext(ModifierContext.class,i);
		}
		public ArgumentItemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_argumentItem; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterArgumentItem(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitArgumentItem(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitArgumentItem(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgumentItemContext argumentItem() throws RecognitionException {
		ArgumentItemContext _localctx = new ArgumentItemContext(_ctx, getState());
		enterRule(_localctx, 128, RULE_argumentItem);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(825);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case CURLYOPENOPEN:
				{
				setState(818);
				math();
				}
				break;
			case CURLYOPEN:
				{
				setState(819);
				declareObject();
				}
				break;
			case BOOL:
			case NUM:
			case QUOTEDSTRING:
				{
				setState(820);
				literal();
				}
				break;
			case SQUAREOPEN:
				{
				setState(821);
				array();
				}
				break;
			case OPEN_BACKTICK:
				{
				setState(822);
				interpolate();
				}
				break;
			case AT:
				{
				setState(823);
				functionCall();
				}
				break;
			case DOLLAR:
				{
				setState(824);
				variableSelector();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(830);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,104,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(827);
					modifier();
					}
					} 
				}
				setState(832);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,104,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArgumentsContext extends ParserRuleContext {
		public TerminalNode OPAREN() { return getToken(IslParser.OPAREN, 0); }
		public TerminalNode CPAREN() { return getToken(IslParser.CPAREN, 0); }
		public List<ArgumentValueContext> argumentValue() {
			return getRuleContexts(ArgumentValueContext.class);
		}
		public ArgumentValueContext argumentValue(int i) {
			return getRuleContext(ArgumentValueContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(IslParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(IslParser.COMMA, i);
		}
		public ArgumentsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arguments; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterArguments(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitArguments(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitArguments(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgumentsContext arguments() throws RecognitionException {
		ArgumentsContext _localctx = new ArgumentsContext(_ctx, getState());
		enterRule(_localctx, 130, RULE_arguments);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(833);
			match(OPAREN);
			setState(842);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 40)) & ~0x3f) == 0 && ((1L << (_la - 40)) & ((1L << (OPEN_BACKTICK - 40)) | (1L << (DOLLAR - 40)) | (1L << (AT - 40)) | (1L << (SQUAREOPEN - 40)) | (1L << (CURLYOPEN - 40)) | (1L << (CURLYOPENOPEN - 40)) | (1L << (BOOL - 40)) | (1L << (NUM - 40)) | (1L << (QUOTEDSTRING - 40)))) != 0)) {
				{
				setState(834);
				argumentValue(0);
				setState(839);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(835);
					match(COMMA);
					setState(836);
					argumentValue(0);
					}
					}
					setState(841);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(844);
			match(CPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RightSideValueContext extends ParserRuleContext {
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public ArrayContext array() {
			return getRuleContext(ArrayContext.class,0);
		}
		public InterpolateContext interpolate() {
			return getRuleContext(InterpolateContext.class,0);
		}
		public FunctionCallContext functionCall() {
			return getRuleContext(FunctionCallContext.class,0);
		}
		public VariableSelectorContext variableSelector() {
			return getRuleContext(VariableSelectorContext.class,0);
		}
		public RightSideValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rightSideValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterRightSideValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitRightSideValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitRightSideValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RightSideValueContext rightSideValue() throws RecognitionException {
		RightSideValueContext _localctx = new RightSideValueContext(_ctx, getState());
		enterRule(_localctx, 132, RULE_rightSideValue);
		try {
			setState(851);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case BOOL:
			case NUM:
			case QUOTEDSTRING:
				enterOuterAlt(_localctx, 1);
				{
				setState(846);
				literal();
				}
				break;
			case SQUAREOPEN:
				enterOuterAlt(_localctx, 2);
				{
				setState(847);
				array();
				}
				break;
			case OPEN_BACKTICK:
				enterOuterAlt(_localctx, 3);
				{
				setState(848);
				interpolate();
				}
				break;
			case AT:
				enterOuterAlt(_localctx, 4);
				{
				setState(849);
				functionCall();
				}
				break;
			case DOLLAR:
				enterOuterAlt(_localctx, 5);
				{
				setState(850);
				variableSelector();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArrayArgumentContext extends ParserRuleContext {
		public ArgumentValueContext argumentValue() {
			return getRuleContext(ArgumentValueContext.class,0);
		}
		public SpreadSelectorContext spreadSelector() {
			return getRuleContext(SpreadSelectorContext.class,0);
		}
		public ArrayArgumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arrayArgument; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterArrayArgument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitArrayArgument(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitArrayArgument(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArrayArgumentContext arrayArgument() throws RecognitionException {
		ArrayArgumentContext _localctx = new ArrayArgumentContext(_ctx, getState());
		enterRule(_localctx, 134, RULE_arrayArgument);
		try {
			setState(855);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case OPEN_BACKTICK:
			case DOLLAR:
			case AT:
			case SQUAREOPEN:
			case CURLYOPEN:
			case CURLYOPENOPEN:
			case BOOL:
			case NUM:
			case QUOTEDSTRING:
				enterOuterAlt(_localctx, 1);
				{
				setState(853);
				argumentValue(0);
				}
				break;
			case SPREAD:
				enterOuterAlt(_localctx, 2);
				{
				setState(854);
				spreadSelector();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArrayContext extends ParserRuleContext {
		public TerminalNode SQUAREOPEN() { return getToken(IslParser.SQUAREOPEN, 0); }
		public TerminalNode SQUARECLOSE() { return getToken(IslParser.SQUARECLOSE, 0); }
		public List<ArrayArgumentContext> arrayArgument() {
			return getRuleContexts(ArrayArgumentContext.class);
		}
		public ArrayArgumentContext arrayArgument(int i) {
			return getRuleContext(ArrayArgumentContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(IslParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(IslParser.COMMA, i);
		}
		public ArrayContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_array; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterArray(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitArray(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitArray(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArrayContext array() throws RecognitionException {
		ArrayContext _localctx = new ArrayContext(_ctx, getState());
		enterRule(_localctx, 136, RULE_array);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(857);
			match(SQUAREOPEN);
			setState(866);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 40)) & ~0x3f) == 0 && ((1L << (_la - 40)) & ((1L << (OPEN_BACKTICK - 40)) | (1L << (DOLLAR - 40)) | (1L << (AT - 40)) | (1L << (SPREAD - 40)) | (1L << (SQUAREOPEN - 40)) | (1L << (CURLYOPEN - 40)) | (1L << (CURLYOPENOPEN - 40)) | (1L << (BOOL - 40)) | (1L << (NUM - 40)) | (1L << (QUOTEDSTRING - 40)))) != 0)) {
				{
				setState(858);
				arrayArgument();
				setState(863);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(859);
					match(COMMA);
					setState(860);
					arrayArgument();
					}
					}
					setState(865);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(868);
			match(SQUARECLOSE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExpressionInterpolateContext extends ParserRuleContext {
		public TerminalNode ENTER_EXPR_INTERP() { return getToken(IslParser.ENTER_EXPR_INTERP, 0); }
		public TerminalNode CURLYCLOSE() { return getToken(IslParser.CURLYCLOSE, 0); }
		public VariableWithModifierContext variableWithModifier() {
			return getRuleContext(VariableWithModifierContext.class,0);
		}
		public FunctionCallContext functionCall() {
			return getRuleContext(FunctionCallContext.class,0);
		}
		public MathContext math() {
			return getRuleContext(MathContext.class,0);
		}
		public ExpressionInterpolateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expressionInterpolate; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterExpressionInterpolate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitExpressionInterpolate(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitExpressionInterpolate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionInterpolateContext expressionInterpolate() throws RecognitionException {
		ExpressionInterpolateContext _localctx = new ExpressionInterpolateContext(_ctx, getState());
		enterRule(_localctx, 138, RULE_expressionInterpolate);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(870);
			match(ENTER_EXPR_INTERP);
			setState(874);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DOLLAR:
				{
				setState(871);
				variableWithModifier();
				}
				break;
			case AT:
				{
				setState(872);
				functionCall();
				}
				break;
			case CURLYOPENOPEN:
				{
				setState(873);
				math();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(876);
			match(CURLYCLOSE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MathInterpolateContext extends ParserRuleContext {
		public TerminalNode ENTER_MATH_INTERP() { return getToken(IslParser.ENTER_MATH_INTERP, 0); }
		public MathExpresionContext mathExpresion() {
			return getRuleContext(MathExpresionContext.class,0);
		}
		public TerminalNode CURLYCLOSECLOSE() { return getToken(IslParser.CURLYCLOSECLOSE, 0); }
		public MathInterpolateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_mathInterpolate; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterMathInterpolate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitMathInterpolate(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitMathInterpolate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MathInterpolateContext mathInterpolate() throws RecognitionException {
		MathInterpolateContext _localctx = new MathInterpolateContext(_ctx, getState());
		enterRule(_localctx, 140, RULE_mathInterpolate);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(878);
			match(ENTER_MATH_INTERP);
			setState(879);
			mathExpresion(0);
			setState(880);
			match(CURLYCLOSECLOSE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FuncCallInterpolateContext extends ParserRuleContext {
		public TerminalNode ENTER_FUNC_INTERP() { return getToken(IslParser.ENTER_FUNC_INTERP, 0); }
		public MultiIdentContext multiIdent() {
			return getRuleContext(MultiIdentContext.class,0);
		}
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
		}
		public FuncCallInterpolateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_funcCallInterpolate; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterFuncCallInterpolate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitFuncCallInterpolate(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitFuncCallInterpolate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FuncCallInterpolateContext funcCallInterpolate() throws RecognitionException {
		FuncCallInterpolateContext _localctx = new FuncCallInterpolateContext(_ctx, getState());
		enterRule(_localctx, 142, RULE_funcCallInterpolate);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(882);
			match(ENTER_FUNC_INTERP);
			setState(883);
			multiIdent();
			setState(884);
			arguments();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SimpleInterpolateVariableContext extends ParserRuleContext {
		public TerminalNode ID_INTERP() { return getToken(IslParser.ID_INTERP, 0); }
		public SimpleInterpolateVariableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_simpleInterpolateVariable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterSimpleInterpolateVariable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitSimpleInterpolateVariable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitSimpleInterpolateVariable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SimpleInterpolateVariableContext simpleInterpolateVariable() throws RecognitionException {
		SimpleInterpolateVariableContext _localctx = new SimpleInterpolateVariableContext(_ctx, getState());
		enterRule(_localctx, 144, RULE_simpleInterpolateVariable);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(886);
			match(ID_INTERP);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InterpolateTextContext extends ParserRuleContext {
		public TerminalNode TEXT() { return getToken(IslParser.TEXT, 0); }
		public TerminalNode RECOVERTOKENS_INTERP() { return getToken(IslParser.RECOVERTOKENS_INTERP, 0); }
		public InterpolateTextContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interpolateText; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterInterpolateText(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitInterpolateText(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitInterpolateText(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InterpolateTextContext interpolateText() throws RecognitionException {
		InterpolateTextContext _localctx = new InterpolateTextContext(_ctx, getState());
		enterRule(_localctx, 146, RULE_interpolateText);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(888);
			_la = _input.LA(1);
			if ( !(_la==TEXT || _la==RECOVERTOKENS_INTERP) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InterpolateContext extends ParserRuleContext {
		public TerminalNode OPEN_BACKTICK() { return getToken(IslParser.OPEN_BACKTICK, 0); }
		public TerminalNode CLOSE_BACKTICK() { return getToken(IslParser.CLOSE_BACKTICK, 0); }
		public List<InterpolateTextContext> interpolateText() {
			return getRuleContexts(InterpolateTextContext.class);
		}
		public InterpolateTextContext interpolateText(int i) {
			return getRuleContext(InterpolateTextContext.class,i);
		}
		public List<SimpleInterpolateVariableContext> simpleInterpolateVariable() {
			return getRuleContexts(SimpleInterpolateVariableContext.class);
		}
		public SimpleInterpolateVariableContext simpleInterpolateVariable(int i) {
			return getRuleContext(SimpleInterpolateVariableContext.class,i);
		}
		public List<ExpressionInterpolateContext> expressionInterpolate() {
			return getRuleContexts(ExpressionInterpolateContext.class);
		}
		public ExpressionInterpolateContext expressionInterpolate(int i) {
			return getRuleContext(ExpressionInterpolateContext.class,i);
		}
		public List<MathInterpolateContext> mathInterpolate() {
			return getRuleContexts(MathInterpolateContext.class);
		}
		public MathInterpolateContext mathInterpolate(int i) {
			return getRuleContext(MathInterpolateContext.class,i);
		}
		public List<FuncCallInterpolateContext> funcCallInterpolate() {
			return getRuleContexts(FuncCallInterpolateContext.class);
		}
		public FuncCallInterpolateContext funcCallInterpolate(int i) {
			return getRuleContext(FuncCallInterpolateContext.class,i);
		}
		public List<TerminalNode> RECOVERTOKENS_INTERP() { return getTokens(IslParser.RECOVERTOKENS_INTERP); }
		public TerminalNode RECOVERTOKENS_INTERP(int i) {
			return getToken(IslParser.RECOVERTOKENS_INTERP, i);
		}
		public InterpolateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interpolate; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterInterpolate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitInterpolate(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitInterpolate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InterpolateContext interpolate() throws RecognitionException {
		InterpolateContext _localctx = new InterpolateContext(_ctx, getState());
		enterRule(_localctx, 148, RULE_interpolate);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(890);
			match(OPEN_BACKTICK);
			setState(899);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 76)) & ~0x3f) == 0 && ((1L << (_la - 76)) & ((1L << (ENTER_EXPR_INTERP - 76)) | (1L << (ENTER_MATH_INTERP - 76)) | (1L << (ENTER_FUNC_INTERP - 76)) | (1L << (ID_INTERP - 76)) | (1L << (TEXT - 76)) | (1L << (RECOVERTOKENS_INTERP - 76)))) != 0)) {
				{
				setState(897);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,112,_ctx) ) {
				case 1:
					{
					setState(891);
					interpolateText();
					}
					break;
				case 2:
					{
					setState(892);
					simpleInterpolateVariable();
					}
					break;
				case 3:
					{
					setState(893);
					expressionInterpolate();
					}
					break;
				case 4:
					{
					setState(894);
					mathInterpolate();
					}
					break;
				case 5:
					{
					setState(895);
					funcCallInterpolate();
					}
					break;
				case 6:
					{
					setState(896);
					match(RECOVERTOKENS_INTERP);
					}
					break;
				}
				}
				setState(901);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(902);
			match(CLOSE_BACKTICK);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LiteralContext extends ParserRuleContext {
		public TerminalNode QUOTEDSTRING() { return getToken(IslParser.QUOTEDSTRING, 0); }
		public TerminalNode NUM() { return getToken(IslParser.NUM, 0); }
		public TerminalNode BOOL() { return getToken(IslParser.BOOL, 0); }
		public LiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LiteralContext literal() throws RecognitionException {
		LiteralContext _localctx = new LiteralContext(_ctx, getState());
		enterRule(_localctx, 150, RULE_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(904);
			_la = _input.LA(1);
			if ( !(((((_la - 68)) & ~0x3f) == 0 && ((1L << (_la - 68)) & ((1L << (BOOL - 68)) | (1L << (NUM - 68)) | (1L << (QUOTEDSTRING - 68)))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RegexStringContext extends ParserRuleContext {
		public List<TerminalNode> MATH_DIV() { return getTokens(IslParser.MATH_DIV); }
		public TerminalNode MATH_DIV(int i) {
			return getToken(IslParser.MATH_DIV, i);
		}
		public RegexStringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_regexString; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterRegexString(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitRegexString(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitRegexString(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RegexStringContext regexString() throws RecognitionException {
		RegexStringContext _localctx = new RegexStringContext(_ctx, getState());
		enterRule(_localctx, 152, RULE_regexString);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(906);
			match(MATH_DIV);
			setState(910);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,114,_ctx);
			while ( _alt!=1 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1+1 ) {
					{
					{
					setState(907);
					_la = _input.LA(1);
					if ( _la <= 0 || (_la==MATH_DIV) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					}
					} 
				}
				setState(912);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,114,_ctx);
			}
			setState(913);
			match(MATH_DIV);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MathContext extends ParserRuleContext {
		public TerminalNode CURLYOPENOPEN() { return getToken(IslParser.CURLYOPENOPEN, 0); }
		public MathExpresionContext mathExpresion() {
			return getRuleContext(MathExpresionContext.class,0);
		}
		public TerminalNode CURLYCLOSECLOSE() { return getToken(IslParser.CURLYCLOSECLOSE, 0); }
		public MathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_math; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterMath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitMath(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitMath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MathContext math() throws RecognitionException {
		MathContext _localctx = new MathContext(_ctx, getState());
		enterRule(_localctx, 154, RULE_math);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(915);
			match(CURLYOPENOPEN);
			setState(916);
			mathExpresion(0);
			setState(917);
			match(CURLYCLOSECLOSE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MathExpresionContext extends ParserRuleContext {
		public TerminalNode OPAREN() { return getToken(IslParser.OPAREN, 0); }
		public List<MathExpresionContext> mathExpresion() {
			return getRuleContexts(MathExpresionContext.class);
		}
		public MathExpresionContext mathExpresion(int i) {
			return getRuleContext(MathExpresionContext.class,i);
		}
		public TerminalNode CPAREN() { return getToken(IslParser.CPAREN, 0); }
		public MathValueContext mathValue() {
			return getRuleContext(MathValueContext.class,0);
		}
		public TerminalNode MATH_TIMES() { return getToken(IslParser.MATH_TIMES, 0); }
		public TerminalNode MATH_DIV() { return getToken(IslParser.MATH_DIV, 0); }
		public TerminalNode MATH_PLUS() { return getToken(IslParser.MATH_PLUS, 0); }
		public TerminalNode MATH_MINUS() { return getToken(IslParser.MATH_MINUS, 0); }
		public MathExpresionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_mathExpresion; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterMathExpresion(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitMathExpresion(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitMathExpresion(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MathExpresionContext mathExpresion() throws RecognitionException {
		return mathExpresion(0);
	}

	private MathExpresionContext mathExpresion(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		MathExpresionContext _localctx = new MathExpresionContext(_ctx, _parentState);
		MathExpresionContext _prevctx = _localctx;
		int _startState = 156;
		enterRecursionRule(_localctx, 156, RULE_mathExpresion, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(925);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case OPAREN:
				{
				setState(920);
				match(OPAREN);
				setState(921);
				mathExpresion(0);
				setState(922);
				match(CPAREN);
				}
				break;
			case DOLLAR:
			case AT:
			case NUM:
				{
				setState(924);
				mathValue();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(935);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,117,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(933);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,116,_ctx) ) {
					case 1:
						{
						_localctx = new MathExpresionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_mathExpresion);
						setState(927);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(928);
						_la = _input.LA(1);
						if ( !(_la==MATH_TIMES || _la==MATH_DIV) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(929);
						mathExpresion(5);
						}
						break;
					case 2:
						{
						_localctx = new MathExpresionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_mathExpresion);
						setState(930);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(931);
						_la = _input.LA(1);
						if ( !(_la==MATH_PLUS || _la==MATH_MINUS) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(932);
						mathExpresion(4);
						}
						break;
					}
					} 
				}
				setState(937);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,117,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class MathValueContext extends ParserRuleContext {
		public TerminalNode NUM() { return getToken(IslParser.NUM, 0); }
		public VariableSelectorContext variableSelector() {
			return getRuleContext(VariableSelectorContext.class,0);
		}
		public FunctionCallContext functionCall() {
			return getRuleContext(FunctionCallContext.class,0);
		}
		public MathValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_mathValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).enterMathValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof IslParserListener ) ((IslParserListener)listener).exitMathValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof IslParserVisitor ) return ((IslParserVisitor<? extends T>)visitor).visitMathValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MathValueContext mathValue() throws RecognitionException {
		MathValueContext _localctx = new MathValueContext(_ctx, getState());
		enterRule(_localctx, 158, RULE_mathValue);
		try {
			setState(941);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NUM:
				enterOuterAlt(_localctx, 1);
				{
				setState(938);
				match(NUM);
				}
				break;
			case DOLLAR:
				enterOuterAlt(_localctx, 2);
				{
				setState(939);
				variableSelector();
				}
				break;
			case AT:
				enterOuterAlt(_localctx, 3);
				{
				setState(940);
				functionCall();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 24:
			return conditionExpression_sempred((ConditionExpressionContext)_localctx, predIndex);
		case 25:
			return assignmentValue_sempred((AssignmentValueContext)_localctx, predIndex);
		case 63:
			return argumentValue_sempred((ArgumentValueContext)_localctx, predIndex);
		case 78:
			return mathExpresion_sempred((MathExpresionContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean conditionExpression_sempred(ConditionExpressionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 3);
		}
		return true;
	}
	private boolean assignmentValue_sempred(AssignmentValueContext _localctx, int predIndex) {
		switch (predIndex) {
		case 1:
			return precpred(_ctx, 2);
		}
		return true;
	}
	private boolean argumentValue_sempred(ArgumentValueContext _localctx, int predIndex) {
		switch (predIndex) {
		case 2:
			return precpred(_ctx, 2);
		}
		return true;
	}
	private boolean mathExpresion_sempred(MathExpresionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 3:
			return precpred(_ctx, 4);
		case 4:
			return precpred(_ctx, 3);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3T\u03b2\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
		",\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64\t"+
		"\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\4=\t="+
		"\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\4C\tC\4D\tD\4E\tE\4F\tF\4G\tG\4H\tH\4I"+
		"\tI\4J\tJ\4K\tK\4L\tL\4M\tM\4N\tN\4O\tO\4P\tP\4Q\tQ\3\2\7\2\u00a4\n\2"+
		"\f\2\16\2\u00a7\13\2\3\2\7\2\u00aa\n\2\f\2\16\2\u00ad\13\2\3\2\7\2\u00b0"+
		"\n\2\f\2\16\2\u00b3\13\2\3\2\5\2\u00b6\n\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\4\3\4\3\4\5\4\u00c3\n\4\3\5\3\5\3\5\3\5\7\5\u00c9\n\5\f\5\16\5"+
		"\u00cc\13\5\5\5\u00ce\n\5\3\5\3\5\3\6\3\6\3\6\5\6\u00d5\n\6\3\7\7\7\u00d8"+
		"\n\7\f\7\16\7\u00db\13\7\3\7\5\7\u00de\n\7\5\7\u00e0\n\7\3\7\3\7\5\7\u00e4"+
		"\n\7\3\7\3\7\3\7\3\7\5\7\u00ea\n\7\3\7\3\7\3\7\3\7\3\b\3\b\3\b\3\b\7\b"+
		"\u00f4\n\b\f\b\16\b\u00f7\13\b\5\b\u00f9\n\b\3\b\3\b\3\t\3\t\3\t\5\t\u0100"+
		"\n\t\3\n\3\n\3\n\3\13\6\13\u0106\n\13\r\13\16\13\u0107\3\f\3\f\5\f\u010c"+
		"\n\f\3\f\3\f\5\f\u0110\n\f\3\f\3\f\3\f\3\f\3\f\3\f\5\f\u0118\n\f\3\f\3"+
		"\f\5\f\u011c\n\f\5\f\u011e\n\f\3\r\6\r\u0121\n\r\r\r\16\r\u0122\3\16\3"+
		"\16\5\16\u0127\n\16\3\16\3\16\5\16\u012b\n\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\5\16\u0133\n\16\3\16\3\16\5\16\u0137\n\16\5\16\u0139\n\16\3\17\3"+
		"\17\3\20\3\20\3\20\3\20\5\20\u0141\n\20\3\20\3\20\3\21\3\21\3\21\3\22"+
		"\3\22\3\22\3\22\5\22\u014c\n\22\3\22\5\22\u014f\n\22\3\22\5\22\u0152\n"+
		"\22\3\23\3\23\3\23\5\23\u0157\n\23\3\24\3\24\3\24\3\24\3\24\7\24\u015e"+
		"\n\24\f\24\16\24\u0161\13\24\3\24\5\24\u0164\n\24\3\24\3\24\3\25\5\25"+
		"\u0169\n\25\3\25\3\25\3\25\5\25\u016e\n\25\3\25\5\25\u0171\n\25\3\25\3"+
		"\25\3\25\3\25\3\25\5\25\u0178\n\25\3\25\3\25\3\26\3\26\3\26\3\26\3\26"+
		"\3\26\5\26\u0182\n\26\3\26\3\26\3\27\3\27\3\30\7\30\u0189\n\30\f\30\16"+
		"\30\u018c\13\30\3\30\3\30\7\30\u0190\n\30\f\30\16\30\u0193\13\30\3\30"+
		"\3\30\7\30\u0197\n\30\f\30\16\30\u019a\13\30\3\30\3\30\7\30\u019e\n\30"+
		"\f\30\16\30\u01a1\13\30\3\31\5\31\u01a4\n\31\3\31\3\31\3\31\3\31\3\31"+
		"\3\31\3\31\3\31\3\31\5\31\u01af\n\31\3\32\3\32\3\32\3\32\3\32\3\32\5\32"+
		"\u01b7\n\32\3\32\3\32\3\32\7\32\u01bc\n\32\f\32\16\32\u01bf\13\32\3\33"+
		"\3\33\3\33\3\33\3\33\3\33\7\33\u01c7\n\33\f\33\16\33\u01ca\13\33\3\34"+
		"\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\5\34\u01d7\n\34\3\34"+
		"\7\34\u01da\n\34\f\34\16\34\u01dd\13\34\3\35\3\35\7\35\u01e1\n\35\f\35"+
		"\16\35\u01e4\13\35\3\35\3\35\7\35\u01e8\n\35\f\35\16\35\u01eb\13\35\3"+
		"\35\3\35\3\36\3\36\3\36\3\36\3\37\3\37\7\37\u01f5\n\37\f\37\16\37\u01f8"+
		"\13\37\3\37\3\37\7\37\u01fc\n\37\f\37\16\37\u01ff\13\37\3\37\3\37\3 \3"+
		" \5 \u0205\n \3 \3 \3 \5 \u020a\n \3 \5 \u020d\n \7 \u020f\n \f \16 \u0212"+
		"\13 \3!\3!\5!\u0216\n!\3!\3!\3!\3!\5!\u021c\n!\3!\5!\u021f\n!\3!\3!\3"+
		"\"\3\"\3\"\3\"\3\"\3\"\5\"\u0229\n\"\3\"\5\"\u022c\n\"\3\"\3\"\3#\5#\u0231"+
		"\n#\3#\3#\5#\u0235\n#\3$\3$\5$\u0239\n$\3$\3$\5$\u023d\n$\3%\3%\3%\3%"+
		"\7%\u0243\n%\f%\16%\u0246\13%\3&\3&\3&\3&\3&\5&\u024d\n&\3&\3&\3\'\3\'"+
		"\3\'\5\'\u0254\n\'\3(\3(\3(\3(\5(\u025a\n(\3)\3)\3)\3)\7)\u0260\n)\f)"+
		"\16)\u0263\13)\3)\5)\u0266\n)\5)\u0268\n)\3)\3)\3*\3*\5*\u026e\n*\3+\3"+
		"+\5+\u0272\n+\5+\u0274\n+\3+\3+\3+\3+\3+\5+\u027b\n+\3+\3+\5+\u027f\n"+
		"+\3+\3+\3,\3,\3,\3,\3,\5,\u0288\n,\3,\3,\3,\3,\5,\u028e\n,\3,\3,\3-\3"+
		"-\3-\3-\3-\5-\u0297\n-\3-\3-\3-\3-\3-\5-\u029e\n-\3.\3.\3.\3.\3.\5.\u02a5"+
		"\n.\3/\3/\7/\u02a9\n/\f/\16/\u02ac\13/\3\60\3\60\3\61\3\61\5\61\u02b2"+
		"\n\61\3\62\3\62\3\62\5\62\u02b7\n\62\3\63\3\63\3\63\3\64\3\64\3\64\3\64"+
		"\5\64\u02c0\n\64\3\65\3\65\3\65\3\65\7\65\u02c6\n\65\f\65\16\65\u02c9"+
		"\13\65\3\65\5\65\u02cc\n\65\5\65\u02ce\n\65\3\65\3\65\3\66\3\66\3\66\3"+
		"\66\3\67\3\67\5\67\u02d8\n\67\3\67\3\67\3\67\38\38\38\38\78\u02e1\n8\f"+
		"8\168\u02e4\138\38\38\39\39\39\39\39\39\59\u02ee\n9\39\39\3:\3:\3:\7:"+
		"\u02f5\n:\f:\16:\u02f8\13:\3:\3:\5:\u02fc\n:\3;\3;\3;\3<\3<\3<\3<\5<\u0305"+
		"\n<\3=\3=\3=\3=\3=\3>\3>\3>\3>\3>\7>\u0311\n>\f>\16>\u0314\13>\3>\3>\3"+
		"?\3?\3?\3?\3?\3?\3?\5?\u031f\n?\5?\u0321\n?\3@\3@\7@\u0325\n@\f@\16@\u0328"+
		"\13@\3A\3A\3A\3A\3A\3A\7A\u0330\nA\fA\16A\u0333\13A\3B\3B\3B\3B\3B\3B"+
		"\3B\5B\u033c\nB\3B\7B\u033f\nB\fB\16B\u0342\13B\3C\3C\3C\3C\7C\u0348\n"+
		"C\fC\16C\u034b\13C\5C\u034d\nC\3C\3C\3D\3D\3D\3D\3D\5D\u0356\nD\3E\3E"+
		"\5E\u035a\nE\3F\3F\3F\3F\7F\u0360\nF\fF\16F\u0363\13F\5F\u0365\nF\3F\3"+
		"F\3G\3G\3G\3G\5G\u036d\nG\3G\3G\3H\3H\3H\3H\3I\3I\3I\3I\3J\3J\3K\3K\3"+
		"L\3L\3L\3L\3L\3L\3L\7L\u0384\nL\fL\16L\u0387\13L\3L\3L\3M\3M\3N\3N\7N"+
		"\u038f\nN\fN\16N\u0392\13N\3N\3N\3O\3O\3O\3O\3P\3P\3P\3P\3P\3P\5P\u03a0"+
		"\nP\3P\3P\3P\3P\3P\3P\7P\u03a8\nP\fP\16P\u03ab\13P\3Q\3Q\3Q\5Q\u03b0\n"+
		"Q\3Q\3\u0390\6\62\64\u0080\u009eR\2\4\6\b\n\f\16\20\22\24\26\30\32\34"+
		"\36 \"$&(*,.\60\62\64\668:<>@BDFHJLNPRTVXZ\\^`bdfhjlnprtvxz|~\u0080\u0082"+
		"\u0084\u0086\u0088\u008a\u008c\u008e\u0090\u0092\u0094\u0096\u0098\u009a"+
		"\u009c\u009e\u00a0\2\13\3\2\13\32\3\2\33\34\4\2//\61\61\b\2\t\n\27\27"+
		"\33\33$$&)++\3\2RS\4\2FGII\3\2CC\3\2BC\3\2DE\2\u040f\2\u00b5\3\2\2\2\4"+
		"\u00b9\3\2\2\2\6\u00c2\3\2\2\2\b\u00c4\3\2\2\2\n\u00d1\3\2\2\2\f\u00e3"+
		"\3\2\2\2\16\u00ef\3\2\2\2\20\u00fc\3\2\2\2\22\u0101\3\2\2\2\24\u0105\3"+
		"\2\2\2\26\u011d\3\2\2\2\30\u0120\3\2\2\2\32\u0138\3\2\2\2\34\u013a\3\2"+
		"\2\2\36\u013c\3\2\2\2 \u0144\3\2\2\2\"\u0147\3\2\2\2$\u0153\3\2\2\2&\u0158"+
		"\3\2\2\2(\u0170\3\2\2\2*\u017b\3\2\2\2,\u0185\3\2\2\2.\u018a\3\2\2\2\60"+
		"\u01ae\3\2\2\2\62\u01b6\3\2\2\2\64\u01c0\3\2\2\2\66\u01d6\3\2\2\28\u01de"+
		"\3\2\2\2:\u01ee\3\2\2\2<\u01f2\3\2\2\2>\u0202\3\2\2\2@\u0215\3\2\2\2B"+
		"\u0222\3\2\2\2D\u0230\3\2\2\2F\u0238\3\2\2\2H\u023e\3\2\2\2J\u0247\3\2"+
		"\2\2L\u0250\3\2\2\2N\u0259\3\2\2\2P\u025b\3\2\2\2R\u026d\3\2\2\2T\u0273"+
		"\3\2\2\2V\u0282\3\2\2\2X\u0291\3\2\2\2Z\u02a4\3\2\2\2\\\u02a6\3\2\2\2"+
		"^\u02ad\3\2\2\2`\u02b1\3\2\2\2b\u02b3\3\2\2\2d\u02b8\3\2\2\2f\u02bf\3"+
		"\2\2\2h\u02c1\3\2\2\2j\u02d1\3\2\2\2l\u02d7\3\2\2\2n\u02dc\3\2\2\2p\u02e7"+
		"\3\2\2\2r\u02f1\3\2\2\2t\u02fd\3\2\2\2v\u0300\3\2\2\2x\u0306\3\2\2\2z"+
		"\u030b\3\2\2\2|\u0317\3\2\2\2~\u0322\3\2\2\2\u0080\u0329\3\2\2\2\u0082"+
		"\u033b\3\2\2\2\u0084\u0343\3\2\2\2\u0086\u0355\3\2\2\2\u0088\u0359\3\2"+
		"\2\2\u008a\u035b\3\2\2\2\u008c\u0368\3\2\2\2\u008e\u0370\3\2\2\2\u0090"+
		"\u0374\3\2\2\2\u0092\u0378\3\2\2\2\u0094\u037a\3\2\2\2\u0096\u037c\3\2"+
		"\2\2\u0098\u038a\3\2\2\2\u009a\u038c\3\2\2\2\u009c\u0395\3\2\2\2\u009e"+
		"\u039f\3\2\2\2\u00a0\u03af\3\2\2\2\u00a2\u00a4\5\4\3\2\u00a3\u00a2\3\2"+
		"\2\2\u00a4\u00a7\3\2\2\2\u00a5\u00a3\3\2\2\2\u00a5\u00a6\3\2\2\2\u00a6"+
		"\u00ab\3\2\2\2\u00a7\u00a5\3\2\2\2\u00a8\u00aa\5p9\2\u00a9\u00a8\3\2\2"+
		"\2\u00aa\u00ad\3\2\2\2\u00ab\u00a9\3\2\2\2\u00ab\u00ac\3\2\2\2\u00ac\u00b1"+
		"\3\2\2\2\u00ad\u00ab\3\2\2\2\u00ae\u00b0\5\f\7\2\u00af\u00ae\3\2\2\2\u00b0"+
		"\u00b3\3\2\2\2\u00b1\u00af\3\2\2\2\u00b1\u00b2\3\2\2\2\u00b2\u00b6\3\2"+
		"\2\2\u00b3\u00b1\3\2\2\2\u00b4\u00b6\5\30\r\2\u00b5\u00a5\3\2\2\2\u00b5"+
		"\u00b4\3\2\2\2\u00b6\u00b7\3\2\2\2\u00b7\u00b8\7\2\2\3\u00b8\3\3\2\2\2"+
		"\u00b9\u00ba\7&\2\2\u00ba\u00bb\7H\2\2\u00bb\u00bc\7)\2\2\u00bc\u00bd"+
		"\7I\2\2\u00bd\u00be\7/\2\2\u00be\5\3\2\2\2\u00bf\u00c3\5P)\2\u00c0\u00c3"+
		"\5\u0098M\2\u00c1\u00c3\5\u008aF\2\u00c2\u00bf\3\2\2\2\u00c2\u00c0\3\2"+
		"\2\2\u00c2\u00c1\3\2\2\2\u00c3\7\3\2\2\2\u00c4\u00cd\78\2\2\u00c5\u00ca"+
		"\5\6\4\2\u00c6\u00c7\7\61\2\2\u00c7\u00c9\5\6\4\2\u00c8\u00c6\3\2\2\2"+
		"\u00c9\u00cc\3\2\2\2\u00ca\u00c8\3\2\2\2\u00ca\u00cb\3\2\2\2\u00cb\u00ce"+
		"\3\2\2\2\u00cc\u00ca\3\2\2\2\u00cd\u00c5\3\2\2\2\u00cd\u00ce\3\2\2\2\u00ce"+
		"\u00cf\3\2\2\2\u00cf\u00d0\79\2\2\u00d0\t\3\2\2\2\u00d1\u00d2\7\64\2\2"+
		"\u00d2\u00d4\7H\2\2\u00d3\u00d5\5\b\5\2\u00d4\u00d3\3\2\2\2\u00d4\u00d5"+
		"\3\2\2\2\u00d5\13\3\2\2\2\u00d6\u00d8\5\n\6\2\u00d7\u00d6\3\2\2\2\u00d8"+
		"\u00db\3\2\2\2\u00d9\u00d7\3\2\2\2\u00d9\u00da\3\2\2\2\u00da\u00e0\3\2"+
		"\2\2\u00db\u00d9\3\2\2\2\u00dc\u00de\7%\2\2\u00dd\u00dc\3\2\2\2\u00dd"+
		"\u00de\3\2\2\2\u00de\u00e0\3\2\2\2\u00df\u00d9\3\2\2\2\u00df\u00dd\3\2"+
		"\2\2\u00e0\u00e1\3\2\2\2\u00e1\u00e4\7\"\2\2\u00e2\u00e4\7#\2\2\u00e3"+
		"\u00df\3\2\2\2\u00e3\u00e2\3\2\2\2\u00e4\u00e5\3\2\2\2\u00e5\u00e6\7H"+
		"\2\2\u00e6\u00e9\5\16\b\2\u00e7\u00e8\7-\2\2\u00e8\u00ea\5f\64\2\u00e9"+
		"\u00e7\3\2\2\2\u00e9\u00ea\3\2\2\2\u00ea\u00eb\3\2\2\2\u00eb\u00ec\7>"+
		"\2\2\u00ec\u00ed\5\24\13\2\u00ed\u00ee\7?\2\2\u00ee\r\3\2\2\2\u00ef\u00f8"+
		"\78\2\2\u00f0\u00f5\5\20\t\2\u00f1\u00f2\7\61\2\2\u00f2\u00f4\5\20\t\2"+
		"\u00f3\u00f1\3\2\2\2\u00f4\u00f7\3\2\2\2\u00f5\u00f3\3\2\2\2\u00f5\u00f6"+
		"\3\2\2\2\u00f6\u00f9\3\2\2\2\u00f7\u00f5\3\2\2\2\u00f8\u00f0\3\2\2\2\u00f8"+
		"\u00f9\3\2\2\2\u00f9\u00fa\3\2\2\2\u00fa\u00fb\79\2\2\u00fb\17\3\2\2\2"+
		"\u00fc\u00ff\5d\63\2\u00fd\u00fe\7-\2\2\u00fe\u0100\5f\64\2\u00ff\u00fd"+
		"\3\2\2\2\u00ff\u0100\3\2\2\2\u0100\21\3\2\2\2\u0101\u0102\7$\2\2\u0102"+
		"\u0103\5\64\33\2\u0103\23\3\2\2\2\u0104\u0106\5\26\f\2\u0105\u0104\3\2"+
		"\2\2\u0106\u0107\3\2\2\2\u0107\u0105\3\2\2\2\u0107\u0108\3\2\2\2\u0108"+
		"\25\3\2\2\2\u0109\u010b\5B\"\2\u010a\u010c\7/\2\2\u010b\u010a\3\2\2\2"+
		"\u010b\u010c\3\2\2\2\u010c\u011e\3\2\2\2\u010d\u010f\5@!\2\u010e\u0110"+
		"\7/\2\2\u010f\u010e\3\2\2\2\u010f\u0110\3\2\2\2\u0110\u011e\3\2\2\2\u0111"+
		"\u011e\5\36\20\2\u0112\u011e\5&\24\2\u0113\u011e\5T+\2\u0114\u011e\5V"+
		",\2\u0115\u0117\5X-\2\u0116\u0118\7/\2\2\u0117\u0116\3\2\2\2\u0117\u0118"+
		"\3\2\2\2\u0118\u011e\3\2\2\2\u0119\u011b\5\22\n\2\u011a\u011c\7/\2\2\u011b"+
		"\u011a\3\2\2\2\u011b\u011c\3\2\2\2\u011c\u011e\3\2\2\2\u011d\u0109\3\2"+
		"\2\2\u011d\u010d\3\2\2\2\u011d\u0111\3\2\2\2\u011d\u0112\3\2\2\2\u011d"+
		"\u0113\3\2\2\2\u011d\u0114\3\2\2\2\u011d\u0115\3\2\2\2\u011d\u0119\3\2"+
		"\2\2\u011e\27\3\2\2\2\u011f\u0121\5\32\16\2\u0120\u011f\3\2\2\2\u0121"+
		"\u0122\3\2\2\2\u0122\u0120\3\2\2\2\u0122\u0123\3\2\2\2\u0123\31\3\2\2"+
		"\2\u0124\u0126\5B\"\2\u0125\u0127\7/\2\2\u0126\u0125\3\2\2\2\u0126\u0127"+
		"\3\2\2\2\u0127\u0139\3\2\2\2\u0128\u012a\5@!\2\u0129\u012b\7/\2\2\u012a"+
		"\u0129\3\2\2\2\u012a\u012b\3\2\2\2\u012b\u0139\3\2\2\2\u012c\u0139\5\36"+
		"\20\2\u012d\u0139\5&\24\2\u012e\u0139\5T+\2\u012f\u0139\5V,\2\u0130\u0132"+
		"\5X-\2\u0131\u0133\7/\2\2\u0132\u0131\3\2\2\2\u0132\u0133\3\2\2\2\u0133"+
		"\u0139\3\2\2\2\u0134\u0136\5\22\n\2\u0135\u0137\7/\2\2\u0136\u0135\3\2"+
		"\2\2\u0136\u0137\3\2\2\2\u0137\u0139\3\2\2\2\u0138\u0124\3\2\2\2\u0138"+
		"\u0128\3\2\2\2\u0138\u012c\3\2\2\2\u0138\u012d\3\2\2\2\u0138\u012e\3\2"+
		"\2\2\u0138\u012f\3\2\2\2\u0138\u0130\3\2\2\2\u0138\u0134\3\2\2\2\u0139"+
		"\33\3\2\2\2\u013a\u013b\t\2\2\2\u013b\35\3\2\2\2\u013c\u013d\7\3\2\2\u013d"+
		"\u013e\5.\30\2\u013e\u0140\5\30\r\2\u013f\u0141\5 \21\2\u0140\u013f\3"+
		"\2\2\2\u0140\u0141\3\2\2\2\u0141\u0142\3\2\2\2\u0142\u0143\7\5\2\2\u0143"+
		"\37\3\2\2\2\u0144\u0145\7\4\2\2\u0145\u0146\5\30\r\2\u0146!\3\2\2\2\u0147"+
		"\u0148\7\3\2\2\u0148\u014b\5.\30\2\u0149\u014c\5Z.\2\u014a\u014c\5P)\2"+
		"\u014b\u0149\3\2\2\2\u014b\u014a\3\2\2\2\u014c\u014e\3\2\2\2\u014d\u014f"+
		"\5$\23\2\u014e\u014d\3\2\2\2\u014e\u014f\3\2\2\2\u014f\u0151\3\2\2\2\u0150"+
		"\u0152\7\5\2\2\u0151\u0150\3\2\2\2\u0151\u0152\3\2\2\2\u0152#\3\2\2\2"+
		"\u0153\u0156\7\4\2\2\u0154\u0157\5Z.\2\u0155\u0157\5P)\2\u0156\u0154\3"+
		"\2\2\2\u0156\u0155\3\2\2\2\u0157%\3\2\2\2\u0158\u0159\7\6\2\2\u0159\u015a"+
		"\78\2\2\u015a\u015b\5\\/\2\u015b\u015f\79\2\2\u015c\u015e\5(\25\2\u015d"+
		"\u015c\3\2\2\2\u015e\u0161\3\2\2\2\u015f\u015d\3\2\2\2\u015f\u0160\3\2"+
		"\2\2\u0160\u0163\3\2\2\2\u0161\u015f\3\2\2\2\u0162\u0164\5*\26\2\u0163"+
		"\u0162\3\2\2\2\u0163\u0164\3\2\2\2\u0164\u0165\3\2\2\2\u0165\u0166\7\b"+
		"\2\2\u0166\'\3\2\2\2\u0167\u0169\5\34\17\2\u0168\u0167\3\2\2\2\u0168\u0169"+
		"\3\2\2\2\u0169\u016d\3\2\2\2\u016a\u016e\5H%\2\u016b\u016e\5\u008aF\2"+
		"\u016c\u016e\5\u0098M\2\u016d\u016a\3\2\2\2\u016d\u016b\3\2\2\2\u016d"+
		"\u016c\3\2\2\2\u016e\u0171\3\2\2\2\u016f\u0171\5\u009aN\2\u0170\u0168"+
		"\3\2\2\2\u0170\u016f\3\2\2\2\u0171\u0172\3\2\2\2\u0172\u0177\7\7\2\2\u0173"+
		"\u0178\5X-\2\u0174\u0178\5\30\r\2\u0175\u0178\5\\/\2\u0176\u0178\5P)\2"+
		"\u0177\u0173\3\2\2\2\u0177\u0174\3\2\2\2\u0177\u0175\3\2\2\2\u0177\u0176"+
		"\3\2\2\2\u0178\u0179\3\2\2\2\u0179\u017a\7/\2\2\u017a)\3\2\2\2\u017b\u017c"+
		"\7\4\2\2\u017c\u0181\7\7\2\2\u017d\u0182\5X-\2\u017e\u0182\5\30\r\2\u017f"+
		"\u0182\5\\/\2\u0180\u0182\5P)\2\u0181\u017d\3\2\2\2\u0181\u017e\3\2\2"+
		"\2\u0181\u017f\3\2\2\2\u0181\u0180\3\2\2\2\u0182\u0183\3\2\2\2\u0183\u0184"+
		"\7/\2\2\u0184+\3\2\2\2\u0185\u0186\t\3\2\2\u0186-\3\2\2\2\u0187\u0189"+
		"\7L\2\2\u0188\u0187\3\2\2\2\u0189\u018c\3\2\2\2\u018a\u0188\3\2\2\2\u018a"+
		"\u018b\3\2\2\2\u018b\u018d\3\2\2\2\u018c\u018a\3\2\2\2\u018d\u0191\78"+
		"\2\2\u018e\u0190\7L\2\2\u018f\u018e\3\2\2\2\u0190\u0193\3\2\2\2\u0191"+
		"\u018f\3\2\2\2\u0191\u0192\3\2\2\2\u0192\u0194\3\2\2\2\u0193\u0191\3\2"+
		"\2\2\u0194\u0198\5\62\32\2\u0195\u0197\7L\2\2\u0196\u0195\3\2\2\2\u0197"+
		"\u019a\3\2\2\2\u0198\u0196\3\2\2\2\u0198\u0199\3\2\2\2\u0199\u019b\3\2"+
		"\2\2\u019a\u0198\3\2\2\2\u019b\u019f\79\2\2\u019c\u019e\7L\2\2\u019d\u019c"+
		"\3\2\2\2\u019e\u01a1\3\2\2\2\u019f\u019d\3\2\2\2\u019f\u01a0\3\2\2\2\u01a0"+
		"/\3\2\2\2\u01a1\u019f\3\2\2\2\u01a2\u01a4\7,\2\2\u01a3\u01a2\3\2\2\2\u01a3"+
		"\u01a4\3\2\2\2\u01a4\u01a5\3\2\2\2\u01a5\u01af\5\\/\2\u01a6\u01a7\5\\"+
		"/\2\u01a7\u01a8\5\34\17\2\u01a8\u01a9\5\\/\2\u01a9\u01af\3\2\2\2\u01aa"+
		"\u01ab\5\\/\2\u01ab\u01ac\5,\27\2\u01ac\u01ad\5\u009aN\2\u01ad\u01af\3"+
		"\2\2\2\u01ae\u01a3\3\2\2\2\u01ae\u01a6\3\2\2\2\u01ae\u01aa\3\2\2\2\u01af"+
		"\61\3\2\2\2\u01b0\u01b1\b\32\1\2\u01b1\u01b2\78\2\2\u01b2\u01b3\5\62\32"+
		"\2\u01b3\u01b4\79\2\2\u01b4\u01b7\3\2\2\2\u01b5\u01b7\5\60\31\2\u01b6"+
		"\u01b0\3\2\2\2\u01b6\u01b5\3\2\2\2\u01b7\u01bd\3\2\2\2\u01b8\u01b9\f\5"+
		"\2\2\u01b9\u01ba\7+\2\2\u01ba\u01bc\5\62\32\6\u01bb\u01b8\3\2\2\2\u01bc"+
		"\u01bf\3\2\2\2\u01bd\u01bb\3\2\2\2\u01bd\u01be\3\2\2\2\u01be\63\3\2\2"+
		"\2\u01bf\u01bd\3\2\2\2\u01c0\u01c1\b\33\1\2\u01c1\u01c2\5\66\34\2\u01c2"+
		"\u01c8\3\2\2\2\u01c3\u01c4\f\4\2\2\u01c4\u01c5\7\67\2\2\u01c5\u01c7\5"+
		"\64\33\5\u01c6\u01c3\3\2\2\2\u01c7\u01ca\3\2\2\2\u01c8\u01c6\3\2\2\2\u01c8"+
		"\u01c9\3\2\2\2\u01c9\65\3\2\2\2\u01ca\u01c8\3\2\2\2\u01cb\u01d7\5\u009c"+
		"O\2\u01cc\u01d7\5P)\2\u01cd\u01d7\5\"\22\2\u01ce\u01d7\5T+\2\u01cf\u01d7"+
		"\5V,\2\u01d0\u01d7\5\u0098M\2\u01d1\u01d7\5\u008aF\2\u01d2\u01d7\5\u0096"+
		"L\2\u01d3\u01d7\5X-\2\u01d4\u01d7\5&\24\2\u01d5\u01d7\5H%\2\u01d6\u01cb"+
		"\3\2\2\2\u01d6\u01cc\3\2\2\2\u01d6\u01cd\3\2\2\2\u01d6\u01ce\3\2\2\2\u01d6"+
		"\u01cf\3\2\2\2\u01d6\u01d0\3\2\2\2\u01d6\u01d1\3\2\2\2\u01d6\u01d2\3\2"+
		"\2\2\u01d6\u01d3\3\2\2\2\u01d6\u01d4\3\2\2\2\u01d6\u01d5\3\2\2\2\u01d7"+
		"\u01db\3\2\2\2\u01d8\u01da\5|?\2\u01d9\u01d8\3\2\2\2\u01da\u01dd\3\2\2"+
		"\2\u01db\u01d9\3\2\2\2\u01db\u01dc\3\2\2\2\u01dc\67\3\2\2\2\u01dd\u01db"+
		"\3\2\2\2\u01de\u01e2\7:\2\2\u01df\u01e1\7L\2\2\u01e0\u01df\3\2\2\2\u01e1"+
		"\u01e4\3\2\2\2\u01e2\u01e0\3\2\2\2\u01e2\u01e3\3\2\2\2\u01e3\u01e5\3\2"+
		"\2\2\u01e4\u01e2\3\2\2\2\u01e5\u01e9\5\62\32\2\u01e6\u01e8\7L\2\2\u01e7"+
		"\u01e6\3\2\2\2\u01e8\u01eb\3\2\2\2\u01e9\u01e7\3\2\2\2\u01e9\u01ea\3\2"+
		"\2\2\u01ea\u01ec\3\2\2\2\u01eb\u01e9\3\2\2\2\u01ec\u01ed\7;\2\2\u01ed"+
		"9\3\2\2\2\u01ee\u01ef\7<\2\2\u01ef\u01f0\7G\2\2\u01f0\u01f1\7=\2\2\u01f1"+
		";\3\2\2\2\u01f2\u01f6\7<\2\2\u01f3\u01f5\7L\2\2\u01f4\u01f3\3\2\2\2\u01f5"+
		"\u01f8\3\2\2\2\u01f6\u01f4\3\2\2\2\u01f6\u01f7\3\2\2\2\u01f7\u01f9\3\2"+
		"\2\2\u01f8\u01f6\3\2\2\2\u01f9\u01fd\7I\2\2\u01fa\u01fc\7L\2\2\u01fb\u01fa"+
		"\3\2\2\2\u01fc\u01ff\3\2\2\2\u01fd\u01fb\3\2\2\2\u01fd\u01fe\3\2\2\2\u01fe"+
		"\u0200\3\2\2\2\u01ff\u01fd\3\2\2\2\u0200\u0201\7=\2\2\u0201=\3\2\2\2\u0202"+
		"\u0204\5`\61\2\u0203\u0205\5:\36\2\u0204\u0203\3\2\2\2\u0204\u0205\3\2"+
		"\2\2\u0205\u0210\3\2\2\2\u0206\u0209\7\66\2\2\u0207\u020a\5<\37\2\u0208"+
		"\u020a\5`\61\2\u0209\u0207\3\2\2\2\u0209\u0208\3\2\2\2\u020a\u020c\3\2"+
		"\2\2\u020b\u020d\5:\36\2\u020c\u020b\3\2\2\2\u020c\u020d\3\2\2\2\u020d"+
		"\u020f\3\2\2\2\u020e\u0206\3\2\2\2\u020f\u0212\3\2\2\2\u0210\u020e\3\2"+
		"\2\2\u0210\u0211\3\2\2\2\u0211?\3\2\2\2\u0212\u0210\3\2\2\2\u0213\u0216"+
		"\5> \2\u0214\u0216\5\u0096L\2\u0215\u0213\3\2\2\2\u0215\u0214\3\2\2\2"+
		"\u0216\u021e\3\2\2\2\u0217\u021b\7-\2\2\u0218\u0219\5r:\2\u0219\u021a"+
		"\7.\2\2\u021a\u021c\3\2\2\2\u021b\u0218\3\2\2\2\u021b\u021c\3\2\2\2\u021c"+
		"\u021f\3\2\2\2\u021d\u021f\7.\2\2\u021e\u0217\3\2\2\2\u021e\u021d\3\2"+
		"\2\2\u021f\u0220\3\2\2\2\u0220\u0221\5\64\33\2\u0221A\3\2\2\2\u0222\u0223"+
		"\7\62\2\2\u0223\u022b\5> \2\u0224\u0228\7-\2\2\u0225\u0226\5r:\2\u0226"+
		"\u0227\7.\2\2\u0227\u0229\3\2\2\2\u0228\u0225\3\2\2\2\u0228\u0229\3\2"+
		"\2\2\u0229\u022c\3\2\2\2\u022a\u022c\7.\2\2\u022b\u0224\3\2\2\2\u022b"+
		"\u022a\3\2\2\2\u022c\u022d\3\2\2\2\u022d\u022e\5\64\33\2\u022eC\3\2\2"+
		"\2\u022f\u0231\5`\61\2\u0230\u022f\3\2\2\2\u0230\u0231\3\2\2\2\u0231\u0234"+
		"\3\2\2\2\u0232\u0235\5:\36\2\u0233\u0235\58\35\2\u0234\u0232\3\2\2\2\u0234"+
		"\u0233\3\2\2\2\u0234\u0235\3\2\2\2\u0235E\3\2\2\2\u0236\u0239\5<\37\2"+
		"\u0237\u0239\5`\61\2\u0238\u0236\3\2\2\2\u0238\u0237\3\2\2\2\u0239\u023c"+
		"\3\2\2\2\u023a\u023d\5:\36\2\u023b\u023d\58\35\2\u023c\u023a\3\2\2\2\u023c"+
		"\u023b\3\2\2\2\u023c\u023d\3\2\2\2\u023dG\3\2\2\2\u023e\u023f\7\62\2\2"+
		"\u023f\u0244\5D#\2\u0240\u0241\7\66\2\2\u0241\u0243\5F$\2\u0242\u0240"+
		"\3\2\2\2\u0243\u0246\3\2\2\2\u0244\u0242\3\2\2\2\u0244\u0245\3\2\2\2\u0245"+
		"I\3\2\2\2\u0246\u0244\3\2\2\2\u0247\u0248\7I\2\2\u0248\u024c\7-\2\2\u0249"+
		"\u024a\5r:\2\u024a\u024b\7.\2\2\u024b\u024d\3\2\2\2\u024c\u0249\3\2\2"+
		"\2\u024c\u024d\3\2\2\2\u024d\u024e\3\2\2\2\u024e\u024f\5\64\33\2\u024f"+
		"K\3\2\2\2\u0250\u0253\7\65\2\2\u0251\u0254\5H%\2\u0252\u0254\5X-\2\u0253"+
		"\u0251\3\2\2\2\u0253\u0252\3\2\2\2\u0254M\3\2\2\2\u0255\u025a\5L\'\2\u0256"+
		"\u025a\5J&\2\u0257\u025a\5@!\2\u0258\u025a\5B\"\2\u0259\u0255\3\2\2\2"+
		"\u0259\u0256\3\2\2\2\u0259\u0257\3\2\2\2\u0259\u0258\3\2\2\2\u025aO\3"+
		"\2\2\2\u025b\u0267\7>\2\2\u025c\u0261\5N(\2\u025d\u025e\t\4\2\2\u025e"+
		"\u0260\5N(\2\u025f\u025d\3\2\2\2\u0260\u0263\3\2\2\2\u0261\u025f\3\2\2"+
		"\2\u0261\u0262\3\2\2\2\u0262\u0265\3\2\2\2\u0263\u0261\3\2\2\2\u0264\u0266"+
		"\7\61\2\2\u0265\u0264\3\2\2\2\u0265\u0266\3\2\2\2\u0266\u0268\3\2\2\2"+
		"\u0267\u025c\3\2\2\2\u0267\u0268\3\2\2\2\u0268\u0269\3\2\2\2\u0269\u026a"+
		"\7?\2\2\u026aQ\3\2\2\2\u026b\u026e\5H%\2\u026c\u026e\5P)\2\u026d\u026b"+
		"\3\2\2\2\u026d\u026c\3\2\2\2\u026eS\3\2\2\2\u026f\u0271\7\35\2\2\u0270"+
		"\u0272\5R*\2\u0271\u0270\3\2\2\2\u0271\u0272\3\2\2\2\u0272\u0274\3\2\2"+
		"\2\u0273\u026f\3\2\2\2\u0273\u0274\3\2\2\2\u0274\u0275\3\2\2\2\u0275\u0276"+
		"\7\36\2\2\u0276\u0277\5d\63\2\u0277\u0278\7\27\2\2\u0278\u027a\5\\/\2"+
		"\u0279\u027b\5\30\r\2\u027a\u0279\3\2\2\2\u027a\u027b\3\2\2\2\u027b\u027e"+
		"\3\2\2\2\u027c\u027f\5P)\2\u027d\u027f\5H%\2\u027e\u027c\3\2\2\2\u027e"+
		"\u027d\3\2\2\2\u027e\u027f\3\2\2\2\u027f\u0280\3\2\2\2\u0280\u0281\7\37"+
		"\2\2\u0281U\3\2\2\2\u0282\u0283\7 \2\2\u0283\u0284\78\2\2\u0284\u0287"+
		"\5\62\32\2\u0285\u0286\7\61\2\2\u0286\u0288\5P)\2\u0287\u0285\3\2\2\2"+
		"\u0287\u0288\3\2\2\2\u0288\u0289\3\2\2\2\u0289\u028d\79\2\2\u028a\u028e"+
		"\5\30\r\2\u028b\u028e\5P)\2\u028c\u028e\5d\63\2\u028d\u028a\3\2\2\2\u028d"+
		"\u028b\3\2\2\2\u028d\u028c\3\2\2\2\u028e\u028f\3\2\2\2\u028f\u0290\7!"+
		"\2\2\u0290W\3\2\2\2\u0291\u0292\7\64\2\2\u0292\u0293\7\66\2\2\u0293\u0296"+
		"\7H\2\2\u0294\u0295\7\66\2\2\u0295\u0297\5b\62\2\u0296\u0294\3\2\2\2\u0296"+
		"\u0297\3\2\2\2\u0297\u0298\3\2\2\2\u0298\u029d\5\u0084C\2\u0299\u029a"+
		"\7>\2\2\u029a\u029b\5\24\13\2\u029b\u029c\7?\2\2\u029c\u029e\3\2\2\2\u029d"+
		"\u0299\3\2\2\2\u029d\u029e\3\2\2\2\u029eY\3\2\2\2\u029f\u02a5\5\\/\2\u02a0"+
		"\u02a1\5\\/\2\u02a1\u02a2\7/\2\2\u02a2\u02a5\3\2\2\2\u02a3\u02a5\7/\2"+
		"\2\u02a4\u029f\3\2\2\2\u02a4\u02a0\3\2\2\2\u02a4\u02a3\3\2\2\2\u02a5["+
		"\3\2\2\2\u02a6\u02aa\5\u0086D\2\u02a7\u02a9\5|?\2\u02a8\u02a7\3\2\2\2"+
		"\u02a9\u02ac\3\2\2\2\u02aa\u02a8\3\2\2\2\u02aa\u02ab\3\2\2\2\u02ab]\3"+
		"\2\2\2\u02ac\u02aa\3\2\2\2\u02ad\u02ae\t\5\2\2\u02ae_\3\2\2\2\u02af\u02b2"+
		"\7H\2\2\u02b0\u02b2\5^\60\2\u02b1\u02af\3\2\2\2\u02b1\u02b0\3\2\2\2\u02b2"+
		"a\3\2\2\2\u02b3\u02b6\5`\61\2\u02b4\u02b5\7\66\2\2\u02b5\u02b7\5`\61\2"+
		"\u02b6\u02b4\3\2\2\2\u02b6\u02b7\3\2\2\2\u02b7c\3\2\2\2\u02b8\u02b9\7"+
		"\62\2\2\u02b9\u02ba\5`\61\2\u02bae\3\2\2\2\u02bb\u02c0\5r:\2\u02bc\u02c0"+
		"\5h\65\2\u02bd\u02c0\5l\67\2\u02be\u02c0\5n8\2\u02bf\u02bb\3\2\2\2\u02bf"+
		"\u02bc\3\2\2\2\u02bf\u02bd\3\2\2\2\u02bf\u02be\3\2\2\2\u02c0g\3\2\2\2"+
		"\u02c1\u02cd\7>\2\2\u02c2\u02c7\5j\66\2\u02c3\u02c4\7\61\2\2\u02c4\u02c6"+
		"\5j\66\2\u02c5\u02c3\3\2\2\2\u02c6\u02c9\3\2\2\2\u02c7\u02c5\3\2\2\2\u02c7"+
		"\u02c8\3\2\2\2\u02c8\u02cb\3\2\2\2\u02c9\u02c7\3\2\2\2\u02ca\u02cc\7\61"+
		"\2\2\u02cb\u02ca\3\2\2\2\u02cb\u02cc\3\2\2\2\u02cc\u02ce\3\2\2\2\u02cd"+
		"\u02c2\3\2\2\2\u02cd\u02ce\3\2\2\2\u02ce\u02cf\3\2\2\2\u02cf\u02d0\7?"+
		"\2\2\u02d0i\3\2\2\2\u02d1\u02d2\5`\61\2\u02d2\u02d3\7-\2\2\u02d3\u02d4"+
		"\5f\64\2\u02d4k\3\2\2\2\u02d5\u02d8\7H\2\2\u02d6\u02d8\5h\65\2\u02d7\u02d5"+
		"\3\2\2\2\u02d7\u02d6\3\2\2\2\u02d7\u02d8\3\2\2\2\u02d8\u02d9\3\2\2\2\u02d9"+
		"\u02da\7<\2\2\u02da\u02db\7=\2\2\u02dbm\3\2\2\2\u02dc\u02dd\7<\2\2\u02dd"+
		"\u02e2\5\u0098M\2\u02de\u02df\7\61\2\2\u02df\u02e1\5\u0098M\2\u02e0\u02de"+
		"\3\2\2\2\u02e1\u02e4\3\2\2\2\u02e2\u02e0\3\2\2\2\u02e2\u02e3\3\2\2\2\u02e3"+
		"\u02e5\3\2\2\2\u02e4\u02e2\3\2\2\2\u02e5\u02e6\7=\2\2\u02e6o\3\2\2\2\u02e7"+
		"\u02e8\7\'\2\2\u02e8\u02ed\7H\2\2\u02e9\u02ea\7(\2\2\u02ea\u02ee\5f\64"+
		"\2\u02eb\u02ec\7)\2\2\u02ec\u02ee\7I\2\2\u02ed\u02e9\3\2\2\2\u02ed\u02eb"+
		"\3\2\2\2\u02ee\u02ef\3\2\2\2\u02ef\u02f0\7/\2\2\u02f0q\3\2\2\2\u02f1\u02f6"+
		"\5`\61\2\u02f2\u02f3\7\66\2\2\u02f3\u02f5\5`\61\2\u02f4\u02f2\3\2\2\2"+
		"\u02f5\u02f8\3\2\2\2\u02f6\u02f4\3\2\2\2\u02f6\u02f7\3\2\2\2\u02f7\u02fb"+
		"\3\2\2\2\u02f8\u02f6\3\2\2\2\u02f9\u02fa\7<\2\2\u02fa\u02fc\7=\2\2\u02fb"+
		"\u02f9\3\2\2\2\u02fb\u02fc\3\2\2\2\u02fcs\3\2\2\2\u02fd\u02fe\7\t\2\2"+
		"\u02fe\u02ff\5.\30\2\u02ffu\3\2\2\2\u0300\u0301\7\3\2\2\u0301\u0302\5"+
		".\30\2\u0302\u0304\5b\62\2\u0303\u0305\5\u0084C\2\u0304\u0303\3\2\2\2"+
		"\u0304\u0305\3\2\2\2\u0305w\3\2\2\2\u0306\u0307\7\n\2\2\u0307\u0308\7"+
		"8\2\2\u0308\u0309\5\u0080A\2\u0309\u030a\79\2\2\u030ay\3\2\2\2\u030b\u030c"+
		"\5b\62\2\u030c\u030d\78\2\2\u030d\u0312\5\62\32\2\u030e\u030f\7\61\2\2"+
		"\u030f\u0311\5\u0080A\2\u0310\u030e\3\2\2\2\u0311\u0314\3\2\2\2\u0312"+
		"\u0310\3\2\2\2\u0312\u0313\3\2\2\2\u0313\u0315\3\2\2\2\u0314\u0312\3\2"+
		"\2\2\u0315\u0316\79\2\2\u0316{\3\2\2\2\u0317\u0320\7\60\2\2\u0318\u0321"+
		"\5v<\2\u0319\u0321\5t;\2\u031a\u0321\5x=\2\u031b\u0321\5z>\2\u031c\u031e"+
		"\5b\62\2\u031d\u031f\5\u0084C\2\u031e\u031d\3\2\2\2\u031e\u031f\3\2\2"+
		"\2\u031f\u0321\3\2\2\2\u0320\u0318\3\2\2\2\u0320\u0319\3\2\2\2\u0320\u031a"+
		"\3\2\2\2\u0320\u031b\3\2\2\2\u0320\u031c\3\2\2\2\u0321}\3\2\2\2\u0322"+
		"\u0326\5H%\2\u0323\u0325\5|?\2\u0324\u0323\3\2\2\2\u0325\u0328\3\2\2\2"+
		"\u0326\u0324\3\2\2\2\u0326\u0327\3\2\2\2\u0327\177\3\2\2\2\u0328\u0326"+
		"\3\2\2\2\u0329\u032a\bA\1\2\u032a\u032b\5\u0082B\2\u032b\u0331\3\2\2\2"+
		"\u032c\u032d\f\4\2\2\u032d\u032e\7\67\2\2\u032e\u0330\5\u0080A\5\u032f"+
		"\u032c\3\2\2\2\u0330\u0333\3\2\2\2\u0331\u032f\3\2\2\2\u0331\u0332\3\2"+
		"\2\2\u0332\u0081\3\2\2\2\u0333\u0331\3\2\2\2\u0334\u033c\5\u009cO\2\u0335"+
		"\u033c\5P)\2\u0336\u033c\5\u0098M\2\u0337\u033c\5\u008aF\2\u0338\u033c"+
		"\5\u0096L\2\u0339\u033c\5X-\2\u033a\u033c\5H%\2\u033b\u0334\3\2\2\2\u033b"+
		"\u0335\3\2\2\2\u033b\u0336\3\2\2\2\u033b\u0337\3\2\2\2\u033b\u0338\3\2"+
		"\2\2\u033b\u0339\3\2\2\2\u033b\u033a\3\2\2\2\u033c\u0340\3\2\2\2\u033d"+
		"\u033f\5|?\2\u033e\u033d\3\2\2\2\u033f\u0342\3\2\2\2\u0340\u033e\3\2\2"+
		"\2\u0340\u0341\3\2\2\2\u0341\u0083\3\2\2\2\u0342\u0340\3\2\2\2\u0343\u034c"+
		"\78\2\2\u0344\u0349\5\u0080A\2\u0345\u0346\7\61\2\2\u0346\u0348\5\u0080"+
		"A\2\u0347\u0345\3\2\2\2\u0348\u034b\3\2\2\2\u0349\u0347\3\2\2\2\u0349"+
		"\u034a\3\2\2\2\u034a\u034d\3\2\2\2\u034b\u0349\3\2\2\2\u034c\u0344\3\2"+
		"\2\2\u034c\u034d\3\2\2\2\u034d\u034e\3\2\2\2\u034e\u034f\79\2\2\u034f"+
		"\u0085\3\2\2\2\u0350\u0356\5\u0098M\2\u0351\u0356\5\u008aF\2\u0352\u0356"+
		"\5\u0096L\2\u0353\u0356\5X-\2\u0354\u0356\5H%\2\u0355\u0350\3\2\2\2\u0355"+
		"\u0351\3\2\2\2\u0355\u0352\3\2\2\2\u0355\u0353\3\2\2\2\u0355\u0354\3\2"+
		"\2\2\u0356\u0087\3\2\2\2\u0357\u035a\5\u0080A\2\u0358\u035a\5L\'\2\u0359"+
		"\u0357\3\2\2\2\u0359\u0358\3\2\2\2\u035a\u0089\3\2\2\2\u035b\u0364\7<"+
		"\2\2\u035c\u0361\5\u0088E\2\u035d\u035e\7\61\2\2\u035e\u0360\5\u0088E"+
		"\2\u035f\u035d\3\2\2\2\u0360\u0363\3\2\2\2\u0361\u035f\3\2\2\2\u0361\u0362"+
		"\3\2\2\2\u0362\u0365\3\2\2\2\u0363\u0361\3\2\2\2\u0364\u035c\3\2\2\2\u0364"+
		"\u0365\3\2\2\2\u0365\u0366\3\2\2\2\u0366\u0367\7=\2\2\u0367\u008b\3\2"+
		"\2\2\u0368\u036c\7N\2\2\u0369\u036d\5~@\2\u036a\u036d\5X-\2\u036b\u036d"+
		"\5\u009cO\2\u036c\u0369\3\2\2\2\u036c\u036a\3\2\2\2\u036c\u036b\3\2\2"+
		"\2\u036d\u036e\3\2\2\2\u036e\u036f\7?\2\2\u036f\u008d\3\2\2\2\u0370\u0371"+
		"\7O\2\2\u0371\u0372\5\u009eP\2\u0372\u0373\7A\2\2\u0373\u008f\3\2\2\2"+
		"\u0374\u0375\7P\2\2\u0375\u0376\5b\62\2\u0376\u0377\5\u0084C\2\u0377\u0091"+
		"\3\2\2\2\u0378\u0379\7Q\2\2\u0379\u0093\3\2\2\2\u037a\u037b\t\6\2\2\u037b"+
		"\u0095\3\2\2\2\u037c\u0385\7*\2\2\u037d\u0384\5\u0094K\2\u037e\u0384\5"+
		"\u0092J\2\u037f\u0384\5\u008cG\2\u0380\u0384\5\u008eH\2\u0381\u0384\5"+
		"\u0090I\2\u0382\u0384\7S\2\2\u0383\u037d\3\2\2\2\u0383\u037e\3\2\2\2\u0383"+
		"\u037f\3\2\2\2\u0383\u0380\3\2\2\2\u0383\u0381\3\2\2\2\u0383\u0382\3\2"+
		"\2\2\u0384\u0387\3\2\2\2\u0385\u0383\3\2\2\2\u0385\u0386\3\2\2\2\u0386"+
		"\u0388\3\2\2\2\u0387\u0385\3\2\2\2\u0388\u0389\7T\2\2\u0389\u0097\3\2"+
		"\2\2\u038a\u038b\t\7\2\2\u038b\u0099\3\2\2\2\u038c\u0390\7C\2\2\u038d"+
		"\u038f\n\b\2\2\u038e\u038d\3\2\2\2\u038f\u0392\3\2\2\2\u0390\u0391\3\2"+
		"\2\2\u0390\u038e\3\2\2\2\u0391\u0393\3\2\2\2\u0392\u0390\3\2\2\2\u0393"+
		"\u0394\7C\2\2\u0394\u009b\3\2\2\2\u0395\u0396\7@\2\2\u0396\u0397\5\u009e"+
		"P\2\u0397\u0398\7A\2\2\u0398\u009d\3\2\2\2\u0399\u039a\bP\1\2\u039a\u039b"+
		"\78\2\2\u039b\u039c\5\u009eP\2\u039c\u039d\79\2\2\u039d\u03a0\3\2\2\2"+
		"\u039e\u03a0\5\u00a0Q\2\u039f\u0399\3\2\2\2\u039f\u039e\3\2\2\2\u03a0"+
		"\u03a9\3\2\2\2\u03a1\u03a2\f\6\2\2\u03a2\u03a3\t\t\2\2\u03a3\u03a8\5\u009e"+
		"P\7\u03a4\u03a5\f\5\2\2\u03a5\u03a6\t\n\2\2\u03a6\u03a8\5\u009eP\6\u03a7"+
		"\u03a1\3\2\2\2\u03a7\u03a4\3\2\2\2\u03a8\u03ab\3\2\2\2\u03a9\u03a7\3\2"+
		"\2\2\u03a9\u03aa\3\2\2\2\u03aa\u009f\3\2\2\2\u03ab\u03a9\3\2\2\2\u03ac"+
		"\u03b0\7G\2\2\u03ad\u03b0\5H%\2\u03ae\u03b0\5X-\2\u03af\u03ac\3\2\2\2"+
		"\u03af\u03ad\3\2\2\2\u03af\u03ae\3\2\2\2\u03b0\u00a1\3\2\2\2y\u00a5\u00ab"+
		"\u00b1\u00b5\u00c2\u00ca\u00cd\u00d4\u00d9\u00dd\u00df\u00e3\u00e9\u00f5"+
		"\u00f8\u00ff\u0107\u010b\u010f\u0117\u011b\u011d\u0122\u0126\u012a\u0132"+
		"\u0136\u0138\u0140\u014b\u014e\u0151\u0156\u015f\u0163\u0168\u016d\u0170"+
		"\u0177\u0181\u018a\u0191\u0198\u019f\u01a3\u01ae\u01b6\u01bd\u01c8\u01d6"+
		"\u01db\u01e2\u01e9\u01f6\u01fd\u0204\u0209\u020c\u0210\u0215\u021b\u021e"+
		"\u0228\u022b\u0230\u0234\u0238\u023c\u0244\u024c\u0253\u0259\u0261\u0265"+
		"\u0267\u026d\u0271\u0273\u027a\u027e\u0287\u028d\u0296\u029d\u02a4\u02aa"+
		"\u02b1\u02b6\u02bf\u02c7\u02cb\u02cd\u02d7\u02e2\u02ed\u02f6\u02fb\u0304"+
		"\u0312\u031e\u0320\u0326\u0331\u033b\u0340\u0349\u034c\u0355\u0359\u0361"+
		"\u0364\u036c\u0383\u0385\u0390\u039f\u03a7\u03a9\u03af";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}