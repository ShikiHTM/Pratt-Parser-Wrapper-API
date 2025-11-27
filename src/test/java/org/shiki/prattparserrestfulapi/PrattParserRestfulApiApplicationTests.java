package org.shiki.prattparserrestfulapi;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.shiki.prattparserrestfulapi.parser.LipsNotation;
import org.shiki.prattparserrestfulapi.parser.Parser;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.junit.jupiter.api.Assertions.*;

class PrattParserRestfulApiApplicationTests {

    @Test
    void TestPlus_1() {
        String expr = "1 + 4 + 5 + 3";

        assertEquals(13, Parser.eval(expr));
    }

    @Test
    void TestPlus_2() {
        String expr = "-1 + 4 - -5 + 3";

        assertEquals(11.0, Parser.eval(expr));
    }


    @Test
    void TestMinus_1() {
        String expr = "10 - 3 - 2";
        assertEquals(5.0, Parser.eval(expr));
    }

    @Test
    void TestMinus_2() {
        String expr = "5 - -4 - 3";
        assertEquals(6, Parser.eval(expr));
    }

    @Test
    void TestMultiply_1() {
        String expr = "2 * 3 * 4";
        assertEquals(24, Parser.eval(expr));
    }

    @Test
    void TestMultiply_2() {
        String expr = "-2 * 3 * -1";
        assertEquals(6, Parser.eval(expr));
    }

    @Test
    void TestDivide_1() {
        String expr = "20 / 2 / 2";
        assertEquals(5, Parser.eval(expr));
    }

    @Test
    void TestDivide_2() {
        String expr = "-16 / 2 / -2";
        assertEquals(4, Parser.eval(expr));
    }

    @Test
    void TestMixed_1() {
        String expr = "2 + 3 * 4 - 5";
        assertEquals(9, Parser.eval(expr)); // 2 + (3*4) - 5
    }

    @Test
    void TestMixed_2() {
        String expr = "-4 + 6 / 2 * 3 - 1";
        assertEquals(4, Parser.eval(expr)); // -4 + ((6/2)*3) - 1
    }
}
