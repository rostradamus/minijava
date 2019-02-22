package test.translate.typechecker;

import ast.BooleanType;
import ast.IntegerType;
import ast.Type;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import parser.ParseException;
import typechecker.ErrorMessage;
import typechecker.TypeChecker;
import typechecker.TypeCheckerException;
import util.SampleCode;

import java.io.File;

import static parser.Parser.parseExp;

/**
 * The difficulty in writing tests for this unit of work is that we should,
 * if at all possible try to not make the testing code be dependant on the
 * Expression type checker returning specific error messages.
 * <p>
 * To try to still have reasonably specific tests that specify relatively
 * precisely what type of error a specific program ought to raise we will:
 * <ul>
 * <li>Provide you with a class ErrorReport that you should use to create
 * error reports.
 * <li>Tests will only inspect the first error in the report.
 * <li>Tests will be written to avoid ambiguities into what is the "first"
 * error as much as possible.
 * </ul>
 *
 * @author kdvolder
 */
public class TypeCheckTest {

    //////////////////////////////////////////////////////////////////////////////////////////
    // Preliminary check....

    /**
     * This test parses and typechecks all the book sample programs. These should
     * type check without any errors.
     * <p>
     * By itself this is not a very good test. E.g. an implementation which does nothing
     * at all will already pass the test!
     */
    @Test
    public void testSampleCode() throws Exception {
        File[] sampleFiles = SampleCode.sampleFiles("java");
        for (File sampleFile : sampleFiles) {
            System.out.println("parsing: " + sampleFile);
            accept(sampleFile);
        }
    }

    ///////////////////////// Helpers /////////////////////////////////////////

    private ErrorMessage typeError(String exp, Type expected, Type actual)
	throws ParseException {
        return ErrorMessage.typeError(parseExp(exp), expected, actual);
    }

    private void accept(File file) throws TypeCheckerException, Exception {
        TypeChecker.parseAndCheck(file);
    }

    private void accept(String string) throws TypeCheckerException, Exception {
        TypeChecker.parseAndCheck(string);
    }

    /**
     * Mostly what we want to do in this set of unit tests is see
     * whether the checker produces the right kind of error
     * reports. This is a helper method to do just that.
     */
    private void expect(ErrorMessage expect, String input) throws Exception {
        try {
            TypeChecker.parseAndCheck(input);
            Assertions.fail(
	      "A TypeCheckerException should have been raised but was not.");
        } catch (TypeCheckerException e) {
            Assertions.assertEquals(expect, e.getFirstMessage());
        }
    }
}
