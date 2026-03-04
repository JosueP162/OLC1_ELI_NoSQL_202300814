package analyzer;

import java_cup.runtime.Symbol;
import controller.ExecutionContext;
import model.reports.Token;
import java.util.List;
import java.util.ArrayList;

%%

%class Lexer
%unicode
%cup
%line
%column
%public

%{
    private ExecutionContext ctx;
    private int tokenCount = 0;

    public Lexer(java.io.Reader in, ExecutionContext ctx) {
        this(in);
        this.ctx = ctx;
    }

    private Symbol token(int type, Object value, String tokenType) {
        tokenCount++;
        if (ctx != null) {
            ctx.addToken(new Token(tokenCount, yytext(), tokenType, yyline + 1, yycolumn + 1));
        }
        return new Symbol(type, yyline + 1, yycolumn + 1, value);
    }

    private Symbol token(int type, String tokenType) {
        return token(type, yytext(), tokenType);
    }
%}

/* Definiciones */
DIGITO        = [0-9]
LETRA         = [a-zA-Z_]
ALFANUM       = [a-zA-Z0-9_]
ENTERO        = {DIGITO}+
DECIMAL       = {DIGITO}+"."{DIGITO}+
IDENTIFICADOR = {LETRA}{ALFANUM}*
CADENA        = \"([^\\\"]|\\.)*\"
ESPACIOS      = [ \t\r\n]+

/* Comentarios */
COMENTARIO_LINEA    = "##"[^\n]*
COMENTARIO_MULTI    = "#*"[^#]*("#"[^*][^#]*)*"#*"

%%

/* ========== ESPACIOS Y COMENTARIOS (ignorar) ========== */
{ESPACIOS}           { /* ignorar */ }
{COMENTARIO_LINEA}   { /* ignorar comentario de línea */ }
{COMENTARIO_MULTI}   { /* ignorar comentario multilínea */ }

/* ========== PALABRAS RESERVADAS ========== */
"database"   { return token(sym.DATABASE, "RESERVADA"); }
"use"        { return token(sym.USE, "RESERVADA"); }
"table"      { return token(sym.TABLE, "RESERVADA"); }
"read"       { return token(sym.READ, "RESERVADA"); }
"add"        { return token(sym.ADD, "RESERVADA"); }
"update"     { return token(sym.UPDATE, "RESERVADA"); }
"clear"      { return token(sym.CLEAR, "RESERVADA"); }
"export"     { return token(sym.EXPORT, "RESERVADA"); }
"fields"     { return token(sym.FIELDS, "RESERVADA"); }
"filter"     { return token(sym.FILTER, "RESERVADA"); }
"store"      { return token(sym.STORE, "RESERVADA"); }
"at"         { return token(sym.AT, "RESERVADA"); }
"set"        { return token(sym.SET, "RESERVADA"); }

/* ========== TIPOS DE DATOS ========== */
"int"        { return token(sym.TIPO_INT, "TIPO"); }
"float"      { return token(sym.TIPO_FLOAT, "TIPO"); }
"bool"       { return token(sym.TIPO_BOOL, "TIPO"); }
"string"     { return token(sym.TIPO_STRING, "TIPO"); }
"array"      { return token(sym.TIPO_ARRAY, "TIPO"); }
"object"     { return token(sym.TIPO_OBJECT, "TIPO"); }
"null"       { return token(sym.NULO, "NULO"); }

/* ========== LITERALES ========== */
"true"       { return token(sym.BOOLEANO, true, "BOOLEANO"); }
"false"      { return token(sym.BOOLEANO, false, "BOOLEANO"); }

{ENTERO}     {
    tokenCount++;
    if (ctx != null) {
        ctx.addToken(new Token(tokenCount, yytext(), "ENTERO", yyline + 1, yycolumn + 1));
    }
    return new Symbol(sym.ENTERO, yyline + 1, yycolumn + 1, Integer.parseInt(yytext()));
}

{DECIMAL}    {
    tokenCount++;
    if (ctx != null) {
        ctx.addToken(new Token(tokenCount, yytext(), "DECIMAL", yyline + 1, yycolumn + 1));
    }
    return new Symbol(sym.DECIMAL, yyline + 1, yycolumn + 1, Double.parseDouble(yytext()));
}

{CADENA}     {
    tokenCount++;
    String valor = yytext().substring(1, yytext().length() - 1); // quitar comillas
    if (ctx != null) {
        ctx.addToken(new Token(tokenCount, yytext(), "CADENA", yyline + 1, yycolumn + 1));
    }
    return new Symbol(sym.CADENA, yyline + 1, yycolumn + 1, valor);
}

/* ========== IDENTIFICADORES ========== */
{IDENTIFICADOR} {
    tokenCount++;
    if (ctx != null) {
        ctx.addToken(new Token(tokenCount, yytext(), "IDENTIFICADOR", yyline + 1, yycolumn + 1));
    }
    return new Symbol(sym.ID, yyline + 1, yycolumn + 1, yytext());
}

/* ========== OPERADORES RELACIONALES ========== */
"=="         { return token(sym.IGUAL_IGUAL, "OP_RELACIONAL"); }
"!="         { return token(sym.DIFERENTE, "OP_RELACIONAL"); }
">="         { return token(sym.MAYOR_IGUAL, "OP_RELACIONAL"); }
"<="         { return token(sym.MENOR_IGUAL, "OP_RELACIONAL"); }
">"          { return token(sym.MAYOR, "OP_RELACIONAL"); }
"<"          { return token(sym.MENOR, "OP_RELACIONAL"); }

/* ========== OPERADORES LÓGICOS ========== */
"&&"         { return token(sym.AND, "OP_LOGICO"); }
"||"         { return token(sym.OR, "OP_LOGICO"); }
"!"          { return token(sym.NOT, "OP_LOGICO"); }

/* ========== OPERADOR ASIGNACIÓN (para SET) ========== */
"="          { return token(sym.IGUAL, "ASIGNACION"); }

/* ========== SIGNOS DE AGRUPACIÓN ========== */
"{"          { return token(sym.LLAVE_A, "LLAVE_A"); }
"}"          { return token(sym.LLAVE_C, "LLAVE_C"); }
"("          { return token(sym.PAREN_A, "PAREN_A"); }
")"          { return token(sym.PAREN_C, "PAREN_C"); }
"["          { return token(sym.CORCHETE_A, "CORCHETE_A"); }
"]"          { return token(sym.CORCHETE_C, "CORCHETE_C"); }

/* ========== SEPARADORES ========== */
";"          { return token(sym.PUNTO_COMA, "PUNTO_COMA"); }
":"          { return token(sym.DOS_PUNTOS, "DOS_PUNTOS"); }
","          { return token(sym.COMA, "COMA"); }
"."          { return token(sym.PUNTO, "PUNTO"); }
"*"          { return token(sym.ASTERISCO, "ASTERISCO"); }

/* ========== ERRORES LÉXICOS ========== */
[^]           {
    if (ctx != null) {
        ctx.addError("Léxico",
            "El carácter '" + yytext() + "' no pertenece al lenguaje.",
            yyline + 1, yycolumn + 1);
    }
    /* NO lanzar excepción - continuar el análisis */
}
