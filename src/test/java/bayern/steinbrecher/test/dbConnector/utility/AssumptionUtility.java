package bayern.steinbrecher.test.dbConnector.utility;

import org.junit.jupiter.api.function.ThrowingSupplier;
import org.opentest4j.TestAbortedException;

/**
 * @author Stefan Huber
 * @since 0.10
 */
public final class AssumptionUtility {
    private AssumptionUtility(){
        throw new UnsupportedOperationException("Creation of instances prohibited");
    }

    public static <T> T assumeDoesNotThrow(ThrowingSupplier<T> supplier, String message){
        try {
            return supplier.get();
        } catch (Throwable throwable) {
            throw new TestAbortedException(message, throwable);
        }
    }
}
